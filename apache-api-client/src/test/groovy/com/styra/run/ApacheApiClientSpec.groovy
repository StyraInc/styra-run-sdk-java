package com.styra.run

import com.styra.run.ApiClient.Method
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.TrustManagerFactory
import java.net.http.HttpTimeoutException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.concurrent.ExecutionException

import static com.google.code.tempusfugit.temporal.Duration.millis
import static com.google.code.tempusfugit.temporal.Timeout.timeout
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout
import static com.styra.run.ApiClient.Method.DELETE
import static com.styra.run.ApiClient.Method.GET
import static com.styra.run.ApiClient.Method.POST
import static com.styra.run.ApiClient.Method.PUT
import static java.time.Duration.between
import static java.time.Instant.now
import static okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE

class ApacheApiClientSpec extends Specification {
    def "DefaultApiClient is used by default when no factory is specified"() {
        when: 'a StyraRun instance is built without specifying an ApiClient factory'
        def client = StyraRun.builder("https://localhost:1234", "my-token")
                .build()
                .apiClient

        then: 'the client used is a DefaultApiClient'
        client instanceof ApacheApiClient
    }

    @Unroll
    def "API client can do basic requests"(Method method, int status, String path, Map<String, String> headers, String requestBody, String responseBody) {
        given: 'a server mocking the Styra Run API'
        def mockServer = new MockWebServer()
        mockServer.enqueue(new MockResponse()
                .setResponseCode(status)
                .with {
                    if (responseBody != null) {
                        it.setBody(responseBody)
                    }
                    return it
                })

        mockServer.start()
        def uri = mockServer.url(path).uri()

        and: 'an API client'
        def client = new ApacheApiClient(new ApiClient.Config(SSLContext.getDefault(),
                Duration.ofSeconds(2), Duration.ofSeconds(2), "test"))

        when: 'a request is made'
        def response = client.request(method, uri, headers, requestBody).get()

        then: 'the server receives it'
        def recordedRequest = mockServer.takeRequest()
        recordedRequest.requestUrl.uri() == uri
        recordedRequest.method == method.name()
        assertHeaders(recordedRequest.headers, headers)
        assertBody(method, recordedRequest, requestBody)

        and: 'the client gets the response'
        response.statusCode == status
        assertBody(response, responseBody)

        cleanup:
        mockServer.shutdown()
        client.close()

        where:
        [method, status, path, headers, requestBody, responseBody] << [
                [GET, PUT, POST, DELETE],
                [200, 300, 400, 500, 999],
                ['/', '/foo', '/foo/bar', '/foo?bar=baz'],
                [[:], [foo: 'bar']],
                [null, '', 'foo'],
                [null, '', 'foo']
        ].combinations()
    }

    @Unroll
    def "API client sends User-Agent header (#userAgent)"() {
        given: 'a server mocking the Styra Run API'
        def mockServer = new MockWebServer()
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200))

        mockServer.start()
        def uri = mockServer.url('/').uri()

        and: 'an API client'
        def client = new ApacheApiClient(new ApiClient.Config(SSLContext.getDefault(),
                Duration.ofSeconds(2), Duration.ofSeconds(2), userAgent))

        when: 'a request is made'
        client.request(GET, uri, [:], null).get()

        then: 'the server receives the expected User-Agent'
        def recordedRequest = mockServer.takeRequest()
        recordedRequest.headers.get("User-Agent") == userAgent

        cleanup:
        mockServer.shutdown()
        client.close()

        where:
        userAgent << ['', 'foobar']
    }

    @Ignore("Triggers 'IOReactorShutdownException: I/O reactor has been shut down' issue")
    @Unroll
    def "Configured connection timeout is respected (#connectionTimeout)"() {
        given: 'a server mocking the Styra Run API that never accepts incoming connections'
        // a server socket with a backlog of one
        def serverSocket = new ServerSocket(0, 1)
        // fill up the backlog so the API client can't connect
        new Socket().connect(serverSocket.getLocalSocketAddress())

        def uri = URI.create("http://localhost:${serverSocket.localPort}")

        and: 'an API client'
        def client = new ApacheApiClient(new ApiClient.Config(SSLContext.getDefault(),
                connectionTimeout, Duration.ofSeconds(99), "test"))

        when: 'a request is made'
        def start = now()
        def responseFuture = client.request(GET, uri, [:], null)
        waitOrTimeout(responseFuture::isDone, timeout(millis(connectionTimeout.toMillis() + 1_000)))

        then: 'the timeout happened within reasonable bounds'
        assertDivergence(between(start, now()), connectionTimeout)

        when: 'the response is retrieved'
        responseFuture.get()

        then: 'we get a timeout exception'
        def e = thrown(ExecutionException)
        e.cause instanceof HttpTimeoutException

        cleanup:
        serverSocket.close()
        client.close()

        where:
        connectionTimeout << [
                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        ]
    }

    @Unroll
    def "Configured request timeout is respected (#requestTimeout)"() {
        given: 'a server mocking the Styra Run API that never sends a response'
        def mockServer = new MockWebServer()
        mockServer.enqueue(new MockResponse()
                .setSocketPolicy(NO_RESPONSE)
                .setResponseCode(200))

        mockServer.start()
        def uri = mockServer.url('/').uri()

        and: 'an API client'
        def client = new ApacheApiClient(new ApiClient.Config(SSLContext.getDefault(),
                Duration.ofSeconds(99), requestTimeout, "test"))

        when: 'a request is made'
        def start = now()
        def responseFuture = client.request(GET, uri, [:], null)
        waitOrTimeout(responseFuture::isDone, timeout(millis(requestTimeout.toMillis() + 1_000)))

        then: 'the timeout happened within reasonable bounds'
        assertDivergence(between(start, now()), requestTimeout)

        when: 'the response is retrieved'
        responseFuture.get()

        then: 'we get a timeout exception'
        def e = thrown(ExecutionException)
        e.cause instanceof SocketTimeoutException

        cleanup:
        mockServer.shutdown()
        client.close()

        where:
        requestTimeout << [
                // It seems like the Apache client can't handle millisecond granularity, instead it rounds up to the closest second
//                Duration.ofMillis(500),
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        ]
    }

    def "Configured SSL Context is respected"() {
        given: 'a server mocking the Styra Run API with a custom certificate'
        def localhost = InetAddress.getByName("localhost").getCanonicalHostName()
        def certificate = new HeldCertificate.Builder()
                .addSubjectAlternativeName(localhost)
                .build()
        def serverCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(certificate)
                .build()

        def mockServer = new MockWebServer()
        mockServer.useHttps(serverCertificates.sslSocketFactory(), false)
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200))

        mockServer.start()
        def uri = mockServer.url('/').uri()

        and: 'an API client with the default SSL Context'
        def clientWithStandardTrust = new ApacheApiClient(new ApiClient.Config(SSLContext.getDefault(),
                Duration.ofSeconds(2), Duration.ofSeconds(2), "test"))

        and: 'an API client with a custom SSL Context that trusts the server cert'
        def trustStore = makeKeystore(certificate.certificate())
        def customSslContext = makeSslContext(trustStore)
        def clientWithCustomTrust = new ApacheApiClient(new ApiClient.Config(customSslContext,
                Duration.ofSeconds(2), Duration.ofSeconds(2), "test"))

        when: 'a request is made with a client with default trust'
        clientWithStandardTrust.request(GET, uri, [:], null).get()

        then: "we get an exception, as the server cert isn't trusted"
        def e = thrown(ExecutionException)
        e.cause instanceof SSLHandshakeException

        when: 'a request is made with a client that has the required custom trust'
        def response = clientWithCustomTrust.request(GET, uri, [:], null).get()

        then: 'the server is trusted'
        response.statusCode == 200

        cleanup:
        mockServer.shutdown()
        clientWithStandardTrust.close()
        clientWithCustomTrust.close()
    }

    static SSLContext makeSslContext(KeyStore trustStore) {
        def sslContext = SSLContext.getInstance("SSL")
        def trust = TrustManagerFactory.getInstance(TrustManagerFactory.defaultAlgorithm)
        trust.init(trustStore)
        sslContext.init([] as KeyManager[], trust.trustManagers , new SecureRandom())
        return sslContext
    }

    static KeyStore makeKeystore(X509Certificate cert, alias = "trusted") {
        def keystore = KeyStore.getInstance(KeyStore.defaultType)
        keystore.load(null, null)
        keystore.setCertificateEntry(alias, cert)
        return keystore
    }

    static void assertDivergence(Duration expiredTime, Duration expectedTime,
                                 Duration acceptedDivergence = Duration.ofMillis(100)) {
        def divergence = (expiredTime - expectedTime).abs()
        assert divergence < acceptedDivergence
    }

    static void assertHeaders(Headers headers, expectedHeaders) {
        expectedHeaders.forEach { expectedHeaderName, expectedHeaderValue ->
            assert headers.get(expectedHeaderName) == expectedHeaderValue
        }
    }

    static void assertBody(Method method, RecordedRequest request, expectedBody) {
        if (method == GET || method == DELETE || expectedBody == null) {
            assert request.body.size() == 0
        } else {
            assert request.body.readUtf8() == expectedBody
        }
    }

    static void assertBody(ApiResponse response, expectedBody) {
        if (expectedBody == null) {
            assert response.body == null || response.body.empty
        } else {
            assert response.body == expectedBody
        }
    }
}

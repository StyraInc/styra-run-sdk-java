package com.styra.run.discovery

import com.styra.run.*
import com.styra.run.utils.CountingApiClient
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.CompletableFuture

import static com.styra.run.ApiClient.Method.GET
import static org.junit.jupiter.api.Assertions.assertEquals

class ApiGatewaySelectorSpec extends Specification {
    private static JSON = new Json()
    private static final ENV_URI = URI.create('https://localhost:1337')
    private static final MAX_ATTEMPTS = 3
    private static final HEADER_SUPPLIER = { -> [:] }

    @Unroll
    def "Selector calls API to fetch existing gateways"() {
        def url = 'https://localhost:1337'
        def envUri = URI.create(url)
        def gatewaysUri = URI.create(url + "/gateways")
        def selectionStrategy = new SimpleGatewaySelectionStrategy.Factory()

        given: 'a mocked API-client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            assertEquals(GET, method)
            assertEquals(gatewaysUri, uri)
            return CompletableFuture.completedFuture(new ApiResponse(200, JSON.from(responseBody)))
        })

        and: 'the gateway-selector to test'
        def selector = new ApiGatewaySelector(selectionStrategy, MAX_ATTEMPTS, client, JSON, envUri, HEADER_SUPPLIER)

        when: 'the selector is asked to fetch gateways'
        def gateways = selector.fetchGateways()

        then: 'they are as expected'
        gateways == expectedGateways

        where:
        responseBody                             || expectedGateways
        [result: []]                             || []
        [result: [[gateway_url: 'https://foo']]] || [gw('https://foo')]
        [result: [[gateway_url: 'https://foo'],
                  [gateway_url: 'https://bar']]] || [gw('https://foo'), gw('https://bar')]

        // The selector ignores gateways containing an invalid URI
        [result: []]                             || []
        [result: [[gateway_url: 'bad{}']]]       || []
        [result: [[gateway_url: '://']]]         || []
        [result: [[gateway_url: ':'],
                  [gateway_url: 'https://foo']]] || [gw('https://foo')]
        [result: [[gateway_url: 'https://foo'],
                  [gateway_url: 'bad{}'],
                  [gateway_url: 'https://bar'],
                  [gateway_url: ':']]]           || [gw('https://foo'), gw('https://bar')]
    }

    @Unroll
    def "Gateway meta-data is maintained"() {
        def selectionStrategy = new SimpleGatewaySelectionStrategy.Factory()

        given: 'a mocked API-client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            return CompletableFuture.completedFuture(new ApiResponse(200, JSON.from(responseBody)))
        })

        and: 'the gateway-selector to test'
        def selector = new ApiGatewaySelector(selectionStrategy, MAX_ATTEMPTS, client, JSON, ENV_URI, HEADER_SUPPLIER)

        when: 'the selector is asked to fetch gateways'
        def gateways = selector.fetchGateways()

        then: 'the returned gateway has the expected meta-data'
        gateways[0].attributes == expectedAttributes

        where:
        responseBody                                       || expectedAttributes
        [result: [[gateway_url: 'https://foo']]]           || [:]
        [result: [[gateway_url: 'https://foo',
                   foo        : 'bar']]]                   || [foo: 'bar']
        [result: [[gateway_url: 'https://foo',
                   aws        : [region : 'us-east-1',
                                 zone_id: 'use1-az1',
                                 zone   : 'us-east-1c']]]] || [aws: [region : 'us-east-1',
                                                                     zone_id: 'use1-az1',
                                                                     zone   : 'us-east-1c']]
    }

    @Unroll
    def "Selector throws exception for unexpected status codes from Run API"() {
        def selectionStrategy = new SimpleGatewaySelectionStrategy.Factory()

        given: 'a mocked API-client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            return CompletableFuture.completedFuture(new ApiResponse(statusCode, responseBody))
        })

        and: 'the gateway-selector to test'
        def selector = new ApiGatewaySelector(selectionStrategy, MAX_ATTEMPTS, client, JSON, ENV_URI, HEADER_SUPPLIER)

        when: 'the selector is asked to fetch gateways'
        selector.fetchGateways()

        then: 'an exception is thrown'
        def exception = thrown(StyraRunHttpException)
        exception.message == expectedExceptionMessage
        exception.apiError == expectedApiError

        where:
        statusCode | responseBody                             || expectedExceptionMessage       | expectedApiError
        400        | JSON.from([])                            || 'Unexpected status code: 400'  | er(null, null)
        400        | JSON.from([code: ''])                    || 'Unexpected status code: 400'  | er('', null)
        400        | JSON.from([message: ''])                 || 'Unexpected status code: 400'  | er(null, '')
        400        | JSON.from([code: '', message: ''])       || 'Unexpected status code: 400'  | er('', '')
        400        | JSON.from([code: 'foo', message: 'bar']) || 'Unexpected status code: 400'  | er('foo', 'bar')

        401        | ''                                       || 'Unexpected status code: 401'  | er(null, null)
        403        | ''                                       || 'Unexpected status code: 403'  | er(null, null)
        405        | ''                                       || 'Unexpected status code: 405'  | er(null, null)
        499        | ''                                       || 'Unexpected status code: 499'  | er(null, null)

        500        | ''                                       || 'Unexpected status code: 500'  | er(null, null)
        501        | ''                                       || 'Unexpected status code: 501'  | er(null, null)
        503        | ''                                       || 'Unexpected status code: 503'  | er(null, null)
        505        | ''                                       || 'Unexpected status code: 505'  | er(null, null)
        599        | ''                                       || 'Unexpected status code: 599'  | er(null, null)

        000        | ''                                       || 'Unexpected status code: 0'    | er(null, null)
        1337       | ''                                       || 'Unexpected status code: 1337' | er(null, null)

        // Broken JSON body in response
        400        | 'foobar'                                 || 'Unexpected status code: 400'  | er(null, null)
        500        | 'foobar'                                 || 'Unexpected status code: 500'  | er(null, null)
    }

    @Unroll
    def "Selector throws exception for broken JSON body in response from Run API"() {
        def selectionStrategy = new SimpleGatewaySelectionStrategy.Factory()

        given: 'a mocked API-client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            return CompletableFuture.completedFuture(new ApiResponse(200, responseBody))
        })

        and: 'the gateway-selector to test'
        def selector = new ApiGatewaySelector(selectionStrategy, MAX_ATTEMPTS, client, JSON, ENV_URI, HEADER_SUPPLIER)

        when: 'the selector is asked to fetch gateways'
        selector.fetchGateways()

        then: 'an exception is thrown'
        def exception = thrown(StyraRunException)
        exception.message == 'Invalid response JSON'

        where:
        responseBody << ['', '{', 'foobar']
    }

    @Unroll
    def "Selector throws exception when API-client throws exception"(Exception exception) {
        def selectionStrategy = new SimpleGatewaySelectionStrategy.Factory()

        given: 'a mocked API-client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            return CompletableFuture.<ApiResponse> failedFuture(exception)
        })

        and: 'the gateway-selector to test'
        def selector = new ApiGatewaySelector(selectionStrategy, MAX_ATTEMPTS, client, JSON, ENV_URI, HEADER_SUPPLIER)

        when: 'the selector is asked to fetch gateways'
        selector.fetchGateways()

        then: 'an exception is thrown'
        def e = thrown(StyraRunException)
        e.message == expectedErrorMessage
        if (exception instanceof StyraRunException) {
            assertEquals(exception, e)
        } else {
            assertEquals(exception, e.cause)
        }

        where:
        exception                              || expectedErrorMessage
        new Exception('foo')                   || 'Unexpected error'
        new RuntimeException('foo')            || 'Unexpected error'
        new IllegalArgumentException()         || 'Unexpected error'
        new NullPointerException()             || 'Unexpected error'
        new StyraRunException('bar')           || 'bar'
        new StyraRunHttpException(111, 'body') || 'Unexpected status code: 111'
    }

    @Unroll
    def "Selector forwards headers to Run API"() {
        def selectionStrategy = new SimpleGatewaySelectionStrategy.Factory()

        given: 'a mocked API-client'
        def client = new CountingApiClient(responseSupplier: { method, uri, requestHeaders, body ->
            assertEquals(headers, requestHeaders)
            return CompletableFuture.completedFuture(new ApiResponse(200, JSON.from([result: []])))
        })

        and: 'the gateway-selector to test'
        def selector = new ApiGatewaySelector(selectionStrategy, MAX_ATTEMPTS, client, JSON, ENV_URI, { -> headers })

        when: 'the selector is asked to fetch gateways'
        def gateways = selector.fetchGateways()

        then: 'the returned gateway has the expected meta-data'
        gateways == []

        where:
        headers << [
                [:],
                [foo: 'bar'],
                [Authorization: 'Bearer my-secret-token']
        ]
    }

    private static Gateway gw(uri, attributes = [:]) {
        return new Gateway(URI.create(uri), attributes)
    }

    private static ApiError er(code, message) {
        return new ApiError(code, message)
    }
}

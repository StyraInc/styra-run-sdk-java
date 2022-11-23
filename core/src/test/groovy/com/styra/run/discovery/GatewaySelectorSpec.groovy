package com.styra.run.discovery

import com.styra.run.exceptions.RetryException
import com.styra.run.exceptions.StyraRunException
import com.styra.run.test.CountingApiClient
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.ExecutionException

import static com.styra.run.ApiClient.Method.GET
import static com.styra.run.test.apiClients.exceptionalResult
import static com.styra.run.test.apiClients.httpResult
import static com.styra.run.test.helpers.gatewaysFrom

class GatewaySelectorSpec extends Specification {
    def "If gateway fetch fails, it will be performed again on subsequent requests"(
            Closure<List<Gateway>> gatewaysSupplier, String expectedErrorMessage) {
        given: 'a factory that will never be called'
        def factory = Mock(GatewaySelectionStrategy.Factory)

        and: 'a gateway-selector that fails on fetch'
        def selector = new GatewaySelector(factory, 10) {
            @Override
            protected List<Gateway> fetchGateways() {
                return gatewaysSupplier()
            }
        }

        and: 'a client that will never be called'
        def client = new CountingApiClient()

        when: 'the selector is called'
        selector.retry(client.requestBuilder(GET)).get()
        factory.create([])

        then: 'we get an exception'
        def e = thrown(ExecutionException)
        e.cause instanceof StyraRunException
        e.cause.message == expectedErrorMessage

        and: 'the factory was never called'
        0 * factory.create([])

        and: 'the client was never called'
        client.hitCount == 0

        where:
        gatewaysSupplier                     || expectedErrorMessage
        ({ -> null })                        || 'No gateways could be fetched'
        ({ -> [] })                          || 'No gateways could be fetched'
        ({ -> throw new Exception("Oops") }) || 'Failed to fetch gateways'

    }

    @Unroll
    def "Retry attempts are bounded by an upper limit"() {
        given: 'a set of gateways'
        def gateways = gatewaysFrom(gatewayCount)

        and: 'a counting API-client that will always return an error'
        def client = new CountingApiClient(responseSupplier: exceptionalResult(new RetryException(new StyraRunException("Pass-through error"))))

        and: 'a gateway-selector'
        def selector = new StaticGatewaySelector(new SimpleGatewaySelectionStrategy.Factory(), maxAttempts, gateways)

        when: 'the selector is called'
        selector.retry(client.requestBuilder(GET)).get()

        then: 'we get an exception'
        ExecutionException e = thrown()
        e.cause instanceof StyraRunException
        e.cause.message == expectedError

        and: 'the client was only called the expected times'
        client.hitCount == expectedApiCalls

        where:
        maxAttempts | gatewayCount || expectedError                     | expectedApiCalls
        0           | 1            || 'No API request attempts allowed' | 0
        1           | 1            || 'Pass-through error'              | 1
        100         | 1            || 'Pass-through error'              | 1
        100         | 5            || 'Pass-through error'              | 5
        5           | 100          || 'Pass-through error'              | 5
    }

    @Unroll
    def "API calls are retried for certain HTTP status codes"() {
        def maxAttempts = 3
        def gateways = gatewaysFrom(5)
        def expectedBody = 'foobar'

        given: 'a counting API-client that will always return an error'
        def client = new CountingApiClient(responseSupplier: httpResult(statusCode, expectedBody))

        and: 'a gateway-selector'
        def selector = new StaticGatewaySelector(new SimpleGatewaySelectionStrategy.Factory(), maxAttempts, gateways)

        when: 'the selector is called'
        def response = selector.retry(client.requestBuilder(GET)).get()

        then: 'we get the expected HTTP response'
        response.statusCode == statusCode
        response.body == expectedBody

        and: 'the client was only called the expected times'
        def expectedApiCalls = expectRetry ? maxAttempts : 1
        client.hitCount == expectedApiCalls

        where:
        statusCode || expectRetry
        101        || false
        200        || false
        400        || false
        401        || false
        403        || false
        421        || true
        500        || false
        501        || false
        502        || true
        503        || true
        504        || true
        505        || false
    }

    @Unroll
    def "API calls are retried on thrown RetryException"() {
        def maxAttempts = 3
        def gateways = gatewaysFrom(5)

        given: 'a counting API-client that will always return an error'
        def client = new CountingApiClient(responseSupplier: exceptionalResult(exception))

        and: 'a gateway-selector'
        def selector = new StaticGatewaySelector(new SimpleGatewaySelectionStrategy.Factory(), maxAttempts, gateways)

        when: 'the selector is called'
        selector.retry(client.requestBuilder(GET)).get()

        then: 'the expected exception was thrown'
        def e = thrown(ExecutionException)
        def thrownException = e.cause
        expectedExType.isInstance(thrownException)
        thrownException.message == expectedExMessage

        and: 'the client was only called the expected times'
        def expectedApiCalls = expectRetry ? maxAttempts : 1
        client.hitCount == expectedApiCalls

        where:
        exception                                        || expectRetry | expectedExType    | expectedExMessage
        new RetryException(new StyraRunException('foo')) || true        | StyraRunException | 'foo'
        new RetryException(new Exception('bar'))         || true        | Exception         | 'bar'
        new RetryException(new RuntimeException('baz'))  || true        | RuntimeException  | 'baz'

        new StyraRunException('foo')                     || false       | StyraRunException | 'foo'
        new Exception('bar')                             || false       | Exception         | 'bar'
        new RuntimeException('baz')                      || false       | RuntimeException  | 'baz'
    }
}

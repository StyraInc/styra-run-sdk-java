package com.styra.run

import com.fasterxml.jackson.jr.ob.JSON
import com.styra.run.test.CountingApiClient
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.ExecutionException

import static com.styra.run.ApiClient.Method.DELETE
import static com.styra.run.ApiClient.Method.GET
import static com.styra.run.ApiClient.Method.POST
import static com.styra.run.ApiClient.Method.PUT
import static BatchQuery.Item
import static com.styra.run.test.apiClients.httpResult
import static java.util.concurrent.CompletableFuture.completedFuture

class StyraRunSpec extends Specification {
    private static final String DEFAULT_GATEWAY = 'https://localhost:1234'
    private static final ArrayList<String> DEFAULT_GATEWAYS = [DEFAULT_GATEWAY]

    @Unroll
    def "SDK Builder throws exceptions when appropriate"() {
        when: 'initialized with a null env URL'
        StyraRun.builder(null as String, 'my-token')

        then: 'an exception is thrown'
        def e1 = thrown(NullPointerException)
        e1.message == 'url must not be null'

        when: 'initialized with a null gateway URL list'
        StyraRun.builder(null as List<String>, 'my-token')

        then: 'an exception is thrown'
        def e2 = thrown(NullPointerException)
        e2.message == 'gateways must not be null'

        when: 'initialized with env URL and a null token'
        StyraRun.builder('https://localhost', null)

        then: 'an exception is thrown'
        def e3 = thrown(NullPointerException)
        e3.message == 'token must not be null'

        when: 'initialized with gateways and a null token'
        StyraRun.builder(['https://localhost'], null)

        then: 'an exception is thrown'
        def e4 = thrown(NullPointerException)
        e4.message == 'token must not be null'

        when: 'built with an invalid env URL'
        StyraRun.builder('invalid{}', 'my-token').build()

        then: 'an exception is thrown'
        def e5 = thrown(IllegalStateException)
        e5.message == 'Malformed environment URI: invalid{}'

        when: 'built with an invalid gateway URL'
        StyraRun.builder(['invalid{}'], 'my-token').build()

        then: 'an exception is thrown'
        def e6 = thrown(IllegalStateException)
        e6.message == 'Malformed gateway URI: invalid{}'
    }

    @Unroll
    def "Authorization header is sent on API requests"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            assert method == POST
            def authzHeader = headers['Authorization']
            assert authzHeader == 'Bearer ' + token
            return completedFuture(new ApiResponse(200, '{}'))
        })

        when: 'the SDK is used'
        StyraRun.builder(DEFAULT_GATEWAYS, token)
                .apiClientFactory({_ -> client })
                .build()
                .query('/')
                .get()

        then: 'the client is used with the expected header'
        client.hitCount == 1

        where:
        token << [
                '',
                'foobar'
        ]
    }

    @Unroll
    def "Request URL is constructed properly for queries"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, body ->
            assert method == POST
            assert requestUri == URI.create(expectedUri)
            return completedFuture(new ApiResponse(200, '{}'))
        })

        when: 'the SDK is used'
        StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .query(path)
                .get()

        then: 'the client is used with the URI'
        client.hitCount == 1

        where:
        path          || expectedUri
        ''            || "$DEFAULT_GATEWAY/data"
        '/'           || "$DEFAULT_GATEWAY/data"
        '////'        || "$DEFAULT_GATEWAY/data"
        'my/path'     || "$DEFAULT_GATEWAY/data/my/path"
        '/my/path'    || "$DEFAULT_GATEWAY/data/my/path"
        '/my/path/'   || "$DEFAULT_GATEWAY/data/my/path"
        'my//path'    || "$DEFAULT_GATEWAY/data/my/path"
        ' my / path ' || "$DEFAULT_GATEWAY/data/my/path"
    }

    @Unroll
    def "An input JSON document is properly constructed for queries"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, body ->
            assert method == POST
            assert body == expectedRequestBody
            return completedFuture(new ApiResponse(200, '{}'))
        })

        when: 'the SDK is used'
        StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .query('/', new Input<>(input as Object))
                .get()

        then: 'the client was called'
        client.hitCount == 1

        where:
        input                                     || expectedRequestBody
        null                                      || '{}'
        ''                                        || '{"input":""}'
        ' '                                       || '{"input":" "}'
        'foo'                                     || '{"input":"foo"}'
        true                                      || '{"input":true}'
        false                                     || '{"input":false}'
        0                                         || '{"input":0}'
        0.0                                       || '{"input":0.0}'
        42                                        || '{"input":42}'
        ['foo', 1337, [a: 'b']]                   || '{"input":["foo",1337,{"a":"b"}]}'
        [foo: 'bar', 42: 1337, l: ['one', 'two']] || '{"input":{"foo":"bar","42":1337,"l":["one","two"]}}'
    }

    @Unroll
    def "API error responses are properly translated into exceptions"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, body ->
            assert method == POST
            def error = [:]
            if (errorCode != null) error['code'] = errorCode
            if (errorMessage != null) error['message'] = errorMessage
            return completedFuture(new ApiResponse(statusCode, JSON.std.asString(error)))
        })

        when: 'the SDK is used'
        StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .query('/')
                .get()

        then: 'an exception was thrown'
        def e = thrown(ExecutionException)
        e.cause instanceof StyraRunHttpException
        with(e.cause as StyraRunHttpException) {
            apiError.code == errorCode
            apiError.message == errorMessage
        }

        and: 'the client was called'
        client.hitCount == 1

        where:
        statusCode | errorCode            | errorMessage
        300        | null                 | null
        400        | null                 | null
        400        | 'resource_not_found' | 'Resource not found: policy not found'
        401        | 'foo'                | null
        402        | null                 | 'bar'
        403        | 'foo'                | 'bar'
        501        | 'foo'                | null
        502        | null                 | 'bar'
        503        | 'foo'                | 'bar'
        999        | null                 | null
    }

    @Unroll
    def "A result object is properly constructed for queries"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, body ->
            assert method == POST
            return completedFuture(new ApiResponse(200, responseBody as String))
        })

        when: 'the SDK is used'
        def result = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .query('/')
                .get()

        then: 'the result is as expected'
        result.get() == expectedResultValue
        result.attributes == expectedResultAttributes

        and: 'the client was called'
        client.hitCount == 1

        where:
        responseBody                     || expectedResultValue | expectedResultAttributes
        '{}'                             || null                | [:]
        '{"result": true}'               || true                | [:]
        '{"result": false}'              || false               | [:]
        '{"result": "hello"}'            || 'hello'             | [:]
        '{"result": ""}'                 || ''                  | [:]
        '{"result": 0}'                  || 0                   | [:]
        '{"result": 42}'                 || 42                  | [:]
        '{"result": -42}'                || -42                 | [:]
        '{"result": 4.2}'                || 4.2                 | [:]
        '{"result": [1, 2, 3]}'          || [1, 2, 3]           | [:]
        '{"result": {"foo": "bar"}}'     || [foo: 'bar']        | [:]
        '{"result": true, "foo": "bar"}' || true                | [foo: 'bar']
        '{"foo": "bar"}'                 || null                | [foo: 'bar']
    }

    def "Batch queries must not have empty items list"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: httpResult(200, '{}'))

        when: 'the SDK is used'
        StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .batchQuery([] as List<Item>)
                .get()

        then: 'an exception was thrown'
        def e = thrown(IllegalArgumentException)
        e.message == 'items must not be empty'

        and: 'the client was never called'
        client.hitCount == 0
    }

    @Unroll
    def "Batch queries can be made"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, requestBody ->
            assert method == POST
            def body = JSON.std.mapFrom(requestBody)
            assert body == expectedRequestBody
            return completedFuture(new ApiResponse(200, JSON.std.asString(responseBody) as String))
        })

        when: 'the SDK is used'
        def result = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .batchQuery(query as List<Item>)
                .get()

        then: 'the result is as expected'
        result.get() == expectedResult.get()
        result.attributes == expectedResult.attributes

        and: 'the client was called'
        client.hitCount == 1

        where:
        query                    | responseBody                 || expectedRequestBody                  | expectedResult
        [new Item('/')]          | [result: [[:]]]              || [items: [[path: '/']]]               | new ListResult([Result.empty()])
        [new Item('/')]          | [result: [[result: true]]]   || [items: [[path: '/']]]               | new ListResult([new Result(true)])
        [new Item('/one'),
         new Item('/two')]       | [result: [[result: true],
                                             [result: false]],] || [items: [[path: '/one'],
                                                                            [path: '/two']]]            | new ListResult([new Result(true),
                                                                                                                          new Result(false)])
        // query with input
        [new Item('/one'),
         new Item('/two',
                 new Input(42))] | [result: [[result: true],
                                             [result: false]],] || [items: [[path: '/one'],
                                                                            [path: '/two', input: 42]]] | new ListResult([new Result(true),
                                                                                                                          new Result(false)])
        // response with attributes
        [new Item('/one'),
         new Item('/two')]       | [result: [[result: true, foo: 'bar'],
                                             [result: false]],] || [items: [[path: '/one'],
                                                                            [path: '/two']]]            | new ListResult([new Result(true, [foo: 'bar']),
                                                                                                                          new Result(false)])
        // response with error
        [new Item('/one'),
         new Item('/two')]       | [result: [[error: [code: 'oopsie', message: 'daisy']],
                                             [result: false]],] || [items: [[path: '/one'],
                                                                            [path: '/two']]]            | new ListResult([Result.empty([error: [code: 'oopsie', message: 'daisy']]),
                                                                                                                          new Result(false)])
    }

    def "Batch queries can be made using a builder"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, bodyJson ->
            assert method == POST
            def body = JSON.std.mapFrom(bodyJson)
            assert body == [items: [
                    [path: '/my/path'],
                    [path: '/my/other/path', input: 'test'],
                    [path: '/', input: 42],
                    [path: '/err']
            ]]

            def responseBody = [result: [
                    [result: 'yes'],
                    [:],
                    [result: true, foo: 'bar'],
                    [error: [
                            code   : 'oopsie',
                            message: 'daisy'
                    ]]
            ]]
            return completedFuture(new ApiResponse(200, JSON.std.asString(responseBody)))
        })

        when: 'a batch query builder is used'
        def response = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .batchQueryBuilder()
                .query('/my/path')
                .query('/my/other/path', new Input('test'))
                .query('/', new Input(42))
                .query('/err')
                .execute()
                .get()

        then: 'we get the expected response'
        response.get() == [
                new Result("yes"),
                Result.empty(),
                new Result(true, [foo: 'bar']),
                Result.empty([error: [code: 'oopsie', message: 'daisy']])
        ]

        and: 'the client was called'
        client.hitCount == 1
    }

    @Unroll
    def "A boolean decision is returned for checks"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, body ->
            assert method == POST
            return completedFuture(new ApiResponse(200, responseBody as String))
        })

        when: 'the SDK is used'
        def decision = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .check('/')
                .get()

        then: 'the decision is as expected'
        decision == expectedDecision

        and: 'the client was called'
        client.hitCount == 1

        where:
        responseBody        || expectedDecision
        '{}'                || false
        '{"result": true}'  || true
        '{"result": false}' || false
        '{"result": 42}'    || false
    }

    @Unroll
    def "A boolean decision is returned for checks, and can be controlled with a predicate"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, body ->
            assert method == POST
            return completedFuture(new ApiResponse(200, responseBody as String))
        })

        when: 'the SDK is used'
        def decision = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .check('/', predicate)
                .get()

        then: 'the decision is as expected'
        decision == expectedDecision

        and: 'the client was called'
        client.hitCount == 1

        where:
        responseBody        | predicate                        || expectedDecision
        '{}'                | { result -> result.get() == 42 } || false
        '{}'                | { result -> result.isEmpty() }   || true
        '{"result": true}'  | { result -> result.get() == 42 } || false
        '{"result": false}' | { result -> result.get() == 42 } || false
        '{"result": 42}'    | { result -> result.get() == 42 } || true
        '{"result": 42}'    | { result -> result.get() == 42 } || true
    }

    @Unroll
    def "Get data"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, requestUri, headers, body ->
            assert method == GET
            assert body == null
            return completedFuture(new ApiResponse(200, responseBody as String))
        })

        when: 'the SDK is used'
        def result = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .getData('/')
                .get()

        then: 'the result is as expected'
        result.get() == expectedResultValue
        result.attributes == expectedResultAttributes

        and: 'the client was called'
        client.hitCount == 1

        where:
        responseBody                     || expectedResultValue | expectedResultAttributes
        '{}'                             || null                | [:]
        '{"result": true}'               || true                | [:]
        '{"result": false}'              || false               | [:]
        '{"result": "hello"}'            || 'hello'             | [:]
        '{"result": ""}'                 || ''                  | [:]
        '{"result": 0}'                  || 0                   | [:]
        '{"result": 42}'                 || 42                  | [:]
        '{"result": -42}'                || -42                 | [:]
        '{"result": 4.2}'                || 4.2                 | [:]
        '{"result": [1, 2, 3]}'          || [1, 2, 3]           | [:]
        '{"result": {"foo": "bar"}}'     || [foo: 'bar']        | [:]
        '{"result": true, "foo": "bar"}' || true                | [foo: 'bar']
        '{"foo": "bar"}'                 || null                | [foo: 'bar']
    }

    @Unroll
    def "Put data"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            assert method == PUT
            assert body == expectedRequestBody
            return completedFuture(new ApiResponse(200, responseBody as String))
        })

        when: 'the SDK is used'
        def result = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .putData('/', value)
                .get()

        then: 'the result is as expected'
        result.get() == null
        result.attributes == expectedResultAttributes

        and: 'the client was called'
        client.hitCount == 1

        where:
        value         | responseBody     || expectedRequestBody | expectedResultAttributes
        true          | '{}'             || 'true'              | [:]
        0             | '{}'             || '0'                 | [:]
        42            | '{}'             || '42'                | [:]
        -42           | '{}'             || '-42'               | [:]
        4.2           | '{}'             || '4.2'               | [:]
        'true'        | '{}'             || '"true"'            | [:]
        [true, false] | '{}'             || '[true,false]'      | [:]
        [true: false] | '{}'             || '{"true":false}'    | [:]

        true          | '{"foo": "bar"}' || 'true'              | [foo: 'bar']
    }

    @Unroll
    def "Delete data"() {
        given: 'a mocked API client'
        def client = new CountingApiClient(responseSupplier: { method, uri, headers, body ->
            assert method == DELETE
            assert body == null
            return completedFuture(new ApiResponse(200, responseBody as String))
        })

        when: 'the SDK is used'
        def result = StyraRun.builder(DEFAULT_GATEWAYS, 'token')
                .apiClientFactory({_ -> client })
                .build()
                .deleteData('/')
                .get()

        then: 'the result is as expected'
        result.get() == null
        result.attributes == expectedResultAttributes

        and: 'the client was called'
        client.hitCount == 1

        where:
        responseBody     || expectedResultAttributes
        '{}'             || [:]
        '{"foo": "bar"}' || [foo: 'bar']
    }
}

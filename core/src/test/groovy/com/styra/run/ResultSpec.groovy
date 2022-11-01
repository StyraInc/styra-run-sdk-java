package com.styra.run

import spock.lang.Specification
import spock.lang.Unroll

class ResultSpec extends Specification {
    @Unroll
    def "A Result carries a value (#value)"() {
        given: 'a Result'
        def result = new Result(value)

        expect:
        result.get() == value
        result.isEmpty() == expectEmpty
        def expectHasValue = value != null
        result.hasValue() == expectHasValue

        where:
        value          || expectEmpty
        null           || true
        []             || true
        [:]            || true

        0              || false
        42             || false
        -42            || false
        4.2            || false
        ''             || false
        'foo'          || false
        false          || false
        true           || false
        ['one', 'two'] || false
        [foo: 'bar']   || false
    }

    @Unroll
    def "A Result can return a value of a specific type (#value; #type)"() {
        given: 'a Result'
        def result = new Result(value)

        expect:
        try {
            def returnedValue = result.get(type)
            assert expectedException == null, 'Expected exception, got none'
            assert type.isInstance(returnedValue)
            assert returnedValue == value
        } catch (e) {
            assert expectedException != null, 'Unexpected exception'
            assert e.message == expectedException
        }

        where:
        value          | type       || expectedException
        null           | Object     || 'Result value is null'
        null           | String     || 'Result value is null'
        null           | Boolean    || 'Result value is null'

        true           | Object     || null
        true           | Boolean    || null
        true           | String     || 'Result value was expected to be of type java.lang.String, but was java.lang.Boolean'
        true           | Integer    || 'Result value was expected to be of type java.lang.Integer, but was java.lang.Boolean'

        42             | Object     || null
        42             | Boolean    || 'Result value was expected to be of type java.lang.Boolean, but was java.lang.Integer'
        42             | String     || 'Result value was expected to be of type java.lang.String, but was java.lang.Integer'
        42             | Number     || null
        42             | Integer    || null

        'hello'        | Object     || null
        'hello'        | Boolean    || 'Result value was expected to be of type java.lang.Boolean, but was java.lang.String'
        'hello'        | String     || null
        'hello'        | Integer    || 'Result value was expected to be of type java.lang.Integer, but was java.lang.String'

        ['one', 'two'] | Object     || null
        ['one', 'two'] | Collection || null
        ['one', 'two'] | List       || null
        ['one', 'two'] | Map        || 'Result value was expected to be of type java.util.Map, but was java.util.ArrayList'

        [foo: 'bar']   | Object     || null
        [foo: 'bar']   | Collection || 'Result value was expected to be of type java.util.Collection, but was java.util.LinkedHashMap'
        [foo: 'bar']   | List       || 'Result value was expected to be of type java.util.List, but was java.util.LinkedHashMap'
        [foo: 'bar']   | Map        || null
    }

    @Unroll
    def "A Result can return a default value if actual value isn't of expected type (#value; #type)"() {
        given: 'a Result'
        def result = new Result(value)

        expect:
        def returnedValue = result.getSafe(type, defaultValue)
        type.isInstance(returnedValue)
        if (expectDefault) {
            assert returnedValue == defaultValue
        } else {
            assert returnedValue == value
        }

        where:
        value | type    | defaultValue || expectDefault
        null  | Object  | true         || true
        null  | Boolean | true         || true
        null  | String  | 'foo'        || true
        null  | Number  | 42           || true
        null  | Integer | 42           || true

        true  | Object  | false        || false
        true  | Boolean | false        || false
        true  | String  | 'foo'        || true
        true  | Number  | 42           || true

        'foo' | Object  | 'bar'        || false
        'foo' | Boolean | false        || true
        'foo' | String  | 'bar'        || false
        'foo' | Number  | 42           || true

        42    | Object  | 1337         || false
        42    | Boolean | true         || true
        42    | String  | 'foo'        || true
        42    | Number  | 1337         || false
        42    | Integer | 1337         || false
    }

    @Unroll
    def "A Result can return a typed list value"() {
        given: 'a Result'
        def result = new Result(value)

        expect:
        try {
            def returnedValue = result.getListOf(type)
            assert expectedException == null, 'Expected exception, got none'
            assert returnedValue == value
        } catch (e) {
            assert expectedException != null, 'Unexpected exception'
            assert e.message == expectedException
        }

        where:
        value        | type    || expectedException
        null         | Number  || 'Result value is null'

        true         | Boolean || 'Result value was expected to be of type java.util.List, but was java.lang.Boolean'
        [foo: 'bar'] | String  || 'Result value was expected to be of type java.util.List, but was java.util.LinkedHashMap'

        []           | Number  || null
        [1, 2]       | Number  || null
        [1, 2]       | Integer || null

        [1, 2]       | Float   || 'Invalid result list item type; expected java.lang.Float, but was java.lang.Integer'
        [1, '2']     | Number  || 'Invalid result list item type; expected java.lang.Number, but was java.lang.String'
        [true]       | Number  || 'Invalid result list item type; expected java.lang.Number, but was java.lang.Boolean'
    }

    @Unroll
    def "A Result can return a typed map value"() {
        given: 'a Result'
        def result = new Result(value)

        expect:
        try {
            def returnedValue = result.getMapOf(type)
            assert expectedException == null, 'Expected exception, got none'
            assert returnedValue == expectedValue
        } catch (e) {
            assert expectedException != null, 'Unexpected exception'
            assert e.message == expectedException
        }

        where:
        value               | type    || expectedValue       | expectedException
        null                | Number  || null                | 'Result value is null'

        true                | Boolean || null                | 'Result value was expected to be of type java.util.Map, but was java.lang.Boolean'
        [true]              | Boolean || null                | 'Result value was expected to be of type java.util.Map, but was java.util.ArrayList'

        [:]                 | Boolean || [:]                 | null
        [v: true]           | Boolean || [v: true]           | null
        [x: true, y: false] | Boolean || [x: true, y: false] | null
        ['foo': true]       | Boolean || ['foo': true]       | null
        [42: true]          | Boolean || ['42': true]        | null

        [x: true, y: 42]    | Boolean || null                | 'Invalid result map entry value type; expected java.lang.Boolean, but was java.lang.Integer'
        [x: true, y: []]    | Boolean || null                | 'Invalid result map entry value type; expected java.lang.Boolean, but was java.util.ArrayList'
    }

    @Unroll
    def "A Result can carry attributes"() {
        given: 'a Result'
        def result = new Result(true, attributes as Map<String, ?>)

        expect:
        result.getAttributes() == attributes

        try {
            def attributeValue = result.getAttribute(key, type)
            assert expectedException == null, 'Expected exception, got none'
            assert type.isInstance(attributeValue)
            assert attributeValue == expectedAttributeValue
        } catch (e) {
            assert expectedException != null, 'Unexpected exception'
            assert e.message == expectedException
        }

        where:
        attributes  | key   | type    || expectedAttributeValue | expectedException
        [:]         | 'foo' | Number  || null                   | "Result attribute 'foo' is null"
        [bar: true] | 'foo' | Number  || null                   | "Result attribute 'foo' is null"

        [foo: true] | 'foo' | Number  || null                   | "Result attribute 'foo' was expected to be of type java.lang.Number, but was java.lang.Boolean"

        [foo: 42]   | 'foo' | Object  || 42                     | null
        [foo: 42]   | 'foo' | Number  || 42                     | null
        [foo: 42]   | 'foo' | Integer || 42                     | null
        [foo: 42]   | 'foo' | Float   || null                   | "Result attribute 'foo' was expected to be of type java.lang.Float, but was java.lang.Integer"
    }
}

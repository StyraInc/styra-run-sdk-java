package com.styra.run;

import com.fasterxml.jackson.jr.ob.JSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class StyraRunTests {
    private static final String TRUE_RESULT = "{\"result\": true}";
    private static final String EMPTY_RESULT = "{}";
    private static final String DEFAULT_BASE_URL = "https://example.com";
    private static final String DEFAULT_PATH = "foo/bar";
    private static final URI DEFAULT_DATA_URI = URI.create(String.format("%s/data/%s", DEFAULT_BASE_URL, DEFAULT_PATH));
    public static final String DEFAULT_TOKEN = "foobar";

    @Test
    void builder() {
        var urlException = assertThrows(NullPointerException.class,
                () -> StyraRun.builder(null, "foo"));
        assertEquals("url must not be null", urlException.getMessage());

        var tokenException = assertThrows(NullPointerException.class,
                () -> StyraRun.builder("https://example.com", null));
        assertEquals("token must not be null", tokenException.getMessage());

        var invalidUrlException = assertThrows(IllegalStateException.class,
                () -> StyraRun.builder("invalid{}", "token").build());
        assertEquals("Malformed API URI", invalidUrlException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "foo"})
    void query_token(String token) throws ExecutionException, InterruptedException {

        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
                assertEquals(String.format("Bearer %s", token), headers.get("Authorization"));
                return CompletableFuture.completedFuture(new ApiResponse(200, TRUE_RESULT));
            }
        };

        StyraRun.builder(DEFAULT_BASE_URL, token)
                .apiClient(mockedApiClient)
                .build()
                .query("/")
                .get();
    }

    static Stream<Arguments> query_url() throws MalformedURLException {
        return Stream.of(
                Arguments.of("https://example.com", null, "https://example.com/data"),
                Arguments.of("https://example.com", "", "https://example.com/data"),
                Arguments.of("https://example.com", "/", "https://example.com/data"),
                Arguments.of("https://example.com", "////", "https://example.com/data"),
                Arguments.of("https://example.com", "/my/path", "https://example.com/data/my/path"),
                Arguments.of("https://example.com", "//my//path", "https://example.com/data/my/path"),
                Arguments.of("https://example.com", "/my  /   path", "https://example.com/data/my/path")
        );
    }

    @ParameterizedTest
    @MethodSource
    void query_url(String url, String path, URI expectedUri) throws ExecutionException, InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
                assertEquals(expectedUri, uri);
                return CompletableFuture.completedFuture(new ApiResponse(200, TRUE_RESULT));
            }
        };

        try {
            StyraRun.builder(url, DEFAULT_TOKEN)
                    .apiClient(mockedApiClient)
                    .build()
                    .query(path)
                    .get();
        } catch (NullPointerException e) {
            if (path == null) {
                assertEquals("path must not be null", e.getMessage());
            } else {
                fail("Unexpected exception");
            }
        }
    }

    static Stream<Arguments> query_input() {
        return Stream.of(
                Arguments.of(null, "{}"),
                Arguments.of(true, "{\"input\": true}"),
                Arguments.of(42, "{\"input\": 42}"),
                Arguments.of("foo", "{\"input\": \"foo\"}"),
                Arguments.of(Map.of("foo", "bar"), "{\"input\": {\"foo\": \"bar\"}}"),
                Arguments.of(List.of("do", "re", "mi"), "{\"input\": [\"do\", \"re\", \"mi\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void query_input(Object input, String expectedBody) throws ExecutionException, InterruptedException, MalformedURLException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
                assertJsonEquals(expectedBody, body);
                if (input != null) {
                    assertJsonEquals(body, Map.of("input", input));
                } else {
                    assertJsonEquals(body, Map.of());
                }

                return CompletableFuture.completedFuture(new ApiResponse(200, TRUE_RESULT));

            }
        };

        StyraRun.builder(DEFAULT_BASE_URL, DEFAULT_TOKEN)
                .apiClient(mockedApiClient)
                .build()
                .query(DEFAULT_PATH, input)
                .get();
    }

    static Stream<Arguments> query_statusCode() {
        return Stream.of(
                Arguments.of(200, false, null, null),
                Arguments.of(201, false, null, null),
                Arguments.of(300, true, null, null),
                Arguments.of(400, true, null, null),
                Arguments.of(400, true, "resource_not_found", "Resource not found: policy not found"),
                Arguments.of(500, true, null, null),
                Arguments.of(500, true, "foo", null),
                Arguments.of(500, true, null, "bar"),
                Arguments.of(500, true, "foo", "bar")
        );
    }

    @ParameterizedTest
    @MethodSource
    void query_statusCode(int statusCode, boolean expectException, String expectedCode, String expectedMessage) throws InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
                var responseBody = new HashMap<String, String>();
                Utils.Nullable.ifNotNull(expectedCode, (v) -> responseBody.put("code", v));
                Utils.Nullable.ifNotNull(expectedMessage, (v) -> responseBody.put("message", v));

                try {
                    return CompletableFuture.completedFuture(new ApiResponse(statusCode, JSON.std.asString(responseBody)));
                } catch (IOException e) {
                    fail(e);
                    throw new RuntimeException(e);
                }
            }
        };

        try {
            StyraRun.builder(DEFAULT_BASE_URL, "foobar")
                    .apiClient(mockedApiClient)
                    .build()
                    .query(DEFAULT_PATH)
                    .get();
        } catch (ExecutionException e) {
            if (expectException) {
                var cause = assertInstanceOf(StyraRunHttpException.class, e.getCause());
                assertEquals(statusCode, cause.getStatusCode());
                assertEquals(String.format("Unexpected status code: %d", statusCode), cause.getMessage());
                Utils.Nullable.ifNotNull(expectedCode, (v) -> assertEquals(v, cause.getApiError().getCode()));
            } else {
                fail("Unexpected exception");
            }
        }
    }

    static Stream<Arguments> query_result() {
        return Stream.of(
                Arguments.of("{}", null, Map.of()),
                Arguments.of("\"foo\"", null, Map.of()),
                Arguments.of("{\"foo\": \"bar\"}", null, Map.of("foo", "bar")),
                Arguments.of("{\"result\": true}", true, Map.of()),
                Arguments.of("{\"result\": false}", false, Map.of()),
                Arguments.of("{\"result\": 1337}", 1337, Map.of()),
                Arguments.of("{\"result\": {\"foo\": \"bar\"}}", Map.of("foo", "bar"), Map.of()),
                Arguments.of("{\"result\": false, \"foo\": \"bar\"}", false, Map.of("foo", "bar"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void query_result(String responseBody, Object expectedResult, Map<String, ?> expectedAttributes) throws ExecutionException, InterruptedException, MalformedURLException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
                return CompletableFuture.completedFuture(new ApiResponse(200, responseBody));
            }
        };

        var result = StyraRun.builder(DEFAULT_BASE_URL, DEFAULT_TOKEN)
                .apiClient(mockedApiClient)
                .build()
                .query(DEFAULT_PATH)
                .get();

        assertEquals(expectedResult, result.get(), "result");
        assertEquals(expectedAttributes, result.getAttributes(), "attributes");
    }

    static Stream<Arguments> check_predicate() {
        return Stream.of(
                Arguments.of("{}", false, Result.empty()),
                Arguments.of("{}", true, Result.empty()),
                Arguments.of("{\"result\": true}", true, new Result<>(true)),
                Arguments.of("{\"result\": false}", false, new Result<>(false)),
                Arguments.of("{\"result\": 42}", false, new Result<>(42)),
                Arguments.of("{\"result\": 42}", true, new Result<>(42))
        );
    }

    @ParameterizedTest
    @MethodSource
    void check_predicate(String responseBody, boolean predicateResult, Result<?> expectedResult) throws ExecutionException, InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
                return CompletableFuture.completedFuture(new ApiResponse(200, responseBody));
            }
        };

        var decision = StyraRun.builder(DEFAULT_BASE_URL, DEFAULT_TOKEN)
                .apiClient(mockedApiClient)
                .build()
                .check(DEFAULT_PATH, (result) -> {
                    assertEquals(expectedResult, result);
                    return predicateResult;
                })
                .get();

        assertEquals(predicateResult, decision);
    }

    static Stream<Arguments> check_defaultPredicate() {
        return Stream.of(
                Arguments.of("{}", false),
                Arguments.of("{\"result\": true}", true),
                Arguments.of("{\"result\": false}", false),
                Arguments.of("{\"result\": 42}", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void check_defaultPredicate(String responseBody, boolean expectedDecision) throws ExecutionException, InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
                return CompletableFuture.completedFuture(new ApiResponse(200, responseBody));
            }
        };

        var decision = StyraRun.builder(DEFAULT_BASE_URL, DEFAULT_TOKEN)
                .apiClient(mockedApiClient)
                .build()
                .check(DEFAULT_PATH)
                .get();

        assertEquals(expectedDecision, decision);
    }

    static Stream<Arguments> getData() {
        return Stream.of(
                Arguments.of("{}", null),
                Arguments.of("{\"foo\": \"bar\"}", null),
                Arguments.of("{\"result\": true}", true),
                Arguments.of("{\"result\": false}", false),
                Arguments.of("{\"result\": 42}", 42),
                Arguments.of("{\"result\": 13.37}", 13.37),
                Arguments.of("{\"result\": \"bar\"}", "bar"),
                Arguments.of("{\"result\": {\"foo\": \"bar\"}}", Map.of("foo", "bar")),
                Arguments.of("{\"result\": [\"do\", \"re\", \"mi\"]}", List.of("do", "re", "mi"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void getData(String responseBody, Object expectedData) throws ExecutionException, InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> get(URI uri, Map<String, String> headers) {
                assertEquals(DEFAULT_DATA_URI, uri);
                return CompletableFuture.completedFuture(new ApiResponse(200, responseBody));
            }
        };

        var result = StyraRun.builder(DEFAULT_BASE_URL, DEFAULT_TOKEN)
                .apiClient(mockedApiClient)
                .build()
                .getData(DEFAULT_PATH)
                .get();

        assertEquals(expectedData, result.get());
    }

    static Stream<Arguments> putData() {
        return Stream.of(
                Arguments.of(true, "true"),
                Arguments.of("true", "\"true\""),
                Arguments.of(42, "42"),
                Arguments.of(13.37, "13.37"),
                Arguments.of(Map.of("foo", "bar"), "{\"foo\": \"bar\"}"),
                Arguments.of(List.of("do", "re", "mi"), "[\"do\", \"re\", \"mi\"]")
        );
    }

    @ParameterizedTest
    @MethodSource
    void putData(Object data, String expectedRequestBody) throws ExecutionException, InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> put(URI uri, String body, Map<String, String> headers) {
                assertEquals(DEFAULT_DATA_URI, uri);
                assertJsonEquals(expectedRequestBody, body);
                return CompletableFuture.completedFuture(new ApiResponse(200, EMPTY_RESULT));
            }
        };

        var result = StyraRun.builder(DEFAULT_BASE_URL, DEFAULT_TOKEN)
                .apiClient(mockedApiClient)
                .build()
                .putData(DEFAULT_PATH, data)
                .get();

        Assertions.assertNull(result.get());
    }

    @Test
    void deleteData() throws ExecutionException, InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> delete(URI uri, Map<String, String> headers) {
                assertEquals(DEFAULT_DATA_URI, uri);
                return CompletableFuture.completedFuture(new ApiResponse(200, EMPTY_RESULT));
            }
        };

        var result = StyraRun.builder(DEFAULT_BASE_URL, DEFAULT_TOKEN)
                .apiClient(mockedApiClient)
                .build()
                .deleteData(DEFAULT_PATH)
                .get();

        assertNull(result.get());
    }

    static void assertJsonEquals(String expected, String actual) {
        try {
            assertEquals(JSON.std.anyFrom(expected), JSON.std.anyFrom(actual));
        } catch (IOException e) {
            fail(e);
            throw new RuntimeException(e);
        }
    }

    static void assertJsonEquals(String expected, Object actual) {
        try {
            assertEquals(JSON.std.anyFrom(expected), actual);
        } catch (IOException e) {
            fail(e);
            throw new RuntimeException(e);
        }
    }
}

abstract class ApiClientMock implements ApiClient {
    @Override
    public CompletableFuture<ApiResponse> get(URI uri, Map<String, String> headers) {
        throw new IllegalStateException("Unexpected GET request");
    }

    @Override
    public CompletableFuture<ApiResponse> put(URI uri, String body, Map<String, String> headers) {
        throw new IllegalStateException("Unexpected PUT request");
    }

    @Override
    public CompletableFuture<ApiResponse> post(URI uri, String body, Map<String, String> headers) {
        throw new IllegalStateException("Unexpected POST request");
    }

    @Override
    public CompletableFuture<ApiResponse> delete(URI uri, Map<String, String> headers) {
        throw new IllegalStateException("Unexpected DELETE request");
    }
}

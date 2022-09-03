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
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class StyraRunTests {
    public static final String RESULT_TRUE = "{\"result\": true}";
    public static final String RESULT_EMPTY = "{}";

    @Test
    void builder() {
        var urlException = assertThrows(NullPointerException.class,
                () -> StyraRun.builder(null, "foo"));
        assertEquals("url must not be null", urlException.getMessage());

        var tokenException = assertThrows(NullPointerException.class,
                () -> StyraRun.builder("https://example.com", null));
        assertEquals("token must not be null", tokenException.getMessage());

        var invalidUrlException = assertThrows(IllegalStateException.class,
                () -> StyraRun.builder("invalid", "token").build());
        assertEquals("Malformed API URL", invalidUrlException.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "foo"})
    void query_token(String token) throws ExecutionException, InterruptedException {

        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URL url, String body, Map<String, String> headers) {
                assertEquals(String.format("Bearer %s", token), headers.get("Authorization"));
                return CompletableFuture.completedFuture(new ApiResponse(200, RESULT_TRUE));
            }
        };

        StyraRun.builder("http://example.com", token)
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
    void query_url(String url, String path, URL expectedUrl) throws ExecutionException, InterruptedException {
        var mockedApiClient = new ApiClientMock() {
            @Override
            public CompletableFuture<ApiResponse> post(URL url, String body, Map<String, String> headers) {
                assertEquals(expectedUrl, url);
                return CompletableFuture.completedFuture(new ApiResponse(200, RESULT_TRUE));
            }
        };

        try {
            StyraRun.builder(url, "foobar")
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
            public CompletableFuture<ApiResponse> post(URL url, String body, Map<String, String> headers) {
                try {
                    assertEquals(JSON.std.anyFrom(expectedBody), JSON.std.anyFrom(body));
                    if (input != null) {
                        assertEquals(JSON.std.anyFrom(body), Map.of("input", input));
                    } else {
                        assertEquals(JSON.std.anyFrom(body), Map.of());
                    }

                    return CompletableFuture.completedFuture(new ApiResponse(200, RESULT_TRUE));
                } catch (IOException e) {
                    fail(e);
                    throw new RuntimeException(e);
                }
            }
        };

        StyraRun.builder("https://example.com", "foobar")
                .apiClient(mockedApiClient)
                .build()
                .query("/my/path", input)
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
            public CompletableFuture<ApiResponse> post(URL url, String body, Map<String, String> headers) {
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
            StyraRun.builder("https://example.com", "foobar")
                    .apiClient(mockedApiClient)
                    .build()
                    .query("/my/path")
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
            public CompletableFuture<ApiResponse> post(URL url, String body, Map<String, String> headers) {
                return CompletableFuture.completedFuture(new ApiResponse(200, responseBody));
            }
        };

        var result = StyraRun.builder("https://example.com", "foobar")
                .apiClient(mockedApiClient)
                .build()
                .query("/")
                .get();

        assertEquals(expectedResult, result.getResult(), "result");
        assertEquals(expectedAttributes, result.getAttributes(), "attributes");
    }
}

abstract class ApiClientMock implements ApiClient {
    @Override
    public CompletableFuture<ApiResponse> get(URL url, Map<String, String> headers) {
        throw new IllegalStateException("Unexpected GET request");
    }

    @Override
    public CompletableFuture<ApiResponse> put(URL url, String body, Map<String, String> headers) {
        throw new IllegalStateException("Unexpected PUT request");
    }

    @Override
    public CompletableFuture<ApiResponse> delete(URL url, Map<String, String> headers) {
        throw new IllegalStateException("Unexpected DELETE request");
    }
}

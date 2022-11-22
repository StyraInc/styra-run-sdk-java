package com.styra.run;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.styra.run.utils.Null.ifNotNull;
import static com.styra.run.utils.Null.map;

public class ApiError {
    public static final String BAD_REQUEST_CODE = "bad_request";
    public static final String UNAUTHORIZED_CODE = "service_unauthorized";
    public static final String INTERNAL_ERROR_CODE = "internal_error";

    private static final String CODE_KEY = "code";
    private static final String MESSAGE_KEY = "message";
    private static final ApiError EMPTY = new ApiError(null, null);

    private final String code;
    private final String message;

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiError empty() {
        return EMPTY;
    }

    public static ApiError fromMap(Map<String, ?> map) {
        String code = map(map.get(CODE_KEY), String::valueOf);
        String message = map(map.get(MESSAGE_KEY), String::valueOf);
        if (code != null || message != null) {
            return new ApiError(code, message);
        }
        return null;
    }

    public static ApiError fromApiResponse(ApiResponse response, Json json) {
        return json.toOptionalMap(response.getBody())
                .map(ApiError::fromMap)
                .orElseGet(ApiError::empty);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Result<Void> toResult() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CODE_KEY, code);
        attributes.put(MESSAGE_KEY, message);
        return new Result<>(null, attributes);
    }

    public Map<String, ?> toMap() {
        Map<String, String> map = new HashMap<>();
        ifNotNull(code, c -> map.put(CODE_KEY, c));
        ifNotNull(message, m -> map.put(MESSAGE_KEY, m));
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiError apiError = (ApiError) o;
        return Objects.equals(code, apiError.code) && Objects.equals(message, apiError.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }

    @Override
    public String toString() {
        return "ApiError{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

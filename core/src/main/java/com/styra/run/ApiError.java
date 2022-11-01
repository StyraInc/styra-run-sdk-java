package com.styra.run;

import java.util.Map;
import java.util.Objects;

import static com.styra.run.utils.Null.map;

public class ApiError {
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
        String code = map(map.get("code"), String::valueOf);
        String message = map(map.get("message"), String::valueOf);
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

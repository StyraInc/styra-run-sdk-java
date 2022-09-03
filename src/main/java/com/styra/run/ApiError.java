package com.styra.run;

import java.util.Map;

import static com.styra.run.Utils.Nullable.map;

public class ApiError {
    private final String code;
    private final String message;

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiError fromMap(Map<String, ?> map) {
        String code = map(map.get("code"), String::valueOf);
        String message = map(map.get("message"), String::valueOf);
        if (code != null || message != null) {
            return new ApiError(code, message);
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

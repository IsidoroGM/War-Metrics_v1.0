package proyecto.IM.warMetrics.Proyecto1.error;

import java.util.Map;

public record ApiError(int status, String message, Map<String, String> fields, String reference, String code) {
    public ApiError(int status, String message, Map<String, String> fields, String reference) {
        this(status, message, fields, reference, null);
    }

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Map.of(), null);
    }

    public static ApiError of(int status, String message, String code) {
        return new ApiError(status, message, Map.of(), null, code);
    }
}

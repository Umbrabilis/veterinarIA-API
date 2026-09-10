package tech.veterinaria_api.common;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> detalles
) {
    public ApiError(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, List.of());
    }

    public ApiError(int status, String error, String message, String path, List<String> detalles) {
        this(Instant.now(), status, error, message, path, detalles);
    }
}

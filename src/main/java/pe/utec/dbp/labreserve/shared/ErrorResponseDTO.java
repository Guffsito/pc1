package pe.utec.dbp.labreserve.shared;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Cuerpo unico de error para toda la API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        ZonedDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> fieldErrors
) {

    public static ErrorResponseDTO of(int status, String error, String message, String path) {
        return new ErrorResponseDTO(ZonedDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponseDTO withFieldErrors(int status,
                                                   String error,
                                                   String message,
                                                   String path,
                                                   List<String> fieldErrors) {
        return new ErrorResponseDTO(ZonedDateTime.now(), status, error, message, path, fieldErrors);
    }
}

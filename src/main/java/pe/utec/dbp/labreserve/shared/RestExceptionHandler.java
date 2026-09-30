package pe.utec.dbp.labreserve.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDTO> handleApi(ApiException ex, HttpServletRequest request) {
        return body(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleBeanValidation(MethodArgumentNotValidException ex,
                                                                 HttpServletRequest request) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(RestExceptionHandler::describe)
                .toList();

        ErrorResponseDTO payload = ErrorResponseDTO.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Los datos enviados no son validos",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(payload);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleParamValidation(ConstraintViolationException ex,
                                                                  HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST,
                "El parametro '" + ex.getName() + "' tiene un formato invalido", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(HttpServletRequest request) {
        return body(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no se pudo leer", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(HttpServletRequest request) {
        return body(HttpStatus.FORBIDDEN, "No tiene permisos para esta operacion", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegrity(HttpServletRequest request) {
        return body(HttpStatus.CONFLICT, "La operacion rompe una restriccion de la base de datos", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleUnexpected(HttpServletRequest request) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    private static String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponseDTO> body(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponseDTO payload = ErrorResponseDTO.of(
                status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(payload);
    }
}

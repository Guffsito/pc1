package pe.utec.dbp.labreserve.shared;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base de negocio: cada subclase fija el estado HTTP con el que
 * el handler global la traduce a un ErrorResponseDTO.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

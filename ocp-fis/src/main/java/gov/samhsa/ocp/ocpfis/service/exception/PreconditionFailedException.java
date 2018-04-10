package gov.samhsa.ocp.ocpfis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
public class PreconditionFailedException extends RuntimeException {
    public PreconditionFailedException() {
        super();
    }
    public PreconditionFailedException(String message) {
        super(message);
    }
    public PreconditionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}


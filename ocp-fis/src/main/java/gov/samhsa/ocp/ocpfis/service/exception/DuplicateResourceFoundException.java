package gov.samhsa.ocp.ocpfis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceFoundException extends RuntimeException {
    public DuplicateResourceFoundException() {
        super();
    }

    public DuplicateResourceFoundException(String message) {
        super(message);
    }
}

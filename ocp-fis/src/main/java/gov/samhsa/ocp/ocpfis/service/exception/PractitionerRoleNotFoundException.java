package gov.samhsa.ocp.ocpfis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PractitionerRoleNotFoundException extends RuntimeException {
    public PractitionerRoleNotFoundException() {
        super();
    }

    public PractitionerRoleNotFoundException(String message) {
        super(message);
    }
}

package gov.samhsa.ocp.ocpfis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason ="Multiple patients found for the given MRN" )
public class MultiplePatientsFoundException extends RuntimeException {
    public MultiplePatientsFoundException() {
        super();
    }

    public MultiplePatientsFoundException(String message, Throwable cause,
                                          boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MultiplePatientsFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultiplePatientsFoundException(String message) {
        super(message);
    }

    public MultiplePatientsFoundException(Throwable cause) {
        super(cause);
    }
}

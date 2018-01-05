package gov.samhsa.ocp.ocpfis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class LocationNotFound extends RuntimeException {
    public LocationNotFound() {
        super();
    }

    public LocationNotFound(String message, Throwable cause,
                            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public LocationNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public LocationNotFound(String message) {
        super(message);
    }

    public LocationNotFound(Throwable cause) {
        super(cause);
    }
}

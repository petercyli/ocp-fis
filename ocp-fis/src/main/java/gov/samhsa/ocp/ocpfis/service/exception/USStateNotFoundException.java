package gov.samhsa.ocp.ocpfis.service.exception;

public class USStateNotFoundException extends RuntimeException {
    public USStateNotFoundException() {
    }

    public USStateNotFoundException(String message) {
        super(message);
    }

    public USStateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public USStateNotFoundException(Throwable cause) {
        super(cause);
    }

    public USStateNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

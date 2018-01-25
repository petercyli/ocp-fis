package gov.samhsa.ocp.ocpfis.service.exception;

public class InvalidValueException extends RuntimeException {

    public InvalidValueException() {
        super();
    }

    public InvalidValueException(String message) {
        super(message);
    }

}

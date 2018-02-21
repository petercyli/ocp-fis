package gov.samhsa.ocp.ocpfis.domain;

public enum RelatedPersonEnum {

    IDENTIFIER_TYPE("identifierType"),
    IDENTIFIER_VALUE("identiferValue");

    private final String code;

    RelatedPersonEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

package gov.samhsa.ocp.ocpfis.domain;

public enum CareTeamFieldEnum {
    STATUS("status"),
    CATEGORY("category"),
    SUBJECT("subject");

    private final String code;

    CareTeamFieldEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

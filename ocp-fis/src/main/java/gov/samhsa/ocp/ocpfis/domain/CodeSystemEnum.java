package gov.samhsa.ocp.ocpfis.domain;

import java.util.Arrays;

public enum CodeSystemEnum {

    // Common Systems


    // Resource Specific (Organize in Alphabetical order)
    APPOINTMENT_PARTICIPATION_TYPE("http://hl7.org/fhir/v3/ParticipationType"),

    CONSENT_PURPOSE_OF_USE("http://hl7.org/fhir/v3/ActReason"),
    CONSENT_ACTION("http://hl7.org/fhir/consentaction");

    private final String url;

    CodeSystemEnum(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public static boolean contains(String s) {
        return Arrays.stream(values()).anyMatch(ProfileURLEnum -> ProfileURLEnum.name().equalsIgnoreCase(s));
    }
}

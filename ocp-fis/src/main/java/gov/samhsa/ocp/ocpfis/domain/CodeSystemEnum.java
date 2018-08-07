package gov.samhsa.ocp.ocpfis.domain;

import java.util.Arrays;

public enum CodeSystemEnum {

    // Common Systems


    // Resource Specific (Organize in Alphabetical order)
    APPOINTMENT_PARTICIPATION_TYPE("http://hl7.org/fhir/v3/ParticipationType"),
    LANGUAGE("http://hl7.org/fhir/ValueSet/all-languages"),
    PROVIDER_ROLE("http://hl7.org/fhir/practitioner-role"),
    PROVIDER_SPECIALTY("http://snomed.info/sct");

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

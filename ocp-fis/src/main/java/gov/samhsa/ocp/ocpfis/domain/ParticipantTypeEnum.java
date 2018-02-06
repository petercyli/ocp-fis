package gov.samhsa.ocp.ocpfis.domain;

import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;

import java.util.Arrays;
import java.util.stream.Stream;

public enum ParticipantTypeEnum {
    practitioner("practitioner", "Practitioner"),
    relatedPerson("relatedPerson", "Related Person"),
    patient("patient", "Patient"),
    organization("organization", "Organization");

    private final String code;
    private final String name;

    ParticipantTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ParticipantTypeEnum fromCode(String code) {
        return asStream()
                .filter(participant -> participant.getCode().equals(code))
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("Participant type cannot be found with code: " + code));
    }

    public static Stream<ParticipantTypeEnum> asStream() {
        return Arrays.stream(values());
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}
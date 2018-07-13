package gov.samhsa.ocp.ocpfis.domain;

import java.util.Arrays;

public enum StructureDefinitionEnum {
    //US Core Profile
    CARETEAM("http://hl7.org/fhir/us/core/StructureDefinition/us-core-careteam"),
    LOCATION("http://hl7.org/fhir/us/core/StructureDefinition/us-core-location"),
    ORGANIZATION("http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization"),

    //Custom OCP Structure Definition
    ACTIVITYDEFINITION("http://hl7.org/fhir/StructureDefinition/ocp-activitydefinition"),
    APPOINTMENT("http://hl7.org/fhir/StructureDefinition/ocp-appointment"),

    COMMUNICATION("http://hl7.org/fhir/StructureDefinition/ocp-communication"),
    CONSENT("http://hl7.org/fhir/StructureDefinition/c2s-consent"),
    COVERAGE("http://hl7.org/fhir/StructureDefinition/ocp-coverage"),
    EPISODEOFCARE("http://hl7.org/fhir/StructureDefinition/ocp-episodeofcare"),
    FLAG("http://hl7.org/fhir/StructureDefinition/ocp-flag"),
    HEALTHCARESERVICE("http://hl7.org/fhir/StructureDefinition/ocp-healthcareservice"),

    PATIENT("http://hl7.org/fhir/StructureDefinition/ocp-patient"),
    PRACTITIONER("http://hl7.org/fhir/StructureDefinition/ocp-practitioner"),
    PRACTITIONERROLE("http://hl7.org/fhir/StructureDefinition/ocp-practitionerrole"),
    RELATEDPERSON("http://hl7.org/fhir/StructureDefinition/ocp-relatedperson"),
    TASK("http://hl7.org/fhir/StructureDefinition/ocp-task");

    private final String url;

    StructureDefinitionEnum(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public static boolean contains(String s) {
        return Arrays.stream(values()).anyMatch(ProfileURLEnum -> ProfileURLEnum.name().equalsIgnoreCase(s));
    }
}

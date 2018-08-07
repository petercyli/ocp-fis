package gov.samhsa.ocp.ocpfis.domain;

import java.util.Arrays;

public enum StructureDefinitionEnum {
    //Caution: Do NOT refactor enum names to include "_"
    //US Core Profile
    CARETEAM("http://hl7.org/fhir/us/core/StructureDefinition/us-core-careteam"),
    LOCATION("http://hl7.org/fhir/us/core/StructureDefinition/us-core-location"),
    ORGANIZATION("http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization"),

    //Custom OCP Structure Definition
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
    TASK("http://hl7.org/fhir/StructureDefinition/ocp-task"),

    // Valuesets etc
    US_CORE_SIMPLE_LANGUAGE("http://hl7.org/fhir/us/core/ValueSet/simple-language"),
    US_CORE_RACE("http://hl7.org/fhir/StructureDefinition/us-core-race"),
    US_CORE_ETHNICITY("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity"),
    US_CORE_BIRTHSEX("http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex");

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

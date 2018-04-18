package gov.samhsa.ocp.ocpfis.service.dto;

public enum LookupPathUrls {

    US_STATE("state", Constants.STATE_PATH),
    IDENTIFIER_TYPE("identifier type", Constants.IDENTIFIER_TYPE_PATH),
    IDENTIFIER_USE("identifier use", Constants.IDENTIFIER_USE_PATH),
    LOCATION_MODE("location mode", Constants.LOCATION_MODE_PATH),
    LOCATION_STATUS("location status", Constants.LOCATION_STATUS_PATH),
    LOCATION_PHYSICAL_TYPE("location physical type", Constants.LOCATION_PHYSICAL_TYPE_PATH),
    ADDRESS_TYPE("address type", Constants.ADDRESS_TYPE_PATH),
    ADDRESS_USE("address use", Constants.ADDRESS_USE_PATH),
    TELECOM_USE("telecom use", Constants.TELECOM_USE_PATH),
    TELECOM_SYSTEM("telecom system", Constants.TELECOM_SYSTEM_PATH),
    PRACTITIONER_ROLE("practitioner role", Constants.PRACTITIONER_ROLE_PATH),
    BIRTH_SEX("birth sex", Constants.BIRTH_SEX_PATH),
    HEALTHCARE_SERVICE_TYPE("healthcare service type", Constants.HEALTHCARE_SERVICE_TYPE_PATH),
    HEALTHCARE_SERVICE_CATEGORY("healthcare service category", Constants.HEALTHCARE_SERVICE_CATEGORY_PATH),
    HEALTHCARE_SERVICE_SPECIALITY("healthcare service speciality", Constants.HEALTHCARE_SERVICE_SPECIALITY_PATH),
    HEALTHCARE_SERVICE_SPECIALITY_2("healthcare service speciality 2", Constants.HEALTHCARE_SERVICE_SPECIALITY_2_PATH),
    HEALTHCARE_SERVICE_REFERRAL_METHOD("healthcare service referral method", Constants.HEALTHCARE_SERVICE_REFERRAL_METHOD_PATH),
    CARE_TEAM_CATEGORY("care team category", Constants.CARE_TEAM_CATEGORY_PATH),
    CARE_TEAM_REASON_CODE("care team reason", Constants.CARE_TEAM_REASON_CODE),
    CARE_TEAM_STATUS("care team status", Constants.CARE_TEAM_STATUS_PATH),
    PARTICIPANT_ROLE("participant role", Constants.PARTICIPANT_ROLE_PATH),
    PUBLICATION_STATUS("publication status", Constants.PUBLICATION_STATUS_PATH),
    DEFINITION_TOPIC("definition topic", Constants.DEFINITION_TOPIC_PATH),
    RESOURCE_TYPE("resource type", Constants.RESOURCE_TYPE_PATH),
    ACTION_PARTICIPATION_TYPE("action participation type", Constants.ACTION_PARTICIPATION_TYPE_PATH),
    RELATED_PERSON_PATIENT_RELATIONSHIPTYPES("related person patient relationship type", Constants.RELATED_PERSON_PATIENT_RELATIONSHIP_PATH),
    TASK_STATUS("task status", Constants.TASK_STATUS_PATH),
    REQUEST_PRIORITY("request priority", Constants.REQUEST_PRIORITY_PATH),
    TASK_PERFORMER_TYPE("task performer type", Constants.TASK_PERFORMER_TYPE_PATH),
    REQUEST_INTENT("request intent", Constants.REQUEST_INTENT_PATH),
    ACTIVITY_DEFINITION_RELATED_ARTIFACT_TYPES("activity definition related artifact type", Constants.ACTIVITY_DEFINITION_RELATED_ARTIFACT_TYPE_PATH),
    COMMUNICATION_STATUS("communication event status", Constants.COMMUNICATION_STATUS_PATH),
    COMMUNICATION_CATEGORY("communication category", Constants.COMMUNICATION_CATEGORY_PATH),
    COMMUNICATION_NOT_DONE_REASON("communication not done reason", Constants.COMMUNICATION_NOT_DONE_REASON_PATH),
    COMMUNICATION_MEDIUM("communication medium", Constants.COMMUNICATION_MEDIUM_PATH),
    APPOINTMENT_TYPE("appointment type", Constants.APPOINTMENT_TYPE_PATH),
    APPOINTMENT_STATUS("appointment status", Constants.APPOINTMENT_STATUS_PATH),
    PARTICIPATION_TYPE("participation type", Constants.PARTICIPATION_TYPE_PATH),
    PARTICIPATION_STATUS("participation status", Constants.PARTICIPATION_STATUS_PATH),
    PARTICIPANT_REQUIRED("participant required", Constants.PARTICIPANT_REQUIRED_PATH),
    PROVIDER_ROLE("provider role", Constants.PROVIDER_ROLE_PATH),
    PROVIDER_SPECIALTY("provider specialty", Constants.PROVIDER_SPECIALTY_PATH),
    FLAG_STATUS("flag status", Constants.FLAG_STATUS_PATH),
    FLAG_CATEGORY("flag category", Constants.FLAG_CATEGORY_PATH),
    CONSENT_STATE_CODE("consent state code", Constants.CONSENT_STATE_PATH),
    CONSENT_CATEGORY("consent category", Constants.CONSENT_CATEGORY_PATH),
    CONSENT_SECURITY_ROLE("security role", Constants.CONSENT_SECURITY_ROLE_PATH),
    CONSENT_ACTION("consent action", Constants.CONSENT_ACTION_PATH),
    PURPOSE_OF_USE("consent purpose of use", Constants.PURPOSE_OF_USE_PATH),
    SIMPLE_LANGUAGE("simple language",Constants.SIMPLE_LANGUAGE);

    private final String type;
    private final String urlPath;

    LookupPathUrls(String type, String urlPath) {
        this.type = type;
        this.urlPath = urlPath;
    }

    public String getType() {
        return type;
    }

    public String getUrlPath() {
        return urlPath;
    }

    // Todo: Need to clean up unused/unnecessary constant variables and rename PROVIDER_ROLE_PATH to PRACTITIONER_ROLE_PATH and
    private static class Constants {
        static final String STATE_PATH = "/ValueSet/usps-state/";
        static final String IDENTIFIER_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/identifier-type";
        static final String IDENTIFIER_USE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/identifier-use";
        static final String LOCATION_MODE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/location-mode";
        static final String LOCATION_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/location-status";
        static final String LOCATION_PHYSICAL_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/location-physical-type";
        static final String ADDRESS_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/address-type";
        static final String ADDRESS_USE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/address-use";
        static final String TELECOM_USE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/contact-point-use";
        static final String TELECOM_SYSTEM_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/contact-point-system";
        static final String PRACTITIONER_ROLE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/practitioner-role";
        static final String BIRTH_SEX_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/us/core/ValueSet/us-core-birthsex";
        static final String HEALTHCARE_SERVICE_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/service-type";
        static final String HEALTHCARE_SERVICE_CATEGORY_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/service-category";
        static final String HEALTHCARE_SERVICE_SPECIALITY_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/c80-practice-codes";
        static final String HEALTHCARE_SERVICE_SPECIALITY_2_PATH = "/ValueSet/practice-setting/";
        static final String HEALTHCARE_SERVICE_REFERRAL_METHOD_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/service-referral-method";
        static final String CARE_TEAM_CATEGORY_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/care-team-category";
        static final String CARE_TEAM_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/care-team-status";
        static final String CARE_TEAM_REASON_CODE = "/ValueSet/clinical-findings";
        static final String PARTICIPANT_ROLE_PATH = "/ValueSet/ocp-careteam-role";
        static final String PUBLICATION_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/publication-status";
        static final String DEFINITION_TOPIC_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/definition-topic";
        static final String RESOURCE_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/resource-types";
        static final String ACTION_PARTICIPATION_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/action-participant-type";
        static final String RELATED_PERSON_PATIENT_RELATIONSHIP_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype";
        static final String TASK_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/task-status";
        static final String REQUEST_PRIORITY_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/request-priority";
        static final String TASK_PERFORMER_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/task-performer-type";
        static final String REQUEST_INTENT_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/request-intent";
        static final String ACTIVITY_DEFINITION_RELATED_ARTIFACT_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/related-artifact-type";
        static final String COMMUNICATION_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/event-status";
        static final String COMMUNICATION_CATEGORY_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/communication-category";
        static final String COMMUNICATION_NOT_DONE_REASON_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/communication-not-done-reason";
        static final String COMMUNICATION_MEDIUM_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/v3-ParticipationMode";
        static final String APPOINTMENT_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/v2-0276";
        static final String APPOINTMENT_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/appointmentstatus";
        static final String PARTICIPATION_TYPE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/v3-ParticipationType";
        static final String PARTICIPATION_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/participationstatus";
        static final String PARTICIPANT_REQUIRED_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/participantrequired";
        static final String PROVIDER_ROLE_PATH = "/ValueSet/ocp-practitioner-role";
        static final String PROVIDER_SPECIALTY_PATH = "/ValueSet/ocp-provider-specialty";
        static final String FLAG_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/flag-status";
        static final String FLAG_CATEGORY_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/flag-category";
        static final String CONSENT_STATE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/consent-state-codes";
        static final String CONSENT_CATEGORY_PATH = "/ValueSet/consent-category";
        static final String CONSENT_SECURITY_ROLE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/security-role-type";
        static final String CONSENT_ACTION_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/consent-action";
        static final String PURPOSE_OF_USE_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/v3-ActReason";
        static final String SIMPLE_LANGUAGE = "/ValueSet/simple-language";
    }
}

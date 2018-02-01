package gov.samhsa.ocp.ocpfis.service.dto;

public enum LookupPathUrls {

    US_STATE("state", Constatns.STATE_PATH),
    IDENTIFIER_TYPE("identifier type",Constatns.IDENTIFIER_TYPE_PATH),
    IDENTIFIER_USE("identifier use", Constatns.IDENTIFIER_USE_PATH),
    LOCATION_MODE("location mode", Constatns.LOCATION_MODE_PATH),
    LOCATION_STATUS("location status", Constatns.LOCATION_STATUS_PATH),
    LOCATION_PHYSICAL_TYPE("location physical type", Constatns.LOCATION_PHYSICAL_TYPE_PATH),
    ADDRESS_TYPE("address type", Constatns.ADDRESS_TYPE_PATH),
    ADDRESS_USE("address use", Constatns.ADDRESS_USE_PATH),
    TELECOM_USE("telecom use", Constatns.TELECOM_USE_PATH),
    TELECOM_SYSTEM("telecom system", Constatns.TELECOM_SYSTEM_PATH),
    PRACTITIONER_ROLE("practitioner role", Constatns.PRACTITIONER_ROLE_PATH),
    BIRTH_SEX("birth sex", Constatns.BIRTH_SEX_PATH),
    HEALTHCARE_SERVICE_TYPE("helathcare service type", Constatns.HEALTHCARE_SERVICE_TYPE_PATH),
    HEALTHCARE_SERVICE_CATEGORY("healthcare service category", Constatns.HEALTHCARE_SERVICE_CATEGORY_PATH),
    CARE_TEAM_CATEGORY("care team category", Constatns.CARE_TEAM_CATEGORY_PATH),
    CARE_TEAM_STATUS("care team status", Constatns.CARE_TEAM_STATUS_PATH),
    PARTICIPANT_ROLE("participant role", Constatns.PARTICIPANT_ROLE_PATH);

    private final String type;
    private final String urlPath;

    LookupPathUrls(String type, String urlPath){
        this.type = type;
        this.urlPath = urlPath;
    }

    public String getType() {
        return type;
    }

    public String getUrlPath() {
        return urlPath;
    }

    private static class Constatns{

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
        static final String HEALTHCARE_SERVICE_CATEGORY_PATH =  "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/service-category";
        static final String CARE_TEAM_CATEGORY_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/care-team-category";
        static final String CARE_TEAM_STATUS_PATH = "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/care-team-status";
        static final String PARTICIPANT_ROLE_PATH = "/ValueSet/$expand?url=" + "http://hl7.org/fhir/ValueSet/participant-role";
    }

}

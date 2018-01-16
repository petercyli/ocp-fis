package gov.samhsa.ocp.ocpfis.service;

public class LocationInfoEnum {
    public enum LocationStatus{
        //All codes from system http://hl7.org/fhir/location-status
        ACTIVE, INACTIVE, SUSPENDED
    }

    public enum LocationAddressUse{
        //All codes from system http://hl7.org/fhir/address-use
        HOME, WORK, TEMP, OLD
    }

    public enum LocationTelecomUse{
        //All codes from system http://hl7.org/fhir/contact-point-use
        HOME, WORK, TEMP, OLD, MOBILE
    }

    public enum LocationTelecomSystem{
        //All codes from system http://hl7.org/fhir/contact-point-system
        PHONE, FAX, EMAIL, PAGER, URL, SMS, OTHER
    }


}

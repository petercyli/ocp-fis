package gov.samhsa.ocp.ocpfis.util;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.UriType;

import java.util.List;

@Slf4j
public class FhirProfileUtil {

    public static void setAppointmentProfileMetaData(IGenericClient fhirClient, Appointment appointment) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.Appointment.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            appointment.setMeta(meta);
        }
    }

    public static void setCareTeamProfileMetaData(IGenericClient fhirClient, CareTeam careTeam) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.CareTeam.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            careTeam.setMeta(meta);
        }
    }

    public static void setRelatedPersonProfileMetaData(IGenericClient fhirClient, CareTeam careTeam) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.RelatedPerson.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            careTeam.setMeta(meta);
        }
    }

    public static void setHealthCareServiceProfileMetaData(IGenericClient fhirClient, HealthcareService healthcareService){
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.HealthcareService.toString());
        if(uriList !=null && !uriList.isEmpty()){
            Meta meta = new Meta().setProfile(uriList);
            healthcareService.setMeta(meta);
        }
    }

    public static void setLocationProfileMetaData(IGenericClient fhirClient, Location fhirLocation) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.Location.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            fhirLocation.setMeta(meta);
        }
    }


    public static void setOrganizationProfileMetaData(IGenericClient fhirClient, Organization organization) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.Organization.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            organization.setMeta(meta);
        }
    }

    public static void setActivityDefinitionProfileMetaData(IGenericClient fhirClient, ActivityDefinition activityDefinition) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.ActivityDefinition.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            activityDefinition.setMeta(meta);
        }
    }

    public static void setPractitionerProfileMetaData(IGenericClient fhirClient, Practitioner practitioner) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.Practitioner.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            practitioner.setMeta(meta);
        }
    }

    public static void setPractitionerRoleProfileMetaData(IGenericClient fhirClient, PractitionerRole practitionerRole) {
        List<UriType> uriList = FhirUtil.getURIList(fhirClient, ResourceType.PractitionerRole.toString());
        if (uriList != null && !uriList.isEmpty()) {
            Meta meta = new Meta().setProfile(uriList);
            practitionerRole.setMeta(meta);
        }
    }
}

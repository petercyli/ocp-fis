package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.DateRangeDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
import gov.samhsa.ocp.ocpfis.service.dto.StatusBooleanValuesDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/lookups")
public class LookUpController {

    private final LookUpService lookUpService;

    public LookUpController(LookUpService lookUpService) {

        this.lookUpService = lookUpService;
    }

    @GetMapping("/date-ranges")
    public List<DateRangeDto> getDateRanges() {

        return lookUpService.getDateRanges();
    }

    @GetMapping("/usps-states")
    public List<ValueSetDto> getUspsStates() {

        return lookUpService.getUspsStates();
    }

    @GetMapping("/identifier-types")
    public List<ValueSetDto> getIdentifierTypes(@RequestParam(value = "resourceType") Optional<String> resourceType) {
        return lookUpService.getIdentifierTypes(resourceType);
    }

    @GetMapping("/identifier-systems")
    public List<IdentifierSystemDto> getIdentifierSystems(@RequestParam(value = "identifierTypeList") Optional<List<String>> identifierTypeList) {
        return lookUpService.getIdentifierSystems(identifierTypeList);
    }

    @GetMapping("/identifier-uses")
    public List<ValueSetDto> getIdentifierUses() {

        return lookUpService.getIdentifierUses();
    }

    @GetMapping("/location-modes")
    public List<ValueSetDto> getLocationModes() {

        return lookUpService.getLocationModes();
    }

    @GetMapping("/location-statuses")
    public List<ValueSetDto> getLocationStatuses() {

        return lookUpService.getLocationStatuses();
    }

    @GetMapping("/location-physical-types")
    public List<ValueSetDto> getLocationPhysicalTypes() {

        return lookUpService.getLocationPhysicalTypes();
    }

    @GetMapping("/address-types")
    public List<ValueSetDto> getAddressTypes() {

        return lookUpService.getAddressTypes();
    }

    @GetMapping("/address-uses")
    public List<ValueSetDto> getAddressUses() {

        return lookUpService.getAddressUses();
    }

    @GetMapping("/telecom-uses")
    public List<ValueSetDto> getTelecomUses() {

        return lookUpService.getTelecomUses();
    }

    @GetMapping("/telecom-systems")
    public List<ValueSetDto> getTelecomSystems() {

        return lookUpService.getTelecomSystems();
    }

    @GetMapping("/organization-statuses")
    public List<StatusBooleanValuesDto> getOrganizationStatuses() {

        return lookUpService.getOrganizationStatuses();
    }

    @GetMapping("/practitioner-roles")
    public List<ValueSetDto> getPractitionerRoles() {

        return lookUpService.getPractitionerRoles();
    }

    @GetMapping("/administrative-genders")
    public List<ValueSetDto> getAdministrativeGenders() {

        return lookUpService.getAdministrativeGenders();
    }

    @GetMapping("/us-core-races")
    public List<ValueSetDto> getUSCoreRaces() {

        return lookUpService.getUSCoreRace();
    }

    @GetMapping("/us-core-ethnicities")
    public List<ValueSetDto> getUSCoreEthnicities() {

        return lookUpService.getUSCoreEthnicity();
    }

    @GetMapping("/us-core-birthsexes")
    public List<ValueSetDto> getUSCoreBirthsexes() {

        return lookUpService.getUSCoreBirthSex();
    }

    @GetMapping("/languages")
    public List<ValueSetDto> getLanguages() {

        return lookUpService.getLanguages();
    }

    @GetMapping("/healthcare-service-types")
    public List<ValueSetDto> getHealthcareServiceTypes() {

        return lookUpService.getHealthcareServiceTypes();
    }

    @GetMapping("/healthcare-service-categories")
    public List<ValueSetDto> getHealthcareServiceCategories() {

        return lookUpService.getHealthcareServiceCategories();
    }

    @GetMapping("/healthcare-service-specialities")
    public List<ValueSetDto> getHealthcareServiceSpecialities() {

        return lookUpService.getHealthcareServiceSpecialities();
    }

    @GetMapping("/healthcare-service-referral-methods")
    public List<ValueSetDto> getHealthcareServiceReferralMethod() {

        return lookUpService.getHealthcareServiceReferralMethods();
    }

    @GetMapping("/healthcare-service-statuses")
    public List<StatusBooleanValuesDto> getHealthcareServiceStatuses() {

        return lookUpService.getHealthcareServiceStatuses();
    }

    @GetMapping("/care-team-categories")
    public List<ValueSetDto> getCareTeamCategories() {

        return lookUpService.getCareTeamCategories();
    }

    @GetMapping("/participant-types")
    public List<ValueSetDto> getParticipantTypes() {

        return lookUpService.getParticipantTypes();
    }

    @GetMapping("/care-team-statuses")
    public List<ValueSetDto> getCareTeamStatuses() {

        return lookUpService.getCareTeamStatuses();
    }

    @GetMapping("/care-team-reasons")
    public List<ValueSetDto> getCareTeamReasons() {

        return lookUpService.getCareTeamReasons();
    }

    @GetMapping("/participant-roles")
    public List<ValueSetDto> getParticipantRole() {

        return lookUpService.getParticipantRoles();
    }

    @GetMapping("/related-person-patient-relationship-types")
    public List<ValueSetDto> getRelatedPersonPatientRelationshipTypes() {

        return lookUpService.getRelatedPersonPatientRelationshipTypes();
    }

    @GetMapping("/publication-status")
    public List<ValueSetDto> getPublicationStatus() {

        return lookUpService.getPublicationStatus();
    }

    @GetMapping("/definition-topic")
    public List<ValueSetDto> getDefinitionTopic() {

        return lookUpService.getDefinitionTopic();
    }

    @GetMapping("/resource-type")
    public List<ValueSetDto> getResourceType() {

        return lookUpService.getResourceType();
    }

    @GetMapping("/action-participant-role")
    public List<ValueSetDto> getActionParticipantRole() {

        return lookUpService.getActionParticipantRole();
    }

    @GetMapping("/action-participant-type")
    public List<ValueSetDto> getActionParticipantType() {

        return lookUpService.getActionParticipantType();
    }

    @GetMapping("/task-status")
    public List<ValueSetDto> getTaskStatus() {

        return lookUpService.getTaskStatus();
    }

    @GetMapping("/request-priority")
    public List<ValueSetDto> getRequestPriority() {
        return lookUpService.getRequestPriority();
    }

    @GetMapping("/task-performer-type")
    public List<ValueSetDto> getTaskPerformerType() {

        return lookUpService.getTaskPerformerType();
    }

    @GetMapping("/request-intent")
    public List<ValueSetDto> getRequestIntent() {

        return lookUpService.getRequestIntent();
    }

    @GetMapping("/activity-definition-related-artifact-types")
    public List<ValueSetDto> getActivityDefinitionRelatedArtifactTypes() {

        return lookUpService.getActivityDefinitionRelatedArtifactTypes();
    }

    @GetMapping("/communication-statuses")
    public List<ValueSetDto> getCommunicationStatus() {
        return lookUpService.getCommunicationStatus();
    }

    @GetMapping("/communication-categories")
    public List<ValueSetDto> getCommunicationCategory() {

        return lookUpService.getCommunicationCategory();
    }

    @GetMapping("/communication-not-done-reasons")
    public List<ValueSetDto> getCommunicationNotDoneReason()
    {
        return lookUpService.getCommunicationNotDoneReason();
    }

    @GetMapping("/communication-mediums")
    public List<ValueSetDto> getCommunicationMedium() {

        return lookUpService.getCommunicationMedium();
    }

    @GetMapping("/appointment-statuses")
    public List<ValueSetDto> getAppointmentStatus() {

        return lookUpService.getAppointmentStatus();
    }

    @GetMapping("/appointment-types")
    public List<ValueSetDto> getAppointmentType() {

        return lookUpService.getAppointmentType();
    }

    @GetMapping("/appointment-participation-statuses")
    public List<ValueSetDto> getAppointmentParticipationStatus() {

        return lookUpService.getAppointmentParticipationStatus();
    }

    @GetMapping("/appointment-participant-types")
    public List<ValueSetDto> getAppointmentParticipantType() {

        return lookUpService.getAppointmentParticipantType();
    }

    @GetMapping("/appointment-participation-types")
    public List<ValueSetDto> getAppointmentParticipationType() {

        return lookUpService.getAppointmentParticipationType();
    }

    @GetMapping("/appointment-participant-required")
    public List<ValueSetDto> getAppointmentParticipantRequired() {

        return lookUpService.getAppointmentParticipantRequired();
    }

    @GetMapping("/provider-role")
    public List<ValueSetDto> getProviderRole() {

        return lookUpService.getProviderRole();
    }

    @GetMapping("/provider-specialty")
    public List<ValueSetDto> getProviderSpecialty() {

        return lookUpService.getProviderSpecialty();
    }

    @GetMapping("/flag-status")
    public List<ValueSetDto> getFlagStatus() {
        return lookUpService.getFlagStatus();
    }

    @GetMapping("/flag-category")
    public List<ValueSetDto> getFlagCategory(){
        return lookUpService.getFlagCategory();
    }
}

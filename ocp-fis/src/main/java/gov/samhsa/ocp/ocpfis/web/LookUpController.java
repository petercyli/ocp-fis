package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
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

    @GetMapping("/usps-states")
    public List<ValueSetDto> getUspsStates() {
        return lookUpService.getUspsStates();
    }

    /**
     * Determine identifier to use for a specific purpose
     * Eg: PRN , EN
     *
     * @return
     */
    @GetMapping("/identifier-types")
    public List<ValueSetDto> getIdentifierTypes(@RequestParam(value = "resourceType") Optional<String> resourceType) {
        return lookUpService.getIdentifierTypes(resourceType);
    }


    @GetMapping("/identifier-systems")
    public List<IdentifierSystemDto> getIdentifierSystems(@RequestParam(value = "identifierTypeList") Optional<List<String>> identifierTypeList) {
        return lookUpService.getIdentifierSystems(identifierTypeList);
    }

    /**
     * Identifies the purpose for this identifier, if known
     * Eg: Usual, Official, Temp
     *
     * @return
     */
    @GetMapping("/identifier-uses")
    public List<ValueSetDto> getIdentifierUses() {
        return lookUpService.getIdentifierUses();
    }

    //LOCATION START

    /**
     * Indicates whether a resource instance represents a specific location or a class of locations
     * Eg: INSTANCE, KIND, NULL
     *
     * @return
     */
    @GetMapping("/location-modes")
    public List<ValueSetDto> getLocationModes() {
        return lookUpService.getLocationModes();
    }

    /**
     * general availability of the resource
     * Eg: ACTIVE, SUSPENDED, INACTIVE, NULL
     *
     * @return
     */
    @GetMapping("/location-statuses")
    public List<ValueSetDto> getLocationStatuses() {
        return lookUpService.getLocationStatuses();
    }

    /**
     * Physical form of the location
     * e.g. building, room, vehicle, road.
     */
    @GetMapping("/location-physical-types")
    public List<ValueSetDto> getLocationPhysicalTypes() {
        return lookUpService.getLocationPhysicalTypes();
    }


    //LOCATION END

    //ADDRESS and TELECOM START

    /**
     * The type of an address (physical / postal)
     * Eg: POSTAL, PHYSICAL, POSTAL & PHYSICAL, NULL
     *
     * @return
     */
    @GetMapping("/address-types")
    public List<ValueSetDto> getAddressTypes() {
        return lookUpService.getAddressTypes();
    }

    /**
     * The use of an address
     * Eg: HOME, WORK, TEMP, OLD, NULL
     *
     * @return
     */
    @GetMapping("/address-uses")
    public List<ValueSetDto> getAddressUses() {
        return lookUpService.getAddressUses();
    }

    /**
     * Identifies the purpose for the contact point
     * Eg: HOME, WORK, TEMP, OLD, MOBILE, NULL
     *
     * @return
     */
    @GetMapping("/telecom-uses")
    public List<ValueSetDto> getTelecomUses() {
        return lookUpService.getTelecomUses();
    }

    /**
     * Telecommunications form for contact point - what communications system is required to make use of the contact.
     * Eg: PHONE, FAX, EMAIL, PAGER, URL, SMS, OTHER, NULL
     *
     * @return
     */
    @GetMapping("/telecom-systems")
    public List<ValueSetDto> getTelecomSystems() {
        return lookUpService.getTelecomSystems();
    }

    //ADDRESS and TELECOM END


    @GetMapping("/organization-statuses")
    public List<StatusBooleanValuesDto> getOrganizationStatuses() {
        return lookUpService.getOrganizationStatuses();
    }

    /**
     * Gives Practitioner roles.
     * Eg: DOCTOR, NURSE, PHARMACIST, RESEARCHER, TEACHER, ICT
     */
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
    public List<ValueSetDto> getPublicationStatus(){
        return lookUpService.getPublicationStatus();
    }

    @GetMapping("/definition-topic")
    public List<ValueSetDto> getDefinitionTopic(){
        return lookUpService.getDefinitionTopic();
    }

    @GetMapping("/resource-type")
    public List<ValueSetDto> getResourceType(){
        return lookUpService.getResourceType();
    }

    @GetMapping("/action-participant-role")
    public List<ValueSetDto> getActionParticipantRole(){
        return lookUpService.getActionParticipantRole();
    }

    @GetMapping("/action-participant-type")
    public List<ValueSetDto> getActionParticipantType(){
        return lookUpService.getActionParticipantType();
    }

    @GetMapping("/task-status")
    public List<ValueSetDto> getTaskStatus(){
        return lookUpService.getTaskStatus();
    }

    @GetMapping("/request-priority")
    public List<ValueSetDto> getRequestPriority(){
        return lookUpService.getRequestPriority();
    }

    @GetMapping("/task-performer-type")
    public List<ValueSetDto> getTaskPerformerType(){
        return lookUpService.getTaskPerformerType();
    }

    @GetMapping("/request-intent")
    public List<ValueSetDto> getRequestIntent(){
        return lookUpService.getRequestIntent();
    }
}

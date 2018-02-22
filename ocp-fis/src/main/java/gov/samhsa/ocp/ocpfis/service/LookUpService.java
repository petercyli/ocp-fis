package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationStatusDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;

import java.util.List;
import java.util.Optional;

public interface LookUpService {
    List<ValueSetDto> getUspsStates();

    List<ValueSetDto> getIdentifierTypes(Optional<String> resourceType);

    List<IdentifierSystemDto> getIdentifierSystems(Optional<List<String>> identifierTypeList);

    List<ValueSetDto> getIdentifierUses();

    List<ValueSetDto> getLocationModes();

    List<ValueSetDto> getLocationStatuses();

    List<ValueSetDto> getLocationPhysicalTypes();

    List<ValueSetDto> getAddressTypes();

    List<ValueSetDto> getAddressUses();

    List<ValueSetDto> getTelecomUses();

    List<ValueSetDto> getTelecomSystems();

    List<ValueSetDto> getAdministrativeGenders();

    List<ValueSetDto> getUSCoreRace();

    List<ValueSetDto> getUSCoreEthnicity();

    List<ValueSetDto> getUSCoreBirthSex();

    List<ValueSetDto> getLanguages();

    List<OrganizationStatusDto> getOrganizationStatuses();

    List<ValueSetDto> getPractitionerRoles();

    List<ValueSetDto> getHealthcareServiceTypes();

    List<ValueSetDto> getHealthcareServiceCategories();

    List<ValueSetDto> getHealthcareServiceReferralMethods();

    List<ValueSetDto> getHealthcareServiceSpecialities();

    List<OrganizationStatusDto> getHealthcareServiceStatuses();

    List<ValueSetDto> getCareTeamCategories();

    List<ValueSetDto> getParticipantTypes();

    List<ValueSetDto> getCareTeamStatuses();

    List<ValueSetDto> getParticipantRoles();

    List<ValueSetDto> getCareTeamReasons();

    List<ValueSetDto> getPublicationStatus();

    List<ValueSetDto> getDefinitionTopic();

    List<ValueSetDto> getResourceType();

    List<ValueSetDto> getActionParticipantRole();

    List<ValueSetDto> getActionParticipantType();

    List<ValueSetDto> getRelatedPersonPatientRelationshipTypes();

    List<ValueSetDto> getTaskStatus();

    List<ValueSetDto> getRequestPriority();

    List<ValueSetDto> getTaskPerformerType();

    List<ValueSetDto> getRequestIntent();

}

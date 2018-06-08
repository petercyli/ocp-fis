package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.DateRangeDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
import gov.samhsa.ocp.ocpfis.service.dto.StatusBooleanValuesDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;

import java.util.List;
import java.util.Optional;

public interface LookUpService {
    List<DateRangeDto> getDateRanges();

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

    List<StatusBooleanValuesDto> getOrganizationStatuses();

    List<ValueSetDto> getPractitionerRoles();

    List<ValueSetDto> getHealthcareServiceTypes();

    List<ValueSetDto> getHealthcareServiceCategories();

    List<ValueSetDto> getHealthcareServiceReferralMethods();

    List<ValueSetDto> getHealthcareServiceSpecialities();

    List<StatusBooleanValuesDto> getHealthcareServiceStatuses();

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

    List<ValueSetDto> getActivityDefinitionRelatedArtifactTypes();

    List<ValueSetDto> getCommunicationStatus();

    List<ValueSetDto> getCommunicationCategory();

    List<ValueSetDto> getCommunicationNotDoneReason();

    List<ValueSetDto> getCommunicationMedium();

    List<ValueSetDto> getAppointmentStatus();

    List<ValueSetDto> getAppointmentType();

    List<ValueSetDto> getAppointmentParticipationStatus();

    List<ValueSetDto> getAppointmentParticipantType();

    List<ValueSetDto> getAppointmentParticipationType();

    List<ValueSetDto> getAppointmentParticipantRequired();

    List<ValueSetDto> getProviderRole();

    List<ValueSetDto> getProviderSpecialty();

    List<ValueSetDto> getFlagStatus();

    List<ValueSetDto> getFlagCategory();

    List<ValueSetDto> getConsentStateCodes();

    List<ValueSetDto> getConsentCategory();

    List<ValueSetDto> getSecurityRole();

    List<ValueSetDto> getConsentAction();

    List<ValueSetDto> getPurposeOfUse();

    List<ValueSetDto> getSecurityLabel();

    List<ValueSetDto> getPolicyholderRelationship();

    List<ValueSetDto> getFmStatus();
}

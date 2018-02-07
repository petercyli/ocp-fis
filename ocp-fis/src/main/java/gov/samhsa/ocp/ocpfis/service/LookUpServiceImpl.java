package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.IdentifierTypeEnum;
import gov.samhsa.ocp.ocpfis.domain.KnownIdentifierSystemEnum;
import gov.samhsa.ocp.ocpfis.domain.LanguageEnum;
import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
import gov.samhsa.ocp.ocpfis.service.dto.LookupPathUrls;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationStatusDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LookUpServiceImpl implements LookUpService {

    private final IGenericClient fhirClient;

    private final FisProperties fisProperties;

    public LookUpServiceImpl(IGenericClient fhirClient, FisProperties fisProperties) {
        this.fhirClient = fhirClient;
        this.fisProperties = fisProperties;
    }

    @Override
    public List<ValueSetDto> getUspsStates() {
        List<ValueSetDto> stateCodes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.US_STATE.getUrlPath(), LookupPathUrls.US_STATE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.US_STATE.getType())) {

            List<ValueSet.ConceptReferenceComponent> statesList = response.getCompose().getInclude().get(0).getConcept();
            statesList.forEach(state -> {
                ValueSetDto temp = new ValueSetDto();
                temp.setCode(state.getCode());
                temp.setDisplay(state.getDisplay());
                stateCodes.add(temp);
            });
        }
        log.info("Found " + stateCodes.size() + " states codes.");
        return stateCodes;
    }

    @Override
    public List<ValueSetDto> getIdentifierTypes(Optional<String> resourceType) {
        List<ValueSetDto> identifierTypes = new ArrayList<>();
        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = new ArrayList<>();

        final List<String> allowedLocationIdentifierTypes = Arrays.asList("EN", "TAX", "NIIP", "PRN");
        final List<String> allowedOrganizationIdentifierTypes = Arrays.asList("EN", "TAX", "NIIP", "PRN");
        final List<String> allowedPatientIdentifierTypes = Arrays.asList("DL", "PPN", "TAX", "MR", "DR", "SB");
        final List<String> allowedPractitionerIdentifierTypes = Arrays.asList("PRN", "TAX", "MD", "SB");

        ValueSet response = getValueSets(LookupPathUrls.IDENTIFIER_TYPE.getUrlPath(), LookupPathUrls.IDENTIFIER_TYPE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.IDENTIFIER_TYPE.getType())) {
            valueSetList = response.getExpansion().getContains();
        }

        if (resourceType.isPresent() && (resourceType.get().trim().equalsIgnoreCase(Enumerations.ResourceType.LOCATION.name()))) {
            log.info("Fetching IdentifierTypes for resource = " + resourceType.get().trim());
            for (ValueSet.ValueSetExpansionContainsComponent type : valueSetList) {
                if (allowedLocationIdentifierTypes.contains(type.getCode().toUpperCase())) {
                    identifierTypes.add(convertIdentifierTypeToValueSetDto(type));
                }
            }
        } else if (resourceType.isPresent() && (resourceType.get().trim().equalsIgnoreCase(Enumerations.ResourceType.ORGANIZATION.name()))) {
            log.info("Fetching IdentifierTypes for resource = " + resourceType.get().trim());
            for (ValueSet.ValueSetExpansionContainsComponent type : valueSetList) {
                if (allowedOrganizationIdentifierTypes.contains(type.getCode().toUpperCase())) {
                    identifierTypes.add(convertIdentifierTypeToValueSetDto(type));
                }
            }
        } else if (resourceType.isPresent() && (resourceType.get().trim().equalsIgnoreCase(Enumerations.ResourceType.PATIENT.name()))) {
            log.info("Fetching IdentifierTypes for resource = " + resourceType.get().trim());
            for (ValueSet.ValueSetExpansionContainsComponent type : valueSetList) {
                if (allowedPatientIdentifierTypes.contains(type.getCode().toUpperCase())) {
                    identifierTypes.add(convertIdentifierTypeToValueSetDto(type));
                }
            }
        } else if (resourceType.isPresent() && (resourceType.get().trim().equalsIgnoreCase(Enumerations.ResourceType.PRACTITIONER.name()))) {
            log.info("Fetching IdentifierTypes for resource = " + resourceType.get().trim());
            for (ValueSet.ValueSetExpansionContainsComponent type : valueSetList) {
                if (allowedPractitionerIdentifierTypes.contains(type.getCode().toUpperCase())) {
                    identifierTypes.add(convertIdentifierTypeToValueSetDto(type));
                }
            }
        } else {
            log.info("Fetching ALL IdentifierTypes");
            identifierTypes = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }

        log.info("Found " + identifierTypes.size() + " identifier types.");
        return identifierTypes;
    }

    @Override
    public List<IdentifierSystemDto> getIdentifierSystems(Optional<List<String>> identifierTypeList) {
        // No FHIR-API or ENUMS available. Creating our own ENUMS instead
        List<IdentifierSystemDto> identifierSystemList = new ArrayList<>();
        List<KnownIdentifierSystemEnum> identifierSystemsByIdentifierTypeEnum = new ArrayList<>();

        if (!identifierTypeList.isPresent() || identifierTypeList.get().size() == 0) {
            log.info("Fetching ALL IdentifierSystems");
            identifierSystemsByIdentifierTypeEnum = Arrays.asList(KnownIdentifierSystemEnum.values());
        } else {
            log.info("Fetching IdentifierSystems for identifierType(s): ");
            identifierTypeList.get().forEach(log::info);

            for (String tempIdentifierType : identifierTypeList.get()) {
                List<KnownIdentifierSystemEnum> tempList = KnownIdentifierSystemEnum.identifierSystemsByIdentifierTypeEnum(IdentifierTypeEnum.valueOf(tempIdentifierType.toUpperCase()));
                identifierSystemsByIdentifierTypeEnum.addAll(tempList);
            }
        }

        if (identifierSystemsByIdentifierTypeEnum.size() < 1) {
            log.error("No Identifier Systems found");
            throw new ResourceNotFoundException("Query was successful, but found no identifier systems found in the configured FHIR server");
        }
        identifierSystemsByIdentifierTypeEnum.forEach(system -> {
            IdentifierSystemDto temp = new IdentifierSystemDto();
            temp.setDisplay(system.getDisplay());
            temp.setOid(system.getOid());
            temp.setUri(system.getUri());
            identifierSystemList.add(temp);
        });
        log.info("Found " + identifierSystemList.size() + " identifier systems.");
        return identifierSystemList;
    }

    @Override
    public List<ValueSetDto> getIdentifierUses() {
        List<ValueSetDto> identifierUses = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.IDENTIFIER_USE.getUrlPath(), LookupPathUrls.IDENTIFIER_USE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.IDENTIFIER_USE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            identifierUses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }

        log.info("Found " + identifierUses.size() + " identifier use codes.");
        return identifierUses;
    }

    @Override
    public List<ValueSetDto> getLocationModes() {
        List<ValueSetDto> locationModes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.LOCATION_MODE.getUrlPath(), LookupPathUrls.LOCATION_MODE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.LOCATION_MODE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            locationModes = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }

        log.info("Found " + locationModes.size() + " location modes.");
        return locationModes;
    }

    @Override
    public List<ValueSetDto> getLocationStatuses() {
        List<ValueSetDto> locationStatuses = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.LOCATION_STATUS.getUrlPath(), LookupPathUrls.LOCATION_STATUS.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.LOCATION_STATUS.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            locationStatuses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + locationStatuses.size() + " location status codes.");
        return locationStatuses;
    }

    @Override
    public List<OrganizationStatusDto> getOrganizationStatuses() {
        List<OrganizationStatusDto> organizationStatuses = Arrays.asList(new OrganizationStatusDto(true, "Active"), new OrganizationStatusDto(false, "Inactive"));
        log.info("Fetching ALL Organization Active Status");
        return organizationStatuses;
    }

    @Override
    public List<ValueSetDto> getLocationPhysicalTypes() {
        List<ValueSetDto> physicalLocationTypes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.LOCATION_PHYSICAL_TYPE.getUrlPath(), LookupPathUrls.LOCATION_PHYSICAL_TYPE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.LOCATION_PHYSICAL_TYPE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            physicalLocationTypes = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + physicalLocationTypes.size() + " physical location type codes.");
        return physicalLocationTypes;

    }

    @Override
    public List<ValueSetDto> getAddressTypes() {
        List<ValueSetDto> addressTypes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.ADDRESS_TYPE.getUrlPath(), LookupPathUrls.ADDRESS_TYPE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.ADDRESS_TYPE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            addressTypes = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + addressTypes.size() + " address type codes.");
        return addressTypes;
    }

    @Override
    public List<ValueSetDto> getAddressUses() {
        List<ValueSetDto> addressUses = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.ADDRESS_USE.getUrlPath(), LookupPathUrls.ADDRESS_USE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.ADDRESS_USE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            addressUses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + addressUses.size() + " address use codes.");
        return addressUses;
    }

    @Override
    public List<ValueSetDto> getTelecomUses() {
        List<ValueSetDto> telecomUses = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.TELECOM_USE.getUrlPath(), LookupPathUrls.TELECOM_USE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.TELECOM_USE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            telecomUses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + telecomUses.size() + " telecom use codes.");
        return telecomUses;
    }

    @Override
    public List<ValueSetDto> getTelecomSystems() {
        List<ValueSetDto> telecomSystems = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.TELECOM_SYSTEM.getUrlPath(), LookupPathUrls.TELECOM_SYSTEM.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.TELECOM_SYSTEM.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            telecomSystems = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + telecomSystems.size() + " telecom system codes.");
        return telecomSystems;
    }

    @Override
    public List<ValueSetDto> getPractitionerRoles() {
        List<ValueSetDto> practitionerRoles = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.PRACTITIONER_ROLE.getUrlPath(), LookupPathUrls.PRACTITIONER_ROLE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.PRACTITIONER_ROLE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            practitionerRoles = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + practitionerRoles.size() + " telecom use codes.");
        return practitionerRoles;
    }

    @Override
    public List<ValueSetDto> getAdministrativeGenders() {
        List<Enumerations.AdministrativeGender> administrativeGenderEnums = Arrays.asList(Enumerations.AdministrativeGender.values());

        List<ValueSetDto> administrativeGenders = administrativeGenderEnums.stream().map(gender -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setDefinition(gender.getDefinition());
            temp.setDisplay(gender.getDisplay());
            temp.setSystem(gender.getSystem());
            temp.setCode(gender.toCode());
            return temp;
        }).collect(Collectors.toList());

        log.info("Found " + administrativeGenders.size() + " AdministrativeGenders.");
        return administrativeGenders;
    }

    @Override
    public List<ValueSetDto> getUSCoreRace() {
        List<ValueSetDto> usCoreRaces = new ArrayList<>();
        ValueSet response = null;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/us/core/ValueSet/omb-race-category";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();

            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

            usCoreRaces = valueSetList.stream().map(object -> {
                ValueSetDto temp = new ValueSetDto();
                temp.setSystem(object.getSystem());
                temp.setCode(object.getCode());
                temp.setDisplay(object.getDisplay());
                return temp;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.debug("Query was unsuccessful - Could not find any omb-race-category", e.getMessage());
        }

        if (response == null) {
            url = fisProperties.getFhir().getServerUrl() + "/ValueSet/omb-race-category";
            try {
                response = (ValueSet) fhirClient.search().byUrl(url).execute();

                List<ValueSet.ConceptSetComponent> valueSetList = response.getCompose().getInclude();

                usCoreRaces = valueSetList.stream().flatMap(obj -> obj.getConcept().stream()).map(object -> {
                    ValueSetDto temp = new ValueSetDto();
                    temp.setCode(object.getCode());
                    temp.setDisplay(object.getDisplay());
                    return temp;
                }).collect(Collectors.toList());


            } catch (ResourceNotFoundException e) {
                log.error("Query was unsuccessful - Could not find any omb-race-category", e.getMessage());
                throw new ResourceNotFoundException("Query was unsuccessful - Could not find any omb-race-category", e);
            }
        }


        return usCoreRaces;
    }

    @Override
    public List<ValueSetDto> getUSCoreEthnicity() {
        List<ValueSetDto> usCoreEthnicites = new ArrayList<>();
        ValueSet response = null;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/us/core/ValueSet/omb-ethnicity-category";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();

            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

            usCoreEthnicites = valueSetList.stream().map(object -> {
                ValueSetDto temp = new ValueSetDto();
                temp.setSystem(object.getSystem());
                temp.setCode(object.getCode());
                temp.setDisplay(object.getDisplay());
                return temp;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.debug("Query was unsuccessful - Could not find any omb-ethnicity-category", e.getMessage());
        }

        if (response == null) {
            url = fisProperties.getFhir().getServerUrl() + "/ValueSet/omb-ethnicity-category";
            try {
                response = (ValueSet) fhirClient.search().byUrl(url).execute();

                List<ValueSet.ConceptSetComponent> valueSetList = response.getCompose().getInclude();

                usCoreEthnicites = valueSetList.stream().flatMap(obj -> obj.getConcept().stream()).map(object -> {
                    ValueSetDto temp = new ValueSetDto();
                    temp.setCode(object.getCode());
                    temp.setDisplay(object.getDisplay());
                    return temp;
                }).collect(Collectors.toList());

            } catch (ResourceNotFoundException e) {
                log.error("Query was unsuccessful - Could not find any omb-ethnicity-category", e.getMessage());
                throw new ResourceNotFoundException("Query was unsuccessful - Could not find any omb-ethnicity-category", e);
            }
        }


        return usCoreEthnicites;

    }

    @Override
    public List<ValueSetDto> getUSCoreBirthSex() {
        List<ValueSetDto> usCoreBirthsexes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.BIRTH_SEX.getUrlPath(), LookupPathUrls.BIRTH_SEX.getType());

        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

        usCoreBirthsexes = valueSetList.stream().map(object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setSystem(object.getSystem());
            temp.setCode(object.getCode());
            temp.setDisplay(object.getDisplay());
            return temp;
        }).collect(Collectors.toList());

        return usCoreBirthsexes;

    }

    @Override
    public List<ValueSetDto> getLanguages() {
        //Use temporary list of enums
        //list to be provided by eversolve
        List<ValueSetDto> languageList = new ArrayList<>();
        List<LanguageEnum> languageEnums = Arrays.asList(LanguageEnum.values());

        languageList = languageEnums.stream().map(object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setCode(object.getCode());
            temp.setDisplay(object.getName());
            return temp;
        }).collect(Collectors.toList());

        return languageList;
    }

    @Override
    public List<ValueSetDto> getHealthcareServiceTypes() {
        List<ValueSetDto> healthCareServiceTypeCodes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.HEALTHCARE_SERVICE_TYPE.getUrlPath(), LookupPathUrls.HEALTHCARE_SERVICE_TYPE.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.HEALTHCARE_SERVICE_TYPE.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> healthCareServiceTypeList = response.getExpansion().getContains();
            healthCareServiceTypeList.forEach(type -> {
                healthCareServiceTypeCodes.add(convertIdentifierTypeToValueSetDto(type));
            });
        }
        log.info("Found " + healthCareServiceTypeCodes.size() + " health care service types.");
        return healthCareServiceTypeCodes;
    }

    @Override
    public List<ValueSetDto> getHealthcareServiceCategories() {
        List<ValueSetDto> healthCareServiceCategoryCodes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.HEALTHCARE_SERVICE_CATEGORY.getUrlPath(), LookupPathUrls.HEALTHCARE_SERVICE_CATEGORY.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.HEALTHCARE_SERVICE_CATEGORY.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> healthCareServiceCategoryList = response.getExpansion().getContains();
            healthCareServiceCategoryList.forEach(type -> {
                 healthCareServiceCategoryCodes.add(convertIdentifierTypeToValueSetDto(type));
            });
        }
        log.info("Found " + healthCareServiceCategoryCodes.size() + " health care service categories.");
        return healthCareServiceCategoryCodes;
    }

    @Override
    public List<ValueSetDto> getHealthcareServiceSpecialities() {
        List<ValueSetDto> healthCareServiceSpecialitiesCodes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY.getUrlPath(), LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY.getType());
        boolean isAvailable = isValueSetAvailableInServer(response, LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY.getType(), Boolean.FALSE);
        if (isAvailable) {
            List<ValueSet.ValueSetExpansionContainsComponent> healthCareServiceCategoryList = response.getExpansion().getContains();
            healthCareServiceSpecialitiesCodes = healthCareServiceCategoryList.stream().map(object -> {
                return convertIdentifierTypeToValueSetDto(object);
            }).collect(Collectors.toList());
        } else {
            // try with different url
            response = getValueSets(LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY_2.getUrlPath(), LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY_2.getType());
            if (isValueSetAvailableInServer(response, LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY_2.getType())) {
                List<ValueSet.ConceptSetComponent> valueSetList = response.getCompose().getInclude();
                healthCareServiceSpecialitiesCodes = valueSetList.stream().flatMap(obj -> obj.getConcept().stream()).map(object -> {
                    return convertIdentifierTypeToValueSetDto(object);
                }).collect(Collectors.toList());
            }
        }

        log.info("Found " + healthCareServiceSpecialitiesCodes.size() + " health care service specialities.");
        return healthCareServiceSpecialitiesCodes;
    }


    @Override
    public List<ValueSetDto> getHealthcareServiceReferralMethods() {
        List<ValueSetDto> healthCareServiceReferralMethodCodes = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.HEALTHCARE_SERVICE_REFERRAL_METHOD.getUrlPath(), LookupPathUrls.HEALTHCARE_SERVICE_REFERRAL_METHOD.getType());
        if (isValueSetAvailableInServer(response, LookupPathUrls.HEALTHCARE_SERVICE_REFERRAL_METHOD.getType())) {
            List<ValueSet.ValueSetExpansionContainsComponent> healthCareServiceCategoryList = response.getExpansion().getContains();
            healthCareServiceCategoryList.forEach(type -> {
                healthCareServiceReferralMethodCodes.add(convertIdentifierTypeToValueSetDto(type));
            });
        }
        log.info("Found " + healthCareServiceReferralMethodCodes.size() + " health care service referral methods.");
        return healthCareServiceReferralMethodCodes;
    }

    @Override
    public List<ValueSetDto> getCareTeamCategories() {
        List<ValueSetDto> careTeamCategory = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.CARE_TEAM_CATEGORY.getUrlPath(), LookupPathUrls.CARE_TEAM_CATEGORY.getType());
        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

        careTeamCategory = valueSetList.stream().map(object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setSystem(object.getSystem());
            temp.setCode(object.getCode());
            temp.setDisplay(object.getDisplay());
            return temp;
        }).collect(Collectors.toList());

        return careTeamCategory;
    }

    @Override
    public List<ValueSetDto> getCareTeamStatuses() {
        List<ValueSetDto> careTeamStatusList = new ArrayList<>();
        ValueSet response = getValueSets(LookupPathUrls.CARE_TEAM_STATUS.getUrlPath(), LookupPathUrls.CARE_TEAM_STATUS.getType());

        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

        careTeamStatusList = valueSetList.stream().map(object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setSystem(object.getSystem());
            temp.setCode(object.getCode());
            temp.setDisplay(object.getDisplay());
            return temp;
        }).collect(Collectors.toList());

        return careTeamStatusList;
    }

    @Override
    public List<ValueSetDto> getParticipantTypes() {
        List<ValueSetDto> participantTypeList = new ArrayList<>();
        List<ParticipantTypeEnum> participantTypeEnums = Arrays.asList(ParticipantTypeEnum.values());

        participantTypeList = participantTypeEnums.stream().map(object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setCode(object.getCode());
            temp.setDisplay(object.getName());
            return temp;
        }).collect(Collectors.toList());

        return participantTypeList;
    }

    @Override
    public List<ValueSetDto> getParticipantRoles() {

        ValueSet response = getValueSets(LookupPathUrls.PARTICIPANT_ROLE.getUrlPath(), LookupPathUrls.PARTICIPANT_ROLE.getType());


        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

        return valueSetList.stream().map(convertToValueSetDto()).collect(Collectors.toList());
    }

    private Function<ValueSet.ValueSetExpansionContainsComponent, ValueSetDto> convertToValueSetDto() {
        return object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setSystem(object.getSystem());
            temp.setCode(object.getCode());
            temp.setDisplay(object.getDisplay());
            return temp;
        };
    }

    private ValueSetDto convertIdentifierTypeToValueSetDto(ValueSet.ConceptReferenceComponent object) {
        ValueSetDto temp = new ValueSetDto();
        temp.setCode(object.getCode());
        temp.setDisplay(object.getDisplay());
        return temp;
    }

    private ValueSetDto convertIdentifierTypeToValueSetDto(ValueSet.ValueSetExpansionContainsComponent identifierType) {
        ValueSetDto temp = new ValueSetDto();
        temp.setSystem(identifierType.getSystem());
        temp.setCode(identifierType.getCode());
        temp.setDisplay(identifierType.getDisplay());
        return temp;
    }

    private ValueSet getValueSets(String urlPath, String type) {

        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + urlPath;

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        } catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any " + type + " code", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any " + type + " code", e);
        }
        return response;
    }


    private boolean checkIfValueSetAvailableInServer(ValueSet response, String type) {
        boolean isAvailable = true;
        if (type.equalsIgnoreCase(LookupPathUrls.US_STATE.getType())
                || type.equalsIgnoreCase(LookupPathUrls.HEALTHCARE_SERVICE_SPECIALITY_2.getType())) {
            if (response == null || response.getCompose() == null ||
                    response.getCompose().getInclude() == null ||
                    response.getCompose().getInclude().size() < 1 ||
                    response.getCompose().getInclude().get(0).getConcept() == null ||
                    response.getCompose().getInclude().get(0).getConcept().size() < 1) {
                isAvailable = false;
            }
        } else {
            if (response == null ||
                    response.getExpansion() == null ||
                    response.getExpansion().getContains() == null ||
                    response.getExpansion().getContains().size() < 1) {
                isAvailable = false;
            }
        }
        return isAvailable;
    }

    private boolean isValueSetAvailableInServer(ValueSet response, String type) {
        return isValueSetAvailableInServer(response, type, Boolean.TRUE);
    }

    private boolean isValueSetAvailableInServer(ValueSet response, String type, boolean isThrow) {
        boolean isAvailable = checkIfValueSetAvailableInServer(response, type);
        if (!isAvailable && isThrow) {
            log.error("Query was successful, but found no " + type + " codes in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no " + type + " codes in the configured FHIR server");
        }
        return isAvailable;
    }
}


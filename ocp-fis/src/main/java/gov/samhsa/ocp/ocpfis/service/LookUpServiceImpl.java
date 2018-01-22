package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.IdentifierTypeEnum;
import gov.samhsa.ocp.ocpfis.domain.KnownIdentifierSystemEnum;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
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
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/usps-state/";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any state code", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any state code", e);
        }

        if (response == null || response.getCompose() == null ||
                response.getCompose().getInclude() == null ||
                response.getCompose().getInclude().size() < 1 ||
                response.getCompose().getInclude().get(0).getConcept() == null ||
                response.getCompose().getInclude().get(0).getConcept().size() < 1) {
            log.error("Query was successful, but found no state codes in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no state codes in the configured FHIR server");
        } else {

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
        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList;

        final List<String> allowedLocationIdentifierTypes = Arrays.asList("EN", "TAX", "NIIP", "PRN");

        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/identifier-type";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any identifier type", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any identifier type", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no identifier types in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no identifier types in the configured FHIR server");
        } else {
            valueSetList = response.getExpansion().getContains();
        }

        if (resourceType.isPresent() && (resourceType.get().trim().equalsIgnoreCase(Enumerations.ResourceType.LOCATION.name()))) {
            log.info("Fetching IdentifierTypes for resource = " + resourceType.get().trim());
            for (ValueSet.ValueSetExpansionContainsComponent type : valueSetList) {
                if (allowedLocationIdentifierTypes.contains(type.getCode().toUpperCase())) {
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
    public List<IdentifierSystemDto> getIdentifierSystems(Optional<String> identifierType) {
        // No FHIR-API or ENUMS available. Creating our own ENUMS instead
        List<IdentifierSystemDto> identifierSystemList = new ArrayList<>();
        List<KnownIdentifierSystemEnum> identifierSystemsByIdentifierTypeEnum;

        if (!identifierType.isPresent()) {
            log.info("Fetching ALL IdentifierSystems");
            identifierSystemsByIdentifierTypeEnum = Arrays.asList(KnownIdentifierSystemEnum.values());
        } else if (identifierType.get().trim().isEmpty()) {
            log.info("Fetching ALL IdentifierSystems");
            identifierSystemsByIdentifierTypeEnum = Arrays.asList(KnownIdentifierSystemEnum.values());
        } else {
            log.info("Fetching IdentifierSystems for identifierType  = " + identifierType.get().trim());
            identifierSystemsByIdentifierTypeEnum = KnownIdentifierSystemEnum.identifierSystemsByIdentifierTypeEnum(IdentifierTypeEnum.valueOf(identifierType.get().toUpperCase()));
        }

        if (identifierSystemsByIdentifierTypeEnum == null || identifierSystemsByIdentifierTypeEnum.size() < 1) {
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
        List<ValueSetDto> identifierUses;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/identifier-use";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any identifier use", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any identifier use", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no identifier use in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no identifier use in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            identifierUses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }

        log.info("Found " + identifierUses.size() + " identifier use codes.");
        return identifierUses;
    }

    @Override
    public List<ValueSetDto> getLocationModes() {
        List<ValueSetDto> locationModes;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/location-mode";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any location mode", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any location mode", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no location mode in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no location mode in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            locationModes = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }

        log.info("Found " + locationModes.size() + " location modes.");
        return locationModes;
    }

    @Override
    public List<ValueSetDto> getLocationStatuses() {
        List<ValueSetDto> locationStatuses;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/location-status";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any location status", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any location status", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no location status in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no location status in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            locationStatuses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + locationStatuses.size() + " location status codes.");
        return locationStatuses;
    }

    @Override
    public List<ValueSetDto> getLocationTypes() {
        List<ValueSetDto> locationTypes;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/location-physical-type";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any location type", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any location type", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no location type in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no location type in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            locationTypes = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + locationTypes.size() + " location type codes.");
        return locationTypes;

    }

    @Override
    public List<ValueSetDto> getAddressTypes() {
        List<ValueSetDto> addressTypes;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/address-type";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any address type", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any address type", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no address type in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no address type in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            addressTypes = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + addressTypes.size() + " address type codes.");
        return addressTypes;
    }

    @Override
    public List<ValueSetDto> getAddressUses() {
        List<ValueSetDto> addressUses;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/address-use";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any address use", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any address use", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no address use in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no address use in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            addressUses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + addressUses.size() + " address use codes.");
        return addressUses;
    }

    @Override
    public List<ValueSetDto> getTelecomUses() {
        List<ValueSetDto> telecomUses;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/contact-point-use";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any telecom use", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any telecom use", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no telecom use in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no telecom use in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            telecomUses = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + telecomUses.size() + " telecom use codes.");
        return telecomUses;
    }

    @Override
    public List<ValueSetDto> getTelecomSystems() {
        List<ValueSetDto> telecomSystems;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/ValueSet/contact-point-system";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        }
        catch (ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any telecom use", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any telecom use", e);
        }

        if (response == null ||
                response.getExpansion() == null ||
                response.getExpansion().getContains() == null ||
                response.getExpansion().getContains().size() < 1) {
            log.error("Query was successful, but found no telecom use in the configured FHIR server");
            throw new ResourceNotFoundException("Query was successful, but found no telecom use in the configured FHIR server");
        } else {
            List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();
            telecomSystems = valueSetList.stream().map(this::convertIdentifierTypeToValueSetDto).collect(Collectors.toList());
        }
        log.info("Found " + telecomSystems.size() + " telecom system codes.");
        return telecomSystems;
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
        List<ValueSetDto> usCoreRaces;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/us/core/ValueSet/omb-race-category";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        } catch (ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any omb-race-category", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any omb-race-category", e);
        }

        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

        usCoreRaces = valueSetList.stream().map(object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setSystem(object.getSystem());
            temp.setCode(object.getCode());
            temp.setDisplay(object.getDisplay());
            return temp;
        }).collect(Collectors.toList());

        return usCoreRaces;
    }

    @Override
    public List<ValueSetDto> getUSCoreEthnicity() {
        List<ValueSetDto> usCoreEthnicites;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/us/core/ValueSet/omb-ethnicity-category";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        } catch (ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any omb-ethnicity-category", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any omb-ethnicity-category", e);
        }

        List<ValueSet.ValueSetExpansionContainsComponent> valueSetList = response.getExpansion().getContains();

        usCoreEthnicites = valueSetList.stream().map(object -> {
            ValueSetDto temp = new ValueSetDto();
            temp.setSystem(object.getSystem());
            temp.setCode(object.getCode());
            temp.setDisplay(object.getDisplay());
            return temp;
        }).collect(Collectors.toList());

        return usCoreEthnicites;

    }

    @Override
    public List<ValueSetDto> getUSCoreBirthSex() {
        List<ValueSetDto> usCoreBirthsexes;
        ValueSet response;
        String url = fisProperties.getFhir().getServerUrl() + "/ValueSet/$expand?url=http://hl7.org/fhir/us/core/ValueSet/us-core-birthsex";

        try {
            response = (ValueSet) fhirClient.search().byUrl(url).execute();
        } catch (ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any us-core-birthsex", e.getMessage());
            throw new ResourceNotFoundException("Query was unsuccessful - Could not find any us-core-birthsex", e);
        }

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

    private ValueSetDto convertIdentifierTypeToValueSetDto(ValueSet.ValueSetExpansionContainsComponent identifierType) {
        ValueSetDto temp = new ValueSetDto();
        temp.setSystem(identifierType.getSystem());
        temp.setCode(identifierType.getCode());
        temp.setDisplay(identifierType.getDisplay());
        return temp;
    }


}

package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFound;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LookUpServiceImpl implements LookUpService {
    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FisProperties fisProperties;

    public LookUpServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
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
        catch (ResourceNotFoundException e) {
            log.error("Query was unsuccessful - Could not find any state code", e.getMessage());
            throw new ResourceNotFound("Query was unsuccessful - Could not find any state code", e);
        }

        if (response == null || response.getCompose() == null ||
                response.getCompose().getInclude() == null ||
                response.getCompose().getInclude().size() < 1 ||
                response.getCompose().getInclude().get(0).getConcept() == null ||
                response.getCompose().getInclude().get(0).getConcept().size() < 1){
            log.error("Query was successful, but found no state codes in the configured FHIR server");
            throw new ResourceNotFound("Query was successful, but found no state codes in the configured FHIR server");
        } else{

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
    public List<ValueSetDto> getLocationModes() {
        List<ValueSetDto> locationModes = new ArrayList<>();
        Location.LocationMode modesArray[]  = Location.LocationMode.values();

        for (Location.LocationMode locationMode : modesArray) {
            ValueSetDto temp = new ValueSetDto();
            temp.setDisplay(locationMode.toCode());
            temp.setSystem(locationMode.getSystem());
            temp.setSystem(locationMode.getDefinition());
            temp.setSystem(locationMode.getDisplay());
            locationModes.add(temp);
        }
        log.info("Found " + locationModes.size() + " location mode codes.");
        return locationModes;
    }

    @Override
    public List<ValueSetDto> getLocationStatuses() {
        List<ValueSetDto> locationStatuses = new ArrayList<>();
        Location.LocationStatus statusArray[]  = Location.LocationStatus.values();

        for (Location.LocationStatus locationStatus : statusArray) {
            ValueSetDto temp = new ValueSetDto();
            temp.setDisplay(locationStatus.toCode());
            temp.setSystem(locationStatus.getSystem());
            temp.setSystem(locationStatus.getDefinition());
            temp.setSystem(locationStatus.getDisplay());
            locationStatuses.add(temp);
        }
        log.info("Found " + locationStatuses.size() + " location status codes.");
        return locationStatuses;
    }

    @Override
    public List<ValueSetDto> getAddressTypes() {
        List<ValueSetDto> addressTypes = new ArrayList<>();
        Address.AddressType addrTypeArray[] = Address.AddressType.values();

        for (Address.AddressType addrType : addrTypeArray) {
            ValueSetDto temp = new ValueSetDto();
            temp.setDisplay(addrType.toCode());
            temp.setSystem(addrType.getSystem());
            temp.setSystem(addrType.getDefinition());
            temp.setSystem(addrType.getDisplay());
            addressTypes.add(temp);
        }
        log.info("Found " + addressTypes.size() + " address type codes.");
        return addressTypes;
    }

    @Override
    public List<ValueSetDto> getAddressUses() {
        List<ValueSetDto> addressUses = new ArrayList<>();
        Address.AddressUse addrUseArray[] = Address.AddressUse.values();

        for (Address.AddressUse addrUse : addrUseArray) {
            ValueSetDto temp = new ValueSetDto();
            temp.setDisplay(addrUse.toCode());
            temp.setSystem(addrUse.getSystem());
            temp.setSystem(addrUse.getDefinition());
            temp.setSystem(addrUse.getDisplay());
            addressUses.add(temp);
        }
        log.info("Found " + addressUses.size() + " address use codes.");
        return addressUses;
    }

    @Override
    public List<ValueSetDto> getTelecomUses() {
        List<ValueSetDto> telecomUses = new ArrayList<>();
        ContactPoint.ContactPointUse telecomUseArray[] = ContactPoint.ContactPointUse.values();

        for (ContactPoint.ContactPointUse telecomUse : telecomUseArray) {
            ValueSetDto temp = new ValueSetDto();
            temp.setDisplay(telecomUse.toCode());
            temp.setSystem(telecomUse.getSystem());
            temp.setSystem(telecomUse.getDefinition());
            temp.setSystem(telecomUse.getDisplay());
            telecomUses.add(temp);
        }
        log.info("Found " + telecomUses.size() + " telecom use codes.");
        return telecomUses;
    }

    @Override
    public List<ValueSetDto> getTelecomSystems() {
        List<ValueSetDto> telecomSystems = new ArrayList<>();
        ContactPoint.ContactPointSystem telecomSystemArray[] = ContactPoint.ContactPointSystem.values();

        for (ContactPoint.ContactPointSystem telecomUse : telecomSystemArray) {
            ValueSetDto temp = new ValueSetDto();
            temp.setDisplay(telecomUse.toCode());
            temp.setSystem(telecomUse.getSystem());
            temp.setSystem(telecomUse.getDefinition());
            temp.setSystem(telecomUse.getDisplay());
            telecomSystems.add(temp);
        }
        log.info("Found " + telecomSystems.size() + " telecom system codes.");
        return telecomSystems;
    }
}

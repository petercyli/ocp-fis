package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;

import java.util.List;
import java.util.Optional;

public interface LookUpService {
    List<ValueSetDto> getUspsStates();
    List<IdentifierSystemDto> getIdentifierSystems(Optional<String> identifierType);
    List<ValueSetDto> getIdentifierUses();

    List<ValueSetDto> getLocationIdentifierTypes();
    List<ValueSetDto> getLocationModes();
    List<ValueSetDto> getLocationStatuses();
    List<ValueSetDto> getLocationTypes();

    List<ValueSetDto> getAddressTypes();
    List<ValueSetDto> getAddressUses();
    List<ValueSetDto> getTelecomUses();
    List<ValueSetDto> getTelecomSystems();
}

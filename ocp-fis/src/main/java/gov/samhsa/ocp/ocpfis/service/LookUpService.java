package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;

import java.util.List;

public interface LookUpService {
    List<ValueSetDto> getUspsStates();
    List<ValueSetDto> getIdentifierTypes();
    List<ValueSetDto> getIdentifierUses();

    List<ValueSetDto> getLocationModes();
    List<ValueSetDto> getLocationStatuses();
    List<ValueSetDto> getLocationTypes();

    List<ValueSetDto> getAddressTypes();
    List<ValueSetDto> getAddressUses();
    List<ValueSetDto> getTelecomUses();
    List<ValueSetDto> getTelecomSystems();
}

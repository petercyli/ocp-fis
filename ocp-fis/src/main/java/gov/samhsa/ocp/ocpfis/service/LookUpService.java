package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;

import java.util.List;

public interface LookUpService {
    List<ValueSetDto> getUspsStates();
    List<ValueSetDto> getLocationModes();
    List<ValueSetDto> getLocationStatuses();

    List<ValueSetDto> getaddressTypes();
    List<ValueSetDto> getAddressUses();
    List<ValueSetDto> getTelecomUses();
    List<ValueSetDto> getTelecomSystems();
}

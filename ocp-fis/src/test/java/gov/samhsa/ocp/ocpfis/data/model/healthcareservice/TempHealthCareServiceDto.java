package gov.samhsa.ocp.ocpfis.data.model.healthcareservice;

import gov.samhsa.ocp.ocpfis.service.dto.NameLogicalIdIdentifiersDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;

import java.util.List;

public class TempHealthCareServiceDto {

    private String organizationId;
    private String organizationName;
    private String locationId;
    private String locationName;
    private boolean active;
    private ValueSetDto category;
    private List<String> programName;
    private List<TelecomDto> telecom;
    private List<ValueSetDto> type;
    private List<ValueSetDto> specialty;
    private List<ValueSetDto> referralMethod;
    private List<NameLogicalIdIdentifiersDto> location;
    private Boolean assignedToCurrentLocation;
}

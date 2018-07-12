package gov.samhsa.ocp.ocpfis.service;


import gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.StructureDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.ValueSetDto;

public interface StructureDefinitionService {

    void createStructureDefinition(StructureDefinitionDto structureDefinitionDto);
}

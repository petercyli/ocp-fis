package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.StructureDefinitionService;
import gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.StructureDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.ValueSetDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Slf4j
public class StructureDefinitionController {

    @Autowired
    private StructureDefinitionService structureDefinitionService;


    @PutMapping("/structure-definitions")
    @ResponseStatus(HttpStatus.OK)
    public void createStructureDefinition(@Valid @RequestBody StructureDefinitionDto structureDefinitionDto){
        log.info("Starting to load object with id : " + structureDefinitionDto.getId());
        structureDefinitionService.createStructureDefinition(structureDefinitionDto);
        log.info("Successfully created Structure Defintion with id : "+structureDefinitionDto.getId());
    }
}

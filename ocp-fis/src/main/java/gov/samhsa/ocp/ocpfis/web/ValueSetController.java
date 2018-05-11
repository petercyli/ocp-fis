package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.ValueSetService;
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
public class ValueSetController {

    @Autowired
    private ValueSetService valueSetService;

    /**
     * Created @PutMapping because an entry will still be created and
     * value of id in the json will be populated instead of a system generated id.
     *
     * @param valueSetDto
     */
    @PutMapping("/valuesets")
    @ResponseStatus(HttpStatus.OK)
    public void createValueSet(@Valid @RequestBody ValueSetDto valueSetDto) {
        log.info("Starting to load object with id : " + valueSetDto.getId());
        valueSetService.createValueSet(valueSetDto);
        log.info("Successfully created ValueSet with id : " + valueSetDto.getId());
    }

}

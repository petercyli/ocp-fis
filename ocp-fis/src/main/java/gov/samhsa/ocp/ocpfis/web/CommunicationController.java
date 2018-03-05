package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CommunicationService;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class CommunicationController {
    @Autowired
    private CommunicationService communicationService;

    @PostMapping("/communications")
    @ResponseStatus(HttpStatus.CREATED)
    public void createCommunication(@Valid @RequestBody CommunicationDto communicationDto){
        communicationService.createCommunication(communicationDto);
    }

    @PutMapping("/communications/{communicationId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateCommunication(@PathVariable String communicationId, @Valid @RequestBody CommunicationDto communicationDto){
        communicationService.updateCommunication(communicationId,communicationDto);
    }

}

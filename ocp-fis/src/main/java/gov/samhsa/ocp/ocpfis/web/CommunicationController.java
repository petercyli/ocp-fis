package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CommunicationService;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class CommunicationController {
    @Autowired
    private CommunicationService communicationService;

    @GetMapping("/communications/search")
    public PageDto<CommunicationDto> getCommunications(@RequestParam Optional<List<String>> statusList,
                                                       @RequestParam(value = "searchKey") String searchKey,
                                                       @RequestParam(value = "searchValue") String searchValue,
                                                       @RequestParam(value = "organization") Optional<String> organization,
                                                       @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                       @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return communicationService.getCommunications(statusList, searchKey, searchValue, organization, Optional.empty(), Optional.empty(), pageNumber, pageSize);
    }


    @PostMapping("/communications")
    @ResponseStatus(HttpStatus.CREATED)
    public void createCommunication(@Valid @RequestBody CommunicationDto communicationDto, @RequestParam(value = "loggedInUser") Optional<String> loggedInUser){
        communicationService.createCommunication(communicationDto, loggedInUser);
    }

    @PutMapping("/communications/{communicationId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateCommunication(@PathVariable String communicationId, @Valid @RequestBody CommunicationDto communicationDto, @RequestParam(value = "loggedInUser") Optional<String> loggedInUser){
        communicationService.updateCommunication(communicationId,communicationDto, loggedInUser);
    }

    @GetMapping("/communications")
    public PageDto<CommunicationDto> getCommunications(@RequestParam(value = "patient") String patient,
                                                       @RequestParam(value = "topic") String topic,
                                                       @RequestParam(value = "resourceType") String resourceType,
                                                       @RequestParam(value = "pageNumber") Optional<Integer> pageNumber,
                                                       @RequestParam(value = "pageSize") Optional<Integer> pageSize) {
        return communicationService.getCommunications(Optional.empty(), "Patient", patient, Optional.empty(), Optional.of(topic), Optional.of(resourceType), pageNumber, pageSize);
    }

}

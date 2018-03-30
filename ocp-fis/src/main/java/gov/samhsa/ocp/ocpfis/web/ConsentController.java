package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.ConsentService;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Optional;
@RestController
public class ConsentController  {
    @Autowired
    private ConsentService consentService;

    @GetMapping("/consents")
    public PageDto<ConsentDto> getConsents(@RequestParam(value = "patient") Optional<String> patient,
                                           @RequestParam(value = "fromActor") Optional<String> fromActor,
                                           @RequestParam(value = "toActor") Optional<String> toActor,
                                           @RequestParam(value = "generalDesignation") Optional<Boolean> generalDesignation,
                                           @RequestParam(value = "status") Optional<String> status,
                                           @RequestParam Optional<Integer> pageNumber,
                                           @RequestParam Optional<Integer> pageSize) {
        return consentService.getConsents(patient, fromActor, toActor, generalDesignation, status, pageNumber, pageSize);
    }

    @PostMapping("/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public void createConsent(@Valid @RequestBody ConsentDto consentDto) {
        consentService.createConsent(consentDto);
    }
}

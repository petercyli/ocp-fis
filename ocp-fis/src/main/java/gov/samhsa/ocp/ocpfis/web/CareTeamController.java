package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/care-teams")
public class CareTeamController {

    @Autowired
    private CareTeamService careTeamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createCareTeam(@Valid @RequestBody CareTeamDto careTeamDto) {
        careTeamService.createCareTeam(careTeamDto);
    }

    @PutMapping("/{careTeamId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateCareTeam(@PathVariable String careTeamId, @Valid @RequestBody CareTeamDto careTeamDto) {
        careTeamService.updateCareTeam(careTeamId, careTeamDto);
    }

    @GetMapping("/search")
    private PageDto<CareTeamDto> getCareTeams(@RequestParam Optional<List<String>> statusList, @RequestParam String searchType, @RequestParam String searchValue, @RequestParam Optional<Integer> pageNumber, @RequestParam Optional<Integer> pageSize) {
        return careTeamService.getCareTeams(statusList, searchType, searchValue, pageNumber, pageSize);
    }

    @GetMapping("/{careTeamId}")
    public CareTeamDto getCareTeamById(@PathVariable String careTeamId) {
        return careTeamService.getCareTeamById(careTeamId);
    }

    @GetMapping("/{careTeamId}/related-persons")
    public List<ParticipantDto> getRelatedPersonsForEdit(@PathVariable String careTeamId){
        return careTeamService.getRelatedPersonsByIdForEdit(careTeamId);
    }

    @GetMapping
    public PageDto<CareTeamDto> getCareTeamsByPatientAndOrganization(@RequestParam String patient,
                                                      @RequestParam Optional<String> organization,
                                                      @RequestParam Optional<List<String>> status,
                                                      @RequestParam Optional<Integer> pageNumber,
                                                      @RequestParam Optional<Integer> pageSize) {
        return careTeamService.getCareTeamsByPatientAndOrganization(patient, organization, status, pageNumber, pageSize);
    }

    @PutMapping("/{careTeamId}/add-relatedPerson")
    public void addRelatedPerson(@PathVariable String careTeamId, @Valid @RequestBody ParticipantDto participantDto){
        careTeamService.addRelatedPerson(careTeamId, participantDto);
    }

    @PutMapping("/{careTeamId}/remove-relatedPerson")
    public void removeRelatedPerson(@PathVariable String careTeamId, @Valid @RequestBody ParticipantDto participantDto){
        careTeamService.removeRelatedPerson(careTeamId,participantDto);
    }
}

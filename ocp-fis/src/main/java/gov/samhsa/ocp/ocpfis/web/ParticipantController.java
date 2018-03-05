package gov.samhsa.ocp.ocpfis.web;


import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.ParticipantService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantSearchDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final CareTeamService careTeamService;

    public ParticipantController(ParticipantService participantService, CareTeamService careTeamService) {
        this.participantService = participantService;
        this.careTeamService = careTeamService;
    }

    @GetMapping("/search")
    public PageDto<ParticipantSearchDto> getAllParticipants(@RequestParam(value = "patientId") String patientId,
                                                            @RequestParam(value = "member") ParticipantTypeEnum member,
                                                            @RequestParam(value = "value") String value,
                                                            @RequestParam(value = "showInActive", defaultValue = "false") Optional<Boolean> showInActive,
                                                            @RequestParam(value = "page", required = false) Optional<Integer> page,
                                                            @RequestParam(value = "size", required = false) Optional<Integer> size) {
        return participantService.getAllParticipants(patientId, member, value, showInActive, page, size);
    }

    @GetMapping
    public List<ReferenceDto> getCareTeamParticipants(@RequestParam(value = "patient") String patient,
                                                      @RequestParam(value = "roles") List<String> roles) {
        return careTeamService.getCareTeamParticipants(patient, roles);
    }

}

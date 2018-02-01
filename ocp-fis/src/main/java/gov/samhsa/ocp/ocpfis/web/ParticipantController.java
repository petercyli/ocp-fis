package gov.samhsa.ocp.ocpfis.web;


import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.ParticipantService;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @GetMapping("/search")
    public PageDto<ParticipantDto> getAllParticipants(@RequestParam(value = "member") ParticipantTypeEnum member,
                                                      @RequestParam(value = "value") String value,
                                                      @RequestParam(value = "showInActive", defaultValue = "false") Optional<Boolean> showInActive,
                                                      @RequestParam(value = "page", required = false) Optional<Integer> page,
                                                      @RequestParam(value = "size", required = false) Optional<Integer> size) {
        return participantService.getAllParticipants(member, value, showInActive, page, size);
    }

}

package gov.samhsa.ocp.ocpfis.web;


import gov.samhsa.ocp.ocpfis.service.ParticipantService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

}

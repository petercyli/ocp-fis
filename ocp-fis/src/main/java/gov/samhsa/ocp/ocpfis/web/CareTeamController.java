package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CareTeamController {

    private final CareTeamService careTeamService;

    public CareTeamController(CareTeamService careTeamService) {
        this.careTeamService = careTeamService;
    }
}

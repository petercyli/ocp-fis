package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/careteams")
public class CareTeamController {

    @Autowired
    private CareTeamService careTeamService;

    @PostMapping
    public void createCareTeam(@Valid @RequestBody CareTeamDto careTeamDto) {
        careTeamService.createCareTeam(careTeamDto);
    }

    @PutMapping("/{careTeamId}")
    public void updateCareTeam(@PathVariable String careTeamId, @Valid @RequestBody CareTeamDto careTeamDto) {
        careTeamService.updateCareTeam(careTeamId, careTeamDto);
    }

}
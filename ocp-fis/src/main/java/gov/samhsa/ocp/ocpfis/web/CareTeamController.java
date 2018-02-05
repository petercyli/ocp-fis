package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/careTeams")
public class CareTeamController {

    @Autowired
    private CareTeamService careTeamService;

    @PostMapping
    public void createCareTeam(@Valid @RequestBody CareTeamDto careTeamDto) {
        careTeamService.createCareTeam(careTeamDto);
    }

    @PutMapping
    public void updateCareTeam(@Valid @RequestBody CareTeamDto careTeamDto) {
        careTeamService.updateCareTeam(careTeamDto);
    }

    @GetMapping("/search")
    private PageDto<CareTeamDto> getCareTeams(@RequestParam Optional<List<String>> statusList, @RequestParam String searchType, @RequestParam String searchValue, @RequestParam Optional<Integer> pageNumber, @RequestParam Optional<Integer> pageSize) {
        return careTeamService.getCareTeam(statusList, searchType, searchValue, pageNumber, pageSize);
    }
}

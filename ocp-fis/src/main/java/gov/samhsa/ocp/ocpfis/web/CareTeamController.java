package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.CareTeamService;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/careTeams")
public class CareTeamController {

    private final CareTeamService careTeamService;

    public CareTeamController(CareTeamService careTeamService) {
        this.careTeamService = careTeamService;
    }

    @GetMapping("/search")
    private PageDto<CareTeamDto> getCareTeams(@RequestParam String searchType, @RequestParam String searchValue, @RequestParam Optional<String> showInactive, @RequestParam Optional<Integer> page, @RequestParam Optional<Integer> size){
        return careTeamService.getCareTeam(searchType,searchValue,showInactive,page,size);
    }
}

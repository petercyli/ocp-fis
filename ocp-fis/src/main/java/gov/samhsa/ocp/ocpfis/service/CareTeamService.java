package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.List;
import java.util.Optional;

public interface CareTeamService {

    void createCareTeam(CareTeamDto careTeamDto);

    void updateCareTeam(CareTeamDto careTeamDto);

    PageDto<CareTeamDto> getCareTeam(Optional<List<String>> statusList,String searchType, String searchValue, Optional<Integer> page, Optional<Integer> size);
}

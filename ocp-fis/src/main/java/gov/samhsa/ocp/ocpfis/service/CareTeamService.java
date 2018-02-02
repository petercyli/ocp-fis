package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.List;
import java.util.Optional;

public interface CareTeamService {

    void createCareTeam(CareTeamDto careTeamDto);

    void updateCareTeam(CareTeamDto careTeamDto);

    PageDto<CareTeamDto> getCareTeam(String searchType, String searchValue, Optional<String> showInactive,Optional<Integer> page, Optional<Integer> size);
}

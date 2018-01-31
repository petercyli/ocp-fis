package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;

public interface CareTeamService {

    void createCareTeam(CareTeamDto careTeamDto);

    void updateCareTeam(String careTeamId, CareTeamDto careTeamDto);
}

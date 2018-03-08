package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;
import java.util.Optional;

public interface CareTeamService {

    void createCareTeam(CareTeamDto careTeamDto);

    void updateCareTeam(String careTeamId, CareTeamDto careTeamDto);

    CareTeamDto getCareTeamById(String careTeamId);

    PageDto<CareTeamDto> getCareTeams(Optional<List<String>> statusList, String searchType, String searchValue, Optional<Integer> page, Optional<Integer> size);

    List<ReferenceDto> getCareTeamParticipants(String patient, Optional<List<String>> roles);

    List<CommunicationReferenceDto> getRecipientsByCommunicationId(String patient, String communication);
}

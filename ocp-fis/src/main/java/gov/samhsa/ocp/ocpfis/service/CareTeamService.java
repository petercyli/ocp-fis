package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;

import java.util.List;
import java.util.Optional;

public interface CareTeamService {

    void createCareTeam(CareTeamDto careTeamDto, Optional<String> loggedInUser);

    void updateCareTeam(String careTeamId, CareTeamDto careTeamDto, Optional<String> loggedInUser);

    CareTeamDto getCareTeamById(String careTeamId);

    PageDto<CareTeamDto> getCareTeams(Optional<List<String>> statusList, String searchType, String searchValue, Optional<Integer> page, Optional<Integer> size);

    List<ParticipantReferenceDto> getCareTeamParticipants(String patient, Optional<List<String>> roles, Optional<String> name, Optional<String> communication);

    List<ReferenceDto> getPatientsInCareTeamsByPractitioner(String practitioner);

    PageDto<CareTeamDto> getCareTeamsByPatientAndOrganization(String patient, Optional<String> organization, Optional<List<String>> status, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    void addRelatedPerson(String careTeamId, ParticipantDto participantDto);

    void removeRelatedPerson(String careTeamId, ParticipantDto participantDto);

    PageDto<ParticipantDto> getRelatedPersonsByIdForEdit(String careTeamId, Optional<String> name, Optional<Integer> pageNumber, Optional<Integer> pageSize);

    List<ReferenceDto> getParticipantMemberFromCareTeam(String patient);
}

package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.OutsideParticipant;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantSearchDto;

import java.util.List;
import java.util.Optional;

public interface ParticipantService {

    PageDto<ParticipantSearchDto> getAllParticipants(String patientId, ParticipantTypeEnum participantType, Optional<String> value, Optional<String> organization, Optional<Boolean> forCareTeam, Optional<Boolean> showInActive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll);


    List<OutsideParticipant> retrieveOutsideParticipants(String patient,
                                                         String participantType,
                                                         String name,
                                                         String organization,
                                                         Optional<Integer> page,
                                                         Optional<Integer> size,
                                                         Optional<Boolean> showAll);

}

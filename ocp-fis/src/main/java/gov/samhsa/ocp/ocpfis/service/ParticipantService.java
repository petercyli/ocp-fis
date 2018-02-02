package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantSearchDto;

import java.util.Optional;

public interface ParticipantService {

    PageDto<ParticipantSearchDto> getAllParticipants(ParticipantTypeEnum participantType, String value, Optional<Boolean> showInActive, Optional<Integer> page, Optional<Integer> size);

}

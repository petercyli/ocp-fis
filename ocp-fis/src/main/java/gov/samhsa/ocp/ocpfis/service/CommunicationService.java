package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.List;
import java.util.Optional;

public interface CommunicationService {
    PageDto<CommunicationDto> getCommunications(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<String> organization, Optional<String> topic, Optional<String> resourceType, Optional<Integer> pageNumber, Optional<Integer> pageSize);
    void createCommunication(CommunicationDto communicationDto, Optional<String> loggedInUser);
    void updateCommunication(String communicationId, CommunicationDto communicationDto, Optional<String> loggedInUser);
    List<String> getRecipientsByCommunicationId(String patient, String communicationId);
}

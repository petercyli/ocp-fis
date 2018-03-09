package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.List;
import java.util.Optional;

public interface CommunicationService {
    PageDto<CommunicationDto> getCommunications(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize);
    void createCommunication(CommunicationDto communicationDto);
    void updateCommunication(String communicationId, CommunicationDto communicationDto);
    List<String> getRecipientsByCommunicationId(String patient, String communicationId);
}

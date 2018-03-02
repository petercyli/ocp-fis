package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CommunicationDto;

public interface CommunicationService {
    void createCommunication(CommunicationDto communicationDto);
    void updateCommunication(String communicationId, CommunicationDto communicationDto);
}

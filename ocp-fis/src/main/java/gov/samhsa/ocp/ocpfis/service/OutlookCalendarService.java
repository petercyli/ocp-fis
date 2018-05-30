package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.CredentialDto;
import gov.samhsa.ocp.ocpfis.service.dto.OutlookCalendarDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutlookCalendarService {
    List<OutlookCalendarDto> getOutlookCalendarAppointments(String emailAddress, String password, Optional<LocalDateTime> start, Optional<LocalDateTime> end);
    void loginToOutlook(CredentialDto credentialDto);
}

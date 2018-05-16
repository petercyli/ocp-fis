package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.EwsCalendarDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EwsCalendarService {
    List<EwsCalendarDto> getEwsCalendarAppointments(String emailAddress, String password, Optional<LocalDateTime> start, Optional<LocalDateTime> end);
}

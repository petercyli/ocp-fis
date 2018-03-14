package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;

import java.util.List;
import java.util.Optional;

public interface AppointmentService {
    PageDto<AppointmentDto> getAppointments(Optional<List<String>> statusList, Optional<String>  searchKey, Optional<String>  searchValue, Optional<Boolean> sortByStartTimeAsc, Optional<Integer> pageNumber, Optional<Integer> pageSize);
    void createAppointment(AppointmentDto appointmentDto);
    void cancelAppointment(String appointmentId);
}

package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;

public interface AppointmentService {
    void createAppointment(AppointmentDto appointmentDto);
}

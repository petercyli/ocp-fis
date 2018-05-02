package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import org.hl7.fhir.dstu3.model.Appointment;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class AppointmentToAppointmentDtoConverter {

    private static final String PATIENT_ACTOR_REFERENCE = "Patient";
    private static final String DATE_TIME_FORMATTER_PATTERN_DATE = "MM/dd/yyyy";

    public static AppointmentDto map(Appointment appointment, Optional<String> requesterReference) {

        AppointmentDto appointmentDto = new AppointmentDto();

        appointmentDto.setLogicalId(appointment.getIdElement().getIdPart());

        if (appointment.hasStatus()) {
            appointmentDto.setStatusCode(appointment.getStatus().toCode());
        }

        if (appointment.hasAppointmentType()) {
            ValueSetDto type = FhirDtoUtil.convertCodeableConceptToValueSetDto(appointment.getAppointmentType());
            appointmentDto.setTypeCode(type.getCode());
        }

        if (appointment.hasDescription()) {
            appointmentDto.setDescription(appointment.getDescription());
        }

        if (appointment.hasParticipant()) {
            List<AppointmentParticipantDto> participantDtos = FhirDtoUtil.convertAppointmentParticipantListToAppointmentParticipantDtoList(appointment.getParticipant());
            appointmentDto.setParticipant(participantDtos);

            if (requesterReference.isPresent()) {
                String reference = requesterReference.get();
                participantDtos.forEach(
                        participant -> {
                            if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) && participant.getParticipationTypeCode().equalsIgnoreCase("AUT")) {
                                appointmentDto.setCanCancel(true);
                            }
                            if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) && !participant.getParticipationStatusCode().equalsIgnoreCase("accepted")) {
                                appointmentDto.setCanAccept(true);
                            }
                            if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) && !participant.getParticipationStatusCode().equalsIgnoreCase("declined")) {
                                appointmentDto.setCanDecline(true);
                            }
                        }
                );
            }
        }

        if (!appointmentDto.getParticipant().isEmpty()) {
            List<String> actorNames = appointmentDto.getParticipant().stream()
                    .filter(participant -> participant.getActorReference() != null && participant.getActorReference().toUpperCase().contains(PATIENT_ACTOR_REFERENCE.toUpperCase()))
                    .map(AppointmentParticipantDto::getActorName)
                    .collect(toList());
            if (!actorNames.isEmpty())
                appointmentDto.setPatientName(actorNames.get(0));
        }

        String duration = "";

        if (appointment.hasStart()) {
            appointmentDto.setStart(DateUtil.convertDateToLocalDateTime(appointment.getStart()));
            DateTimeFormatter startFormatterDate = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER_PATTERN_DATE);
            String formattedDate = appointmentDto.getStart().format(startFormatterDate);
            appointmentDto.setAppointmentDate(formattedDate);

            duration = duration + DateUtil.convertLocalDateTimeToHumanReadableFormat(appointmentDto.getStart());
        }

        if (appointment.hasEnd()) {
            appointmentDto.setEnd(DateUtil.convertDateToLocalDateTime(appointment.getEnd()));

            duration = duration + " - " + DateUtil.convertLocalDateTimeToHumanReadableFormat(appointmentDto.getEnd());
        }

        if (appointment.hasCreated()) {
            appointmentDto.setCreated(DateUtil.convertDateToLocalDateTime(appointment.getCreated()));
        }
        appointmentDto.setAppointmentDuration(duration);

        return appointmentDto;
    }

}

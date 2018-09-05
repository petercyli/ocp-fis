package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.constants.AppointmentConstants;
import gov.samhsa.ocp.ocpfis.service.HealthcareServiceService;
import gov.samhsa.ocp.ocpfis.service.LocationService;
import gov.samhsa.ocp.ocpfis.service.PatientService;
import gov.samhsa.ocp.ocpfis.service.PractitionerService;
import gov.samhsa.ocp.ocpfis.service.RelatedPersonService;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentDto;
import gov.samhsa.ocp.ocpfis.service.dto.AppointmentParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.HealthcareServiceDto;
import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import org.hl7.fhir.dstu3.model.Appointment;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
public class AppointmentToAppointmentDtoConverter {
    private final PatientService patientService;
    private final RelatedPersonService relatedPersonService;
    private final PractitionerService practitionerService;
    private final HealthcareServiceService healthcareServiceService;
    private final LocationService locationService;


    public AppointmentToAppointmentDtoConverter(PatientService patientService, RelatedPersonService relatedPersonService, PractitionerService practitionerService, HealthcareServiceService healthcareServiceService, LocationService locationService) {
        this.patientService = patientService;
        this.relatedPersonService = relatedPersonService;
        this.practitionerService = practitionerService;
        this.healthcareServiceService = healthcareServiceService;
        this.locationService = locationService;
    }

    public AppointmentDto map(Appointment appointment, Optional<String> requesterReference, Optional<Boolean> needMoreDetails) {

        AppointmentDto appointmentDto = new AppointmentDto();

        appointmentDto.setLogicalId(appointment.getIdElement().getIdPart());

        if (appointment.hasStatus()) {
            appointmentDto.setStatusCode(appointment.getStatus().toCode());
        }

        if (appointment.hasAppointmentType()) {
            ValueSetDto type = FhirDtoUtil.convertCodeableConceptToValueSetDto(appointment.getAppointmentType());
            appointmentDto.setTypeCode(type.getCode());
            appointmentDto.setTypeSystem(type.getSystem());
            appointmentDto.setTypeDisplay(type.getDisplay());
        }

        if (appointment.hasDescription()) {
            appointmentDto.setDescription(appointment.getDescription());
        }

        if (appointment.hasParticipant()) {
            List<AppointmentParticipantDto> participantDtos = convertAppointmentParticipantListToAppointmentParticipantDtoList(appointment, appointmentDto, needMoreDetails);
            appointmentDto.setParticipant(participantDtos);

            if (requesterReference.isPresent()) {
                String reference = requesterReference.get();
                participantDtos.forEach(
                        participant -> {
                            if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim())) {
                                appointmentDto.setRequesterParticipationStatusCode(participant.getParticipationStatusCode());
                            }
                            if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) &&
                                    participant.getParticipationTypeCode().equalsIgnoreCase(AppointmentConstants.AUTHOR_PARTICIPANT_TYPE_CODE)) {
                                appointmentDto.setCanEdit(true);
                                if (!appointment.getStatus().toCode().equalsIgnoreCase(AppointmentConstants.CANCELLED_APPOINTMENT_STATUS)) {
                                    appointmentDto.setCanCancel(true);
                                }
                            } else if (appointment.getStatus().toCode().equalsIgnoreCase(AppointmentConstants.PROPOSED_APPOINTMENT_STATUS) ||
                                    appointment.getStatus().toCode().equalsIgnoreCase(AppointmentConstants.PENDING_APPOINTMENT_STATUS) ||
                                    appointment.getStatus().toCode().equalsIgnoreCase(AppointmentConstants.BOOKED_APPOINTMENT_STATUS)) {
                                if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) && participant.getParticipationStatusCode().equalsIgnoreCase(AppointmentConstants.NEEDS_ACTION_PARTICIPATION_STATUS)) {
                                    appointmentDto.setCanAccept(true);
                                    appointmentDto.setCanDecline(true);
                                    appointmentDto.setCanTentativelyAccept(true);
                                } else if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) && participant.getParticipationStatusCode().equalsIgnoreCase(AppointmentConstants.ACCEPTED_PARTICIPATION_STATUS)) {
                                    appointmentDto.setCanDecline(true);
                                    appointmentDto.setCanTentativelyAccept(true);
                                } else if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) && participant.getParticipationStatusCode().equalsIgnoreCase(AppointmentConstants.DECLINED_PARTICIPATION_STATUS)) {
                                    appointmentDto.setCanAccept(true);
                                    appointmentDto.setCanTentativelyAccept(true);
                                } else if (participant.getActorReference().trim().equalsIgnoreCase(reference.trim()) && participant.getParticipationStatusCode().equalsIgnoreCase(AppointmentConstants.TENTATIVE_PARTICIPATION_STATUS)) {
                                    appointmentDto.setCanAccept(true);
                                    appointmentDto.setCanDecline(true);
                                }
                            }
                        }
                );
            }
        }

        if (!appointmentDto.getParticipant().isEmpty()) {
            List<AppointmentParticipantDto> patientActors = appointmentDto.getParticipant().stream()
                    .filter(participant -> participant.getActorReference() != null && participant.getActorReference().toUpperCase().contains(AppointmentConstants.PATIENT_ACTOR_REFERENCE.toUpperCase()))
                    .collect(toList());
            if (!patientActors.isEmpty()) {
                appointmentDto.setPatientName(patientActors.get(0).getActorName());
                String resourceId = patientActors.get(0).getActorReference().trim().split("/")[1];
                appointmentDto.setPatientId(resourceId);
            }

            List<AppointmentParticipantDto> creators = appointmentDto.getParticipant().stream()
                    .filter(participant -> participant.getActorReference() != null && !participant.getActorReference().isEmpty() && participant.getActorName() != null && !participant.getActorName().isEmpty() && participant.getParticipationTypeCode().equalsIgnoreCase(AppointmentConstants.AUTHOR_PARTICIPANT_TYPE_CODE))
                    .collect(toList());

            if (creators != null && !creators.isEmpty()) {
                appointmentDto.setCreatorName(creators.get(0).getActorName().trim());
                appointmentDto.setCreatorReference(creators.get(0).getActorReference().trim());
                appointmentDto.setCreatorRequired(creators.get(0).getParticipantRequiredCode());
                try {
                    appointmentDto.setCreatorRequiredDisplay(Optional.of(Appointment.ParticipantRequired.fromCode(creators.get(0).getParticipantRequiredCode()).getDisplay()));
                } catch (FHIRException e) {
                    e.printStackTrace();
                }
            }

            List<String> participantName = appointmentDto.getParticipant().stream().map(AppointmentParticipantDto::getActorName).collect(toList());
            appointmentDto.setParticipantName(participantName);
        }

        String duration = "";

        if (appointment.hasStart()) {
            appointmentDto.setStart(DateUtil.convertUTCDateToLocalDateTime(appointment.getStart()));
            DateTimeFormatter startFormatterDate = DateTimeFormatter.ofPattern(AppointmentConstants.DATE_TIME_FORMATTER_PATTERN_DATE);
            String formattedDate = appointmentDto.getStart().format(startFormatterDate);
            appointmentDto.setAppointmentDate(formattedDate);

            duration = duration + DateUtil.convertLocalDateTimeToHumanReadableFormat(appointmentDto.getStart());
        }

        if (appointment.hasEnd()) {
            appointmentDto.setEnd(DateUtil.convertUTCDateToLocalDateTime(appointment.getEnd()));

            duration = duration + " - " + DateUtil.convertLocalDateTimeToHumanReadableFormat(appointmentDto.getEnd()) + " " + "ET"; // Cheating, because DateUtil.getCurrentTimeZone(); displays "UTC" on DEMO and QA servers
        }

        if (appointment.hasCreated()) {
            appointmentDto.setCreated(DateUtil.convertUTCDateToLocalDateTime(appointment.getCreated()));
        }
        appointmentDto.setAppointmentDuration(duration);

        return appointmentDto;
    }

    private List<AppointmentParticipantDto> convertAppointmentParticipantListToAppointmentParticipantDtoList(Appointment apt, AppointmentDto appointmentDto, Optional<Boolean> needMoreDetails) {
        List<AppointmentParticipantDto> participants = new ArrayList<>();
        List<Appointment.AppointmentParticipantComponent> source = apt.getParticipant();
        if (source != null && source.size() > 0) {
            source.forEach(member -> {
                AppointmentParticipantDto participantDto = new AppointmentParticipantDto();
                participantDto.setActorName(member.getActor().getDisplay());
                participantDto.setActorReference(member.getActor().getReference());
                if (member.getRequired() != null) {
                    participantDto.setParticipantRequiredCode(member.getRequired().toCode());
                    participantDto.setParticipantRequiredDisplay(member.getRequired().getDisplay());
                    participantDto.setParticipantRequiredSystem(member.getRequired().getSystem());
                }
                if (member.getStatus() != null) {
                    participantDto.setParticipationStatusCode(member.getStatus().toCode());
                    participantDto.setParticipationStatusDisplay(member.getStatus().getDisplay());
                    participantDto.setParticipantRequiredSystem(member.getStatus().getSystem());
                }
                if (member.getType() != null && !member.getType().isEmpty() && !member.getType().get(0).getCoding().isEmpty()) {
                    participantDto.setParticipationTypeCode(member.getType().get(0).getCoding().get(0).getCode());
                    if (member.getType().get(0).getCoding().get(0).getDisplay() != null && !member.getType().get(0).getCoding().get(0).getDisplay().isEmpty()) {
                        participantDto.setParticipationTypeDisplay(member.getType().get(0).getCoding().get(0).getDisplay());
                    }
                    if (member.getType().get(0).getCoding().get(0).getSystem() != null && !member.getType().get(0).getCoding().get(0).getSystem().isEmpty()) {
                        participantDto.setParticipationTypeSystem(member.getType().get(0).getCoding().get(0).getSystem());
                    }
                }

                if (needMoreDetails.isPresent() && needMoreDetails.get()) {
                    if (member.getActor().getReference().startsWith("Patient")) {

                        participantDto.setPatient(true);
                        PatientDto patient = patientService.getPatientDemographicsInfoOnly(member.getActor().getReference());
                        appointmentDto.setPatient(patient);
                        setParticipantTelecom(participantDto, patient.getTelecoms());

                    } else if (member.getActor().getReference().startsWith("Location")) {

                        participantDto.setLocation(true);
                        LocationDto loc = locationService.getLocation(member.getActor().getReference());
                        appointmentDto.setLocation(loc);
                        setParticipantTelecom(participantDto, loc.getTelecoms());

                    } else if (member.getActor().getReference().startsWith("Practitioner")) {

                        participantDto.setPractitioner(true);
                        PractitionerDto prac = practitionerService.getPractitionerDemographicsOnly(member.getActor().getReference());
                        setParticipantTelecom(participantDto, prac.getTelecoms());

                    } else if (member.getActor().getReference().startsWith("RelatedPerson")) {

                        participantDto.setRelatedPerson(true);
                        RelatedPersonDto rel = relatedPersonService.getRelatedPersonById(member.getActor().getReference());
                        setParticipantTelecom(participantDto, rel.getTelecoms());

                    } else if (member.getActor().getReference().startsWith("HealthcareService")) {

                        participantDto.setHealthcareService(true);
                        HealthcareServiceDto hcs = healthcareServiceService.getHealthcareService(member.getActor().getReference());
                        setParticipantTelecom(participantDto, hcs.getTelecom());
                    }
                }

                participants.add(participantDto);
            });
        }
        return participants;
    }

    private void setParticipantTelecom(AppointmentParticipantDto participantDto, List<TelecomDto> telecoms) {
        for (TelecomDto t : telecoms) {
            if (t.getSystem().isPresent() && t.getSystem().get().equalsIgnoreCase("email")) {
                participantDto.setEmail(t.getValue().isPresent() ? t.getValue().get() : "N/A");
            } else if (t.getSystem().isPresent() && t.getSystem().get().equalsIgnoreCase("phone")) {
                participantDto.setPhone(t.getValue().isPresent() ? t.getValue().get() : "N/A");
            }
        }
    }

}

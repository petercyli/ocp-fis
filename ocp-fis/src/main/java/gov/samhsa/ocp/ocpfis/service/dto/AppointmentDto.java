package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentDto {
    private String logicalId;

    private String statusCode;
    //private String statusSystem;
    private String statusDisplay;

    private String typeCode;
    //private String typeSystem;
    private String typeDisplay;

    private String description;

    private LocalDateTime start;
    private LocalDateTime end;
    private LocalDateTime created;

    private List<ReferenceDto> slot; //To be used later
    private List<ReferenceDto> incomingReferral;//To be used later

    //Used to identify person(Practitioner/Patient) who created the appointment from the UI
    private String creatorReference;
    private String creatorName;

    private List<AppointmentParticipantDto> participant;

}

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
public class OutlookCalendarDto {
    private String subject;
    private LocalDateTime start;
    private LocalDateTime end;
    private String location;
    private String organizerEmail;
    private String organizerName;
    private List<NameAndEmailAddressDto> requiredAttendees;
    private List<NameAndEmailAddressDto> optionalAttendees;
    private double durationInMinutes;
    private String timeZone;

    private String myResponse;
    private String calUid;

    private boolean allDayEvent;
    private boolean cancelled;
    private boolean meeting;
    private boolean recurring;
    private boolean responseRequested;

    //For UI
    private List<String> allAttendeeName;
    private List<String> requiredAttendeeName;
    private List<String> optionalAttendeeName;
}

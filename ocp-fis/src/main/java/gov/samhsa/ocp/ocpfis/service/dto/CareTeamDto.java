package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CareTeamDto {
    private String id;

    private String name;

    private String statusCode;
    private String statusDisplay;

    private String categoryCode;
    private String categoryDisplay;

    private String subjectId;
    private String subjectFirstName;
    private String subjectLastName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String endDate;

    private List<ParticipantDto> participants;
}

package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import gov.samhsa.ocp.ocpfis.service.validation.CareTeamCategoryCodeConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.CareTeamStatusCodeConstraint;
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

    @CareTeamStatusCodeConstraint
    private String statusCode;

    @CareTeamCategoryCodeConstraint
    private String categoryCode;

    private String subjectId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String endDate;

    private List<ParticipantDto> participants;
}

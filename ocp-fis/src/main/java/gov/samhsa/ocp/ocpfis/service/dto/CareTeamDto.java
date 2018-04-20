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
    private String statusDisplay;

    @CareTeamCategoryCodeConstraint
    private String categoryCode;
    private String categoryDisplay;

    private String subjectId;
    private String subjectFirstName;
    private String subjectLastName;

    private String reasonCode;
    private String reasonDisplay;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String endDate;

    private List<ParticipantDto> participants;

    private String managingOrganization;
}
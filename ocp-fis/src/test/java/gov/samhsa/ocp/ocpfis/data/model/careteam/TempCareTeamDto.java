package gov.samhsa.ocp.ocpfis.data.model.careteam;

import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempCareTeamDto {
    private String id;

    private String name;

    private String statusCode;
    private String statusDisplay;

    private String categoryCode;
    private String categoryDisplay;

    private String subjectId;
    private String subjectFirstName;
    private String subjectLastName;

    private String reasonCode;
    private String reasonDisplay;

    private String startDate;

    private String endDate;

    private List<ParticipantDto> participants;

    private String managingOrganization;
}

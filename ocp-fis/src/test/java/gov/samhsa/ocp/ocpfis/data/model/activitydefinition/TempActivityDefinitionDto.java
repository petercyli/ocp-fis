package gov.samhsa.ocp.ocpfis.data.model.activitydefinition;

import com.fasterxml.jackson.annotation.JsonFormat;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedArtifactDto;
import gov.samhsa.ocp.ocpfis.service.dto.TimingDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempActivityDefinitionDto {
    private String logicalId;
    private String version;
    private String name;
    private String title;
    private ValueSetDto status;

    private String date;
    private String publisher;
    private String description;

    private TempPeriodDto effectivePeriod;
    private ValueSetDto topic;
    private List<RelatedArtifactDto> relatedArtifact;
    private ValueSetDto kind;

    private TempTimingDto timing;

    private ValueSetDto actionParticipantType;
    private ValueSetDto actionParticipantRole;

}

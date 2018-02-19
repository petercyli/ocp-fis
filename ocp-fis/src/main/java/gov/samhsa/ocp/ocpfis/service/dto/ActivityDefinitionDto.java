package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityDefinitionDto {
    private String logicalId;
    private String version;

    @NotNull
    @NotEmpty
    private String name;

    @NotNull
    @NotEmpty
    private String title;

    @NotNull
    @NotEmpty
    private ValueSetDto status;

    @NotNull
    @NotEmpty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String date;

    @NotNull
    @NotEmpty
    private String publisherReference;
    private String description;

    @NotNull
    @NotEmpty
    private PeriodDto effectivePeriod;

    @NotNull
    @NotEmpty
    private ValueSetDto topic;

    @NotNull
    @NotEmpty
    private List<ValueSetDto> relatedArtifact;

    @NotNull
    @NotEmpty
    private ValueSetDto kind;

    private TimingDto timing;

    @NotNull
    @NotEmpty
    private ActionParticipantDto participant;
}

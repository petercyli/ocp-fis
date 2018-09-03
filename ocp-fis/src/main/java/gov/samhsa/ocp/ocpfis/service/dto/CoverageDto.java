package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverageDto {
    private String logicalId;

    private String status;

    private Optional<String> statusDisplay;

    private String type;

    private Optional<String> typeDisplay;

    private ReferenceDto subscriber;

    private String subscriberId;

    private ReferenceDto beneficiary;

    private String relationship;

    private Optional<String> relationshipDisplay;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/YYYY")
    private String endDate;

    private String groupingPlanDisplay;

    private String network;
}

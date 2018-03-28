package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentDto {

    private String logicalId;

    private IdentifierDto identifier;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate endDate;

    private LocalDateTime dateTime;

    private String status;

    private boolean generalDesignation;

    private ReferenceDto patient;

    private List<ReferenceDto> fromActor;

    private List<ReferenceDto> toActor;

    private String fromGeneralDesignation;

    private String toGeneralDesignation;
}

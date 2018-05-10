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

public class CommunicationDto {
    private String logicalId;

    //Comments about Communication.
    private String note;

    //Message part content
    private String payloadContent;

    private boolean notDone;

    private String statusCode;
    private String statusValue;

    private String notDoneReasonCode;
    private String notDoneReasonValue;

    private String categoryCode;
    private String categoryValue;

    private String mediumCode;
    private String mediumValue;

    private ReferenceDto definition;
    private ReferenceDto topic;
    private ReferenceDto subject;
    private ReferenceDto sender;
    private ReferenceDto context;
    private ReferenceDto organization;

    private List<ReferenceDto> recipient;

    private LocalDateTime sent;
    private LocalDateTime received;
    private LocalDateTime lastUpdated;

}

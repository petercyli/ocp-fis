package gov.samhsa.ocp.ocpfis.service.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private String mediumVaule;

    private ReferenceDto definition;
    private ReferenceDto topic;
    private ReferenceDto subject;
    private ReferenceDto sender;
    private ReferenceDto context;

    private List<ReferenceDto> recipient;

    //Communication Sent Date
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy HH:mm:ss")
    private LocalDate sent;

    //Communication Received Date
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate received;

}

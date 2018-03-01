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


    /*To be supported later once organization relationship is established.
    Allow user to select from ActivityDefinition list for the organization
    in the current context and kind = Communication. There could be one or
    more ActivityDefinition templates defined for Communication (e.g. Outreach).
     */
    private ReferenceDto definition;

    private ReferenceDto topic;
    private ReferenceDto subject;
    private List<ReferenceDto> recipient;
    private ReferenceDto sender;

    /* Select from list of Episode of Care for current patient context
     * and current organization context. Generally there should be only
     * one active Episode of Care for given patient and organization.
     * Keeping it optional to allow communication that is not related to
     * specific episode of care.*/
    private ContextDto context;

    //Communication Sent Date
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate sent;

    //Communication Received Date
    //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM/dd/yyyy")
    private LocalDate received;

}

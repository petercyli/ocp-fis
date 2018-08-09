package gov.samhsa.ocp.ocpfis.service.dto;

import gov.samhsa.ocp.ocpfis.service.validation.RelatedPersonIdentifierTypeConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.RelationshipCodeConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RelatedPersonDto {

    private String relatedPersonId;

    //identifier
    @RelatedPersonIdentifierTypeConstraint
    private String identifierType;

    private String identifierValue;

    //active
    private boolean active;

    //patient
    private String patient;

    //relationship
    @RelationshipCodeConstraint
    private String relationshipCode;

    private String relationshipValue;

    private String relationshipSystem;

    //name
    private String firstName;

    private String lastName;

    //gender
    private String genderCode;

    private String genderValue;

    //birthDate
    private String birthDate;

    //period
    private String startDate;

    private String endDate;

    private List<AddressDto> addresses;

    private List<TelecomDto> telecoms;

}

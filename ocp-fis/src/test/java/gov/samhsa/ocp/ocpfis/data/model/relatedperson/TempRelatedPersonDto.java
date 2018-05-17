package gov.samhsa.ocp.ocpfis.data.model.relatedperson;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.validation.DateConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.GenderCodeConstraint;
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
public class TempRelatedPersonDto {

    private String relatedPersonId;

    //identifier
    private String identifierType;

    private String identifierValue;

    //active
    private boolean active;

    //patient
    private String patient;

    //relationship
    private String relationshipCode;

    private String relationshipValue;

    //name
    private String firstName;

    private String lastName;

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

package gov.samhsa.ocp.ocpfis.service.dto;

import gov.samhsa.ocp.ocpfis.service.validation.DateConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.GenderCodeConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.RelatedPersonIdentifierTypeConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.RelationshipCodeConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.StateCodeConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.TelecomCodeConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    //name
    private String firstName;

    private String lastName;

    //telecom
    @TelecomCodeConstraint
    private String telecomCode;

    private String telecomUse;

    private String telecomValue;

    //gender
    @GenderCodeConstraint
    private String genderCode;

    private String genderValue;

    //birthDate
    private String birthDate;

    //address
    private String address1;

    private String address2;

    private String city;

    @StateCodeConstraint
    private String state;

    private String zip;

    private String country;

    //period
    @DateConstraint
    private String startDate;

    @DateConstraint
    private String endDate;
}

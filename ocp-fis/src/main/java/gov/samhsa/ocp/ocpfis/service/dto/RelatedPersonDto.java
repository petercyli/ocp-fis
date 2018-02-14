package gov.samhsa.ocp.ocpfis.service.dto;

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

    //telecom
    private String telecomCode;

    private String telecomUse;

    private String telecomValue;

    //gender
    private String genderCode;

    private String genderValue;

    //birthDate
    private String birthDate;

    //address
    private String address1;

    private String address2;

    private String city;

    private String state;

    private String zip;

    private String country;

    //period
    private String startDate;

    private String endDate;
}

package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import gov.samhsa.ocp.ocpfis.service.validation.AdministrativeGenderConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.BirthsexConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.EthnicityConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.LanguageConstraint;
import gov.samhsa.ocp.ocpfis.service.validation.RaceConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientDto {
    private String id;

    private String resourceURL;

    @Valid
    private List<IdentifierDto> identifier;

    private boolean active;

    // Human Name (family, given name)
    private List<NameDto> name;

    @NotEmpty
    @AdministrativeGenderConstraint
    private String genderCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @RaceConstraint
    private String race;

    @EthnicityConstraint
    private String ethnicity;

    @BirthsexConstraint
    private String birthSex;

    private List<AddressDto> address;

    private List<TelecomDto> telecom;

    @LanguageConstraint
    private String language;


}

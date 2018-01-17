package gov.samhsa.ocp.ocpfis.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

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
    private String genderCode;

    private LocalDate birthDate;

    private String locale;

    private String race;

    private String ethnicity;

    private String birthSex;

    private List<AddressDto> address;

    private List<TelecomDto> telecom;

    private String mrn;

}

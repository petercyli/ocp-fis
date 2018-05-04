package gov.samhsa.ocp.ocpfis.data.model.patient;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.FlagDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.validation.AdministrativeGenderConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempPatientDto {
    private String id;

    private String resourceURL;

    private List<TempIdentifierDto> identifier;

    private boolean active;

    // Human Name (family, given name)
    private List<NameDto> name;

    private String genderCode;

    private String birthDate;

    private String race;

    private String ethnicity;

    private String birthSex;

    private List<AddressDto> addresses;

    private List<TelecomDto> telecoms;

    private String language;

    private Optional<List<FlagDto>> flags;

    Optional<String> organizationId;

    Optional<String> practitionerId;
}

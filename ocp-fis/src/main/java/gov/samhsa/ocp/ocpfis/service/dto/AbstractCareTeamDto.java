package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AbstractCareTeamDto {
    public enum ProviderType{
        PRACTITIONER, ORGANIZATION;
    }
    protected String id;

    protected String display;

    @Valid
    @NotEmpty
    protected List<IdentifierDto> identifiers;

    protected Optional<String> phoneNumber;

    protected Optional<String> email;

    @Valid
    protected AddressDto address;

    protected ProviderType providerType;
}

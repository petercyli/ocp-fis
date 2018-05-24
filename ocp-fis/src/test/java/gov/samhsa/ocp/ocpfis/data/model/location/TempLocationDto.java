package gov.samhsa.ocp.ocpfis.data.model.location;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempLocationDto {

    private String managingLocationLogicalId;
    private String status;
    private String physicalType;
    private AddressDto address;
    private List<TelecomDto> telecoms;
    private String name;

    private List<IdentifierDto> identifiers;

    private String managingOrganization;
}

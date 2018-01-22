package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateLocationDto {

    private String status;
    private List<IdentifierDto> identifiers;
    private String physicalType;
    private String name;
    private AddressDto address;
    private List<TelecomDto> telecoms;
}

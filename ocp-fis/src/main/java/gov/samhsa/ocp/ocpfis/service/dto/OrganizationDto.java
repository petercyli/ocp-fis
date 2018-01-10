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
public class OrganizationDto {
    private String id;
    private List<IdentifierDto> identifiers;
    private boolean active;
    private String name;
    private List<AddressDto> addresses;
    private List<TelecomDto> telecoms;
}
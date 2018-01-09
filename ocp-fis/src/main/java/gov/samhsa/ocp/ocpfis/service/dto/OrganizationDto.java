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
    private IdentifierDto identifier;
    private boolean active;
    private String name;
    private List<AddressDto> addresses;
    private List<TelecomDto> telecoms;
}
package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactDto {
    private String purpose;
    private Optional<String> purposeDisplay;
    private NameDto name;
    private List<TelecomDto> telecoms;
    private AddressDto address;
}

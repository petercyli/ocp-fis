package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto extends NameLogicalIdIdentifiersDto {
    private String managingLocationLogicalId;
    private String status;
    private ValueSetDto physicalType;
    private AddressDto address;
    private List<TelecomDto> telecoms;
    private Optional<Boolean> assignToCurrentPractitioner;

}

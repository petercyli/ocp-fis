package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HealthCareServiceDto extends NameLogicalIdIdentifiersDto{
    private String organizationId;
    private boolean active;
    private String categorySystem;
    private String categoryValue;
    private List<String> programName;
    private List<TelecomDto> telecom;
    private List<ValueSetDto> type;
    private List<NameLogicalIdIdentifiersDto> location;
      private  boolean assignedToCurrentLocation;
}

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
public class HealthCareServiceDto {
    private String logicalId;
    private String organizationId;
    private boolean active;
    private String name;
    private String categorySystem;
    private String categoryValue;
    private List<String> programName;
    private List<IdentifierDto> identifier;
    private List<TelecomDto> telecom;
    private List<ValueSetDto> type;
    private List<NameLogicalIdIdentifiersDto> location;
      private  boolean assignedToCurrentLocation;
}

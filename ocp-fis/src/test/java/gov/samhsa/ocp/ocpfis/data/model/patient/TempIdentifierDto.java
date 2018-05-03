package gov.samhsa.ocp.ocpfis.data.model.patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempIdentifierDto {
    private String system;
    private String value;
}

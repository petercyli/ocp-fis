package gov.samhsa.ocp.ocpfis.data.model.patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempIdentifierTypeDto {
    private String system;
    private String oid;
    private String display;
}

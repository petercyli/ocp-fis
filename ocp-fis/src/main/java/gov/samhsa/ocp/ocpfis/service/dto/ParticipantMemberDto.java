package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantMemberDto {
    private String type;
    private String id;
    private Optional<String> firstName;
    private Optional<String> lastName;
    private Optional<String> name;
}

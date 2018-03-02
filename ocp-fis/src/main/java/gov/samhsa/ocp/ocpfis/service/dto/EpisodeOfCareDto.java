package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EpisodeOfCareDto {
    String id;

    String status;

    String type;

    String patient;

    String managingOrganization;

    String start;

    String end;

    String careManager;
}

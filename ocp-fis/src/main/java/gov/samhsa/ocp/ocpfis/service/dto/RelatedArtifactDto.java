package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RelatedArtifactDto {

    String type;

    String display;

    String citation;

    String url;

    //Attachment document

}

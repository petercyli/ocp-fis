package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.RelatedArtifactDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class RelatedArtifactListToValueSetDtoListConverter extends AbstractConverter<List<RelatedArtifact>, List<RelatedArtifactDto>> {

    @Override
    protected List<RelatedArtifactDto> convert(List<RelatedArtifact> source) {
        RelatedArtifactDto dto = new RelatedArtifactDto();
        List<RelatedArtifactDto> dtos = new ArrayList<>();

        if (!source.isEmpty()) {
            int sourceSize = source.size();
            if (sourceSize > 0) {
                source.forEach(coding -> {
                    dto.setType(coding.getType().toCode());
                    dto.setDisplay(coding.getDisplay());
                    dto.setCitation(coding.getCitation());
                    dto.setUrl(coding.getUrl());
                    dtos.add(dto);
                });
            }
        }
        return dtos;

    }


}



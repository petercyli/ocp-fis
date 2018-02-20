package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class RelatedArtifactListToValueSetDtoListConverter extends AbstractConverter<List<RelatedArtifact>, List<ValueSetDto>> {

    @Override
    protected List<ValueSetDto> convert(List<RelatedArtifact> source) {
        ValueSetDto valueSetDto = new ValueSetDto();
        List<ValueSetDto> valueSetDtos = new ArrayList<>();

        if (!source.isEmpty()) {
            int sourceSize = source.size();
            if (sourceSize > 0) {
                source.forEach(coding -> {
                    valueSetDto.setCode(coding.getType().toCode());
                    valueSetDtos.add(valueSetDto);
                });
            }
        }
        return valueSetDtos;

    }


}



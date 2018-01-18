package gov.samhsa.ocp.ocpfis.service.mapping.reverse;

import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import org.hl7.fhir.dstu3.model.Identifier;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IdentifierDtoListToIdentifierListConverter extends AbstractConverter<List<IdentifierDto>, List<Identifier>> {

    @Override
    protected List<Identifier> convert(List<IdentifierDto> source) {
        List<Identifier> identifierList = new ArrayList<>();
        if (source != null && source.size() > 0) {
            for (IdentifierDto tempIdentifierDto : source) {
                Identifier tempIdentifier = new Identifier();
                if (tempIdentifierDto.getSystem() != null)
                    tempIdentifier.setSystem(tempIdentifierDto.getSystem());
                if (tempIdentifierDto.getValue() != null)
                    tempIdentifier.setValue(tempIdentifierDto.getValue());
                identifierList.add(tempIdentifier);
            }
        }
        return identifierList;
    }
}

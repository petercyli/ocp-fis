package gov.samhsa.ocp.ocpfis.service.mapping;



import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Identifier;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IdentifierListToIdentifierDtoListConverter extends AbstractConverter<List<Identifier>,List<IdentifierDto>> {
    @Override
    protected List<IdentifierDto> convert(List<Identifier> source) {
        List<IdentifierDto> identifierDtoList = new ArrayList<>();

        if (source != null && source.size() > 0) {

            for (Identifier tempIdentifier : source) {
                IdentifierDto tempIdentifierDto = new IdentifierDto();
                if (tempIdentifier.getSystem() != null)
                    tempIdentifierDto.setSystem(tempIdentifier.getSystem().toString());
                if (tempIdentifier.getValue() != null)
                    tempIdentifierDto.setValue(tempIdentifier.getValue());
                identifierDtoList.add(tempIdentifierDto);
            }
        }
        return identifierDtoList;
    }
}
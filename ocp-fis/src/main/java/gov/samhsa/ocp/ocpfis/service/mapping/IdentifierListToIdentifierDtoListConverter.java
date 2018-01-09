package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Identifier;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IdentifierListToIdentifierDtoListConverter extends AbstractConverter<List<Identifier>, List<IdentifierDto>> {
    private final String OID_TEXT = "urn:oid:";

    @Override
    protected List<IdentifierDto> convert(List<Identifier> source) {
        List<IdentifierDto> identifierDtos = new ArrayList<>();
        if (source != null && source.size() > 0) {
            for (Identifier identifier : source) {
                String systemOid = identifier.getSystem() != null ? identifier.getSystem() : "";
                identifierDtos.add(
                        IdentifierDto.builder()
                                .system(systemOid)
                                .oid(systemOid.startsWith(OID_TEXT)
                                        ? systemOid.replace(OID_TEXT,"")
                                        : "")
                                .value(identifier.getValue())
                                .display(identifier.getValue())
                                .build()
                );
            }
        }
        return identifierDtos;
    }
}

package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.domain.KnownIdentifierSystemEnum;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.exception.IdentifierSystemNotFoundException;
import org.hl7.fhir.dstu3.model.Identifier;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

@Component
public class IdentifierToIdentifierDtoConverter extends AbstractConverter<Identifier, IdentifierDto> {
    private final String OID_TEXT = "urn:oid:";
    private final String URL_TEXT = "http";
    private final String OID_NUMBER = "2.16";

    IdentifierDto identifierDto;

    @Override
    protected IdentifierDto convert(Identifier identifier) {

        if (identifier != null) {
            String systemOid = identifier.getSystem() != null ? identifier.getSystem() : "";
            String systemDisplay = null;

            try {
                if (systemOid.startsWith(OID_TEXT) || systemOid.startsWith(URL_TEXT)) {
                    systemDisplay = KnownIdentifierSystemEnum.fromUri(systemOid).getDisplay();
                } else if (systemOid.startsWith(OID_NUMBER)) {
                    systemDisplay = KnownIdentifierSystemEnum.fromOid(systemOid).getDisplay();
                } else
                    systemDisplay = systemOid;
            } catch (IdentifierSystemNotFoundException e) {
                systemDisplay = systemOid;
            }

            identifierDto = IdentifierDto.builder()
                    .system(systemOid)
                    .oid(systemOid.startsWith(OID_TEXT)
                            ? systemOid.replace(OID_TEXT, "")
                            : "")
                    .systemDisplay(systemDisplay)
                    .value(identifier.getValue())
                    .display(identifier.getValue())
                    .build();
        }
        return identifierDto;
    }

}


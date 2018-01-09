package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerToPractitionerDtoMap extends PropertyMap<Practitioner,PractitionerDto> {

    @Autowired
    private TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter;

    @Autowired
    private IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter;

    @Override
    protected void configure() {
        using(telecomListToTelecomDtoListConverter).map(source.getTelecom()).setTelecoms(null);
        using(identifierListToIdentifierDtoListConverter).map(source.getIdentifier()).setIdentifiers(null);
    }
}

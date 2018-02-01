package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HealthcareServiceToHealthCareServiceDtoMap extends PropertyMap<HealthcareService, HealthCareServiceDto> {

    @Autowired
    private TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter;

    @Autowired
    private IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter;

    @Override
    protected void configure() {
        map().setName(source.getName());
        map().setActive(source.getActive());
        using(telecomListToTelecomDtoListConverter).map(source.getTelecom()).setTelecom(null);
     //   using(identifierListToIdentifierDtoListConverter).map(source.getIdentifier()).setIdentifier(null);
    }
}


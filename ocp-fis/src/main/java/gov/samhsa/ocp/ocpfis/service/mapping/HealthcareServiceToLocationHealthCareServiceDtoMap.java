package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.LocationHealthCareServiceDto;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HealthcareServiceToLocationHealthCareServiceDtoMap extends PropertyMap<HealthcareService, LocationHealthCareServiceDto> {

    private final TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter;

    private final IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter;

    private final CodeableConceptListToValueSetDtoListConverter codeableConceptListToValueSetDtoListConverter;

    private final CodeableConceptToValueSetDtoConverter codeableConceptToValueSetDtoConverter;


    @Autowired
    public HealthcareServiceToLocationHealthCareServiceDtoMap(TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter, IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter, CodeableConceptListToValueSetDtoListConverter codeableConceptListToValueSetDtoListConverter, CodeableConceptToValueSetDtoConverter codeableConceptToValueSetDtoConverter) {
        this.telecomListToTelecomDtoListConverter = telecomListToTelecomDtoListConverter;
        this.identifierListToIdentifierDtoListConverter = identifierListToIdentifierDtoListConverter;
        this.codeableConceptListToValueSetDtoListConverter = codeableConceptListToValueSetDtoListConverter;
        this.codeableConceptToValueSetDtoConverter = codeableConceptToValueSetDtoConverter;
    }

    @Override
    protected void configure() {
        map().setResourceURL(source.getId());
        map().setName(source.getName());
        map().setActive(source.getActive());
        map().setOrganizationName(source.getProvidedBy().getDisplay());
        map().setCategorySystem(source.getCategory().getCodingFirstRep().getSystem());
        map().setCategoryValue(source.getCategory().getCodingFirstRep().getCode());
        using(telecomListToTelecomDtoListConverter).map(source.getTelecom()).setTelecom(null);
        using(identifierListToIdentifierDtoListConverter).map(source.getIdentifier()).setIdentifiers(null);
        using(codeableConceptListToValueSetDtoListConverter).map(source.getType()).setType(null);
        using(codeableConceptToValueSetDtoConverter).map(source.getCategory()).setCategory(null);
    }
}

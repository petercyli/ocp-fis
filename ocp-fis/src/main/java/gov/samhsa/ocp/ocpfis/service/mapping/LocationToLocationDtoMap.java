package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import org.hl7.fhir.dstu3.model.Location;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocationToLocationDtoMap extends PropertyMap<Location, LocationDto> {

    private final AddressToAddressDtoConverter addressToAddressDtoConverter;

    private final TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter;

    private final IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter;

    private final CodeableConceptToValueSetDtoConverter codeableConceptToValueSetDtoConverter;

    @Autowired
    public LocationToLocationDtoMap(AddressToAddressDtoConverter addressToAddressDtoConverter, TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter, IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter, CodeableConceptToValueSetDtoConverter codeableConceptToValueSetDtoConverter) {
        this.addressToAddressDtoConverter = addressToAddressDtoConverter;
        this.telecomListToTelecomDtoListConverter = telecomListToTelecomDtoListConverter;
        this.identifierListToIdentifierDtoListConverter = identifierListToIdentifierDtoListConverter;
        this.codeableConceptToValueSetDtoConverter = codeableConceptToValueSetDtoConverter;
    }

    @Override
    protected void configure() {
        map().setResourceURL(source.getId());
        map().setName(source.getName());
        map().setStatus(source.getStatusElement().asStringValue());
        using(addressToAddressDtoConverter).map(source.getAddress()).setAddress(null);
        using(telecomListToTelecomDtoListConverter).map(source.getTelecom()).setTelecoms(null);
        using(identifierListToIdentifierDtoListConverter).map(source.getIdentifier()).setIdentifiers(null);
        using(codeableConceptToValueSetDtoConverter).map(source.getPhysicalType()).setPhysicalType(null);
    }
}

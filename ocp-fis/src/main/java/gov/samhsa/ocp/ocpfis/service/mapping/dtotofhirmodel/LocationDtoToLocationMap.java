package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.LocationDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Location;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LocationDtoToLocationMap extends PropertyMap<LocationDto, Location> {

    private final IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter;

    private final AddressDtoToAddressConverter addressDtoToAddressConverter;

    private final TelecomDtoListToTelecomListConverter telecomDtoListToTelecomListConverter;

    private final ValuesetDtoToCodeableConceptConverter valuesetDtoToCodeableConceptConverter;

    @Autowired
    public LocationDtoToLocationMap(IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter, AddressDtoToAddressConverter addressDtoToAddressConverter, TelecomDtoListToTelecomListConverter telecomDtoListToTelecomListConverter, ValuesetDtoToCodeableConceptConverter valuesetDtoToCodeableConceptConverter) {
        this.identifierDtoListToIdentifierListConverter = identifierDtoListToIdentifierListConverter;
        this.addressDtoToAddressConverter = addressDtoToAddressConverter;
        this.telecomDtoListToTelecomListConverter = telecomDtoListToTelecomListConverter;
        this.valuesetDtoToCodeableConceptConverter = valuesetDtoToCodeableConceptConverter;
    }

    @Override
    protected void configure() {
        using(identifierDtoListToIdentifierListConverter).map(source.getIdentifiers()).setIdentifier(null);
        using(addressDtoToAddressConverter).map(source.getAddress()).setAddress(null);
        using(telecomDtoListToTelecomListConverter).map(source.getTelecoms()).setTelecom(null);
        using(valuesetDtoToCodeableConceptConverter).map(source.getPhysicalType()).setPhysicalType(null);
    }
}

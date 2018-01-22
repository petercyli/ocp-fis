package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import org.hl7.fhir.dstu3.model.Organization;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrganizationDtoToOrganizationMap extends PropertyMap<OrganizationDto, Organization> {
    @Autowired
    private IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter;

    @Autowired
    private AddressDtoListToAddressListConverter addressDtoListToAddressListConverter;

    @Autowired
    private TelecomDtoListToTelecomListConverter telecomDtoListToTelecomListConverter;

    @Override
    protected void configure() {
        map().setName(source.getName());
        using(identifierDtoListToIdentifierListConverter).map(source.getIdentifiers()).setIdentifier(null);
        using(addressDtoListToAddressListConverter).map(source.getAddresses()).setAddress(null);
        using(telecomDtoListToTelecomListConverter).map(source.getTelecoms()).setTelecom(null);
    }

}

package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import org.hl7.fhir.dstu3.model.Organization;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrganizationToOrganizationDtoMap extends PropertyMap<Organization, OrganizationDto> {

    @Autowired
    private AddressListToAddressDtoListConverter addressListToAddressDtoListConverter;

    @Autowired
    private TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter;

    @Override
    protected void configure() {
        map().setName(source.getName());
        using(addressListToAddressDtoListConverter).map(source.getAddress()).setAddresses(null);
        using(telecomListToTelecomDtoListConverter).map(source.getTelecom()).setTelecoms(null);
    }
}

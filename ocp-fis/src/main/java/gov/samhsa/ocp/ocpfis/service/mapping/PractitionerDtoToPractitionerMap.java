package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.AddressDtoListToAddressListConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.IdentifierDtoListToIdentifierListConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.NameDtoListToHumanNameListConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.TelecomDtoListToTelecomListConverter;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerDtoToPractitionerMap extends PropertyMap<PractitionerDto, Practitioner> {
    @Autowired
    private NameDtoListToHumanNameListConverter nameDtoListToHumanNameListConverter;

    @Autowired
    private AddressDtoListToAddressListConverter addressDtoListToAddressListConverter;

    @Autowired
    private IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter;

    @Autowired
    private TelecomDtoListToTelecomListConverter telecomDtoListToTelecomListConverter;

    @Override
    protected void configure() {
        using(nameDtoListToHumanNameListConverter).map(source.getName()).setName(null);
        using(addressDtoListToAddressListConverter).map(source.getAddress()).setAddress(null);
        using(identifierDtoListToIdentifierListConverter).map(source.getIdentifiers()).setIdentifier(null);
        using(telecomDtoListToTelecomListConverter).map(source.getTelecoms()).setTelecom(null);
    }
}

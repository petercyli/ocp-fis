package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientDtoToPatientMap extends PropertyMap<PatientDto, Patient> {
    @Autowired
    private NameDtoListToHumanNameListConverter nameDtoListToHumanNameListConverter;

    @Autowired
    private AddressDtoListToAddressListConverter addressDtoListToAddressListConverter;

    @Autowired
    private IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter;

    @Autowired
    private TelecomDtoListToTelecomListConverter telecomDtoListToTelecomListConverter;

    public PatientDtoToPatientMap(NameDtoListToHumanNameListConverter nameDtoListToHumanNameListConverter,
                                  AddressDtoListToAddressListConverter addressDtoListToAddressListConverter,
                                  IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter,
                                  TelecomDtoListToTelecomListConverter telecomDtoListToTelecomListConverter) {
        this.nameDtoListToHumanNameListConverter = nameDtoListToHumanNameListConverter;
        this.addressDtoListToAddressListConverter = addressDtoListToAddressListConverter;
        this.identifierDtoListToIdentifierListConverter = identifierDtoListToIdentifierListConverter;
        this.telecomDtoListToTelecomListConverter = telecomDtoListToTelecomListConverter;
    }

    @Override
    protected void configure() {
        map().setActive(source.isActive());

        using(nameDtoListToHumanNameListConverter).map(source.getName()).setName(null);
        using(addressDtoListToAddressListConverter).map(source.getAddress()).setAddress(null);
        using(identifierDtoListToIdentifierListConverter).map(source.getIdentifier()).setIdentifier(null);
        using(telecomDtoListToTelecomListConverter).map(source.getTelecom()).setTelecom(null);
    }

}

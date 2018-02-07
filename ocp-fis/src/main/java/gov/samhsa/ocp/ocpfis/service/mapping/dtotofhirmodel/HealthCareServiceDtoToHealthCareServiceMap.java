package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class HealthCareServiceDtoToHealthCareServiceMap extends PropertyMap<HealthCareServiceDto, HealthcareService> {

    @Autowired
    private IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter;

    @Autowired
    TelecomDtoListToTelecomListConverter telecomDtoListToTelecomListConverter;

    @Autowired
    ValuesetDtoListToCodeableConceptListConverter valuesetDtoListToCodeableConceptListConverter;

    @Autowired
    ValuesetDtoToCodeableConceptConverter valuesetDtoToCodeableConceptConverter;

    @Override
    protected void configure() {
        using(identifierDtoListToIdentifierListConverter).map(source.getIdentifiers()).setIdentifier(null);
        using(telecomDtoListToTelecomListConverter).map(source.getTelecom()).setTelecom(null);
        using(valuesetDtoListToCodeableConceptListConverter).map(source.getType()).setType(null);
        using(valuesetDtoListToCodeableConceptListConverter).map(source.getSpecialty()).setSpecialty(null);
        using(valuesetDtoListToCodeableConceptListConverter).map(source.getReferralMethod()).setReferralMethod(null);
        using(valuesetDtoToCodeableConceptConverter).map(source.getCategory()).setCategory(null);
    }

}


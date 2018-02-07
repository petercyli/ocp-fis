package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.HealthCareServiceDto;
import org.hl7.fhir.dstu3.model.HealthcareService;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HealthcareServiceToHealthCareServiceDtoMap extends PropertyMap<HealthcareService, HealthCareServiceDto> {

    private final TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter;

    private final IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter;

    private final HealthecareServiceTypeListToHealthcareServiceTypeDtoListConverter healthecareServiceTypeListToHealthcareServiceTypeDtoListConverter;

    private final HealthecareServiceCategoryToHealthcareServiceCategoryDtoConverter  healthecareServiceCategoryToHealthcareServiceCategoryDtoConverter;

    private final StringTypeListToStringListConverter stringTypeListToStringListConverter;

    @Autowired
    public HealthcareServiceToHealthCareServiceDtoMap(TelecomListToTelecomDtoListConverter telecomListToTelecomDtoListConverter, IdentifierListToIdentifierDtoListConverter identifierListToIdentifierDtoListConverter, HealthecareServiceTypeListToHealthcareServiceTypeDtoListConverter healthecareServiceTypeListToHealthcareServiceTypeDtoListConverter, HealthecareServiceCategoryToHealthcareServiceCategoryDtoConverter  healthecareServiceCategoryToHealthcareServiceCategoryDtoConverter, StringTypeListToStringListConverter stringTypeListToStringListConverter) {
        this.telecomListToTelecomDtoListConverter = telecomListToTelecomDtoListConverter;
        this.identifierListToIdentifierDtoListConverter = identifierListToIdentifierDtoListConverter;
        this.healthecareServiceTypeListToHealthcareServiceTypeDtoListConverter = healthecareServiceTypeListToHealthcareServiceTypeDtoListConverter;
        this.healthecareServiceCategoryToHealthcareServiceCategoryDtoConverter = healthecareServiceCategoryToHealthcareServiceCategoryDtoConverter;
        this.stringTypeListToStringListConverter = stringTypeListToStringListConverter;
    }
    @Override
    protected void configure() {
        map().setName(source.getName());
        map().setActive(source.getActive());
        map().setOrganizationId(source.getProvidedBy().getReference());
        map().setOrganizationName(source.getProvidedBy().getDisplay());
        map().setCategorySystem(source.getCategory().getCodingFirstRep().getSystem());
        map().setCategoryValue(source.getCategory().getCodingFirstRep().getCode());
        using(stringTypeListToStringListConverter).map(source.getProgramName()).setProgramName(null);
        using(telecomListToTelecomDtoListConverter).map(source.getTelecom()).setTelecom(null);
        using(identifierListToIdentifierDtoListConverter).map(source.getIdentifier()).setIdentifiers(null);
        using(healthecareServiceTypeListToHealthcareServiceTypeDtoListConverter).map(source.getType()).setType(null);
        using(healthecareServiceTypeListToHealthcareServiceTypeDtoListConverter).map(source.getSpecialty()).setSpecialty(null);
        using(healthecareServiceTypeListToHealthcareServiceTypeDtoListConverter).map(source.getReferralMethod()).setReferralMethod(null);
        using(healthecareServiceCategoryToHealthcareServiceCategoryDtoConverter).map(source.getCategory()).setCategory(null);
    }
}


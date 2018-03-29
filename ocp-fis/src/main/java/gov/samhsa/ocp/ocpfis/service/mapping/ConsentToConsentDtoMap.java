package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import org.hl7.fhir.dstu3.model.Consent;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ConsentToConsentDtoMap extends PropertyMap<Consent, ConsentDto> {
    private static final String PATIENT_ACTOR_REFERENCE = "Patient";
    private static final String FROM_ACTOR_REFERENCE = "FromActor";
    private static final String TO_ACTOR_REFERENCE = "ToActor";

    private static final String DATE_TIME_FORMATTER_PATTERN_DATE = "MM/dd/yyyy";

    private final ConsentActorComponentListToReferenceDtoListConverter consentActorComponentListToReferenceDtoListConverter;
    private final ResourceIdToLogicalIdConverter resourceIdToLogicalIdConverter;

    @Autowired
    public ConsentToConsentDtoMap(ConsentActorComponentListToReferenceDtoListConverter consentActorComponentListToReferenceDtoListConverter,
                                  ResourceIdToLogicalIdConverter resourceIdToLogicalIdConverter) {
        this.consentActorComponentListToReferenceDtoListConverter = consentActorComponentListToReferenceDtoListConverter;
        this.resourceIdToLogicalIdConverter = resourceIdToLogicalIdConverter;
    }

    @Override
    protected void configure() {
        using(resourceIdToLogicalIdConverter).map(source).setLogicalId(null);
        using(consentActorComponentListToReferenceDtoListConverter).map(source.getActor()).setFromActor(null);
        using(consentActorComponentListToReferenceDtoListConverter).map(source.getActor()).setToActor(null);
    }

}
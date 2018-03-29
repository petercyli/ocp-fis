package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import org.hl7.fhir.dstu3.model.Consent;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ConsentToConsentDtoMap extends PropertyMap<Consent, ConsentDto> {
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
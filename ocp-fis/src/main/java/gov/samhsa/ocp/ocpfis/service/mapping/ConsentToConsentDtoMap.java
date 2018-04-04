package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import org.hl7.fhir.dstu3.model.Consent;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ConsentToConsentDtoMap extends PropertyMap<Consent, ConsentDto> {
    private final ConsentActorComponentListToConsenToReferenceDtoListConverter consentActorComponentListToConsenToReferenceDtoListConverter;
    private final ConsentActorComponentListToConsenFromReferenceDtoListConverter consentActorComponentListToConsenFromReferenceDtoListConverter;
    private final ResourceIdToLogicalIdConverter resourceIdToLogicalIdConverter;
    private final PeriodToPeriodDtoConverter periodToPeriodDtoConverter;
    private final CodeableConceptListToValueSetDtoListConverter codeableConceptListToValueSetDtoListConverter;
    private final IdentifierToIdentifierDtoConverter identifierToIdentifierDtoConverter;

    @Autowired
    public ConsentToConsentDtoMap(ConsentActorComponentListToConsenToReferenceDtoListConverter consentActorComponentListToConsenToReferenceDtoListConverter,
                                  ConsentActorComponentListToConsenFromReferenceDtoListConverter consentActorComponentListToConsenFromReferenceDtoListConverter,
                                  ResourceIdToLogicalIdConverter resourceIdToLogicalIdConverter,
                                  PeriodToPeriodDtoConverter periodToPeriodDtoConverter,
                                  CodeableConceptListToValueSetDtoListConverter CodeableConceptListToValueSetDtoListConverter,
                                  IdentifierToIdentifierDtoConverter identifierToIdentifierDtoConverter) {
        this.consentActorComponentListToConsenToReferenceDtoListConverter = consentActorComponentListToConsenToReferenceDtoListConverter;
        this.consentActorComponentListToConsenFromReferenceDtoListConverter = consentActorComponentListToConsenFromReferenceDtoListConverter;
        this.resourceIdToLogicalIdConverter = resourceIdToLogicalIdConverter;
        this.periodToPeriodDtoConverter = periodToPeriodDtoConverter;
        this.codeableConceptListToValueSetDtoListConverter = CodeableConceptListToValueSetDtoListConverter;
        this.identifierToIdentifierDtoConverter = identifierToIdentifierDtoConverter;
    }

    @Override
    protected void configure() {
        using(resourceIdToLogicalIdConverter).map(source).setLogicalId(null);
        using(consentActorComponentListToConsenFromReferenceDtoListConverter).map(source.getActor()).setFromActor(null);
        using(consentActorComponentListToConsenToReferenceDtoListConverter).map(source.getActor()).setToActor(null);
        using(periodToPeriodDtoConverter).map(source.getPeriod()).setPeriod(null);
        using(codeableConceptListToValueSetDtoListConverter).map(source.getCategory()).setCategory(null);
        using(identifierToIdentifierDtoConverter).map(source.getIdentifier()).setIdentifier(null);
    }

}
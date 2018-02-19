package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityDefinitionToActivityDefinitionDtoMap extends PropertyMap<ActivityDefinition, ActivityDefinitionDto>{

    private final RelatedArtifactListToValueSetListDtoConverter relatedArtifactListToValueSetListDtoConverter;
    private final CodeableConceptListToValueSetDtoConverter codeableConceptListToValueSetDtoConverter;


    @Autowired
    public ActivityDefinitionToActivityDefinitionDtoMap(CodeableConceptListToValueSetDtoConverter codeableConceptListToValueSetDtoConverter, RelatedArtifactListToValueSetListDtoConverter relatedArtifactListToValueSetListDtoConverter) {

        this.relatedArtifactListToValueSetListDtoConverter = relatedArtifactListToValueSetListDtoConverter;
        this.codeableConceptListToValueSetDtoConverter = codeableConceptListToValueSetDtoConverter;
    }

    @Override
    protected void configure() {
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setTitle(source.getTitle());
        map().setVersion(source.getVersion());
        map().setPublisherReference(source.getPublisher());
        using(codeableConceptListToValueSetDtoConverter).map(source.getTopic()).setTopic(null);
        using(relatedArtifactListToValueSetListDtoConverter).map(source.getRelatedArtifact()).setRelatedArtifact(null);
    }
}

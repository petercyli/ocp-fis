package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityDefinitionToActivityDefinitionDtoMap extends PropertyMap<ActivityDefinition, ActivityDefinitionDto>{

    private final CodeableConceptToValueSetDtoConverter codeableConceptToValueSetDtoConverter;
    private final CodeableConceptListToValueSetDtoListConverter codeableConceptListToValueSetDtoListConverter;


    @Autowired
    public ActivityDefinitionToActivityDefinitionDtoMap(CodeableConceptListToValueSetDtoListConverter codeableConceptListToValueSetDtoListConverter, CodeableConceptToValueSetDtoConverter codeableConceptToValueSetDtoConverter) {

        this.codeableConceptToValueSetDtoConverter = codeableConceptToValueSetDtoConverter;
        this.codeableConceptListToValueSetDtoListConverter = codeableConceptListToValueSetDtoListConverter;
    }

    @Override
    protected void configure() {
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setTitle(source.getTitle());
        map().setVersion(source.getVersion());
        //using(codeableConceptToValueSetDtoConverter).map(source.getStatus()).setStatus(null);
        //using(codeableConceptToValueSetDtoConverter).map(source.getTopic()).setTopic(null);
        //using(codeableConceptToValueSetDtoConverter).map(source.getKind()).setKind(null);
        //using(codeableConceptListToValueSetDtoListConverter).map(source.getRelatedArtifact()).setRelatedArtifact(null);
    }
}

package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;

@Service
@Slf4j
public class ActivityDefinitionServiceImpl implements ActivityDefinitionService{
    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public ActivityDefinitionServiceImpl(ModelMapper modelMapper,IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.modelMapper=modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }


    @Override
    public void createActivityDefinition(ActivityDefinitionDto activityDefinitionDto) {
       ActivityDefinition activityDefinition=modelMapper.map(activityDefinitionDto,ActivityDefinition.class);
        activityDefinition.setDate(java.sql.Date.valueOf(activityDefinitionDto.getDate()));

        fhirClient.create().resource(activityDefinition).execute();
    }
}

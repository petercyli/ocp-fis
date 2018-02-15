package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public PageDto<ActivityDefinitionDto> getAllActivityDefinitionsByOrganization(String organizationResourceId, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize){
        int numberOfActivityDefinitionsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.ActivityDefinition.name());

        Bundle firstPageActivityDefinitionSearchBundle;
        Bundle otherPageActivityDefinitionSearchBundle;
        boolean firstPage = true;

        IQuery activityDefinitionsSearchQuery = fhirClient.search().forResource(ActivityDefinition.class).where(new ReferenceClientParam("organization").hasId(organizationResourceId));

        // Check if there are any additional search criteria
        activityDefinitionsSearchQuery = addAdditionalSearchConditions(activityDefinitionsSearchQuery, searchKey, searchValue);

        //The following bundle only contains Page 1 of the resultSet with location
        firstPageActivityDefinitionSearchBundle = (Bundle) activityDefinitionsSearchQuery.count(numberOfActivityDefinitionsPerPage)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (firstPageActivityDefinitionSearchBundle == null || firstPageActivityDefinitionSearchBundle.getEntry().isEmpty()) {
            log.info("No Activity Definition found for the given OrganizationID:" + organizationResourceId);
            return new PageDto<>(new ArrayList<>(), numberOfActivityDefinitionsPerPage, 0, 0, 0, 0);
        }

        log.info("FHIR Activity Definition(s) bundle retrieved " + firstPageActivityDefinitionSearchBundle.getTotal() + " Activity Definition(s) from FHIR server successfully");

        otherPageActivityDefinitionSearchBundle = firstPageActivityDefinitionSearchBundle;
        if (pageNumber.isPresent() && pageNumber.get() > 1) {
            // Load the required page
            firstPage = false;
            otherPageActivityDefinitionSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, otherPageActivityDefinitionSearchBundle, pageNumber.get(), numberOfActivityDefinitionsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedActivityDefinitions = otherPageActivityDefinitionSearchBundle.getEntry();

        //Arrange Page related info
        List<ActivityDefinitionDto> activityDefinitionsList = retrievedActivityDefinitions.stream().map(aa -> convertActivityDefinitionBundleEntryToActivityDefinitionDto(aa)).collect(Collectors.toList());
        double totalPages = Math.ceil((double) otherPageActivityDefinitionSearchBundle.getTotal() / numberOfActivityDefinitionsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(activityDefinitionsList, numberOfActivityDefinitionsPerPage, totalPages, currentPage, activityDefinitionsList.size(), otherPageActivityDefinitionSearchBundle.getTotal());
    }


    @Override
    public void createActivityDefinition(ActivityDefinitionDto activityDefinitionDto) {
       ActivityDefinition activityDefinition=modelMapper.map(activityDefinitionDto,ActivityDefinition.class);
        activityDefinition.setDate(java.sql.Date.valueOf(activityDefinitionDto.getDate()));

        fhirClient.create().resource(activityDefinition).execute();
    }

    private IQuery addAdditionalSearchConditions(IQuery activityDefinitionsSearchQuery, Optional<String> searchKey, Optional<String> searchValue) {
        if (searchKey.isPresent() && !SearchKeyEnum.HealthcareServiceSearchKey.contains(searchKey.get())) {
            throw new BadRequestException("Unidentified search key:" + searchKey.get());
        } else if ((searchKey.isPresent() && !searchValue.isPresent()) ||
                (searchKey.isPresent() && searchValue.get().trim().isEmpty())) {
            throw new BadRequestException("No search value found for the search key" + searchKey.get());
        }

        // Check if there are any additional search criteria
        if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.NAME.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.NAME.name() + " = " + searchValue.get().trim());
            activityDefinitionsSearchQuery.where(new StringClientParam("name").matches().value(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.LOGICALID.name() + " = " + searchValue.get().trim());
            activityDefinitionsSearchQuery.where(new TokenClientParam("_id").exactly().code(searchValue.get().trim()));
        } else if (searchKey.isPresent() && searchKey.get().equalsIgnoreCase(SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name())) {
            log.info("Searching for " + SearchKeyEnum.HealthcareServiceSearchKey.IDENTIFIERVALUE.name() + " = " + searchValue.get().trim());
            activityDefinitionsSearchQuery.where(new TokenClientParam("identifier").exactly().code(searchValue.get().trim()));
        } else {
            log.info("No additional search criteria entered.");
        }
        return activityDefinitionsSearchQuery;
    }

    private ActivityDefinitionDto convertActivityDefinitionBundleEntryToActivityDefinitionDto(Bundle.BundleEntryComponent fhirActivityDefinitionModel) {
        ActivityDefinitionDto tempActivityDefinitionDto = modelMapper.map(fhirActivityDefinitionModel.getResource(), ActivityDefinitionDto.class);
        tempActivityDefinitionDto.setLogicalId(fhirActivityDefinitionModel.getResource().getIdElement().getIdPart());

        return tempActivityDefinitionDto;
    }



}

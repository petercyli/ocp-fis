package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.ActivityDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TimingDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.BadRequestException;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.ActivityDefinitionDtoToActivityDefinitionConverter;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Timing;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class ActivityDefinitionServiceImpl implements ActivityDefinitionService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    private final OrganizationService organizationService;

    @Autowired
    public ActivityDefinitionServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties, OrganizationService organizationService) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.organizationService = organizationService;
    }

    @Override
    public PageDto<ActivityDefinitionDto> getAllActivityDefinitionsByOrganization(String organizationResourceId, Optional<String> searchKey, Optional<String> searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfActivityDefinitionsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.ActivityDefinition.name());

        Bundle firstPageActivityDefinitionSearchBundle;
        Bundle otherPageActivityDefinitionSearchBundle;
        boolean firstPage = true;

        IQuery activityDefinitionsSearchQuery = fhirClient.search().forResource(ActivityDefinition.class).where(new StringClientParam("publisher").matches().value("Organization/" + organizationResourceId));

        //Set Sort order
        activityDefinitionsSearchQuery = FhirUtil.setLastUpdatedTimeSortOrder(activityDefinitionsSearchQuery, true);

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
        List<ActivityDefinitionDto> activityDefinitionsList = retrievedActivityDefinitions.stream().map(this::convertActivityDefinitionBundleEntryToActivityDefinitionDto).collect(toList());

        double totalPages = Math.ceil((double) otherPageActivityDefinitionSearchBundle.getTotal() / numberOfActivityDefinitionsPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(activityDefinitionsList, numberOfActivityDefinitionsPerPage, totalPages, currentPage, activityDefinitionsList.size(), otherPageActivityDefinitionSearchBundle.getTotal());
    }

    @Override
    public ActivityDefinitionDto getActivityDefinitionById(String id) {

        Bundle bundle = fhirClient.search().forResource(ActivityDefinition.class)
                .where(new TokenClientParam("_id").exactly().code(id))
                .returnBundle(Bundle.class).execute();

        if (bundle == null || bundle.isEmpty()) {
            throw new ResourceNotFoundException("No ActivityDefinition was found for the given id : " + id);
        }

        Bundle.BundleEntryComponent component = bundle.getEntry().get(0);
        return convertActivityDefinitionBundleEntryToActivityDefinitionDto(component);
    }

    @Override
    public void createActivityDefinition(ActivityDefinitionDto activityDefinitionDto, String organizationId) {
        if (!isDuplicate(activityDefinitionDto, organizationId)) {
            String version = fisProperties.getActivityDefinition().getVersion();

            ActivityDefinition activityDefinition = ActivityDefinitionDtoToActivityDefinitionConverter.map(activityDefinitionDto, organizationId, version);

            fhirClient.create().resource(activityDefinition).execute();
        } else {
            throw new DuplicateResourceFoundException("Duplicate Activity Definition is already present.");
        }
    }

    @Override
    public void updateActivityDefinition(ActivityDefinitionDto activityDefinitionDto, String organizationId, String activityDefinitionId) {
        String version = fisProperties.getActivityDefinition().getVersion();

        ActivityDefinition activityDefinition = ActivityDefinitionDtoToActivityDefinitionConverter.map(activityDefinitionDto, organizationId, version);
        activityDefinition.setId(activityDefinitionId);

        fhirClient.update().resource(activityDefinition).execute();
    }

    @Override
    public List<ReferenceDto> getActivityDefinitionsByPractitioner(String practitioner) {
        List<ReferenceDto> referenceOrganizationDtos = organizationService.getOrganizationsByPractitioner(practitioner);

        return referenceOrganizationDtos.stream()
                .flatMap(it -> getActivityDefinitionByOrganization(FhirDtoUtil.getIdFromReferenceDto(it, ResourceType.Organization)).stream())
                .collect(toList());
    }

    private List<ReferenceDto> getActivityDefinitionByOrganization(String organization) {
        List<ReferenceDto> activityDefinitions = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(ActivityDefinition.class)
                .where(new StringClientParam("publisher").matches().value(ResourceType.Organization + "/" + organization))
                .where(new TokenClientParam("status").exactly().code(String.valueOf(Enumerations.PublicationStatus.ACTIVE).toLowerCase()))
                .count(fisProperties.getResourceSinglePageLimit())
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> activityDefinitionComponents = bundle.getEntry();

            if (activityDefinitionComponents != null) {
                activityDefinitions = activityDefinitionComponents.stream()
                        .map(it -> (ActivityDefinition) it.getResource())
                        .map(it -> FhirDtoUtil.mapActivityDefinitionToReferenceDto(it))
                        .collect(toList());
            }
        }

        return activityDefinitions;
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
            activityDefinitionsSearchQuery.where(new RichStringClientParam("name").contains().value(searchValue.get().trim()));
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
        ActivityDefinition activityDefinition = (ActivityDefinition) fhirActivityDefinitionModel.getResource();

        tempActivityDefinitionDto.getStatus().setCode(activityDefinition.getStatus().toCode());

        tempActivityDefinitionDto.getKind().setCode(activityDefinition.getKind().toCode());

        if (activityDefinition.getEffectivePeriod() != null && !activityDefinition.getEffectivePeriod().isEmpty()) {
            PeriodDto periodDto = new PeriodDto();
            tempActivityDefinitionDto.setEffectivePeriod(periodDto);

            if (null != activityDefinition.getEffectivePeriod().getStart())
                tempActivityDefinitionDto.getEffectivePeriod().setStart(DateUtil.convertDateToLocalDate(activityDefinition.getEffectivePeriod().getStart()));
            if (null != activityDefinition.getEffectivePeriod().getEnd())
                tempActivityDefinitionDto.getEffectivePeriod().setEnd(DateUtil.convertDateToLocalDate(activityDefinition.getEffectivePeriod().getEnd()));
        }

        activityDefinition.getParticipant().stream().findFirst().ifPresent(participant -> {
            participant.getRole().getCoding().stream().findFirst().ifPresent(role -> {
                ValueSetDto valueSetDto = new ValueSetDto();
                valueSetDto.setCode(role.getCode());
                valueSetDto.setDisplay(role.getDisplay());
                valueSetDto.setSystem(role.getSystem());
                tempActivityDefinitionDto.setActionParticipantRole(valueSetDto);
            });
            if (participant.hasType())
                tempActivityDefinitionDto.setActionParticipantType(FhirDtoUtil.convertCodeToValueSetDto(participant.getType().toCode(), lookUpService.getActionParticipantType()));
        });

        TimingDto timingDto = new TimingDto();
        tempActivityDefinitionDto.setTiming(timingDto);
        try {
            if ((activityDefinition.getTimingTiming() != null) && !activityDefinition.getTimingTiming().isEmpty()) {
                if ((activityDefinition.getTimingTiming().getRepeat() != null || !(activityDefinition.getTimingTiming().getRepeat().isEmpty()))) {
                    tempActivityDefinitionDto.getTiming().setDurationMax((activityDefinition.getTimingTiming().getRepeat().getDurationMax().floatValue()));
                    tempActivityDefinitionDto.getTiming().setFrequency(activityDefinition.getTimingTiming().getRepeat().getFrequency());
                }
            }
        } catch (FHIRException e) {
            log.error("FHIR Exception when setting Duration and Frequency", e);
        }
        return tempActivityDefinitionDto;
    }


    private boolean isDuplicate(ActivityDefinitionDto activityDefinitionDto, String organizationid) {
        return activityDefinitionDto.getStatus().getCode().equalsIgnoreCase(Enumerations.PublicationStatus.ACTIVE.toString()) && (isDuplicateWithNamePublisherKindAndStatus(activityDefinitionDto, organizationid) || isDuplicateWithTitlePublisherKindAndStatus(activityDefinitionDto, organizationid));
    }


    private boolean isDuplicateWithNamePublisherKindAndStatus(ActivityDefinitionDto activityDefinitionDto, String organizationid) {
        Bundle duplicateCheckWithNamePublisherAndStatusBundle = fhirClient.search().forResource(ActivityDefinition.class)
                .where(new StringClientParam("publisher").matches().value("Organization/" + organizationid))
                .where(new TokenClientParam("status").exactly().code("active"))
                .where(new StringClientParam("name").matches().value(activityDefinitionDto.getName()))
                .returnBundle(Bundle.class)
                .execute();

        return hasSameKind(duplicateCheckWithNamePublisherAndStatusBundle, activityDefinitionDto);

    }

    private boolean isDuplicateWithTitlePublisherKindAndStatus(ActivityDefinitionDto activityDefinitionDto, String organizationid) {

        Bundle duplicateCheckWithTitlePublisherAndStatusBundle = fhirClient.search().forResource(ActivityDefinition.class)
                .where(new StringClientParam("publisher").matches().value("Organization/" + organizationid))
                .where(new TokenClientParam("status").exactly().code("active"))
                .where(new StringClientParam("title").matches().value(activityDefinitionDto.getTitle()))
                .returnBundle(Bundle.class)
                .execute();

        return hasSameKind(duplicateCheckWithTitlePublisherAndStatusBundle, activityDefinitionDto);
    }

    private boolean hasSameKind(Bundle bundle, ActivityDefinitionDto activityDefinitionDto) {
        List<Bundle.BundleEntryComponent> duplicateCheckList = new ArrayList<>();
        if (!bundle.isEmpty()) {
            duplicateCheckList = bundle.getEntry().stream().filter(activityDefinitionResource -> {
                ActivityDefinition activityDefinition = (ActivityDefinition) activityDefinitionResource.getResource();
                return activityDefinition.getKind().toCode().equalsIgnoreCase(activityDefinitionDto.getKind().getCode());
            }).collect(toList());
        }
        return !duplicateCheckList.isEmpty();

    }


}

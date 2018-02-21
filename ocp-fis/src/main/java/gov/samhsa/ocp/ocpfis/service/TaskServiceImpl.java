package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.ContextDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.TaskDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirUtils;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public TaskServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
    }

    @Override
    public PageDto<TaskDto> getTasks(Optional<List<String>> statusList, String searchKey, String searchValue, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfTasksPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Task.name());
        IQuery iQuery = fhirClient.search().forResource(Task.class);

        //Check for patient
        if (searchKey.equalsIgnoreCase("patientId"))
            iQuery.where(new ReferenceClientParam("patient").hasId("Patient/" + searchValue));

        //Check for organization
        if (searchKey.equalsIgnoreCase("organizationId"))
            iQuery.where(new ReferenceClientParam("organization").hasId("Organization/" + searchValue));

        //Check for task
        if (searchKey.equalsIgnoreCase("taskId"))
            iQuery.where(new TokenClientParam("_id").exactly().code(searchValue));

        //Check for status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            iQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        }

        Bundle firstPageTaskBundle;
        Bundle otherPageTaskBundle;
        boolean firstPage = true;

        //Bundle retrieves care team along with its participant and subject
        firstPageTaskBundle = (Bundle) iQuery
                .count(numberOfTasksPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPageTaskBundle == null || firstPageTaskBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Tasks were found in the FHIR server.");
        }

        otherPageTaskBundle = firstPageTaskBundle;

        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageTaskBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageTaskBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageTaskBundle, pageNumber.get(), numberOfTasksPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedTasks = otherPageTaskBundle.getEntry();


        List<TaskDto> taskDtos = retrievedTasks.stream().filter(retrivedBundle -> retrivedBundle.getResource().getResourceType().equals(ResourceType.Task)).map(retrievedTask -> {

            Task task = (Task) retrievedTask.getResource();

            TaskDto taskDto = new TaskDto();

            ValueSetDto performerTypeDto =  new ValueSetDto();
            ReferenceDto partof = new ReferenceDto();

            taskDto.setLogicalId(task.getIdElement().getIdPart());
            taskDto.setDescription(task.getDescription());
            taskDto.setNote(task.getNote().get(0).getText());

            if (task.getStatus() != null) {

                taskDto.setStatus(ValueSetDto.builder()
                        .code((task.getStatus().toCode() != null && !task.getStatus().toCode().isEmpty()) ? task.getStatus().toCode() : null)
                        .display((task.getStatus().getDisplay() != null && !task.getStatus().getDisplay().isEmpty()) ? task.getStatus().getDisplay() : null)
                        .build());
            }

            if (task.getIntent() != null) {

                taskDto.setIntent(ValueSetDto.builder()
                        .code((task.getIntent().toCode() != null && !task.getIntent().toCode().isEmpty()) ? task.getIntent().toCode() : null)
                        .display((task.getIntent().getDisplay() != null && !task.getIntent().getDisplay().isEmpty()) ? task.getIntent().getDisplay() : null)
                        .build());
            }

            if (task.getPriority() != null) {
                taskDto.setPriority(ValueSetDto.builder()
                        .code((task.getPriority().toCode() != null && !task.getPriority().toCode().isEmpty()) ? task.getPriority().toCode() : null)
                        .display((task.getPriority().getDisplay() != null && !task.getPriority().getDisplay().isEmpty()) ? task.getPriority().getDisplay() : null)
                        .build());
            }

            if (task.getPerformerType() != null) {
                task.getPerformerType().stream().findFirst().ifPresent(performerType -> {
                    performerType.getCoding().stream().findFirst().ifPresent(coding -> {
                        performerTypeDto.setCode((coding.getCode() != null && !coding.getCode().isEmpty()) ? coding.getCode() : null);
                        performerTypeDto.setDisplay((getDisplay(coding.getCode(), Optional.ofNullable(lookUpService.getTaskPerformerType()))).orElse(null));
                    });
                });

                taskDto.setPerformerType(performerTypeDto);
            }

            if (task.hasPartOf()) {
                taskDto.setPartOf(ReferenceDto.builder()
                        .reference((task.getPartOf().get(0).getReference() != null && !task.getPartOf().get(0).getReference().isEmpty()) ? task.getPartOf().get(0).getReference() : null)
                        .display((task.getPartOf().get(0).getDisplay() != null && !task.getPartOf().get(0).getDisplay().isEmpty()) ? task.getPartOf().get(0).getDisplay() : null)
                        .build());
            }

            if (task.hasFor()) {
                taskDto.setBeneficiary(ReferenceDto.builder()
                        .reference((task.getFor().getReference() != null && !task.getFor().getReference().isEmpty()) ? task.getFor().getReference(): null)
                        .display((task.getFor().getDisplay() != null && !task.getFor().getDisplay().isEmpty()) ? task.getFor().getDisplay(): null)
                        .build());
            }

            if (task.hasRequester()) {
                if(task.getRequester().hasOnBehalfOf())
                taskDto.setOnBehalfOf(ReferenceDto.builder()
                        .reference((task.getRequester().getOnBehalfOf().getReference() != null && !task.getRequester().getOnBehalfOf().getReference().isEmpty()) ? task.getRequester().getOnBehalfOf().getReference(): null)
                        .display((task.getRequester().getOnBehalfOf().getDisplay() != null && !task.getRequester().getOnBehalfOf().getDisplay().isEmpty()) ? task.getRequester().getOnBehalfOf().getDisplay(): null)
                        .build());
            }


            if (task.hasRequester()) {
                if(task.getRequester().hasAgent())
                    taskDto.setAgent(ReferenceDto.builder()
                            .reference((task.getRequester().getAgent().getReference() != null && !task.getRequester().getAgent().getReference().isEmpty()) ? task.getRequester().getOnBehalfOf().getReference(): null)
                            .display((task.getRequester().getAgent().getDisplay() != null && !task.getRequester().getAgent().getDisplay().isEmpty()) ? task.getRequester().getOnBehalfOf().getDisplay(): null)
                            .build());
            }

            if (task.hasOwner()) {
                    taskDto.setOwner(ReferenceDto.builder()
                            .reference((task.getOwner().getReference() != null && !task.getOwner().getReference().isEmpty()) ? task.getOwner().getReference(): null)
                            .display((task.getOwner().getDisplay() != null && !task.getOwner().getDisplay().isEmpty()) ? task.getOwner().getDisplay(): null)
                            .build());
            }

            if (task.hasDefinition()) {
                try {
                    taskDto.setDefinition(ReferenceDto.builder()
                            .reference((task.hasDefinitionReference()) ? task.getDefinitionReference().getReference(): null)
                            .display((task.hasDefinitionReference()) ? task.getDefinitionReference().getDisplay(): null)
                            .build());
                } catch (FHIRException e) {
                }
            }

            if (task.hasContext()) {
                taskDto.setContext(ContextDto.builder()
                        .logicalId((task.hasContext()) ? task.getContext().getReference() : null)
                        .build());
            }

            if(task.hasLastModified() ) {
                taskDto.setLastModified(FhirUtils.convertToLocalDate(task.getLastModified()));
            }

            if(task.hasAuthoredOn()) {
                taskDto.setAuthoredOn(FhirUtils.convertToLocalDate(task.getAuthoredOn()));
            }

            if(task.getExecutionPeriod()!=null && !task.getExecutionPeriod().isEmpty()) {
                PeriodDto periodDto = new PeriodDto();
                taskDto.setExecutionPeriod(periodDto);
                taskDto.getExecutionPeriod().setStart((task.getExecutionPeriod().hasStart()) ? FhirUtils.convertToLocalDate(task.getExecutionPeriod().getStart()): null);
                taskDto.getExecutionPeriod().setEnd((task.getExecutionPeriod().hasEnd()) ? FhirUtils.convertToLocalDate(task.getExecutionPeriod().getEnd()): null);
            }

            return taskDto;

        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageTaskBundle.getTotal() / numberOfTasksPerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(taskDtos, numberOfTasksPerPage, totalPages, currentPage, taskDtos.size(), otherPageTaskBundle.getTotal());
    }

    private Optional<String> getDisplay(String code, Optional<List<ValueSetDto>> lookupValueSets) {
        Optional<String> lookupDisplay = Optional.empty();
        if (lookupValueSets.isPresent()) {
            lookupDisplay = lookupValueSets.get().stream()
                    .filter(lookupValue -> code.equalsIgnoreCase(lookupValue.getCode()))
                    .map(ValueSetDto::getDisplay).findFirst();

        }
        return lookupDisplay;
    }

}

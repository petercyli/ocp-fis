package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.*;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Timing;
import org.hl7.fhir.dstu3.model.codesystems.ActionParticipantType;
import org.hl7.fhir.dstu3.model.codesystems.DefinitionTopic;
import org.hl7.fhir.dstu3.model.codesystems.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.codesystems.Relationship;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static gov.samhsa.ocp.ocpfis.service.PatientServiceImpl.TO_DO;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;

    @Autowired
    private LookUpService lookUpService;

    public OrganizationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
    }

    @Override
    public OrganizationDto getOrganization(String organizationId) {
        final Organization retrievedOrganization = fhirClient.read().resource(Organization.class).withId(organizationId).execute();
        if (retrievedOrganization == null || retrievedOrganization.isEmpty()) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server.");
        }
        final OrganizationDto organizationDto = modelMapper.map(retrievedOrganization, OrganizationDto.class);
        organizationDto.setLogicalId(retrievedOrganization.getIdElement().getIdPart());
        return organizationDto;
    }

    @Override
    public PageDto<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class);

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPageOrganizationSearchBundle;
        Bundle otherPageOrganizationSearchBundle;
        boolean firstPage = true;

        firstPageOrganizationSearchBundle = (Bundle) organizationIQuery
                .count(numberOfOrganizationsPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPageOrganizationSearchBundle == null || firstPageOrganizationSearchBundle.getEntry().size() < 1) {
            log.info("No organizations were found for the given criteria.");
            return new PageDto<>(new ArrayList<>(), numberOfOrganizationsPerPage, 0, 0, 0, 0);
        }

        otherPageOrganizationSearchBundle = firstPageOrganizationSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPageOrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            // Load the required page
            otherPageOrganizationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedOrganizations = otherPageOrganizationSearchBundle.getEntry();

        List<OrganizationDto> organizationsList = retrievedOrganizations.stream().map(retrievedOrganization -> {
            OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    @Override
    public PageDto<OrganizationDto> searchOrganizations(OrganizationController.SearchType type, String value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class);

        if (type.equals(OrganizationController.SearchType.name))
            organizationIQuery.where(new StringClientParam("name").matches().value(value.trim()));

        if (type.equals(OrganizationController.SearchType.identifier))
            organizationIQuery.where(new TokenClientParam("identifier").exactly().code(value));

        if (type.equals(OrganizationController.SearchType.logicalId))
            organizationIQuery.where(new TokenClientParam("_id").exactly().code(value));

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPageOrganizationSearchBundle;
        Bundle otherPageOrganizationSearchBundle;
        boolean firstPage = true;

        firstPageOrganizationSearchBundle = (Bundle) organizationIQuery.count(numberOfOrganizationsPerPage).returnBundle(Bundle.class)
                .execute();

        if (firstPageOrganizationSearchBundle == null || firstPageOrganizationSearchBundle.isEmpty() || firstPageOrganizationSearchBundle.getEntry().size() < 1) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server.");
        }

        otherPageOrganizationSearchBundle = firstPageOrganizationSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPageOrganizationSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;

            otherPageOrganizationSearchBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageOrganizationSearchBundle, page.get(), numberOfOrganizationsPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedOrganizations = otherPageOrganizationSearchBundle.getEntry();

        List<OrganizationDto> organizationsList = retrievedOrganizations.stream().map(retrievedOrganization -> {
            OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    private int getOrganizationsCountByIdentifier(String system, String code) {
        log.info("Searching organizations with identifier.system : " + system + " and code : " + code);
        IQuery searchQuery = fhirClient.search().forResource(Organization.class)
                .where(new TokenClientParam("identifier").exactly().systemAndCode(system, code));
        Bundle searchBundle = (Bundle) searchQuery.returnBundle(Bundle.class).execute();
        log.info("Total " + searchBundle.getTotal());
        return searchBundle.getTotal();
    }


    @Override
    public void createOrganization(OrganizationDto organizationDto) {

        //Check Duplicate Identifier
        int existingNumberOfOrganizations = this.getOrganizationsCountByIdentifier(organizationDto.getIdentifiers().get(0).getSystem(), organizationDto.getIdentifiers().get(0).getValue());
        String identifier = organizationDto.getIdentifiers().get(0).getValue();

        //When there is no duplicate identifier, the organization gets created
        if (existingNumberOfOrganizations == 0) {
            //Create Fhir Organization
            Organization fhirOrganization = modelMapper.map(organizationDto, Organization.class);
            fhirOrganization.setActive(Boolean.TRUE);

            final ValidationResult validationResult = fhirValidator.validateWithResult(fhirOrganization);
            if (validationResult.isSuccessful()) {
                MethodOutcome serverResponse = fhirClient.create().resource(fhirOrganization).execute();
                log.info("Successfully created a new organization :" + serverResponse.getId().getIdPart());
                createActivityDefinition(serverResponse);
            } else {
                throw new FHIRFormatErrorException("FHIR Organization Validation is not successful" + validationResult.getMessages());
            }
        } else {
            throw new DuplicateResourceFoundException("Organization with the Identifier " + identifier + " is already present.");
        }
    }

    @Override
    public void updateOrganization(String organizationId, OrganizationDto organizationDto) {
        log.info("Updating the Organization with Id:" + organizationId);
        //Check Duplicate Identifier
        boolean hasDuplicateIdentifier = organizationDto.getIdentifiers().stream().anyMatch(identifierDto -> {
            IQuery organizationsWithUpdatedIdentifierQuery = fhirClient.search()
                    .forResource(Organization.class)
                    .where(new TokenClientParam("identifier")
                            .exactly().systemAndCode(identifierDto.getSystem(), identifierDto.getValue()));
            Bundle organizationWithUpdatedIdentifierBundle = (Bundle) organizationsWithUpdatedIdentifierQuery.returnBundle(Bundle.class).execute();
            Bundle organizationWithUpdatedIdentifierAndSameResourceIdBundle = (Bundle) organizationsWithUpdatedIdentifierQuery.where(new TokenClientParam("_id").exactly().code(organizationId)).returnBundle(Bundle.class).execute();
            if (organizationWithUpdatedIdentifierBundle.getTotal() > 0) {
                return organizationWithUpdatedIdentifierBundle.getTotal() != organizationWithUpdatedIdentifierAndSameResourceIdBundle.getTotal();
            }
            return false;
        });

        Organization existingOrganization = fhirClient.read().resource(Organization.class).withId(organizationId.trim()).execute();

        if (!hasDuplicateIdentifier) {
            Organization updatedOrganization = modelMapper.map(organizationDto, Organization.class);
            existingOrganization.setIdentifier(updatedOrganization.getIdentifier());
            existingOrganization.setName(updatedOrganization.getName());
            existingOrganization.setTelecom(updatedOrganization.getTelecom());
            existingOrganization.setAddress(updatedOrganization.getAddress());
            existingOrganization.setActive(updatedOrganization.getActive());

            // Validate the resource
            final ValidationResult validationResult = fhirValidator.validateWithResult(existingOrganization);
            if (validationResult.isSuccessful()) {
                log.info("Update Organization: Validation successful? " + validationResult.isSuccessful() + " for OrganizationID:" + organizationId);

                fhirClient.update().resource(existingOrganization)
                        .execute();
                log.info("Organization successfully updated");
            } else {
                throw new FHIRFormatErrorException("FHIR Organization Validation is not successful" + validationResult.getMessages());
            }
        } else {
            throw new DuplicateResourceFoundException("Organization with the Identifier " + organizationId + " is already present.");
        }
    }

    @Override
    public void inactivateOrganization(String organizationId) {
        log.info("Inactivating the organization Id: " + organizationId);
        Organization existingFhirOrganization = readOrganizationFromServer(organizationId);
        setOrganizationStatusToInactive(existingFhirOrganization);
    }

    @Override
    public List<ReferenceDto> getOrganizationsByPractitioner(String practitioner) {
        List<ReferenceDto> organizations = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(PractitionerRole.class)
                .where(new ReferenceClientParam("practitioner").hasId(ResourceType.Practitioner + "/" + practitioner))
                .include(PractitionerRole.INCLUDE_ORGANIZATION)
                .count(fisProperties.getResourceSinglePageLimit())
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> organizationComponents = bundle.getEntry();

            if (organizationComponents != null) {
                organizations = organizationComponents.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.PractitionerRole))
                        .map(it -> (PractitionerRole) it.getResource())
                        .map(it -> (Organization) it.getOrganization().getResource())
                        .map(FhirDtoUtil::mapOrganizationToReferenceDto)
                        .distinct()
                        .collect(toList());

            }
        }

        return organizations;
    }

    private Organization readOrganizationFromServer(String organizationId) {
        Organization existingFhirOrganization;

        try {
            existingFhirOrganization = fhirClient.read().resource(Organization.class).withId(organizationId.trim()).execute();
        } catch (BaseServerResponseException e) {
            log.error("FHIR Client returned with an error while reading the organization with ID: " + organizationId);
            throw new ResourceNotFoundException("FHIR Client returned with an error while reading the organization:" + e.getMessage());
        }
        return existingFhirOrganization;
    }

    private void setOrganizationStatusToInactive(Organization existingFhirOrganization) {
        existingFhirOrganization.setActive(false);
        try {
            MethodOutcome serverResponse = fhirClient.update().resource(existingFhirOrganization).execute();
            log.info("Inactivated the organization :" + serverResponse.getId().getIdPart());
        } catch (BaseServerResponseException e) {
            log.error("Could NOT inactivate organization");
            throw new FHIRClientException("FHIR Client returned with an error while inactivating the organization:" + e.getMessage());
        }
    }

    private void createActivityDefinition(MethodOutcome methodOutcome){
        ActivityDefinition activityDefinition=new ActivityDefinition();
        activityDefinition.setVersion(fisProperties.getActivityDefinition().getVersion());
        activityDefinition.setName("To-Do");
        activityDefinition.setTitle("To-Do");

        activityDefinition.setStatus(Enumerations.PublicationStatus.ACTIVE);

        activityDefinition.setKind(ActivityDefinition.ActivityDefinitionKind.ACTIVITYDEFINITION);
        CodeableConcept topic=new CodeableConcept();
        topic.addCoding().setCode(DefinitionTopic.TREATMENT.toCode()).setDisplay(DefinitionTopic.TREATMENT.getDisplay())
                .setSystem(DefinitionTopic.TREATMENT.getSystem());
        activityDefinition.setTopic(Arrays.asList(topic));

        activityDefinition.setDate( java.sql.Date.valueOf(LocalDate.now()));
        activityDefinition.setPublisher("Organization/"+methodOutcome.getId().getIdPart());
        activityDefinition.setDescription(TO_DO);

        Period period=new Period();
        period.setStart(java.sql.Date.valueOf(LocalDate.now()));
        period.setEnd(java.sql.Date.valueOf(LocalDate.now().plusYears(20)));
        activityDefinition.setEffectivePeriod(period);

        Timing timing=new Timing();
        timing.getRepeat().setDurationMax(10);
        timing.getRepeat().setFrequency(1);
        activityDefinition.setTiming(timing);

        CodeableConcept participantRole=new CodeableConcept();
        ValueSetDto participantRoleValueSet= FhirDtoUtil.convertCodeToValueSetDto("O", lookUpService.getActionParticipantRole());
        participantRole.addCoding().setCode(participantRoleValueSet.getCode())
                        .setDisplay(participantRoleValueSet.getDisplay())
                        .setSystem(participantRoleValueSet.getSystem());
        activityDefinition.addParticipant()
                .setRole(participantRole)
                .setType(ActivityDefinition.ActivityParticipantType.PRACTITIONER);

        RelatedArtifact relatedArtifact=new RelatedArtifact();
        relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DOCUMENTATION);
        relatedArtifact.setDisplay("To-Do List");
        activityDefinition.setRelatedArtifact(Arrays.asList(relatedArtifact));

        fhirClient.create().resource(activityDefinition).execute();
    }
}

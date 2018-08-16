package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.ProvenanceActivityEnum;
import gov.samhsa.ocp.ocpfis.service.dto.ContactDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.OrganizationNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.AddressDtoToAddressConverter;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.FhirResourceUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.ProvenanceUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StringType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static ca.uhn.fhir.rest.api.Constants.PARAM_LASTUPDATED;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {


    private final ModelMapper modelMapper;
    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final FisProperties fisProperties;

    private final LookUpService lookUpService;
    private final ProvenanceUtil provenanceUtil;


    @Autowired
    public OrganizationServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties, LookUpService lookUpService, ProvenanceUtil provenanceUtil) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.lookUpService = lookUpService;
        this.provenanceUtil = provenanceUtil;
    }

    @Override
    public OrganizationDto getOrganization(String organizationId) {
        final Organization retrievedOrganization = fhirClient.read().resource(Organization.class).withId(organizationId).execute();
        if (retrievedOrganization == null || retrievedOrganization.isEmpty()) {
            throw new OrganizationNotFoundException("No organizations were found in the FHIR server.");
        }
        final OrganizationDto organizationDto = modelMapper.map(retrievedOrganization, OrganizationDto.class);
        organizationDto.setContacts(Optional.of(convertContactListToContactListDto(retrievedOrganization)));
        organizationDto.setLogicalId(retrievedOrganization.getIdElement().getIdPart());
        return organizationDto;
    }

    @Override
    public PageDto<OrganizationDto> getAllOrganizations(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class);

        //Set Sort order
        organizationIQuery = FhirOperationUtil.setLastUpdatedTimeSortOrder(organizationIQuery, true);

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            organizationIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPageOrganizationSearchBundle;
        Bundle otherPageOrganizationSearchBundle;
        boolean firstPage = true;

        firstPageOrganizationSearchBundle = PaginationUtil.getSearchBundleFirstPage(organizationIQuery, numberOfOrganizationsPerPage, Optional.empty());

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
            Organization organization = (Organization) retrievedOrganization.getResource();
            organizationDto.setContacts(Optional.of(convertContactListToContactListDto(organization)));
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    @Override
    public PageDto<OrganizationDto> searchOrganizations(Optional<OrganizationController.SearchType> type, Optional<String> value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll) {
        int numberOfOrganizationsPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.Organization.name());

        IQuery organizationIQuery = fhirClient.search().forResource(Organization.class).sort().descending(PARAM_LASTUPDATED);

        type.ifPresent(t -> {
                    if (t.equals(OrganizationController.SearchType.name))
                        value.ifPresent(v -> organizationIQuery.where(new RichStringClientParam("name").contains().value(v.trim())));

                    if (t.equals(OrganizationController.SearchType.identifier))
                        value.ifPresent(v -> organizationIQuery.where(new TokenClientParam("identifier").exactly().code(v)));

                    if (t.equals(OrganizationController.SearchType.logicalId))
                        value.ifPresent(v -> organizationIQuery.where(new TokenClientParam("_id").exactly().code(v)));
                }
        );

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

        if (showAll.isPresent() && showAll.get()) {
            List<OrganizationDto> organizationDtos = convertAllBundleToSingleOrganizationDtoList(firstPageOrganizationSearchBundle, numberOfOrganizationsPerPage);
            return (PageDto<OrganizationDto>) PaginationUtil.applyPaginationForCustomArrayList(organizationDtos, organizationDtos.size(), Optional.of(1), false);
        }

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
            Organization organization = (Organization) retrievedOrganization.getResource();
            organizationDto.setContacts(Optional.of(convertContactListToContactListDto(organization)));
            organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
            return organizationDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageOrganizationSearchBundle.getTotal() / numberOfOrganizationsPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(organizationsList, numberOfOrganizationsPerPage, totalPages, currentPage, organizationsList.size(), otherPageOrganizationSearchBundle.getTotal());
    }

    private int getOrganizationsCountByIdentifier(String system, String code) {
        log.info("Searching organizations with identifier.system : " + system + " and code : " + code);

        Bundle searchBundle = fhirClient.search().forResource(Organization.class).where(new TokenClientParam("identifier").exactly().systemAndCode(system, code))
                .returnBundle(Bundle.class).execute();
        if (searchBundle.getTotal() == 0) {
            Bundle organizationBundle = fhirClient.search().forResource(Organization.class).returnBundle(Bundle.class).execute();
            return FhirOperationUtil.getAllBundleComponentsAsList(organizationBundle, Optional.empty(), fhirClient, fisProperties).stream().filter(organization -> {
                Organization o = (Organization) organization.getResource();
                return o.getIdentifier().stream().anyMatch(identifier ->
                        (identifier.getSystem().equalsIgnoreCase(system) &&
                                identifier.getValue().replaceAll(" ", "")
                                        .replaceAll("-", "").trim()
                                        .equalsIgnoreCase(code.replaceAll(" ", "").replaceAll("-", "").trim()))
                );
            }).collect(toList()).size();
        } else {
            return searchBundle.getTotal();
        }
    }


    @Override
    public void createOrganization(OrganizationDto organizationDto, Optional<String> loggedInUser) {
        List<String> idList = new ArrayList<>();

        //Check Duplicate Identifier
        int existingNumberOfOrganizations = this.getOrganizationsCountByIdentifier(organizationDto.getIdentifiers().get(0).getSystem(), organizationDto.getIdentifiers().get(0).getValue());
        String identifier = organizationDto.getIdentifiers().get(0).getValue();

        //When there is no duplicate identifier, the organization gets created
        if (existingNumberOfOrganizations == 0) {
            // Map
            Organization fhirOrganization = modelMapper.map(organizationDto, Organization.class);
            fhirOrganization.setActive(Boolean.TRUE);
            organizationDto.getContacts().ifPresent(contacts->{
                contacts.forEach(contact->{
                    Organization.OrganizationContactComponent orgCon=convertContactDtoToorganizationContact(contact);
                    fhirOrganization.addContact(orgCon);
                });
            });

            //Set Profile Meta Data
            FhirProfileUtil.setOrganizationProfileMetaData(fhirClient, fhirOrganization);

            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, fhirOrganization, Optional.empty(), ResourceType.Organization.name(), "Create Organization");

            //Create
            MethodOutcome serverResponse = FhirOperationUtil.createFhirResource(fhirClient, fhirOrganization, ResourceType.Organization.name());
            idList.add(ResourceType.Organization.name() + "/" + FhirOperationUtil.getFhirId(serverResponse));

            // Add TO DO Activity Definition
            ActivityDefinition activityDefinition = FhirResourceUtil.createToDoActivityDefinition(serverResponse.getId().getIdPart(), fisProperties, lookUpService, fhirClient);

            //Set Profile Meta Data
            FhirProfileUtil.setActivityDefinitionProfileMetaData(fhirClient, activityDefinition);

            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, activityDefinition, Optional.empty(), ResourceType.ActivityDefinition.name(), "Create ActivityDefinition (when creating an Organization)");

            //Create TO DO Activity Definition
            MethodOutcome activityDefinitionMethodOutcome = FhirOperationUtil.createFhirResource(fhirClient, activityDefinition, ResourceType.ActivityDefinition.name());
            idList.add(ResourceType.ActivityDefinition.name() + "/" + FhirOperationUtil.getFhirId(activityDefinitionMethodOutcome));

            if(fisProperties.isProvenanceEnabled()) {
                provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.CREATE, loggedInUser);
            }

        } else {
            throw new DuplicateResourceFoundException("Organization with the Identifier " + identifier + " is already present.");
        }
    }

    @Override
    public void updateOrganization(String organizationId, OrganizationDto organizationDto, Optional<String> loggedInUser) {
        List<String> idList = new ArrayList<>();

        log.info("Updating the Organization with Id:" + organizationId);

        Organization existingOrganization = fhirClient.read().resource(Organization.class).withId(organizationId.trim()).execute();
        organizationDto.setLogicalId(organizationId);
        if (!isDuplicateWhileUpdate(organizationDto)) {
            Organization updatedOrganization = modelMapper.map(organizationDto, Organization.class);
            existingOrganization.setIdentifier(updatedOrganization.getIdentifier());
            existingOrganization.setName(updatedOrganization.getName());
            existingOrganization.setTelecom(updatedOrganization.getTelecom());
            existingOrganization.setAddress(updatedOrganization.getAddress());
            existingOrganization.setActive(updatedOrganization.getActive());

            organizationDto.getContacts().ifPresent(contacts->{
                contacts.forEach(contact->{
                    Organization.OrganizationContactComponent orgCon=convertContactDtoToorganizationContact(contact);
                    existingOrganization.addContact(orgCon);
                });
            });

            //Set Profile Meta Data
            FhirProfileUtil.setOrganizationProfileMetaData(fhirClient, existingOrganization);

            //Validate
            FhirOperationUtil.validateFhirResource(fhirValidator, existingOrganization, Optional.of(organizationId), ResourceType.Organization.name(), "Update Organization");

            //Update
            MethodOutcome methodOutcome = FhirOperationUtil.updateFhirResource(fhirClient, existingOrganization, "Update Organization");
            idList.add(ResourceType.Organization.name() + "/" + FhirOperationUtil.getFhirId(methodOutcome));

            if(fisProperties.isProvenanceEnabled()) {
                provenanceUtil.createProvenance(idList, ProvenanceActivityEnum.UPDATE, loggedInUser);
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
                .sort().descending(PARAM_LASTUPDATED)
                .returnBundle(Bundle.class).execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> organizationComponents = FhirOperationUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties);

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

        //Set Profile Meta Data
        FhirProfileUtil.setOrganizationProfileMetaData(fhirClient, existingFhirOrganization);

        //Validate
        FhirOperationUtil.validateFhirResource(fhirValidator, existingFhirOrganization, Optional.of(existingFhirOrganization.getId()), ResourceType.Organization.name(), "Update Organization");

        //Update
        FhirOperationUtil.updateFhirResource(fhirClient, existingFhirOrganization, "Inactivate Organization");
    }

    private List<OrganizationDto> convertAllBundleToSingleOrganizationDtoList(Bundle firstPageOrganizationSearchBundle, int numberOBundlePerPage) {
        return FhirOperationUtil.getAllBundleComponentsAsList(firstPageOrganizationSearchBundle, Optional.of(numberOBundlePerPage), fhirClient, fisProperties)
                .stream()
                .map(retrievedOrganization -> {
                    OrganizationDto organizationDto = modelMapper.map(retrievedOrganization.getResource(), OrganizationDto.class);
                    Organization organization = (Organization) retrievedOrganization.getResource();
                    organizationDto.setContacts(Optional.of(convertContactListToContactListDto(organization)));
                    organizationDto.setLogicalId(retrievedOrganization.getResource().getIdElement().getIdPart());
                    return organizationDto;
                })
                .collect(toList());
    }

    private boolean isDuplicateWhileUpdate(OrganizationDto organizationDto) {
        final Organization organization = fhirClient.read().resource(Organization.class).withId(organizationDto.getLogicalId()).execute();

        Bundle searchOrganization = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(Organization.class)
                .where(Organization.IDENTIFIER.exactly().systemAndIdentifier(organizationDto.getIdentifiers().stream().findFirst().get().getSystem(), organizationDto.getIdentifiers().stream().findFirst().get().getValue())))
                .returnBundle(Bundle.class).execute();

        if (!searchOrganization.getEntry().isEmpty()) {
            return !organization.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getSystem())
                    && identifier.getValue().equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getValue()));
        } else {
            Bundle organizationBundle = (Bundle) FhirOperationUtil.setNoCacheControlDirective(fhirClient.search().forResource(Organization.class)).returnBundle(Bundle.class).execute();
            List<Bundle.BundleEntryComponent> bundleEntryComponents = FhirOperationUtil.getAllBundleComponentsAsList(organizationBundle, Optional.empty(), fhirClient, fisProperties).stream().filter(org -> {
                Organization o = (Organization) org.getResource();
                return o.getIdentifier().stream().anyMatch(identifier -> identifier.getSystem().equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getSystem()) && identifier.getValue().replaceAll(" ", "")
                        .replaceAll("-", "").trim()
                        .equalsIgnoreCase(organizationDto.getIdentifiers().stream().findFirst().get().getValue().replaceAll(" ", "").replaceAll("-", "").trim()));
            }).collect(toList());
            if (bundleEntryComponents.isEmpty()) {
                return false;
            } else {
                return !bundleEntryComponents.stream().anyMatch(resource -> {
                    Organization oRes = (Organization) resource.getResource();
                    return oRes.getIdElement().getIdPart().equalsIgnoreCase(organization.getIdElement().getIdPart());
                });
            }
        }
    }

    private Organization.OrganizationContactComponent convertContactDtoToorganizationContact(ContactDto contact){
        Organization.OrganizationContactComponent orgCon=new Organization.OrganizationContactComponent();
        orgCon.setPurpose(FhirDtoUtil.convertValuesetDtoToCodeableConcept(FhirDtoUtil.convertCodeToValueSetDto(contact.getPurpose(),lookUpService.getContactPurpose())));
        HumanName humanName=new HumanName();
        humanName.setFamily(contact.getName().getLastName());
        humanName.addGiven(contact.getName().getLastName());
        orgCon.setName(humanName);
        orgCon.setTelecom(FhirResourceUtil.convertTelecomDtoListToTelecomList(contact.getTelecoms(),lookUpService));
        orgCon.setAddress(FhirResourceUtil.convertAddressDtoToAddress(contact.getAddress(),lookUpService));
        return orgCon;
    }

    private ContactDto convertOrganizationContactToContactDto(Organization.OrganizationContactComponent contact){
        ContactDto contactDto=new ContactDto();
        contactDto.setPurpose(FhirDtoUtil.convertCodeableConceptToValueSetDto(contact.getPurpose()).getCode());
        contactDto.setPurposeDisplay(Optional.of(FhirDtoUtil.convertCodeableConceptToValueSetDto(contact.getPurpose()).getDisplay()));
        NameDto nameDto=new NameDto();
        nameDto.setLastName(contact.getName().getFamily());
        contact.getName().getGiven().stream().findAny().ifPresent(gn->nameDto.setFirstName(gn.toString()));
        contactDto.setName(nameDto);
        contactDto.setTelecoms(FhirDtoUtil.convertTelecomListToTelecomDtoList(contact.getTelecom()));
        contactDto.setAddress(FhirDtoUtil.convertAddressToAddressDto(contact.getAddress()));
        return contactDto;
    }

    private List<ContactDto> convertContactListToContactListDto(Organization organization){
        List<ContactDto> contactDtos=new ArrayList<>();
        organization.getContact().forEach(con->{
            ContactDto contactDto= convertOrganizationContactToContactDto(con);
            contactDtos.add(contactDto);
        });
        return contactDtos;
    }
}

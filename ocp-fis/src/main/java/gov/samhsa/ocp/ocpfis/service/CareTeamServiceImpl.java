package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.CareTeamFieldEnum;
import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.CareTeamToCareTeamDtoConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.CareTeamDtoToCareTeamConverter;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import gov.samhsa.ocp.ocpfis.util.RichStringClientParam;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class CareTeamServiceImpl implements CareTeamService {

    public static final String STATUS_ACTIVE = "active";
    public static final String CAREMANAGER_ROLE = "171M00000X";
    private final IGenericClient fhirClient;
    private final FhirValidator fhirValidator;
    private final LookUpService lookUpService;
    private final FisProperties fisProperties;
    private final CommunicationService communicationService;

    @Autowired
    public CareTeamServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties, CommunicationService communicationService) {
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.communicationService = communicationService;
    }

    @Override
    public void createCareTeam(CareTeamDto careTeamDto) {
        checkForDuplicates(careTeamDto);

        try {
            final CareTeam careTeam = CareTeamDtoToCareTeamConverter.map(careTeamDto);

            validate(careTeam);

            fhirClient.create().resource(careTeam).execute();

        } catch (FHIRException | ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while creating a care team:" + e.getMessage());

        }
    }

    @Override
    public void updateCareTeam(String careTeamId, CareTeamDto careTeamDto) {
        try {
            careTeamDto.setId(careTeamId);
            final CareTeam careTeam = CareTeamDtoToCareTeamConverter.map(careTeamDto);

            validate(careTeam);

            fhirClient.update().resource(careTeam).execute();

        } catch (FHIRException | ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while creating a care team:" + e.getMessage());

        }
    }

    @Override
    public PageDto<CareTeamDto> getCareTeams(Optional<List<String>> statusList, String searchType, String searchValue, Optional<Integer> page, Optional<Integer> size) {
        int numberOfCareTeamMembersPerPage = PaginationUtil.getValidPageSize(fisProperties, size, ResourceType.CareTeam.name());
        IQuery iQuery = fhirClient.search().forResource(CareTeam.class);

        //Set Sort order
        iQuery = FhirUtil.setLastUpdatedTimeSortOrder(iQuery, true);

        //Check for patient
        if (searchType.equalsIgnoreCase("patientId"))
            iQuery.where(new ReferenceClientParam("patient").hasId("Patient/" + searchValue));

        //Check for status
        if (statusList.isPresent() && !statusList.get().isEmpty()) {
            iQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        }

        Bundle firstPageCareTeamBundle;
        Bundle otherPageCareTeamBundle;
        boolean firstPage = true;

        //Bundle retrieves care team along with its participant and subject
        firstPageCareTeamBundle = (Bundle) iQuery
                .include(CareTeam.INCLUDE_PARTICIPANT)
                .include(CareTeam.INCLUDE_SUBJECT)
                .count(numberOfCareTeamMembersPerPage)
                .returnBundle(Bundle.class).execute();

        if (firstPageCareTeamBundle == null || firstPageCareTeamBundle.getEntry().isEmpty()) {
            log.info("No Care Team members were found for the given criteria.");
            return new PageDto<>(new ArrayList<>(), numberOfCareTeamMembersPerPage, 0, 0, 0, 0);
        }

        otherPageCareTeamBundle = firstPageCareTeamBundle;

        if (page.isPresent() && page.get() > 1 && otherPageCareTeamBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageCareTeamBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageCareTeamBundle, page.get(), numberOfCareTeamMembersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedCareTeamMembers = otherPageCareTeamBundle.getEntry();

        List<CareTeam> careTeams = retrievedCareTeamMembers
                .stream()
                .filter(retrivedBundle -> retrivedBundle.getResource().getResourceType().equals(ResourceType.CareTeam))
                .map(retrievedCareTeamMember -> (CareTeam) retrievedCareTeamMember.getResource())
                .collect(toList());

        List<CareTeamDto> careTeamDtos = careTeams.stream().map(careTeam -> {

            CareTeamDto careTeamDto = new CareTeamDto();
            careTeamDto.setId(careTeam.getIdElement().getIdPart());
            careTeamDto.setName((careTeam.getName() != null && !careTeam.getName().isEmpty()) ? careTeam.getName() : null);
            if (careTeam.getStatus() != null) {
                careTeamDto.setStatusCode((careTeam.getStatus().toCode() != null && !careTeam.getStatus().toCode().isEmpty()) ? careTeam.getStatus().toCode() : null);
                careTeamDto.setStatusDisplay((FhirDtoUtil.getDisplayForCode(careTeam.getStatus().toCode(), lookUpService.getCareTeamStatuses())).orElse(null));
            }

            //Get Category
            careTeam.getCategory().stream().findFirst().ifPresent(category -> category.getCoding().stream().findFirst().ifPresent(coding -> {
                careTeamDto.setCategoryCode((coding.getCode() != null && !coding.getCode().isEmpty()) ? coding.getCode() : null);
                careTeamDto.setCategoryDisplay((FhirDtoUtil.getDisplayForCode(coding.getCode(), lookUpService.getCareTeamCategories())).orElse(null));
            }));
            String subjectReference = careTeam.getSubject().getReference();
            String patientId = subjectReference.substring(subjectReference.lastIndexOf("/") + 1);

            Optional<Bundle.BundleEntryComponent> patientBundleEntryComponent = retrievedCareTeamMembers.stream().filter(careTeamWithItsSubjectAndParticipant -> careTeamWithItsSubjectAndParticipant.getResource().getResourceType().equals(ResourceType.Patient)).filter(patientSubject -> patientSubject.getResource().getIdElement().getIdPart().equalsIgnoreCase(patientId)
            ).findFirst();

            patientBundleEntryComponent.ifPresent(patient -> {
                Patient subjectPatient = (Patient) patient.getResource();

                subjectPatient.getName().stream().findFirst().ifPresent(name -> {
                    careTeamDto.setSubjectLastName((name.getFamily() != null && !name.getFamily().isEmpty()) ? (name.getFamily()) : null);
                    name.getGiven().stream().findFirst().ifPresent(firstname -> careTeamDto.setSubjectFirstName(firstname.toString()));
                });
                careTeamDto.setSubjectId(patientId);
            });

            //Getting the reason codes
            careTeam.getReasonCode().stream().findFirst().ifPresent(reasonCode -> reasonCode.getCoding().stream().findFirst().ifPresent(code -> {
                careTeamDto.setReasonCode((code.getCode() != null && !code.getCode().isEmpty()) ? code.getCode() : null);
                careTeamDto.setReasonDisplay((FhirDtoUtil.getDisplayForCode(code.getCode(), lookUpService.getCareTeamReasons())).orElse(null));
            }));

            //Getting for participant
            List<ParticipantDto> participantDtos = new ArrayList<>();
            careTeam.getParticipant().forEach(participant -> {
                ParticipantDto participantDto = new ParticipantDto();
                //Getting participant role
                if (participant.getRole() != null && !participant.getRole().isEmpty()) {

                    participant.getRole().getCoding().stream().findFirst().ifPresent(participantRole -> {
                        participantDto.setRoleCode((participantRole.getCode() != null && !participantRole.getCode().isEmpty()) ? participantRole.getCode() : null);
                        participantDto.setRoleDisplay((FhirDtoUtil.getDisplayForCode(participantRole.getCode(), lookUpService.getParticipantRoles())).orElse(null));
                    });

                }

                //Getting participant start and end date
                if (participant.getPeriod() != null && !participant.getPeriod().isEmpty()) {
                    participantDto.setStartDate((participant.getPeriod().getStart() != null) ? DateUtil.convertDateToString(participant.getPeriod().getStart()) : null);
                    participantDto.setEndDate((participant.getPeriod().getEnd() != null) ? DateUtil.convertDateToString(participant.getPeriod().getEnd()) : null);
                }

                //Getting participant member and onBehalfof
                if (participant.getMember() != null && !participant.getMember().isEmpty()) {
                    String participantMemberReference = participant.getMember().getReference();
                    String participantId = participantMemberReference.split("/")[1];
                    String participantType = participantMemberReference.split("/")[0];

                    //Getting the member
                    retrievedCareTeamMembers.forEach(careTeamWithItsSubjectAndPartipant -> {
                        Resource resource = careTeamWithItsSubjectAndPartipant.getResource();
                        if (resource.getResourceType().toString().trim().replaceAll(" ", "").equalsIgnoreCase(participantType.trim().replaceAll(" ", ""))) {

                            if (resource.getIdElement().getIdPart().equalsIgnoreCase(participantId)) {
                                switch (resource.getResourceType()) {
                                    case Patient:
                                        Patient patient = (Patient) resource;
                                        patient.getName().stream().findFirst().ifPresent(name -> {
                                            name.getGiven().stream().findFirst().ifPresent(firstName -> participantDto.setMemberFirstName(Optional.ofNullable(firstName.toString())));
                                            participantDto.setMemberLastName(Optional.ofNullable(name.getFamily()));
                                        });
                                        participantDto.setMemberId(participantId);
                                        participantDto.setMemberType(patient.fhirType());
                                        break;

                                    case Practitioner:
                                        Practitioner practitioner = (Practitioner) resource;
                                        practitioner.getName().stream().findFirst().ifPresent(name -> {
                                            name.getGiven().stream().findFirst().ifPresent(firstName -> participantDto.setMemberFirstName(Optional.ofNullable(firstName.toString())));
                                            participantDto.setMemberLastName(Optional.ofNullable(name.getFamily()));
                                        });
                                        participantDto.setMemberId(participantId);
                                        participantDto.setMemberType(practitioner.fhirType());

                                        if (participant.getOnBehalfOf() != null && !participant.getOnBehalfOf().isEmpty()) {
                                            String organizationId = participant.getOnBehalfOf().getReference().split("/")[1];

                                            Bundle organizationBundle = (Bundle) fhirClient.search().forResource(Organization.class)
                                                    .where(new TokenClientParam("_id").exactly().code(organizationId))
                                                    .prettyPrint()
                                                    .execute();
                                            Optional<Resource> organizationResource = organizationBundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).findFirst();
                                            Organization organization = (Organization) organizationResource.get();

                                            participantDto.setOnBehalfOfId(organizationId);
                                            participantDto.setOnBehalfOfName(organization.getName());
                                        }
                                        break;

                                    case Organization:
                                        Organization organization = (Organization) resource;
                                        participantDto.setMemberName(Optional.ofNullable(organization.getName()));
                                        participantDto.setMemberId(participantId);
                                        participantDto.setMemberType(organization.fhirType());
                                        break;

                                    case RelatedPerson:
                                        RelatedPerson relatedPerson = (RelatedPerson) resource;
                                        relatedPerson.getName().stream().findFirst().ifPresent(name -> {
                                            name.getGiven().stream().findFirst().ifPresent(firstName -> participantDto.setMemberFirstName(Optional.ofNullable(firstName.toString())));
                                            participantDto.setMemberLastName(Optional.ofNullable(name.getFamily()));
                                        });
                                        participantDto.setMemberId(participantId);
                                        participantDto.setMemberType(relatedPerson.fhirType());
                                        break;
                                }
                            }
                        }

                    });

                }

                participantDtos.add(participantDto);
            });

            careTeamDto.setParticipants(participantDtos);
            if (careTeam.getPeriod() != null && !careTeam.getPeriod().isEmpty()) {
                careTeamDto.setStartDate((careTeam.getPeriod().getStart() != null) ? DateUtil.convertDateToString(careTeam.getPeriod().getStart()) : null);
                careTeamDto.setEndDate((careTeam.getPeriod().getEnd() != null) ? DateUtil.convertDateToString(careTeam.getPeriod().getEnd()) : null);
            }
            return careTeamDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageCareTeamBundle.getTotal() / numberOfCareTeamMembersPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(careTeamDtos, numberOfCareTeamMembersPerPage, totalPages, currentPage, careTeamDtos.size(), otherPageCareTeamBundle.getTotal());
    }

    @Override
    public List<ParticipantReferenceDto> getCareTeamParticipants(String patient, Optional<List<String>> roles, Optional<String> name, Optional<String> communication) {
        List<ReferenceDto> participantsByRoles = new ArrayList<>();
        List<ParticipantReferenceDto> participantsSelected = new ArrayList<>();

        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new ReferenceClientParam("patient").hasId("Patient/" + patient))
                .include(CareTeam.INCLUDE_PARTICIPANT)
                .returnBundle(Bundle.class).execute();

        if (careTeamBundle != null) {
            List<Bundle.BundleEntryComponent> retrievedCareTeams = careTeamBundle.getEntry();

            if (retrievedCareTeams != null) {
                List<CareTeam> careTeams = retrievedCareTeams.stream()
                        .filter(bundle -> bundle.getResource().getResourceType().equals(ResourceType.CareTeam))
                        .map(careTeamMember -> (CareTeam) careTeamMember.getResource()).collect(toList());


                participantsByRoles = careTeams.stream()
                        .flatMap(it -> CareTeamToCareTeamDtoConverter.mapToParticipants(it, roles, name).stream()).collect(toList());

            }
        }

        //retrieve recipients by Id
        List<String> recipients = new ArrayList<>();

        if (communication.isPresent()) {

            recipients = communicationService.getRecipientsByCommunicationId(patient, communication.get());

        }

        for (ReferenceDto participant : participantsByRoles) {
            ParticipantReferenceDto participantReferenceDto = new ParticipantReferenceDto();
            participantReferenceDto.setReference(participant.getReference());
            participantReferenceDto.setDisplay(participant.getDisplay());

            if (recipients.contains(FhirDtoUtil.getIdFromParticipantReferenceDto(participant))) {
                participantReferenceDto.setSelected(true);
            }
            participantsSelected.add(participantReferenceDto);
        }

        return participantsSelected;
    }

    @Override
    public CareTeamDto getCareTeamById(String careTeamById) {
        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new TokenClientParam("_id").exactly().code(careTeamById))
                .include(CareTeam.INCLUDE_PARTICIPANT)
                .include(CareTeam.INCLUDE_SUBJECT)
                .returnBundle(Bundle.class)
                .execute();

        if (careTeamBundle == null || careTeamBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No CareTeam was found for the given careTeamID : " + careTeamById);
        }

        CareTeam careTeam = (CareTeam) careTeamBundle.getEntry().get(0).getResource();

        return convertCareTeamToCareTeamDto(careTeam);
    }

    @Override
    public List<ReferenceDto> getPatientsInCareTeamsByPractitioner(String practitioner) {
        List<ReferenceDto> patients = new ArrayList<>();

        Bundle bundle = fhirClient.search().forResource(CareTeam.class)
                .where(new ReferenceClientParam("participant").hasId(practitioner))
                .include(CareTeam.INCLUDE_PATIENT)
                .include(CareTeam.INCLUDE_PARTICIPANT)
                .returnBundle(Bundle.class)
                .execute();

        if (bundle != null) {
            List<Bundle.BundleEntryComponent> components = bundle.getEntry();
            if (components != null) {
                patients = components.stream()
                        .filter(it -> it.getResource().getResourceType().equals(ResourceType.CareTeam))
                        .map(it -> (CareTeam) it.getResource())
                        .filter(it -> checkIfParticipantIsCareManager(it.getParticipant()))
                        .map(it -> (Patient) it.getSubject().getResource())
                        .map(FhirDtoUtil::mapPatientToReferenceDto)
                        .collect(toList());
            }
        }

        return patients;
    }

    public PageDto<CareTeamDto> getCareTeamsByPatientAndOrganization(String patient, Optional<String> organization, Optional<List<String>> status, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfCareTeamsPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.CareTeam.name());

        IQuery iQuery = fhirClient.search().forResource(CareTeam.class)
                .where(new ReferenceClientParam("patient").hasId(patient))
                .include(CareTeam.INCLUDE_SUBJECT)
                .include(CareTeam.INCLUDE_PARTICIPANT);

        if (status.isPresent() && !status.get().isEmpty()) {
            iQuery.where(new TokenClientParam("status").exactly().codes(status.get()));
        }

        Bundle bundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();

        List<Bundle.BundleEntryComponent> components = FhirUtil.getAllBundleComponentsAsList(bundle, Optional.empty(), fhirClient, fisProperties);

        List<CareTeam> careTeams = components.stream()
                .filter(it -> it.getResource().getResourceType().equals(ResourceType.CareTeam))
                .map(it -> (CareTeam) it.getResource())
                .filter(it -> {
                    if (organization.isPresent()) {
                        List<Reference> managingOrganizations = it.getManagingOrganization();
                        return managingOrganizations.stream().anyMatch(managingOrganization -> managingOrganization.getReference().contains(organization.get()));
                    } else {
                        //do not filter
                        return true;
                    }
                })
                .collect(toList());

        List<CareTeamDto> careTeamDtos = careTeams.stream()
                .map(this::convertCareTeamToCareTeamDto)
                .collect(toList());

        return (PageDto<CareTeamDto>) PaginationUtil.applyPaginationForCustomArrayList(careTeamDtos, numberOfCareTeamsPerPage, pageNumber, false);
    }

    @Override
    public void addRelatedPerson(String careTeamId, ParticipantDto participantDto) {
        CareTeam careTeam = fhirClient.read().resource(CareTeam.class).withId(careTeamId).execute();

        List<CareTeam.CareTeamParticipantComponent> components = careTeam.getParticipant();
        components.add(convertParticipantDtoToParticipant(participantDto));
        careTeam.setParticipant(components);
        validate(careTeam);
        fhirClient.update().resource(careTeam).execute();

    }

    @Override
    public void removeRelatedPerson(String careTeamId, ParticipantDto participantDto) {
        CareTeam careTeam = fhirClient.read().resource(CareTeam.class).withId(careTeamId).execute();

        List<CareTeam.CareTeamParticipantComponent> components = careTeam.getParticipant();
        components.removeIf(com -> com.getMember().getReference().split("/")[1].equals(participantDto.getMemberId()));

        careTeam.setParticipant(components);

        validate(careTeam);
        fhirClient.update().resource(careTeam).execute();
    }

    @Override
    public PageDto<ParticipantDto> getRelatedPersonsByIdForEdit(String careTeamId, Optional<String> name, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfRelatedPersonPerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.RelatedPerson.name());
        CareTeamDto careTeamDto = getCareTeamById(careTeamId);
        List<ParticipantDto> participantInCareTeam = careTeamDto.getParticipants();

        //Get all the relatedPerson for the patient
        IQuery iQuery = fhirClient.search().forResource(RelatedPerson.class)
                .where(new ReferenceClientParam("patient").hasId(careTeamDto.getSubjectId()));


        name.ifPresent(n->iQuery.where(new RichStringClientParam("name").contains().value(name.get().trim())));
        Bundle relatedPersonForPatientBundle = (Bundle) iQuery.returnBundle(Bundle.class).execute();


        List<ParticipantDto> participantDtoFromRelatedPersons = FhirUtil.getAllBundleComponentsAsList(relatedPersonForPatientBundle, Optional.empty(), fhirClient, fisProperties)
                .stream().map(rp -> (RelatedPerson) rp.getResource())
                .map(rp -> {
                    ParticipantDto participantDto = new ParticipantDto();
                    participantDto.setMemberType(ParticipantTypeEnum.relatedPerson.getCode());
                    rp.getName().stream().findFirst().ifPresent(r -> {
                        participantDto.setMemberLastName(Optional.ofNullable(r.getFamily()));
                        r.getGiven().stream().findFirst().ifPresent(given -> participantDto.setMemberFirstName(Optional.ofNullable(given.toString())));
                    });
                    participantDto.setMemberId(rp.getIdElement().getIdPart());
                    participantDto.setIsInCareTeam(Optional.of(false));
                    return participantDto;
                }).collect(toList());


        List<ParticipantDto> participantDtoList = new ArrayList<>();
        participantDtoFromRelatedPersons.forEach(rp -> {
            String memberId = rp.getMemberId();
            Optional<ParticipantDto> participantDto = participantInCareTeam.stream().filter(p -> p.getMemberId().equalsIgnoreCase(memberId)).findFirst();
            if (participantDto.isPresent()) {
                ParticipantDto par = participantDto.get();
                par.setIsInCareTeam(Optional.of(true));
                participantDtoList.add(par);
            } else {
                ParticipantDto par = rp;
                participantDtoList.add(par);
            }
        });

        return (PageDto<ParticipantDto>) PaginationUtil.applyPaginationForCustomArrayList(participantDtoList, numberOfRelatedPersonPerPage, pageNumber, false);
    }

    private CareTeamDto convertCareTeamToCareTeamDto(CareTeam careTeam) {
        final CareTeamDto careTeamDto = CareTeamToCareTeamDtoConverter.map(careTeam);

        if (careTeamDto.getStatusCode() != null) {
            careTeamDto.setStatusDisplay((FhirDtoUtil.getDisplayForCode(careTeamDto.getStatusCode(), lookUpService.getCareTeamStatuses())).orElse(null));
        }

        if (careTeamDto.getCategoryCode() != null) {
            careTeamDto.setCategoryDisplay((FhirDtoUtil.getDisplayForCode(careTeamDto.getCategoryCode(), lookUpService.getCareTeamCategories())).orElse(null));
        }

        if (careTeamDto.getReasonCode() != null) {
            careTeamDto.setReasonDisplay((FhirDtoUtil.getDisplayForCode(careTeamDto.getReasonCode(), lookUpService.getCareTeamReasons())).orElse(null));
        }

        for (ParticipantDto dto : careTeamDto.getParticipants()) {
            if (dto.getRoleCode() != null) {
                dto.setRoleDisplay((FhirDtoUtil.getDisplayForCode(dto.getRoleCode(), lookUpService.getParticipantRoles())).orElse(null));
            }
        }

        return careTeamDto;
    }

    private boolean checkIfParticipantIsCareManager(List<CareTeam.CareTeamParticipantComponent> components) {
        //write logic to check if each participant, if of type Practitioner, is a CareManager or not
        return components.stream()
                .map(component -> {
                    Reference member = component.getMember();
                    String role = "";
                    if (member.getReference().contains(ResourceType.Practitioner.toString())) {
                        role = FhirUtil.getRoleFromCodeableConcept(component.getRole());
                    }
                    return role;
                })
                .anyMatch(t -> t.contains(CAREMANAGER_ROLE));
    }

    private void checkForDuplicates(CareTeamDto careTeamDto) {
        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new TokenClientParam(CareTeamFieldEnum.STATUS.getCode()).exactly().code(STATUS_ACTIVE))
                .and(new TokenClientParam(CareTeamFieldEnum.SUBJECT.getCode()).exactly().code(careTeamDto.getSubjectId()))
                .and(new TokenClientParam(CareTeamFieldEnum.CATEGORY.getCode()).exactly().code(careTeamDto.getCategoryCode()))
                .returnBundle(Bundle.class)
                .execute();

        log.info("Existing CareTeam size : " + careTeamBundle.getEntry().size());
        if (careTeamBundle.getEntry().size() > 1) {
            throw new DuplicateResourceFoundException("CareTeam already exists with the given subject ID and category Code in active status");
        }
    }

    private void validate(CareTeam careTeam) {
        final ValidationResult validationResult = fhirValidator.validateWithResult(careTeam);

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("FHIR CareTeam validation is not successful" + validationResult.getMessages());
        }
    }

    private CareTeam.CareTeamParticipantComponent convertParticipantDtoToParticipant(ParticipantDto participantDto) {

        CareTeam.CareTeamParticipantComponent careTeamParticipant = new CareTeam.CareTeamParticipantComponent();

        String memberType = participantDto.getMemberType();

        if (memberType.equalsIgnoreCase(ParticipantTypeEnum.practitioner.getCode())) {
            careTeamParticipant.getMember().setReference(ParticipantTypeEnum.practitioner.getName() + "/" + participantDto.getMemberId());

        } else if (memberType.equalsIgnoreCase(ParticipantTypeEnum.patient.getCode())) {
            careTeamParticipant.getMember().setReference(ParticipantTypeEnum.patient.getName() + "/" + participantDto.getMemberId());

        } else if (memberType.equalsIgnoreCase(ParticipantTypeEnum.organization.getCode())) {
            careTeamParticipant.getMember().setReference(ParticipantTypeEnum.organization.getName() + "/" + participantDto.getMemberId());

        } else if (memberType.equalsIgnoreCase(ParticipantTypeEnum.relatedPerson.getCode())) {
            careTeamParticipant.getMember().setReference(ParticipantTypeEnum.relatedPerson.getName() + "/" + participantDto.getMemberId());
        }

        Coding codingRoleCode = new Coding();
        codingRoleCode.setCode(participantDto.getRoleCode());
        CodeableConcept codeableConceptRoleCode = new CodeableConcept().addCoding(codingRoleCode);
        careTeamParticipant.setRole(codeableConceptRoleCode);

        Period participantPeriod = new Period();
        try {
            participantPeriod.setStart(DateUtil.convertStringToDate(participantDto.getStartDate()));
            participantPeriod.setEnd(DateUtil.convertStringToDate(participantDto.getEndDate()));
            careTeamParticipant.setPeriod(participantPeriod);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return careTeamParticipant;
    }


}

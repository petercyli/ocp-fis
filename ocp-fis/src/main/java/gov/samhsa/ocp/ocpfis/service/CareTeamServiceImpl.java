package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.domain.CareTeamFieldEnum;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.CommunicationReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
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
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
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


        List<CareTeamDto> careTeamDtos = retrievedCareTeamMembers.stream().filter(retrivedBundle -> retrivedBundle.getResource().getResourceType().equals(ResourceType.CareTeam)).map(retrievedCareTeamMember -> {
            CareTeam careTeam = (CareTeam) retrievedCareTeamMember.getResource();
            CareTeamDto careTeamDto = new CareTeamDto();
            careTeamDto.setId(careTeam.getIdElement().getIdPart());
            careTeamDto.setName((careTeam.getName() != null && !careTeam.getName().isEmpty()) ? careTeam.getName() : null);
            if (careTeam.getStatus() != null) {
                careTeamDto.setStatusCode((careTeam.getStatus().toCode() != null && !careTeam.getStatus().toCode().isEmpty()) ? careTeam.getStatus().toCode() : null);
                careTeamDto.setStatusDisplay((FhirDtoUtil.getDisplayForCode(careTeam.getStatus().toCode(), Optional.ofNullable(lookUpService.getCareTeamStatuses()))).orElse(null));
            }

            //Get Category
            careTeam.getCategory().stream().findFirst().ifPresent(category -> category.getCoding().stream().findFirst().ifPresent(coding -> {
                careTeamDto.setCategoryCode((coding.getCode() != null && !coding.getCode().isEmpty()) ? coding.getCode() : null);
                careTeamDto.setCategoryDisplay((FhirDtoUtil.getDisplayForCode(coding.getCode(), Optional.ofNullable(lookUpService.getCareTeamCategories()))).orElse(null));
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
                careTeamDto.setReasonDisplay((FhirDtoUtil.getDisplayForCode(code.getCode(), Optional.ofNullable(lookUpService.getCareTeamReasons()))).orElse(null));
            }));

            //Getting for participant
            List<ParticipantDto> participantDtos = new ArrayList<>();
            careTeam.getParticipant().forEach(participant -> {
                ParticipantDto participantDto = new ParticipantDto();
                //Getting participant role
                if (participant.getRole() != null && !participant.getRole().isEmpty()) {

                    participant.getRole().getCoding().stream().findFirst().ifPresent(participantRole -> {
                        participantDto.setRoleCode((participantRole.getCode() != null && !participantRole.getCode().isEmpty()) ? participantRole.getCode() : null);
                        participantDto.setRoleDisplay((FhirDtoUtil.getDisplayForCode(participantRole.getCode(), Optional.ofNullable(lookUpService.getParticipantRoles()))).orElse(null));
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
    public List<CommunicationReferenceDto> getCareTeamParticipants(String patient, Optional<List<String>> roles, Optional<String> communication) {
        List<ReferenceDto> participantsByRoles = new ArrayList<>();
        List<CommunicationReferenceDto> participantsSelected = new ArrayList<>();

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
                        .flatMap(it -> CareTeamToCareTeamDtoConverter.mapToParticipants(it, roles).stream()).collect(toList());
            }
        }

        //retrieve recipients by Id
        List<String> recipients = new ArrayList<>();

        if (communication.isPresent()) {

            recipients = communicationService.getRecipientsByCommunicationId(patient, communication.get());

        }

        for (ReferenceDto participant : participantsByRoles) {
            CommunicationReferenceDto communicationReferenceDto = new CommunicationReferenceDto();
            communicationReferenceDto.setReference(participant.getReference());
            communicationReferenceDto.setDisplay(participant.getDisplay());

            if (recipients.contains(FhirDtoUtil.getIdFromParticipantReferenceDto(participant))) {
                communicationReferenceDto.setSelected(true);
            }
            participantsSelected.add(communicationReferenceDto);
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

        final CareTeamDto careTeamDto = CareTeamToCareTeamDtoConverter.map(careTeam);

        if (careTeamDto.getStatusCode() != null) {
            careTeamDto.setStatusDisplay((FhirDtoUtil.getDisplayForCode(careTeamDto.getStatusCode(), Optional.ofNullable(lookUpService.getCareTeamStatuses()))).orElse(null));
        }

        if (careTeamDto.getCategoryCode() != null) {
            careTeamDto.setCategoryDisplay((FhirDtoUtil.getDisplayForCode(careTeamDto.getCategoryCode(), Optional.ofNullable(lookUpService.getCareTeamCategories()))).orElse(null));
        }

        if (careTeamDto.getReasonCode() != null) {
            careTeamDto.setReasonDisplay((FhirDtoUtil.getDisplayForCode(careTeamDto.getReasonCode(), Optional.ofNullable(lookUpService.getCareTeamReasons()))).orElse(null));
        }

        for (ParticipantDto dto : careTeamDto.getParticipants()) {
            if (dto.getRoleCode() != null) {
                dto.setRoleDisplay((FhirDtoUtil.getDisplayForCode(dto.getRoleCode(), Optional.ofNullable(lookUpService.getParticipantRoles()))).orElse(null));
            }
        }

        return careTeamDto;
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
                        .map(it -> FhirDtoUtil.mapPatientToReferenceDto(it))
                        .collect(toList());
            }
        }

        return patients;
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


}

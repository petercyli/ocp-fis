package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.CareTeamDtoToCareTeamConverter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class CareTeamServiceImpl implements CareTeamService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    private final LookUpService lookUpService;

    @Autowired
    public CareTeamServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties, LookUpService lookUpService) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.lookUpService = lookUpService;
    }

    @Override
    public void createCareTeam(CareTeamDto careTeamDto) {
        checkForDuplicates(careTeamDto);

        try {
            final CareTeam careTeam = CareTeamDtoToCareTeamConverter.map(careTeamDto);

            validate(careTeam);

            fhirClient.create().resource(careTeam).execute();

        } catch (FHIRException e) {
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

        } catch (FHIRException e) {
            throw new FHIRClientException("FHIR Client returned with an error while creating a care team:" + e.getMessage());

        }
    }

    @Override
    public PageDto<CareTeamDto> getCareTeam(Optional<List<String>> statusList, String searchType, String searchValue, Optional<Integer> page, Optional<Integer> size) {
        int numberOfCareTeamMembersPerPage = size.filter(s -> s > 0 &&
                s <= fisProperties.getPractitioner().getPagination().getMaxSize()).orElse(fisProperties.getPractitioner().getPagination().getDefaultSize());

        IQuery iQuery = fhirClient.search().forResource(CareTeam.class);

        //Check for patient
        if (searchType.equalsIgnoreCase("patientId"))
            iQuery.where(new ReferenceClientParam("patient").hasId("Patient/" + searchValue));

        //Check for status
        if (statusList.isPresent() && statusList.get().size() > 0) {
            iQuery.where(new TokenClientParam("status").exactly().codes(statusList.get()));
        }


        Bundle firstPageCareTeamBundle;
        Bundle otherPageCareTeamBundle;
        boolean firstPage = true;

        firstPageCareTeamBundle = (Bundle) iQuery
                .count(numberOfCareTeamMembersPerPage)
                .returnBundle(Bundle.class).execute();

        if (firstPageCareTeamBundle == null || firstPageCareTeamBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No Care Team members were found in the FHIR server.");
        }

        otherPageCareTeamBundle = firstPageCareTeamBundle;

        if (page.isPresent() && page.get() > 1 && otherPageCareTeamBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageCareTeamBundle = getCareTeamBundleAfterFirstPage(firstPageCareTeamBundle, page.get(), numberOfCareTeamMembersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedCareTeamMembers = otherPageCareTeamBundle.getEntry();

        IQuery careTeamWithItsSubjectAndParticipantQuery = iQuery.include(CareTeam.INCLUDE_PARTICIPANT)
                .include(CareTeam.INCLUDE_SUBJECT);

        Bundle careTeamWithItsSubjectAndParticipantBundleForTotalEntry = (Bundle) careTeamWithItsSubjectAndParticipantQuery
                .returnBundle(Bundle.class)
                .execute();

        int totalEntry = careTeamWithItsSubjectAndParticipantBundleForTotalEntry.getTotal();

        Bundle careTeamWithItsSubjectAndParticipantBundle = (Bundle) careTeamWithItsSubjectAndParticipantQuery
                .count(totalEntry)
                .returnBundle(Bundle.class)
                .execute();

        List<CareTeamDto> careTeamDtos = retrievedCareTeamMembers.stream().map(retrievedCareTeamMember -> {
            CareTeam careTeam = (CareTeam) retrievedCareTeamMember.getResource();
            CareTeamDto careTeamDto = new CareTeamDto();
            careTeamDto.setId(careTeam.getIdElement().getIdPart());
            careTeamDto.setName((careTeam.getName() != null && !careTeam.getName().isEmpty()) ? careTeam.getName() : "");
            if (careTeam.getStatus() != null) {
                careTeamDto.setStatusCode((careTeam.getStatus().toCode() != null && !careTeam.getStatus().toCode().isEmpty()) ? careTeam.getStatus().toCode() : "");
                careTeamDto.setStatusDisplay((getCareTeamDisplay(careTeam.getStatus().toCode(), lookUpService.getCareTeamStatuses())).orElse(""));
            }
            careTeam.getCategory().stream().findFirst().ifPresent(category -> {
                category.getCoding().stream().findFirst().ifPresent(coding -> {
                    careTeamDto.setCategoryCode((coding.getCode() != null && !coding.getCode().isEmpty()) ? coding.getCode() : "");
                    careTeamDto.setCategoryDisplay((getCareTeamDisplay(coding.getCode(), lookUpService.getCareTeamCategories())).orElse(""));
                });
            });
            String subjectReference = careTeam.getSubject().getReference();
            String patientId = subjectReference.substring(subjectReference.lastIndexOf("/") + 1);

            Optional<Bundle.BundleEntryComponent> patientBundleEntryComponent = careTeamWithItsSubjectAndParticipantBundle.getEntry().stream().filter(careTeamWithItsSubjectAndParticipant -> {
                        return careTeamWithItsSubjectAndParticipant.getResource().getIdElement().getIdPart().equalsIgnoreCase(patientId);
                    }
            ).findFirst();

            patientBundleEntryComponent.ifPresent(patient -> {
                Patient subjectPatient = (Patient) patient.getResource();

                subjectPatient.getName().stream().findFirst().ifPresent(name -> {
                    careTeamDto.setSubjectLastName((name.getFamily() != null && !name.getFamily().isEmpty()) ? (name.getFamily()) : "");
                    name.getGiven().stream().findFirst().ifPresent(firstname -> {
                        careTeamDto.setSubjectFirstName(firstname.toString());
                    });
                });
                careTeamDto.setSubjectId(patientId);
            });


            //Getting for participant
            List<ParticipantDto> participantDtos = new ArrayList<>();
            careTeam.getParticipant().forEach(participant -> {
                ParticipantDto participantDto = new ParticipantDto();
                //Getting participant role
                if (participant.getRole() != null && !participant.getRole().isEmpty()) {

                    participant.getRole().getCoding().stream().findFirst().ifPresent(participantRole -> {
                        participantDto.setRoleCode((participantRole.getCode() != null && !participantRole.getCode().isEmpty()) ? participantRole.getCode() : "");
                        participantDto.setRoleDisplay((participantRole.getDisplay() != null && !participantRole.getDisplay().isEmpty()) ? participantRole.getDisplay() : "");
                    });

                }

                //Getting participant member and onBehalfof
                if (participant.getMember() != null && !participant.getMember().isEmpty()) {
                    String participantMemberReference = participant.getMember().getReference();
                    String participantId = participantMemberReference.split("/")[1];
                    String participantType = participantMemberReference.split("/")[0];

                    //Getting the member
                    careTeamWithItsSubjectAndParticipantBundle.getEntry().forEach(careTeamWithItsSubjectAndPartipant -> {
                        Resource resource = careTeamWithItsSubjectAndPartipant.getResource();
                        if (resource.getResourceType().toString().trim().replaceAll(" ", "").equalsIgnoreCase(participantType.trim().replaceAll(" ", ""))) {

                            if (resource.getIdElement().getIdPart().equalsIgnoreCase(participantId)) {
                                switch (resource.getResourceType()) {
                                    case Patient:
                                        Patient patient = (Patient) resource;
                                        patient.getName().stream().findFirst().ifPresent(name->{
                                            name.getGiven().stream().findFirst().ifPresent(firstname->{
                                                participantDto.setMemberFirstName(Optional.ofNullable(firstname.toString()));
                                            });
                                            participantDto.setMemberLastName(Optional.ofNullable(name.getFamily()));
                                        });
                                        participantDto.setMemberId(participantId);
                                        participantDto.setMemberType(patient.fhirType());
                                        break;

                                    case Practitioner:
                                        Practitioner practitioner = (Practitioner) resource;
                                        practitioner.getName().stream().findFirst().ifPresent(name->{
                                            name.getGiven().stream().findFirst().ifPresent(firstname->{
                                                participantDto.setMemberFirstName(Optional.ofNullable(firstname.toString()));
                                            });
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

                                }
                            }
                        }

                    });

                }

                participantDtos.add(participantDto);
            });

            careTeamDto.setParticipants(participantDtos);
            return careTeamDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageCareTeamBundle.getTotal() / numberOfCareTeamMembersPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(careTeamDtos, numberOfCareTeamMembersPerPage, totalPages, currentPage, careTeamDtos.size(), otherPageCareTeamBundle.getTotal());
    }

    private void checkForDuplicates(CareTeamDto careTeamDto) {

    }

    private void validate(CareTeam careTeam) {
        final ValidationResult validationResult = fhirValidator.validateWithResult(careTeam);

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("FHIR CareTeam validation is not successful" + validationResult.getMessages());
        }
    }

    private Optional<String> getCareTeamDisplay(String code, List<ValueSetDto> lookupValues) {
        return lookupValues.stream().filter(lookupValue -> code.equalsIgnoreCase(lookupValue.getCode())).map(valueSet -> valueSet.getDisplay()).findFirst();
    }

    private Bundle getCareTeamBundleAfterFirstPage(Bundle careTeamBundle, int page, int size) {
        if (careTeamBundle.getLink(Bundle.LINK_NEXT) != null) {

            int offset = ((page >= 1 ? page : 1) - 1) * size;

            if (offset >= careTeamBundle.getTotal()) {
                throw new ResourceNotFoundException("No Care Team members were found in the FHIR server.");
            }

            String pageUrl = fisProperties.getFhir().getServerUrl()
                    + "?_getpages=" + careTeamBundle.getId()
                    + "&_getpagesoffset=" + offset
                    + "&_count=" + size
                    + "&_bundletype=searchset";

            // Load the required page
            return fhirClient.search().byUrl(pageUrl)
                    .returnBundle(Bundle.class)
                    .execute();
        }
        return careTeamBundle;
    }


}

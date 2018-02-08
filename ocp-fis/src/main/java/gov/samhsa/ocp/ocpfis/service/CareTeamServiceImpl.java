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
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.CareTeamToCareTeamDtoConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.CareTeamDtoToCareTeamConverter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
public class CareTeamServiceImpl implements CareTeamService {

    public static final String STATUS_ACTIVE = "active";

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
        int numberOfCareTeamMembersPerPage = size.filter(s -> s > 0 &&
                s <= fisProperties.getCareTeam().getPagination().getMaxSize()).orElse(fisProperties.getCareTeam().getPagination().getDefaultSize());

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

        firstPageCareTeamBundle = (Bundle) iQuery
                .count(numberOfCareTeamMembersPerPage)
                .returnBundle(Bundle.class).execute();

        if (firstPageCareTeamBundle == null || firstPageCareTeamBundle.getEntry().isEmpty()) {
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
            careTeamDto.setName((careTeam.getName() != null && !careTeam.getName().isEmpty()) ? careTeam.getName() : null);
            if (careTeam.getStatus() != null) {
                careTeamDto.setStatusCode((careTeam.getStatus().toCode() != null && !careTeam.getStatus().toCode().isEmpty()) ? careTeam.getStatus().toCode() : null);
                careTeamDto.setStatusDisplay((getCareTeamDisplay(careTeam.getStatus().toCode(), Optional.ofNullable(lookUpService.getCareTeamStatuses()))).orElse(null));
            }

            //Get Category
            careTeam.getCategory().stream().findFirst().ifPresent(category -> {
                category.getCoding().stream().findFirst().ifPresent(coding -> {
                    careTeamDto.setCategoryCode((coding.getCode() != null && !coding.getCode().isEmpty()) ? coding.getCode() : null);
                    careTeamDto.setCategoryDisplay((getCareTeamDisplay(coding.getCode(), Optional.ofNullable(lookUpService.getCareTeamCategories()))).orElse(null));
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
                    careTeamDto.setSubjectLastName((name.getFamily() != null && !name.getFamily().isEmpty()) ? (name.getFamily()) : null);
                    name.getGiven().stream().findFirst().ifPresent(firstname -> {
                        careTeamDto.setSubjectFirstName(firstname.toString());
                    });
                });
                careTeamDto.setSubjectId(patientId);
            });

            //Getting the reason codes
            careTeam.getReasonCode().stream().findFirst().ifPresent(reasonCode->{
                reasonCode.getCoding().stream().findFirst().ifPresent(code->{
                    careTeamDto.setReasonCode((code.getCode() !=null && !code.getCode().isEmpty())?code.getCode():null);
                    careTeamDto.setReasonDisplay((getCareTeamDisplay(code.getCode(), Optional.ofNullable(lookUpService.getCareTeamReasons()))).orElse(null));
                });
            });

            //Getting for participant
            List<ParticipantDto> participantDtos = new ArrayList<>();
            careTeam.getParticipant().forEach(participant -> {
                ParticipantDto participantDto = new ParticipantDto();
                //Getting participant role
                if (participant.getRole() != null && !participant.getRole().isEmpty()) {

                    participant.getRole().getCoding().stream().findFirst().ifPresent(participantRole -> {
                        participantDto.setRoleCode((participantRole.getCode() != null && !participantRole.getCode().isEmpty()) ? participantRole.getCode() : null);
                        participantDto.setRoleDisplay((participantRole.getDisplay() != null && !participantRole.getDisplay().isEmpty()) ? participantRole.getDisplay() : null);
                    });

                }

                //Getting participant start and end date
                if(participant.getPeriod() !=null && !participant.getPeriod().isEmpty()){
                    participantDto.setStartDate((participant.getPeriod().getStart()!=null)? convertDateToString(participant.getPeriod().getStart()) :null);
                    participantDto.setEndDate((participant.getPeriod().getEnd()!=null)?convertDateToString(participant.getPeriod().getEnd()): null );
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
                                        patient.getName().stream().findFirst().ifPresent(name -> {
                                            name.getGiven().stream().findFirst().ifPresent(firstName -> {
                                                participantDto.setMemberFirstName(Optional.ofNullable(firstName.toString()));
                                            });
                                            participantDto.setMemberLastName(Optional.ofNullable(name.getFamily()));
                                        });
                                        participantDto.setMemberId(participantId);
                                        participantDto.setMemberType(patient.fhirType());
                                        break;

                                    case Practitioner:
                                        Practitioner practitioner = (Practitioner) resource;
                                        practitioner.getName().stream().findFirst().ifPresent(name -> {
                                            name.getGiven().stream().findFirst().ifPresent(firstName -> {
                                                participantDto.setMemberFirstName(Optional.ofNullable(firstName.toString()));
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

                                    case RelatedPerson:
                                        RelatedPerson relatedPerson= (RelatedPerson) resource;
                                        relatedPerson.getName().stream().findFirst().ifPresent(name -> {
                                            name.getGiven().stream().findFirst().ifPresent(firstName -> {
                                                participantDto.setMemberFirstName(Optional.ofNullable(firstName.toString()));
                                            });
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
            if(careTeam.getPeriod()!=null &&!careTeam.getPeriod().isEmpty()) {
                careTeamDto.setStartDate((careTeam.getPeriod().getStart() != null) ? convertDateToString(careTeam.getPeriod().getStart()) : null);
                careTeamDto.setEndDate((careTeam.getPeriod().getEnd() != null) ? convertDateToString(careTeam.getPeriod().getEnd()):null);
            }
            return careTeamDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageCareTeamBundle.getTotal() / numberOfCareTeamMembersPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(careTeamDtos, numberOfCareTeamMembersPerPage, totalPages, currentPage, careTeamDtos.size(), otherPageCareTeamBundle.getTotal());
    }

    @Override
    public CareTeamDto getCareTeamById(String careTeamById) {
        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new TokenClientParam("_id").exactly().code(careTeamById))
                .returnBundle(Bundle.class)
                .execute();

        if (careTeamBundle == null || careTeamBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No CareTeam was found for the given careTeamID : " + careTeamById);
        }

        CareTeam careTeam = (CareTeam) careTeamBundle.getEntry().get(0).getResource();

        final CareTeamDto careTeamDto = CareTeamToCareTeamDtoConverter.map(careTeam);

        return careTeamDto;
    }

    private void checkForDuplicates(CareTeamDto careTeamDto) {
        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new TokenClientParam(CareTeamFieldEnum.STATUS.getCode()).exactly().code(STATUS_ACTIVE))
                .and(new TokenClientParam(CareTeamFieldEnum.SUBJECT.getCode()).exactly().code(careTeamDto.getSubjectId()))
                .and(new TokenClientParam(CareTeamFieldEnum.CATEGORY.getCode()).exactly().code(careTeamDto.getCategoryCode()))
                .returnBundle(Bundle.class)
                .execute();

        log.info("Existing CareTeam size : " + careTeamBundle.getEntry().size());
        if (careTeamBundle != null && careTeamBundle.getEntry().size() > 1) {
            throw new DuplicateResourceFoundException("CareTeam already exists with the given subject ID and category Code in active status");
        }
    }

    private void validate(CareTeam careTeam) {
        final ValidationResult validationResult = fhirValidator.validateWithResult(careTeam);

        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("FHIR CareTeam validation is not successful" + validationResult.getMessages());
        }
    }


    private Optional<String> getCareTeamDisplay(String code, Optional<List<ValueSetDto>> lookupValueSets) {
        Optional<String> lookupDisplay=null;
        if(lookupValueSets.isPresent()){
            lookupDisplay=lookupValueSets.get().stream()
                    .filter(lookupValue -> code.equalsIgnoreCase(lookupValue.getCode()))
                    .map(valueSet -> valueSet.getDisplay()).findFirst();

        }
        return lookupDisplay;
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

    private String convertDateToString(Date date){
        DateFormat df=new SimpleDateFormat("MM/dd/yyyy");
        return df.format(date);
    }


}

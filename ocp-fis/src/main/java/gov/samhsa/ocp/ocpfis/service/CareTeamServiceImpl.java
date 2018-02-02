package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantMemberDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.SubjectDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

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
    public CareTeamServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties,LookUpService lookUpService) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;
        this.lookUpService=lookUpService;
    }

    @Override
    public void createCareTeam(CareTeamDto careTeamDto) {

    }

    @Override
    public void updateCareTeam(String careTeamId, CareTeamDto careTeamDto) {

    }

    @Override
    public PageDto<CareTeamDto> getCareTeam(String searchType, String searchValue, Optional<String> showInactive,Optional<Integer> page, Optional<Integer> size) {
        int numberOfCareTeamMembersPerPage = size.filter(s -> s > 0 &&
                s <= fisProperties.getPractitioner().getPagination().getMaxSize()).orElse(fisProperties.getPractitioner().getPagination().getDefaultSize());

        IQuery iQuery= fhirClient.search().forResource(CareTeam.class);

        if(searchType.equalsIgnoreCase("patient"))
            iQuery.where(new ReferenceClientParam("patient").hasId("Patient/"+searchValue));


        Bundle firstPageCareTeamBundle;
        Bundle otherPageCareTeamBundle;
        boolean firstPage=true;

        firstPageCareTeamBundle= (Bundle) iQuery
                .count(numberOfCareTeamMembersPerPage)
                .returnBundle(Bundle.class).execute();

        if(firstPageCareTeamBundle==null || firstPageCareTeamBundle.getEntry().size()<1){
            throw new ResourceAccessException("No Care Team members were found in the FHIR server.");
        }

        otherPageCareTeamBundle=firstPageCareTeamBundle;

        if(page.isPresent() && page.get()>1 && otherPageCareTeamBundle.getLink(Bundle.LINK_NEXT) !=null){
            firstPage=false;
            otherPageCareTeamBundle=getCareTeamBundleAfterFirstPage(firstPageCareTeamBundle,page.get(),numberOfCareTeamMembersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedCareTeamMembers=otherPageCareTeamBundle.getEntry();

        IQuery careTeamWithItsSubjectAndParticipantQuery= iQuery.include(CareTeam.INCLUDE_PARTICIPANT)
                .include(CareTeam.INCLUDE_SUBJECT);

        Bundle careTeamWithItsSubjectAndParticipantBundle= (Bundle) careTeamWithItsSubjectAndParticipantQuery.returnBundle(Bundle.class).execute();


        careTeamWithItsSubjectAndParticipantBundle.getTotal();
        List<CareTeamDto> careTeamDtos=retrievedCareTeamMembers.stream().map(retrievedCareTeamMember->{
            CareTeam careTeam= (CareTeam) retrievedCareTeamMember.getResource();
             CareTeamDto careTeamDto=  modelMapper.map(careTeam,CareTeamDto.class);
            careTeamDto.setId(retrievedCareTeamMember.getResource().getIdElement().getIdPart());

            String subjectReference=careTeam.getSubject().getReference();
            String patientId=subjectReference.substring(subjectReference.lastIndexOf("/")+1);

           Optional<Bundle.BundleEntryComponent> patientBundleEntryComponent=careTeamWithItsSubjectAndParticipantBundle.getEntry().stream().filter(careTeamWithItsSubjectAndParticipant-> {
                               return careTeamWithItsSubjectAndParticipant.getResource().getIdElement().getIdPart().equalsIgnoreCase(patientId);
                   }
            ).findFirst();

           patientBundleEntryComponent.ifPresent(patient->{
               Patient subjectPatient= (Patient) patient.getResource();
               String firstName="";
               String lastName="";
               if(subjectPatient.getName() !=null && subjectPatient.getName().size()>0) {
                   if (subjectPatient.getName().get(0).getFamily() != null && !subjectPatient.getName().get(0).getFamily().isEmpty()) {
                       lastName = subjectPatient.getName().get(0).getFamily();
                   }
                   if (subjectPatient.getName().get(0).getGiven() != null && subjectPatient.getName().get(0).getGiven().size()>0){
                       firstName=subjectPatient.getName().get(0).getGiven().get(0).toString();
                   }
               }
               careTeamDto.setSubject(SubjectDto.builder().id(subjectPatient.getIdElement().getIdPart())
                       .lastName(lastName)
                       .firstName(firstName)
                       .build());
           });

           List<ParticipantDto> participantDtos=new ArrayList<>();
                careTeam.getParticipant().forEach(participant->{
                    ParticipantDto participantDto=new ParticipantDto();
                    if(participant.getRole() !=null && !participant.getRole().isEmpty()) {

                        if (participant.getRole().getCoding().size() > 0 && participant.getRole().getCoding() != null){
                            Coding participantRole=participant.getRole().getCoding().get(0);

                            ValueSetDto roleDto=new ValueSetDto();
                            roleDto.setCode((participantRole.getCode()!=null && !participantRole.getCode().isEmpty())?participantRole.getCode():"");
                            roleDto.setDisplay((participantRole.getDisplay()!=null && !participantRole.getDisplay().isEmpty())?participantRole.getDisplay():"");
                            participantDto.setRole(roleDto);
                        }

                    }

                    if(participant.getMember() !=null && !participant.getMember().isEmpty()){
                        String participantMemberReference=participant.getMember().getReference();
                        String participantId=participantMemberReference.split("/")[1];
                        String participantType=participantMemberReference.split("/")[0];

                        careTeamWithItsSubjectAndParticipantBundle.getEntry().forEach(careTeamWithItsSubjectAndPartipant->{
                          Resource resource= careTeamWithItsSubjectAndPartipant.getResource();
                          if(resource.getResourceType().toString().trim().replaceAll(" ","").equalsIgnoreCase(participantType.trim().replaceAll(" ",""))){

                              if(resource.getIdElement().getIdPart().equalsIgnoreCase(participantId)) {
                                  ParticipantMemberDto participantMemberDto = new ParticipantMemberDto();
                                  if(resource.getResourceType().toString().equalsIgnoreCase("Patient")){
                                      Patient patient= (Patient) resource;

                                      participantMemberDto.setId(participantId);
                                      participantMemberDto.setFirstName(Optional.ofNullable(patient.getName().get(0).getGiven().get(0).toString()));
                                      participantMemberDto.setLastName(Optional.of(patient.getName().get(0).getFamily()));
                                      participantMemberDto.setType(patient.fhirType());
                                  }else if(resource.getResourceType().toString().equalsIgnoreCase("Practitioner")){
                                    Practitioner practitioner= (Practitioner) resource;
                                      participantMemberDto.setId(participantId);
                                      participantMemberDto.setFirstName(Optional.ofNullable(practitioner.getName().get(0).getGiven().get(0).toString()));
                                      participantMemberDto.setLastName(Optional.of(practitioner.getName().get(0).getFamily()));
                                      participantMemberDto.setType(practitioner.fhirType());
                                  }else if(resource.getResourceType().toString().equalsIgnoreCase("Organization")){
                                      Organization organization= (Organization) resource;
                                      participantMemberDto.setId(participantId);
                                     participantMemberDto.setName(Optional.ofNullable(organization.getName()));
                                      participantMemberDto.setType(organization.fhirType());
                                  }

                                  participantDto.setMember(participantMemberDto);
                              }
                            }

                        });

                    }

                    participantDtos.add(participantDto);
                });

            careTeamDto.setPraticipants(participantDtos);
            return careTeamDto;
        }).collect(toList());

        double totalPages = Math.ceil((double) otherPageCareTeamBundle.getTotal() / numberOfCareTeamMembersPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(careTeamDtos,numberOfCareTeamMembersPerPage,totalPages,currentPage,careTeamDtos.size(),otherPageCareTeamBundle.getTotal());
    }

    private Bundle getCareTeamBundleAfterFirstPage(Bundle careTeamBundle,int page, int size) {
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


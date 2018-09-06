package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.OutsideParticipant;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantMemberDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantSearchDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerRoleDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class ParticipantServiceImpl implements ParticipantService {

    private final PractitionerService practitionerService;
    private final OrganizationService organizationService;
    private final PatientService patientService;
    private final RelatedPersonService relatedPersonService;
    private final EpisodeOfCareService episodeOfCareService;

    public ParticipantServiceImpl(PractitionerService practitionerService, OrganizationService organizationService, PatientService patientService, RelatedPersonService relatedPersonService, EpisodeOfCareService episodeOfCareService) {
        this.practitionerService = practitionerService;
        this.organizationService = organizationService;
        this.patientService = patientService;
        this.relatedPersonService = relatedPersonService;
        this.episodeOfCareService = episodeOfCareService;
    }

    public PageDto<ParticipantSearchDto> getAllParticipants(String patientId, ParticipantTypeEnum participantType, Optional<String> value, Optional<String> organization, Optional<Boolean> forCareTeam, Optional<Boolean> showInActive, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll) {
        final String typeCode = participantType.getCode();

        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.practitioner.getCode())) {
            if (forCareTeam.isPresent()) {
                if (!forCareTeam.get()) {
                    organization = organizationId(organization, patientId);
                }
            } else {
                organization = organizationId(organization, patientId);
            }
            PageDto<PractitionerDto> pageDto = practitionerService.searchPractitioners(Optional.ofNullable(PractitionerController.SearchType.name), value, organization, showInActive, page, size, showAll);
            participantsDto = convertPractitionersToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.organization.getCode())) {
            PageDto<OrganizationDto> pageDto = organizationService.searchOrganizations(Optional.ofNullable(OrganizationController.SearchType.name), value, showInActive, page, size, showAll);
            participantsDto = convertOrganizationsToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.patient.getCode())) {
            //refactor getPatientsByValue to match other apis
            PageDto<PatientDto> pageDto = patientService.getPatientsByValue(Optional.ofNullable("name"), value, Optional.empty(), organizationId(organization, patientId), Optional.empty(), showInActive, page, size, showAll);
            participantsDto = convertPatientsToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.relatedPerson.getCode())) {
            PageDto<RelatedPersonDto> pageDto = relatedPersonService.searchRelatedPersons(patientId, Optional.of("name"), value, showInActive, page, size, showAll);
            participantsDto = convertRelatedPersonsToParticipantsDto(pageDto, participantType);
        }

        return participantsDto;
    }

    @Override
    public List<OutsideParticipant> retrieveOutsideParticipants(String patient, String participantType, String name, String organization, Optional<Integer> page, Optional<Integer> size, Optional<Boolean> showAll) {

        if (participantType.equalsIgnoreCase("relatedPerson")) {
            //get relatedPerson of the patient
            //public PageDto<RelatedPersonDto> searchRelatedPersons(String patientId, Optional<String> searchKey, Optional<String> searchValue, Optional<Boolean> showInactive, Optional<Integer> pageNumber, Optional<Integer> pageSize, Optional<Boolean> showAll) {
            PageDto<RelatedPersonDto> relatedPersonDtos = relatedPersonService.searchRelatedPersons(patient, Optional.of("name"), Optional.of(name), Optional.of(true), page, size, showAll);

            List<RelatedPersonDto> relatedPersonList = relatedPersonDtos.getElements();

            return relatedPersonList.stream().map(relatedPersonDto -> {
                OutsideParticipant outsideParticipant = new OutsideParticipant();
                outsideParticipant.setName(relatedPersonDto.getFirstName() + " " + relatedPersonDto.getLastName());
                outsideParticipant.setParticipantId(relatedPersonDto.getRelatedPersonId());
                outsideParticipant.setIdentifierType(relatedPersonDto.getIdentifierValue());
                outsideParticipant.setIdentifierValue(relatedPersonDto.getIdentifierType());
                return outsideParticipant;
            }).collect(toList());
        } else {
            //bring all practitioners belonging to the given organization
            List<PractitionerDto> orgPractitionersList = practitionerService.getAllPractitionersInOrganization(organization);

            //bring all practitioners in the system
            List<PractitionerDto> allPractitionersList = practitionerService.getAllPractitionersInSystem(size, name);

            Set<String> orgPractitionersSet = orgPractitionersList.stream().map(practitioner -> practitioner.getLogicalId()).collect(toSet());
            Set<String> allPractitionersSet = allPractitionersList.stream().map(practitionerDto -> practitionerDto.getLogicalId()).collect(toSet());

            //allPractitionerSet now only contains practitioners outside the organization
            allPractitionersSet.removeAll(orgPractitionersSet);

            //now take allPractitionersList (which has everything) and only keep those which are in allPractitionerSet (now only containing outsiders)
            List<OutsideParticipant> outsideParticipantList = allPractitionersList.stream()
                    .filter(x -> allPractitionersSet.contains(x.getLogicalId()))
                    .map(practitionerDto -> {
                        OutsideParticipant outsideParticipant = new OutsideParticipant();

                        NameDto nameDto = practitionerDto.getName().stream().findFirst().get();
                        outsideParticipant.setName(nameDto.getFirstName() + " " + nameDto.getLastName());
                        outsideParticipant.setParticipantId(practitionerDto.getLogicalId());
                        IdentifierDto identifierDto = practitionerDto.getIdentifiers().stream().findFirst().get();

                        outsideParticipant.setIdentifierType(identifierDto.getSystem());
                        outsideParticipant.setIdentifierValue(identifierDto.getValue());

                        List<PractitionerRoleDto> roleDtos = practitionerDto.getPractitionerRoles();

                        List<ReferenceDto> orgRefDtos = roleDtos.stream().map(role -> {
                            return role.getOrganization();
                        }).collect(toList());

                        outsideParticipant.setAssociatedOrganizations(orgRefDtos);

                        return outsideParticipant;
                    })
                    .collect(toList());
            return outsideParticipantList;
        }
    }

    private Optional<String> retrieveOrganization(String patientId) {
        if (patientService.getPatientById(patientId).getOrganizationId() != null) {
            return patientService.getPatientById(patientId).getOrganizationId();
        } else {
            //TODO: Remove this episode of cares after the next data purge.
            List<EpisodeOfCareDto> episodeOfCareDtos = episodeOfCareService.getEpisodeOfCares(patientId, Optional.empty());
            if (!episodeOfCareDtos.isEmpty()) {
                return Optional.of(episodeOfCareDtos.stream().findFirst().get().getManagingOrganization().getReference());
            }
            return Optional.empty();
        }
    }

    private Optional<String> organizationId(Optional<String> organization, String patientId) {
        if (!organization.isPresent()) {
            return retrieveOrganization(patientId);
        }
        return organization;
    }

    private PageDto<ParticipantSearchDto> convertPractitionersToParticipantsDto(PageDto<PractitionerDto> sourcePageDto, ParticipantTypeEnum participantType) {
        //source
        List<PractitionerDto> sourcePractitionerDtoList = sourcePageDto.getElements();

        //destination
        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        List<ParticipantSearchDto> participantSearchDtoList = new ArrayList<>();

        for (PractitionerDto sourceDto : sourcePractitionerDtoList) {
            //destination
            ParticipantSearchDto participantSearchDto = new ParticipantSearchDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();

            List<NameDto> nameList = sourceDto.getName();
            if (nameList != null && nameList.size() > 0) {
                memberDto.setFirstName(Optional.of(nameList.get(0).getFirstName()));
                memberDto.setLastName(Optional.of(nameList.get(0).getLastName()));
            }
            memberDto.setId(sourceDto.getLogicalId());
            memberDto.setType(participantType.getCode());

            participantSearchDto.setMember(memberDto);

            participantSearchDto.setTelecoms(sourceDto.getTelecoms());
            participantSearchDto.setAddresses(sourceDto.getAddresses());
            participantSearchDto.setPractitionerRoles(sourceDto.getPractitionerRoles());
            participantSearchDto.setIdentifiers(sourceDto.getIdentifiers());


            participantSearchDtoList.add(participantSearchDto);

        }

        participantsDto.setElements(participantSearchDtoList);
        participantsDto.setSize(sourcePageDto.getSize());
        participantsDto.setTotalNumberOfPages(sourcePageDto.getTotalNumberOfPages());
        participantsDto.setCurrentPage(sourcePageDto.getCurrentPage());
        participantsDto.setCurrentPageSize(sourcePageDto.getCurrentPageSize());
        participantsDto.setTotalElements(sourcePageDto.getTotalElements());

        return participantsDto;
    }

    private PageDto<ParticipantSearchDto> convertOrganizationsToParticipantsDto(PageDto<OrganizationDto> pageDto, ParticipantTypeEnum participantType) {
        List<OrganizationDto> organizationDtoList = pageDto.getElements();
        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        List<ParticipantSearchDto> participantSearchDtoList = new ArrayList<>();

        for (OrganizationDto dto : organizationDtoList) {
            ParticipantSearchDto participantSearchDto = new ParticipantSearchDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();

            memberDto.setName(Optional.ofNullable(dto.getName()));
            memberDto.setId(dto.getLogicalId());
            memberDto.setType(participantType.getCode());

            participantSearchDto.setMember(memberDto);

            participantSearchDtoList.add(participantSearchDto);

        }

        participantsDto.setElements(participantSearchDtoList);
        participantsDto.setSize(pageDto.getSize());
        participantsDto.setTotalNumberOfPages(pageDto.getTotalNumberOfPages());
        participantsDto.setCurrentPage(pageDto.getCurrentPage());
        participantsDto.setCurrentPageSize(pageDto.getCurrentPageSize());
        participantsDto.setTotalElements(pageDto.getTotalElements());

        return participantsDto;
    }

    private PageDto<ParticipantSearchDto> convertPatientsToParticipantsDto(PageDto<PatientDto> pageDto, ParticipantTypeEnum participantType) {
        List<PatientDto> patientList = pageDto.getElements();
        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        List<ParticipantSearchDto> participantSearchDtoList = new ArrayList<>();

        for (PatientDto dto : patientList) {
            ParticipantSearchDto participantSearchDto = new ParticipantSearchDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();
            List<NameDto> nameList = dto.getName();
            if (nameList != null && nameList.size() > 0) {
                memberDto.setFirstName(Optional.of(nameList.get(0).getFirstName()));
                memberDto.setLastName(Optional.of(nameList.get(0).getLastName()));
            }
            memberDto.setId(dto.getId());
            memberDto.setType(participantType.getCode());

            participantSearchDto.setMember(memberDto);

            participantSearchDtoList.add(participantSearchDto);
        }

        participantsDto.setElements(participantSearchDtoList);
        participantsDto.setSize(pageDto.getSize());
        participantsDto.setTotalNumberOfPages(pageDto.getTotalNumberOfPages());
        participantsDto.setCurrentPage(pageDto.getCurrentPage());
        participantsDto.setCurrentPageSize(pageDto.getCurrentPageSize());
        participantsDto.setTotalElements(pageDto.getTotalElements());

        return participantsDto;
    }

    private PageDto<ParticipantSearchDto> convertRelatedPersonsToParticipantsDto(PageDto<RelatedPersonDto> pageDto, ParticipantTypeEnum participantType) {
        List<RelatedPersonDto> relatedPersonDtos = pageDto.getElements();
        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        List<ParticipantSearchDto> participantSearchDtoList = new ArrayList<>();

        for (RelatedPersonDto dto : relatedPersonDtos) {
            ParticipantSearchDto participantSearchDto = new ParticipantSearchDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();

            memberDto.setFirstName(Optional.of(dto.getFirstName()));
            memberDto.setLastName(Optional.of(dto.getLastName()));
            memberDto.setId(dto.getRelatedPersonId());
            memberDto.setType(participantType.getCode());

            participantSearchDto.setMember(memberDto);

            setOnBehalfOfDtoAndRole(participantSearchDtoList, participantSearchDto);
        }

        participantsDto.setElements(participantSearchDtoList);
        participantsDto.setSize(pageDto.getSize());
        participantsDto.setTotalNumberOfPages(pageDto.getTotalNumberOfPages());
        participantsDto.setCurrentPage(pageDto.getCurrentPage());
        participantsDto.setCurrentPageSize(pageDto.getCurrentPageSize());
        participantsDto.setTotalElements(pageDto.getTotalElements());

        return participantsDto;
    }

    private void setOnBehalfOfDtoAndRole(List<ParticipantSearchDto> participantSearchDtoList, ParticipantSearchDto participantSearchDto) {
        participantSearchDtoList.add(participantSearchDto);
    }

}

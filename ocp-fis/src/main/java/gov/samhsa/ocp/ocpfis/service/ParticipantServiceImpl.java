package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.domain.SearchKeyEnum;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantMemberDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantOnBehalfOfDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantSearchDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.web.OrganizationController;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ParticipantServiceImpl implements ParticipantService {

    private final PractitionerService practitionerService;
    private final OrganizationService organizationService;
    private final PatientService patientService;
    private final RelatedPersonService relatedPersonService;

    public ParticipantServiceImpl(PractitionerService practitionerService, OrganizationService organizationService, PatientService patientService, RelatedPersonService relatedPersonService) {
        this.practitionerService = practitionerService;
        this.organizationService = organizationService;
        this.patientService = patientService;
        this.relatedPersonService = relatedPersonService;
    }

    public PageDto<ParticipantSearchDto> getAllParticipants(ParticipantTypeEnum participantType, String value, Optional<Boolean> showInActive, Optional<Integer> page, Optional<Integer> size) {
        final String typeCode = participantType.getCode();

        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.practitioner.getCode())) {
            PageDto<PractitionerDto> pageDto = practitionerService.searchPractitioners(PractitionerController.SearchType.name, value, showInActive, page, size);
            participantsDto = convertPractitionersToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.organization.getCode())) {
            PageDto<OrganizationDto> pageDto = organizationService.searchOrganizations(OrganizationController.SearchType.name, value, showInActive, page, size);
            participantsDto = convertOrganizationsToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.patient.getCode())) {
            //refactor getPatientsByValue to match other apis
            PageDto<PatientDto> pageDto = patientService.getPatientsByValue(value, "name", showInActive, page, size);
            participantsDto = convertPatientsToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.relatedPerson.getCode())) {
            PageDto<RelatedPersonDto> pageDto = relatedPersonService.searchRelatedPersons("name", value, showInActive, page, size);
            participantsDto = convertRelatedPersonsToParticipantsDto(pageDto, participantType);
        }

        return participantsDto;
    }

    private PageDto<ParticipantSearchDto> convertPractitionersToParticipantsDto(PageDto<PractitionerDto> pageDto, ParticipantTypeEnum participantType) {
        List<PractitionerDto> practitionerDtoList = pageDto.getElements();
        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        List<ParticipantSearchDto> participantSearchDtoList = new ArrayList<>();

        for (PractitionerDto dto : practitionerDtoList) {
            ParticipantSearchDto participantSearchDto = new ParticipantSearchDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();

            List<NameDto> nameList = dto.getName();
            if(nameList != null && nameList.size() > 0) {
                memberDto.setFirstName(Optional.of(nameList.get(0).getFirstName()));
                memberDto.setLastName(Optional.of(nameList.get(0).getLastName()));
            }
            memberDto.setId(dto.getLogicalId());
            memberDto.setType(participantType.getCode());

            participantSearchDto.setMember(memberDto);

            ParticipantOnBehalfOfDto participantOnBehalfOfDto = new ParticipantOnBehalfOfDto();
            participantSearchDto.setOnBehalfOfDto(participantOnBehalfOfDto);

            ValueSetDto valueSetDto = new ValueSetDto();
            participantSearchDto.setRole(valueSetDto);

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

    private PageDto<ParticipantSearchDto> convertOrganizationsToParticipantsDto(PageDto<OrganizationDto> pageDto, ParticipantTypeEnum participantType) {
        List<OrganizationDto> organizationDtoList = pageDto.getElements();
        PageDto<ParticipantSearchDto> participantsDto = new PageDto<>();

        List<ParticipantSearchDto> participantSearchDtoList = new ArrayList<>();

        for (OrganizationDto dto : organizationDtoList) {
            ParticipantSearchDto participantSearchDto = new ParticipantSearchDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();

            memberDto.setName(Optional.of(dto.getName()));
            memberDto.setId(dto.getLogicalId());
            memberDto.setType(participantType.getCode());

            participantSearchDto.setMember(memberDto);

            ParticipantOnBehalfOfDto participantOnBehalfOfDto = new ParticipantOnBehalfOfDto();
            participantSearchDto.setOnBehalfOfDto(participantOnBehalfOfDto);

            ValueSetDto valueSetDto = new ValueSetDto();
            participantSearchDto.setRole(valueSetDto);

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
            if(nameList != null && nameList.size() > 0) {
                memberDto.setFirstName(Optional.of(nameList.get(0).getFirstName()));
                memberDto.setLastName(Optional.of(nameList.get(0).getLastName()));
            }
            memberDto.setId(dto.getId());
            memberDto.setType(participantType.getCode());

            participantSearchDto.setMember(memberDto);

            ParticipantOnBehalfOfDto participantOnBehalfOfDto = new ParticipantOnBehalfOfDto();
            participantSearchDto.setOnBehalfOfDto(participantOnBehalfOfDto);

            ValueSetDto valueSetDto = new ValueSetDto();
            participantSearchDto.setRole(valueSetDto);

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
        ParticipantOnBehalfOfDto participantOnBehalfOfDto = new ParticipantOnBehalfOfDto();
        participantSearchDto.setOnBehalfOfDto(participantOnBehalfOfDto);

        ValueSetDto valueSetDto = new ValueSetDto();
        participantSearchDto.setRole(valueSetDto);

        participantSearchDtoList.add(participantSearchDto);
    }

}

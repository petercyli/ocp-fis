package gov.samhsa.ocp.ocpfis.service;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import gov.samhsa.ocp.ocpfis.service.dto.OrganizationDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantMemberDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantOnBehalfOfDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
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

    public ParticipantServiceImpl(PractitionerService practitionerService, OrganizationService organizationService, PatientService patientService) {
        this.practitionerService = practitionerService;
        this.organizationService = organizationService;
        this.patientService = patientService;
    }

    public PageDto<ParticipantDto> getAllParticipants(ParticipantTypeEnum participantType, String value, Optional<Boolean> showInActive, Optional<Integer> page, Optional<Integer> size) {
        final String typeCode = participantType.getCode();

        PageDto<ParticipantDto> participantsDto = new PageDto<>();

        if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.practitioner.getCode())) {
            PageDto<PractitionerDto> pageDto = practitionerService.searchPractitioners(PractitionerController.SearchType.name, value, showInActive, page, size);
            participantsDto = convertPractitionersToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.organization.getCode())) {
            PageDto<OrganizationDto> pageDto = organizationService.searchOrganizations(OrganizationController.SearchType.name, value, showInActive, page, size);
            participantsDto = convertOrganizationsToParticipantsDto(pageDto, participantType);

        } else if (typeCode.equalsIgnoreCase(ParticipantTypeEnum.patient.getCode())) {
            //refactor getPatientsByValue to match other apis
            PageDto<PatientDto> pageDto = patientService.getPatientsByValue(value, "name",  showInActive, page, size);
            participantsDto = convertPatientsToParticipantsDto(pageDto, participantType);

        }

        return participantsDto;
    }

    private PageDto<ParticipantDto> convertPractitionersToParticipantsDto(PageDto<PractitionerDto> pageDto, ParticipantTypeEnum participantType) {
        List<PractitionerDto> practitionerDtoList = pageDto.getElements();
        PageDto<ParticipantDto> participantsDto = new PageDto<>();

        List<ParticipantDto> participantDtoList = new ArrayList<>();

        for(PractitionerDto dto : practitionerDtoList) {
            ParticipantDto participantDto = new ParticipantDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();
            List<NameDto> nameList = dto.getName();
            memberDto.setFirstName(Optional.of(nameList.get(0).getFirstName()));
            memberDto.setLastName(Optional.of(nameList.get(0).getLastName()));
            memberDto.setId(dto.getLogicalId());
            memberDto.setType(participantType.getCode());

            participantDto.setMember(memberDto);

            ParticipantOnBehalfOfDto participantOnBehalfOfDto = new ParticipantOnBehalfOfDto();
            participantDto.setOnBehalfOfDto(participantOnBehalfOfDto);

            ValueSetDto valueSetDto = new ValueSetDto();
            participantDto.setRole(valueSetDto);

            participantDtoList.add(participantDto);

        }

        participantsDto.setElements(participantDtoList);
        participantsDto.setSize(pageDto.getSize());
        participantsDto.setTotalNumberOfPages(pageDto.getTotalNumberOfPages());
        participantsDto.setCurrentPage(pageDto.getCurrentPage());
        participantsDto.setCurrentPageSize(pageDto.getCurrentPageSize());
        participantsDto.setTotalElements(pageDto.getTotalElements());

        return  participantsDto;
    }

    private PageDto<ParticipantDto> convertOrganizationsToParticipantsDto(PageDto<OrganizationDto> pageDto, ParticipantTypeEnum participantType) {
        List<OrganizationDto> organizationDtoList = pageDto.getElements();
        PageDto<ParticipantDto> participantsDto = new PageDto<>();

        List<ParticipantDto> participantDtoList = new ArrayList<>();

        for(OrganizationDto dto : organizationDtoList) {
            ParticipantDto participantDto = new ParticipantDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();

            memberDto.setName(Optional.of(dto.getName()));
            memberDto.setId(dto.getLogicalId());
            memberDto.setType(participantType.getCode());

            participantDto.setMember(memberDto);

            ParticipantOnBehalfOfDto participantOnBehalfOfDto = new ParticipantOnBehalfOfDto();
            participantDto.setOnBehalfOfDto(participantOnBehalfOfDto);

            ValueSetDto valueSetDto = new ValueSetDto();
            participantDto.setRole(valueSetDto);

            participantDtoList.add(participantDto);

        }

        participantsDto.setElements(participantDtoList);
        participantsDto.setSize(pageDto.getSize());
        participantsDto.setTotalNumberOfPages(pageDto.getTotalNumberOfPages());
        participantsDto.setCurrentPage(pageDto.getCurrentPage());
        participantsDto.setCurrentPageSize(pageDto.getCurrentPageSize());
        participantsDto.setTotalElements(pageDto.getTotalElements());

        return  participantsDto;
    }

    private PageDto<ParticipantDto> convertPatientsToParticipantsDto(PageDto<PatientDto> pageDto, ParticipantTypeEnum participantType) {
        List<PatientDto> patientList = pageDto.getElements();
        PageDto<ParticipantDto> participantsDto = new PageDto<>();

        List<ParticipantDto> participantDtoList = new ArrayList<>();

        for(PatientDto dto : patientList) {
            ParticipantDto participantDto = new ParticipantDto();

            ParticipantMemberDto memberDto = new ParticipantMemberDto();
            List<NameDto> nameList = dto.getName();
            memberDto.setFirstName(Optional.of(nameList.get(0).getFirstName()));
            memberDto.setLastName(Optional.of(nameList.get(0).getLastName()));
            memberDto.setId(dto.getId());
            memberDto.setType(participantType.getCode());

            participantDto.setMember(memberDto);

            ParticipantOnBehalfOfDto participantOnBehalfOfDto = new ParticipantOnBehalfOfDto();
            participantDto.setOnBehalfOfDto(participantOnBehalfOfDto);

            ValueSetDto valueSetDto = new ValueSetDto();
            participantDto.setRole(valueSetDto);

            participantDtoList.add(participantDto);

        }

        participantsDto.setElements(participantDtoList);
        participantsDto.setSize(pageDto.getSize());
        participantsDto.setTotalNumberOfPages(pageDto.getTotalNumberOfPages());
        participantsDto.setCurrentPage(pageDto.getCurrentPage());
        participantsDto.setCurrentPageSize(pageDto.getCurrentPageSize());
        participantsDto.setTotalElements(pageDto.getTotalElements());

        return  participantsDto;
    }

}

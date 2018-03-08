package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedPerson;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CareTeamToCareTeamDtoConverter {

    public static CareTeamDto map(CareTeam careTeam) {
        CareTeamDto careTeamDto = new CareTeamDto();

        //id
        careTeamDto.setId(careTeam.getIdElement().getIdPart());

        //name
        careTeamDto.setName(careTeam.getName());

        //status
        CareTeam.CareTeamStatus careTeamStatus = careTeam.getStatus();
        careTeamDto.setStatusCode(careTeamStatus.toCode());
        careTeamDto.setStatusDisplay(careTeamStatus.getDisplay());

        //categories
        List<CodeableConcept> codeableConceptList = careTeam.getCategory();
        CodeableConcept codeableConcept = codeableConceptList.stream().findFirst().orElse(null);
        if (codeableConcept != null) {
            List<Coding> codingList = codeableConcept.getCoding();
            codingList.stream().findFirst().ifPresent(coding -> careTeamDto.setCategoryCode(coding.getCode()));
        }

        //subject
        careTeamDto.setSubjectId(careTeam.getSubject().getReference().replace("Patient/", ""));
        Patient patientSubject = (Patient) careTeam.getSubject().getResource();
        if (patientSubject != null && patientSubject.getName() != null && patientSubject.getName().get(0) != null) {
            careTeamDto.setSubjectFirstName(patientSubject.getName().get(0).getGiven().get(0).toString());
            careTeamDto.setSubjectLastName(patientSubject.getName().get(0).getFamily());
        }

        //reasonCode
        List<CodeableConcept> codeableConceptReasonCodeList = careTeam.getReasonCode();
        CodeableConcept codeableConceptReasonCode = codeableConceptReasonCodeList.stream().findFirst().orElse(null);

        if (codeableConceptReasonCode != null) {
            List<Coding> codingReasonCodeList = codeableConceptReasonCode.getCoding();
            codingReasonCodeList.stream().findFirst().ifPresent(codingReasonCode -> careTeamDto.setReasonCode(codingReasonCode.getCode()));
        }

        //participants
        List<CareTeam.CareTeamParticipantComponent> careTeamParticipantComponentList = careTeam.getParticipant();
        List<ParticipantDto> participantDtos = new ArrayList<>();

        //start and end date
        Period period = careTeam.getPeriod();
        if (period.getStart() != null) {
            careTeamDto.setStartDate(DateUtil.convertDateToString(period.getStart()));
        }

        if (period.getEnd() != null) {
            careTeamDto.setEndDate(DateUtil.convertDateToString(period.getEnd()));
        }

        for (CareTeam.CareTeamParticipantComponent careTeamParticipantComponent : careTeamParticipantComponentList) {
            Reference member = careTeamParticipantComponent.getMember();

            ParticipantDto participantDto = new ParticipantDto();


            populateParticipantMemberInformation(member, participantDto);

            CodeableConcept roleCodeableConcept = careTeamParticipantComponent.getRole();
            List<Coding> codingRoleCodeList = roleCodeableConcept.getCoding();
            Coding codingRoleCode = codingRoleCodeList.stream().findFirst().orElse(null);
            if (codingRoleCode != null) {
                participantDto.setRoleCode(codingRoleCode.getCode());
            }

            participantDto.setStartDate(DateUtil.convertDateToString(careTeamParticipantComponent.getPeriod().getStart()));
            participantDto.setEndDate(DateUtil.convertDateToString(careTeamParticipantComponent.getPeriod().getEnd()));


            participantDtos.add(participantDto);
        }

        careTeamDto.setParticipants(participantDtos);

        return careTeamDto;
    }

    public static List<ReferenceDto> mapToPartipants(CareTeam careTeam, Optional<List<String>> roles) {
        List<ReferenceDto> referenceDtos = new ArrayList<>();

        List<CareTeam.CareTeamParticipantComponent> careTeamParticipantComponentList = careTeam.getParticipant();

        for (CareTeam.CareTeamParticipantComponent careTeamParticipantComponent : careTeamParticipantComponentList) {
            Reference member = careTeamParticipantComponent.getMember();

            ReferenceDto referenceDto = new ReferenceDto();

            if (member.getReference().contains(ParticipantTypeEnum.practitioner.getName())) {
                referenceDto.setReference(member.getReference());

                Practitioner practitioner = (Practitioner) member.getResource();

                CodeableConcept roleCodeableConcept = careTeamParticipantComponent.getRole();
                List<Coding> codingRoleCodeList = roleCodeableConcept.getCoding();
                Coding codingRoleCode = codingRoleCodeList.stream().findFirst().orElse(null);
                if (codingRoleCode != null) {
                    String role = codingRoleCode.getCode();

                    if (roles.isPresent() && roles.get().contains(role))
                        if (practitioner != null && practitioner.getName() != null && practitioner.getName().get(0) != null) {
                            referenceDto.setDisplay(practitioner.getName().get(0).getGiven().get(0).toString() + " " + practitioner.getName().get(0).getFamily());
                        }

                    referenceDtos.add(referenceDto);
                }
            }
        }

        return referenceDtos;

    }

    private static void populateParticipantMemberInformation(Reference member, ParticipantDto participantDto) {
        if (member.getReference().contains(ParticipantTypeEnum.organization.getName())) {
            participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.organization.getName() + "/", ""));
            participantDto.setMemberType(ParticipantTypeEnum.organization.getCode());

            Organization organization = (Organization) member.getResource();
            participantDto.setMemberName(Optional.ofNullable(organization.getName()));

        } else if (member.getReference().contains(ParticipantTypeEnum.patient.getName())) {
            participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.patient.getName() + "/", ""));
            participantDto.setMemberType(ParticipantTypeEnum.patient.getCode());

            Patient patientMember = (Patient) member.getResource();
            if (patientMember != null && patientMember.getName() != null && patientMember.getName().get(0) != null) {
                participantDto.setMemberFirstName(Optional.ofNullable(patientMember.getName().get(0).getGiven().get(0).toString()));
                participantDto.setMemberLastName(Optional.ofNullable(patientMember.getName().get(0).getFamily()));
            }

        } else if (member.getReference().contains(ParticipantTypeEnum.practitioner.getName())) {
            participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.practitioner.getName() + "/", ""));
            participantDto.setMemberType(ParticipantTypeEnum.practitioner.getCode());

            Practitioner practitioner = (Practitioner) member.getResource();
            if (practitioner != null && practitioner.getName() != null && practitioner.getName().get(0) != null) {
                participantDto.setMemberFirstName(Optional.ofNullable(practitioner.getName().get(0).getGiven().get(0).toString()));
                participantDto.setMemberLastName(Optional.ofNullable(practitioner.getName().get(0).getFamily()));
            }

        } else if (member.getReference().contains(ParticipantTypeEnum.relatedPerson.getName())) {
            participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.relatedPerson.getName() + "/", ""));
            participantDto.setMemberType(ParticipantTypeEnum.relatedPerson.getCode());

            RelatedPerson relatedPerson = (RelatedPerson) member.getResource();
            if (relatedPerson != null && relatedPerson.getName() != null && relatedPerson.getName().get(0) != null) {
                participantDto.setMemberFirstName(Optional.ofNullable(relatedPerson.getName().get(0).getGiven().get(0).toString()));
                participantDto.setMemberLastName(Optional.ofNullable(relatedPerson.getName().get(0).getFamily()));
            }
        }
    }


}

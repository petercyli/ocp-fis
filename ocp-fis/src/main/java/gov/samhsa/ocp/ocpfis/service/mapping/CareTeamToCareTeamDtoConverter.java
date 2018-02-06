package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

        //categories
        List<CodeableConcept> codeableConceptList = careTeam.getCategory();
        CodeableConcept codeableConcept = codeableConceptList.stream().findFirst().get();
        List<Coding> codingList = codeableConcept.getCoding();
        Coding coding = codingList.stream().findFirst().get();
        careTeamDto.setCategoryCode(coding.getCode());

        //subject
        careTeamDto.setSubjectId(careTeam.getSubject().getReference().replace("Patient/", ""));

        //participants
        List<CareTeam.CareTeamParticipantComponent> careTeamParticipantComponentList = careTeam.getParticipant();
        List<ParticipantDto> participantDtos = new ArrayList<>();

        //start and end date
        Period period = careTeam.getPeriod();
        if(period.getStart() != null) {
            careTeamDto.setStartDate(convertToString(period.getStart()));
        }

        if(period.getEnd() != null) {
            careTeamDto.setEndDate(convertToString(period.getEnd()));
        }

        for (CareTeam.CareTeamParticipantComponent careTeamParticipantComponent : careTeamParticipantComponentList) {
            Reference member = careTeamParticipantComponent.getMember();

            ParticipantDto participantDto = new ParticipantDto();


            if (member.getReference().contains(ParticipantTypeEnum.organization.getName())) {
                participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.organization.getName() + "/", ""));
                participantDto.setMemberType(ParticipantTypeEnum.organization.getCode());

            } else if (member.getReference().contains(ParticipantTypeEnum.patient.getName())) {
                participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.patient.getName() + "/", ""));
                participantDto.setMemberType(ParticipantTypeEnum.patient.getCode());

            } else if (member.getReference().contains(ParticipantTypeEnum.practitioner.getName())) {
                participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.practitioner.getName() + "/", ""));
                participantDto.setMemberType(ParticipantTypeEnum.practitioner.getCode());

            } else if (member.getReference().contains(ParticipantTypeEnum.relatedPerson.getName())) {
                participantDto.setMemberId(member.getReference().replace(ParticipantTypeEnum.relatedPerson.getName() + "/", ""));
                participantDto.setMemberType(ParticipantTypeEnum.relatedPerson.getCode());
            }

            participantDtos.add(participantDto);
        }

        careTeamDto.setParticipants(participantDtos);

        return careTeamDto;
    }

    private static String convertToString(Date date) {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        return df.format(date);
    }


}

package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.domain.ParticipantTypeEnum;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.ArrayList;
import java.util.List;

public class CareTeamDtoToCareTeamConverter {

    public static CareTeam map(CareTeamDto careTeamDto) throws FHIRException {
        CareTeam careTeam = new CareTeam();
        //id
        careTeam.setId(careTeamDto.getId());

        //name
        careTeam.setName(careTeamDto.getName());

        //status
        CareTeam.CareTeamStatus careTeamStatus = CareTeam.CareTeamStatus.fromCode(careTeamDto.getStatusCode());
        careTeam.setStatus(careTeamStatus);

        //categories
        Coding coding = new Coding();
        coding.setCode(careTeamDto.getCategoryCode());
        CodeableConcept codeableConcept = new CodeableConcept().addCoding(coding);
        careTeam.addCategory(codeableConcept);

        //subject
        careTeam.getSubject().setReference("Patient/" + careTeamDto.getSubjectId());

        //participants
        List<ParticipantDto> participantDtoList = careTeamDto.getParticipants();
        List<CareTeam.CareTeamParticipantComponent> participantsList = new ArrayList<>();

        for(ParticipantDto participantDto : participantDtoList) {
            CareTeam.CareTeamParticipantComponent careTeamParticipant = new CareTeam.CareTeamParticipantComponent();

            String memberType = participantDto.getMemberType();

            if(memberType.equalsIgnoreCase(ParticipantTypeEnum.practitioner.getCode())) {
                careTeamParticipant.getMember().setReference("Practitioner/" + participantDto.getMemberId());

            } else if (memberType.equalsIgnoreCase(ParticipantTypeEnum.patient.getCode())) {
                careTeamParticipant.getMember().setReference("Patient/" + participantDto.getMemberId());

            } else if (memberType.equalsIgnoreCase(ParticipantTypeEnum.organization.getCode())) {
                careTeamParticipant.getMember().setReference("Organization/" + participantDto.getMemberId());
            }

            //TODO: onBehalfOfDto
            //participantDto.getOnBehalfOfDto();

            //TODO: onRole
            //participantDto.getRole()

            participantsList.add(careTeamParticipant);
        }


        careTeam.setParticipant(participantsList);

        return careTeam;
    }


}

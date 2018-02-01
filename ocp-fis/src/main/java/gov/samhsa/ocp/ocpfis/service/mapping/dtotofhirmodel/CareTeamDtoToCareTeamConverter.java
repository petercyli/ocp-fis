package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantDto;
import gov.samhsa.ocp.ocpfis.service.dto.ParticipantMemberDto;
import gov.samhsa.ocp.ocpfis.service.dto.SubjectDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.ArrayList;
import java.util.List;

public class CareTeamDtoToCareTeamConverter {

    //@Autowired
    //private static IdentifierDtoListToIdentifierListConverter identifierDtoListToIdentifierListConverter;

    public static CareTeam map(CareTeamDto careTeamDto) throws FHIRException {
        CareTeam careTeam = new CareTeam();
        //id
        careTeam.setId(careTeamDto.getId());

        //name
        careTeam.setName(careTeamDto.getName());

        //identifier
        //careTeam.setIdentifier(identifierDtoListToIdentifierListConverter.convert(careTeamDto.getIdentifiers()));

        //status
        ValueSetDto careTeamDtoStatusDto = careTeamDto.getStatus();
        CareTeam.CareTeamStatus careTeamStatus = CareTeam.CareTeamStatus.fromCode(careTeamDtoStatusDto.getCode());
        careTeam.setStatus(careTeamStatus);

        //categories
        List<ValueSetDto> categoryDtoList = careTeamDto.getCategories();
        if(categoryDtoList.size() > 0) {
            ValueSetDto categoryDto = categoryDtoList.get(0);
            Coding coding = new Coding();
            coding.setCode(categoryDto.getCode());
            coding.setDisplay(categoryDto.getDisplay());
            coding.setSystem(categoryDto.getSystem());
            //definition?

            CodeableConcept codeableConcept = new CodeableConcept().addCoding(coding);
            careTeam.addCategory(codeableConcept);
        }

        //subject
        SubjectDto subjectDto = careTeamDto.getSubject();
        careTeam.getSubject().setReference("patient/" + subjectDto.getId());

        //participants
        List<ParticipantDto> participantDtoList = careTeamDto.getParticipants();
        List<CareTeam.CareTeamParticipantComponent> participantsList = new ArrayList<>();

        for(ParticipantDto participantDto : participantDtoList) {
            CareTeam.CareTeamParticipantComponent careTeamParticipant = new CareTeam.CareTeamParticipantComponent();

            //member
            ParticipantMemberDto memberDto = participantDto.getMember();

            if(memberDto.getType().equalsIgnoreCase("participant")) {
                careTeamParticipant.getMember().setReference("participant/" + memberDto.getId());

            } else if (memberDto.getType().equalsIgnoreCase("patient")) {
                careTeamParticipant.getMember().setReference("patient/" + memberDto.getId());

            } else if (memberDto.getType().equalsIgnoreCase("organization")) {
                careTeamParticipant.getMember().setReference("organization/" + memberDto.getId());
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

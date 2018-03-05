package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.exceptions.FHIRException;

import java.util.List;
import java.util.StringJoiner;

//import gov.samhsa.ocp.ocpfis.util.FhirUtils;

@Slf4j
public class EpisodeOfCareToEpisodeOfCareDtoMapper {

    public static final String NA = "NA";

    public static EpisodeOfCareDto map(EpisodeOfCare episodeOfCare) {
        EpisodeOfCareDto dto = new EpisodeOfCareDto();

        //id
        dto.setId(episodeOfCare.getIdElement().getIdPart());

        //status
        EpisodeOfCare.EpisodeOfCareStatus status = episodeOfCare.getStatus();
        if (status != null) {
            dto.setStatus(status.toCode());
        }

        //type
        List<CodeableConcept> types = episodeOfCare.getType();
        CodeableConcept type = types.stream().findFirst().orElse(null);
        if (type != null) {
            List<Coding> codingList = type.getCoding();
            codingList.stream().findFirst().ifPresent(it -> dto.setType(it.getCode()));
        }

        //patient
        if(episodeOfCare.getPatient() != null) {
            dto.setPatient(episodeOfCare.getPatient().getReference().replace(ResourceType.Patient + "/", ""));
        }

        //managing organization
        if(episodeOfCare.getManagingOrganization() != null) {
            dto.setManagingOrganization(episodeOfCare.getManagingOrganization().getReference().replace(ResourceType.Organization + "/", ""));
        }

        //start date
        Period period = episodeOfCare.getPeriod();
        if (period != null && period.getStart() != null) {
            dto.setStart(DateUtil.convertToString(period.getStart()));
        }

        //end date
        if (period != null && period.getEnd() != null) {
            dto.setEnd(DateUtil.convertToString(period.getEnd()));
        }

        //care manager
        if(episodeOfCare.getCareManager() != null) {
            dto.setCareManager(episodeOfCare.getCareManager().getReference().replace(ResourceType.Practitioner + "/", ""));
        }

        return dto;
    }

    public static ReferenceDto mapToReferenceDto(Task task) {
        ReferenceDto dto = new ReferenceDto();

        dto.setReference(task.getContext().getReference());

        if (task.hasContext()) {
            EpisodeOfCare episodeOfCare = (EpisodeOfCare) task.getContext().getResource();
            dto.setDisplay(createDisplayForEpisodeOfCare(task, episodeOfCare));
        }

        return dto;
    }

    private static String createDisplayForEpisodeOfCare(Task task, EpisodeOfCare episodeOfCare) {
        ReferenceDto activityDefinitionRefDto = null;
        try {
            activityDefinitionRefDto = convertReferenceToReferenceDto(task.getDefinitionReference());
        } catch (FHIRException e) {
            log.error("ReferenceDto for ActivityDefintion could not be retrieved");
        }

        String eocType = activityDefinitionRefDto != null ? activityDefinitionRefDto.getDisplay() : NA;
        String date = episodeOfCare.getPeriod() != null ? DateUtil.convertToString(episodeOfCare.getPeriod().getStart()) : NA;
        String agent = episodeOfCare.getCareManager() != null ? episodeOfCare.getCareManager().getDisplay() : NA;


        return new StringJoiner("-").add(eocType).add(date).add(agent).toString();
    }

    private static ReferenceDto convertReferenceToReferenceDto(Reference reference) {
        ReferenceDto referenceDto = new ReferenceDto();
        referenceDto.setDisplay(reference.getDisplay());
        referenceDto.setReference(reference.getReference());
        return referenceDto;
    }
}

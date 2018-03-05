package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.EpisodeOfCareDto;
//import gov.samhsa.ocp.ocpfis.util.FhirUtils;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.List;

public class EpisodeOfCareToEpisodeOfCareDtoMapper {

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
}

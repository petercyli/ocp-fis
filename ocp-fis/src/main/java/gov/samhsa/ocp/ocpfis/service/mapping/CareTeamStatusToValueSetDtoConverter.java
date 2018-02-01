package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

@Component
public class CareTeamStatusToValueSetDtoConverter extends AbstractConverter<CareTeam.CareTeamStatus,ValueSetDto>{
    @Override
    protected ValueSetDto convert(CareTeam.CareTeamStatus source) {
        ValueSetDto tempValueSetDto=new ValueSetDto();
        if(source !=null){
            if(source.getDefinition() !=null)
                tempValueSetDto.setDefinition(source.getDefinition());
            if(source.getDisplay() !=null)
                tempValueSetDto.setDisplay(source.getDisplay());
            if(source.getSystem()!=null)
                tempValueSetDto.setSystem(source.getSystem());
        }
        return tempValueSetDto;
    }
}

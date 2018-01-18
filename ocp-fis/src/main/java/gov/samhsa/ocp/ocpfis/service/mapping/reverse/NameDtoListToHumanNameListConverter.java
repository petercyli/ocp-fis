package gov.samhsa.ocp.ocpfis.service.mapping.reverse;

import gov.samhsa.ocp.ocpfis.service.dto.NameDto;
import org.hl7.fhir.dstu3.model.HumanName;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NameDtoListToHumanNameListConverter extends AbstractConverter<List<NameDto>, List<HumanName>> {

    @Override
    protected List<HumanName> convert(List<NameDto> source) {
        List<HumanName> humanNameList = new ArrayList<>();
        if (source != null && source.size() > 0) {
            for (NameDto tempNameDto : source) {
                HumanName tempHumanName = new HumanName();
                if (tempNameDto.getLastName() != null)
                    tempHumanName.setFamily(tempNameDto.getLastName());

                if (tempNameDto.getFirstName() != null) {
                    tempHumanName.addGiven(tempNameDto.getFirstName());
                }

                humanNameList.add(tempHumanName);
            }
        }

        return humanNameList;
    }
}

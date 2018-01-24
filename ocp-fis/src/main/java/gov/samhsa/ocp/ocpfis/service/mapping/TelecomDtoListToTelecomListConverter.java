package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelecomDtoListToTelecomListConverter extends AbstractConverter<List<TelecomDto>, List<ContactPoint>> {

    @Override
    protected List<ContactPoint> convert(List<TelecomDto> source) {
        List<ContactPoint> telecomList = new ArrayList<>();
        if (source != null && source.size() > 0) {

            for (TelecomDto tempTelecomDto : source) {
                ContactPoint tempTelecom = new ContactPoint();
                if (tempTelecomDto.getValue() != null)
                    tempTelecom.setValue(tempTelecomDto.getValue());
                if (tempTelecomDto.getSystem() != null)
                    tempTelecom.setSystem(ContactPoint.ContactPointSystem.valueOf(tempTelecomDto.getSystem().toUpperCase()));
                if (tempTelecomDto.getUse() != null)
                    tempTelecom.setUse(ContactPoint.ContactPointUse.valueOf(tempTelecomDto.getUse().toString()));
                telecomList.add(tempTelecom);
            }
        }
        return telecomList;
    }
}

package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.modelmapper.AbstractConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TelecomListToTelecomDtoListConverter extends AbstractConverter<List<ContactPoint>, List<TelecomDto>> {

    @Override
    protected List<TelecomDto> convert(List<ContactPoint> source) {
        List<TelecomDto> telecomDtoList = new ArrayList<>();

        if (source != null && source.size() > 0) {

            for (ContactPoint tempTelecom : source) {
                TelecomDto tempTelecomDto = new TelecomDto();

                if (tempTelecom.getSystem() != null && tempTelecom.getSystem().toCode().equalsIgnoreCase("phone")) {
                    String phoneNumber = tempTelecom.getValue();
                    if(phoneNumber != null && !phoneNumber.trim().isEmpty()){
                        String formattedPhone = String.valueOf(phoneNumber).replaceFirst("(\\d{3})(\\d{3})(\\d+)", "$1-$2-$3");
                        tempTelecomDto.setValue(Optional.ofNullable(formattedPhone));
                    }
                    tempTelecomDto.setSystem(Optional.ofNullable(tempTelecom.getSystem().toCode()));
                } else {
                    tempTelecomDto.setValue(Optional.ofNullable(tempTelecom.getValue()));
                }
                tempTelecomDto.setSystem(Optional.ofNullable(tempTelecom.getSystem().toCode()));
                if (tempTelecom.getUse() != null) {
                    tempTelecomDto.setUse(Optional.ofNullable(tempTelecom.getUse().toCode()));
                }
                telecomDtoList.add(tempTelecomDto);
            }

        }
        return telecomDtoList;
    }
}

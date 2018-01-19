package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.LocationInfoEnum;
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
            int rank = 0;
            for (TelecomDto tempTelecomDto : source) {
                ContactPoint tempContactPoint = new ContactPoint();

                tempContactPoint.setRank(++rank);

                if (tempTelecomDto.getSystem() != null && !tempTelecomDto.getSystem().trim().isEmpty()) {
                    for (LocationInfoEnum.LocationTelecomSystem addrSystem : LocationInfoEnum.LocationTelecomSystem.values()) {
                        if (addrSystem.name().equalsIgnoreCase(tempTelecomDto.getSystem())) {
                            tempContactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(addrSystem.name().toUpperCase()));
                        }
                    }
                }

                if (tempTelecomDto.getValue() != null && !tempTelecomDto.getValue().isEmpty()) {
                    tempContactPoint.setValue(tempTelecomDto.getValue());
                }

                if (tempTelecomDto.getUse() != null && !tempTelecomDto.getUse().trim().isEmpty()) {
                    for (LocationInfoEnum.LocationTelecomUse addrUse : LocationInfoEnum.LocationTelecomUse.values()) {
                        if (addrUse.name().equalsIgnoreCase(tempTelecomDto.getUse())) {
                            tempContactPoint.setUse(ContactPoint.ContactPointUse.valueOf(addrUse.name().toUpperCase()));
                        }
                    }
                }
                telecomList.add(tempContactPoint);
            }
        }

        return telecomList;
    }

}

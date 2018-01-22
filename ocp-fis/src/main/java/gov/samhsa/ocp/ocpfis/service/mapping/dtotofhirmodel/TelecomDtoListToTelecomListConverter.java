package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.modelmapper.AbstractConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelecomDtoListToTelecomListConverter extends AbstractConverter<List<TelecomDto>, List<ContactPoint>> {
    @Autowired
    private LookUpService lookUpService;

    @Override
    protected List<ContactPoint> convert(List<TelecomDto> source) {
        List<ContactPoint> telecomList = new ArrayList<>();
        List<ValueSetDto> validTelecomUses =  lookUpService.getTelecomUses();
        List<ValueSetDto> validTelecomSystems =  lookUpService.getTelecomSystems();

        if (source != null && source.size() > 0) {
            int rank = 0;
            for (TelecomDto tempTelecomDto : source) {
                ContactPoint tempContactPoint = new ContactPoint();

                tempContactPoint.setRank(++rank);

                if (tempTelecomDto.getSystem() != null && !tempTelecomDto.getSystem().trim().isEmpty()) {
                    for (ValueSetDto validTelecomSystem : validTelecomSystems) {
                        if (validTelecomSystem.getDisplay().equalsIgnoreCase(tempTelecomDto.getSystem())) {
                            tempContactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(validTelecomSystem.getDisplay().toUpperCase()));
                        }
                    }
                }

                if (tempTelecomDto.getValue() != null && !tempTelecomDto.getValue().isEmpty()) {
                    tempContactPoint.setValue(tempTelecomDto.getValue());
                }

                if (tempTelecomDto.getUse() != null && !tempTelecomDto.getUse().trim().isEmpty()) {
                    for (ValueSetDto validTelecomUse : validTelecomUses) {
                        if (validTelecomUse.getDisplay().equalsIgnoreCase(tempTelecomDto.getUse())) {
                            tempContactPoint.setUse(ContactPoint.ContactPointUse.valueOf(validTelecomUse.getDisplay().toUpperCase()));
                        }
                    }
                }
                telecomList.add(tempContactPoint);
            }
        }

        return telecomList;
    }

}

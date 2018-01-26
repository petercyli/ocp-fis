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

                tempTelecomDto.getSystem().ifPresent(obj -> {
                            for (ValueSetDto validTelecomSystem : validTelecomSystems) {
                                if (validTelecomSystem.getDisplay().equalsIgnoreCase(obj)) {
                                    tempContactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(validTelecomSystem.getDisplay().toUpperCase()));
                                }
                            }
                        });

                tempTelecomDto.getValue().ifPresent(obj -> {
                    tempContactPoint.setValue(obj);
                });

                tempTelecomDto.getUse().ifPresent(obj -> {
                    for (ValueSetDto validTelecomUse : validTelecomUses) {
                        if (validTelecomUse.getDisplay().equalsIgnoreCase(obj)) {
                            tempContactPoint.setUse(ContactPoint.ContactPointUse.valueOf(validTelecomUse.getDisplay().toUpperCase()));
                        }
                    }
                });

                telecomList.add(tempContactPoint);
            }
        }

        return telecomList;
    }

}

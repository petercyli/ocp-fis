package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class RaceValidator implements ConstraintValidator<RaceConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(RaceConstraint raceConstraint) {

    }

    @Override
    public boolean isValid(String raceCodeToCheck, ConstraintValidatorContext cxt) {

        if(raceCodeToCheck == null) {
            //this value is optional
            return true;
        }

        List<ValueSetDto> list = lookUpService.getUSCoreRace();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(raceCodeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid race code.");
        }

        return isValid;
    }
}

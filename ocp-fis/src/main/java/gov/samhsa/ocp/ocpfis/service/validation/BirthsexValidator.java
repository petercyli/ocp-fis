package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class BirthsexValidator implements ConstraintValidator<BirthsexConstraint, String> {
    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(BirthsexConstraint birthsexConstraint) {

    }

    @Override
    public boolean isValid(String birthsexToCheck, ConstraintValidatorContext cxt) {

        if (birthsexToCheck == null) {
            //this value is optional
            return true;
        }

        List<ValueSetDto> list = lookUpService.getUSCoreBirthSex();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(birthsexToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid Birthsex code.");
        }

        return isValid;

    }
}

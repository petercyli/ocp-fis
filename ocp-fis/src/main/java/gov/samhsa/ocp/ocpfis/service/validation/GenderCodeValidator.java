package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class GenderCodeValidator implements ConstraintValidator<GenderCodeConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(GenderCodeConstraint statusCodeConstraint) {

    }

    @Override
    public boolean isValid(String genderCodeToCheck, ConstraintValidatorContext cxt) {

        List<ValueSetDto> list = lookUpService.getAdministrativeGenders();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(genderCodeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid genderCode");
        }

        return isValid;
    }
}

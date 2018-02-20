package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class StateCodeValidator implements ConstraintValidator<StateCodeConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(StateCodeConstraint statusCodeConstraint) {

    }

    @Override
    public boolean isValid(String stateCodeToCheck, ConstraintValidatorContext cxt) {

        if (stateCodeToCheck == null || stateCodeToCheck.isEmpty()) {
            //this is optional field
            return true;
        }

        List<ValueSetDto> list = lookUpService.getUspsStates();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(stateCodeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid State Code");
        }

        return isValid;
    }
}

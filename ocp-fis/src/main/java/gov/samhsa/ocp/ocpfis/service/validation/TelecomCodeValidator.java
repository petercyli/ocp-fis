package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class TelecomCodeValidator implements ConstraintValidator<TelecomCodeConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(TelecomCodeConstraint statusCodeConstraint) {

    }

    @Override
    public boolean isValid(String telecomCodeToCheck, ConstraintValidatorContext cxt) {

        if (telecomCodeToCheck == null || telecomCodeToCheck.isEmpty()) {
            return true;
        }

        List<ValueSetDto> list = lookUpService.getTelecomSystems();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(telecomCodeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid Telecom code");
        }

        return isValid;
    }
}

package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class StatusCodeValidator implements ConstraintValidator<StatusCodeConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(StatusCodeConstraint statusCodeConstraint) {

    }

    @Override
    public boolean isValid(String statusCodeToCheck, ConstraintValidatorContext cxt) {

        List<ValueSetDto> list = lookUpService.getCareTeamStatuses();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(statusCodeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid Status Code for CareTeam");
        }

        return isValid;
    }
}

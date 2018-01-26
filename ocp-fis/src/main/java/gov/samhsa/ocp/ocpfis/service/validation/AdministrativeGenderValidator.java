package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class AdministrativeGenderValidator implements ConstraintValidator<AdministrativeGenderConstraint, String> {
    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(AdministrativeGenderConstraint administrativeGenderConstraint) {

    }

    @Override
    public boolean isValid(String administrativeGenderCodeToCheck, ConstraintValidatorContext cxt) {

        if (administrativeGenderCodeToCheck == null) {
            //this value is optional
            return true;
        }

        List<ValueSetDto> list = lookUpService.getAdministrativeGenders();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(administrativeGenderCodeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid AdministrativeGender code.");
        }

        return isValid;

    }
}

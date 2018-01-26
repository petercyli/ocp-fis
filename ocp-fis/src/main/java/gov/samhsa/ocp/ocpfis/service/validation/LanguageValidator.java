package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class LanguageValidator implements ConstraintValidator<LanguageConstraint, String> {
    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(LanguageConstraint languageConstraint) {

    }

    @Override
    public boolean isValid(String languageToCheck, ConstraintValidatorContext cxt) {

        if(languageToCheck == null) {
            //this value is optional
            return true;
        }

        List<ValueSetDto> list = lookUpService.getLanguages();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(languageToCheck));

        if(!isValid) {
            throw new InvalidValueException("Received invalid Language code.");
        }

        return isValid;
    }
}

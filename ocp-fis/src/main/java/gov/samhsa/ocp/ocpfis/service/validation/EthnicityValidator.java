package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class EthnicityValidator implements ConstraintValidator<EthnicityConstraint, String> {
    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(EthnicityConstraint ethnicityConstraint) {

    }

    @Override
    public boolean isValid(String ethnicityConstraintToCheck, ConstraintValidatorContext cxt) {

        if(ethnicityConstraintToCheck == null) {
            //this value is optional
            return true;
        }

        List<ValueSetDto> list = lookUpService.getUSCoreEthnicity();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(ethnicityConstraintToCheck));

        if(!isValid) {
            throw new InvalidValueException("Received invalid Ethnicity code.");
        }

        return isValid;
    }
}

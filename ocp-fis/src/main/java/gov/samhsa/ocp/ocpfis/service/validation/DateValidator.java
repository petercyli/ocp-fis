package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateValidator implements ConstraintValidator<DateConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(DateConstraint statusCodeConstraint) {

    }

    @Override
    public boolean isValid(String dateToCheck, ConstraintValidatorContext cxt) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        boolean isValid = false;
        try {
            Date date = sdf.parse(dateToCheck);
            isValid = true;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format " + e);
        }

        return isValid;
    }

}

package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class RelationshipCodeValidator implements ConstraintValidator<RelationshipCodeConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    @Override
    public void initialize(RelationshipCodeConstraint statusCodeConstraint) {

    }

    @Override
    public boolean isValid(String relationshipCodeToCheck, ConstraintValidatorContext cxt) {

        List<ValueSetDto> list = lookUpService.getRelatedPersonPatientRelationshipTypes();

        boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(relationshipCodeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid PatientRelationshipType");
        }

        return isValid;
    }
}

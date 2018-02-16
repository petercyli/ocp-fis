package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierDto;
import gov.samhsa.ocp.ocpfis.service.dto.IdentifierSystemDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RelatedPersonIdentifierTypeValidator implements ConstraintValidator<RelatedPersonIdentifierTypeConstraint, String> {

    @Autowired
    LookUpService lookUpService;

    final List<String> allowedPatientIdentifierTypes = Arrays.asList("DL", "PPN", "TAX", "MR", "DR", "SB");

    @Override
    public void initialize(RelatedPersonIdentifierTypeConstraint statusCodeConstraint) {

    }

    @Override
    public boolean isValid(String identifierTypeToCheck, ConstraintValidatorContext cxt) {

        List<IdentifierSystemDto> list = lookUpService.getIdentifierSystems(Optional.of(allowedPatientIdentifierTypes));

        boolean isValid = list.stream().anyMatch(t -> t.getOid().equals(identifierTypeToCheck));

        if (!isValid) {
            throw new InvalidValueException("Received invalid identifier type");
        }

        return isValid;
    }
}

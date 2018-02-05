package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class RoleCodeValidator implements ConstraintValidator<RoleCodeConstraint, String> {

   @Autowired
   LookUpService lookUpService;

   @Override
   public void initialize(RoleCodeConstraint constraint) {
   }

   public boolean isValid(String roleCodeToCheck, ConstraintValidatorContext context) {
      if(roleCodeToCheck == null) {
         //this value is optional
         return true;
      }

      List<ValueSetDto> list = lookUpService.getParticipantRoles();

      boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(roleCodeToCheck));

      if(!isValid) {
         throw new InvalidValueException("Received invalid Role code for a participant");
      }

      return isValid;
   }
}

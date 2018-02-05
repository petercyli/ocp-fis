package gov.samhsa.ocp.ocpfis.service.validation;

import gov.samhsa.ocp.ocpfis.service.LookUpService;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.InvalidValueException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class CareTeamCategoryCodeValidator implements ConstraintValidator<CareTeamCategoryCodeConstraint, String> {

   @Autowired
   LookUpService lookUpService;

   @Override
   public void initialize(CareTeamCategoryCodeConstraint constraint) {
   }

   @Override
   public boolean isValid(String categoryCodeToCheck, ConstraintValidatorContext context) {

      List<ValueSetDto> list = lookUpService.getCareTeamCategories();

      boolean isValid = list.stream().anyMatch(t -> t.getCode().equals(categoryCodeToCheck));

      if(!isValid) {
         throw new InvalidValueException("Received invalid Category Code for CareTeam");
      }

      return isValid;
   }
}

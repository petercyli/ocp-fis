package gov.samhsa.ocp.ocpfis.service.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CareTeamStatusCodeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CareTeamStatusCodeConstraint {
    String message() default "Invalid CareTeam Status Code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

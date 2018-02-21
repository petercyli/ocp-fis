package gov.samhsa.ocp.ocpfis.service.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RelatedPersonIdentifierTypeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RelatedPersonIdentifierTypeConstraint {

    String message() default "Invalid identifier type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

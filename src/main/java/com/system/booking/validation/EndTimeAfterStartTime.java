package com.system.booking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = EndTimeAfterStartTimeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EndTimeAfterStartTime {

    String message() default "End time must be strictly after start time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

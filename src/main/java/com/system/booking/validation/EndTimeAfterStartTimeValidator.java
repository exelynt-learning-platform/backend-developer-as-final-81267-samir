package com.system.booking.validation;

import com.system.booking.dto.request.ReservationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EndTimeAfterStartTimeValidator
        implements ConstraintValidator<EndTimeAfterStartTime, ReservationRequest> {

    @Override
    public boolean isValid(ReservationRequest request, ConstraintValidatorContext context) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true; // individual @NotNull annotations handle null checks
        }
        return request.getEndTime().isAfter(request.getStartTime());
    }
}

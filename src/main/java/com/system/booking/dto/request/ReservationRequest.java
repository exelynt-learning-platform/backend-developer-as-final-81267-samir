package com.system.booking.dto.request;

import com.system.booking.validation.EndTimeAfterStartTime;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EndTimeAfterStartTime
public class ReservationRequest {

    @NotNull(message = "Resource ID is mandatory")
    private Long resourceId;

    @NotNull(message = "Start time is mandatory")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is mandatory")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    @NotNull(message = "Price is mandatory")
    @Positive(message = "Price must be strictly positive")
    private BigDecimal price;
}

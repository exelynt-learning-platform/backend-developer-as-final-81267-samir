package com.system.booking.dto.request;

import com.system.booking.model.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationStatusUpdateRequest {

    @NotNull(message = "Reservation status is mandatory")
    private ReservationStatus status;
}

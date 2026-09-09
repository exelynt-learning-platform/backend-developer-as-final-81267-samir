package com.system.booking.dto.response;

import com.system.booking.model.entity.Reservation;
import com.system.booking.model.enums.ReservationStatus;
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
public class ReservationResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long resourceId;
    private String resourceName;
    private String resourceType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
    private ReservationStatus status;

    public static ReservationResponse fromEntity(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .userId(reservation.getUser() != null ? reservation.getUser().getId() : null)
                .username(reservation.getUser() != null ? reservation.getUser().getUsername() : null)
                .resourceId(reservation.getResource() != null ? reservation.getResource().getId() : null)
                .resourceName(reservation.getResource() != null ? reservation.getResource().getName() : null)
                .resourceType(reservation.getResource() != null ? reservation.getResource().getType() : null)
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .price(reservation.getPrice())
                .status(reservation.getStatus())
                .build();
    }
}

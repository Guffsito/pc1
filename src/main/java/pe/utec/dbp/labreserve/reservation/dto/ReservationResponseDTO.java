package pe.utec.dbp.labreserve.reservation.dto;

import pe.utec.dbp.labreserve.model.LabReservation;

public record ReservationResponseDTO(Long id, Long slotId, String studentUsername, String status) {

    public static ReservationResponseDTO from(LabReservation reservation) {
        return new ReservationResponseDTO(
                reservation.getId(),
                reservation.getSlot().getId(),
                reservation.getStudent().getUsername(),
                reservation.getStatus().name()
        );
    }
}

package pe.utec.dbp.labreserve.reservation.dto;

import pe.utec.dbp.labreserve.model.LabReservation;

public record MyReservationResponseDTO(Long id, String equipmentCode, String status) {

    public static MyReservationResponseDTO from(LabReservation reservation) {
        return new MyReservationResponseDTO(
                reservation.getId(),
                reservation.getSlot().getEquipmentCode(),
                reservation.getStatus().name()
        );
    }
}

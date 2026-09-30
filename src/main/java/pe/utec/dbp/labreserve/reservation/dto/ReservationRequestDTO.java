package pe.utec.dbp.labreserve.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservationRequestDTO(

        @NotBlank(message = "purpose es obligatorio")
        @Size(max = 250, message = "purpose no puede superar 250 caracteres")
        String purpose
) {
}

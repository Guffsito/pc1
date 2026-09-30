package pe.utec.dbp.labreserve.slot.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;

public record SlotRequestDTO(

        @NotBlank(message = "equipmentCode es obligatorio")
        @Size(max = 50, message = "equipmentCode no puede superar 50 caracteres")
        String equipmentCode,

        @NotNull(message = "startTime es obligatorio")
        @Future(message = "startTime debe ser una fecha futura")
        ZonedDateTime startTime,

        @NotNull(message = "endTime es obligatorio")
        @Future(message = "endTime debe ser una fecha futura")
        ZonedDateTime endTime,

        @NotNull(message = "capacity es obligatorio")
        @Min(value = 1, message = "capacity debe ser mayor o igual a 1")
        Integer capacity
) {
}

package pe.utec.dbp.labreserve.laboratory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LaboratoryRequestDTO(

        @NotBlank(message = "name es obligatorio")
        @Size(max = 100, message = "name no puede superar 100 caracteres")
        String name,

        @NotBlank(message = "location es obligatorio")
        @Size(max = 150, message = "location no puede superar 150 caracteres")
        String location,

        @NotNull(message = "managerId es obligatorio")
        Long managerId
) {
}

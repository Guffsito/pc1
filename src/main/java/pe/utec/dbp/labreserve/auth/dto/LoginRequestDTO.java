package pe.utec.dbp.labreserve.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "username es obligatorio")
        String username,

        @NotBlank(message = "password es obligatorio")
        String password
) {
}

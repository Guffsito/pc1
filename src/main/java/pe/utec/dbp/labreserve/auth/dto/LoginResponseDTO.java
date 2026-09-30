package pe.utec.dbp.labreserve.auth.dto;

public record LoginResponseDTO(String token, long expiresIn) {
}

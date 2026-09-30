package pe.utec.dbp.labreserve.auth.dto;

import pe.utec.dbp.labreserve.model.UserAccount;

public record RegisterResponseDTO(Long id, String username, String email) {

    public static RegisterResponseDTO from(UserAccount account) {
        return new RegisterResponseDTO(account.getId(), account.getUsername(), account.getEmail());
    }
}

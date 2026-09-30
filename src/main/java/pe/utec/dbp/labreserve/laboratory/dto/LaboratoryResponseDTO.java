package pe.utec.dbp.labreserve.laboratory.dto;

import pe.utec.dbp.labreserve.model.Laboratory;

public record LaboratoryResponseDTO(Long id, String name, String location, Long managerId, String status) {

    public static LaboratoryResponseDTO from(Laboratory laboratory) {
        return new LaboratoryResponseDTO(
                laboratory.getId(),
                laboratory.getName(),
                laboratory.getLocation(),
                laboratory.getManager().getId(),
                laboratory.getStatus().name()
        );
    }
}

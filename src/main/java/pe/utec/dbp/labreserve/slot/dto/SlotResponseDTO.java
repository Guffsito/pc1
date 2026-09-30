package pe.utec.dbp.labreserve.slot.dto;

import pe.utec.dbp.labreserve.model.EquipmentSlot;

public record SlotResponseDTO(Long id, String laboratoryName, String equipmentCode, String status) {

    public static SlotResponseDTO from(EquipmentSlot slot) {
        return new SlotResponseDTO(
                slot.getId(),
                slot.getLaboratory().getName(),
                slot.getEquipmentCode(),
                slot.getStatus().name()
        );
    }
}

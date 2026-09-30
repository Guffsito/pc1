package pe.utec.dbp.labreserve.slot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utec.dbp.labreserve.laboratory.LaboratoryService;
import pe.utec.dbp.labreserve.model.EquipmentSlot;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.SlotStatus;
import pe.utec.dbp.labreserve.security.AuthenticatedUser;
import pe.utec.dbp.labreserve.shared.BadRequestException;
import pe.utec.dbp.labreserve.shared.ConflictException;
import pe.utec.dbp.labreserve.shared.ForbiddenException;
import pe.utec.dbp.labreserve.shared.PageResponseDTO;
import pe.utec.dbp.labreserve.slot.dto.SlotRequestDTO;
import pe.utec.dbp.labreserve.slot.dto.SlotResponseDTO;

import java.time.ZonedDateTime;

@Service
public class SlotService {

    private final SlotRepository slotRepository;
    private final LaboratoryService laboratoryService;

    public SlotService(SlotRepository slotRepository, LaboratoryService laboratoryService) {
        this.slotRepository = slotRepository;
        this.laboratoryService = laboratoryService;
    }

    @Transactional
    public SlotResponseDTO publish(Long laboratoryId, SlotRequestDTO request, AuthenticatedUser actor) {
        Laboratory laboratory = laboratoryService.getById(laboratoryId);

        if (!laboratory.isActive()) {
            throw new ConflictException("El laboratorio no esta ACTIVE");
        }

        // El admin publica en cualquier laboratorio; el tecnico solo en el que gestiona.
        if (!actor.isAdmin() && !laboratory.isManagedBy(actor.getId())) {
            throw new ForbiddenException("Usted no es el encargado de este laboratorio");
        }

        validateWindow(request.startTime(), request.endTime());

        if (hasOverlap(laboratoryId, request.equipmentCode(), request.startTime(), request.endTime())) {
            throw new ConflictException("El equipo ya tiene un turno en ese horario");
        }

        EquipmentSlot slot = new EquipmentSlot(
                laboratory,
                request.equipmentCode(),
                request.startTime(),
                request.endTime(),
                request.capacity()
        );

        return SlotResponseDTO.from(slotRepository.save(slot));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<SlotResponseDTO> search(Long laboratoryId,
                                                   String equipmentCode,
                                                   ZonedDateTime from,
                                                   int page,
                                                   int size) {

        String code = (equipmentCode == null || equipmentCode.isBlank()) ? null : equipmentCode.trim();

        Page<EquipmentSlot> result = slotRepository.searchOpenSlots(
                laboratoryId,
                code,
                from,
                ZonedDateTime.now(),
                SlotStatus.AVAILABLE,
                PageRequest.of(page, size)
        );

        return PageResponseDTO.map(result, SlotResponseDTO::from);
    }

    boolean hasOverlap(Long laboratoryId, String equipmentCode, ZonedDateTime start, ZonedDateTime end) {
        return slotRepository.findLiveSlotsForEquipment(laboratoryId, equipmentCode).stream()
                .anyMatch(slot -> slot.overlaps(start, end));
    }

    private void validateWindow(ZonedDateTime start, ZonedDateTime end) {
        if (!start.isBefore(end)) {
            throw new BadRequestException("startTime debe ser anterior a endTime");
        }
        if (!start.isAfter(ZonedDateTime.now())) {
            throw new BadRequestException("startTime debe ser una fecha futura");
        }
    }
}

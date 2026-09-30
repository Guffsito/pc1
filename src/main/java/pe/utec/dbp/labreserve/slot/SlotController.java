package pe.utec.dbp.labreserve.slot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utec.dbp.labreserve.security.AuthenticatedUser;
import pe.utec.dbp.labreserve.shared.PageResponseDTO;
import pe.utec.dbp.labreserve.slot.dto.SlotRequestDTO;
import pe.utec.dbp.labreserve.slot.dto.SlotResponseDTO;

import java.time.ZonedDateTime;

@RestController
@Validated
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping("/laboratories/{labId}/slots")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<SlotResponseDTO> publish(@PathVariable Long labId,
                                                   @Valid @RequestBody SlotRequestDTO request,
                                                   @AuthenticationPrincipal AuthenticatedUser actor) {

        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.publish(labId, request, actor));
    }

    @GetMapping("/equipment-slots")
    public ResponseEntity<PageResponseDTO<SlotResponseDTO>> search(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page no puede ser negativo") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size) {

        return ResponseEntity.ok(slotService.search(laboratoryId, equipmentCode, from, page, size));
    }
}

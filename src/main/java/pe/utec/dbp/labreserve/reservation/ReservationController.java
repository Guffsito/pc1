package pe.utec.dbp.labreserve.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.utec.dbp.labreserve.reservation.dto.MyReservationResponseDTO;
import pe.utec.dbp.labreserve.reservation.dto.ReservationRequestDTO;
import pe.utec.dbp.labreserve.reservation.dto.ReservationResponseDTO;
import pe.utec.dbp.labreserve.security.AuthenticatedUser;
import pe.utec.dbp.labreserve.shared.PageResponseDTO;

@RestController
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/equipment-slots/{slotId}/reservations")
    public ResponseEntity<ReservationResponseDTO> reserve(
            @PathVariable Long slotId,
            @Valid @RequestBody ReservationRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser actor) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.reserve(slotId, request, actor));
    }

    @GetMapping("/my-lab-reservations")
    public ResponseEntity<PageResponseDTO<MyReservationResponseDTO>> listMine(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page no puede ser negativo") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size debe ser al menos 1")
            @Max(value = 100, message = "size no puede superar 100") int size,
            @AuthenticationPrincipal AuthenticatedUser actor) {

        return ResponseEntity.ok(reservationService.listMine(actor, status, page, size));
    }
}

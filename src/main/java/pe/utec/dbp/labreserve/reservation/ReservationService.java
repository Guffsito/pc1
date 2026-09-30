package pe.utec.dbp.labreserve.reservation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utec.dbp.labreserve.auth.UserRepository;
import pe.utec.dbp.labreserve.model.EquipmentSlot;
import pe.utec.dbp.labreserve.model.LabReservation;
import pe.utec.dbp.labreserve.model.ReservationStatus;
import pe.utec.dbp.labreserve.model.SlotStatus;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.reservation.dto.MyReservationResponseDTO;
import pe.utec.dbp.labreserve.reservation.dto.ReservationRequestDTO;
import pe.utec.dbp.labreserve.reservation.dto.ReservationResponseDTO;
import pe.utec.dbp.labreserve.security.AuthenticatedUser;
import pe.utec.dbp.labreserve.shared.BadRequestException;
import pe.utec.dbp.labreserve.shared.ConflictException;
import pe.utec.dbp.labreserve.shared.NotFoundException;
import pe.utec.dbp.labreserve.shared.PageResponseDTO;
import pe.utec.dbp.labreserve.slot.SlotRepository;

import java.time.ZonedDateTime;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              SlotRepository slotRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    /**
     * Toda la reserva ocurre en una sola transaccion: se bloquea el turno, se valida
     * capacidad y solapamiento, se guarda la reserva y se actualiza el estado del turno.
     */
    @Transactional
    public ReservationResponseDTO reserve(Long slotId, ReservationRequestDTO request, AuthenticatedUser actor) {
        EquipmentSlot slot = slotRepository.lockById(slotId)
                .orElseThrow(() -> NotFoundException.of("El turno", slotId));

        if (slot.getStatus() == SlotStatus.CANCELLED) {
            throw new ConflictException("El turno esta cancelado");
        }
        if (!slot.getStartTime().isAfter(ZonedDateTime.now())) {
            throw new BadRequestException("El turno ya comenzo");
        }
        if (reservationRepository.existsBySlotAndStudent(slotId, actor.getId())) {
            throw new ConflictException("Usted ya reservo este turno");
        }
        if (overlapsWithExistingReservation(actor.getId(), slot.getStartTime(), slot.getEndTime())) {
            throw new ConflictException("Usted ya tiene una reserva en ese horario");
        }

        long taken = reservationRepository.countActiveBySlot(slotId, ReservationStatus.RESERVED);
        if (taken >= slot.getCapacity()) {
            throw new ConflictException("El turno ya no tiene cupos disponibles");
        }

        UserAccount student = userRepository.findById(actor.getId())
                .orElseThrow(() -> NotFoundException.of("El usuario", actor.getId()));

        LabReservation reservation = reservationRepository.save(
                new LabReservation(slot, student, request.purpose(), ZonedDateTime.now()));

        if (taken + 1 >= slot.getCapacity()) {
            slot.setStatus(SlotStatus.FULL);
            slotRepository.save(slot);
        }

        return ReservationResponseDTO.from(reservation);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<MyReservationResponseDTO> listMine(AuthenticatedUser actor,
                                                              String status,
                                                              int page,
                                                              int size) {
        ReservationStatus filter = parseStatus(status);

        Page<LabReservation> result = reservationRepository.findOwnedBy(
                actor.getId(), filter, PageRequest.of(page, size));

        return PageResponseDTO.map(result, MyReservationResponseDTO::from);
    }

    boolean overlapsWithExistingReservation(Long studentId, ZonedDateTime start, ZonedDateTime end) {
        return reservationRepository.countOverlapping(
                studentId, start, end, ReservationStatus.RESERVED) > 0;
    }

    private ReservationStatus parseStatus(String raw) {
        try {
            return ReservationStatus.parseFilter(raw).orElse(null);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("status debe ser all, reserved, used o cancelled");
        }
    }
}

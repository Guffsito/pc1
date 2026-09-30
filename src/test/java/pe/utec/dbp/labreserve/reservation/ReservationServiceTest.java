package pe.utec.dbp.labreserve.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.utec.dbp.labreserve.auth.UserRepository;
import pe.utec.dbp.labreserve.model.EquipmentSlot;
import pe.utec.dbp.labreserve.model.LabReservation;
import pe.utec.dbp.labreserve.model.LabStatus;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.ReservationStatus;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.SlotStatus;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.reservation.dto.ReservationRequestDTO;
import pe.utec.dbp.labreserve.reservation.dto.ReservationResponseDTO;
import pe.utec.dbp.labreserve.security.AuthenticatedUser;
import pe.utec.dbp.labreserve.shared.ConflictException;
import pe.utec.dbp.labreserve.shared.NotFoundException;
import pe.utec.dbp.labreserve.slot.SlotRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService: reglas de negocio de la reserva")
class ReservationServiceTest {

    private static final Long SLOT_ID = 6L;
    private static final Long STUDENT_ID = 3L;
    private static final ReservationRequestDTO REQUEST =
            new ReservationRequestDTO("Prototipo del curso de Diseno");

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    private EquipmentSlot slot;
    private UserAccount student;
    private AuthenticatedUser actor;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = ZonedDateTime.now().plusDays(5).withHour(10).withMinute(0);
        endTime = startTime.plusHours(2);

        UserAccount technician = new UserAccount(
                "tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN);
        ReflectionTestUtils.setField(technician, "id", 2L);

        Laboratory fabLab = new Laboratory("FabLab", "Pabellon A", technician, LabStatus.ACTIVE);
        ReflectionTestUtils.setField(fabLab, "id", 1L);

        slot = new EquipmentSlot(fabLab, "IMP-3D-04", startTime, endTime, 1);
        ReflectionTestUtils.setField(slot, "id", SLOT_ID);

        student = new UserAccount("raul.lab", "raul@utec.edu.pe", "hash", Role.ROLE_STUDENT);
        ReflectionTestUtils.setField(student, "id", STUDENT_ID);

        actor = new AuthenticatedUser(STUDENT_ID, "raul.lab", null, Role.ROLE_STUDENT);
    }

    @Test
    @DisplayName("rechaza la reserva si el estudiante ya tiene otra que se solapa")
    void rechazaReservaSolapada() {
        when(slotRepository.lockById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(false);
        when(reservationRepository.countOverlapping(
                STUDENT_ID, startTime, endTime, ReservationStatus.RESERVED)).thenReturn(1L);

        assertThatThrownBy(() -> reservationService.reserve(SLOT_ID, REQUEST, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya tiene una reserva en ese horario");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("acepta la reserva cuando el horario esta libre y marca el turno como FULL")
    void aceptaReservaSinSolapamiento() {
        when(slotRepository.lockById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(false);
        when(reservationRepository.countOverlapping(
                STUDENT_ID, startTime, endTime, ReservationStatus.RESERVED)).thenReturn(0L);
        when(reservationRepository.countActiveBySlot(SLOT_ID, ReservationStatus.RESERVED)).thenReturn(0L);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(reservationRepository.save(any(LabReservation.class))).thenAnswer(invocation -> {
            LabReservation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 18L);
            return saved;
        });

        ReservationResponseDTO response = reservationService.reserve(SLOT_ID, REQUEST, actor);

        assertThat(response.id()).isEqualTo(18L);
        assertThat(response.slotId()).isEqualTo(SLOT_ID);
        assertThat(response.studentUsername()).isEqualTo("raul.lab");
        assertThat(response.status()).isEqualTo("RESERVED");
        // La capacidad era 1, asi que el turno queda sin cupos.
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.FULL);
        verify(slotRepository).save(slot);
    }

    @Test
    @DisplayName("no permite reservar dos veces el mismo turno")
    void rechazaReservaDuplicada() {
        when(slotRepository.lockById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.reserve(SLOT_ID, REQUEST, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya reservo este turno");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("rechaza la reserva cuando el turno ya no tiene cupos")
    void rechazaReservaSinCapacidad() {
        when(slotRepository.lockById(SLOT_ID)).thenReturn(Optional.of(slot));
        when(reservationRepository.existsBySlotAndStudent(SLOT_ID, STUDENT_ID)).thenReturn(false);
        when(reservationRepository.countOverlapping(
                STUDENT_ID, startTime, endTime, ReservationStatus.RESERVED)).thenReturn(0L);
        when(reservationRepository.countActiveBySlot(SLOT_ID, ReservationStatus.RESERVED)).thenReturn(1L);

        assertThatThrownBy(() -> reservationService.reserve(SLOT_ID, REQUEST, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cupos disponibles");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("devuelve 404 cuando el turno no existe")
    void rechazaTurnoInexistente() {
        when(slotRepository.lockById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserve(99L, REQUEST, actor))
                .isInstanceOf(NotFoundException.class);

        verify(reservationRepository, never()).countOverlapping(anyLong(), any(), any(), eq(ReservationStatus.RESERVED));
    }
}

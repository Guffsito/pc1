package pe.utec.dbp.labreserve.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import pe.utec.dbp.labreserve.auth.UserRepository;
import pe.utec.dbp.labreserve.laboratory.LaboratoryRepository;
import pe.utec.dbp.labreserve.model.EquipmentSlot;
import pe.utec.dbp.labreserve.model.LabReservation;
import pe.utec.dbp.labreserve.model.LabStatus;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.ReservationStatus;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.reservation.ReservationRepository;
import pe.utec.dbp.labreserve.slot.SlotRepository;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ReservationRepository: reservas propias y restriccion unica")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private UserRepository userRepository;

    private UserAccount raul;
    private UserAccount ana;
    private EquipmentSlot manana;
    private EquipmentSlot tarde;

    @BeforeEach
    void setUp() {
        UserAccount technician = userRepository.save(new UserAccount(
                "tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN));
        raul = userRepository.save(new UserAccount(
                "raul.lab", "raul@utec.edu.pe", "hash", Role.ROLE_STUDENT));
        ana = userRepository.save(new UserAccount(
                "ana.lab", "ana@utec.edu.pe", "hash", Role.ROLE_STUDENT));

        Laboratory fabLab = laboratoryRepository.save(
                new Laboratory("FabLab", "Pabellon A", technician, LabStatus.ACTIVE));

        ZonedDateTime base = ZonedDateTime.now().plusDays(4).withHour(9).withMinute(0).withSecond(0).withNano(0);

        manana = slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-04", base, base.plusHours(3), 2));
        tarde = slotRepository.save(new EquipmentSlot(
                fabLab, "CNC-01", base.plusHours(6), base.plusHours(8), 2));

        reservationRepository.saveAndFlush(
                new LabReservation(manana, raul, "Prototipo del curso de Diseno", ZonedDateTime.now().minusHours(2)));
        reservationRepository.saveAndFlush(
                new LabReservation(tarde, raul, "Corte de piezas", ZonedDateTime.now()));
    }

    @Test
    @DisplayName("la base impide dos reservas del mismo estudiante en el mismo turno")
    void respetaRestriccionUnica() {
        LabReservation duplicada = new LabReservation(manana, raul, "Otra vez", ZonedDateTime.now());

        assertThatThrownBy(() -> reservationRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("otro estudiante si puede reservar el mismo turno mientras haya cupo")
    void permiteOtroEstudianteEnElMismoTurno() {
        LabReservation deAna = new LabReservation(manana, ana, "Practica", ZonedDateTime.now());

        assertThat(reservationRepository.saveAndFlush(deAna).getId()).isNotNull();
    }

    @Test
    @DisplayName("detecta el cruce de horarios del estudiante")
    void cuentaReservasSolapadas() {
        long cruza = reservationRepository.countOverlapping(
                raul.getId(),
                manana.getStartTime().plusHours(1),
                manana.getStartTime().plusHours(2),
                ReservationStatus.RESERVED);

        long noCruza = reservationRepository.countOverlapping(
                raul.getId(),
                manana.getEndTime(),
                manana.getEndTime().plusHours(1),
                ReservationStatus.RESERVED);

        assertThat(cruza).isEqualTo(1);
        assertThat(noCruza).isZero();
    }

    @Test
    @DisplayName("mis reservas salen ordenadas por reservedAt descendente")
    void ordenaPorFechaDeReserva() {
        Page<LabReservation> page = reservationRepository.findOwnedBy(
                raul.getId(), null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(reservation -> reservation.getSlot().getEquipmentCode())
                .containsExactly("CNC-01", "IMP-3D-04");
    }

    @Test
    @DisplayName("solo devuelve las reservas del estudiante autenticado y filtra por estado")
    void filtraPorDuenoYEstado() {
        assertThat(reservationRepository.findOwnedBy(ana.getId(), null, PageRequest.of(0, 10)))
                .isEmpty();

        assertThat(reservationRepository.findOwnedBy(
                raul.getId(), ReservationStatus.CANCELLED, PageRequest.of(0, 10)))
                .isEmpty();

        assertThat(reservationRepository.findOwnedBy(
                raul.getId(), ReservationStatus.RESERVED, PageRequest.of(0, 10)))
                .hasSize(2);
    }

    @Test
    @DisplayName("cuenta los cupos tomados de un turno")
    void cuentaCuposTomados() {
        assertThat(reservationRepository.countActiveBySlot(manana.getId(), ReservationStatus.RESERVED))
                .isEqualTo(1);
        assertThat(reservationRepository.existsBySlotAndStudent(manana.getId(), raul.getId())).isTrue();
        assertThat(reservationRepository.existsBySlotAndStudent(manana.getId(), ana.getId())).isFalse();
    }
}

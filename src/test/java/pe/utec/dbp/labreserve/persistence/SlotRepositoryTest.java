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
import pe.utec.dbp.labreserve.model.LabStatus;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.SlotStatus;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.slot.SlotRepository;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SlotRepository: busqueda por equipo y restriccion unica")
class SlotRepositoryTest {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Laboratory fabLab;
    private Laboratory robotica;
    private ZonedDateTime base;

    @BeforeEach
    void setUp() {
        UserAccount technician = userRepository.save(new UserAccount(
                "tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN));

        fabLab = laboratoryRepository.save(
                new Laboratory("FabLab", "Pabellon A", technician, LabStatus.ACTIVE));
        robotica = laboratoryRepository.save(
                new Laboratory("Robotica", "Pabellon B", technician, LabStatus.ACTIVE));

        base = ZonedDateTime.now().plusDays(10).withHour(9).withMinute(0).withSecond(0).withNano(0);

        slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-04", base, base.plusHours(2), 1));
        slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-07", base.plusDays(1), base.plusDays(1).plusHours(2), 2));
        slotRepository.save(new EquipmentSlot(robotica, "CNC-01", base.plusDays(2), base.plusDays(2).plusHours(2), 3));

        EquipmentSlot pasado = new EquipmentSlot(
                fabLab, "IMP-3D-04", base.minusDays(30), base.minusDays(30).plusHours(2), 1);
        slotRepository.save(pasado);

        EquipmentSlot cancelado = new EquipmentSlot(
                fabLab, "CNC-09", base.plusDays(3), base.plusDays(3).plusHours(2), 1);
        cancelado.setStatus(SlotStatus.CANCELLED);
        slotRepository.save(cancelado);

        slotRepository.flush();
    }

    @Test
    @DisplayName("filtra por codigo parcial de equipo sin distinguir mayusculas")
    void buscaPorCodigoParcial() {
        Page<EquipmentSlot> page = slotRepository.searchOpenSlots(
                null, "imp-3d", null, ZonedDateTime.now(), SlotStatus.AVAILABLE, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(EquipmentSlot::getEquipmentCode)
                .containsExactly("IMP-3D-04", "IMP-3D-07");
    }

    @Test
    @DisplayName("combina el filtro de laboratorio con el de fecha desde")
    void combinaFiltros() {
        Page<EquipmentSlot> page = slotRepository.searchOpenSlots(
                fabLab.getId(), null, base.plusHours(12), ZonedDateTime.now(),
                SlotStatus.AVAILABLE, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEquipmentCode()).isEqualTo("IMP-3D-07");
    }

    @Test
    @DisplayName("deja fuera los turnos pasados, los cancelados y respeta la paginacion")
    void ignoraPasadosYCancelados() {
        Page<EquipmentSlot> primera = slotRepository.searchOpenSlots(
                null, null, null, ZonedDateTime.now(), SlotStatus.AVAILABLE, PageRequest.of(0, 2));

        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(primera.getContent()).hasSize(2);
        assertThat(primera.getContent())
                .extracting(EquipmentSlot::getEquipmentCode)
                .doesNotContain("CNC-09");
    }

    @Test
    @DisplayName("los turnos vigentes del equipo excluyen los cancelados")
    void listaTurnosVigentesDelEquipo() {
        List<EquipmentSlot> vigentes = slotRepository.findLiveSlotsForEquipment(fabLab.getId(), "CNC-09");
        assertThat(vigentes).isEmpty();

        List<EquipmentSlot> impresora = slotRepository.findLiveSlotsForEquipment(fabLab.getId(), "IMP-3D-04");
        assertThat(impresora).hasSize(2);
    }

    @Test
    @DisplayName("la base rechaza dos turnos del mismo equipo con la misma hora de inicio")
    void respetaRestriccionUnica() {
        EquipmentSlot duplicado = new EquipmentSlot(fabLab, "IMP-3D-04", base, base.plusHours(4), 2);

        assertThatThrownBy(() -> slotRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("el mismo codigo de equipo en otro laboratorio si es valido")
    void permiteMismoEquipoEnOtroLaboratorio() {
        EquipmentSlot enOtroLab = new EquipmentSlot(robotica, "IMP-3D-04", base, base.plusHours(2), 1);

        assertThat(slotRepository.saveAndFlush(enOtroLab).getId()).isNotNull();
    }
}

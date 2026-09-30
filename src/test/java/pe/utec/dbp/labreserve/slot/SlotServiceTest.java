package pe.utec.dbp.labreserve.slot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.utec.dbp.labreserve.laboratory.LaboratoryService;
import pe.utec.dbp.labreserve.model.EquipmentSlot;
import pe.utec.dbp.labreserve.model.LabStatus;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.security.AuthenticatedUser;
import pe.utec.dbp.labreserve.shared.BadRequestException;
import pe.utec.dbp.labreserve.shared.ConflictException;
import pe.utec.dbp.labreserve.shared.ForbiddenException;
import pe.utec.dbp.labreserve.slot.dto.SlotRequestDTO;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlotService: publicacion de turnos de equipo")
class SlotServiceTest {

    private static final Long LAB_ID = 1L;
    private static final Long TECHNICIAN_ID = 2L;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private LaboratoryService laboratoryService;

    @InjectMocks
    private SlotService slotService;

    private UserAccount technician;
    private Laboratory fabLab;
    private AuthenticatedUser actor;
    private ZonedDateTime startTime;

    @BeforeEach
    void setUp() {
        technician = new UserAccount("tec.fablab", "tecnico@utec.edu.pe", "hash", Role.ROLE_TECHNICIAN);
        ReflectionTestUtils.setField(technician, "id", TECHNICIAN_ID);

        fabLab = new Laboratory("FabLab", "Pabellon A", technician, LabStatus.ACTIVE);
        ReflectionTestUtils.setField(fabLab, "id", LAB_ID);

        actor = new AuthenticatedUser(TECHNICIAN_ID, "tec.fablab", null, Role.ROLE_TECHNICIAN);
        startTime = ZonedDateTime.now().plusDays(8).withHour(10).withMinute(0);
    }

    @Test
    @DisplayName("rechaza un turno que se cruza con otro del mismo equipo")
    void rechazaTurnoSolapado() {
        EquipmentSlot existing = new EquipmentSlot(
                fabLab, "IMP-3D-04", startTime, startTime.plusHours(2), 1);

        when(laboratoryService.getById(LAB_ID)).thenReturn(fabLab);
        when(slotRepository.findLiveSlotsForEquipment(LAB_ID, "IMP-3D-04")).thenReturn(List.of(existing));

        SlotRequestDTO request = new SlotRequestDTO(
                "IMP-3D-04", startTime.plusHours(1), startTime.plusHours(3), 1);

        assertThatThrownBy(() -> slotService.publish(LAB_ID, request, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya tiene un turno");

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("acepta un turno que solo toca el borde del anterior")
    void aceptaTurnoContiguo() {
        EquipmentSlot existing = new EquipmentSlot(
                fabLab, "IMP-3D-04", startTime, startTime.plusHours(2), 1);

        when(laboratoryService.getById(LAB_ID)).thenReturn(fabLab);
        when(slotRepository.findLiveSlotsForEquipment(LAB_ID, "IMP-3D-04")).thenReturn(List.of(existing));
        when(slotRepository.save(any(EquipmentSlot.class))).thenAnswer(invocation -> {
            EquipmentSlot saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        SlotRequestDTO request = new SlotRequestDTO(
                "IMP-3D-04", startTime.plusHours(2), startTime.plusHours(4), 2);

        assertThat(slotService.publish(LAB_ID, request, actor).status()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("no deja publicar en un laboratorio en mantenimiento")
    void rechazaLaboratorioInactivo() {
        fabLab.setStatus(LabStatus.MAINTENANCE);
        when(laboratoryService.getById(LAB_ID)).thenReturn(fabLab);

        SlotRequestDTO request = new SlotRequestDTO(
                "IMP-3D-04", startTime, startTime.plusHours(2), 1);

        assertThatThrownBy(() -> slotService.publish(LAB_ID, request, actor))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("un tecnico no puede publicar en un laboratorio que no gestiona")
    void rechazaTecnicoAjeno() {
        when(laboratoryService.getById(LAB_ID)).thenReturn(fabLab);

        AuthenticatedUser otro = new AuthenticatedUser(99L, "tec.robotica", null, Role.ROLE_TECHNICIAN);
        SlotRequestDTO request = new SlotRequestDTO(
                "IMP-3D-04", startTime, startTime.plusHours(2), 1);

        assertThatThrownBy(() -> slotService.publish(LAB_ID, request, otro))
                .isInstanceOf(ForbiddenException.class);

        verify(slotRepository, never()).findLiveSlotsForEquipment(anyLong(), anyString());
    }

    @Test
    @DisplayName("exige que startTime sea anterior a endTime")
    void rechazaRangoInvertido() {
        when(laboratoryService.getById(LAB_ID)).thenReturn(fabLab);

        SlotRequestDTO request = new SlotRequestDTO(
                "IMP-3D-04", startTime.plusHours(3), startTime.plusHours(1), 1);

        assertThatThrownBy(() -> slotService.publish(LAB_ID, request, actor))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("anterior a endTime");
    }
}

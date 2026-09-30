package pe.utec.dbp.labreserve.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.utec.dbp.labreserve.auth.UserRepository;
import pe.utec.dbp.labreserve.laboratory.LaboratoryRepository;
import pe.utec.dbp.labreserve.model.EquipmentSlot;
import pe.utec.dbp.labreserve.model.LabStatus;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.slot.SlotRepository;

import java.time.ZonedDateTime;

/**
 * Datos de arranque para poder probar la coleccion de Postman sin crear todo a mano.
 * No se ejecuta durante las pruebas.
 */
@Component
@Profile("!test")
public class SeedData implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final SlotRepository slotRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedData(UserRepository userRepository,
                    LaboratoryRepository laboratoryRepository,
                    SlotRepository slotRepository,
                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.slotRepository = slotRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        UserAccount admin = userRepository.save(new UserAccount(
                "admin.utec", "admin@utec.edu.pe", passwordEncoder.encode("AdminPass2026"), Role.ROLE_ADMIN));

        UserAccount technician = userRepository.save(new UserAccount(
                "tec.fablab", "tecnico@utec.edu.pe", passwordEncoder.encode("TecPass2026"), Role.ROLE_TECHNICIAN));

        userRepository.save(new UserAccount(
                "raul.lab", "raul@utec.edu.pe", passwordEncoder.encode("LabPass2026"), Role.ROLE_STUDENT));

        Laboratory fabLab = laboratoryRepository.save(
                new Laboratory("FabLab", "Pabellon A - piso 3", technician, LabStatus.ACTIVE));

        laboratoryRepository.save(
                new Laboratory("Robotica", "Pabellon B - piso 1", admin, LabStatus.MAINTENANCE));

        ZonedDateTime firstShift = ZonedDateTime.now()
                .plusDays(3)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);

        slotRepository.save(new EquipmentSlot(fabLab, "IMP-3D-04", firstShift, firstShift.plusHours(2), 1));
        slotRepository.save(new EquipmentSlot(fabLab, "CNC-01",
                firstShift.plusDays(1), firstShift.plusDays(1).plusHours(3), 2));
    }
}

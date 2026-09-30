package pe.utec.dbp.labreserve.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.utec.dbp.labreserve.auth.UserRepository;
import pe.utec.dbp.labreserve.laboratory.LaboratoryRepository;
import pe.utec.dbp.labreserve.model.LabStatus;
import pe.utec.dbp.labreserve.model.Laboratory;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.reservation.ReservationRepository;
import pe.utec.dbp.labreserve.slot.SlotRepository;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Recorrido completo de la API")
class LabReserveApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long fabLabId;
    private ZonedDateTime startTime;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        slotRepository.deleteAll();
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new UserAccount(
                "admin.utec", "admin@utec.edu.pe", passwordEncoder.encode("AdminPass2026"), Role.ROLE_ADMIN));
        UserAccount technician = userRepository.save(new UserAccount(
                "tec.fablab", "tecnico@utec.edu.pe", passwordEncoder.encode("TecPass2026"), Role.ROLE_TECHNICIAN));

        fabLabId = laboratoryRepository.save(
                new Laboratory("FabLab", "Pabellon A", technician, LabStatus.ACTIVE)).getId();

        startTime = ZonedDateTime.now().plusDays(6).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("registro, publicacion de turno, reserva y consulta de mis reservas")
    void recorridoFeliz() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"raul.lab","email":"raul@utec.edu.pe","password":"LabPass2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("raul.lab"))
                .andExpect(jsonPath("$.email").value("raul@utec.edu.pe"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String technicianToken = login("tec.fablab", "TecPass2026");
        String studentToken = login("raul.lab", "LabPass2026");

        String slotId = mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(startTime, startTime.plusHours(2), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.laboratoryName").value("FabLab"))
                .andExpect(jsonPath("$.equipmentCode").value("IMP-3D-04"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(slotId, "$.id").toString();

        mockMvc.perform(get("/equipment-slots").param("equipmentCode", "imp-3d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[0].equipmentCode").value("IMP-3D-04"));

        mockMvc.perform(post("/equipment-slots/" + id + "/reservations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"Prototipo del curso de Diseno"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotId").value(Integer.parseInt(id)))
                .andExpect(jsonPath("$.studentUsername").value("raul.lab"))
                .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(get("/my-lab-reservations")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].equipmentCode").value("IMP-3D-04"))
                .andExpect(jsonPath("$.content[0].status").value("RESERVED"));
    }

    @Test
    @DisplayName("el registro valida unicidad, formato de email y largo del password")
    void validacionesDeRegistro() throws Exception {
        registrar("raul.lab", "raul@utec.edu.pe", "LabPass2026").andExpect(status().isCreated());

        registrar("raul.lab", "otro@utec.edu.pe", "LabPass2026").andExpect(status().isConflict());
        registrar("otro.lab", "raul@utec.edu.pe", "LabPass2026").andExpect(status().isConflict());
        registrar("corto.lab", "corto@utec.edu.pe", "1234567").andExpect(status().isBadRequest());
        registrar("mal.lab", "sin-arroba", "LabPass2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/auth/register"));
    }

    @Test
    @DisplayName("las credenciales invalidas devuelven 401")
    void credencialesInvalidas() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"tec.fablab","password":"NoEsMiPassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("solo tecnicos y admin publican turnos, y sin token la respuesta es 401")
    void autorizacionDeTurnos() throws Exception {
        registrar("raul.lab", "raul@utec.edu.pe", "LabPass2026").andExpect(status().isCreated());
        String studentToken = login("raul.lab", "LabPass2026");

        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(startTime, startTime.plusHours(2), 1)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(startTime, startTime.plusHours(2), 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("no se pueden publicar dos turnos cruzados para el mismo equipo")
    void turnosCruzados() throws Exception {
        String technicianToken = login("tec.fablab", "TecPass2026");

        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(startTime, startTime.plusHours(2), 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/laboratories/" + fabLabId + "/slots")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotBody(startTime.plusHours(1), startTime.plusHours(3), 1)))
                .andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions registrar(String username,
                                                                        String email,
                                                                        String password) throws Exception {
        String body = """
                {"username":"%s","email":"%s","password":"%s"}
                """.formatted(username, email, password);

        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.token");
    }

    private String slotBody(ZonedDateTime start, ZonedDateTime end, int capacity) {
        DateTimeFormatter iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return """
                {"equipmentCode":"IMP-3D-04","startTime":"%s","endTime":"%s","capacity":%d}
                """.formatted(start.format(iso), end.format(iso), capacity);
    }
}

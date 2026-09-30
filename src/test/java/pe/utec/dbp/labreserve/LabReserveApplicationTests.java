package pe.utec.dbp.labreserve;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LabReserveApplicationTests {

    @Test
    @DisplayName("el contexto de Spring levanta correctamente")
    void contextLoads() {
    }
}

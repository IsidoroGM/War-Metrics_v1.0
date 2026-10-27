package proyecto.IM.warMetrics.Proyecto1.error;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiExceptionHandlerTest {
    @Test
    void errorInternoIncluyeReferenciaSinExponerLaExcepcion() {
        var response = new ApiExceptionHandler().unexpected(new IllegalStateException("detalle interno de prueba"));
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().message().contains("detalle interno"));
        assertDoesNotThrow(() -> UUID.fromString(response.getBody().reference()));
    }
}

package proyecto.IM.warMetrics.Proyecto1.error;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ApiExceptionHandlerTest {
    @Test
    void conflictoDeUnicidadDevuelve409SinExponerSql() {
        var response = new ApiExceptionHandler().conflict(
            new DataIntegrityViolationException("SQLState 23505: detalle SQL del correo duplicado"));
        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().message().contains("SQL"));
        assertFalse(response.getBody().message().contains("23505"));
    }

    @Test
    void errorInternoIncluyeReferenciaSinExponerLaExcepcion() {
        var response = new ApiExceptionHandler().unexpected(new IllegalStateException("detalle interno de prueba"));
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().message().contains("detalle interno"));
        assertDoesNotThrow(() -> UUID.fromString(response.getBody().reference()));
    }
}

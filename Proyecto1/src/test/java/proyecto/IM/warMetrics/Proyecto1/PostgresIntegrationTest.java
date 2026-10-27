package proyecto.IM.warMetrics.Proyecto1;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;

// Estas pruebas solo pueden recrear el esquema de la base local desechable de CI.
@EnabledIfEnvironmentVariable(named = "PG_TEST_URL",
    matches = "jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):\\d+/warmetrics_test(?:\\?.*)?")
@ActiveProfiles("postgres-test")
class PostgresIntegrationTest extends ApiIntegrationTest {}

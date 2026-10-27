package proyecto.IM.warMetrics.Proyecto1;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "PG_TEST_URL",
    matches = "jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):\\d+/warmetrics_test(?:\\?.*)?")
class PostgresRestartPersistenceTest extends RestartPersistenceTest {
    @Override
    protected String databaseUrl() {
        return System.getenv("PG_TEST_URL");
    }

    @Override
    protected String[] connectionOptions() {
        return new String[]{"--spring.profiles.active=postgres-test"};
    }
}

package proyecto.IM.warMetrics.Proyecto1;

import static org.junit.jupiter.api.Assertions.*;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {
    @Autowired Environment environment;
    @Autowired ObjectMapper mapper;

    Map<String, Object> perfil() {
        return new HashMap<>(Map.of("numAtaques", 10, "danoPorAtaque", 1, "impactoX", 3,
            "herirX", 4, "salvacionX", 5, "noHayDolorX", 0));
    }

    final class Client {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies)
            .version(HttpClient.Version.HTTP_1_1).build();
        String token;
        String header;
        long userId;

        Response call(String method, String path, Object body) throws Exception {
            return call(method, path, body, true);
        }

        Response call(String method, String path, Object body, boolean sendCsrf) throws Exception {
            if (!method.equals("GET") && sendCsrf && token == null) csrf();
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"
                + environment.getProperty("local.server.port") + path));
            if (body != null) request.header("Content-Type", "application/json");
            if (!method.equals("GET") && sendCsrf) request.header(header, token);
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode data = response.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(response.body());
            return new Response(response.statusCode(), data, response.headers());
        }

        void csrf() throws Exception {
            var response = call("GET", "/api/auth/csrf", null);
            assertEquals(200, response.status());
            token = response.body().path("token").asText();
            header = response.body().path("headerName").asText();
        }

        String register() throws Exception {
            String name = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            assertEquals(201, call("POST", "/api/auth/registro",
                Map.of("username", name, "email", name + "@example.test", "password", "AuditPass1!")).status());
            return name;
        }

        String loginNew() throws Exception {
            String name = register();
            var response = call("POST", "/api/auth/login", Map.of("username", name, "password", "AuditPass1!"));
            assertEquals(200, response.status(), response.body().toString());
            userId = response.body().path("id").asLong();
            token = null;
            return name;
        }
    }

    record Response(int status, JsonNode body, HttpHeaders headers) {}

    @Test
    void accesoAnonimoYCsrf() throws Exception {
        var client = new Client();
        for (String path : new String[]{"/api/auth/me", "/api/tarjetas", "/api/historial"}) {
            assertEquals(401, client.call("GET", path, null).status());
        }
        var missingCsrf = client.call("POST", "/api/v1/combate/simular", perfil(), false);
        assertEquals(403, missingCsrf.status());
        assertEquals("CSRF_INVALID", missingCsrf.body().path("code").asText());
        assertEquals(403, client.call("POST", "/api/auth/registro", Map.of(), false).status());
        var result = client.call("POST", "/api/v1/combate/simular", perfil());
        assertEquals(200, result.status());
        assertEquals(2.22, result.body().path("dañoMedioEstadistico").asDouble(), 0.001);
        assertFalse(client.cookies.getCookieStore().getCookies().isEmpty());
        assertTrue(client.cookies.getCookieStore().getCookies().stream().allMatch(cookie -> cookie.isHttpOnly()));
    }

    @Test
    void cuentasAisladasEnTarjetasEHistorial() throws Exception {
        var a = new Client();
        var b = new Client();
        a.loginNew();
        b.loginNew();
        var card = perfil();
        card.put("nombre", "<b>Texto de prueba</b>");
        assertEquals(201, a.call("POST", "/api/tarjetas/guardar", card).status());
        var cards = a.call("GET", "/api/tarjetas", null);
        assertEquals(200, cards.status());
        long id = cards.body().get(0).path("id").asLong();
        assertEquals("<b>Texto de prueba</b>", cards.body().get(0).path("nombre").asText());
        assertEquals(403, b.call("GET", "/api/tarjetas/usuario/" + a.userId, null).status());
        assertEquals(404, b.call("PUT", "/api/tarjetas/" + id, card).status());
        assertEquals(404, b.call("DELETE", "/api/tarjetas/" + id, null).status());
        card.put("usuarioId", a.userId);
        assertEquals(403, b.call("POST", "/api/tarjetas/guardar", card).status());
        card.remove("usuarioId");
        card.put("nombre", "Actualizada");
        assertEquals(200, a.call("PUT", "/api/tarjetas/" + id, card).status());
        assertEquals(0, b.call("GET", "/api/tarjetas", null).body().size());

        var history = new HashMap<String, Object>(Map.of("titulo", "Batalla", "aleatorio", 3, "media", 2.22));
        assertEquals(201, a.call("POST", "/api/historial/guardar", history).status());
        long logId = a.call("GET", "/api/historial", null).body().get(0).path("id").asLong();
        assertEquals(403, b.call("GET", "/api/historial/usuario/" + a.userId, null).status());
        assertEquals(404, b.call("DELETE", "/api/historial/" + logId, null).status());
        history.put("usuarioId", a.userId);
        assertEquals(403, b.call("POST", "/api/historial/guardar", history).status());
        assertEquals(204, a.call("DELETE", "/api/historial/" + logId, null).status());
        assertEquals(204, a.call("DELETE", "/api/tarjetas/" + id, null).status());
        assertEquals(0, a.call("GET", "/api/tarjetas", null).body().size());
    }

    @Test
    void validacionesImpidenResultadosYRegistrosInvalidos() throws Exception {
        var client = new Client();
        client.loginNew();
        for (var invalid : Map.<String, Object>of("numAtaques", -10, "danoPorAtaque", 101,
                "impactoX", 7, "herirX", 0, "salvacionX", 8, "noHayDolorX", -1,
                "repeticionImpacto", "INVALID").entrySet()) {
            var req = perfil();
            req.put(invalid.getKey(), invalid.getValue());
            assertEquals(400, client.call("POST", "/api/v1/combate/simular", req).status(), invalid.getKey());
        }
        var huge = perfil();
        huge.put("numAtaques", 10001);
        assertEquals(400, client.call("POST", "/api/v1/combate/simular", huge).status());
        assertEquals(400, client.call("POST", "/api/v1/combate/simular", Map.of()).status());
        var card = perfil();
        card.put("nombre", " ");
        assertEquals(400, client.call("POST", "/api/tarjetas/guardar", card).status());
        card.put("nombre", "Unidad");
        card.put("notas", "a".repeat(501));
        assertEquals(400, client.call("POST", "/api/tarjetas/guardar", card).status());
        assertEquals(400, client.call("POST", "/api/historial/guardar",
            Map.of("titulo", "", "aleatorio", -1, "media", -2)).status());
        assertEquals(0, client.call("GET", "/api/tarjetas", null).body().size());
        assertEquals(400, client.call("POST", "/api/auth/login", Map.of()).status());
        assertEquals(400, client.call("POST", "/api/auth/login",
            Map.of("username", "someone", "password", "á".repeat(40))).status());
        assertEquals(400, client.call("POST", "/api/auth/registro",
            Map.of("username", "validuser", "email", "a@example.test", "password", "A1!" + "a".repeat(70))).status());
    }

    @Test
    void duplicadosYCredencialesTienenErroresClaros() throws Exception {
        var client = new Client();
        String name = client.register();
        var duplicate = client.call("POST", "/api/auth/registro",
            Map.of("username", name, "email", name + "@example.test", "password", "AuditPass1!"));
        assertEquals(409, duplicate.status());
        assertFalse(duplicate.body().path("message").asText().isBlank());
        var missing = client.call("POST", "/api/auth/login", Map.of("username", "nonexistent", "password", "Wrong1!"));
        var wrong = client.call("POST", "/api/auth/login", Map.of("username", name, "password", "Wrong1!"));
        assertEquals(401, missing.status());
        assertEquals(missing.body().path("message").asText(), wrong.body().path("message").asText());
    }

    @Test
    void cantidadesEnterasNoAceptanDecimalesTruncados() throws Exception {
        var client = new Client();
        for (String field : new String[]{"numAtaques", "danoPorAtaque", "impactoX", "herirX", "salvacionX", "noHayDolorX"}) {
            var req = perfil();
            req.put(field, ((Number) req.get(field)).doubleValue() + 0.5);
            var response = client.call("POST", "/api/v1/combate/simular", req);
            assertEquals(400, response.status(), "No se debe truncar " + field);
        }
    }

    @Test
    void loginRotaSesionYCsrfYLogoutInvalidaAcceso() throws Exception {
        var client = new Client();
        String name = client.register();
        String oldToken = client.token;
        String oldSession = client.cookies.getCookieStore().getCookies().get(0).getValue();
        assertEquals(200, client.call("POST", "/api/auth/login",
            Map.of("username", name, "password", "AuditPass1!")).status());
        assertNotEquals(oldSession, client.cookies.getCookieStore().getCookies().get(0).getValue());
        assertEquals(200, client.call("GET", "/api/auth/me", null).status());
        client.token = oldToken;
        var staleCsrf = client.call("POST", "/api/v1/combate/simular", perfil());
        assertEquals(403, staleCsrf.status());
        assertEquals("CSRF_INVALID", staleCsrf.body().path("code").asText());
        client.token = null;
        assertEquals(200, client.call("POST", "/api/v1/combate/simular", perfil()).status());
        assertEquals(204, client.call("POST", "/api/auth/logout", null).status());
        assertEquals(401, client.call("GET", "/api/auth/me", null).status());
        assertEquals(401, client.call("GET", "/api/tarjetas", null).status());
    }

    @Test
    void soloHealthEsPublicoEnActuator() throws Exception {
        var client = new Client();
        var health = client.call("GET", "/actuator/health", null);
        assertEquals(200, health.status());
        assertEquals("UP", health.body().path("status").asText());
        assertFalse(health.body().has("components"));
        assertNotEquals(200, client.call("GET", "/actuator/env", null).status());
        client.loginNew();
        for (String path : new String[]{"/actuator/env", "/actuator/mappings", "/actuator/loggers"}) {
            var forbidden = client.call("GET", path, null);
            assertEquals(403, forbidden.status());
            assertEquals("FORBIDDEN", forbidden.body().path("code").asText());
        }
    }
}

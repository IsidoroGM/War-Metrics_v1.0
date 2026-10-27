package proyecto.IM.warMetrics.Proyecto1;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import proyecto.IM.warMetrics.Proyecto1.model.*;
import proyecto.IM.warMetrics.Proyecto1.repository.*;

class RestartPersistenceTest {
    @TempDir Path temp;

    private ConfigurableApplicationContext start(String database) {
        return new SpringApplicationBuilder(Proyecto1Application.class).run(
            "--server.port=0", "--spring.datasource.url=" + database,
            "--spring.jpa.hibernate.ddl-auto=update", "--spring.main.banner-mode=off",
            "--logging.level.root=WARN");
    }

    @Test
    void usuariosTarjetasEHistorialSobrevivenAlReinicio() {
        String database = "jdbc:h2:file:" + temp.resolve("persistencia").toAbsolutePath().toString().replace('\\', '/')
            + ";DB_CLOSE_ON_EXIT=FALSE";
        Long userId;
        try (var first = start(database)) {
            var usuarios = first.getBean(UsuarioRepository.class);
            var password = first.getBean(PasswordEncoder.class).encode("AuditPass1!");
            var user = usuarios.saveAndFlush(new Usuario("persistencia", password, "persistencia@example.test"));
            userId = user.getId();
            var card = new TarjetaUnidad();
            card.setUsuario(user);
            card.setNombre("Unidad persistente");
            card.setNumAtaques(10);
            card.setDanoPorAtaque(1);
            card.setImpactoX(3);
            card.setHerirX(4);
            card.setSalvacionX(5);
            first.getBean(TarjetaUnidadRepository.class).saveAndFlush(card);
            first.getBean(HistorialTiradaRepository.class)
                .saveAndFlush(new HistorialTirada("Batalla persistente", "", 3, 2.22, user));
        }
        try (var second = start(database)) {
            var user = second.getBean(UsuarioRepository.class).findById(userId).orElseThrow();
            assertEquals("persistencia", user.getUsername());
            assertTrue(second.getBean(PasswordEncoder.class).matches("AuditPass1!", user.getPassword()));
            assertEquals("Unidad persistente",
                second.getBean(TarjetaUnidadRepository.class).findByUsuarioId(userId).getFirst().getNombre());
            assertEquals("Batalla persistente",
                second.getBean(HistorialTiradaRepository.class).findByUsuarioIdOrderByFechaCreacionDesc(userId)
                    .getFirst().getTitulo());
        }
    }
}

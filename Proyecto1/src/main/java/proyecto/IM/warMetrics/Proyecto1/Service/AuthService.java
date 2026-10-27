package proyecto.IM.warMetrics.Proyecto1.Service;

import java.util.Locale;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import proyecto.IM.warMetrics.Proyecto1.dto.RegistroRequest;
import proyecto.IM.warMetrics.Proyecto1.model.Usuario;
import proyecto.IM.warMetrics.Proyecto1.repository.UsuarioRepository;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPassword;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.dummyPassword = passwordEncoder.encode("WarMetrics-placeholder-1!");
    }

    public void registrarUsuario(RegistroRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByUsername(username) || usuarios.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario o el correo ya están registrados.");
        }
        usuarios.saveAndFlush(new Usuario(username, passwordEncoder.encode(request.getPassword()), email));
    }

    public Usuario login(String username, String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña supera el tamaño permitido.");
        }
        Usuario user = usuarios.findByUsername(username.trim()).orElse(null);
        boolean matches = passwordEncoder.matches(password, user == null ? dummyPassword : user.getPassword());
        if (user == null || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
        }
        return user;
    }
}

package proyecto.IM.warMetrics.Proyecto1.Service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import proyecto.IM.warMetrics.Proyecto1.config.SessionUser;
import proyecto.IM.warMetrics.Proyecto1.model.Usuario;
import proyecto.IM.warMetrics.Proyecto1.repository.UsuarioRepository;

@Service
public class CurrentUserService {
    private final UsuarioRepository usuarios;

    public CurrentUserService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar.");
        }
        return usuarios.findById(user.id()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión ya no es válida. Inicia sesión de nuevo."));
    }

    public void checkRequestedId(Long requestedId, Usuario user) {
        if (requestedId != null && !requestedId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes acceder a datos de otra cuenta.");
        }
    }
}

package proyecto.IM.warMetrics.Proyecto1.controller;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import proyecto.IM.warMetrics.Proyecto1.Service.AuthService;
import proyecto.IM.warMetrics.Proyecto1.Service.CurrentUserService;
import proyecto.IM.warMetrics.Proyecto1.config.SessionUser;
import proyecto.IM.warMetrics.Proyecto1.dto.LoginRequest;
import proyecto.IM.warMetrics.Proyecto1.dto.RegistroRequest;
import proyecto.IM.warMetrics.Proyecto1.model.Usuario;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUser;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    public AuthController(AuthService authService, CurrentUserService currentUser,
            SecurityContextRepository contextRepository, SessionAuthenticationStrategy sessionStrategy) {
        this.authService = authService;
        this.currentUser = currentUser;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody RegistroRequest request) {
        authService.registrarUsuario(request);
        return ResponseEntity.status(201).body(Map.of("message", "Usuario registrado con éxito. Ya puedes iniciar sesión."));
    }

    @PostMapping("/login")
    public SessionUser login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        Usuario usuario = authService.login(request.getUsername(), request.getPassword());
        SessionUser user = new SessionUser(usuario.getId(), usuario.getUsername());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
        sessionStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return user;
    }

    @GetMapping("/me")
    public SessionUser me(Authentication authentication) {
        Usuario user = currentUser.require(authentication);
        return new SessionUser(user.getId(), user.getUsername());
    }
}

package proyecto.IM.warMetrics.Proyecto1.controller;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import proyecto.IM.warMetrics.Proyecto1.Service.CurrentUserService;
import proyecto.IM.warMetrics.Proyecto1.dto.GuardarTiradasRequest;
import proyecto.IM.warMetrics.Proyecto1.model.HistorialTirada;
import proyecto.IM.warMetrics.Proyecto1.repository.HistorialTiradaRepository;

@RestController
@RequestMapping("/api/historial")
public class HistorialController {
    private final HistorialTiradaRepository historial;
    private final CurrentUserService currentUser;

    public HistorialController(HistorialTiradaRepository historial, CurrentUserService currentUser) {
        this.historial = historial;
        this.currentUser = currentUser;
    }

    @PostMapping("/guardar")
    public ResponseEntity<Map<String, String>> guardar(@Valid @RequestBody GuardarTiradasRequest req,
            Authentication authentication) {
        var user = currentUser.require(authentication);
        currentUser.checkRequestedId(req.getUsuarioId(), user);
        historial.saveAndFlush(new HistorialTirada(req.getTitulo().trim(), req.getNotas(),
            req.getAleatorio(), req.getMedia(), user));
        return ResponseEntity.status(201).body(Map.of("message", "Tirada guardada en el historial."));
    }

    @GetMapping
    public List<HistorialTirada> obtener(Authentication authentication) {
        return historial.findByUsuarioIdOrderByFechaCreacionDesc(currentUser.require(authentication).getId());
    }

    @GetMapping("/usuario/{id}")
    public List<HistorialTirada> obtenerPorUsuario(@PathVariable Long id, Authentication authentication) {
        var user = currentUser.require(authentication);
        currentUser.checkRequestedId(id, user);
        return historial.findByUsuarioIdOrderByFechaCreacionDesc(user.getId());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> borrar(@PathVariable Long id, Authentication authentication) {
        var tirada = historial.findByIdAndUsuarioId(id, currentUser.require(authentication).getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la tirada."));
        historial.delete(tirada);
        return ResponseEntity.noContent().build();
    }
}

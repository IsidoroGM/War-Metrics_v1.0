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
import proyecto.IM.warMetrics.Proyecto1.dto.TarjetaUnidadRequest;
import proyecto.IM.warMetrics.Proyecto1.model.TarjetaUnidad;
import proyecto.IM.warMetrics.Proyecto1.repository.TarjetaUnidadRepository;

@RestController
@RequestMapping("/api/tarjetas")
public class TarjetaUnidadController {
    private final TarjetaUnidadRepository tarjetas;
    private final CurrentUserService currentUser;

    public TarjetaUnidadController(TarjetaUnidadRepository tarjetas, CurrentUserService currentUser) {
        this.tarjetas = tarjetas;
        this.currentUser = currentUser;
    }

    @PostMapping("/guardar")
    public ResponseEntity<Map<String, String>> guardar(@Valid @RequestBody TarjetaUnidadRequest req,
            Authentication authentication) {
        var user = currentUser.require(authentication);
        currentUser.checkRequestedId(req.getUsuarioId(), user);
        TarjetaUnidad tarjeta = new TarjetaUnidad();
        tarjeta.setUsuario(user);
        copiar(req, tarjeta);
        tarjetas.saveAndFlush(tarjeta);
        return ResponseEntity.status(201).body(Map.of("message", "Tarjeta guardada con éxito."));
    }

    @GetMapping
    public List<TarjetaUnidad> obtener(Authentication authentication) {
        return tarjetas.findByUsuarioId(currentUser.require(authentication).getId());
    }

    // Compatibilidad con el cliente anterior; el ID se comprueba contra la sesión.
    @GetMapping("/usuario/{usuarioId}")
    public List<TarjetaUnidad> obtenerPorUsuario(@PathVariable Long usuarioId, Authentication authentication) {
        var user = currentUser.require(authentication);
        currentUser.checkRequestedId(usuarioId, user);
        return tarjetas.findByUsuarioId(user.getId());
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, String> actualizar(@PathVariable Long id, @Valid @RequestBody TarjetaUnidadRequest req,
            Authentication authentication) {
        var user = currentUser.require(authentication);
        currentUser.checkRequestedId(req.getUsuarioId(), user);
        var tarjeta = tarjetas.findByIdAndUsuarioId(id, user.getId()).orElseThrow(() -> noEncontrada());
        copiar(req, tarjeta);
        tarjetas.saveAndFlush(tarjeta);
        return Map.of("message", "Tarjeta actualizada con éxito.");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> borrar(@PathVariable Long id, Authentication authentication) {
        var tarjeta = tarjetas.findByIdAndUsuarioId(id, currentUser.require(authentication).getId())
            .orElseThrow(() -> noEncontrada());
        tarjetas.delete(tarjeta);
        return ResponseEntity.noContent().build();
    }

    private ResponseStatusException noEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la tarjeta.");
    }

    private void copiar(TarjetaUnidadRequest req, TarjetaUnidad tarjeta) {
        tarjeta.setNombre(req.getNombre().trim());
        tarjeta.setNotas(req.getNotas());
        tarjeta.setNumAtaques(req.getNumAtaques());
        tarjeta.setImpactoX(req.getImpactoX());
        tarjeta.setRepeticionImpacto(req.getRepeticionImpacto());
        tarjeta.setEspecialSeisImpacto(req.getEspecialSeisImpacto());
        tarjeta.setHerirX(req.getHerirX());
        tarjeta.setRepeticionHerir(req.getRepeticionHerir());
        tarjeta.setSeisHeridaInsalvable(req.isSeisHeridaInsalvable());
        tarjeta.setSalvacionX(req.getSalvacionX());
        tarjeta.setDanoPorAtaque(req.getDanoPorAtaque());
        tarjeta.setNoHayDolorX(req.getNoHayDolorX());
    }
}

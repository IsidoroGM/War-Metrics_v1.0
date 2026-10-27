package proyecto.IM.warMetrics.Proyecto1.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import proyecto.IM.warMetrics.Proyecto1.model.HistorialTirada;

@Repository
public interface HistorialTiradaRepository extends JpaRepository<HistorialTirada, Long> {
    Optional<HistorialTirada> findByIdAndUsuarioId(Long id, Long usuarioId);
    
    // Fíjate en cómo nombramos el método: Spring lo traduce solo a SQL ordenado
    List<HistorialTirada> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
}
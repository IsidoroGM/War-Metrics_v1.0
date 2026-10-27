package proyecto.IM.warMetrics.Proyecto1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class TarjetaUnidadRequest extends AtaqueRequest {
    @Positive
    private Long usuarioId;
    @NotBlank(message = "Indica un nombre para la unidad.")
    @Size(max = 120, message = "El nombre admite hasta 120 caracteres.")
    private String nombre;
    @Size(max = 500, message = "Las notas admiten hasta 500 caracteres.")
    private String notas;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}

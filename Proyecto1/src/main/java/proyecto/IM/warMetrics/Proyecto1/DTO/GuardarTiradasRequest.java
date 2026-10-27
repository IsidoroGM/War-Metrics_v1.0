package proyecto.IM.warMetrics.Proyecto1.dto;

import jakarta.validation.constraints.*;

public class GuardarTiradasRequest {
    @Positive
    private Long usuarioId;
    @NotBlank(message = "Indica un título para la tirada.")
    @Size(max = 120, message = "El título admite hasta 120 caracteres.")
    private String titulo;
    @Size(max = 500, message = "Las notas admiten hasta 500 caracteres.")
    private String notas;
    @Min(0) @Max(2000000)
    private int aleatorio;
    @DecimalMin("0") @DecimalMax("2000000")
    private double media;

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public int getAleatorio() { return aleatorio; }
    public void setAleatorio(int aleatorio) { this.aleatorio = aleatorio; }
    public double getMedia() { return media; }
    public void setMedia(double media) { this.media = media; }
}

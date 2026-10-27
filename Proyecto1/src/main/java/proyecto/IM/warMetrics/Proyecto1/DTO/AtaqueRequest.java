package proyecto.IM.warMetrics.Proyecto1.dto;

import jakarta.validation.constraints.*;

/**
 * Esta clase es el molde que recibirá los datos desde el formulario web.
 */
public class AtaqueRequest {
    // Parámetros básicos
    @Min(value = 1, message = "Los ataques deben estar entre 1 y 10000.") @Max(10000)
    private int numAtaques;
    @Min(1) @Max(100)
    private int danoPorAtaque;
    
    // 1º Tirada: Impactar
    @Min(1) @Max(6)
    private int impactoX; 
    @NotNull @Pattern(regexp = "NONE|ONES|ALL", message = "Selecciona una repetición válida.")
    private String repeticionImpacto = "NONE"; // "NONE", "ONES", "ALL"
    @NotNull @Pattern(regexp = "NONE|EXTRA_HIT|AUTO_WOUND", message = "Selecciona una regla válida.")
    private String especialSeisImpacto = "NONE"; // "EXTRA_HIT", "AUTO_WOUND", "NONE"
    
    // 2º Tirada: Herir
    @Min(1) @Max(6)
    private int herirX;
    @NotNull @Pattern(regexp = "NONE|ONES|ALL", message = "Selecciona una repetición válida.")
    private String repeticionHerir = "NONE"; // "NONE", "ONES", "ALL"
    private boolean seisHeridaInsalvable;
    
    // 3º y 4º Tirada: Salvaciones
    @Min(2) @Max(7)
    private int salvacionX;
    @Min(0) @Max(7)
    private int noHayDolorX;

    // Getters y Setters (Necesarios para que Spring Boot procese el JSON)
    public int getNumAtaques() { return numAtaques; }
    public void setNumAtaques(int numAtaques) { this.numAtaques = numAtaques; }

    public int getDanoPorAtaque() { return danoPorAtaque; }
    public void setDanoPorAtaque(int danoPorAtaque) { this.danoPorAtaque = danoPorAtaque; }

    public int getImpactoX() { return impactoX; }
    public void setImpactoX(int impactoX) { this.impactoX = impactoX; }

    public String getRepeticionImpacto() { return repeticionImpacto; }
    public void setRepeticionImpacto(String repeticionImpacto) { this.repeticionImpacto = repeticionImpacto; }

    public String getEspecialSeisImpacto() { return especialSeisImpacto; }
    public void setEspecialSeisImpacto(String especialSeisImpacto) { this.especialSeisImpacto = especialSeisImpacto; }

    public int getHerirX() { return herirX; }
    public void setHerirX(int herirX) { this.herirX = herirX; }

    public String getRepeticionHerir() { return repeticionHerir; }
    public void setRepeticionHerir(String repeticionHerir) { this.repeticionHerir = repeticionHerir; }

    public boolean isSeisHeridaInsalvable() { return seisHeridaInsalvable; }
    public void setSeisHeridaInsalvable(boolean seisHeridaInsalvable) { this.seisHeridaInsalvable = seisHeridaInsalvable; }

    public int getSalvacionX() { return salvacionX; }
    public void setSalvacionX(int salvacionX) { this.salvacionX = salvacionX; }

    public int getNoHayDolorX() { return noHayDolorX; }
    public void setNoHayDolorX(int noHayDolorX) { this.noHayDolorX = noHayDolorX; }

    
}
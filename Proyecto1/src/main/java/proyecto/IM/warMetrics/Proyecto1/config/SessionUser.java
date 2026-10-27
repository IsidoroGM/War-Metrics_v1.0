package proyecto.IM.warMetrics.Proyecto1.config;

import java.io.Serializable;

/** Identidad de la sesión; nunca contiene la contraseña. */
public record SessionUser(Long id, String username) implements Serializable {}

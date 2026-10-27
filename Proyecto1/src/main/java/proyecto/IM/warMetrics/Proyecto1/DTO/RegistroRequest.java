package proyecto.IM.warMetrics.Proyecto1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroRequest {

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(min = 3, max = 20, message = "El usuario debe tener entre 3 y 20 caracteres")
    @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Usa letras sin acentos, números, guion o guion bajo para el usuario.")
    private String username;

    // Aquí está la validación automática de correo (@ y .)
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 254, message = "El correo es demasiado largo.")
    private String email;

    // Aquí está tu regla matemática (Regex) para la contraseña
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
        message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un símbolo especial"
    )
    @Size(max = 72, message = "La contraseña admite hasta 72 caracteres.")
    private String password;

    // Getters y Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
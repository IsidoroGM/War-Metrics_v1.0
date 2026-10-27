package proyecto.IM.warMetrics.Proyecto1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank @Size(max = 20) private String username;
    @NotBlank @Size(max = 72) private String password;

    // Getters y Setters
    public String getUsername() {
         return username; 
        }

    public void setUsername(String username) {
         this.username = username; 
        }

    public String getPassword() { 
        return password; 
        }

    public void setPassword(String password) {
         this.password = password;
        }
}
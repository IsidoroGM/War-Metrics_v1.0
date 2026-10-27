package proyecto.IM.warMetrics.Proyecto1.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import proyecto.IM.warMetrics.Proyecto1.Service.SimuladorService;
import proyecto.IM.warMetrics.Proyecto1.dto.AtaqueRequest;
import proyecto.IM.warMetrics.Proyecto1.dto.ResultadoResponse;


@RestController 
@RequestMapping("/api/v1/combate")
// Permitimos que una web externa o local pueda llamar a este controlador


public class AtaqueController {

    private final SimuladorService simuladorService;

    // Inyectamos el servicio mediante el constructor
    public AtaqueController(SimuladorService simuladorService) {
        this.simuladorService = simuladorService;
    }

    // Endpoint 1: Comprueba si el servidor está operativo (GET)
    // Ruta completa: http://localhost:8080/api/v1/combate/status
    @GetMapping("/status")
    public String comprobarEstado() {
        return "¡El motor de WarMetrics está en línea y listo para la batalla!";
    }

    // Endpoint 2: Realiza el cálculo de la simulación (POST)
    // Ruta completa: http://localhost:8080/api/v1/combate/simular

    @PostMapping("/simular")
    public ResultadoResponse simularAtaque(@Valid @RequestBody AtaqueRequest request) {
        // Ahora devolvemos directamente el objeto con los dos cálculos
        return simuladorService.ejecutarSimulacion(request);
    }

}

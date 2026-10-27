package proyecto.IM.warMetrics.Proyecto1.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import proyecto.IM.warMetrics.Proyecto1.dto.AtaqueRequest;
import proyecto.IM.warMetrics.Proyecto1.dto.ResultadoResponse;

@Service
public class SimuladorService {

    public ResultadoResponse ejecutarSimulacion(AtaqueRequest req) {
        // Ejecutamos los cálculos
        int aleatorio = calcularHeridasAleatorias(req);
        double mediaRedondeada = calcularMediaEstadistica(req);

        return new ResultadoResponse(aleatorio, mediaRedondeada);
    }

    // ------------------- SIMULACIÓN REAL (ALEATORIA) -------------------
    private int calcularHeridasAleatorias(AtaqueRequest req) {
        int impactos = 0;
        int heridasEfectivas = 0;
        int heridasTrasSalvacion = 0;
        int dañoFinal = 0;
        int heridasAutomaticas = 0;

        // 1. Fase de impacto
        for (int i = 0; i < req.getNumAtaques(); i++) {
            // SEGURIDAD: Si repeticion es null, usamos "NONE"
            String repImpacto = req.getRepeticionImpacto() != null ? req.getRepeticionImpacto() : "NONE";
            int resultado = tirarD6(repImpacto, req.getImpactoX());
            
            if (resultado >= req.getImpactoX() || req.getImpactoX() <= 1) {
                impactos++;
                // Reglas especiales en 6s
                if (resultado == 6 && req.getEspecialSeisImpacto() != null) {
                    if ("EXTRA_HIT".equals(req.getEspecialSeisImpacto())) {
                        impactos++;
                    } else if ("AUTO_WOUND".equals(req.getEspecialSeisImpacto())) {
                        heridasAutomaticas++;
                        impactos--; // El impacto se convierte en herida automática
                    }
                }
            }
        }

        // 2. Fase de herir
        for (int i = 0; i < impactos; i++) {
            String repHerir = req.getRepeticionHerir() != null ? req.getRepeticionHerir() : "NONE";
            int resultado = tirarD6(repHerir, req.getHerirX());
            
            if (resultado == 6 && req.isSeisHeridaInsalvable()) {
                heridasTrasSalvacion++;
            } else if (resultado >= req.getHerirX()) {
                heridasEfectivas++;
            }
        }

        heridasEfectivas += heridasAutomaticas;

        // 3. Salvación
        for (int i = 0; i < heridasEfectivas; i++) {
            int tirada = ThreadLocalRandom.current().nextInt(1, 7);
            if (tirada < req.getSalvacionX()) {
                heridasTrasSalvacion++;
            }
        }

        // 4. Daño + FNP (No Hay Dolor)
        // Usamos danoPorAtaque (sin ñ) para asegurar compatibilidad
        int valorDano = req.getDanoPorAtaque() > 0 ? req.getDanoPorAtaque() : 1;

        for (int i = 0; i < heridasTrasSalvacion; i++) {
            for (int d = 0; d < valorDano; d++) {
                if (req.getNoHayDolorX() > 0 && req.getNoHayDolorX() <= 6) {
                    int tirada = ThreadLocalRandom.current().nextInt(1, 7);
                    if (tirada < req.getNoHayDolorX()) {
                        dañoFinal++;
                    }
                } else {
                    dañoFinal++;
                }
            }
        }
        return dañoFinal;
    }

    private int tirarD6(String repeticion, int umbral) {
        int dado = ThreadLocalRandom.current().nextInt(1, 7);
        if ("ONES".equals(repeticion) && dado == 1) {
            dado = ThreadLocalRandom.current().nextInt(1, 7);
        } else if ("ALL".equals(repeticion) && dado < umbral) {
            dado = ThreadLocalRandom.current().nextInt(1, 7);
        }
        return dado;
    }

    // Cada resultado tiene 36 parejas equiprobables de tirada inicial y repetición.
    // Trabajar con recuentos enteros evita probabilidades > 1 y errores de redondeo.
    private double calcularMediaEstadistica(AtaqueRequest req) {
        int impactos = contarExitos(req.getImpactoX(), req.getRepeticionImpacto());
        int seisesImpacto = contarSeises(req.getImpactoX(), req.getRepeticionImpacto());
        int automaticas = 0;
        if ("EXTRA_HIT".equals(req.getEspecialSeisImpacto())) impactos += seisesImpacto;
        if ("AUTO_WOUND".equals(req.getEspecialSeisImpacto())) {
            automaticas = seisesImpacto;
            impactos -= seisesImpacto;
        }
        int heridas = contarExitos(req.getHerirX(), req.getRepeticionHerir());
        int devastadoras = req.isSeisHeridaInsalvable()
            ? contarSeises(req.getHerirX(), req.getRepeticionHerir()) : 0;
        long paraSalvar = automaticas * 36L + impactos * (long) (heridas - devastadoras);
        long insalvables = impactos * (long) devastadoras;
        int falloSalvacion = Math.max(0, Math.min(6, req.getSalvacionX() - 1));
        int falloFnp = req.getNoHayDolorX() > 0 && req.getNoHayDolorX() <= 6
            ? req.getNoHayDolorX() - 1 : 6;
        long numerador = (paraSalvar * falloSalvacion + insalvables * 6)
            * req.getNumAtaques() * req.getDanoPorAtaque() * falloFnp;
        // Denominador: 36 impactos * 36 heridas * 6 salvación * 6 FNP.
        return BigDecimal.valueOf(numerador).divide(BigDecimal.valueOf(46656), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private int contarExitos(int umbral, String repeticion) {
        int exitos = 0;
        for (int primero = 1; primero <= 6; primero++)
        for (int segundo = 1; segundo <= 6; segundo++) {
            if (resultadoRepeticion(primero, segundo, umbral, repeticion) >= umbral) exitos++;
        }
        return exitos;
    }

    private int contarSeises(int umbral, String repeticion) {
        int seises = 0;
        for (int primero = 1; primero <= 6; primero++)
        for (int segundo = 1; segundo <= 6; segundo++) {
            if (resultadoRepeticion(primero, segundo, umbral, repeticion) == 6) seises++;
        }
        return seises;
    }

    private int resultadoRepeticion(int primero, int segundo, int umbral, String repeticion) {
        boolean repetir = ("ONES".equals(repeticion) && primero == 1)
            || ("ALL".equals(repeticion) && primero < umbral);
        return repetir ? segundo : primero;
    }
}

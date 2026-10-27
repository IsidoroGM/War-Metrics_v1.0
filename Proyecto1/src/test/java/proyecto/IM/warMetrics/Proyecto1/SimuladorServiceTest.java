package proyecto.IM.warMetrics.Proyecto1;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import proyecto.IM.warMetrics.Proyecto1.Service.SimuladorService;
import proyecto.IM.warMetrics.Proyecto1.dto.AtaqueRequest;

class SimuladorServiceTest {
    private final SimuladorService simulador = new SimuladorService();

    private AtaqueRequest basico() {
        var req = new AtaqueRequest();
        req.setNumAtaques(10);
        req.setDanoPorAtaque(1);
        req.setImpactoX(3);
        req.setHerirX(4);
        req.setSalvacionX(5);
        return req;
    }

    @Test
    void casoBasico() {
        assertEquals(2.22, simulador.ejecutarSimulacion(basico()).getDañoMedioEstadistico(), 0.001);
    }

    @Test
    void repetirFallosNoEquivaleARepetirTodoSalvoSeises() {
        var req = basico();
        req.setNumAtaques(36);
        req.setRepeticionImpacto("ALL");
        req.setEspecialSeisImpacto("EXTRA_HIT");
        req.setHerirX(2);
        req.setSalvacionX(7);
        assertEquals(33.33, simulador.ejecutarSimulacion(req).getDañoMedioEstadistico(), 0.001);
    }

    @Test
    void repetirUnosConUmbralUnoNoSuperaElCienPorCien() {
        var req = basico();
        req.setNumAtaques(36);
        req.setImpactoX(1);
        req.setRepeticionImpacto("ONES");
        req.setHerirX(2);
        req.setSalvacionX(7);
        assertEquals(30.0, simulador.ejecutarSimulacion(req).getDañoMedioEstadistico(), 0.001);
    }

    @Test
    void redondeaMedioCentimoHaciaArriba() {
        var req = basico();
        req.setNumAtaques(6);
        req.setDanoPorAtaque(2);
        req.setImpactoX(4);
        req.setRepeticionImpacto("ALL");
        req.setHerirX(3);
        req.setRepeticionHerir("ONES");
        req.setSalvacionX(2);
        req.setNoHayDolorX(0);
        req.setSeisHeridaInsalvable(true);
        assertEquals(2.63, simulador.ejecutarSimulacion(req).getDañoMedioEstadistico(), 0.001);
    }

    @Test
    void mediaCoincideConEnumeracionDeLasCarasDelDado() {
        String[] repeticiones = {"NONE", "ONES", "ALL"};
        String[] especiales = {"NONE", "EXTRA_HIT", "AUTO_WOUND"};
        for (int impacto = 1; impacto <= 6; impacto++)
        for (int herir = 1; herir <= 6; herir++)
        for (String repImpacto : repeticiones)
        for (String repHerir : repeticiones)
        for (String especial : especiales)
        for (boolean devastadoras : new boolean[]{false, true})
        for (int salvacion : new int[]{2, 5, 7})
        for (int fnp : new int[]{0, 5}) {
            var req = basico();
            req.setNumAtaques(6);
            req.setDanoPorAtaque(2);
            req.setImpactoX(impacto);
            req.setHerirX(herir);
            req.setRepeticionImpacto(repImpacto);
            req.setRepeticionHerir(repHerir);
            req.setEspecialSeisImpacto(especial);
            req.setSeisHeridaInsalvable(devastadoras);
            req.setSalvacionX(salvacion);
            req.setNoHayDolorX(fnp);
            double esperado = mediaEnumerada(req);
            var resultado = simulador.ejecutarSimulacion(req);
            assertEquals(esperado, resultado.getDañoMedioEstadistico(), 0.005000001,
                "impacto=" + impacto + ", herir=" + herir + ", " + repImpacto + "/" + repHerir + "/" + especial);
            assertTrue(resultado.getDañoAleatorio() >= 0 && resultado.getDañoAleatorio() <= 24);
        }
    }

    // Enumeración independiente de las 36 parejas de tirada inicial y repetición.
    private double[] distribucion(int umbral, String repeticion) {
        double[] prob = new double[7];
        for (int primero = 1; primero <= 6; primero++)
        for (int segundo = 1; segundo <= 6; segundo++) {
            boolean repetir = ("ONES".equals(repeticion) && primero == 1)
                || ("ALL".equals(repeticion) && primero < umbral);
            prob[repetir ? segundo : primero] += 1.0 / 36.0;
        }
        return prob;
    }

    private double mediaEnumerada(AtaqueRequest req) {
        double[] impactos = distribucion(req.getImpactoX(), req.getRepeticionImpacto());
        double[] heridas = distribucion(req.getHerirX(), req.getRepeticionHerir());
        double media = 0;
        for (int impacto = req.getImpactoX(); impacto <= 6; impacto++) {
            boolean automatica = impacto == 6 && "AUTO_WOUND".equals(req.getEspecialSeisImpacto());
            int cantidad = impacto == 6 && "EXTRA_HIT".equals(req.getEspecialSeisImpacto()) ? 2 : 1;
            double heridasQuePasan = 0;
            if (automatica) {
                heridasQuePasan = (req.getSalvacionX() - 1.0) / 6;
            } else {
                for (int herida = req.getHerirX(); herida <= 6; herida++) {
                    double falloSalvacion = herida == 6 && req.isSeisHeridaInsalvable()
                        ? 1 : (req.getSalvacionX() - 1.0) / 6;
                    heridasQuePasan += heridas[herida] * falloSalvacion;
                }
            }
            media += impactos[impacto] * cantidad * heridasQuePasan;
        }
        double falloFnp = req.getNoHayDolorX() == 0 || req.getNoHayDolorX() == 7
            ? 1 : (req.getNoHayDolorX() - 1.0) / 6;
        return req.getNumAtaques() * req.getDanoPorAtaque() * media * falloFnp;
    }
}

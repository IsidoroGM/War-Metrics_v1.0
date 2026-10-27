let ultimoResultado = null;

function idsPerfil(armeria = false) {
    return armeria ? {
        numAtaques: 'nueva-unidad-ataques', danoPorAtaque: 'nueva-unidad-dano',
        impactoX: 'nueva-unidad-impacto', herirX: 'nueva-unidad-herir',
        salvacionX: 'nueva-unidad-salvacion', noHayDolorX: 'nueva-unidad-fnp',
        repeticionImpacto: 'nueva-unidad-rep-impacto', repeticionHerir: 'nueva-unidad-rep-herir',
        especialSeisImpacto: 'nueva-unidad-esp-impacto', seisHeridaInsalvable: 'nueva-unidad-devastadoras'
    } : {
        numAtaques: 'numAtaques', danoPorAtaque: 'dañoPorAtaque', impactoX: 'impactoX',
        herirX: 'herirX', salvacionX: 'salvacionX', noHayDolorX: 'noHayDolorX',
        repeticionImpacto: 'repeticionImpacto', repeticionHerir: 'repeticionHerir',
        especialSeisImpacto: 'especialSeisImpacto', seisHeridaInsalvable: 'seisHeridaInsalvable'
    };
}

function leerPerfil(armeria = false) {
    const result = {};
    for (const [name, id] of Object.entries(idsPerfil(armeria))) {
        const field = document.getElementById(id);
        result[name] = field.type === 'checkbox' ? field.checked
            : field.type === 'number' ? Number(field.value) : field.value;
    }
    return result;
}

async function ejecutarSimulacion() {
    if (!validarCampos(Object.values(idsPerfil()))) return;
    const button = document.getElementById('btn-simular');
    button.disabled = true;
    ultimoResultado = null;
    gestionarPanelGuardado();
    try {
        const perfil = leerPerfil();
        const resultado = await apiRequest('/v1/combate/simular', { method: 'POST', body: perfil });
        mostrarResultadosEnPantalla(resultado);
        ultimoResultado = resultado;
        actualizarResumenRapido(perfil, resultado);
        gestionarPanelGuardado();
        reproducirSonidoTerminal();
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    } finally {
        button.disabled = false;
    }
}

function valoresResultado(res) {
    return {
        real: res.dañoAleatorio ?? res.danoAleatorio,
        media: res.dañoMedioEstadistico ?? res.danoMedioEstadistico
    };
}

function mostrarResultadosEnPantalla(res) {
    const { real, media } = valoresResultado(res);
    if (!Number.isFinite(real) || !Number.isFinite(media)) throw new Error('El servidor devolvió un resultado no válido.');
    document.getElementById('contenedorResultados').style.display = 'flex';
    document.getElementById('resultado-aleatorio').textContent = real;
    document.getElementById('resultado-estadistico').textContent = media.toFixed(2);
}

function gestionarPanelGuardado() {
    const panel = document.getElementById('panel-guardar-tirada');
    panel.classList.toggle('pantalla-oculta', !usuarioActual || !ultimoResultado);
}

function actualizarResumenRapido(perfil, resultado) {
    const list = document.getElementById('quick-stats');
    list.querySelector('p')?.remove();
    const entry = document.createElement('div');
    const { real, media } = valoresResultado(resultado);
    entry.className = 'resumen-entrada';
    entry.textContent = perfil.numAtaques + ' Atqs: ' + real + ' (Med: ' + media.toFixed(2) + ')';
    list.prepend(entry);
    while (list.children.length > 20) list.lastElementChild.remove();
}

window.addEventListener('load', async () => {
    const status = document.getElementById('motor-status');
    try {
        await apiRequest('/v1/combate/status');
        status.textContent = 'ONLINE';
        status.style.color = '#2ecc71';
    } catch {
        status.textContent = 'OFFLINE';
        status.style.color = '#e74c3c';
    }
});

function cambiarPantalla(destino) {
    if (['unidades', 'historial'].includes(destino) && !usuarioActual) {
        mostrarNotificacion('Inicia sesión para acceder a esta sección.', 'error');
        return;
    }
    document.querySelectorAll('.pantalla-seccion').forEach(el => {
        el.classList.toggle('pantalla-oculta', el.id !== 'pantalla-' + destino);
        el.classList.toggle('pantalla-activa', el.id === 'pantalla-' + destino);
    });
    document.querySelectorAll('.sidebar ul li a').forEach(el => el.classList.remove('active'));
    document.getElementById('menu-' + destino)?.classList.add('active');
    document.querySelector('.left-sidebar')?.classList.remove('active');
    activarEfectoCambioPantalla();
    if (destino === 'unidades') cargarTarjetasUnidad();
    if (destino === 'historial') cargarHistorialDefinitivo();
}

function activarEfectoCambioPantalla() {
    const contentArea = document.querySelector('.content-area');
    if (!contentArea) return;
    reproducirSonidoTerminal();
    contentArea.classList.remove('hud-transition');
    void contentArea.offsetWidth;
    contentArea.classList.add('hud-transition');
    setTimeout(() => contentArea.classList.remove('hud-transition'), 500);
}

function reproducirSonidoTerminal() {
    const audio = new Audio('/sounds/750435__rescopicsound__ui-click-menu-modern-interface-select-small-01.mp3');
    audio.volume = 0.3;
    audio.play().catch(() => {});
}

function toggleSidebar() {
    document.querySelector('.left-sidebar')?.classList.toggle('active');
}

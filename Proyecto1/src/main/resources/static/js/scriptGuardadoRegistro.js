async function enviarGuardado() {
    if (!usuarioActual) return mostrarNotificacion('Inicia sesión para guardar resultados.', 'error');
    if (!ultimoResultado) return mostrarNotificacion('Realiza una simulación antes de guardar.', 'error');
    if (!validarCampos(['save-titulo', 'save-notas'])) return;
    const button = document.getElementById('btn-guardar-tirada');
    button.disabled = true;
    try {
        const { real, media } = valoresResultado(ultimoResultado);
        const result = await apiRequest('/historial/guardar', {
            method: 'POST',
            body: {
                titulo: document.getElementById('save-titulo').value.trim(),
                notas: document.getElementById('save-notas').value.trim(),
                aleatorio: real, media
            }
        });
        document.getElementById('panel-guardar-tirada').classList.add('pantalla-oculta');
        document.getElementById('save-titulo').value = '';
        document.getElementById('save-notas').value = '';
        mostrarNotificacion(result.message);
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    } finally {
        button.disabled = false;
    }
}

async function cargarHistorialDefinitivo() {
    if (!usuarioActual) return;
    const owner = usuarioActual.id;
    const list = document.getElementById('historial-completo');
    try {
        const tiradas = await apiRequest('/historial');
        if (usuarioActual?.id !== owner) return;
        list.replaceChildren();
        if (!tiradas.length) {
            const empty = document.createElement('p');
            empty.textContent = 'El registro está vacío. Guarda una tirada del simulador.';
            list.append(empty);
        }
        for (const tirada of tiradas) {
            const card = document.createElement('article');
            card.className = 'historial-card glass-panel';
            const title = document.createElement('h4');
            title.textContent = tirada.titulo;
            const date = document.createElement('small');
            date.textContent = new Date(tirada.fechaCreacion).toLocaleString();
            const notes = document.createElement('p');
            notes.className = 'notas-usuario';
            notes.textContent = tirada.notas || 'Sin notas tácticas adjuntas.';
            const stats = document.createElement('p');
            stats.textContent = 'Dado: ' + tirada.danoAleatorio + ' | Media: ' + tirada.danoMedioEstadistico;
            const button = document.createElement('button');
            button.className = 'btn-secondary btn-danger';
            button.type = 'button';
            button.textContent = 'Eliminar registro';
            button.addEventListener('click', () => borrarTirada(tirada.id));
            card.append(title, date, notes, stats, button);
            list.append(card);
        }
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    }
}

async function borrarTirada(id) {
    if (!confirm('¿Quieres eliminar este registro de batalla?')) return;
    try {
        await apiRequest('/historial/' + id, { method: 'DELETE' });
        await cargarHistorialDefinitivo();
        mostrarNotificacion('Registro eliminado.');
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    }
}

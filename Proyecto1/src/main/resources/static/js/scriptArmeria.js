let idTarjetaEditando = null;
let misTarjetasGlobal = [];

function botonTarjeta(texto, accion, peligro = false) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = peligro ? 'btn-secondary btn-danger' : 'btn-secondary';
    button.textContent = texto;
    button.addEventListener('click', accion);
    return button;
}

async function cargarTarjetasUnidad() {
    if (!usuarioActual) return;
    const owner = usuarioActual.id;
    const list = document.getElementById('lista-tarjetas-unidad');
    try {
        const tarjetas = await apiRequest('/tarjetas');
        if (usuarioActual?.id !== owner) return;
        misTarjetasGlobal = tarjetas;
        list.replaceChildren();
        if (!tarjetas.length) {
            const empty = document.createElement('p');
            empty.textContent = 'No tienes unidades guardadas. Crea una arriba.';
            list.append(empty);
        }
        for (const tarjeta of tarjetas) {
            const card = document.createElement('article');
            card.className = 'tarjeta-unidad glass-panel';
            const title = document.createElement('h4');
            title.textContent = tarjeta.nombre;
            const notes = document.createElement('p');
            notes.className = 'notas-usuario';
            notes.textContent = tarjeta.notas || 'Sin notas tácticas';
            const stats = document.createElement('p');
            stats.textContent = 'Ataques: ' + tarjeta.numAtaques + ' | Impacto: ' + tarjeta.impactoX
                + '+ | Herir: ' + tarjeta.herirX + '+ | Daño: ' + tarjeta.danoPorAtaque;
            const actions = document.createElement('div');
            actions.className = 'acciones-tarjeta';
            actions.append(
                botonTarjeta('Desplegar', () => prepararEquiparUnidad(tarjeta.id)),
                botonTarjeta('Editar', () => prepararEdicion(tarjeta.id)),
                botonTarjeta('Eliminar', () => borrarTarjeta(tarjeta.id), true)
            );
            card.append(title, notes, stats, actions);
            list.append(card);
        }
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    }
}

async function guardarNuevaUnidad() {
    if (!usuarioActual) return mostrarNotificacion('Inicia sesión para guardar unidades.', 'error');
    if (!validarCampos(['nueva-unidad-nombre', 'nueva-unidad-notas', ...Object.values(idsPerfil(true))])) return;
    const button = document.getElementById('btn-guardar-unidad');
    button.disabled = true;
    try {
        const editing = idTarjetaEditando !== null;
        const payload = {
            ...leerPerfil(true),
            nombre: document.getElementById('nueva-unidad-nombre').value.trim(),
            notas: document.getElementById('nueva-unidad-notas').value.trim()
        };
        const result = await apiRequest(editing ? '/tarjetas/' + idTarjetaEditando : '/tarjetas/guardar', {
            method: editing ? 'PUT' : 'POST', body: payload
        });
        resetearFormularioArmeria();
        await cargarTarjetasUnidad();
        mostrarNotificacion(result.message);
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    } finally {
        button.disabled = false;
    }
}

function prepararEquiparUnidad(id) {
    const tarjeta = misTarjetasGlobal.find(t => t.id === id);
    if (tarjeta) equiparUnidad(tarjeta);
}

function equiparUnidad(tarjeta) {
    for (const [name, id] of Object.entries(idsPerfil())) {
        const field = document.getElementById(id);
        if (field.type === 'checkbox') field.checked = Boolean(tarjeta[name]);
        else field.value = tarjeta[name];
    }
    cambiarPantalla('simulador');
    mostrarNotificacion('Unidad "' + tarjeta.nombre + '" lista para el combate.');
}

function prepararEdicion(id) {
    const tarjeta = misTarjetasGlobal.find(t => t.id === id);
    if (!tarjeta) return;
    for (const [name, fieldId] of Object.entries(idsPerfil(true))) {
        const field = document.getElementById(fieldId);
        if (field.type === 'checkbox') field.checked = Boolean(tarjeta[name]);
        else field.value = tarjeta[name];
    }
    document.getElementById('nueva-unidad-nombre').value = tarjeta.nombre;
    document.getElementById('nueva-unidad-notas').value = tarjeta.notas || '';
    idTarjetaEditando = id;
    document.getElementById('btn-guardar-unidad').textContent = 'Actualizar Unidad en Armería';
    document.getElementById('btn-cancelar-edicion').hidden = false;
    document.getElementById('nueva-unidad-nombre').focus();
    document.querySelector('#pantalla-unidades .step-group')?.scrollIntoView({ behavior: 'smooth' });
}

async function borrarTarjeta(id) {
    if (!confirm('¿Quieres eliminar esta unidad?')) return;
    try {
        await apiRequest('/tarjetas/' + id, { method: 'DELETE' });
        if (idTarjetaEditando === id) resetearFormularioArmeria();
        await cargarTarjetasUnidad();
        mostrarNotificacion('Unidad eliminada.');
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    }
}

function resetearFormularioArmeria() {
    idTarjetaEditando = null;
    const defaults = {
        numAtaques: 1, danoPorAtaque: 1, impactoX: 3, herirX: 4, salvacionX: 5, noHayDolorX: 0,
        repeticionImpacto: 'NONE', repeticionHerir: 'NONE', especialSeisImpacto: 'NONE', seisHeridaInsalvable: false
    };
    for (const [name, id] of Object.entries(idsPerfil(true))) {
        const field = document.getElementById(id);
        if (field.type === 'checkbox') field.checked = defaults[name];
        else field.value = defaults[name];
    }
    document.getElementById('nueva-unidad-nombre').value = '';
    document.getElementById('nueva-unidad-notas').value = '';
    document.getElementById('btn-guardar-unidad').textContent = 'Guardar en Armería';
    document.getElementById('btn-cancelar-edicion').hidden = true;
}

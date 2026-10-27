let usuarioActual = null;
let modoRegistro = false;
let versionSesion = 0;

function enviarFormularioAuth(event) {
    event.preventDefault();
    return modoRegistro ? hacerRegistro() : hacerLogin();
}

document.getElementById('auth-form').addEventListener('submit', enviarFormularioAuth);

function mostrarMensajeAuth(mensaje, error = false) {
    const box = document.getElementById('auth-mensaje');
    box.textContent = mensaje;
    box.style.color = error ? '#ff7373' : 'var(--accent)';
}

function toggleModoAuth() {
    modoRegistro = !modoRegistro;
    document.getElementById('campo-email').hidden = !modoRegistro;
    document.getElementById('auth-email').disabled = !modoRegistro;
    document.getElementById('btn-login').hidden = modoRegistro;
    document.getElementById('btn-registro').hidden = !modoRegistro;
    document.getElementById('auth-password').autocomplete = modoRegistro ? 'new-password' : 'current-password';
    document.getElementById('texto-toggle-auth').textContent =
        modoRegistro ? '¿Ya tienes cuenta? Entra aquí' : '¿No tienes cuenta? Regístrate';
    mostrarMensajeAuth('');
}

async function hacerLogin() {
    if (!validarCampos(['auth-username', 'auth-password'])) return;
    const button = document.getElementById('btn-login');
    if (button.disabled) return;
    const version = ++versionSesion;
    button.disabled = true;
    try {
        const user = await apiRequest('/auth/login', {
            method: 'POST',
            body: {
                username: document.getElementById('auth-username').value.trim(),
                password: document.getElementById('auth-password').value
            }
        });
        if (version !== versionSesion) return;
        usuarioActual = user;
        reiniciarCsrf();
        document.getElementById('auth-password').value = '';
        mostrarMensajeAuth('');
        actualizarInterfazAuth();
        mostrarNotificacion('Sesión iniciada.');
    } catch (error) {
        if (version === versionSesion) mostrarMensajeAuth(error.message, true);
    } finally {
        button.disabled = false;
    }
}

async function hacerRegistro() {
    if (!validarCampos(['auth-username', 'auth-password', 'auth-email'])) return;
    const button = document.getElementById('btn-registro');
    if (button.disabled) return;
    button.disabled = true;
    try {
        const result = await apiRequest('/auth/registro', {
            method: 'POST',
            body: {
                username: document.getElementById('auth-username').value.trim(),
                email: document.getElementById('auth-email').value.trim(),
                password: document.getElementById('auth-password').value
            }
        });
        if (modoRegistro) toggleModoAuth();
        mostrarMensajeAuth(result.message);
        document.getElementById('auth-password').value = '';
    } catch (error) {
        mostrarMensajeAuth(error.message, true);
    } finally {
        button.disabled = false;
    }
}

async function cerrarSesion() {
    ++versionSesion;
    try {
        await apiRequest('/auth/logout', { method: 'POST' });
        invalidarSesion();
        reiniciarCsrf();
        mostrarNotificacion('Sesión cerrada.');
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
    }
}

function invalidarSesion() {
    ++versionSesion;
    usuarioActual = null;
    if (typeof misTarjetasGlobal !== 'undefined') misTarjetasGlobal = [];
    ['lista-tarjetas-unidad', 'historial-completo'].forEach(id => document.getElementById(id)?.replaceChildren());
    if (typeof resetearFormularioArmeria === 'function') resetearFormularioArmeria();
    actualizarInterfazAuth();
    if (typeof cambiarPantalla === 'function') cambiarPantalla('simulador');
}

function actualizarInterfazAuth() {
    const logged = Boolean(usuarioActual);
    document.getElementById('auth-panel').classList.toggle('pantalla-oculta', logged);
    document.getElementById('auth-panel').classList.toggle('pantalla-activa', !logged);
    document.getElementById('user-panel').classList.toggle('pantalla-oculta', !logged);
    document.getElementById('user-panel').classList.toggle('pantalla-activa', logged);
    document.getElementById('bienvenida-usuario').textContent = logged ? 'Hola, ' + usuarioActual.username : '';
    if (typeof gestionarPanelGuardado === 'function') gestionarPanelGuardado();
}

document.addEventListener('DOMContentLoaded', async () => {
    const version = versionSesion;
    // Retirar la identidad antigua: solo el servidor decide si hay una sesión válida.
    try {
        ['warmetrics_user', 'usuarioId', 'token'].forEach(key => localStorage.removeItem(key));
    } catch { /* Las cookies de sesión funcionan aunque el almacenamiento esté bloqueado. */ }
    try {
        const user = await apiRequest('/auth/me');
        if (version !== versionSesion) return;
        usuarioActual = user;
    } catch (error) {
        if (version !== versionSesion) return;
        usuarioActual = null;
        if (error.status !== 401) mostrarNotificacion(error.message, 'error');
    }
    actualizarInterfazAuth();
});

function togglePasswordVisibility() {
    const input = document.getElementById('auth-password');
    input.type = input.type === 'password' ? 'text' : 'password';
    document.getElementById('toggle-eye').textContent = input.type === 'password' ? '👁️' : '🙈';
}

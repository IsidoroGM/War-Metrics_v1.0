const API_BASE = '/api';
let csrfToken = null;
let csrfLoading = null;
let notificationTimer;

async function obtenerCsrf() {
    if (csrfToken) return csrfToken;
    if (!csrfLoading) {
        csrfLoading = (async () => {
            const response = await fetch(API_BASE + '/auth/csrf', { credentials: 'same-origin' });
            if (!response.ok) throw new Error('No se pudo preparar la conexión segura. Actualiza la página.');
            csrfToken = await response.json();
            return csrfToken;
        })().finally(() => { csrfLoading = null; });
    }
    return csrfLoading;
}

function reiniciarCsrf() {
    csrfToken = null;
}

function mensajeDeError(data, status) {
    if (data && typeof data === 'object' && typeof data.message === 'string') {
        const detalles = Object.values(data.fields || {}).slice(0, 3);
        return data.message + (detalles.length ? ' ' + detalles.join(' ') : '')
            + (data.reference ? ' Referencia: ' + data.reference : '');
    }
    if (status === 401) return 'Inicia sesión para continuar.';
    if (status === 403) return 'La sesión de seguridad ha caducado. Actualiza la página.';
    if (status === 404) return 'No se encontró el recurso solicitado.';
    if (status === 409) return 'Ya existe un registro con esos datos.';
    if (status >= 500) return 'El servidor no pudo completar la operación. Inténtalo más tarde.';
    return 'No se pudo completar la petición. Revisa los datos introducidos.';
}

async function apiRequest(path, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const mutable = !['GET', 'HEAD', 'OPTIONS'].includes(method);
    const body = options.body === undefined ? undefined : JSON.stringify(options.body);
    const sessionVersion = typeof versionSesion === 'undefined' ? null : versionSesion;
    try {
        for (let attempt = 0; attempt < 2; attempt++) {
            const headers = { ...options.headers };
            if (body !== undefined) headers['Content-Type'] = 'application/json';
            if (mutable) {
                const csrf = await obtenerCsrf();
                headers[csrf.headerName] = csrf.token;
            }
            const response = await fetch(API_BASE + path, {
                ...options, method, headers, credentials: 'same-origin', body
            });
            const text = await response.text();
            let data = null;
            try { data = text ? JSON.parse(text) : null; } catch { /* Una respuesta antigua puede ser texto. */ }
            if (!response.ok) {
                if (response.status === 403 && data?.code === 'CSRF_INVALID') {
                    reiniciarCsrf();
                    // El filtro rechazó la petición antes de ejecutar el controlador.
                    // Se puede renovar y reintentar una vez sin duplicar la operación.
                    if (mutable && attempt === 0) continue;
                }
                if (response.status === 401) {
                    reiniciarCsrf();
                    if (!['/auth/login', '/auth/me'].includes(path) && typeof invalidarSesion === 'function'
                            && (sessionVersion === null || sessionVersion === versionSesion)) {
                        invalidarSesion();
                    }
                }
                const error = new Error(mensajeDeError(data, response.status));
                error.status = response.status;
                throw error;
            }
            return data === null ? text : data;
        }
    } catch (error) {
        if (error instanceof TypeError) {
            throw new Error('No se pudo conectar con el servidor. Comprueba la conexión e inténtalo de nuevo.');
        }
        throw error;
    }
}

function validarCampos(ids) {
    for (const id of ids) {
        const field = document.getElementById(id);
        if (!field || !field.reportValidity()) return false;
    }
    return true;
}

function mostrarNotificacion(mensaje, tipo = 'success') {
    const box = document.getElementById('notificaciones');
    if (!box) return;
    clearTimeout(notificationTimer);
    box.textContent = mensaje;
    box.dataset.tipo = tipo;
    box.hidden = false;
    notificationTimer = setTimeout(() => { box.hidden = true; }, tipo === 'error' ? 10000 : 6000);
}

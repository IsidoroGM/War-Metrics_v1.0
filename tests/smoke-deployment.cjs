const assert = require('node:assert/strict');
const { setTimeout: sleep } = require('node:timers/promises');

const base = new URL(process.argv[2] || 'http://127.0.0.1:8080');
if (!['http:', 'https:'].includes(base.protocol)) throw new Error('La URL debe usar HTTP o HTTPS.');

async function get(route, expected = [200]) {
    const response = await fetch(new URL(route, base), { signal: AbortSignal.timeout(10000) });
    assert.ok(expected.includes(response.status), route + ': HTTP ' + response.status + ', esperado ' + expected.join('/'));
    return response;
}

async function waitForServer() {
    const deadline = Date.now() + 60000;
    while (Date.now() < deadline) {
        try {
            const response = await fetch(new URL('/actuator/health', base), { signal: AbortSignal.timeout(2000) });
            if (response.ok) return;
        } catch { /* La JVM puede estar arrancando. */ }
        await sleep(750);
    }
    throw new Error('El servicio no está preparado después de 60 segundos.');
}

async function main() {
    if (process.argv.includes('--wait')) await waitForServer();
    const home = await (await get('/')).text();
    assert.ok(home.includes('id="auth-form"'), 'La página no contiene el formulario corregido.');
    const health = await (await get('/actuator/health')).json();
    assert.equal(health.status, 'UP');
    assert.ok(!health.components, 'El estado de salud no debe exponer detalles internos.');
    await get('/api/v1/combate/status');
    for (const route of ['/api/auth/me', '/api/tarjetas', '/api/historial']) await get(route, [401]);
    await get('/actuator/env', [401, 403]);
    const csrf = await get('/api/auth/csrf');
    const token = await csrf.json();
    assert.equal(token.headerName, 'X-CSRF-TOKEN');
    assert.ok(typeof token.token === 'string' && token.token.length > 0);
    const cookie = csrf.headers.get('set-cookie') || '';
    assert.match(cookie, /HttpOnly/i);
    assert.match(cookie, /SameSite=Lax/i);
    if (base.protocol === 'https:') assert.match(cookie, /;\s*Secure(?:;|$)/i);
    const security = await (await get('/js/scriptSecurity.js')).text();
    assert.ok(security.includes('CSRF_INVALID'), 'La versión pública no contiene las últimas correcciones.');
    console.log('Correcto: web, salud, API, sesión, CSRF, cookies y diagnósticos protegidos.');
}

main().catch(error => {
    console.error(error.message);
    process.exitCode = 1;
});

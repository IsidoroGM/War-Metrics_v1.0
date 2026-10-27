const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const staticRoot = path.join(__dirname, '../Proyecto1/src/main/resources/static');
const html = fs.readFileSync(path.join(staticRoot, 'index.html'), 'utf8');

class Element {
    constructor(tag = 'div') {
        this.tagName = tag;
        this.children = [];
        this.listeners = {};
        this.style = {};
        this.dataset = {};
        this.value = '';
        this.type = '';
        this.checked = false;
        this.disabled = false;
        this.hidden = false;
        this._text = '';
        const classes = new Set();
        this.classList = {
            add: (...names) => names.forEach(n => classes.add(n)),
            remove: (...names) => names.forEach(n => classes.delete(n)),
            contains: name => classes.has(name),
            toggle(name, value) {
                const active = value === undefined ? !classes.has(name) : value;
                if (active) classes.add(name); else classes.delete(name);
                return active;
            }
        };
    }
    set innerHTML(_) { throw new Error('El renderizado no debe interpretar texto como HTML'); }
    get textContent() { return this._text + this.children.map(x => x.textContent).join(''); }
    set textContent(value) { this._text = String(value); this.replaceChildren(); }
    get lastElementChild() { return this.children.at(-1); }
    append(...children) { for (const child of children) { child.parent = this; this.children.push(child); } }
    prepend(child) { child.parent = this; this.children.unshift(child); }
    replaceChildren(...children) { this.children = []; this.append(...children); }
    remove() { if (this.parent) this.parent.children = this.parent.children.filter(x => x !== this); }
    querySelector(tag) { return this.children.find(x => x.tagName === tag) || null; }
    addEventListener(name, callback) { this.listeners[name] = callback; }
    async click() { await this.listeners.click?.(); }
    focus() { this.focused = true; }
    scrollIntoView() {}
    reportValidity() {
        if (this.required && !String(this.value).trim()) return false;
        if (this.maxLength && String(this.value).length > this.maxLength) return false;
        if (this.type === 'number') {
            const n = Number(this.value);
            return Number.isInteger(n) && n >= this.min && n <= this.max;
        }
        return true;
    }
}

function response(body, status = 200) {
    return {
        ok: status >= 200 && status < 300, status,
        json: async () => body,
        text: async () => body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body)
    };
}

function setup() {
    const elements = new Map();
    for (const match of html.matchAll(/<([a-z]+)([^>]*\bid="([^"]+)"[^>]*)>/g)) {
        const el = new Element(match[1]);
        const attrs = match[2];
        el.id = match[3];
        el.type = attrs.match(/\btype="([^"]+)"/)?.[1] || '';
        el.value = attrs.match(/\bvalue="([^"]*)"/)?.[1] || '';
        el.min = Number(attrs.match(/\bmin="([^"]+)"/)?.[1] ?? -Infinity);
        el.max = Number(attrs.match(/\bmax="([^"]+)"/)?.[1] ?? Infinity);
        el.maxLength = Number(attrs.match(/\bmaxlength="([^"]+)"/)?.[1] ?? 0);
        el.required = /\srequired\b/.test(attrs);
        el.hidden = /\shidden\b/.test(attrs);
        (attrs.match(/\bclass="([^"]+)"/)?.[1] || '').split(/\s+/).filter(Boolean).forEach(x => el.classList.add(x));
        elements.set(el.id, el);
    }
    for (const id of ['repeticionImpacto', 'repeticionHerir', 'especialSeisImpacto',
        'nueva-unidad-rep-impacto', 'nueva-unidad-rep-herir', 'nueva-unidad-esp-impacto']) elements.get(id).value = 'NONE';
    const domEvents = {}, windowEvents = {};
    const sidebar = new Element(), content = new Element(), group = new Element();
    const storage = new Map([['warmetrics_user', '{invalid old JSON'], ['usuarioId', '999']]);
    const ctx = {
        console, localStorage: { removeItem: key => storage.delete(key) },
        document: {
            getElementById: id => elements.get(id) || null,
            createElement: tag => new Element(tag),
            addEventListener: (name, cb) => { domEvents[name] = cb; },
            querySelector: selector => selector === '.left-sidebar' ? sidebar
                : selector === '.content-area' ? content : group,
            querySelectorAll: selector => selector === '.pantalla-seccion'
                ? ['pantalla-simulador', 'pantalla-unidades', 'pantalla-historial'].map(id => elements.get(id)) : []
        },
        window: { addEventListener: (name, cb) => { windowEvents[name] = cb; } },
        fetch: async () => { throw new Error('Petición no prevista'); },
        setTimeout: () => 1, clearTimeout() {}, confirm: () => true,
        Audio: function () { this.play = () => Promise.resolve(); }
    };
    vm.createContext(ctx);
    for (const match of html.matchAll(/<script src="([^"]+)"/g)) {
        vm.runInContext(fs.readFileSync(path.join(staticRoot, match[1]), 'utf8'), ctx, { filename: match[1] });
    }
    const run = code => vm.runInContext(code, ctx);
    return { ctx, elements, storage, run, domEvents, windowEvents };
}

test('las peticiones mutables comparten CSRF y envían la cookie de sesión', async () => {
    const x = setup(), calls = [];
    x.ctx.fetch = async (url, options) => {
        calls.push({ url, options });
        return response(url.endsWith('/csrf') ? { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' } : { ok: true });
    };
    await Promise.all([x.run("apiRequest('/tarjetas/guardar', {method:'POST',body:{nombre:'A'}})"),
        x.run("apiRequest('/historial/guardar', {method:'POST',body:{titulo:'B'}})")]);
    assert.equal(calls.filter(c => c.url.endsWith('/csrf')).length, 1);
    for (const call of calls.filter(c => !c.url.endsWith('/csrf'))) {
        assert.equal(call.options.headers['X-CSRF-TOKEN'], 'csrf-token');
        assert.equal(call.options.credentials, 'same-origin');
        assert.ok(call.options.body.startsWith('{'));
    }
});

test('un error 500 antiguo se convierte en mensaje legible', async () => {
    const x = setup();
    x.elements.get('auth-username').value = 'tester';
    x.elements.get('auth-password').value = 'Password1!';
    x.ctx.fetch = async url => response(url.endsWith('/csrf')
        ? { headerName: 'X-CSRF-TOKEN', token: 'csrf' } : { timestamp: 'now', error: 'Internal Server Error', path: '/api/auth/login' },
        url.endsWith('/csrf') ? 200 : 500);
    await x.ctx.hacerLogin();
    const message = x.elements.get('auth-mensaje').textContent;
    assert.match(message, /servidor no pudo/);
    assert.ok(!message.includes('timestamp') && !message.includes('{'));
    assert.equal(x.elements.get('btn-login').disabled, false);
});

test('los errores de validación y la referencia interna se presentan sin JSON', () => {
    const x = setup();
    const message = x.run("mensajeDeError({message:'Revisa los datos.',fields:{titulo:'Falta el título.'},reference:'ref-123'},400)");
    assert.match(message, /Falta el título/);
    assert.match(message, /ref-123/);
});

test('el registro conserva la confirmación y vuelve a modo login', async () => {
    const x = setup();
    x.ctx.toggleModoAuth();
    x.elements.get('auth-username').value = 'tester';
    x.elements.get('auth-password').value = 'Password1!';
    x.elements.get('auth-email').value = 'tester@example.test';
    x.ctx.apiRequest = async () => ({ message: 'Usuario registrado con éxito.' });
    await x.ctx.hacerRegistro();
    assert.match(x.elements.get('auth-mensaje').textContent, /éxito/);
    assert.equal(x.elements.get('campo-email').hidden, true);
    assert.equal(x.elements.get('btn-login').hidden, false);
    assert.equal(x.elements.get('btn-registro').hidden, true);
});

test('la sesión se restaura consultando al servidor y descarta localStorage antiguo', async () => {
    const x = setup();
    x.ctx.apiRequest = async route => {
        assert.equal(route, '/auth/me');
        return { id: 7, username: 'servidor' };
    };
    await x.domEvents.DOMContentLoaded();
    assert.equal(x.run('usuarioActual.id'), 7);
    assert.equal(x.storage.size, 0);
    assert.equal(x.elements.get('auth-panel').classList.contains('pantalla-oculta'), true);
});

test('tarjetas renderiza texto seguro y ofrece editar y cancelar', async () => {
    const x = setup();
    const marker = '<img src=x onerror=alert(1)>';
    x.run('usuarioActual={id:1}');
    x.ctx.apiRequest = async () => [{ id: 4, nombre: marker, notas: marker, numAtaques: 12,
        danoPorAtaque: 2, impactoX: 3, herirX: 4, salvacionX: 5, noHayDolorX: 0,
        repeticionImpacto: 'NONE', repeticionHerir: 'ONES', especialSeisImpacto: 'NONE' }];
    await x.ctx.cargarTarjetasUnidad();
    const card = x.elements.get('lista-tarjetas-unidad').children[0];
    assert.equal(card.children[0].textContent, marker);
    assert.equal(card.children[1].textContent, marker);
    const edit = card.children.at(-1).children.find(b => b.textContent === 'Editar');
    await edit.click();
    assert.equal(x.run('idTarjetaEditando'), 4);
    assert.equal(x.elements.get('nueva-unidad-nombre').value, marker);
    assert.equal(x.elements.get('btn-cancelar-edicion').hidden, false);
    x.ctx.resetearFormularioArmeria();
    assert.equal(x.run('idTarjetaEditando'), null);
    assert.equal(x.elements.get('btn-cancelar-edicion').hidden, true);
});

test('la edición usa PUT y no envía una identidad del navegador', async () => {
    const x = setup(), calls = [];
    x.run('usuarioActual={id:1}; idTarjetaEditando=9');
    x.elements.get('nueva-unidad-nombre').value = 'Actualizada';
    x.ctx.apiRequest = async (route, options) => {
        calls.push({ route, options });
        return options ? { message: 'Unidad actualizada.' } : [];
    };
    await x.ctx.guardarNuevaUnidad();
    assert.equal(calls[0].route, '/tarjetas/9');
    assert.equal(calls[0].options.method, 'PUT');
    assert.ok(!Object.hasOwn(calls[0].options.body, 'usuarioId'));
    assert.equal(x.run('idTarjetaEditando'), null);
});

test('historial conserva título y notas como texto', async () => {
    const x = setup();
    x.run('usuarioActual={id:1}');
    const marker = '<b>Texto de usuario</b>';
    x.ctx.apiRequest = async () => [{ id: 5, titulo: marker, notas: marker,
        fechaCreacion: '2026-10-27T12:00:00', danoAleatorio: 2, danoMedioEstadistico: 2.22 }];
    await x.ctx.cargarHistorialDefinitivo();
    const card = x.elements.get('historial-completo').children[0];
    assert.equal(card.children[0].textContent, marker);
    assert.equal(card.children[2].textContent, marker);
});

test('simulación valida límites y conserva el FNP cero', async () => {
    const x = setup(), calls = [];
    x.ctx.apiRequest = async (route, options) => {
        calls.push({ route, options });
        return { dañoAleatorio: 3, dañoMedioEstadistico: 2.22 };
    };
    x.elements.get('numAtaques').value = '-10';
    await x.ctx.ejecutarSimulacion();
    assert.equal(calls.length, 0);
    x.elements.get('numAtaques').value = '10';
    await x.ctx.ejecutarSimulacion();
    assert.equal(calls.length, 1);
    assert.equal(calls[0].options.body.noHayDolorX, 0);
    assert.equal(x.elements.get('resultado-estadistico').textContent, '2.22');
    assert.equal(x.elements.get('btn-simular').disabled, false);
});

test('un 401 limpia los datos privados y vuelve al acceso', async () => {
    const x = setup();
    x.run('usuarioActual={id:1}; misTarjetasGlobal=[{id:1}]');
    x.elements.get('lista-tarjetas-unidad').append(new Element('article'));
    x.ctx.fetch = async () => response({ message: 'La sesión caducó.' }, 401);
    await x.ctx.cargarTarjetasUnidad();
    assert.equal(x.run('usuarioActual'), null);
    assert.equal(x.run('misTarjetasGlobal.length'), 0);
    assert.equal(x.elements.get('lista-tarjetas-unidad').children.length, 0);
    assert.equal(x.elements.get('auth-panel').classList.contains('pantalla-activa'), true);
    assert.equal(x.elements.get('notificaciones').hidden, false);
});

test('un CSRF caducado se renueva antes de reintentar una sola vez', async () => {
    const x = setup();
    let tokens = 0, attempts = 0;
    x.ctx.fetch = async (url, options) => {
        if (url.endsWith('/csrf')) return response({ headerName: 'X-CSRF-TOKEN', token: 'token-' + ++tokens });
        attempts++;
        if (attempts === 1) return response({ code: 'CSRF_INVALID', message: 'CSRF caducado.' }, 403);
        assert.equal(options.headers['X-CSRF-TOKEN'], 'token-2');
        return response({ message: 'Guardado.' }, 201);
    };
    const result = await x.run("apiRequest('/tarjetas/guardar', {method:'POST',body:{nombre:'Unidad'}})");
    assert.equal(result.message, 'Guardado.');
    assert.equal(tokens, 2);
    assert.equal(attempts, 2);
});

test('un 403 de permisos no se reintenta', async () => {
    const x = setup();
    let attempts = 0;
    x.ctx.fetch = async url => {
        if (url.endsWith('/csrf')) return response({ headerName: 'X-CSRF-TOKEN', token: 'token' });
        attempts++;
        return response({ code: 'FORBIDDEN', message: 'No autorizado.' }, 403);
    };
    await assert.rejects(x.run("apiRequest('/tarjetas/1', {method:'DELETE'})"), error => error.status === 403);
    assert.equal(attempts, 1);
});

test('la recuperación de CSRF no entra en un bucle', async () => {
    const x = setup();
    let attempts = 0;
    x.ctx.fetch = async url => {
        if (url.endsWith('/csrf')) return response({ headerName: 'X-CSRF-TOKEN', token: 'token' });
        attempts++;
        return response({ code: 'CSRF_INVALID', message: 'CSRF caducado.' }, 403);
    };
    await assert.rejects(x.run("apiRequest('/historial/1', {method:'DELETE'})"), error => error.status === 403);
    assert.equal(attempts, 2);
});

test('la primera operación tras caducar la sesión limpia la identidad anterior', async () => {
    const x = setup();
    x.run('usuarioActual={id:1}; misTarjetasGlobal=[{id:1}]');
    let attempts = 0;
    x.ctx.fetch = async url => {
        if (url.endsWith('/csrf')) return response({ headerName: 'X-CSRF-TOKEN', token: 'token' });
        return ++attempts === 1 ? response({ code: 'CSRF_INVALID', message: 'CSRF caducado.' }, 403)
            : response({ message: 'Inicia sesión.' }, 401);
    };
    await assert.rejects(x.run("apiRequest('/tarjetas/guardar', {method:'POST',body:{nombre:'Unidad'}})"),
        error => error.status === 401);
    assert.equal(x.run('usuarioActual'), null);
    assert.equal(x.run('misTarjetasGlobal.length'), 0);
});

test('bloquear localStorage no impide recuperar la sesión del servidor', async () => {
    const x = setup();
    x.ctx.localStorage = { removeItem() { throw new Error('Almacenamiento bloqueado'); } };
    x.ctx.apiRequest = async () => ({ id: 7, username: 'sesion-valida' });
    await x.domEvents.DOMContentLoaded();
    assert.equal(x.run('usuarioActual.id'), 7);
});

test('una respuesta antigua de restauración no borra un login nuevo', async () => {
    const x = setup();
    let rejectRestore;
    x.ctx.apiRequest = route => route === '/auth/me'
        ? new Promise((resolve, reject) => { rejectRestore = reject; })
        : Promise.resolve({ id: 7, username: 'login-nuevo' });
    const pending = x.domEvents.DOMContentLoaded();
    x.elements.get('auth-username').value = 'login-nuevo';
    x.elements.get('auth-password').value = 'Password1!';
    await x.ctx.hacerLogin();
    rejectRestore(Object.assign(new Error('Petición anterior sin sesión'), { status: 401 }));
    await pending;
    assert.equal(x.run('usuarioActual.id'), 7);
});

test('un 401 pendiente de la cuenta anterior no desconecta un login nuevo', async () => {
    const x = setup();
    x.run('usuarioActual={id:1}');
    let finishOldRequest;
    x.ctx.fetch = () => new Promise(resolve => { finishOldRequest = resolve; });
    const oldRequest = x.run("apiRequest('/tarjetas')");
    x.run('versionSesion++; usuarioActual={id:2}');
    finishOldRequest(response({ message: 'Petición anterior sin sesión.' }, 401));
    await assert.rejects(oldRequest, error => error.status === 401);
    assert.equal(x.run('usuarioActual.id'), 2);
});

test('el formulario envía login o registro y desactiva el correo oculto', async () => {
    const x = setup(), calls = [];
    const form = x.elements.get('auth-form');
    assert.ok(form, 'El acceso debe ser un formulario con envío nativo mediante Enter');
    x.ctx.hacerLogin = async () => calls.push('login');
    x.ctx.hacerRegistro = async () => calls.push('registro');
    let prevented = 0;
    await form.listeners.submit({ preventDefault() { prevented++; } });
    x.ctx.toggleModoAuth();
    assert.equal(x.elements.get('auth-email').disabled, false);
    await form.listeners.submit({ preventDefault() { prevented++; } });
    x.ctx.toggleModoAuth();
    assert.equal(x.elements.get('auth-email').disabled, true);
    assert.deepEqual(calls, ['login', 'registro']);
    assert.equal(prevented, 2);
});

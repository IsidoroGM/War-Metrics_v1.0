# WarMetrics — Simulador táctico de combate

Aplicación web para simular tiradas y calcular medias de daño, con armería e historial por usuario.

**Tecnologías:** Java 21, Spring Boot 4.0.2, Maven, Spring Security, Spring Data JPA, H2 y JavaScript sin framework.

## Inicio local

Requisitos: JDK 21 o superior. El wrapper incluido permite trabajar sin instalar Maven.

```powershell
git clone https://github.com/IsidoroGM/War-Metrics_v1.0.git
cd War-Metrics_v1.0/Proyecto1
.\mvnw.cmd spring-boot:run
```

En Linux/macOS, desde `Proyecto1`:

```sh
sh mvnw spring-boot:run
```

Abrir [http://localhost:8080](http://localhost:8080). Spring Boot sirve tanto la página como la API. Los archivos del frontend están en `Proyecto1/src/main/resources/static`.

Para editar la web, arrancar el backend y recargar esa dirección. Live Server no incorpora un proxy hacia la API y no forma parte de la configuración soportada.

## Datos persistentes

Por defecto se utiliza H2 en archivo, en `runtime-data/warmetricsdb`, relativo al directorio desde el que se arranca Java. Arrancar siempre desde el mismo directorio para usar la misma base. Un reinicio conserva usuarios, tarjetas e historial; será necesario iniciar sesión de nuevo.

La carpeta `runtime-data/` está excluida de Git. Las bases antiguas de `data/` y `Proyecto1/data/` no se abren ni modifican automáticamente.

| Variable | Valor por defecto | Uso |
|---|---|---|
| PORT | 8080 | Puerto HTTP |
| DB_URL | jdbc:h2:file:./runtime-data/warmetricsdb;DB_CLOSE_ON_EXIT=FALSE | Ubicación de H2 |
| DB_USERNAME | sa | Usuario de H2 |
| DB_PASSWORD | vacío | Contraseña de H2 |
| SESSION_COOKIE_SECURE | false | Usar true cuando la web se sirve mediante HTTPS |

Los tests usan una base independiente en memoria. La prueba de reinicio utiliza archivos temporales.

## Cuentas y seguridad

- El registro exige usuario de 3 a 20 caracteres (letras sin acentos, números, guion o guion bajo), correo válido y contraseña de 8 a 72 caracteres con mayúscula, número y uno de estos símbolos: `@$!%*?&`.
- Las contraseñas se almacenan mediante BCrypt.
- El servidor mantiene la sesión; la cookie de sesión es HttpOnly y SameSite=Lax. La sesión caduca tras 30 minutos sin actividad.
- Las operaciones que modifican datos requieren CSRF, incluido el simulador público. El cliente obtiene el token en `GET /api/auth/csrf` y lo envía en la cabecera indicada por la respuesta.
- Si caduca el token, el cliente lo renueva y reintenta una sola vez cuando recibe el código `CSRF_INVALID`. Las denegaciones de permisos no se reintentan.
- Tarjetas e historial se asocian al usuario de la sesión. No se confía en una identidad almacenada en localStorage.
- El único diagnóstico público es `GET /actuator/health`, sin detalles internos.
- En despliegues HTTPS, configurar `SESSION_COOKIE_SECURE=true` y un proxy de confianza que transmita el protocolo original.

## Funciones y límites

El motor resuelve impacto, herida, salvación y No Hay Dolor. Permite repetir unos o fallos, impacto extra o herida automática en seis y heridas devastadoras. La media utiliza recuentos exactos y redondeo decimal a dos cifras.

Se admiten entre 1 y 10.000 ataques y entre 1 y 100 puntos de daño por ataque. Los umbrales de impacto y herida van de 1 a 6; la salvación, de 2 a 7 (7 significa sin salvación); FNP, de 0 a 7 (0 y 7 significan sin FNP). Estos campos requieren valores enteros; la API rechaza los decimales en lugar de truncarlos.

La armería permite crear, editar, cancelar la edición, desplegar y eliminar tarjetas. Nombres y títulos admiten hasta 120 caracteres; las notas, hasta 500. El contenido se muestra como texto, sin interpretarlo como HTML.

## API

| Método | Ruta | Acceso |
|---|---|---|
| GET | /api/auth/csrf | Público |
| POST | /api/auth/registro | Público + CSRF |
| POST | /api/auth/login | Público + CSRF |
| GET | /api/auth/me | Sesión |
| POST | /api/auth/logout | Sesión + CSRF |
| GET | /api/v1/combate/status | Público |
| POST | /api/v1/combate/simular | Público + CSRF |
| GET | /api/tarjetas | Tarjetas propias |
| POST | /api/tarjetas/guardar | Sesión + CSRF |
| PUT / DELETE | /api/tarjetas/{id} | Propietario + CSRF |
| GET | /api/historial | Historial propio |
| POST | /api/historial/guardar | Sesión + CSRF |
| DELETE | /api/historial/{id} | Propietario + CSRF |

Las rutas anteriores `/api/tarjetas/usuario/{id}` y `/api/historial/usuario/{id}` se mantienen, pero solo permiten el ID de la sesión.

Los errores usan JSON con `status`, `message`, `fields`, `reference` y un `code` opcional. Las entradas inválidas devuelven 400; una sesión ausente, 401; una operación no autorizada, 403; un recurso inexistente o ajeno, 404; un registro duplicado, 409. Los errores internos devuelven 500 con una referencia que permite localizar la excepción en el registro del servidor.

## Pruebas y construcción

Desde la raíz, con Maven instalado:

```sh
mvn -B -ntp -f Proyecto1/pom.xml verify
node --test tests/frontend.test.cjs
```

O usar el wrapper desde `Proyecto1`: `sh mvnw verify` en Linux/macOS o `.\mvnw.cmd verify` en Windows.

La suite comprueba cálculo, validación, sesión, CSRF, aislamiento entre cuentas, errores, persistencia tras reinicio y comportamiento del frontend. GitHub Actions ejecuta las comprobaciones con Java 21 y Node 22, además de construir y arrancar el contenedor.

## Docker

```sh
docker build -t warmetrics .
docker run --rm -p 8080:8080 -v warmetrics-data:/app/runtime-data warmetrics
```

La construcción ejecuta las pruebas Maven. El volumen conserva los datos al recrear el contenedor. El ejemplo sirve HTTP local; detrás de HTTPS, añadir `-e SESSION_COOKIE_SECURE=true`.

## Plan de correcciones

El estado y la verificación de los problemas detectados se documentan en [PLAN_SOLUCIONES.md](PLAN_SOLUCIONES.md).

La preparación y los requisitos del servicio actual están en [DESPLIEGUE_RENDER.md](DESPLIEGUE_RENDER.md).

Autor: Isidoro Gordillo.

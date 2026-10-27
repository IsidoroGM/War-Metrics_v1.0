# Despliegue de WarMetrics en Render

Servicio público: https://war-metrics.onrender.com/

## Estado comprobado

La URL pública responde y el motor devuelve HTTP 200. El JavaScript público todavía contiene la implementación anterior y `/actuator/health` devuelve 404. Las correcciones están publicadas en la [PR #2](https://github.com/IsidoroGM/War-Metrics_v1.0/pull/2), pero aún no se han desplegado.

Las 16 pruebas de backend y 18 de frontend pasan localmente. GitHub Actions ha completado correctamente la validación con Java 21 y la construcción y comprobación de la imagen Docker. Antes de desplegar se debe verificar el resultado del último commit de la PR.

El panel de Render confirma: servicio War-Metrics, plan Free, runtime Docker, rama Despliegue-en-Render y último commit desplegado b6d9182. No hay ruta de salud HTTP configurada.

Los logs del 27 de octubre de 2026 a las 12:25:39 UTC muestran que el 500 original fue un conflicto de unicidad del correo (SQLState 23505). La nueva versión responde 409 con un mensaje legible, con regresiones para correo repetido y excepción de integridad.

## Configuración del servicio existente

| Ajuste | Valor para esta versión |
|---|---|
| Repositorio | IsidoroGM/War-Metrics_v1.0 |
| Rama | Actualmente Despliegue-en-Render; la PR apunta a main. Alinear la rama de Render con la versión integrada antes de desplegar |
| Runtime | Docker |
| Root Directory / Build Context | Raíz del repositorio |
| Dockerfile | ./Dockerfile |
| Health Check Path | /actuator/health |
| SPRING_PROFILES_ACTIVE | render |
| DB_URL | jdbc:postgresql://HOST_NEON/neondb?sslmode=require&channelBinding=require |
| DB_USERNAME | Usuario de la base de Neon |
| DB_PASSWORD | Contraseña de la base de Neon, como variable secreta |
| Disco persistente | No se necesita: los datos residen en PostgreSQL externo |
| PORT | Usar la variable que proporciona Render |

El usuario ha elegido mantener Render Free y utilizar una base externa nueva. Solo hay datos de prueba y no se requiere migrarlos. Se preparará PostgreSQL en el plan Free de Neon, manteniendo el servicio de Render en Free.

El perfil render exige URL, usuario y contraseña y selecciona el controlador PostgreSQL. Activa TLS y channel binding, limita el pool a tres conexiones y configura la cookie HTTPS. Si faltan las credenciales, el arranque falla en lugar de utilizar H2 local.

La URL JDBC no debe contener usuario ni contraseña; se configuran por separado en Render. La conexión se obtiene del proyecto de Neon, opción Java. El ejemplo de .env.example contiene solo marcadores. El perfil local mantiene H2 en archivo para desarrollo.

El almacenamiento del plan Free de Neon persiste separado del contenedor y está sujeto a sus límites de uso; no se contratará un plan de pago. [Planes de Neon](https://neon.com/docs/introduction/plans), [conexión Java/JDBC](https://neon.com/docs/guides/java).

## Secuencia de publicación

1. Revisar y aprobar los cambios de la rama de correcciones.
2. Confirmar que los trabajos de Java 21, frontend y contenedor de GitHub Actions pasan.
3. Crear el proyecto gratuito de Neon y comprobar su conexión PostgreSQL.
4. Configurar en Render las cuatro variables indicadas, sin contratar disco ni cambiar el plan Free. Los datos de prueba anteriores no se migrarán.
5. Alinear la rama vinculada de Render con la versión revisada, configurar /actuator/health y desplegar el commit verificado.
6. Comprobar la URL, registro, login, tarjetas, historial y persistencia tras un reinicio controlado.

Render puede desplegar automáticamente al cambiar la rama vinculada. La integración de la rama de correcciones debe hacerse después de verificar la configuración y los datos. [Funcionamiento de los despliegues de Render](https://render.com/docs/deploys).

## Comprobación pública después del despliegue

Desde la raíz:

```sh
node tests/smoke-deployment.cjs https://war-metrics.onrender.com/ --wait
```

La comprobación solo hace peticiones GET; no registra cuentas ni modifica tarjetas o historial. Comprueba la versión de la página, salud, acceso anónimo denegado, token CSRF y atributos seguros de la cookie HTTPS.

El estado saludable debe devolver HTTP 200 en `/actuator/health`. [Comprobaciones de salud de Render](https://render.com/docs/health-checks).

## Pendiente de conexión

Se ha accedido al panel de Render mediante Chrome y revisado configuración y logs. Falta acceso a la cuenta de Neon para provisionar la base y obtener sus credenciales; puede hacerse mediante la integración de Neon instalada y conectada o mediante una sesión del usuario en Neon. Hasta entonces no se sustituye el despliegue actual.

## Validación PostgreSQL

GitHub Actions dispone de un servicio PostgreSQL 16 desechable para repetir las pruebas de API y el reinicio de la aplicación. Estas pruebas solo se habilitan con una URL local de la base warmetrics_test; no recrean el esquema de una base externa.


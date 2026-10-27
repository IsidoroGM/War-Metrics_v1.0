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
| SESSION_COOKIE_SECURE | true |
| DB_URL | jdbc:h2:file:/app/runtime-data/warmetricsdb;DB_CLOSE_ON_EXIT=FALSE |
| Disco persistente | Montado en /app/runtime-data |
| PORT | Usar la variable que proporciona Render |

Hay que aplicar los ajustes pendientes en Render tras resolver el almacenamiento. El Dockerfile crea el directorio y declara VOLUME, pero esa declaración no configura por sí sola un disco persistente en Render.

Render utiliza almacenamiento efímero por defecto: sin un disco persistente, los archivos se pierden al reiniciar o desplegar. Los discos persistentes requieren un servicio de pago. Si el servicio es gratuito, hay que resolver el almacenamiento antes de publicar esta versión con H2; una base externa requerirá configurar su controlador y conexión. [Documentación de discos de Render](https://render.com/docs/disks).

## Secuencia de publicación

1. Revisar y aprobar los cambios de la rama de correcciones.
2. Confirmar que los trabajos de Java 21, frontend y contenedor de GitHub Actions pasan.
3. Revisar el servicio existente en Render: rama, almacenamiento, variables y logs del error de registro original.
4. Conservar una copia de los datos existentes que deban mantenerse antes de reiniciar el servicio. Las bases antiguas versionadas no se importan automáticamente.
5. Aplicar la configuración indicada y desplegar el commit verificado.
6. Comprobar la URL, registro, login, tarjetas, historial y persistencia tras un reinicio controlado.

Render puede desplegar automáticamente al cambiar la rama vinculada. La integración de la rama de correcciones debe hacerse después de verificar la configuración y los datos. [Funcionamiento de los despliegues de Render](https://render.com/docs/deploys).

## Comprobación pública después del despliegue

Desde la raíz:

```sh
node tests/smoke-deployment.cjs https://war-metrics.onrender.com/ --wait
```

La comprobación solo hace peticiones GET; no registra cuentas ni modifica tarjetas o historial. Comprueba la versión de la página, salud, acceso anónimo denegado, token CSRF y atributos seguros de la cookie HTTPS.

El estado saludable debe devolver HTTP 200 en `/actuator/health`. [Comprobaciones de salud de Render](https://render.com/docs/health-checks).

## Decisión pendiente

Se ha accedido al panel mediante la sesión existente de Chrome y se han revisado configuración y logs. El servicio sigue en Free y se espera la decisión del usuario sobre base externa o plan con disco. No se ha modificado la configuración de producción ni contratado ningún recurso.


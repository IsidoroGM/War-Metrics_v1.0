# Despliegue de WarMetrics en Render

Servicio público: https://war-metrics.onrender.com/

## Estado comprobado

La URL pública responde y el motor devuelve HTTP 200. El JavaScript público todavía contiene la implementación anterior y `/actuator/health` devuelve 404. Las correcciones locales aún no están publicadas.

Las 15 pruebas de backend y 18 de frontend pasan localmente. GitHub Actions está preparado para comprobarlas con Java 21 y construir y arrancar la imagen Docker. Su resultado debe revisarse antes de desplegar.

## Configuración del servicio existente

| Ajuste | Valor para esta versión |
|---|---|
| Repositorio | IsidoroGM/War-Metrics_v1.0 |
| Rama | Confirmar la rama vinculada en el servicio; integrar la versión revisada antes de desplegar |
| Runtime | Docker |
| Root Directory / Build Context | Raíz del repositorio |
| Dockerfile | ./Dockerfile |
| Health Check Path | /actuator/health |
| SESSION_COOKIE_SECURE | true |
| DB_URL | jdbc:h2:file:/app/runtime-data/warmetricsdb;DB_CLOSE_ON_EXIT=FALSE |
| Disco persistente | Montado en /app/runtime-data |
| PORT | Usar la variable que proporciona Render |

Hay que confirmar estos valores en Render, especialmente la rama y el disco. El Dockerfile crea el directorio y declara VOLUME, pero esa declaración no configura por sí sola un disco persistente en Render.

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

## Pendiente de acceso

La integración de Render debe estar instalada y conectada para poder confirmar el plan, el disco, las variables, los logs y los controles de despliegue del servicio. No se ha modificado la configuración de producción.


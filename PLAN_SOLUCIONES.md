# Plan de soluciones de WarMetrics

Estado inicial: revisión del commit b6d9182. Correcciones implementadas sobre la copia local.

| Fase | Trabajo | Criterio de finalización | Estado |
|---|---|---|---|
| 1 | Sesiones de servidor, propiedad de registros, CSRF y protección de Actuator | Acceso anónimo rechazado; una cuenta no puede consultar ni modificar datos de otra | Aplicado y probado |
| 2 | H2 persistente y configuración de pruebas independiente | Un reinicio conserva usuarios, tarjetas e historial | Aplicado y probado |
| 3 | Probabilidades, validación y respuestas de error | Casos de regresión correctos; entradas inválidas devuelven 400 y duplicados 409 | Aplicado y probado |
| 4 | Renderizado seguro, edición de tarjetas y mensajes | Sin interpolar texto de usuario como HTML; edición accesible; mensajes visibles | Aplicado y probado |
| 5 | Instalación, Docker y comprobaciones finales | Compilación y pruebas correctas; instrucciones reproducibles | Aplicado; Docker pendiente de ejecutar |

## Decisiones

- Autenticación mediante sesión del servidor y cookie HttpOnly; la identidad no se tomará de localStorage ni de un ID enviado por el navegador.
- CSRF activo para las operaciones que modifican datos, incluidos registro, login y logout.
- El simulador seguirá disponible sin iniciar sesión; guardar tarjetas e historial requerirá una sesión válida.
- Desarrollo desde el propio servidor Spring Boot. El frontend y la API compartirán origen.
- Persistencia por defecto en una carpeta nueva `runtime-data/`, para no abrir ni modificar las bases antiguas versionadas. En Docker se montará esa carpeta como volumen.
- Las pruebas usarán H2 en memoria y no tocarán datos reales.
- Los errores internos devolverán un mensaje comprensible y una referencia; la excepción completa permanecerá en el registro del servidor.

## Verificación prevista

1. Pruebas deterministas de probabilidades con todas las caras del dado y reglas especiales.
2. Pruebas HTTP de sesión, CSRF, aislamiento de cuentas, validaciones y errores.
3. Comprobación de persistencia con reinicio y datos ficticios.
4. Pruebas del frontend para renderizado seguro, edición, sesión y mensajes.
5. Compilación Maven y revisión del diff. Docker se comprobará si el entorno dispone de su motor.

El error exacto de la captura requiere la excepción del backend desplegado. Se ha corregido el tratamiento de errores y añadido una referencia para localizarla.

## Resultado de la implementación

Los 11 problemas del análisis tienen una corrección local:

| Problema | Solución |
|---|---|
| API sin protección | Sesión HttpOnly, rotación al autenticar, CSRF y consultas limitadas al propietario |
| XSS almacenado | Elementos DOM y textContent para nombres, títulos y notas |
| Pérdida de datos al reiniciar | H2 en archivo, carpeta runtime-data y volumen Docker |
| Actuator público | Solo health público y sin detalles internos; otros diagnósticos denegados |
| Medias incorrectas | Recuentos exactos de las parejas de dados y redondeo decimal HALF_UP |
| Entradas inválidas | Rangos, límites, longitudes y validación activa en los controladores |
| Edición inaccesible | Botón Editar y acción Cancelar edición |
| Códigos HTTP erróneos | Respuestas uniformes 400/401/403/404/409 y referencia para fallos 500 |
| Live Server desconectado | Desarrollo desde Spring Boot y frontend/API del mismo origen |
| Guía y Java inconsistentes | Java 21 en POM, Docker, configuración y guía; dependencias sin uso retiradas |
| Mensaje de registro borrado | Cambio de modo antes de mostrar la confirmación |

### Verificación realizada

- Maven verify: BUILD SUCCESS. 15 pruebas de backend, sin fallos ni errores.
- Una de las pruebas recorre 11.664 combinaciones de umbrales, repeticiones, reglas y salvaciones.
- Persistencia: dos arranques independientes sobre una base temporal conservan usuario, contraseña BCrypt, tarjeta e historial.
- Node: 18 pruebas de frontend correctas, sin paquetes externos.
- Navegador integrado: registro, login, creación y edición de tarjeta, despliegue, simulación con media 33,33, guardado en historial, recuperación de sesión tras recarga y logout comprobados.
- Etiquetas HTML de prueba visibles como texto literal en la interfaz.
- Dockerfile actualizado para ejecutar las pruebas Maven; workflow de GitHub Actions añadido para Java 21 y Node 22.

La verificación local se ejecutó con JDK 25.0.2 compilando para Java 21. El motor Docker no está disponible; la imagen y el workflow de GitHub quedan pendientes de ejecución en sus respectivos entornos.

Los cambios no se han publicado ni desplegado. Las bases antiguas versionadas no se han modificado. El error 500 original del servidor desplegado no puede atribuirse a una excepción concreta sin su log; ahora el cliente evita mostrar JSON crudo y los nuevos errores internos generan una referencia de diagnóstico.

El diseño de sesión y CSRF se contrastó con la documentación de [sesiones de Spring Security](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/session-management.html) y [protección CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).

## Segunda revisión de flujos

Se reprodujeron y corrigieron estos casos adicionales:

- Un token CSRF caducado impedía la primera operación y podía dejar una identidad antigua visible. El servidor diferencia `CSRF_INVALID` de una denegación de permisos; el cliente renueva el token y reintenta una sola vez únicamente cuando el filtro rechazó la petición antes del controlador. Si ya no hay sesión, limpia la identidad y pide iniciar sesión.
- Una respuesta pendiente de restauración o de una cuenta anterior podía borrar un login posterior. Las respuestas se contrastan con la versión de sesión antes de modificar la identidad.
- Un navegador con localStorage bloqueado impedía recuperar una sesión válida. La limpieza de valores antiguos ya no bloquea la autenticación mediante cookies.
- El JSON aceptaba decimales en campos enteros y los truncaba silenciosamente. Se ha desactivado esa conversión: cantidades y umbrales decimales devuelven HTTP 400.
- Pulsar Enter en acceso o registro no enviaba el formulario. Ahora utiliza un formulario nativo y el correo oculto queda desactivado durante el login.

Verificación: 15 pruebas de backend y 18 de frontend correctas. Los nuevos casos fallaron antes de aplicar sus correcciones. Registro e inicio de sesión mediante Enter comprobados también en navegador con una cuenta ficticia y base en memoria. El servidor temporal se cierra al terminar la comprobación.

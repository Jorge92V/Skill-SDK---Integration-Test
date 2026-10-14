# netpay-sdk — skill v0.6

Archivo para humanos (mantenedores). No forma parte de las instrucciones que carga Claude.

## Estado
v0.6: incorpora `references/lecciones-campo.md` (integración real, firmas verificadas en el AAR).
v0.5: references/ integra la doc pública de NetPay (SDK 1.1.9, Smart PinPad 2.0+) con etiquetas
[doc] / [skill] / VALIDAR, y `assets/ejemplo-kotlin/` trae adapter + folio + estados + fake (SIN COMPILAR).
Siguen en VALIDAR: catálogo de códigos de respuesta, cashback, devolución,
cierre de lote, selección de ambiente en el SDK, changelog. La tabla de reintento por estatus es política
del skill pendiente de visto bueno de NetPay.

## Pendientes por construir (referenciados en SKILL.md, hoy NO existen)
| Ruta | Para qué | Estado |
|---|---|---|
| `assets/ejemplo-kotlin/` | Ejemplo Kotlin: adapter propio + fake para pruebas sin terminal | Escrito, sin compilar |
| `assets/fixtures/` | Respuestas del SDK anonimizadas, con versión y TTL; no autoritativas | Por construir (requiere `scrub.py`) |
| `scripts/validar_integracion.py` | Chequeo estático offline del proyecto del integrador (logs sensibles, folio no persistido/reutilizado, reverso propio, falta de consulta por folio, doble toque, package name fuera de flavor, secretos) | Por construir |
| `scripts/scrub.py` | Anonimizar respuestas reales antes de convertirlas en fixtures (uso interno NetPay) | Por construir |

SKILL.md condiciona su uso a "si existe en el paquete"; el modelo no debe describir su contenido mientras no existan.

## Pendientes de datos (dueño: NetPay)
- `references/sandbox.md`: logística de app dev (no hay catálogo de tarjetas/montos: decisión del dueño, las pruebas se hacen una a una).
- `references/transacciones.md`: firmas, catálogo de estatus de reimpresión por folio con columna "Habilita reintento", timeouts, ciclo de vida, esquema de respuesta, ventanas.
- `references/dispositivos.md`: matriz por app/modelo/versión.
- `references/glosario.md`: enum de MSI y plazos; otros tipos de promoción si existen.
- `references/errores.md`, `changelog.md`, `quickstart.md`, `seguridad.md` §Logging.
- `references/casos-de-prueba.md`: guion de entorno T05/T08/T10/T11.

## Evals
`evals/evals.json`. Solo se corren los casos `listo`. Al llenar un VALIDAR, revisar los casos `pendiente_*` relacionados y los `listo` que hoy esperan "no confirmado".

## Defectos de la documentación fuente (para el dueño de la doc de NetPay)
- (campo) Falta documentar `<queries>`/visibilidad de paquetes para apps con `targetSdk >= 30`; sin eso `doTrans`
  lanza `IllegalArgumentException: Unknown URL content://...`.
- (campo) La doc nombra `responseCode`; el SDK 1.1.9 expone `code`. Firmas reales de `SaleRequest`,
  `CancelRequest` y `ReprintRequest` (con `storeId`) no coinciden con la doc.
- (campo) Sin catálogo de excepciones del SDK (incluye `IllegalArgumentException` por provider no resuelto).
- (campo) Requisitos de build no documentados: Jetifier, reglas de minify, jitpack con `FAIL_ON_PROJECT_REPOS`.
- Orden de parámetros de `SaleRequest` distinto entre ejemplos Kotlin y Java.
- Nombre del campo de folio: `folio` / `folioId` / `folioNumber`.
- Callback `onActivityResult` vs `onActivityCallResult`.
- JSON de ejemplo con `autCode` y `meessage` (¿nombres reales o typos?).
- `amount` con o sin propina (se contradice desde 1.1.8); `transType` `PRE` sin documentar.
- Versión de Smart PinPad DEV (1.3.7 / 1.3.10.2+) vs soporte v1.x terminado y v2.1.3+ requerido; SDK 1.1.9 vs 1.1.5.
- Sin catálogo público de `responseCode` ni de errores de `SmartApiException`.
- Matriz de certificación del SDK no está en la doc pública; las reglas de sandbox salen de la matriz de API.
- Cuánto tarda el reverso y qué ve la app; comportamiento en segundo plano.

## Decisiones pendientes del dueño
- S3: el skill solo permite tarjetas de prueba (la doc de NetPay permite propias con cuidado del NIP).
- Visto bueno de NetPay a la política de reintento y a la lista de campos permitidos en logs.
- Confirmar que el SDK no tiene selección explícita de ambiente (lo marca la Smart PinPad).

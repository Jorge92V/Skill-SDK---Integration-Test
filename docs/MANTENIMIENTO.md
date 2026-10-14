# Mantenimiento del skill netpay-sdk

Archivo para mantenedores. No forma parte de las instrucciones del skill.

## Estado del contenido
- v0.6: `references/lecciones-campo.md` (integración real: CoreX, Kozen, Android 15; firmas verificadas en el AAR 1.1.9).
- v0.5: `references/` integra la doc pública de NetPay (SDK 1.1.9, Smart PinPad 2.0+) con etiquetas [doc] / [skill] / VALIDAR.
- Estructura portable (multiagente): `SKILL.md` con solo `name` y `description`, detalle en `references/`, código en `examples/kotlin/`.
- Lo que falta confirmar está en [OPEN_QUESTIONS.md](../OPEN_QUESTIONS.md).

## Reglas de mantenimiento
- `SKILL.md` < 500 líneas, frontmatter solo con `name` y `description`, sin nombres de herramientas de un agente.
- Links relativos desde `SKILL.md`, un nivel de profundidad.
- Antes de subir: `python3 tools/validate_skill.py` (también corre en CI).
- Al cambiar reglas: correr `evals/prompts.json` en al menos una plataforma y `evals/regresion-v0.6.json` si aplica.
- Nunca agregar credenciales, seriales, authorities, packages de apps NetPay ni datos de comercio.

## Ideas no construidas (no referenciadas por el skill)
| Ruta propuesta | Para qué |
|---|---|
| `netpay-sdk/scripts/validar_integracion.py` | Chequeo estático offline del proyecto del integrador (logs sensibles, folio no persistido, reverso propio, falta de consulta por folio, doble toque, secretos). Solo stdlib; el skill debe seguir funcionando sin él (checklist de certificacion.md). |
| fixtures anonimizados | Respuestas del SDK anonimizadas con versión y TTL, solo con un script de anonimización interno de NetPay. |

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

## Decisiones del dueño
- S3: el skill solo permite tarjetas de prueba (la doc de NetPay permite propias con cuidado del NIP).
- Pendiente visto bueno de NetPay: política de reintento y lista de campos permitidos en logs.

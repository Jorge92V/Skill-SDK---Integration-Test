# Ejemplo Kotlin — adapter + folio + máquina de estados + cancelación

**Estado:** las firmas del SDK 1.1.9 usadas aquí se verificaron contra el AAR en una integración real
([lecciones-campo.md](../../references/lecciones-campo.md) §firmas). Este código de ejemplo en sí NO se ha compilado como proyecto:
copiarlo, compilar contra el AAR y ajustar los `TODO(soporte NetPay)` (valor de `storeId` en
cancelación/reimpresión y valores por defecto de los parámetros no usados de `SaleRequest`).

## Antes de copiar nada (primer arranque)
1. Dependencia y repositorio: [quickstart.md](../../references/quickstart.md) §3 (con `FAIL_ON_PROJECT_REPOS`, jitpack va en
   `dependencyResolutionManagement`) y `android.enableJetifier=true`.
2. `<queries>` en el manifiesto con el authority/acción del SDK (leídos del AAR o del mensaje de la
   excepción, nunca por package). Sin esto, `doTrans` lanza `IllegalArgumentException: Unknown URL ...`.
3. El Activity de cobro con `launchMode="singleTask"` y `configChanges` (ver cabecera de `CheckoutActivity.kt`).
4. Release con minify: reglas `-keep` del SDK (lecciones-campo.md §build).
5. Smart PinPad DEV instalada y con sesión. El ambiente lo determina la PinPad, no este código.

## Archivos
| Archivo | Para qué |
|---|---|
| `PaymentGateway.kt` | Interfaz propia (el SDK no expone una), estados (incluye `NO_INICIADA`) y modelos |
| `NetpaySmartGateway.kt` | Única clase que toca el SDK: argumentos nombrados, `code`, `when` con subclases primero, captura `Exception` |
| `FolioStore.kt` | Persiste folio, estado y monto en centavos ANTES de llamar al SDK |
| `PaymentCoordinator.kt` | Estados, bloqueo de cobro, falla de invocación vs DESCONOCIDA, consulta por folio, reintento, cancelación |
| `VentasAprobadas.kt` | Registro local de ventas aprobadas para cancelar (reglas CDMX, sin doble cancelación) |
| `FakePaymentGateway.kt` | Fake para pruebas unitarias sin terminal (T05, T08, T11, T12, T19, T20) |
| `CheckoutActivity.kt` | Cableado: `onResume` una vez, `onActivityResult`, salidas de cada estado |

## Qué NO hace (a propósito)
- No reversa nunca. El reverso lo hace la Smart PinPad.
- No reintenta solo. El reintento exige estatus catalogado, confirmación del cajero y folio nuevo vinculado.
- No deja estados sin salida: toda pantalla bloqueada ofrece consultar de nuevo o escalar.
- No loguea la respuesta completa ni datos de tarjeta/EMV; usa una etiqueta propia (`pagos`).
- No guarda ni pide credenciales de NetPay (viven en la Smart PinPad DEV).
- Cierre de lote, histórico y devolución: no documentados en el SDK; no hay código.

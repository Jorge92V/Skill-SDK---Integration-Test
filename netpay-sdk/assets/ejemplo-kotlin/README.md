# Ejemplo Kotlin — adapter + folio + máquina de estados

**Estado: SIN COMPILAR.** No hay AAR ni terminal en el entorno donde se escribió. Las firmas de
`SaleRequest`, `ReprintRequest(appId, "", folioId = folio)`, `CancelRequest`, `doTrans`, `onResult` y las
constantes `Constants.*_REQUEST` salen de la doc pública de NetPay (SDK 1.1.9, Smart PinPad 2.0+). Lo que la doc
no confirma está como `TODO(soporte NetPay)` en el código: nombres de acceso a campos de respuesta, tipo de retorno
de `onResult`, nombre vigente del callback (`onActivityResult` vs `onActivityCallResult`) y nombre del campo del
folio (`folio` vs `folioId`). Compilar contra el AAR y ajustar esos puntos.

## Archivos
| Archivo | Para qué |
|---|---|
| `PaymentGateway.kt` | Interfaz propia (el SDK no expone una) y modelos de resultado |
| `NetpaySmartGateway.kt` | Única clase que toca el SDK; copia solo campos permitidos para logs |
| `FolioStore.kt` | Persiste folio y estado ANTES de llamar al SDK (sobrevive a crash) |
| `PaymentCoordinator.kt` | Estados, bloqueo de cobro, DESCONOCIDA, consulta por folio, reintento con confirmación |
| `FakePaymentGateway.kt` | Fake para pruebas unitarias sin terminal (T05, T08, T11, T12) |
| `CheckoutActivity.kt` | Cableado mínimo en un Activity |

## Cómo usarlo
1. Agregar la dependencia y el repositorio (`references/quickstart.md` §3).
2. Copiar estos archivos, cambiar el package y conectar `CheckoutActivity` a tu UI.
3. Instalar Smart PinPad DEV en la terminal (`quickstart.md` §2). El ambiente lo determina la PinPad instalada,
   no este código: no hay selección de ambiente aquí (VALIDAR con NetPay).
4. Correr T01–T19 de `references/casos-de-prueba.md`; T05, T08 y T12 son obligatorios.

## Qué NO hace (a propósito)
- No reversa nunca. El reverso lo hace la Smart PinPad.
- No reintenta solo. El reintento exige estatus catalogado, confirmación del cajero y folio nuevo vinculado.
- No loguea la respuesta completa ni datos de tarjeta/EMV.
- No guarda ni pide credenciales de NetPay (viven en la Smart PinPad DEV).
- Cierre de lote, histórico y devolución: no documentados en el SDK; no hay código.

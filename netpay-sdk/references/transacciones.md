# Transacciones — SDK Smart (flujo, estado, folio)

> Fuente: documentación pública de NetPay (docs.netpay.com.mx: Smart SDK Terminales, Reimpresión por
> folio SDK, Manejo de reversos SDK, Certificación), consultada el 2026-10-13. Cada dato lleva
> su origen: **[doc]** = documentado por NetPay, **[skill]** = política de este skill (no de
> NetPay), **VALIDAR** = no confirmado. NetPay debe validar todo lo marcado [skill] y VALIDAR.
> **[campo]** = verificado en una integración real contra el AAR 1.1.9 (references/lecciones-campo.md); ante
> contradicción con [doc], prevalece lo observado en el AAR y se reporta a NetPay.
> Versión del SDK de referencia: 1.1.9 con Smart PinPad 2.0 o superior [doc]. La doc de NetPay
> tiene inconsistencias entre secciones (ver README, "Defectos de la documentación fuente").

## §operaciones
Todas se envían con `smartApi.doTrans(request)` [doc]. Los parámetros llevan `appId` = package
name de LA APP DEL INTEGRADOR [doc]. Requisito con `targetSdk >= 30`: `<queries>` en el manifiesto
(lecciones-campo.md §visibilidad) [campo]. `doTrans` puede lanzar excepciones distintas de
`SmartApiException`: capturar `Exception` [campo].

| Operación | Request [doc] | Notas [doc] |
|---|---|---|
| Venta | `SaleRequest(appId, amount: Double, tip: Double?, msi: Int?, waiter, settlementId, settlementDate, table, folio, ...)` [campo: orden real del AAR] | Usar argumentos NOMBRADOS: el orden difiere de los ejemplos de la doc. `amount` 0.01–999,999.99, 2 decimales; fuera de rango el SDK lanza `SmartApiException` "Monto inválido" [campo]. En la app, montos en centavos (`Long`). |
| Venta con promoción | `SaleRequest` con `msi` | No hay request aparte. `msi` ∈ {3, 6, 9, 12, 18}. 18 no aplica a Amex (matriz de certificación). |
| Propina | `SaleRequest` con `tip` | Máx. 25% (límite actual); debe activarse en el menú oculto de la Smart PinPad. |
| Cancelación | `CancelRequest(appId, orderId, storeId)` [campo] (valor de `storeId`: VALIDAR) | Solo mismo día, antes de las 20:00 hora de Ciudad de México, por el monto total. |
| Reimpresión por orderId | `ReprintRequest(appId, orderId, storeId, folioId)` [campo] | |
| Reimpresión por folio | `ReprintRequest(appId = appId, orderId = "", storeId = ..., folioId = folio)` | Con `orderId = ""` explícito; sin `orderId` ni `folioId` el SDK lanza "OrderId inválido Reprint Request" [campo]. Si se envían ambos, gana `orderId` y la recuperación por folio no se completa. `folioId` máx. 250 caracteres. |
| Ticket personalizado | `PrintRequest(appId, page)` | Requiere SDK 1.1.9 y Smart PinPad 2.0+; se certifica ANTES que el resto. |
| Lectura banda magnética | `ReadRequest(appId)` | Solo tarjetas NO financieras. |
| Devolución, cierre de lote, histórico, cashback | — | NO documentados en el SDK 1.1.9: ver dispositivos.md. |

Para respuesta y callbacks ver §respuesta.

## §adapter
- El SDK NO expone interfaz inyectable (confirmado por el dueño). El integrador define su propia
  interfaz (p. ej. `PaymentGateway`) y una implementación que envuelve al SDK; la lógica de la app
  (folio, estados, UI) depende solo de esa interfaz y se prueba con un fake sin terminal.
- Ejemplo de referencia: [`examples/kotlin/`](../examples/kotlin/README.md) (sin compilar; ver su README).

## §folio
- El folio viaja en la venta (`folio`) y es la llave para recuperar el resultado. Si la venta no
  lo llevó, no se puede recuperar por folio [doc].
- CoreX NO bloquea ni deduplica por folio (confirmado por el dueño): dos ventas con el mismo
  folio son dos cobros. Si el folio se repite, la reimpresión devuelve la ÚLTIMA transacción con
  ese folio [doc].
- NetpayPOS: deduplicación VALIDAR. Supuesto conservador: igual que CoreX.
- La app genera un folio único por intención, lo persiste ANTES de llamar al SDK y nunca lo
  reutiliza en una venta nueva [skill].
- Formato permitido del folio: VALIDAR (la reimpresión acepta hasta 250 caracteres [doc]).
- Nombre del campo: la venta usa `folio` (ejemplos con argumentos nombrados) y la doc de
  reimpresión habla de `folioId`/`sale.folioId`; en las respuestas aparece `folioNumber`. VALIDAR.

## §estado — máquina de estados de la app del integrador [skill]
| Estado | Cómo se llega | Qué hace la app |
|---|---|---|
| INICIADA | Folio generado y persistido | Bloquea UI de cobro |
| ENVIADA | `doTrans` invocado | Espera respuesta; UI bloqueada |
| APROBADA | `resultCode == RESULT_OK`, `success == true` y `code == "00"` (`responseCode` en la doc) | Muestra resultado, libera UI |
| DESCONOCIDA | Cualquier otro caso (ver abajo) | "Pendiente de confirmación"; consulta por folio; UI bloqueada pero con salida (consultar de nuevo / escalar) |
| NO_INICIADA [campo] | `doTrans` de la venta falló al invocar Y la consulta por folio tampoco pudo invocarse | "No se pudo comunicar con la Smart PinPad; el cobro no se inició"; libera UI |
| RESUELTA | La consulta por folio devuelve un estatus catalogado | Actúa según §consulta-por-folio |

Falla al INVOCAR (excepción síncrona) ≠ respuesta no concluyente [campo]: ver lecciones-campo.md §salida.
DESCONOCIDA incluye: `RESULT_CANCELED`, `data` nulo, excepción al leer la respuesta, ausencia de respuesta,
corte de comunicación, crash o app reabierta sin resultado final, y cualquier `responseCode` distinto
de "00" (el catálogo de códigos NO está publicado: solo se conocen "00" y el ejemplo "05" =
"SIN COMUNICACIÓN"; ver errores.md). La consulta por folio es barata y evita decidir sobre un código
no catalogado.

Tiempo de espera [doc]: el flujo de venta puede tardar hasta **3 minutos** (5 con cashback). No pases
a DESCONOCIDA por tiempo antes de ese plazo, salvo corte detectado. En certificación se prueban
esperas de 3 y de 4+ minutos: el comercio debe tener definido qué hace si no llega respuesta.

Desde DESCONOCIDA solo se sale a RESUELTA. Si la consulta falla o devuelve algo no catalogado, la
venta se queda en DESCONOCIDA y se escala a soporte.

## §consulta-por-folio — reimpresión por folio [doc salvo indicación]
Se valida el campo `reprintModule` de la respuesta. NO se debe usar `responseCode` para validar
el resultado de una reimpresión [doc].

| `reprintModule` / `transType` | Significado [doc] | Estado resultante | ¿Habilita reintento? [skill, propuesta pendiente de visto bueno de NetPay] |
|---|---|---|---|
| `C` + `A` | Aprobada | APROBADA | No |
| `C` + `V` | Cancelada | RESUELTA (cancelada) | No es reintento: es una venta nueva si el comercio la requiere |
| `D` | Declinada (regla de fraude o emisor) | RESUELTA (declinada) | Sí, con confirmación del cajero |
| `RV` | Reversada: no debe considerarse exitosa | RESUELTA (reversada) | Sí, con confirmación del cajero |
| `PRV` | Pendiente de reverso: se reversa con la siguiente transacción | RESUELTA (pendiente de reverso) | Sí: la venta nueva es el mecanismo documentado que dispara el reverso (ver §reverso), con confirmación del cajero |
| `success == false`, "No se encontraron datos" | Folio inexistente. Puede deberse a interrupción antes de completarse en línea (cancelación del usuario, red no disponible) o a declinación local de la terminal | RESUELTA (sin registro) | Condicional: solo tras verificar pantalla/ticket de la terminal y con confirmación del cajero |
| Cualquier otro | — | No catalogado | No: escalar |

La respuesta de reimpresión incluye además `orderId`, `folioNumber`, `isRePrint` (true), `transType`,
`success`, `message` y `authCode` [doc: los JSON de ejemplo escriben `autCode` y `meessage`; VALIDAR
nombres reales].

## §reverso
- El reverso lo ejecuta la Smart PinPad: con 2.0.x hace hasta 3 reintentos automáticos [doc]. La app
  del integrador nunca genera reversos (confirmado por el dueño; la doc no le asigna ninguna acción).
- Si los 3 reintentos fallan (red inestable, apagado de la terminal, batería retirada), la transacción
  queda en `PRV`: aprobada en NetPay Manager y pendiente de reversar. La siguiente transacción
  (desde 1 centavo, cualquier tarjeta) dispara el reverso [doc].
- Prioridad de fuentes [doc]: pantalla y ticket de la terminal primero; NetPay Manager puede mostrar
  estatus de reverso inconsistentes. No usar Print-Ticket para verificar reversos.
- Ejemplo documentado de respuesta de sin comunicación: mensaje "SIN COMUNICACIÓN", `responseCode`
  "05", `transType` "A", con `rrnNumber` y `terminalId`. No hay campo dedicado de reverso en el JSON.
- La implementación del manejo de reversos es obligatoria antes de certificar [doc].
- Cuántos segundos tarda la Smart PinPad en reversar y qué ve la app durante ese lapso: VALIDAR.

## §reintento [skill]
Condiciones (todas): (1) la consulta por folio devolvió un estatus cuya fila de §consulta-por-folio
habilita reintento; (2) el cajero lo confirma; (3) folio NUEVO; (4) se registra el vínculo con el folio
anterior. Nunca automático.
Riesgo residual (declarar siempre): sin deduplicación por folio, una transacción en vuelo puede
aprobarse después de la consulta; el reintento sería un segundo cobro.

## §ciclo-de-vida
La doc no describe qué pasa si la app del integrador pasa a segundo plano o muere a mitad de una venta,
ni si la Smart PinPad conserva el estado: VALIDAR (es un caso de la matriz: T11).
Regla mínima [skill]: al reabrir con un folio persistido sin resultado final, pasar a DESCONOCIDA y
consultar por folio antes de permitir otra venta.
[campo] `onActivityResult` corre ANTES de `onResume`: esa revisión va en `onResume` (no en `onCreate`) y UNA
vez por instancia del Activity, o un estatus no catalogado provoca un bucle de consultas. Evitar recreación
del Activity con `launchMode="singleTask"` y `configChanges` (lecciones-campo.md §ciclo).
Multi-instancia: la matriz de certificación exige bloquear el botón de cobro tras el primer envío, sin
ventas duplicadas.

## §respuesta
[campo] `onResult` devuelve `BaseResponse`; `CancelResponse` y `ReprintResponse` heredan de `SaleResponse`
(en un `when` con `is`, subclases primero). `success` es `Boolean` no nulo; el código se lee con `code`; el folio
con `folioNumber`. El resultado llega por `startActivityForResult` (funciona con `ComponentActivity`/Compose).
Recepción [doc]: en `onActivityResult(requestCode, resultCode, data)`, si `data != null`, llamar
`smartApi.onResult(requestCode, resultCode, data)` y convertir según `requestCode`:
`Constants.SALE_REQUEST` → `SaleResponse`, `CANCEL_REQUEST` → `CancelResponse`,
`REPRINT_REQUEST` → `ReprintResponse`, `PRINT_REQUEST` → `PrintResponse`, `READ_MAGNETIC_REQUEST` →
`ReadResponse`. `resultCode`: `RESULT_OK` si la operación se completó; `RESULT_CANCELED` si el usuario
se retiró o falló. La doc también usa el nombre `onActivityCallResult`: VALIDAR cuál es el callback
vigente.

Campos de la respuesta de venta (SDK 1.1.9) [doc]: affiliation, applicationLabel, arqc, aid, amount,
authAmount, authCode, bankName, bin, cardExpDate, cardType (D/C), cardTypeName, cityName,
responseCode, folioNumber, hexSign, internalNumber, moduleCharge, moduleLote, customerName,
terminalId, orderId, preAuth, promotion, rePrintDate, rePrintMark, reprintModule, cardNumber, storeId,
storeName, streetName, tableId, tipAmount, tipLessAmount, transDate, transType, transactionCertificate,
transactionId, message, rrnNumber, sdkVersion, spanRoute, traceability, hasPin, isRePrint, success,
isQps, preStatus.
- `code == "00"` (la doc lo llama `responseCode`) indica transacción exitosa; cualquier otro valor se trata como problema.
- Desde SDK 1.1.9, `rrnNumber` es independiente de `orderId` (en 1.1.8, `orderId` = RRN + serie).
  Si se reimprime con `orderId`, puede requerir ajuste al migrar.
- `amount` ya no incluye propina desde 1.1.8 (la doc se contradice en este punto: VALIDAR).
- `transType`: `A` venta, `V` cancelación; un ejemplo de 1.1.9 trae `PRE` sin documentar.
- Campos sensibles y permitidos para log: ver seguridad.md §logging.

## §ventanas
- Cancelación: mismo día, antes de las 20:00 (hora de Ciudad de México), monto total [doc]. En la app: zona
  `America/Mexico_City` (no la del dispositivo), registro local de ventas aprobadas porque el SDK no expone
  histórico, sin doble cancelación; cancelación no confirmada = "sin confirmar", ni cancelada ni vigente [campo].
- Devolución y su ventana respecto al cierre de lote: NO documentado para SDK: VALIDAR.
- Cierre de lote automático: la doc indica corte automático cerca de las 450 transacciones (menú
  oculto de la Smart PinPad). No hay operación de cierre/histórico por SDK documentada.

# Glosario negocio → SDK

> [doc] = documentado por NetPay (SDK 1.1.9). VALIDAR = pendiente.

| Término de negocio | Equivale a | Campo/enum del SDK |
|---|---|---|
| Venta con promoción | MSI (mapeo interno de NetPay, confirmado por el dueño). El SDK no distingue otros tipos | `msi: Int` en `SaleRequest` [doc] |
| MSI (plazos) | Meses sin intereses | 3, 6, 9, 12, 18 [doc]. 18 no aplica a Amex. Plazos por emisor/afiliación: VALIDAR |
| Folio | Identificador de la intención de cobro, generado por la app del integrador | `folio` en `SaleRequest`; `folioId` en `ReprintRequest`; `folioNumber` en respuestas. Nombres: VALIDAR |
| orderId | Identificador de la transacción en NetPay | `orderId` (desde 1.1.9 ya no es RRN + serie) [doc] |
| rrnNumber | Referencia de recuperación de la transacción | `rrnNumber` (independiente de `orderId` desde 1.1.9) [doc] |
| Reimpresión por folio | Consulta del estatus de una transacción por folio | `ReprintRequest(appId, orderId = "", folioId = …)` [doc] |
| `reprintModule` | Estatus devuelto por la reimpresión | `C` aprobada/cancelada, `D` declinada, `RV` reversada, `PRV` pendiente de reverso [doc] |
| `transType` | Tipo de transacción | `A` venta, `V` cancelación (y `PRE` sin documentar) [doc] |
| Cancelación | Anulación de una venta del mismo día | `CancelRequest(appId, orderId)`; antes de 20:00 CDMX, monto total [doc] |
| Devolución | Operación distinta de la cancelación (existe en API) | No documentada en SDK: VALIDAR |
| Reverso | Lo ejecuta la Smart PinPad (3 reintentos); nunca la app del integrador | — |
| Reverso pendiente (PRV) | Aprobada en NetPay, por reversar con la siguiente transacción | `reprintModule == "PRV"` [doc] |
| Cashback | Solo CoreX (confirmado por el dueño) | Sin parámetro documentado en `SaleRequest`: VALIDAR |
| Smart PinPad | App de NetPay en la terminal que procesa el cobro | — |
| Smart PinPad DEV | Clon de la productiva apuntando a sandbox (leyenda "VERSIÓN DEMO") | — |
| appId | Package name de la app del integrador | Primer parámetro de todos los requests [doc] |

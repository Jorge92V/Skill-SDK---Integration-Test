# Errores y estatus

> NetPay NO publica un catálogo de `responseCode` ni de errores de `SmartApiException` (consultado
> 2026-10-13). Solo se conocen los valores de abajo. Cualquier otro código se reporta como "no catalogado" y
> se escala a soporte; si llegó durante una venta, la venta queda DESCONOCIDA (transacciones.md §estado).
> Los estatus de la reimpresión por folio están en transacciones.md §consulta-por-folio.

| Código / mensaje | Significado [doc] | Acción del integrador [skill] |
|---|---|---|
| `responseCode` "00" con `success` true | Transacción exitosa | APROBADA |
| `responseCode` "05", mensaje "SIN COMUNICACIÓN" (ejemplo con `transType` "A") | Sin comunicación durante la venta | DESCONOCIDA: consultar por folio; puede quedar en `PRV` |
| Mensajes "ERROR AL LEER LA TARJETA", "SIN COMUNICACIÓN", "ERROR DE CONEXIÓN" (pantalla y ticket de la terminal) | Falla de lectura o de comunicación; puede activar reverso automático | La pantalla y el ticket de la terminal tienen prioridad sobre NetPay Manager; consultar por folio |
| Mensaje "TRANSACCION RECHAZADA" | Rechazo | Código asociado no catalogado: DESCONOCIDA y consultar por folio (la reimpresión devuelve `D`) |
| Mensaje "Cancelado por el usuario" | El usuario canceló sin insertar tarjeta | Sin cobro; libera UI |
| Mensaje "INTENTE OTRA INTERFAZ" | Contactless Visa declinada en monto alto | Pedir otra interfaz (chip) |
| Reimpresión: "No se encontraron datos", `success` false | Folio inexistente | Ver §consulta-por-folio (sin registro) |
| `SmartApiException` | Falla al invocar el SDK; hay `message` en texto libre | DESCONOCIDA si ya se llamó a `doTrans`; no hay catálogo |
| `RESULT_CANCELED` | El usuario se retiró o falló | DESCONOCIDA: consultar por folio |
| Error 13 al instalar la Smart PinPad DEV | Está instalada la "App de Pagos" | Pedir a NetPay que la desinstale |

Para ampliar este catálogo: usar el top-N de códigos según el volumen de tickets de soporte (pendiente de
NetPay).

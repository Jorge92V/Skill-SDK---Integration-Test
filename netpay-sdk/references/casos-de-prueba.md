# Casos de prueba para el integrador

Criterio de aprobación: la app maneja la CATEGORÍA de resultado correctamente. No se exige respuesta idéntica
(el sandbox no es determinista). Un resultado fuera de lo documentado en sandbox.md se reporta como deriva
del sandbox.

Obligatorios para certificación en este skill: T05, T08 y T12 (la implementación de reversos es obligatoria
antes de certificar [doc]). La columna "Matriz NetPay" cruza con los escenarios de la matriz de certificación
de API (la de SDK existe pero no está en la doc pública consultada: VALIDAR que coincida).

| ID | Escenario | Cómo se provoca | Resultado esperado | Matriz NetPay |
|---|---|---|---|---|
| T01 | Venta aprobada (débito NIP, crédito NIP, contactless) | Datos documentados en sandbox.md | `success`, `responseCode` "00"; libera UI | 1, 5, 7 |
| T02 | Venta declinada | Monto > $12,000 (regla de fraude, solo PRUEBAS) | Declinada; mensaje al cajero, sin cobro | 9 |
| T03 | Venta con promoción (MSI 3/6/9/12; 18 salvo Amex) | Crédito, plazo del glosario | Plazo aplicado | opcional |
| T04 | Promoción no elegible | VALIDAR (sin regla documentada) | Rechazo claro, sin cobro | — |
| T05 | Sin respuesta (espera de 3 min y de 4+ min) | Entorno: temporizador / cortar comunicación | DESCONOCIDA → consulta por folio; sin reverso propio; sin reintento ciego | 10, 11 |
| T06 | Cancelación válida | Venta del mismo día, antes de 20:00 CDMX, monto total | Cancelación aprobada (`transType` "V") | opcional |
| T07 | Cancelación de venta inexistente / ya cancelada | `orderId` inexistente o cancelada | Error controlado; la app evita cancelar una venta ya cancelada | opcional |
| T08 | Reversos: automático (`RV`) y manual (`PRV` → venta posterior → `RV`) | Entorno: retirar tarjeta antes de aprobar; apagar terminal o retirar batería | La app NO reversa; la consulta por folio refleja `RV` / `PRV`; ante `PRV` el cajero sabe que la venta no es cobro válido | 12–17 |
| T09 | Cierre de lote | — | No aplica: no documentado en SDK | — |
| T10 | Terminal sin conexión | Entorno | Falla controlada, sin transacción colgada | — |
| T11 | App a segundo plano o muerta a mitad de venta | Entorno | Al volver/reabrir: consulta por folio antes de permitir otra venta | — |
| T12 | Doble toque / multi-instancia en cobrar | UI | Botón bloqueado tras el primer envío; un solo folio, un solo cobro (CoreX no deduplica) | 2 |
| T13 | Cancelado por el usuario / tarjeta retirada antes de iniciar | Cualquier tarjeta sin insertarla | "Cancelado por el usuario"; sin cobro | 8 |
| T14 | Venta con firma autógrafa (tarjeta sin NIP) | Chip sin NIP | La terminal pide firma; aprobada | 3 |
| T15 | Reimpresión por orderId y por folio (aprobada y declinada) con multi-instancia | Operación | Estatus según §consulta-por-folio | recomendada |
| T16 | Folio inexistente | Folio que no existe | `success == false`, "No se encontraron datos"; sin conclusión de cobro | — |
| T17 | Propina (activada / desactivada en menú oculto) | Datos del sandbox | Monto y propina correctos; `amount` sin propina desde 1.1.8 | opcional |
| T18 | Cashback (solo CoreX) | VALIDAR | Espera de 5 min; certificación propia | opcional |

Cambios de alcance (MSI, propina, cashback, devoluciones, NFC) requieren recertificar [doc].

## Guion de entorno (T05, T08, T10, T11)
Pasos exactos para provocarlos en terminal real: los de T05 (temporizador) y T08 (retirar tarjeta, apagar
terminal/retirar batería) están en la matriz de NetPay; los de T10 y T11 son VALIDAR.

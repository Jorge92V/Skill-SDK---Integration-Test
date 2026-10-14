# Dispositivos y matriz de capacidades

> Estados: SOPORTADA / NO DOCUMENTADA / NO SOPORTADA / VALIDAR. Fuente: documentación pública del SDK
> 1.1.9 (2026-10-13). La doc pública NO distingue NetpayPOS de CoreX; por confirmación del dueño las
> operaciones están homologadas entre ambas, salvo cashback (solo CoreX).

## Matriz por operación
| Operación | Estado | Detalle |
|---|---|---|
| Venta (`SaleRequest`) | SOPORTADA | [doc] |
| Venta con promoción (MSI) | SOPORTADA | `msi` ∈ {3,6,9,12,18}; requiere recertificar si no se certificó antes [doc] |
| Propina | SOPORTADA | ≤25%; se activa en menú oculto de la Smart PinPad [doc] |
| Cancelación (`CancelRequest`) | SOPORTADA | Mismo día, antes de 20:00 CDMX, monto total [doc] |
| Reimpresión por orderId | SOPORTADA | [doc] |
| Reimpresión por folio | SOPORTADA | CoreX [doc]; NetpayPOS por homologación (dueño) |
| Ticket personalizado / QR impreso | SOPORTADA | SDK 1.1.9 + Smart PinPad 2.0+; se certifica primero [doc] |
| Lectura banda magnética (`ReadRequest`) | SOPORTADA | Solo tarjetas no financieras [doc] |
| Lectura de tags NFC | SOPORTADA | NTAG 213/215/216; SDK 1.1.9 sin librería extra [doc] |
| Cámara (QR / código de barras) | SOPORTADA | PAX A910S y PAX IM30; APIs estándar de Android, sin Google Play Services [doc] |
| Bloqueo de botones de navegación | SOPORTADA | `enableNavigation(NavBarButtons.ALL, false)`; recomendado SDK 1.1.9 + Smart PinPad 2.2 [doc] |
| Ocultar barra de navegación digital | NO DOCUMENTADA | La doc marca la sección "en mantenimiento" |
| Cashback | VALIDAR | Solo CoreX (dueño). Sin parámetro documentado en `SaleRequest`; espera de 5 min con cashback [doc]; requiere certificación propia |
| Devolución | NO DOCUMENTADA en SDK | Existe en la integración API |
| Cierre de lote | NO DOCUMENTADA en SDK | Solo menú oculto y corte automático (~450 trx) [doc]. Hay un requerimiento abierto de NetPay para exponerlo |
| Histórico de transacciones | NO DOCUMENTADA en SDK | Ídem |

## Marcas y modelos
Marcas: PAX, Kozen, Urovo. Cámara: solo PAX A910S e IM30 [doc]. Comportamiento específico de Kozen y
Urovo: la doc solo indica que "puede variar" (VALIDAR). Matriz modelo × versión: VALIDAR.
[campo] Integración real funcionando: CoreX + SDK 1.1.9 en terminal Kozen con Android 15. El nombre de modelo
que reporta el sistema puede no coincidir con el comercial: registrar ambos y confirmar con NetPay.

## Versiones [doc]
| Pieza | Dato |
|---|---|
| SDK | 1.1.9 es la versión con la tabla de campos documentada; 1.1.5–1.1.8 en páginas antiguas |
| Smart PinPad | SDK 1.1.9 ↔ PinPad 2.0+. v1.x.x sin soporte desde 2025-12-30; migrar a v2.1.3 o posterior |
| Android | API 22 (5.1) o superior |

## Recertificación [doc]
Se requiere ante cambios de: librería `.aar`, versión de Smart PinPad, tipo de integración (API/SDK/COM),
modelo o marca de terminal, y funcionalidades no certificadas (MSI, propina, cashback, devoluciones, NFC).

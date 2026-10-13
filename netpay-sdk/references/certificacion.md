# Preparación para certificación

> La certificación es un proceso de NetPay. Este checklist prepara la integración; no la certifica.
> Fuente del proceso: docs.netpay.com.mx, "Certificación y Pasos a Seguir" (2026-10-13).

## Proceso de NetPay [doc]
1. Desarrollo: solicitar terminal de pruebas e integrar según la doc.
2. Certificación en sandbox: agendar sesión con Integraciones, completar la matriz de pruebas y ejecutarla.
   Flujo de 9 pasos: alcance, datos del proyecto, preguntas técnicas, certificación del ticket personalizado
   (si aplica, va primero), ejecución, entrega de evidencia (ticket impreso y JSON de la MISMA transacción),
   revisión de NetPay, cierre y producción. Si hay observaciones, se corrige y se repite.
3. Pre-producción: el asesor comercial de NetPay solicita las terminales productivas (no el integrador, y no
   antes de concluir la certificación: puede haber multas por falta de uso); prueba piloto con una terminal.
4. Producción: despliegue del resto.
5. Solo SDK: publicación de la app del integrador en PAX STORE después de certificar (el procedimiento está
   en la doc de NetPay; firma y despliegue quedan fuera del alcance de este skill).

## Checklist técnico (también es el chequeo manual si no hay script)
- [ ] Casos T05, T08 y T12 ejecutados y los que apliquen de T01–T19 — ver casos-de-prueba.md
- [ ] Manejo de reversos implementado (obligatorio antes de certificar [doc]): estatus `RV` y `PRV` por consulta
- [ ] Folio único por intención, viaja en la venta, persistido antes de llamar al SDK, nunca reutilizado
- [ ] Sin reverso propio en la app
- [ ] Consulta por folio ante estado desconocido y al reabrir con folio pendiente
- [ ] Espera mínima de 3 minutos (5 con cashback) antes de dar por perdida una respuesta
- [ ] Reintento solo según transacciones.md §reintento (nunca automático, folio nuevo vinculado)
- [ ] Bloqueo del botón de cobro tras el primer envío (multi-instancia)
- [ ] Validación de resultado de reimpresión con `reprintModule`, no con `responseCode`
- [ ] Sin PAN/CVV/tracks/PIN en memoria persistente, logs ni analytics
- [ ] Logs solo con campos permitidos (seguridad.md §logging)
- [ ] Sin credenciales de NetPay en el código ni en el repo
- [ ] Llamadas al SDK solo a través del adapter propio (transacciones.md §adapter)
- [ ] Ticket personalizado (si lo usa): SDK 1.1.9 y Smart PinPad 2.0+, certificado antes que el resto
- [ ] Si existe `scripts/validar_integracion.py` en el paquete: sin hallazgos críticos

## Recertificación [doc]
Cambios en: librería `.aar`, versión de Smart PinPad, tipo de integración, modelo o marca de terminal, y
funcionalidades no certificadas (MSI, propina, cashback, devoluciones, NFC).

## Paso a producción (go-live)
La doc indica que el cambio de ambiente debe poder hacerse "por configuración, sin recompilar" y lista como
datos de configuración la URL base, el path y el body del token (esa lista corresponde a la integración API).
Para SDK, la doc no describe qué cambia en la app del integrador: según la doc del SDK, el ambiente lo marca
la Smart PinPad de la terminal. VALIDAR con NetPay. Habilitación de URLs/puertos/IPs de producción en la
red de la sucursal: sí es requisito [doc]. Firma y despliegue: fuera del alcance de este skill.
No se prueba con la Smart PinPad productiva ni con tarjetas reales fuera del proceso de NetPay.

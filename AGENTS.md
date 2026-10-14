# AGENTS.md

Este repo contiene el skill `netpay-sdk`: integración de cobros con el Smart SDK Android de NetPay (NetpayPOS y CoreX/SmartCore) en terminales PAX, Kozen y Urovo.
Si tu agente no carga skills de forma nativa: cuando la tarea trate de ese SDK, lee y sigue [netpay-sdk/SKILL.md](netpay-sdk/SKILL.md) antes de responder o generar código.
SKILL.md enlaza el detalle en `netpay-sdk/references/` y el código de referencia en `netpay-sdk/examples/kotlin/`; léelos solo cuando el paso lo pida.
Las reglas de seguridad (§6) y de folio/reverso (§7) son bloqueantes: nunca credenciales, PAN ni respuesta completa en logs; la app del integrador nunca reversa.
No inventes firmas, códigos ni datos de prueba: lo que no esté en `references/` se marca `TODO(soporte NetPay)` o se escala.
Mantenimiento del repo: [README.md](README.md), [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md) y `python3 tools/validate_skill.py`.

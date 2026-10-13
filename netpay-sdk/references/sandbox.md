# Sandbox (Smart PinPad DEV) — ambiente de pruebas

## Qué es
- [dueño] La app dev es un clon de la app productiva que apunta a un ambiente sandbox. NO es un mock: el
  comportamiento (incluido el reverso) es el real. Las llaves/credenciales se inicializan dentro de la app
  dev. El integrador solo recibe la app dev. No hay panel de configuración: se prueba una a una con tarjetas
  y montos. El sandbox NO es determinista.
- [doc] Se instala desde Netpay Market como "Smart PinPad DEV"; el login usa Store ID y Password de prueba
  entregados por el equipo de Integraciones; muestra la leyenda "VERSIÓN DEMO". Pasos en quickstart.md.
- [dueño] El package name de la Smart PinPad DEV es distinto al de la productiva. [doc] La doc del SDK no
  expone ese package: el SDK pasa solo `appId` = package de la app del integrador. Por tanto, según la doc,
  el código del integrador NO elige el ambiente: lo determina la Smart PinPad instalada en la terminal
  (DEV en pruebas, productiva después de certificar). VALIDAR con NetPay que no exista ninguna selección
  explícita en el SDK.

## Reglas [skill]
- Solo tarjetas de prueba en la Smart PinPad DEV. (La doc de NetPay permite tarjetas propias con cuidado
  del NIP; el skill es más estricto a propósito: un PAN real no debe pasar por un ambiente de pruebas ni
  por sus logs. NetPay puede relajarlo.)
- No instalar la Smart PinPad productiva en terminales de prueba.
- Un resultado distinto a las reglas documentadas = "posible deriva del sandbox": reportar, no asumir error de integración.
- Al pedir datos de prueba, el skill solo cita lo documentado aquí. No completa por analogía ni con números
  de prueba públicos de otras marcas o adquirentes.

## Reglas de resultado documentadas
> Origen: matriz de certificación de la integración API, ejecutada en el ambiente PRUEBAS. Para SDK la
> misma matriz existe pero no está en la doc pública consultada: validar que apliquen igual.

| Caso | Tarjeta (tipo) | Monto | Resultado esperado [doc] |
|---|---|---|---|
| Venta con NIP, débito | Débito con NIP | Cualquiera | Aprobada |
| Venta con NIP, crédito | Crédito con NIP | Cualquiera | Aprobada |
| Venta con firma | Chip sin NIP, o cualquier tarjeta sin NIP, o contactless Mastercard > $1,000 | Cualquiera | La terminal pide firma; aprobada |
| Contactless Mastercard | Contactless MC | $1 a $999 | Aprobada (sujeto al emisor) |
| Contactless Mastercard | Contactless MC | > $10,001 | Aprobada o declinada, ambas válidas |
| Contactless Visa | Contactless Visa | $1 a $999 | Aprobada (sujeto al emisor) |
| Contactless Visa | Contactless Visa | > $1,200 | Aprobada o declinada; si declina, "INTENTE OTRA INTERFAZ" |
| Regla de fraude (solo PRUEBAS) | Cualquiera | > $12,000 | Declinación simulada |
| Cancelado por el usuario | Cualquiera, sin insertarla | — | Mensaje "Cancelado por el usuario" |
| MSI 3/6/9/12 | Crédito (y débito en la matriz) | Cualquiera | Aprobada con el plazo; 18 no aplica a Amex |
| Reverso automático | Tarjeta con NIP | Cualquiera | Retirar la tarjeta antes de aprobarse → `RV` |
| Reverso manual | Cualquiera | Cualquiera | Apagar la terminal o retirar la batería → `PRV`; la siguiente venta lo reversa → `RV` |

## Datos de prueba
No hay catálogo de tarjetas ni montos por caso: el sandbox no es determinista y las pruebas se hacen una a
una. El skill solo cita las reglas de arriba y no inventa tarjetas ni montos.

## Cómo obtener la app dev y terminales/tarjetas de prueba
Terminal de pruebas: se solicita a NetPay y se manda su número de serie [doc]. Tarjetas físicas de prueba y
cómo las obtiene un integrador externo: VALIDAR.

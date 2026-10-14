# Lecciones de campo — primera integración real

> Origen: una integración hecha de cero con este skill (SDK Smart 1.1.9, CoreX, terminal Kozen, Android 15),
> con firmas verificadas contra el AAR y comportamiento observado en terminal. Etiqueta: **[campo]** =
> observado/verificado en esa integración; no es documentación oficial de NetPay. Si algo de aquí contradice
> otra referencia, se reporta a soporte de integraciones de NetPay como hallazgo. No contiene credenciales,
> seriales, authorities, packages de NetPay ni datos de comercio, y así debe seguir.
>
> Leer ANTES de generar código del SDK y ANTES de depurar un cobro que "no abre la PinPad" o que se queda en
> "pendiente de confirmación".

## §visibilidad — "no abre la PinPad" (Android 11+) [campo]
- Síntoma: `doTrans` lanza `IllegalArgumentException: Unknown URL content://<authority>/...` antes de abrir
  la Smart PinPad. La consulta por folio falla igual.
- Causa: el SDK escribe en un ContentProvider de la Smart PinPad y la resuelve con un intent implícito. Con
  `targetSdk >= 30`, Android oculta ambos si la app no los declara.
- Arreglo: declarar `<queries>` en el manifiesto de la app del integrador, con un `<provider>` (el authority
  que aparece en el mensaje de la excepción) y un `<intent>` (la acción y el esquema que usa el SDK).
  Se declaran por authority/acción, NUNCA por package: el ambiente lo sigue decidiendo la PinPad instalada
  (S2). Nunca fijar el package de la PinPad en código ni manifiesto.
- Los valores salen de las constantes del propio SDK: leerlos del mensaje de la excepción o del AAR. El
  skill no los conoce y no los inventa.
- Estado: no está en la doc de NetPay. VALIDAR con soporte.

```xml
<!-- AndroidManifest.xml de la app del integrador. Los valores entre <> se leen del AAR o de la excepción. -->
<queries>
    <provider android:authorities="<authority del SDK>" />
    <intent>
        <action android:name="<acción del SDK>" />
        <data android:scheme="<esquema del SDK>" />
    </intent>
</queries>
```

## §salida — toda falla debe tener salida [campo]
- `doTrans` puede lanzar excepciones que NO son `SmartApiException` (p. ej. `IllegalArgumentException`).
  Capturar `Exception`, no solo `SmartApiException`.
- Distinguir dos situaciones:
  1. **Falla al INVOCAR** (excepción síncrona, la terminal no respondió nada). Se intenta la consulta por
     folio; si TAMPOCO puede invocarse, la transacción nunca llegó a la PinPad: estado `NO_INICIADA`, se
     libera la UI con "No se pudo comunicar con la Smart PinPad; el cobro no se inició".
  2. **La terminal respondió algo no concluyente** (cancelado, `data` nulo, código no catalogado, sin
     respuesta, app muerta): DESCONOCIDA, consultar por folio, sin otra venta hasta resolver.
- Toda pantalla de bloqueo tiene una acción de salida (consultar de nuevo, cerrar con confirmación o
  escalar). Un estado terminal sin botón es un defecto.
- Primera sospecha ante un bucle en "pendiente de confirmación": que la consulta por folio ni siquiera se
  pueda invocar (§visibilidad).

## §ciclo — no ciclar consultas [campo]
- El resultado llega por `startActivityForResult` → `onActivityResult` del Activity. Funciona con
  `ComponentActivity` / Compose.
- `onActivityResult` corre ANTES de `onResume`. Revisar "folio sin resultado final" en `onResume`, no en
  `onCreate`, para no marcar DESCONOCIDA un resultado que está por llegar.
- Hacer esa revisión UNA vez por instancia del Activity. Si se repite en cada `onResume`, un estatus no
  catalogado provoca: consulta → regresa de la PinPad → `onResume` → consulta… en bucle.
- Evitar que el Activity se recree durante el cobro: `android:configChanges="orientation|screenSize|
  screenLayout|smallestScreenSize|keyboardHidden|uiMode"` y `android:launchMode="singleTask"`.

## §firmas — SDK 1.1.9 verificado en el AAR [campo]
La doc pública se contradice; esto es lo observado en el AAR:
- `SaleRequest(appId, amount: Double, tip: Double?, msi: Int?, waiter, settlementId, settlementDate, table,
  folio, ...)`. El orden real difiere de los ejemplos: usar SIEMPRE argumentos nombrados.
- `CancelRequest(appId, orderId, storeId)`; `ReprintRequest(appId, orderId, storeId, folioId)`. Qué valor
  lleva `storeId` y si es opcional: VALIDAR (no confundir con credenciales: no es la contraseña).
- `onResult(requestCode, resultCode, data)` devuelve `BaseResponse`. `CancelResponse` y `ReprintResponse`
  heredan de `SaleResponse`: en un `when` con `is`, poner primero las subclases.
- Campos como propiedades Kotlin: `success` es `Boolean` no nulo; lo que la doc llama `responseCode` se lee
  con **`code`**; el folio de la respuesta es `folioNumber`.
- Validaciones locales que lanzan `SmartApiException` al invocar: monto fuera de 0.01–999,999.99
  ("Monto inválido"); reimpresión sin `orderId` ni `folioId` ("OrderId inválido Reprint Request"). La
  reimpresión por folio lleva `orderId = ""` explícito.
- Montos: manejar centavos (`Long`) en la app y convertir a `Double` con 2 decimales solo al llamar al SDK.

## §build — Gradle [campo]
- Con `FAIL_ON_PROJECT_REPOS`, el repositorio `https://jitpack.io` va en `dependencyResolutionManagement`
  (settings.gradle). En la prueba bajó sin credenciales (VALIDAR en otros entornos).
- El SDK arrastra librerías `com.android.support`: con AndroidX falla el merge del manifiesto
  (`appComponentFactory`). Solución: `android.enableJetifier=true` en `gradle.properties` del proyecto.
- Release con minify: conservar el SDK (usa Gson sobre sus modelos). VALIDAR con NetPay:
  ```
  -keep class mx.com.netpay.sdk.** { *; }
  -keepattributes Signature, *Annotation*
  ```
- `targetSdk 35` obliga a pantalla de borde a borde: manejar insets en la UI.

## §terminal — estado persistido y pruebas en terminal [campo]
- El folio y el estado se guardan antes de llamar al SDK, así que sobreviven a `adb install -r`: un folio
  atascado de una prueba anterior reaparece. Ofrecer reinicio de estado SOLO en builds de desarrollo, o
  limpiar datos de la app de prueba.
- En terminales POS, `adb shell am force-stop` puede no terminar el proceso; `pm clear <package propio>` sí
  lo reinicia. Tocar SOLO la app propia: nunca apps de NetPay ni otras apps de la terminal.
- `screencap` puede salir negro en terminales: no asumir que se opera la UI con capturas; pedir al usuario
  el toque y leer el log propio.
- El modelo que reporta el sistema puede no coincidir con el nombre comercial: registrar ambos y confirmar
  con NetPay para la matriz de modelos.

## §diagnostico — diagnóstico seguro [campo]
- Loguear con una etiqueta propia y leer solo esa (`adb logcat -s <tag>`). No leer logs ni listar apps de
  terceros en la terminal.
- Excepciones: loguear clase, mensaje y los 3 primeros frames (ocurren antes de abrir la PinPad, sin datos
  de tarjeta). Sigue vigente S5: nunca el objeto de respuesta completo.
- No leer ni citar configuración global del desarrollador (p. ej. `~/.gradle/gradle.properties`) que pueda
  tener credenciales. Si aparece una credencial: no repetirla y pedir que se rote (S1).

## §propina-cancelacion [campo]
- Propina: `tip` va aparte de `amount` (`// SUPUESTO:` hasta confirmar con T17). Máximo 25% del monto,
  validado en la app antes de llamar al SDK. Debe estar activada en el menú oculto de la PinPad.
- Cancelación: el SDK no expone histórico. La app guarda localmente las ventas aprobadas (folio, `orderId`,
  montos, hora, estado; nunca datos de tarjeta) para poder cancelarlas.
- Reglas en la app: mismo día y antes de las 20:00 en zona `America/Mexico_City` (no la del dispositivo),
  monto total, y no cancelar dos veces (T07).
- Cancelación no confirmada (código no catalogado o sin respuesta): marcarla "sin confirmar", no asumirla
  cancelada ni vigente; pedir verificar la terminal y escalar.

## §primer-arranque — checklist en terminal [campo]
1. Smart PinPad DEV instalada y con sesión (credenciales de prueba en la PinPad; nunca en la app ni en el chat).
2. `<queries>` declarado (§visibilidad) y `targetSdk` coherente con la terminal.
3. Una venta de prueba abre la PinPad. Si no: leer el log propio y revisar §visibilidad antes que nada.
4. T12, T05 y T08 antes de pedir certificación.
5. Solo tarjetas de prueba (S3).

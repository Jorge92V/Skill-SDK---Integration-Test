# Quickstart — integración por SDK Smart

> Fuente: docs.netpay.com.mx (Smart SDK Terminales, Configuración inicial de la terminal, Manual de
> SmartPinPad DEV), consultada el 2026-10-13. [doc] = documentado, [skill] = política, VALIDAR = pendiente.

## 1. Requisitos
- Terminal Smart con Internet, en modo debug (solo para integraciones), Android 5.1 (API 22) o superior
  [doc]. Cómo activar el modo debug: VALIDAR.
- Credenciales de prueba (Store ID y Password) que entrega por correo el equipo de Integraciones de
  NetPay [doc]. Se capturan en la Smart PinPad DEV, NO en la app del integrador.
- Tarjetas de prueba (con y sin NIP, débito y crédito) [doc]. Cómo las obtiene un externo: VALIDAR.
- Enviar el número de serie de la terminal (parte trasera) en respuesta al correo de documentación [doc].
- Revisar con NetPay las URLs, puertos e IPs que deben habilitarse en la red de la sucursal (documento
  de acceso restringido) [doc].

## 2. Instalar la Smart PinPad DEV
1. Abrir Netpay Market en la terminal, buscar "Smart PinPad DEV" y presionar OBTENER → INSTALAR [doc].
2. Instalar ÚNICAMENTE la app llamada DEV; la productiva cobra transacciones reales [doc].
3. Error 13 al instalar: indica que está instalada la "App de Pagos"; NetPay debe desinstalarla, no el
   integrador [doc].
4. Abrir la app, capturar Store ID y Password de prueba y presionar Iniciar (un solo login) [doc].
   Una terminal de pruebas muestra la leyenda "VERSIÓN DEMO" [doc].
5. Opcional, menú oculto (mantener presionada la esquina inferior derecha ~2-3 s): habilitar propina y
   menú de restaurante [doc]. MSI por defecto: 3, 6, 9, 12 y 18 [doc].
6. Versión de la Smart PinPad DEV a instalar: VALIDAR. La doc cita 1.3.7 y 1.3.10.2+, pero la línea
   v1.x.x perdió soporte el 30-dic-2025 y se exige v2.1.3 o posterior.

## 3. Dependencia del SDK [doc]
```gradle
// build.gradle (raíz): repositorio
maven { url 'https://jitpack.io' }
// build.gradle (módulo app)
implementation 'com.gitlab.netpaymx.netpay-sdks:smart-sdk:V1.1.9'
```
[campo] Si `settings.gradle` usa `FAIL_ON_PROJECT_REPOS`, el repositorio va en `dependencyResolutionManagement`.
En la prueba la dependencia bajó sin credenciales (VALIDAR). Con AndroidX: `android.enableJetifier=true` en el
`gradle.properties` DEL PROYECTO (el SDK arrastra `com.android.support` y falla el merge de
`appComponentFactory`). Release con minify: `-keep class mx.com.netpay.sdk.** { *; }` y
`-keepattributes Signature, *Annotation*` (VALIDAR). `targetSdk 35`: pantalla de borde a borde, manejar insets.

Imports: `mx.com.netpay.sdk.SmartApiFactory`, `mx.com.netpay.sdk.exceptions.SmartApiException`,
`mx.com.netpay.sdk.models.*`. Versión vigente a usar: VALIDAR (la doc cita 1.1.9; una página en inglés
cita 1.1.5). Credenciales de acceso al repositorio: la doc no las menciona (VALIDAR).

## 4. Inicializar [doc]
```kotlin
private val smartApi = SmartApiFactory.createSmartApi(this)   // Kotlin
// Java: SmartApiFactory.INSTANCE.createSmartApi(this)
```
No se documenta intent, action ni package de la Smart PinPad: el SDK lo resuelve por dentro. El
integrador pasa `appId` = package name de SU app.

**[campo] Obligatorio con `targetSdk >= 30`:** declarar `<queries>` con el `<provider>` (authority) y el
`<intent>` (acción/esquema) del SDK; sin eso `doTrans` lanza `IllegalArgumentException: Unknown URL
content://...` y la PinPad no abre. Por authority/acción, NUNCA por package. Valores: del AAR o del
mensaje de la excepción. Detalle y plantilla: lecciones-campo.md §visibilidad (VALIDAR con NetPay).

## 5. Primera venta [doc + skill]
Usar el adapter de [`examples/kotlin/`](../examples/kotlin/README.md) (folio persistido → `SaleRequest` con argumentos nombrados →
`doTrans` → `onActivityResult` → máquina de estados de transacciones.md §estado). Datos de prueba: solo
los de sandbox.md.

## 5b. Primer arranque en terminal
Seguir lecciones-campo.md §primer-arranque y, si algo falla, §diagnostico.

## 6. Cómo saber la versión
Versión de una app instalada: desde los ajustes de la terminal (hay un tutorial en la sección FAQs de la
doc). Versión del SDK: el campo `sdkVersion` de la respuesta y la dependencia de Gradle [doc].

## 7. Siguiente: certificación
Ver certificacion.md.

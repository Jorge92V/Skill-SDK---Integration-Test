# Preguntas abiertas

Lo que el skill no puede afirmar todavía. No se rellenó: cada punto está marcado VALIDAR o
`TODO(soporte NetPay)` en el archivo indicado. Al resolver uno, actualiza ese archivo y bórralo de aquí.

## Para NetPay (contenido del SDK)
| # | Pregunta | Dónde |
|---|---|---|
| 1 | ¿Qué valor lleva `storeId` en `CancelRequest` y `ReprintRequest`, y es opcional? | references/transacciones.md, references/lecciones-campo.md §firmas, examples/kotlin/NetpaySmartGateway.kt |
| 2 | ¿`<queries>` (provider + intent) es el requisito oficial para `targetSdk >= 30`? Falta en la doc. | references/lecciones-campo.md §visibilidad |
| 3 | ¿Reglas de minify (`-keep`) oficiales? ¿La dependencia de jitpack baja sin credenciales en todos los entornos? | references/quickstart.md, references/lecciones-campo.md §build |
| 4 | ¿El SDK tiene alguna selección explícita de ambiente, o lo decide siempre la Smart PinPad instalada? | SKILL.md S2, references/sandbox.md |
| 5 | Visto bueno a la política de reintento por estatus (columna "¿Habilita reintento?"). | references/transacciones.md §consulta-por-folio |
| 6 | Visto bueno a la lista de campos permitidos en logs. | references/seguridad.md §logging |
| 7 | Catálogo de `responseCode`/`code` y de excepciones del SDK. | references/errores.md |
| 8 | Cashback en el SDK: ¿parámetro de `SaleRequest`? (solo CoreX) | references/dispositivos.md, references/glosario.md |
| 9 | Devolución, cierre de lote e histórico por SDK: ¿existen? | references/dispositivos.md, references/transacciones.md §ventanas |
| 10 | Deduplicación por folio en NetpayPOS (hoy se asume igual que CoreX: no deduplica). | references/transacciones.md §folio |
| 11 | Formato permitido del folio (la reimpresión acepta hasta 250 caracteres). | references/transacciones.md §folio, examples/kotlin/FolioStore.kt |
| 12 | Tiempo que tarda el reverso y qué ve la app durante ese lapso; comportamiento con la app en segundo plano. | references/transacciones.md §reverso, §ciclo-de-vida |
| 13 | ¿`amount` incluye propina desde 1.1.8? ¿`tip` va aparte? (supuesto en el ejemplo, confirmar con T17) | references/transacciones.md §respuesta, references/lecciones-campo.md §propina-cancelacion |
| 14 | Versión vigente de la Smart PinPad DEV y del SDK (la doc cita 1.1.9 y 1.1.5; PinPad 1.3.x vs 2.1.3+). | references/quickstart.md |
| 15 | Matriz modelo × versión del SDK y comportamiento de Urovo. | references/dispositivos.md |
| 16 | Matriz de certificación del SDK (las reglas de resultado del sandbox salen de la matriz de API). | references/sandbox.md, references/casos-de-prueba.md |
| 17 | Cómo obtiene un integrador externo terminal de pruebas y tarjetas de prueba físicas; cómo activar el modo debug. | references/quickstart.md, references/sandbox.md |
| 18 | Guion de entorno para T10 y T11; regla que provoca T04 (promoción no elegible). | references/casos-de-prueba.md |
| 19 | Plazos MSI por emisor/afiliación. | references/glosario.md |
| 20 | Qué contienen los logs de "Enviar Logs" de la Smart PinPad. | references/seguridad.md |
| 21 | Cambio de ambiente en el go-live para SDK (qué cambia en la app del integrador, si algo). | references/certificacion.md |

## Inconsistencias internas detectadas (no se corrigieron para no cambiar contenido)
- `references/transacciones.md` §folio y `references/glosario.md` marcan VALIDAR los nombres `folio` / `folioId` /
  `folioNumber`, pero `references/lecciones-campo.md` §firmas los verificó en el AAR 1.1.9. Por la regla de
  precedencia, gana [campo]; falta limpiar el VALIDAR.
- `references/transacciones.md` §respuesta marca VALIDAR el callback (`onActivityResult` vs `onActivityCallResult`);
  lecciones-campo.md §ciclo confirma `onActivityResult` vía `startActivityForResult`.
- Los `.kt` de `examples/kotlin/` dicen "SIN COMPILAR" en la cabecera y el README dice "firmas verificadas en el AAR":
  ambas cosas son ciertas (firmas verificadas, ejemplo no compilado como proyecto), pero conviene compilarlo y
  quitar la ambigüedad.

## Portabilidad entre agentes
| # | Pregunta | Estado |
|---|---|---|
| P1 | ¿Gemini app/web acepta el ZIP con la carpeta `netpay-sdk/` arriba, o espera `SKILL.md` en la raíz del ZIP? La ayuda dice "SKILL.md en su carpeta principal". | Sin probar |
| P2 | ChatGPT: la ayuda permite "Upload from your computer" pero no dice el formato (ZIP/carpeta/SKILL.md) ni límites. Solo planes Business, Enterprise, Healthcare y Edu. | Sin probar |
| P3 | Cursor y Copilot (VS Code) leen también `~/.claude/skills`: si el skill está en `~/.agents/skills` y en `~/.claude/skills`, ¿aparece duplicado? | Sin probar |
| P4 | Antigravity CLI reemplazó a Gemini CLI para el tier gratuito y Google One (18-jun-2026). ¿Lee los mismos directorios de skills? | Sin documentar |
| P5 | Copilot cloud agent: la doc de Copilot CLI no confirma si el agente en la nube carga skills igual. | Sin verificar |
| P6 | Claude Code: la doc no confirma si sigue enlaces simbólicos en `~/.claude/skills` (Codex sí lo confirma). `install.sh` copia por defecto. | Sin verificar |
| P7 | Los comandos no interactivos de `evals/run_evals.py` (`claude -p`, `codex exec`, `gemini -p`, `copilot -p`, `cursor-agent -p`) no se probaron en este repo. | Sin probar |

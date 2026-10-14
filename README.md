# netpay-sdk — skill multiagente para integrar el Smart SDK de NetPay

Skill para que un agente de IA guíe a un integrador a cobrar con el Smart SDK Android de NetPay desde su
propia app en terminales PAX, Kozen y Urovo, hablando con NetpayPOS o CoreX/SmartCore: venta, MSI,
cancelación, consulta por folio, resultado desconocido, reversos, errores, pruebas con la app dev y
certificación. Sigue el formato abierto [Agent Skills](https://agentskills.io/specification)
(`SKILL.md` + `references/` + `examples/`), así que el mismo directorio sirve en varios agentes.

```
.
├── netpay-sdk/                 ← el skill (esta carpeta es lo que se instala)
│   ├── SKILL.md                ← frontmatter name + description; flujo y reglas
│   ├── references/             ← detalle que el agente lee bajo demanda
│   └── examples/kotlin/        ← adapter, folio, estados, cancelación, fake
├── AGENTS.md                   ← fallback para agentes sin soporte de skills
├── install.sh                  ← instalación por agente / ZIP para apps web
├── evals/                      ← 12 prompts con criterios + runner + regresión
├── tools/validate_skill.py     ← validador (también en CI)
├── OPEN_QUESTIONS.md           ← lo que falta confirmar (no se rellenó)
└── docs/MANTENIMIENTO.md
```

## Instalación rápida
```bash
git clone <este repo> && cd <repo>
./install.sh --dry-run     # muestra qué haría
./install.sh               # detecta agentes e instala una copia
./install.sh --link        # o enlace simbólico (se actualiza con git pull)
./install.sh --project ~/mi-app-pos   # dentro del repo de la app del integrador (.agents/ y .claude/)
./install.sh --zip         # dist/netpay-sdk.zip para Gemini app/web y ChatGPT
```
`install.sh` es idempotente: si el destino ya es igual no hace nada, y si existe algo distinto no lo
sobrescribe salvo con `--force` (deja el respaldo en `~/.netpay-sdk-backups/`, fuera de los directorios de skills para
que ningún agente lo cargue como duplicado).

## Instalación por agente
Rutas verificadas en la documentación oficial el 2026-10-13 (fuentes al final).

| Agente | Usuario (todos tus proyectos) | Proyecto (en el repo) | Notas |
|---|---|---|---|
| Claude Code | `~/.claude/skills/netpay-sdk/` | `.claude/skills/netpay-sdk/` | Frontmatter solo necesita `description`; acepta `name`. [1] |
| Codex CLI (y app de escritorio de ChatGPT) | `~/.agents/skills/netpay-sdk/` | `.agents/skills/netpay-sdk/` | Escanea desde el directorio actual hasta la raíz del repo; sigue symlinks. Uso explícito con `$netpay-sdk` o `/skills`. [2] |
| Gemini CLI | `~/.gemini/skills/` o `~/.agents/skills/` | `.gemini/skills/` o `.agents/skills/` | Pide confirmación al activar. También `gemini skills install <repo-git> --path netpay-sdk`. [3][4] |
| Cursor | `~/.cursor/skills/` o `~/.agents/skills/` | `.cursor/skills/` o `.agents/skills/` | También lee `.claude/skills` y `.codex/skills` por compatibilidad. [5] |
| GitHub Copilot (CLI y VS Code) | `~/.copilot/skills/` o `~/.agents/skills/` | `.github/skills/`, `.agents/skills/` o `.claude/skills/` | En VS Code un `name` inválido hace que el skill no cargue sin aviso. [6][7] |
| Gemini app/web | — | — | Manual: gemini.google.com → menú → Customize → Skills → Create → Upload → elige `dist/netpay-sdk.zip` (o la carpeta `netpay-sdk/`). Solo web; máx. 100 MB; sin scripts con internet; cuenta personal (no trabajo/escuela por ahora); despliegue gradual desde el 13-oct-2026. [8][9] |
| ChatGPT | — | — | Manual: Skills → Create → "Upload from your computer". Solo Business, Enterprise, Healthcare y Edu, según la configuración del workspace; el skill se escanea antes de activarse. La ayuda no especifica el formato del archivo: prueba con `dist/netpay-sdk.zip`. [10] |

`~/.agents/skills/` es el directorio común: lo leen Codex, Gemini CLI, Cursor y Copilot. Por eso
`install.sh` instala ahí una sola copia y otra en `~/.claude/skills/` solo si detecta Claude Code.

## Estado por plataforma
"Soportada" significa que pasó los 12 casos de `evals/prompts.json` (ver criterio en ese archivo). Hasta
hoy **ninguna plataforma se ha probado con el eval**; lo verificado es que la documentación oficial de
cada una carga este formato.

| Plataforma | Carga nativa de SKILL.md (verificado en docs) | Probada con eval | Estado |
|---|---|---|---|
| Claude Code | Sí [1] | No | Best effort |
| Codex CLI | Sí [2] | No | Best effort |
| Gemini CLI | Sí [3] | No | Best effort |
| Cursor | Sí [5] | No | Best effort |
| GitHub Copilot CLI / VS Code | Sí [6][7] | No | Best effort |
| Gemini app/web | Sí, por upload [8] | No | Best effort |
| ChatGPT | Upload documentado, formato no especificado [10] | No | Best effort |
| Otros agentes | Vía `AGENTS.md` (pide leer `netpay-sdk/SKILL.md`) | No | Best effort |

Para promover una plataforma a "Soportada": corre `evals/run_evals.py` (ver `evals/README.md`), guarda el
`RESUMEN.md` y actualiza esta tabla.

## Limitaciones conocidas
- **Ejecución de comandos**: solo los agentes con shell (Claude Code, Codex CLI, Gemini CLI, Cursor, Copilot)
  pueden correr `adb`/Gradle. Gemini app/web y ChatGPT trabajan en modo "sin shell" o "solo chat": el skill
  genera los comandos y pide pegar la salida (SKILL.md §1).
- **Gemini app/web**: los skills no se sincronizan entre la app de Gemini y Workspace; Gems pasa a skills
  durante 2026–2027 [9]. La subida de skills no está disponible para cuentas de trabajo o escuela por ahora [8].
- **Gemini CLI**: fue reemplazado por Antigravity CLI para el tier gratuito y Google One el 18-jun-2026 [3];
  no está documentado si Antigravity lee los mismos directorios.
- **Duplicados**: Cursor y Copilot leen `~/.agents/skills` y `~/.claude/skills`; si instalas en ambos, el
  skill podría aparecer dos veces (sin probar). Usa `./install.sh --targets agents` si no usas Claude Code.
- **Campos específicos**: el frontmatter lleva solo `name` y `description` a propósito (portabilidad;
  Claude.ai rechaza campos fuera de la especificación [1]). No hay `allowed-tools`, `paths` ni
  `disable-model-invocation`.
- **Sin scripts en el skill**: el skill no depende de ejecutar nada; los scripts del repo (`install.sh`,
  `tools/`, `evals/`) son para mantenedores y usan solo stdlib/bash.

## Validación y CI
`python3 tools/validate_skill.py` revisa: frontmatter con solo `name`/`description`, `name` con formato
válido (minúsculas, números y guiones, ≤ 64) e igual a la carpeta, `description` ≤ 1024, `SKILL.md` < 500
líneas, links relativos existentes y ausencia de números de tarjeta o llaves privadas. Corre en cada push
(`.github/workflows/validate.yml`).

## Fuentes
1. Claude Code — Skills: https://code.claude.com/docs/en/skills
2. OpenAI — Build skills (Codex): https://learn.chatgpt.com/docs/build-skills (redirigido desde developers.openai.com/codex/skills)
3. Gemini CLI — Agent Skills: https://geminicli.com/docs/cli/skills/
4. Gemini CLI — Creating Agent Skills: https://geminicli.com/docs/cli/creating-skills/
5. Cursor — Agent Skills: https://cursor.com/docs/skills
6. GitHub Docs — Create skills (Copilot CLI): https://docs.github.com/en/copilot/how-tos/copilot-cli/customize-copilot/create-skills
7. VS Code — Agent Skills: https://code.visualstudio.com/docs/copilot/customization/agent-skills
8. Gemini Apps Help — Create & manage skills: https://support.google.com/gemini/answer/17094296
9. Google Workspace Updates — Skills in the Gemini app (sep-2026): https://workspaceupdates.googleblog.com/2026/09/skills-gemini-app-workspace.html
10. OpenAI Help — Skills in ChatGPT: https://help.openai.com/en/articles/20001066-skills-in-chatgpt
11. Especificación Agent Skills: https://agentskills.io/specification

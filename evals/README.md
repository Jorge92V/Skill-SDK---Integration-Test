# Evals

| Archivo | Qué es |
|---|---|
| `prompts.json` | 12 prompts reales de integradores con criterios verificables (`debe` / `no_debe`). Es el set que decide si una plataforma pasa a "Soportada". |
| `run_evals.py` | Corre los prompts contra el CLI de un agente y deja las respuestas en `runs/` para revisión humana. Solo stdlib. |
| `regresion-v0.6.json` | 67 casos de regresión de las versiones anteriores del skill (activación, funcionales, anti-alucinación, seguridad). Se conservan sin cambios de contenido. |

## Cómo correrlo
1. Instala el skill para el agente a probar (`./install.sh` o `./install.sh --project <dir>`).
2. Corre los prompts desde un directorio donde el agente vea el skill:
   ```
   python3 evals/run_evals.py --agent codex --cmd 'codex exec {prompt}' --cwd <dir>
   ```
   Los comandos de ejemplo en la cabecera de `run_evals.py` no se probaron en este repo: confirma la opción
   no interactiva de tu CLI con `--help`.
3. Plataformas sin CLI (Gemini app/web, ChatGPT): `--manual` genera un archivo por caso; pega cada prompt
   en una conversación nueva con el skill instalado y pega la respuesta en el archivo.
4. Revisa cada `E**.md`: el caso pasa solo si cumple todo `debe` y nada de `no_debe`. Anota el veredicto
   en `RESUMEN.md`.
5. Corre también sin el skill (baseline) para ver qué aporta.

`runs/` no se versiona por defecto (puede contener respuestas largas); agrega al repo solo los
`RESUMEN.md` que respalden un cambio de estado de una plataforma.

#!/usr/bin/env python3
"""Corre los prompts de evals/prompts.json contra un agente y deja las respuestas para revisión humana.
Solo stdlib (Python 3.8+). No llama a ninguna red por sí mismo: ejecuta el comando del agente que le pases.

Ejemplos (ajusta el comando a tu CLI; revisa su --help):
  python3 evals/run_evals.py --agent claude  --cmd 'claude -p {prompt}'
  python3 evals/run_evals.py --agent codex   --cmd 'codex exec {prompt}'
  python3 evals/run_evals.py --agent gemini  --cmd 'gemini -p {prompt}'
  python3 evals/run_evals.py --agent copilot --cmd 'copilot -p {prompt}'
  python3 evals/run_evals.py --agent cursor  --cmd 'cursor-agent -p {prompt}'
  python3 evals/run_evals.py --agent chatgpt --manual     # plataformas de solo chat: genera plantillas

{prompt} se sustituye como UN argumento. Si el comando no lo contiene, el prompt se manda por stdin.
Salida: evals/runs/<agente>-<fecha>/  con un .md por caso (prompt, respuesta, checklist) y RESUMEN.md.
"""
import argparse
import datetime
import json
import os
import shlex
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def checklist(case):
    lines = ["### Debe (todos)"] + ["- [ ] " + d for d in case["debe"]]
    lines += ["", "### No debe (ninguno)"] + ["- [ ] no: " + n for n in case["no_debe"]]
    lines += ["", "**Veredicto:** PASA / FALLA  ", "**Notas:**", ""]
    return "\n".join(lines)


def run_case(cmd, prompt, cwd, timeout):
    if "{prompt}" in cmd:
        args = [a.replace("{prompt}", prompt) for a in shlex.split(cmd)]
        stdin = None
    else:
        args, stdin = shlex.split(cmd), prompt
    try:
        p = subprocess.run(args, input=stdin, capture_output=True, text=True, cwd=cwd, timeout=timeout)
        out = p.stdout.strip()
        if p.returncode != 0:
            out += "\n\n[código de salida %d]\n%s" % (p.returncode, p.stderr.strip()[-2000:])
        return out
    except FileNotFoundError:
        return "[ERROR] No se encontró el comando: %s" % args[0]
    except subprocess.TimeoutExpired:
        return "[ERROR] Tiempo agotado (%ss)" % timeout


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--agent", required=True, help="nombre corto de la plataforma (para la carpeta de salida)")
    ap.add_argument("--cmd", help="comando del agente con {prompt}")
    ap.add_argument("--manual", action="store_true", help="solo genera plantillas para pegar respuestas a mano")
    ap.add_argument("--cwd", default=".", help="directorio donde corre el agente (donde el skill esté instalado)")
    ap.add_argument("--only", help="IDs separados por coma, p. ej. E01,E05")
    ap.add_argument("--timeout", type=int, default=600)
    ap.add_argument("--out", default=os.path.join(HERE, "runs"))
    a = ap.parse_args()
    if not a.manual and not a.cmd:
        ap.error("usa --cmd '<comando> {prompt}' o --manual")

    with open(os.path.join(HERE, "prompts.json"), encoding="utf-8") as fh:
        cases = json.load(fh)["casos"]
    if a.only:
        wanted = set(a.only.split(","))
        cases = [c for c in cases if c["id"] in wanted]

    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    out_dir = os.path.join(a.out, "%s-%s" % (a.agent, stamp))
    os.makedirs(out_dir, exist_ok=True)
    rows = []
    for c in cases:
        print("%s %s ..." % (c["id"], c["tema"]), flush=True)
        if a.manual:
            answer = "<!-- Pega aquí la respuesta completa de la plataforma -->"
        else:
            answer = run_case(a.cmd, c["prompt"], a.cwd, a.timeout)
        body = "# %s — %s\n\n## Prompt\n\n%s\n\n## Respuesta (%s)\n\n%s\n\n## Revisión\n\n%s" % (
            c["id"], c["tema"], c["prompt"], a.agent, answer, checklist(c))
        with open(os.path.join(out_dir, c["id"] + ".md"), "w", encoding="utf-8") as fh:
            fh.write(body)
        rows.append("| %s | %s | | |" % (c["id"], c["tema"]))

    summary = ["# Resumen — %s (%s)" % (a.agent, stamp), "",
               "Comando: `%s`" % ("manual" if a.manual else a.cmd), "",
               "| Caso | Tema | Veredicto | Notas |", "|---|---|---|---|"] + rows
    with open(os.path.join(out_dir, "RESUMEN.md"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(summary) + "\n")
    print("Listo: %s" % out_dir)
    return 0


if __name__ == "__main__":
    sys.exit(main())

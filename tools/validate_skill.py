#!/usr/bin/env python3
"""Valida los skills del repo. Solo stdlib (Python 3.8+). Uso: python3 tools/validate_skill.py [raiz]

Chequeos (fallan el CI):
  1. Cada carpeta con SKILL.md tiene frontmatter YAML con SOLO `name` y `description`.
  2. `name`: 1-64 chars, [a-z0-9] y guiones, sin guion al inicio/fin ni dobles, igual al nombre de la carpeta.
  3. `description`: 1-1024 chars.
  4. SKILL.md < 500 líneas (recomendación de la especificación Agent Skills).
  5. Todos los links relativos de los .md del repo apuntan a archivos existentes.
  6. Sin datos sensibles evidentes: números tipo tarjeta (Luhn válido, 13-19 dígitos) ni llaves privadas.
Advertencias (no fallan): nombres de herramientas propias de un agente dentro del skill.
"""
import os
import re
import sys

NAME_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
LINK_RE = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
CARD_RE = re.compile(r"(?<![\d.])(?:\d[ -]?){13,19}(?![\d.])")
AGENT_WORDS = re.compile(r"\b(Claude|Codex|Gemini|Copilot|Cursor|ChatGPT|Bash tool|Read tool|Write tool)\b")
SKIP_DIRS = {".git", "node_modules", "dist", "runs"}


def luhn(digits):
    total, alt = 0, False
    for ch in reversed(digits):
        d = int(ch)
        if alt:
            d *= 2
            if d > 9:
                d -= 9
        total += d
        alt = not alt
    return total % 10 == 0


def parse_frontmatter(text):
    if not text.startswith("---\n"):
        return None, "falta frontmatter (--- en la primera línea)"
    end = text.find("\n---", 4)
    if end < 0:
        return None, "frontmatter sin cierre ---"
    fields = {}
    for line in text[4:end].splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if line[0].isspace():
            return None, "frontmatter con valores multilínea o anidados: usa una línea por campo"
        key, sep, value = line.partition(":")
        if not sep:
            return None, "línea inválida en frontmatter: %r" % line
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        fields[key.strip()] = value
    return fields, None


def walk(root):
    for d, dirs, files in os.walk(root):
        dirs[:] = [x for x in dirs if x not in SKIP_DIRS]
        for f in files:
            yield os.path.join(d, f)


def main():
    root = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else ".")
    errors, warnings = [], []
    skills = [p for p in walk(root) if os.path.basename(p) == "SKILL.md"]
    if not skills:
        errors.append("no se encontró ningún SKILL.md")

    for path in skills:
        rel = os.path.relpath(path, root)
        folder = os.path.basename(os.path.dirname(path))
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        fm, err = parse_frontmatter(text)
        if err:
            errors.append("%s: %s" % (rel, err))
            continue
        extra = set(fm) - {"name", "description"}
        if extra:
            errors.append("%s: campos no portables en frontmatter: %s" % (rel, ", ".join(sorted(extra))))
        name, desc = fm.get("name", ""), fm.get("description", "")
        if not (1 <= len(name) <= 64 and NAME_RE.match(name)):
            errors.append("%s: name inválido %r (minúsculas, números y guiones; 1-64)" % (rel, name))
        if name != folder:
            errors.append("%s: name %r no coincide con la carpeta %r" % (rel, name, folder))
        if not 1 <= len(desc) <= 1024:
            errors.append("%s: description debe tener 1-1024 caracteres (tiene %d)" % (rel, len(desc)))
        lines = text.count("\n") + 1
        if lines >= 500:
            errors.append("%s: %d líneas (máximo recomendado 500)" % (rel, lines))
        skill_dir = os.path.dirname(path)
        for p in walk(skill_dir):
            if p.endswith(".md"):
                with open(p, encoding="utf-8") as fh:
                    for i, line in enumerate(fh, 1):
                        m = AGENT_WORDS.search(line)
                        if m:
                            warnings.append("%s:%d menciona %r (el skill debe ser neutral)" % (os.path.relpath(p, root), i, m.group(1)))

    for p in walk(root):
        if not p.endswith((".md", ".kt", ".json", ".sh", ".py", ".yml", ".txt")):
            continue
        rel = os.path.relpath(p, root)
        with open(p, encoding="utf-8", errors="replace") as fh:
            content = fh.read()
        if p.endswith(".md"):
            for m in LINK_RE.finditer(content):
                target = m.group(1)
                if re.match(r"^[a-z][a-z0-9+.-]*:", target) or target.startswith("#"):
                    continue
                target = target.split("#", 1)[0]
                if target and not os.path.exists(os.path.normpath(os.path.join(os.path.dirname(p), target))):
                    errors.append("%s: link roto -> %s" % (rel, m.group(1)))
        for m in CARD_RE.finditer(content):
            digits = re.sub(r"\D", "", m.group(0))
            if 13 <= len(digits) <= 19 and luhn(digits) and len(set(digits)) > 1:
                errors.append("%s: posible número de tarjeta (%s…)" % (rel, digits[:4]))
        if ("PRIVATE " + "KEY-----") in content:
            errors.append("%s: contiene una llave privada" % rel)

    for w in warnings:
        print("AVISO:", w)
    for e in errors:
        print("ERROR:", e)
    print("%d skill(s), %d error(es), %d aviso(s)" % (len(skills), len(errors), len(warnings)))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())

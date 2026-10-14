#!/usr/bin/env bash
# Instala el skill netpay-sdk en los directorios de skills de los agentes detectados.
# Directorios verificados en la documentación oficial (fuentes en README.md):
#   Claude Code ............ ~/.claude/skills/                 (proyecto: .claude/skills/)
#   Codex CLI .............. ~/.agents/skills/                 (proyecto: .agents/skills/)
#   Gemini CLI ............. ~/.gemini/skills/ o ~/.agents/skills/ (proyecto: .gemini/ o .agents/skills/)
#   Cursor ................. ~/.cursor/skills/ o ~/.agents/skills/ (también lee ~/.claude/skills/)
#   GitHub Copilot (CLI/VS Code) ~/.copilot/skills/ o ~/.agents/skills/ (también lee ~/.claude/skills/)
# Por defecto instala UNA copia en ~/.agents/skills/ (la leen Codex, Gemini CLI, Cursor y Copilot)
# y otra en ~/.claude/skills/ solo si detecta Claude Code.
#
# Uso:
#   ./install.sh                 # detecta agentes e instala (copia)
#   ./install.sh --link          # enlace simbólico en vez de copia (se actualiza con git pull)
#   ./install.sh --project DIR   # instala en DIR/.agents/skills y DIR/.claude/skills (para el repo del integrador)
#   ./install.sh --targets agents,claude,gemini,cursor,copilot   # elige destinos explícitos
#   ./install.sh --force         # reemplaza una instalación distinta (respaldo en ~/.netpay-sdk-backups/)
#   ./install.sh --dry-run       # muestra qué haría sin tocar nada
#   ./install.sh --zip           # genera dist/netpay-sdk.zip para subir a Gemini app/web o ChatGPT
# Idempotente: si el destino ya es igual (o ya enlaza aquí), no hace nada.
set -euo pipefail

SKILL_NAME="netpay-sdk"
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$REPO_DIR/$SKILL_NAME"
MODE="copy"; FORCE=0; DRY=0; PROJECT=""; TARGETS=""; ZIP=0

while [ $# -gt 0 ]; do
  case "$1" in
    --link) MODE="link" ;;
    --copy) MODE="copy" ;;
    --force) FORCE=1 ;;
    --dry-run) DRY=1 ;;
    --zip) ZIP=1 ;;
    --project) PROJECT="${2:?falta DIR}"; shift ;;
    --targets) TARGETS="${2:?falta lista}"; shift ;;
    -h|--help) sed -n '2,22p' "$0"; exit 0 ;;
    *) echo "Opción desconocida: $1" >&2; exit 2 ;;
  esac
  shift
done

[ -f "$SRC/SKILL.md" ] || { echo "No encuentro $SRC/SKILL.md" >&2; exit 1; }

run() { if [ "$DRY" = 1 ]; then echo "  [dry-run] $*"; else "$@"; fi; }

make_zip() {
  local out="$REPO_DIR/dist/$SKILL_NAME.zip"
  run mkdir -p "$REPO_DIR/dist"
  if [ "$DRY" = 1 ]; then echo "  [dry-run] zip -> $out"; return; fi
  rm -f "$out"
  # zip estándar si existe; si no, Python stdlib. Excluye archivos ocultos (.DS_Store, etc.).
  if command -v zip >/dev/null 2>&1; then
    (cd "$REPO_DIR" && zip -qr "$out" "$SKILL_NAME" -x '*/.*')
  else
    python3 - "$REPO_DIR" "$SKILL_NAME" "$out" <<'PY'
import os, sys, zipfile
root, name, out = sys.argv[1:4]
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for d, dirs, files in os.walk(os.path.join(root, name)):
        dirs[:] = [x for x in dirs if not x.startswith(".")]
        for f in files:
            if f.startswith("."): continue
            p = os.path.join(d, f); z.write(p, os.path.relpath(p, root))
PY
  fi
  echo "ZIP listo: $out (contiene $SKILL_NAME/SKILL.md)"
}

if [ "$ZIP" = 1 ]; then make_zip; exit 0; fi

same_install() {  # 0 si el destino ya es esta versión
  local dest="$1"
  if [ -L "$dest" ]; then [ "$(cd "$dest" && pwd -P)" = "$(cd "$SRC" && pwd -P)" ]; return; fi
  [ -d "$dest" ] && diff -rq "$SRC" "$dest" >/dev/null 2>&1
}

install_to() {  # $1 = directorio de skills, $2 = etiqueta
  local base="$1" label="$2" dest="$1/$SKILL_NAME"
  if [ -e "$dest" ] || [ -L "$dest" ]; then
    if same_install "$dest"; then echo "= $label: ya instalado en $dest"; return; fi
    if [ "$FORCE" != 1 ]; then
      echo "! $label: $dest existe y es distinto. No lo toco (usa --force para reemplazar con respaldo)."; return
    fi
    # El respaldo va FUERA del directorio de skills para que ningún agente lo cargue como skill duplicado.
    local bakdir="$HOME/.netpay-sdk-backups" bak
    bak="$bakdir/$(echo "$base" | tr '/' '_')-$(date +%Y%m%d%H%M%S)"
    echo "~ $label: respaldo $dest -> $bak"; run mkdir -p "$bakdir"; run mv "$dest" "$bak"
  fi
  run mkdir -p "$base"
  if [ "$MODE" = link ]; then run ln -s "$SRC" "$dest"; else run cp -R "$SRC" "$dest"; fi
  echo "+ $label: instalado ($MODE) en $dest"
}

has() { command -v "$1" >/dev/null 2>&1; }

if [ -n "$PROJECT" ]; then
  [ -d "$PROJECT" ] || { echo "No existe $PROJECT" >&2; exit 1; }
  install_to "$PROJECT/.agents/skills" "proyecto (.agents: Codex, Gemini CLI, Cursor, Copilot)"
  install_to "$PROJECT/.claude/skills" "proyecto (.claude: Claude Code)"
  exit 0
fi

if [ -z "$TARGETS" ]; then
  detected=""
  { has codex || [ -d "$HOME/.codex" ]; } && detected="$detected codex"
  { has gemini || [ -d "$HOME/.gemini" ]; } && detected="$detected gemini"
  { has cursor-agent || has cursor || [ -d "$HOME/.cursor" ]; } && detected="$detected cursor"
  { has copilot || [ -d "$HOME/.copilot" ] || has code; } && detected="$detected copilot"
  { has claude || [ -d "$HOME/.claude" ]; } && detected="$detected claude"
  echo "Agentes detectados:${detected:- ninguno}"
  [ -z "$detected" ] && { echo "Nada que instalar. Usa --targets o --zip."; exit 0; }
  TARGETS="agents"
  case "$detected" in *claude*) TARGETS="$TARGETS,claude" ;; esac
fi

IFS=',' read -r -a list <<<"$TARGETS"
for t in "${list[@]}"; do
  case "$t" in
    agents)  install_to "$HOME/.agents/skills"  "~/.agents (Codex, Gemini CLI, Cursor, Copilot)" ;;
    claude)  install_to "$HOME/.claude/skills"  "Claude Code" ;;
    gemini)  install_to "$HOME/.gemini/skills"  "Gemini CLI" ;;
    cursor)  install_to "$HOME/.cursor/skills"  "Cursor" ;;
    copilot) install_to "$HOME/.copilot/skills" "GitHub Copilot" ;;
    codex)   install_to "$HOME/.agents/skills"  "Codex CLI" ;;
    *) echo "Destino desconocido: $t" >&2 ;;
  esac
done

cat <<'EOF'
Listo. Notas:
- Cursor y Copilot leen también ~/.claude/skills: si instalaste en ~/.agents y ~/.claude, pueden listar el skill dos veces.
- Gemini app/web y ChatGPT no leen tu disco: usa ./install.sh --zip y sube el ZIP (ver README.md).
- Reinicia el agente si el skill no aparece.
EOF

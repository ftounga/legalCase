#!/usr/bin/env bash
# PostToolUse / Edit|Write — self-check grep des composants décisionnels frontend.
# Mémoire feedback_self_check_grep_pre_commit : éviter les régressions silencieuses du registre d'outils
# (vague 10 → 3 FAIL non détectés). Advisory uniquement (jamais bloquant) : injecte un rappel + un
# diff d'intégrité si le fichier touché est le registre/panel décisionnel.
set -euo pipefail
input="$(cat 2>/dev/null || true)"
command -v jq >/dev/null 2>&1 || exit 0
f="$(printf '%s' "$input" | jq -r '.tool_input.file_path // .tool_response.filePath // empty' 2>/dev/null || true)"
[ -z "$f" ] && exit 0

# Ne se déclenche que sur les fichiers du système décisionnel frontend.
case "$f" in
  *decisional-tools-panel*|*tool-registry*|*decision-tool*|*TOOL_REGISTRY*|*decisional*) : ;;
  *) exit 0 ;;
esac
[ -f "$f" ] || exit 0

ctx="🔎 Self-check décisionnel requis (mémoire feedback_self_check_grep_pre_commit) sur $(basename "$f") :
- Vérifier que chaque tool ajouté à TOOL_REGISTRY a son entrée dans KNOWN_FRONTEND_TOOL_IDS (test d'intégrité) et inversement (pas d'orphelin DB).
- Vérifier les handlers (click)/(change) non câblés et les imports orphelins.
- Si seed backend decision_tool_visibility_rules touché : cohérence layer/visibility (mémoire feedback_liquibase_insert_column_check)."

# Diff d'intégrité best-effort, BORNÉ au repo (jamais de grep récursif non borné — cf. incident
# /tmp/.. = / qui faisait timeout le hook). On localise le fichier KNOWN_FRONTEND_TOOL_IDS via git
# (rapide, indexé) et on compare les IDs du registre édité.
repo="$(git -C "$(dirname "$f")" rev-parse --show-toplevel 2>/dev/null || true)"
if [ -n "$repo" ]; then
  reg="$(grep -oiE "id:[[:space:]]*['\"][a-z0-9_-]+['\"]" "$f" 2>/dev/null | grep -oiE "['\"][a-z0-9_-]+['\"]" | tr -d "\"'" | sort -u || true)"
  known_file="$(cd "$repo" && git grep -l "KNOWN_FRONTEND_TOOL_IDS" -- '*.ts' 2>/dev/null | head -1 || true)"
  if [ -n "$reg" ] && [ -n "$known_file" ] && [ -f "$repo/$known_file" ]; then
    missing=""
    while IFS= read -r id; do
      [ -z "$id" ] && continue
      grep -q "$id" "$repo/$known_file" 2>/dev/null || missing="$missing $id"
    done <<< "$reg"
    [ -n "$missing" ] && ctx="$ctx
- ⚠️ IDs présents dans le registre mais ABSENTS de $known_file :$missing"
  fi
fi

jq -nc --arg c "$ctx" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:$c}}'
exit 0

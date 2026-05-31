#!/usr/bin/env bash
# PostToolUse / Edit|Write — rappel smoke E2E si une préoccupation transversale est touchée.
# CLAUDE.md (préoccupations transversales) : auth/Principal, workspace context, plans/limites,
# navigation/routing. Advisory : on n'exécute PAS les E2E (lents) automatiquement — on rappelle
# qu'ils sont obligatoires AVANT push (cd e2e && npm test). Jamais bloquant.
set -euo pipefail
input="$(cat 2>/dev/null || true)"
command -v jq >/dev/null 2>&1 || exit 0
f="$(printf '%s' "$input" | jq -r '.tool_input.file_path // .tool_response.filePath // empty' 2>/dev/null || true)"
[ -z "$f" ] && exit 0
# Ne jamais réagir sur l'outillage Claude lui-même ni les deps (ex. hooks/guard-*.sh contient "guard").
case "$f" in */.claude/*|*/node_modules/*|*/.git/*) exit 0 ;; esac

concern=""
case "$f" in
  *[Aa]uth*|*[Pp]rincipal*|*[Ss]ecurity*|*[Jj]wt*|*[Oo][Aa]uth*|*[Tt]oken*) concern="auth/Principal" ;;
  *[Ww]orkspace*|*[Tt]enant*) concern="workspace context / multi-tenant" ;;
  *[Pp]lan*[Ll]imit*|*[Qq]uota*|*[Gg]ate*|*[Ss]ubscription*) concern="plans / limites / quotas" ;;
  *[Rr]outing*|*[Rr]outes*|*[Gg]uard*|*[Nn]avigation*|*app.routes*|*-routing.module*) concern="navigation / routing / guard" ;;
  *) exit 0 ;;
esac

ctx="🚦 Préoccupation transversale touchée ($concern) sur $(basename "$f") — CLAUDE.md.
AVANT tout push : exécuter les smoke tests E2E → cd e2e && npm test.
Si échec E2E => BLOCAGE avant push (règle anti-régression). Lister explicitement les composants impactés dans la mini-spec."
jq -nc --arg c "$ctx" '{hookSpecificOutput:{hookEventName:"PostToolUse",additionalContext:$c}}'
exit 0

#!/usr/bin/env bash
# PreToolUse / Bash — garde-fou gouvernance AI LegalCase.
# - HARD BLOCK (jamais légitime) : écriture SQL directe dans les tables backlog_* (blocker §7,
#   la source de vérité = les MD, la DB est un cache de lecture — F-178).
# - AVERTISSEMENT injecté : gh pr merge (rappel vérifs pré-merge), git push --force sur master.
# Fail-open : toute erreur interne => exit 0 (ne bloque jamais le travail par accident).
set -euo pipefail
input="$(cat 2>/dev/null || true)"
command -v jq >/dev/null 2>&1 || exit 0
cmd="$(printf '%s' "$input" | jq -r '.tool_input.command // empty' 2>/dev/null || true)"
[ -z "$cmd" ] && exit 0

deny() {
  jq -nc --arg r "$1" '{hookSpecificOutput:{hookEventName:"PreToolUse",permissionDecision:"deny",permissionDecisionReason:$r}}'
  exit 0
}
warn() {
  jq -nc --arg c "$1" '{hookSpecificOutput:{hookEventName:"PreToolUse",additionalContext:$c}}'
  exit 0
}

# --- HARD BLOCK : mutation directe des tables backlog_* ---------------------------------------
# Détecte INSERT/UPDATE/DELETE/TRUNCATE/ALTER visant backlog_features|subfeatures|marketing_tasks
if printf '%s' "$cmd" | grep -qiE '(insert|update|delete|truncate|alter|drop)[^;]*\bbacklog_(features|subfeatures|marketing_tasks)\b'; then
  deny "REFUS gouvernance (blocker §7) : édition directe des tables backlog_* interdite. La source de vérité est docs/PRODUCT_SPEC.md / docs/MARKETING_BACKLOG.md (Option A, F-178). La DB est un cache resynchronisé par cron. Édite le MD source à la place."
fi

# --- AVERTISSEMENT : push --force sur master/main --------------------------------------------
# Force ET master DANS LE MÊME statement git push (pas séparés par ; | &&) — évite de matcher
# un "git worktree remove --force" + "git push <branche>" dans une commande composée.
if printf '%s' "$cmd" | grep -qiE 'git +push[^;|&]*(--force|-f\b)[^;|&]*(master|main)|git +push[^;|&]*(master|main)[^;|&]*(--force|-f\b)'; then
  warn "⚠️ push --force sur master/main détecté. Vérifie que ce n'est pas une récupération destructive (cf. mémoire feedback_commit_master_recovery : préférer git branch -f sur un SHA). Repo multi-session — master bouge."
fi

# --- AVERTISSEMENT : gh pr merge ------------------------------------------------------------
# Heuristique anti-faux-positif : ne pas avertir si "gh pr merge" apparaît DANS un message de
# commit / corps de PR (heredoc, -m, --body/--title, gh pr create) plutôt qu'en vraie commande.
if printf '%s' "$cmd" | grep -qiE 'gh +pr +merge' \
   && ! printf '%s' "$cmd" | grep -qiE '(pr +create|commit|[[:space:]]-m[[:space:]]|--body|--title|<<)'; then
  warn "Rappel pré-merge (mémoires feedback_pre_merge_endpoint_check + feedback_pre_merge_visibility_seed_check) : (1) si PR frontend, vérifier que les endpoints /api/v1/... consommés existent côté backend (master ou PR back ouverte) ; (2) si le backend INSERT/UPDATE decision_tool_visibility_rules, vérifier TOOL_REGISTRY frontend + KNOWN_FRONTEND_TOOL_IDS ; (3) gh pr checks <PR#> tous verts ; (4) review checklist affichée."
fi

exit 0

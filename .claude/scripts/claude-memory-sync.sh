#!/usr/bin/env bash
# Sync du dossier auto-memory Claude entre machines via S3.
# Raison : la dev workstation AWS est SSM-only (pas de SSH/rsync entrant) ; S3 est le canal
# (la box a un rôle AdministratorAccess via IMDS). La mémoire ne transite PAS par git.
#
# BIDIRECTIONNEL & SANS PERTE : on utilise `aws s3 sync` au niveau fichier (union, newest-wins),
# JAMAIS `--delete`. Ainsi push (local→S3) et pull (S3→local) ne suppriment jamais la mémoire
# de l'autre machine. ⚠️ Conséquence : une suppression de mémoire ne se propage pas automatiquement
# (un fichier supprimé d'un côté réapparaît au prochain sync). Pour supprimer pour de vrai :
#   aws s3 rm s3://$BUCKET/$PREFIX/<fichier>.md   (puis supprimer localement et sur la box)
#
# Usage :
#   claude-memory-sync.sh push   # local/box  → S3  (sauve la mémoire de CETTE machine)
#   claude-memory-sync.sh pull   # S3 → local/box   (absorbe la mémoire des autres machines)
#
# Env : CLAUDE_MEMORY_BUCKET (défaut documents prod), AWS_REGION (eu-west-3),
#       AWS_PROFILE (local: legalcase-terraform ; box: vide => rôle IMDS).
set -euo pipefail

BUCKET="${CLAUDE_MEMORY_BUCKET:-legalcase-production-documents-504895205419}"
PREFIX="_claude-tooling/claude-memory/legalCase"
REGION="${AWS_REGION:-eu-west-3}"
PROFILE_ARG=(); [ -n "${AWS_PROFILE:-}" ] && PROFILE_ARG=(--profile "$AWS_PROFILE")

# Chemin mémoire auto-calculé pour CETTE machine (slug = chemin repo, '/' -> '-').
# Local : ~francky/.claude/projects/-home-francky-dev-legalCase/memory
# Box   : ~ubuntu/.claude/projects/-home-ubuntu-dev-legalCase/memory
REPO="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SLUG="$(printf '%s' "$REPO" | sed 's#/#-#g')"
MEM="$HOME/.claude/projects/$SLUG/memory"
S3="s3://$BUCKET/$PREFIX/"

count() { find "$MEM" -maxdepth 1 -name '*.md' 2>/dev/null | wc -l | tr -d ' '; }

case "${1:-}" in
  push)
    [ -d "$MEM" ] || { echo "❌ pas de mémoire locale en $MEM"; exit 1; }
    aws s3 sync "$MEM" "$S3" --exclude '*' --include '*.md' --region "$REGION" "${PROFILE_ARG[@]}"
    echo "✅ push : $(count) fichiers → $S3"
    ;;
  pull)
    mkdir -p "$MEM"
    aws s3 sync "$S3" "$MEM" --exclude '*' --include '*.md' --region "$REGION" "${PROFILE_ARG[@]}"
    echo "✅ pull : $(count) fichiers ← $S3"
    ;;
  *)
    echo "Usage: $0 push|pull"; exit 2 ;;
esac

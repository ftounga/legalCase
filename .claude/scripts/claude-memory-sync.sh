#!/usr/bin/env bash
# Sync du dossier auto-memory Claude entre machines via S3.
# Raison : la dev workstation AWS est SSM-only (SG egress-only, pas de SSH/rsync entrant) ;
# S3 est le canal le plus simple (la box a un rôle AdministratorAccess via IMDS).
#
# La mémoire ne transite PAS par git (elle vit sous ~/.claude/projects/<slug>/memory/, hors repo).
# Or la skill autonomous-delivery-wave s'appuie dessus (rate-limits, worktrees, squash-merge, etc.).
#
# Usage :
#   .claude/scripts/claude-memory-sync.sh push   # depuis la machine SOURCE (ex. poste local)
#   .claude/scripts/claude-memory-sync.sh pull   # depuis la machine CIBLE (ex. dev workstation AWS)
#
# Paramètres (env, valeurs par défaut adaptées à l'infra LegalCase) :
#   CLAUDE_MEMORY_BUCKET (défaut: legalcase-production-documents-504895205419)
#   AWS_REGION           (défaut: eu-west-3)
#   AWS_PROFILE          (optionnel, ex: legalcase-terraform en local ; vide sur la box = rôle IMDS)
set -euo pipefail

BUCKET="${CLAUDE_MEMORY_BUCKET:-legalcase-production-documents-504895205419}"
KEY="_claude-tooling/claude-memory/legalCase-memory.tgz"
REGION="${AWS_REGION:-eu-west-3}"
PROFILE_ARG=(); [ -n "${AWS_PROFILE:-}" ] && PROFILE_ARG=(--profile "$AWS_PROFILE")

# Chemin mémoire auto-calculé pour CETTE machine (slug = chemin repo, '/' -> '-').
# Local : /home/francky/.claude/projects/-home-francky-dev-legalCase/memory
# Box   : /home/ubuntu/.claude/projects/-home-ubuntu-dev-legalCase/memory
REPO="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
SLUG="$(printf '%s' "$REPO" | sed 's#/#-#g')"
MEM="$HOME/.claude/projects/$SLUG/memory"

case "${1:-}" in
  push)
    [ -d "$MEM" ] || { echo "❌ Pas de mémoire locale en $MEM"; exit 1; }
    tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
    tar -czf "$tmp/mem.tgz" -C "$MEM" .
    aws s3 cp "$tmp/mem.tgz" "s3://$BUCKET/$KEY" --region "$REGION" "${PROFILE_ARG[@]}"
    echo "✅ push : $(find "$MEM" -maxdepth 1 -name '*.md' | wc -l) fichiers → s3://$BUCKET/$KEY"
    ;;
  pull)
    mkdir -p "$MEM"
    tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
    aws s3 cp "s3://$BUCKET/$KEY" "$tmp/mem.tgz" --region "$REGION" "${PROFILE_ARG[@]}"
    tar -xzf "$tmp/mem.tgz" -C "$MEM"
    echo "✅ pull : mémoire restaurée dans $MEM ($(find "$MEM" -maxdepth 1 -name '*.md' | wc -l) fichiers)"
    ;;
  *)
    echo "Usage: $0 push|pull"; exit 2 ;;
esac

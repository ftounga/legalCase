#!/usr/bin/env bash
# À EXÉCUTER SUR LA BOX (avec sudo/root). Installe un timer systemd qui pousse la mémoire Claude
# vers S3 toutes les 15 min. Couvre les arrêts NON pilotés par le wrapper legalcase :
# auto-stop EventBridge 22h Paris, arrêt manuel console, crash. Idempotent (réécrit + reenable).
# Installé automatiquement à chaque `legalcase start`.
set -euo pipefail

REPO_USER="${REPO_USER:-ubuntu}"
REPO_DIR="${REPO_DIR:-/home/$REPO_USER/dev/legalCase}"
SYNC="$REPO_DIR/.claude/scripts/claude-memory-sync.sh"

[ -f "$SYNC" ] || { echo "❌ script sync introuvable: $SYNC"; exit 1; }

cat >/etc/systemd/system/claude-memory-push.service <<EOF
[Unit]
Description=Push Claude memory to S3 (LegalCase)
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
User=$REPO_USER
Environment=HOME=/home/$REPO_USER
WorkingDirectory=$REPO_DIR
ExecStart=/bin/bash $SYNC push
EOF

cat >/etc/systemd/system/claude-memory-push.timer <<EOF
[Unit]
Description=Periodic Claude memory push to S3 (LegalCase)

[Timer]
OnBootSec=3min
OnUnitActiveSec=15min
Persistent=true

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now claude-memory-push.timer
echo "✅ timer claude-memory-push actif (push S3 toutes les 15 min, + au boot)"
systemctl list-timers claude-memory-push.timer --no-pager 2>/dev/null | head -3 || true

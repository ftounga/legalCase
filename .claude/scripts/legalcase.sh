#!/usr/bin/env bash
# Wrapper de pilotage de la dev workstation AWS LegalCase.
# « legalcase start » embarque la synchro mémoire : push local -> S3 -> pull sur la box.
#
#   legalcase start    # push mémoire locale + démarre la box + git pull + memory pull sur la box
#   legalcase stop     # arrête la box
#   legalcase status   # état de l'instance
#   legalcase ssh      # session SSM interactive (alias: connect)
#
# Synchro mémoire = ONE-WAY local -> box (cohérent avec « local = source de vérité »).
# Si la box accumule de la mémoire (ex. vague autonome), la pousser AVANT le prochain start
# (sinon le pull la recouvre). Voir CLAUDE_MEMORY_BUCKET dans claude-memory-sync.sh.
set -euo pipefail

INSTANCE_ID="${LEGALCASE_INSTANCE_ID:-i-0abfa3acd040534d0}"
REGION="${AWS_REGION:-eu-west-3}"
export AWS_PROFILE="${AWS_PROFILE:-legalcase-terraform}"   # profil valide local ; la box utilise l'IMDS
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AWS=(aws --region "$REGION")

wait_ssm() {
  echo "… attente agent SSM en ligne"
  for _ in $(seq 1 30); do
    [ "$("${AWS[@]}" ssm describe-instance-information \
        --filters "Key=InstanceIds,Values=$INSTANCE_ID" \
        --query 'InstanceInformationList[].PingStatus' --output text 2>/dev/null)" = "Online" ] \
      && { echo "✅ SSM Online"; return 0; }
    sleep 10
  done
  echo "❌ SSM pas en ligne après 5 min"; return 1
}

run_on_box() {  # $1 = commande shell (exécutée en tant qu'ubuntu)
  local params cid st
  params="$(jq -n --arg c "runuser -l ubuntu -c '$1'" '{commands:[$c]}')"
  cid="$("${AWS[@]}" ssm send-command --instance-ids "$INSTANCE_ID" \
        --document-name AWS-RunShellScript --parameters "$params" \
        --query 'Command.CommandId' --output text)"
  for _ in $(seq 1 30); do
    st="$("${AWS[@]}" ssm get-command-invocation --command-id "$cid" --instance-id "$INSTANCE_ID" \
         --query 'Status' --output text 2>/dev/null || true)"
    { [ "$st" = "Success" ] || [ "$st" = "Failed" ]; } && break; sleep 5
  done
  echo "--- sortie box ($st) ---"
  "${AWS[@]}" ssm get-command-invocation --command-id "$cid" --instance-id "$INSTANCE_ID" \
    --query 'StandardOutputContent' --output text
  [ "$st" = "Success" ]
}

case "${1:-}" in
  start)
    echo "▶ push de la mémoire locale vers S3"
    "$HERE/claude-memory-sync.sh" push || echo "⚠️ push mémoire échoué (on continue)"
    echo "▶ démarrage $INSTANCE_ID"
    "${AWS[@]}" ec2 start-instances --instance-ids "$INSTANCE_ID" \
      --query 'StartingInstances[].CurrentState.Name' --output text
    "${AWS[@]}" ec2 wait instance-running --instance-ids "$INSTANCE_ID"
    echo "✅ instance running"
    wait_ssm
    echo "▶ git pull + memory pull sur la box"
    run_on_box 'cd ~/dev/legalCase && (git pull --rebase origin master 2>&1 || echo GIT_PULL_ISSUE) && .claude/scripts/claude-memory-sync.sh pull 2>&1'
    echo "🎉 box prête et synchronisée. Pense à ouvrir /hooks dans ta session Claude sur la box."
    ;;
  stop)
    echo "■ arrêt $INSTANCE_ID"
    "${AWS[@]}" ec2 stop-instances --instance-ids "$INSTANCE_ID" \
      --query 'StoppingInstances[].CurrentState.Name' --output text
    ;;
  status)
    "${AWS[@]}" ec2 describe-instances --instance-ids "$INSTANCE_ID" \
      --query 'Reservations[].Instances[].{Id:InstanceId,State:State.Name,Type:InstanceType,Launch:LaunchTime}' \
      --output table
    ;;
  ssh|connect)
    exec "${AWS[@]}" ssm start-session --target "$INSTANCE_ID"
    ;;
  *)
    echo "Usage: legalcase start|stop|status|ssh"; exit 2 ;;
esac

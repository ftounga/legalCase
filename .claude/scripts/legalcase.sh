#!/usr/bin/env bash
# Wrapper de pilotage de la dev workstation AWS LegalCase, avec synchro mémoire BIDIRECTIONNELLE.
#
#   legalcase start        # pull S3->local, push local->S3, démarre la box, (box) git pull + memory pull
#                          #   + installe le timer d'auto-push (filet anti auto-stop 22h / crash)
#   legalcase stop         # (box) push final box->S3, arrête la box, puis pull S3->local
#   legalcase pull-memory  # local : absorbe la mémoire S3 (ex. après une vague autonome sur la box)
#   legalcase push-memory  # local : pousse la mémoire locale vers S3
#   legalcase status       # état de l'instance
#   legalcase ssh          # session SSM interactive (alias: connect)
#
# Mémoire = union sans perte (cf. claude-memory-sync.sh) : aucun sens n'efface l'autre.
set -euo pipefail

INSTANCE_ID="${LEGALCASE_INSTANCE_ID:-i-0abfa3acd040534d0}"
REGION="${AWS_REGION:-eu-west-3}"
export AWS_PROFILE="${AWS_PROFILE:-legalcase-terraform}"   # profil valide local ; la box utilise l'IMDS
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SYNC="$HERE/claude-memory-sync.sh"
AWS=(aws --region "$REGION")

state() { "${AWS[@]}" ec2 describe-instances --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[].Instances[].State.Name' --output text 2>/dev/null; }

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
  for _ in $(seq 1 36); do
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
    echo "▶ sync mémoire avant démarrage (pull S3->local puis push local->S3)"
    "$SYNC" pull || echo "⚠️ pull mémoire échoué (on continue)"
    "$SYNC" push || echo "⚠️ push mémoire échoué (on continue)"
    echo "▶ démarrage $INSTANCE_ID"
    "${AWS[@]}" ec2 start-instances --instance-ids "$INSTANCE_ID" \
      --query 'StartingInstances[].CurrentState.Name' --output text
    "${AWS[@]}" ec2 wait instance-running --instance-ids "$INSTANCE_ID"; echo "✅ instance running"
    wait_ssm
    echo "▶ box : git pull + memory pull + (ré)install du timer d'auto-push"
    run_on_box 'cd ~/dev/legalCase && (git pull --rebase origin master 2>&1 || echo GIT_PULL_ISSUE) && .claude/scripts/claude-memory-sync.sh pull 2>&1 && sudo bash .claude/scripts/claude-memory-autopush-install.sh 2>&1'
    echo "🎉 box prête et synchronisée. Ouvre /hooks dans ta session Claude sur la box."
    ;;
  stop)
    if [ "$(state)" = "running" ]; then
      echo "▶ box : push final de la mémoire vers S3 avant arrêt"
      run_on_box 'cd ~/dev/legalCase && .claude/scripts/claude-memory-sync.sh push 2>&1' || echo "⚠️ push box échoué (on continue l'arrêt)"
    fi
    echo "■ arrêt $INSTANCE_ID"
    "${AWS[@]}" ec2 stop-instances --instance-ids "$INSTANCE_ID" \
      --query 'StoppingInstances[].CurrentState.Name' --output text
    echo "▶ local : pull de la mémoire (absorbe ce que la box a accumulé)"
    "$SYNC" pull || echo "⚠️ pull local échoué"
    ;;
  pull-memory)
    "$SYNC" pull ;;
  push-memory)
    "$SYNC" push ;;
  status)
    "${AWS[@]}" ec2 describe-instances --instance-ids "$INSTANCE_ID" \
      --query 'Reservations[].Instances[].{Id:InstanceId,State:State.Name,Type:InstanceType,Launch:LaunchTime}' \
      --output table ;;
  ssh|connect)
    exec "${AWS[@]}" ssm start-session --target "$INSTANCE_ID" ;;
  *)
    echo "Usage: legalcase start|stop|pull-memory|push-memory|status|ssh"; exit 2 ;;
esac

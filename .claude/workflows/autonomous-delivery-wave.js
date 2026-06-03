export const meta = {
  name: 'autonomous-delivery-wave',
  description: "Livre une vague de N features du backlog PRODUCT_SPEC en équipe d'agents, gouvernance CLAUDE.md respectée, décider-par-défaut + flag, auto-merge --admin, plafond budget. NE CRÉE PAS de feature.",
  whenToUse: "Vague autonome nocturne : 'livre 10 features du backlog, récap au matin'. Suppose la skill ai-skills/autonomous-delivery-wave.md.",
  phases: [
    { title: 'Bootstrap', detail: "git fetch origin, détecter SF en cours non finies, lire mémoire" },
    { title: 'Audit+File', detail: "audit couverture FR/BE + file des features À faire" },
    { title: 'Classer', detail: "étiqueter chaque feature 🟢/🟠/🔴" },
    { title: 'Livrer', detail: "par feature : cadrage→spec→dev(back//front)→checklists→merge" },
    { title: 'Docs+Staging', detail: "docs groupées + 1 déploiement staging unique" },
    { title: 'Récap', detail: "récap unique d'arbitrages" },
  ],
}

// ─── args ────────────────────────────────────────────────────────────────────
// args = { waveSize?: number, dateISO?: string, features?: string[] }
// dateISO OBLIGATOIRE (Date.now() interdit dans les scripts Workflow) — passé au lancement.
const WAVE_SIZE = (args && args.waveSize) || 10
const DATE = (args && args.dateISO) || 'DATE-A-PASSER-EN-ARGS'
const EXPLICIT = (args && args.features) || null  // si fournie, court-circuite l'audit→file
// Concurrence agents : bornée par le rate-limit Anthropic (TPM/RPM du tier), PAS par le CPU.
// Workflow plafonne nativement à min(16, cores-2). On laisse Workflow gérer la file ;
// CONCURRENCY sert à brider volontairement plus bas si le tier sature (429). Défaut = cap natif.
const CONCURRENCY = (args && args.concurrency) || 6

// ─── schémas de sortie (validation au niveau tool-call, pas de parsing) ───────
const QUEUE_SCHEMA = {
  type: 'object',
  required: ['inProgress', 'queue'],
  properties: {
    inProgress: { type: 'array', items: { type: 'object', required: ['id', 'state', 'action'],
      properties: { id: {type:'string'}, state:{type:'string'}, action:{type:'string'} } } },
    coverageGaps: { type: 'array', items: { type: 'string' } },
    queue: { type: 'array', items: { type: 'object', required: ['id', 'title', 'risk', 'subfeatures'],
      properties: {
        id: { type: 'string' }, title: { type: 'string' },
        risk: { enum: ['green', 'orange', 'red'] },
        reason: { type: 'string' },
        parallelizable: { type: 'boolean' },
        subfeatures: { type: 'array', items: { type: 'string' } },
      } } },
  },
}
const DELIVERY_SCHEMA = {
  type: 'object',
  required: ['featureId', 'status'],
  properties: {
    featureId: { type: 'string' },
    status: { enum: ['merged', 'halted', 'failed'] },
    prs: { type: 'array', items: { type: 'string' } },
    arbitrages: { type: 'array', items: { type: 'object', required: ['decision', 'why', 'reversible'],
      properties: { gate:{type:'string'}, decision:{type:'string'}, why:{type:'string'},
        alternative:{type:'string'}, reversible:{type:'boolean'} } } },
    haltQuestion: { type: 'string' },
    residualRisks: { type: 'array', items: { type: 'string' } },
  },
}

// ─── Phase 1 : Bootstrap + Audit + File ──────────────────────────────────────
phase('Bootstrap')
log('Vague autonome — bootstrap état + audit couverture')

const plan = await agent(
  `Tu prépares une vague de livraison autonome de ${WAVE_SIZE} features pour AI LegalCase.
   Lis et applique ai-skills/autonomous-delivery-wave.md (Phases 0 et 1).
   1) git fetch origin ; raisonne sur origin/master. Détecte les sessions parallèles actives
      (git reflog -10 = pulls/rebases non émis par toi + présence de /tmp/parallel-session-untracked/).
   2) Détecte le travail en cours RÉELLEMENT non terminé. ⚠️ NE te fie PAS à 'git branch --no-merged' :
      le repo merge en SQUASH, donc les branches livrées apparaissent faussement non-mergées
      (incident 2026-05-30 : F-213/F-219 TERMINÉES vues comme 'à finir'). Source de vérité = statut
      PRODUCT_SPEC.md ('🎉 Terminée' = fini) + gh pr list --state open. Pour une branche suspecte,
      teste le CONTENU : git cherry origin/master feat/SF-X (vide = déjà dans master). -> inProgress.
   3) Lis MEMORY.md + feedbacks pertinents (rate-limit, worktrees, docs->redeploy, pré-merge checks).
   4) Audit de couverture FR ET BE des 3 domaines métier (règle mémoire). -> coverageGaps.
   5) ${EXPLICIT ? `File IMPOSÉE par le PO : ${JSON.stringify(EXPLICIT)}. Ordonne-la par dépendances.`
        : `Construis la file des ${WAVE_SIZE} features prioritaires statut "À faire" de PRODUCT_SPEC.md
           (jamais Backlog sans GO étape 0, jamais Bloqué). Ordonne par dépendances → valeur → effort.`}
   6) Classe CHAQUE feature 🟢 green / 🟠 orange / 🔴 red (cf. skill Phase 2) avec reason.
   NE CRÉE AUCUNE feature absente de PRODUCT_SPEC.md. Retourne le JSON QUEUE_SCHEMA.`,
  { label: 'bootstrap+audit+queue', phase: 'Audit+File', schema: QUEUE_SCHEMA, agentType: 'Explore' }
)

log(`File: ${plan.queue.map(f => `${f.id}(${f.risk})`).join(', ')}`)
if (plan.inProgress.length) log(`En cours à finir d'abord: ${plan.inProgress.map(p => p.id).join(', ')}`)

// On finit d'abord le travail en cours, puis on traite la file (verts/oranges livrés, rouges parkés).
const toFinish = plan.inProgress.map(p => ({ id: p.id, title: p.action, risk: 'green', resume: true, subfeatures: [] }))
const toDeliver = [...toFinish, ...plan.queue.filter(f => f.risk !== 'red')]
const parked = plan.queue.filter(f => f.risk === 'red')

phase('Classer')
log(`${toDeliver.length} à livrer, ${parked.length} parkées (🔴 HALT)`)

// ─── Phase 2 : Livraison — pipeline par feature, budget-borné ─────────────────
phase('Livrer')
const results = []
for (const f of toDeliver) {
  if (budget.total && budget.remaining() < 80_000) {
    log(`Budget restant ${Math.round(budget.remaining()/1000)}k < seuil — arrêt propre avant ${f.id}`)
    break
  }
  log(`▶ ${f.id} (${f.risk}) — reste ${budget.total ? Math.round(budget.remaining()/1000)+'k' : '∞'}`)

  const res = await agent(
    `Tu livres la feature ${f.id} ("${f.title}") de bout en bout en autonomie.
     ${f.resume ? 'REPRENDS le travail en cours non terminé de cette feature et FINIS-le.' : ''}
     Applique INTÉGRALEMENT ai-skills/autonomous-delivery-wave.md (Phase 3) + feature-autonome.md
     + parallel-frontback-delivery.md.
     Séquence CLAUDE.md par SF : cadrage cohérence (étape 0/0bis si impact écran) → mini-spec →
     readiness → dev (back//front si contrat figé, worktrees isolés, UUID Liquibase pré-assignés,
     self-check grep pré-commit) → compile+tests verts (+ smoke E2E si transversal) →
     review checklist → release checklist → gh pr create → gh pr merge --squash --delete-branch --admin
     (backend AVANT frontend ; vérifs pré-merge endpoints + visibility seed).
     RÈGLE GATE: sur un gate produit RÉVERSIBLE (🟠 cohérence écran, choix UX, nouvelle table simple),
     DÉCIDE par défaut, implémente, et TRACE l'arbitrage. Sur de l'IRRÉVERSIBLE/sécurité/coûteux
     (🔴) → status 'halted' + haltQuestion, ne livre pas en aveugle.
     INTERDIT: commit docs (couplage CI redeploy) — les docs sont groupées par l'orchestrateur en fin.
     INTERDIT: déploiement staging par feature.
     Si master-red post-merge non résolu en 2 tentatives/30min → revert + status 'failed'.
     Max ~2-3 agents claude concurrents (rate-limit). NE CRÉE AUCUNE feature.
     Retourne le JSON DELIVERY_SCHEMA.`,
    { label: `deliver:${f.id}`, phase: 'Livrer', schema: DELIVERY_SCHEMA, isolation: 'worktree' }
  )
  results.push(res)
  log(`${f.id} → ${res ? res.status : 'null'}`)
}

// ─── Phase 3 : Docs groupées + staging unique ────────────────────────────────
phase('Docs+Staging')
const merged = results.filter(r => r && r.status === 'merged')
if (merged.length) {
  await agent(
    `Docs post-merge groupées de la vague + déploiement staging unique (Phase 4 de la skill).
     Features mergées: ${merged.map(m => m.featureId).join(', ')}.
     1) UN commit docs/wave-${DATE}-complete: statuts "Terminée" dans PRODUCT_SPEC.md,
        1 entrée historique PAR SF, MAJ ARCHITECTURE_CANONIQUE.md si nouvelles tables,
        MAJ MARKETING_BACKLOG.md si une feature couvre une tâche M-XX.
     2) Déploiement staging UNIQUE: gh workflow run backend.yml --ref master ; front auto ;
        healthcheck https://staging.legalcase.fr/api/actuator/health (background).
     Ne déclenche le redeploy qu'UNE fois, après tous les merges.`,
    { label: 'docs+staging', phase: 'Docs+Staging' }
  )
}

// ─── Phase 4 : Récap unique d'arbitrages ─────────────────────────────────────
phase('Récap')
return {
  date: DATE,
  delivered: merged.map(m => ({ id: m.featureId, prs: m.prs, arbitrages: m.arbitrages || [], risks: m.residualRisks || [] })),
  halted: results.filter(r => r && r.status === 'halted').map(r => ({ id: r.featureId, question: r.haltQuestion })),
  parkedRed: parked.map(f => ({ id: f.id, reason: f.reason })),
  failed: results.filter(r => r && r.status === 'failed').map(r => r.featureId),
  notReached: toDeliver.slice(results.length).map(f => f.id),
  coverageGaps: plan.coverageGaps || [],
  budgetSpent: budget.total ? `${Math.round(budget.spent()/1000)}k / ${Math.round(budget.total/1000)}k` : 'illimité',
}

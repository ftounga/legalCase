export const meta = {
  name: 'drh-wave',
  description: "Lance une vague de prospection DRH (employeur) de bout en bout : sourcing Apollo (5 secteurs : securite/proprete/transport/restauration/medico-social prive), exclut les hopitaux publics + deja-contactes, enrich, generation d'accroches secteur-aware en parallele, push direct dans la campagne Lemlist DRH. Acquisition, ne cree pas de feature produit.",
  whenToUse: "Lancer une nouvelle vague d'emailing DRH : 'lance la vague DRH' / 'run drh-wave'. args.perSector = nb d'entreprises par secteur (defaut 20). Suppose Apollo + cle Lemlist configures (.apollo_key, .lemlist_key).",
  phases: [
    { title: 'Source', detail: 'apollo_drh_pipeline.py (5 secteurs, exclut public + deja-contactes, enrich)' },
    { title: 'Personnalise', detail: 'split -> fan-out accroches secteur-aware -> merge + nettoyage' },
    { title: 'Push', detail: 'lemlist_push.py -> pousse dans la campagne Lemlist DRH' },
  ],
}

// Lancer : Workflow({ name: "drh-wave", args: { perSector: 20 } })
const DIR = 'tools/prospection-apollo'
const DRH_CAMPAIGN = 'cam_sikMYuuPxpjoYysSa' // « DRH — 5 secteurs »
const perSector = (args && args.perSector) || 20

// ───────────────── Phase 1 : SOURCE ─────────────────
phase('Source')
const src = await agent(
  `Sourcing DRH 5 secteurs. Exécute :\n` +
  `cd ${DIR} && python3 apollo_drh_pipeline.py --per-sector ${perSector} --out drh-wave-domfit.csv 2>&1 | tail -25\n` +
  `Puis: wc -l ${DIR}/drh-wave-domfit.csv\n` +
  `Retourne le nombre de DRH (lignes - 1).`,
  { label: 'source', phase: 'Source',
    schema: { type: 'object', properties: { count: { type: 'number' } }, required: ['count'] } }
)
log(`Source: ${src ? src.count : '?'} DRH (hors public + hors deja-contactes)`)
if (!src || src.count === 0) { return { error: 'aucun DRH source', src } }

// ───────────────── Phase 2 : PERSONNALISE ─────────────────
phase('Personnalise')
const prep = await agent(
  `Découpe en lots : cd ${DIR} && python3 drh_split_batches.py --csv drh-wave-domfit.csv\n` +
  `Le script affiche "NBATCHES=N". Retourne N.`,
  { label: 'split', phase: 'Personnalise',
    schema: { type: 'object', properties: { nbatches: { type: 'number' } }, required: ['nbatches'] } }
)
const N = prep && prep.nbatches ? prep.nbatches : 0
log(`Personnalisation: ${N} lots`)

await parallel(Array.from({ length: N }, (_, i) => () => agent(
  `Génère les accroches DRH du lot ${i + 1}. Lis ${DIR}/drh_batches/batch_${i + 1}.tsv ` +
  `(lignes : gid<TAB>secteur<TAB>entreprise<TAB>specialites). Contexte : LegalCase chiffre l'exposition ` +
  `prud'homale d'une rupture cote EMPLOYEUR ; ton = avis d'expert, PAS de vente. ` +
  `Pour CHAQUE ligne, UNE accroche introPerso FR : s'insere apres « Bonjour [Prenom], » (minuscule, sans « Bonjour »), ` +
  `≤ 28 mots, factuel, jamais flagorneur, JAMAIS « IA », cite l'entreprise, et evoque l'angle prud'homal du SECTEUR : ` +
  `securite privee = abandons de poste/fautes/turnover ; proprete = transferts annexe 7 / L.1224-1 ; ` +
  `transport = inaptitudes/AT/licenciements ; restauration = turnover/periode d'essai/saisonnalite ; ` +
  `medico-social prive = disciplinaire/inaptitude/tension RH. Si l'entreprise est manifestement d'un autre secteur ` +
  `que son etiquette, cale l'angle sur son activite reelle. Varie la formulation. ` +
  `Ecris ${DIR}/drh_batches/batch_${i + 1}_out.tsv au format « gid<TAB>accroche » (meme gid, ordre preserve). Reponds « ok ${i + 1} ».`,
  { label: `perso:${i + 1}`, phase: 'Personnalise' }
)))

const merged = await agent(
  `Assemble + nettoie : cd ${DIR} && python3 drh_merge_intros.py --csv drh-wave-domfit.csv --out drh-wave-lemlist.csv\n` +
  `Retourne le nombre de lignes finales.`,
  { label: 'merge', phase: 'Personnalise',
    schema: { type: 'object', properties: { count: { type: 'number' } }, required: ['count'] } }
)
log(`CSV DRH pret: ${merged ? merged.count : '?'} contacts`)

// ───────────────── Phase 3 : PUSH ─────────────────
phase('Push')
const push = await agent(
  `Pousse les leads dans la campagne Lemlist DRH. Etapes :\n` +
  `1) Dry-run : cd ${DIR} && python3 lemlist_push.py --csv drh-wave-lemlist.csv --campaign ${DRH_CAMPAIGN} --dry-run\n` +
  `2) Push reel : cd ${DIR} && python3 lemlist_push.py --csv drh-wave-lemlist.csv --campaign ${DRH_CAMPAIGN}\n` +
  `(lemlist_push.py envoie deja un User-Agent.) Les "HTTP 400 already in the campaign" = doublons, OK. ` +
  `Signale tout autre HTTP 4xx/5xx. Retourne {pushed: true/false, note: "<nb pousses / erreurs>"}.`,
  { label: 'push', phase: 'Push',
    schema: { type: 'object', properties: { pushed: { type: 'boolean' }, note: { type: 'string' } }, required: ['pushed'] } }
)

log(push && push.pushed ? `Push DRH OK: ${push.note || ''}` : `Push DRH: ${push ? push.note || '' : 'echec'}`)
return { sourced: src.count, personalized: merged ? merged.count : null, push }

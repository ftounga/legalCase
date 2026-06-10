export const meta = {
  name: 'avocat-wave',
  description: 'Vague avocats: sourcing Apollo + perso introPerso (fan-out) + push Lemlist',
  phases: [
    { title: 'Source', detail: 'avocat_pipeline.py (source + enrich + filtre domaine-fit)' },
    { title: 'Personnalise', detail: 'split -> fan-out accroches -> merge CSV Lemlist' },
    { title: 'Push', detail: 'lemlist_push.py si API dispo, sinon CSV prêt pour import manuel' },
  ],
}

// Lancer : Workflow({ scriptPath: "tools/prospection-apollo/avocat-wave.workflow.js", args: { perDomain: 100 } })
const DIR = 'tools/prospection-apollo'
const perDomain = (args && args.perDomain) || 100

// ───────────────── Phase 1 : SOURCE ─────────────────
phase('Source')
const src = await agent(
  `Sourcing avocats 3 domaines. Exécute exactement :\n` +
  `cd ${DIR} && python3 avocat_pipeline.py --per-domain ${perDomain} --out avocat-wave-domfit.csv 2>&1 | tail -25\n` +
  `Puis: wc -l ${DIR}/avocat-wave-domfit.csv\n` +
  `Retourne le nombre de contacts domaine-fit (lignes - 1 pour l'en-tête).`,
  { label: 'source', phase: 'Source',
    schema: { type: 'object', properties: { count: { type: 'number' } }, required: ['count'] } }
)
log(`Source: ${src ? src.count : '?'} contacts domaine-fit`)
if (!src || src.count === 0) { return { error: 'aucun contact sourcé', src } }

// ───────────────── Phase 2 : PERSONNALISE ─────────────────
phase('Personnalise')
const prep = await agent(
  `Découpe le CSV en lots pour la génération d'accroches. Exécute :\n` +
  `cd ${DIR} && python3 split_batches.py --csv avocat-wave-domfit.csv\n` +
  `Le script affiche une ligne "NBATCHES=N". Retourne N (entier).`,
  { label: 'split', phase: 'Personnalise',
    schema: { type: 'object', properties: { nbatches: { type: 'number' } }, required: ['nbatches'] } }
)
const N = prep && prep.nbatches ? prep.nbatches : 0
log(`Personnalisation: ${N} lots à traiter en parallèle`)

await parallel(Array.from({ length: N }, (_, i) => () => agent(
  `Génère les accroches du lot ${i + 1}. Lis le fichier ${DIR}/batches/batch_${i + 1}.tsv ` +
  `(chaque ligne : gid<TAB>domaine<TAB>cabinet<TAB>specialites). ` +
  `Pour CHAQUE ligne, rédige UNE accroche introPerso en français : s'insère après « Bonjour [Prénom], » ` +
  `(commence par une minuscule, PAS de « Bonjour » dedans), ≤ 28 mots, ton confrère, factuel, jamais flagorneur, ` +
  `référence 1-2 spécialités RÉELLEMENT pertinentes pour le domaine (ignore le hors-sujet), varie la formulation, ` +
  `JAMAIS le mot « IA ». Si rien d'exploitable : accroche sobre sur le domaine. ` +
  `Écris ${DIR}/batches/batch_${i + 1}_out.tsv au format « gid<TAB>accroche » — MÊME gid que l'entrée, ordre des lignes préservé. ` +
  `Réponds juste « ok ${i + 1} ».`,
  { label: `perso:${i + 1}`, phase: 'Personnalise' }
)))

const merged = await agent(
  `Assemble le CSV final Lemlist. Exécute :\n` +
  `cd ${DIR} && python3 merge_intros.py --csv avocat-wave-domfit.csv --out avocat-wave-lemlist.csv\n` +
  `Retourne le nombre de lignes du CSV final.`,
  { label: 'merge', phase: 'Personnalise',
    schema: { type: 'object', properties: { count: { type: 'number' } }, required: ['count'] } }
)
log(`CSV Lemlist prêt: ${merged ? merged.count : '?'} contacts personnalisés`)

// ───────────────── Phase 3 : PUSH ─────────────────
phase('Push')
const push = await agent(
  `Pousse les leads dans la campagne Lemlist SI l'API est disponible. Étapes :\n` +
  `1) Dry-run : cd ${DIR} && python3 lemlist_push.py --csv avocat-wave-lemlist.csv --dry-run\n` +
  `2) Test API : python3 -c "import base64,urllib.request,urllib.error; k=open('${DIR}/.lemlist_key').read().strip(); ` +
  `a=base64.b64encode((':'+k).encode()).decode();\\n` +
  `r=urllib.request.Request('https://api.lemlist.com/api/team',headers={'Authorization':'Basic '+a});\\n` +
  `import sys;\\ntry:\\n urllib.request.urlopen(r,timeout=20); print('API_OK')\\nexcept urllib.error.HTTPError as e: print('API_KO',e.code)" \n` +
  `3) Si « API_OK » : relance SANS --dry-run pour pousser réellement (python3 lemlist_push.py --csv avocat-wave-lemlist.csv). ` +
  `Si « API_KO » (ex. 403/1010 = plan sans API) : NE PAS pousser, indiquer que le CSV ${DIR}/avocat-wave-lemlist.csv est prêt pour import manuel dans Lemlist. ` +
  `Retourne {pushed: true/false, note: "..."}.`,
  { label: 'push', phase: 'Push',
    schema: { type: 'object', properties: { pushed: { type: 'boolean' }, note: { type: 'string' } }, required: ['pushed'] } }
)

log(push && push.pushed ? `Push OK: ${push.note || ''}` : `Push différé (API indisponible) — import manuel. ${push ? push.note || '' : ''}`)
return { sourced: src.count, personalized: merged ? merged.count : null, push }

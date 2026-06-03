export const meta = {
  name: 'afrique-product-spec',
  description: "Génère la fiche produit EXHAUSTIVE de LegalCase Afrique OHADA, calibrée sur la PRODUCT_SPEC Europe (volumétrie/granularité/format), par fan-out + critique de complétude + boucle jusqu'à parité. Livrable = HYPOTHÈSE de cadrage hors backlog. NE TOUCHE PAS au PRODUCT_SPEC.md live ni au sync F-178.",
  whenToUse: "Cadrage LegalCase Afrique OHADA : 'génère/parfais la fiche produit Afrique'. Suppose docs/afrique/CADRAGE-STRATEGIQUE-OHADA.md figé. Ré-invocable : chaque run repart du draft existant pour le parfaire.",
  phases: [
    { title: 'Calibrer', detail: "extraire l'ADN de PRODUCT_SPEC.md (format, taxonomie, volumétrie, granularité)" },
    { title: 'Cartographier', detail: "domaines OHADA (10 Actes uniformes) + infra Afrique + plateforme, avec cibles de densité" },
    { title: 'Générer', detail: "1 agent par domaine : table de features au pattern dossier-centric" },
    { title: 'Critiquer', detail: "critique de complétude par domaine, boucle jusqu'à parité avec la référence" },
    { title: 'Assembler', detail: "écrit docs/afrique/PRODUCT_SPEC_OHADA_DRAFT.md (HYPOTHÈSE, hors backlog)" },
    { title: 'Récap', detail: "parité atteinte, trous, doutes résiduels" },
  ],
}

// ─── args ────────────────────────────────────────────────────────────────────
// args = { dateISO?: string, referenceSpec?: string, cadrage?: string, outputFile?: string,
//          parityRounds?: number, domainsOverride?: string[] }
// dateISO OBLIGATOIRE (Date.now() interdit dans les scripts Workflow) — passé au lancement.
const DATE = (args && args.dateISO) || 'DATE-A-PASSER-EN-ARGS'
const REFERENCE = (args && args.referenceSpec) || 'docs/PRODUCT_SPEC.md'
const CADRAGE = (args && args.cadrage) || 'docs/afrique/CADRAGE-STRATEGIQUE-OHADA.md'
const OUTPUT = (args && args.outputFile) || 'docs/afrique/PRODUCT_SPEC_OHADA_DRAFT.md'
const PARITY_ROUNDS = (args && args.parityRounds) || 2  // tentatives de rattrapage par domaine
const DOMAINS_OVERRIDE = (args && args.domainsOverride) || null

// ─── schémas (validation au tool-call, pas de parsing) ───────────────────────
const CALIBRATION_SCHEMA = {
  type: 'object',
  required: ['format', 'taxonomy', 'volumetry', 'granularityRules'],
  properties: {
    format: { type: 'object', required: ['tableColumns', 'statusVocab'], properties: {
      tableColumns: { type: 'array', items: { type: 'string' } },
      statusVocab: { type: 'array', items: { type: 'string' } },
      rowExample: { type: 'string' } } },
    taxonomy: { type: 'object', properties: {
      sectionOrder: { type: 'array', items: { type: 'string' } },
      blocPattern: { type: 'string' } } },
    volumetry: { type: 'object', required: ['totalFeatures', 'perDomainTarget'], properties: {
      totalFeatures: { type: 'number' },
      perDomainTarget: { type: 'number' },          // densité cible de features par domaine
      descLenWordsAvg: { type: 'number' } } },
    granularityRules: { type: 'array', items: { type: 'string' } }, // "1 comportement concret = 1 ligne", etc.
  },
}
const DOMAINMAP_SCHEMA = {
  type: 'object',
  required: ['domains'],
  properties: { domains: { type: 'array', items: {
    type: 'object', required: ['key', 'title', 'kind', 'targetCount'],
    properties: {
      key: { type: 'string' },
      title: { type: 'string' },
      kind: { enum: ['platform', 'ohada-domain', 'infra-afrique'] },
      acteUniforme: { type: 'string' },     // pour kind=ohada-domain
      targetCount: { type: 'number' },      // nb de features visé (calibré sur la densité de référence)
      situations: { type: 'array', items: { type: 'string' } },
    } } } },
}
const DOMAINSPEC_SCHEMA = {
  type: 'object',
  required: ['domainKey', 'features'],
  properties: {
    domainKey: { type: 'string' },
    blocTitle: { type: 'string' },
    features: { type: 'array', items: {
      type: 'object', required: ['id', 'title', 'description', 'status', 'groundedness'],
      properties: {
        id: { type: 'string' },            // ex. F-OH-AUDCG-01
        title: { type: 'string' },
        description: { type: 'string' },
        status: { type: 'string' },        // 'Hypothèse' par défaut (hors backlog)
        groundedness: { enum: ['acte-uniforme', 'plateforme-reutilisee', 'hypothese'] },
        decisionTool: { type: 'boolean' }, // est-ce un outil décisionnel / simulateur ?
      } } },
  },
}
const CRITIC_SCHEMA = {
  type: 'object',
  required: ['domainKey', 'parityOk'],
  properties: {
    domainKey: { type: 'string' },
    parityOk: { type: 'boolean' },          // densité + couverture des situations atteintes ?
    coverageGaps: { type: 'array', items: { type: 'string' } }, // situations métier non couvertes
    suggestedAdds: { type: 'number' },
    qualityNotes: { type: 'array', items: { type: 'string' } },  // gadget/redondance/granularité
  },
}

// ─── Phase 1 : Calibrer sur la référence ─────────────────────────────────────
phase('Calibrer')
log(`Calibration sur ${REFERENCE} — extraction de l'ADN de spec`)
const cal = await agent(
  `Tu calibres un standard de fiche produit. Lis INTÉGRALEMENT ${REFERENCE} (la PRODUCT_SPEC de
   LegalCase Europe) et ${CADRAGE} (le cadrage stratégique figé Afrique OHADA, décisions D1-D9).
   Extrais l'ADN RÉUTILISABLE de la spec de référence, à reproduire à l'identique pour la version OHADA :
   - format EXACT des tables de features (colonnes, vocabulaire de statut, un exemple de ligne) ;
   - taxonomie (ordre des sections, pattern de découpage en "Blocs") ;
   - volumétrie : nombre total de features, DENSITÉ cible par domaine (features / domaine), longueur
     moyenne de description ;
   - règles de granularité observées (ce qui mérite une ligne vs ce qui n'en mérite pas).
   Objectif : donner aux générateurs aval une cible MESURABLE de parité. Retourne CALIBRATION_SCHEMA.`,
  { label: 'calibrate', phase: 'Calibrer', schema: CALIBRATION_SCHEMA, agentType: 'Explore' }
)
log(`Référence: ${cal.volumetry.totalFeatures} features, densité cible ~${cal.volumetry.perDomainTarget}/domaine`)

// ─── Phase 2 : Cartographier les domaines OHADA ──────────────────────────────
phase('Cartographier')
const map = await agent(
  `Tu construis la carte des domaines de la fiche produit LegalCase Afrique OHADA.
   Base juridique = les 10 Actes uniformes OHADA listés dans ${CADRAGE} (§2) : AUDCG, AUSCGIE, AUS,
   AUPSRVE, AUPC, AUDCIF/SYSCOHADA, AUA, AUM, AUCTMR, AUSCOOP. Ajoute :
   - les domaines 'infra-afrique' du §3 (multi-pays OHADA-17, paiement bi-rail, hébergement/résidence
     données, i18n/XAF/charte, bijuridisme Option A francophone, auth) ;
   - les domaines 'platform' réutilisés du §4 (fondations, dossiers, upload/stockage, pipeline IA
     chunk→doc→dossier, outils décisionnels/simulateurs, génération d'actes, Q&A interactive).
   ${DOMAINS_OVERRIDE ? `Restreins-toi à ces domaines: ${JSON.stringify(DOMAINS_OVERRIDE)}.` : ''}
   Pour CHAQUE domaine : un targetCount de features cohérent avec la densité de référence
   (~${cal.volumetry.perDomainTarget}/domaine, module selon la richesse de l'Acte), et la liste des
   situations métier réelles à couvrir. Respecte D5 (OHADA droit des affaires only) et D7 (francophone).
   Retourne DOMAINMAP_SCHEMA.`,
  { label: 'domain-map', phase: 'Cartographier', schema: DOMAINMAP_SCHEMA, agentType: 'Explore' }
)
const domains = map.domains
log(`${domains.length} domaines, cible totale ~${domains.reduce((s, d) => s + (d.targetCount || 0), 0)} features`)

// ─── Phases 3+4 : Générer chaque domaine puis boucler jusqu'à parité ──────────
// Pipeline indépendant par domaine : génération → (critique → rattrapage) jusqu'à parité ou
// PARITY_ROUNDS atteint. Chaque domaine progresse sans barrière avec les autres.
phase('Générer')
const specs = await parallel(domains.map(d => async () => {
  if (budget.total && budget.remaining() < 60_000) {
    log(`Budget bas — domaine ${d.key} généré en 1 passe sans rattrapage`)
  }
  const genPrompt = (extra) =>
    `Tu spécifies le domaine "${d.title}" (${d.kind}${d.acteUniforme ? ', ' + d.acteUniforme : ''}) de la
     fiche produit LegalCase Afrique OHADA, AU MÊME NIVEAU d'exigence et de volumétrie que la référence.
     Calibration à respecter STRICTEMENT : colonnes ${JSON.stringify(cal.format.tableColumns)},
     statut par défaut "Hypothèse", densité cible ~${d.targetCount} features, règles de granularité
     ${JSON.stringify(cal.granularityRules)}. Lis ${REFERENCE} pour le NIVEAU, et ${CADRAGE} pour le contexte.
     Si ${OUTPUT} existe déjà, lis la section de CE domaine et AMÉLIORE-la (ne repars pas de zéro).
     Couvre les situations métier réelles de l'Acte : ${JSON.stringify(d.situations || [])}.
     Applique le pattern dossier-centric (D3) : situation → upload pièces → analyse pipeline IA →
     outil décisionnel/simulateur → génération d'acte. Marque groundedness pour chaque feature
     (acte-uniforme / plateforme-reutilisee / hypothese). IDs forme F-OH-<CLE>-NN.
     ${extra || ''} Retourne DOMAINSPEC_SCHEMA.`

  let spec = await agent(genPrompt(), { label: `gen:${d.key}`, phase: 'Générer', schema: DOMAINSPEC_SCHEMA })
  let round = 0
  while (round < PARITY_ROUNDS && !(budget.total && budget.remaining() < 60_000)) {
    const critic = await agent(
      `Critique de complétude du domaine "${d.title}" pour la fiche produit OHADA.
       Voici la spec actuelle: ${JSON.stringify(spec.features)}.
       Compare à la densité cible (~${d.targetCount}) et à la couverture des situations
       ${JSON.stringify(d.situations || [])}. Signale : situations non couvertes, features gadget/
       redondantes, écarts de granularité avec la référence. parityOk=true seulement si densité ET
       couverture sont atteintes sans gadget. Retourne CRITIC_SCHEMA.`,
      { label: `critic:${d.key}`, phase: 'Critiquer', schema: CRITIC_SCHEMA })
    if (critic.parityOk) { log(`✓ parité ${d.key} (round ${round})`); break }
    log(`↻ ${d.key}: ${critic.coverageGaps?.length || 0} trous, +${critic.suggestedAdds || 0} — rattrapage`)
    spec = await agent(
      genPrompt(`RATTRAPAGE: complète les trous suivants sans casser l'existant: ` +
        `${JSON.stringify(critic.coverageGaps || [])}. Corrige aussi: ${JSON.stringify(critic.qualityNotes || [])}.`),
      { label: `refine:${d.key}`, phase: 'Générer', schema: DOMAINSPEC_SCHEMA })
    round++
  }
  return spec
}))

const ok = specs.filter(Boolean)
const totalFeatures = ok.reduce((s, sp) => s + (sp.features?.length || 0), 0)
log(`Généré: ${ok.length}/${domains.length} domaines, ${totalFeatures} features`)

// ─── Phase 5 : Assembler le livrable (HYPOTHÈSE, hors backlog) ────────────────
phase('Assembler')
await agent(
  `Assemble la fiche produit LegalCase Afrique OHADA et ÉCRIS-la dans ${OUTPUT} (crée le dossier si besoin).
   Données par domaine (JSON): ${JSON.stringify(ok)}.
   Mets en tête un bloc d'avertissement clair :
   "> **HYPOTHÈSE DE CADRAGE — hors backlog.** Générée le ${DATE} par le workflow afrique-product-spec.
    > NE PAS synchroniser vers le backlog F-178. NE PAS confondre avec un ordre de build. Verrou
    > d'activation: voir docs/afrique/CADRAGE-STRATEGIQUE-OHADA.md."
   Reproduis EXACTEMENT le format de table de la référence (colonnes ${JSON.stringify(cal.format.tableColumns)}),
   statut "Hypothèse". Organise en sections: 1) Plateforme réutilisée, 2) Domaines OHADA (un Bloc par
   Acte uniforme), 3) Infra Afrique / transversal. Ajoute une table de synthèse (domaine → nb features →
   groundedness dominante) et une section "Doutes résiduels / à valider" reprenant les coverageGaps non
   résolus. N'écris RIEN ailleurs que dans ${OUTPUT}. Ne modifie PAS docs/PRODUCT_SPEC.md.`,
  { label: 'assemble', phase: 'Assembler' })

// ─── Phase 6 : Récap ─────────────────────────────────────────────────────────
phase('Récap')
return {
  date: DATE,
  output: OUTPUT,
  referenceTotal: cal.volumetry.totalFeatures,
  generatedTotal: totalFeatures,
  parityRatio: cal.volumetry.totalFeatures ? +(totalFeatures / cal.volumetry.totalFeatures).toFixed(2) : null,
  domains: ok.map(s => ({ key: s.domainKey, features: s.features?.length || 0 })),
  budgetSpent: budget.total ? `${Math.round(budget.spent() / 1000)}k / ${Math.round(budget.total / 1000)}k` : 'illimité',
  note: 'HYPOTHÈSE de cadrage — hors backlog F-178. Activation conditionnée au verrou radar OHADA.',
}

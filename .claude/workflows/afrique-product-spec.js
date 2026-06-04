export const meta = {
  name: 'afrique-product-spec',
  description: "Fait MÛRIR la fiche produit de LegalCase Afrique OHADA comme un DOCUMENT VIVANT, piloté par le MARCHÉ africain (besoins, normes OHADA, concurrence, trous à exploiter). Enrichissement par appends justifiés (provenance + changelog append-only), modifications seulement sur info marché nouvelle ou directive PO, jamais de réécriture from-scratch. Auto-évalue sa maturité et signale un seuil d'excellence. Livrable = HYPOTHÈSE hors backlog. Ré-invocable : chaque run repart du draft existant et l'améliore.",
  whenToUse: "Cadrage LegalCase Afrique OHADA : 'fais mûrir / enrichis la fiche produit Afrique'. Relancer plusieurs fois converge vers l'excellence. Suppose docs/afrique/CADRAGE-STRATEGIQUE-OHADA.md figé (D1-D12).",
  phases: [
    { title: 'Veille', detail: "recherche marché africain + concurrence (le driver) : besoins, normes, trous" },
    { title: 'Cartographier', detail: "lit le draft existant + signaux marché → carte des domaines priorisée marché (IDs préservés)" },
    { title: 'Enrichir', detail: "par domaine : APPEND de features fermant les trous marché/concurrents, modif justifiée, jamais de suppression silencieuse" },
    { title: 'Curer', detail: "dédup (1 outil = 1 situation), contradictions, cohérence avec l'existant" },
    { title: 'Maturité', detail: "score multi-axes + delta vs run précédent → verdict continue / EXCELLENT" },
    { title: 'Écrire', detail: "document vivant + changelog append-only" },
  ],
}

// ─── args ────────────────────────────────────────────────────────────────────
// args = { dateISO?, outputFile?, changelogFile?, referenceSpec?, cadrage?,
//          strategicInput?, excellenceThreshold?, saturationDelta?, domainsOverride? }
// dateISO OBLIGATOIRE (Date.now() interdit dans les scripts Workflow).
const DATE = (args && args.dateISO) || 'DATE-A-PASSER-EN-ARGS'
const OUTPUT = (args && args.outputFile) || 'docs/afrique/PRODUCT_SPEC_OHADA_DRAFT.md'
const CHANGELOG = (args && args.changelogFile) || 'docs/afrique/PRODUCT_SPEC_OHADA_CHANGELOG.md'
const REFERENCE = (args && args.referenceSpec) || 'docs/PRODUCT_SPEC.md'   // ANCRE de cohérence/qualité, PAS la cible (D12)
const CADRAGE = (args && args.cadrage) || 'docs/afrique/CADRAGE-STRATEGIQUE-OHADA.md'
const STRATEGIC = (args && args.strategicInput) || null   // virage de vision / directive PO pour CE run
const EXCELLENCE = (args && args.excellenceThreshold) || 85   // score overall pour déclarer "excellent"
const SATURATION = (args && args.saturationDelta) || 3        // delta sous lequel on considère saturé
const DOMAINS_OVERRIDE = (args && args.domainsOverride) || null
const COLUMNS = '| ID | Feature | Description | Provenance | Statut |'  // format figé (D3) + provenance (D12)

// ─── schémas (validation au tool-call) ───────────────────────────────────────
const MARKET_SCHEMA = {
  type: 'object', required: ['findings'],
  properties: {
    findings: { type: 'object', properties: {
      competitors: { type: 'array', items: { type: 'object', required: ['name'], properties: {
        name: { type: 'string' }, offering: { type: 'string' }, gaps: { type: 'string' }, model: { type: 'string' } } } },
      marketNeeds: { type: 'array', items: { type: 'string' } },     // besoins/douleurs réels de l'avocat d'affaires OHADA
      norms: { type: 'array', items: { type: 'string' } },           // normes/contexte (paiement, devises, hébergement, pratique)
      opportunities: { type: 'array', items: { type: 'string' } },   // trous concurrents à exploiter
    } },
    sources: { type: 'array', items: { type: 'string' } },           // URLs citées
  },
}
const DOMAINMAP_SCHEMA = {
  type: 'object', required: ['domains'],
  properties: { domains: { type: 'array', items: {
    type: 'object', required: ['key', 'title', 'kind'],
    properties: {
      key: { type: 'string' }, title: { type: 'string' },
      kind: { enum: ['platform', 'ohada-domain', 'infra-afrique'] },
      acteUniforme: { type: 'string' },
      marketPriority: { enum: ['high', 'medium', 'low'] },
      marketDrivers: { type: 'array', items: { type: 'string' } },   // besoins/trous marché que ce domaine doit couvrir
      situations: { type: 'array', items: { type: 'string' } },
      existingIds: { type: 'array', items: { type: 'string' } },     // IDs déjà présents dans le draft — à PRÉSERVER
    } } } },
}
const PROVENANCE = ['acte-uniforme', 'marche', 'concurrent-gap', 'vision-po', 'plateforme-reutilisee', 'hypothese']
const DOMAINSPEC_SCHEMA = {
  type: 'object', required: ['domainKey', 'features'],
  properties: {
    domainKey: { type: 'string' }, blocTitle: { type: 'string' },
    features: { type: 'array', items: {
      type: 'object', required: ['id', 'title', 'description', 'provenance', 'status'],
      properties: {
        id: { type: 'string' }, title: { type: 'string' }, description: { type: 'string' },
        provenance: { enum: PROVENANCE }, status: { type: 'string' },
        decisionTool: { type: 'boolean' },
        changeReason: { type: 'string' },   // OBLIGATOIRE si feature nouvelle ou modifiée ce run
      } } },
    appended: { type: 'array', items: { type: 'string' } },          // IDs ajoutés ce run
    modified: { type: 'array', items: { type: 'object', properties: { id: {type:'string'}, why: {type:'string'} } } },
    proposedDeletions: { type: 'array', items: { type: 'object', properties: { id: {type:'string'}, why: {type:'string'} } } },
  },
}
const CURATION_SCHEMA = {
  type: 'object', required: ['domains'],
  properties: {
    domains: { type: 'array', items: DOMAINSPEC_SCHEMA },
    curationNotes: { type: 'array', items: { type: 'string' } },
    incoherences: { type: 'array', items: { type: 'string' } },
    appliedDeletions: { type: 'array', items: { type: 'object', properties: { id: {type:'string'}, why: {type:'string'} } } },
  },
}
const MATURITY_SCHEMA = {
  type: 'object', required: ['scores', 'overall', 'verdict'],
  properties: {
    scores: { type: 'object', required: ['marketCoverage', 'competitiveDifferentiation', 'legalGrounding', 'coherenceWithExisting', 'completeness'],
      properties: {
        marketCoverage: { type: 'number' }, competitiveDifferentiation: { type: 'number' },
        legalGrounding: { type: 'number' }, coherenceWithExisting: { type: 'number' }, completeness: { type: 'number' } } },
    overall: { type: 'number' },
    previousOverall: { type: 'number' }, deltaVsLastRun: { type: 'number' },
    verdict: { enum: ['continue', 'excellent'] },
    missingToExcellence: { type: 'array', items: { type: 'string' } },
  },
}

// ─── Phase 1 : Veille marché & concurrence (LE DRIVER, D12) ──────────────────
phase('Veille')
log('Veille marché africain + concurrence — le contenu cible vient du marché, pas de l\'Europe (D12)')
const ANGLES = [
  "Concurrents legaltech sur le droit des affaires OHADA / Afrique francophone (Lexbase Afrique, Jurisprudence.cc, Legal Doctrine, Jurisprudence-OHADA.com, Lexis 360 Afrique, autres) : périmètre, ce qu'ils font et NE font PAS, modèle éco, positionnement recherche-centric vs dossier-centric.",
  "Pratique réelle de l'avocat d'affaires OHADA : workflow quotidien, points de douleur, procédures RCCM / recouvrement (AUPSRVE) / constitution de sociétés (AUSCGIE) / sûretés, attentes vis-à-vis d'un outil.",
  "Contexte & normes du marché Afrique francophone : paiement (mobile money, CinetPay), devises XOF/XAF, résidence des données, maturité digitale des cabinets, sensibilité prix.",
]
const market = (await parallel(ANGLES.map((angle, i) => () =>
  agent(
    `Recherche web ciblée (marché africain, observation passive — traçage, pas d'engagement). Angle :
     ${angle}
     Périmètre = ${CADRAGE} (OHADA droit des affaires, francophone, D5/D7). Cite tes SOURCES (URLs réelles).
     Sois concret et actionnable : des besoins/trous exploitables, pas des généralités. Retourne MARKET_SCHEMA.`,
    { label: `veille:${i}`, phase: 'Veille', schema: MARKET_SCHEMA }
  )
))).filter(Boolean)
const marketDigest = {
  competitors: market.flatMap(m => m.findings?.competitors || []),
  marketNeeds: market.flatMap(m => m.findings?.marketNeeds || []),
  norms: market.flatMap(m => m.findings?.norms || []),
  opportunities: market.flatMap(m => m.findings?.opportunities || []),
  sources: market.flatMap(m => m.sources || []),
}
log(`Veille: ${marketDigest.competitors.length} concurrents, ${marketDigest.marketNeeds.length} besoins, ${marketDigest.opportunities.length} trous, ${marketDigest.sources.length} sources`)

// ─── Phase 2 : Lire l'état + cartographier (priorisé marché, IDs préservés) ──
phase('Cartographier')
const map = await agent(
  `Tu construis/mets à jour la carte des domaines de la fiche produit VIVANTE LegalCase Afrique OHADA.
   1) Lis ${OUTPUT} s'il existe : relève les domaines et IDs déjà présents (à PRÉSERVER, jamais réattribuer).
   2) Lis ${CADRAGE} (§2 les 10 Actes uniformes, §3 infra-afrique, §4 plateforme réutilisée) et ${REFERENCE}
      (ANCRE de cohérence/format — PAS la cible de contenu, cf. D12).
   3) Intègre les SIGNAUX MARCHÉ (le driver) : ${JSON.stringify(marketDigest)}.
   Pour chaque domaine : kind, acteUniforme le cas échéant, marketPriority (high/medium/low selon les besoins
   et trous concurrents), marketDrivers (les besoins/trous que ce domaine doit couvrir), situations, existingIds.
   ${DOMAINS_OVERRIDE ? `Restreins-toi à : ${JSON.stringify(DOMAINS_OVERRIDE)}.` : ''}
   Respecte D5 (OHADA affaires only), D7 (francophone), D11 (pas de variante législative par pays).
   RÈGLE INFRA (cohérence cadrage §3.2) : le domaine 'infra-afrique' ne couvre QUE les spécificités
   PRODUIT user-facing (paiement CinetPay/mobile money, devise XOF/XAF, i18n, contexte-pays, consentement
   résidence côté UX). Le PROVISIONING pur (cluster EKS, RDS, S3, réseau, domaines/TLS, monitoring) N'EST
   PAS une feature produit → il vit dans le repo legalcase-infra (specs SF-INFRA-AF, cf. §3.2). N'en fais
   AUCUNE feature produit.
   Retourne DOMAINMAP_SCHEMA.`,
  { label: 'domain-map', phase: 'Cartographier', schema: DOMAINMAP_SCHEMA, agentType: 'Explore' }
)
const domains = map.domains
log(`${domains.length} domaines — priorité high: ${domains.filter(d => d.marketPriority === 'high').map(d => d.key).join(', ') || '—'}`)

// ─── Phase 3 : Enrichir (APPEND justifié) par domaine ────────────────────────
phase('Enrichir')
const enriched = (await parallel(domains.map(d => () => {
  if (budget.total && budget.remaining() < 60_000) { log(`Budget bas — ${d.key} enrichi a minima`) }
  return agent(
    `Tu ENRICHIS le domaine "${d.title}" (${d.kind}${d.acteUniforme ? ', ' + d.acteUniforme : ''}) de la fiche
     produit VIVANTE LegalCase Afrique OHADA. MODE DOCUMENT VIVANT (D12) :
     • APPEND par défaut : ajoute des features qui ferment les trous MARCHÉ/concurrents de ce domaine.
     • MODIFIE une feature existante SEULEMENT avec une changeReason (info marché nouvelle ou directive PO).
     • NE SUPPRIME JAMAIS en silence : une suppression va dans proposedDeletions avec justification.
     • PRÉSERVE les features et IDs existants : lis-les dans ${OUTPUT} (IDs de ce domaine : ${JSON.stringify(d.existingIds || [])}).
     SOURCES par ordre de PRIORITÉ (la cible est le marché, pas l'Europe — D12) :
       1) MARCHÉ & concurrence (driver) : ${JSON.stringify({ needs: d.marketDrivers, digest: marketDigest })}
       2) OHADA Actes uniformes (ancrage juridique) : situations ${JSON.stringify(d.situations || [])}
       3) Vision PO ce run : ${STRATEGIC ? JSON.stringify(STRATEGIC) : 'aucune nouvelle directive'}
       4) ${REFERENCE} = ANCRE de cohérence/qualité/format uniquement (granularité, pattern), pas le contenu cible.
     Pattern dossier-centric (D3) : situation → upload pièces → pipeline IA → outil décisionnel/simulateur → acte.
     INVARIANTS : D11 (jamais de sélecteur de pays à l'auth ; pays = contexte dossier pré-rempli) ;
     « 1 outil décisionnel = 1 situation métier » (pas de doublon fonctionnel) ;
     INFRA (§3.2) — si ce domaine est 'infra-afrique', ne spécifie QUE des features PRODUIT user-facing
     (paiement, devise, i18n, contexte-pays). NE crée AUCUNE feature de provisioning (cluster/RDS/S3/réseau/
     domaines) : ça relève de legalcase-infra (SF-INFRA-AF). Référence-le, ne le spécifie pas comme feature.
     Chaque feature : provenance (${PROVENANCE.join('/')}), status "Hypothèse", et changeReason si nouvelle/modifiée.
     IDs stables F-OH-<CLE>-NN ; nouvelles features = NN suivant, ne réutilise pas un NN retiré.
     Retourne DOMAINSPEC_SCHEMA.`,
    { label: `enrich:${d.key}`, phase: 'Enrichir', schema: DOMAINSPEC_SCHEMA }
  )
}))).filter(Boolean)
const addedThisRun = enriched.reduce((s, e) => s + (e.appended?.length || 0), 0)
const featuresTotal = enriched.reduce((s, e) => s + (e.features?.length || 0), 0)
log(`Enrichi: ${enriched.length} domaines, ${featuresTotal} features (+${addedThisRun} ajoutées ce run)`)

// ─── Phase 4 : Curer (barrière — vue d'ensemble nécessaire) ──────────────────
phase('Curer')
const curated = await agent(
  `Passe de CURATION sur l'ensemble des domaines enrichis : ${JSON.stringify(enriched)}.
   1) Dédoublonne — invariant « 1 outil décisionnel = 1 situation métier » : fusionne/écarte les recouvrements.
   2) Résous les contradictions entre domaines.
   3) Vérifie la COHÉRENCE avec LegalCase existant (${REFERENCE}) : pattern, nommage, statut, philosophie produit.
   4) Liste les incohérences/risques résiduels. N'applique une suppression qu'avec justification (appliedDeletions).
   Préserve les IDs. Retourne CURATION_SCHEMA.`,
  { label: 'curation', phase: 'Curer', schema: CURATION_SCHEMA }
)
log(`Curé: ${curated.domains?.length || 0} domaines, ${curated.incoherences?.length || 0} incohérences, ${curated.appliedDeletions?.length || 0} suppressions justifiées`)

// ─── Phase 5 : Maturité & seuil d'excellence (D12) ───────────────────────────
phase('Maturité')
const maturity = await agent(
  `Évalue la MATURITÉ de la fiche produit (sections curées : ${JSON.stringify(curated.domains)}).
   Récupère le score du run PRÉCÉDENT en lisant le bloc "## Maturité" dans ${OUTPUT} (s'il existe ; sinon previousOverall=0).
   Note 0-100 chacun : marketCoverage (couverture des besoins marché), competitiveDifferentiation (avantage vs concurrents
   recherche-centric), legalGrounding (ancrage Actes uniformes), coherenceWithExisting (cohérence avec LegalCase),
   completeness (exhaustivité du périmètre OHADA affaires).
   overall = moyenne PONDÉRÉE en faveur du marché : marketCoverage et competitiveDifferentiation comptent double.
   deltaVsLastRun = overall - previousOverall.
   verdict = 'excellent' si overall >= ${EXCELLENCE} ET |deltaVsLastRun| <= ${SATURATION} (rendements décroissants) ;
   sinon 'continue' avec missingToExcellence (ce qu'il reste à combler pour exceller). Retourne MATURITY_SCHEMA.`,
  { label: 'maturity', phase: 'Maturité', schema: MATURITY_SCHEMA }
)
log(`Maturité: overall ${maturity.overall}/100 (Δ ${maturity.deltaVsLastRun}) → ${maturity.verdict.toUpperCase()}`)

// ─── Phase 6 : Écrire le document vivant + changelog append-only ─────────────
phase('Écrire')
await agent(
  `Écris le DOCUMENT VIVANT ${OUTPUT} et APPENDE au changelog ${CHANGELOG} (crée les fichiers/dossier si besoin).

   ${OUTPUT} (réécriture COMPLÈTE du fichier mais en PRÉSERVANT tout le contenu existant non touché — c'est un
   document qui grandit, pas une régénération from-scratch) :
     • En-tête : "> **HYPOTHÈSE DE CADRAGE — hors backlog F-178.** Document vivant. MAJ ${DATE}. Cible = marché
       africain (D12). NE PAS confondre avec un ordre de build. Verrou : voir CADRAGE-STRATEGIQUE-OHADA.md."
     • Bloc "## Maturité" : scores ${JSON.stringify(maturity.scores)}, overall ${maturity.overall}, Δ ${maturity.deltaVsLastRun},
       verdict **${maturity.verdict}** ${maturity.verdict === 'excellent' ? '(— fiche produit excellente, rendements décroissants, tu peux arrêter de relancer)' : `(continuer ; manque : ${JSON.stringify(maturity.missingToExcellence || [])})`}.
     • Sections : 1) Plateforme réutilisée, 2) Domaines OHADA (un Bloc par Acte uniforme), 3) Infra Afrique / transversal.
       Tables au format ${COLUMNS} (colonne Provenance = ${PROVENANCE.join('/')}), statut "Hypothèse". Données : ${JSON.stringify(curated.domains)}.
     • Table de synthèse (domaine → nb features → priorité marché → provenance dominante) + section "Doutes résiduels"
       (incohérences ${JSON.stringify(curated.incoherences || [])} + sources marché ${JSON.stringify(marketDigest.sources)}).

   ${CHANGELOG} (APPEND-ONLY — lis l'existant, AJOUTE une entrée en tête, ne réécris JAMAIS les entrées passées) :
     "## ${DATE} — run de maturation
      - overall ${maturity.overall}/100 (Δ ${maturity.deltaVsLastRun}) → ${maturity.verdict}
      - ajoutées: ${addedThisRun} features ; modifiées/justifiées et suppressions: voir détail
      - directive PO ce run: ${STRATEGIC ? JSON.stringify(STRATEGIC) : 'aucune'}
      - principaux trous marché adressés / sources"

   N'écris RIEN ailleurs que dans ${OUTPUT} et ${CHANGELOG}. Ne modifie PAS docs/PRODUCT_SPEC.md.`,
  { label: 'write', phase: 'Écrire' }
)

// ─── Récap ───────────────────────────────────────────────────────────────────
return {
  date: DATE,
  output: OUTPUT, changelog: CHANGELOG,
  maturity: { overall: maturity.overall, delta: maturity.deltaVsLastRun, verdict: maturity.verdict, scores: maturity.scores },
  excellent: maturity.verdict === 'excellent',
  featuresTotal, addedThisRun,
  appliedDeletions: curated.appliedDeletions || [],
  incoherences: curated.incoherences || [],
  marketSources: marketDigest.sources.length,
  strategicInput: STRATEGIC || null,
  budgetSpent: budget.total ? `${Math.round(budget.spent() / 1000)}k / ${Math.round(budget.total / 1000)}k` : 'illimité',
  note: maturity.verdict === 'excellent'
    ? 'Fiche produit jugée EXCELLENTE (rendements décroissants). Inutile de relancer sauf nouvelle info marché ou virage PO.'
    : `Relancer pour continuer à mûrir. Manque : ${JSON.stringify(maturity.missingToExcellence || [])}`,
}

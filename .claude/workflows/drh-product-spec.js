export const meta = {
  name: 'drh-product-spec',
  description: "Fait MÛRIR la fiche produit de LegalCase DRH (offre employeur) comme un DOCUMENT VIVANT, piloté par le MARCHÉ DRH/entreprise (besoins, concurrence RH/legal-ops, normes d'achat corporate, trous à exploiter). Enrichissement par appends justifiés (provenance + changelog append-only), modifications seulement sur info marché nouvelle ou directive PO, jamais de réécriture from-scratch. Auto-évalue sa maturité et signale un seuil d'excellence. Livrable = HYPOTHÈSE hors backlog. Ré-invocable : chaque run repart du draft existant et l'améliore.",
  whenToUse: "Cadrage LegalCase DRH (offre employeur) : 'fais mûrir / enrichis la fiche produit DRH'. Relancer plusieurs fois converge vers l'excellence. Suppose docs/drh/CADRAGE-STRATEGIQUE-DRH.md figé (D1-D12).",
  phases: [
    { title: 'Veille', detail: "recherche marché DRH/entreprise + concurrence (le driver) : besoins, normes d'achat, trous" },
    { title: 'Cartographier', detail: "lit le draft existant + signaux marché → carte des situations employeur priorisée marché (IDs préservés)" },
    { title: 'Enrichir', detail: "par domaine : APPEND de features fermant les trous marché/concurrents, modif justifiée, jamais de suppression silencieuse" },
    { title: 'Curer', detail: "dédup (1 outil = 1 situation), contradictions, cohérence avec l'existant" },
    { title: 'Maturité', detail: "score multi-axes + delta vs run précédent → verdict continue / EXCELLENT" },
    { title: 'Écrire', detail: "document vivant + changelog append-only" },
  ],
}

// ─── args ────────────────────────────────────────────────────────────────────
// args = { dateISO?, outputFile?, changelogFile?, referenceSpec?, cadrage?,
//          strategicInput?, excellenceThreshold?, saturationDelta?, domainsOverride? }
const DATE = (args && args.dateISO) || 'DATE-A-PASSER-EN-ARGS'
const OUTPUT = (args && args.outputFile) || 'docs/drh/PRODUCT_SPEC_DRH_DRAFT.md'
const CHANGELOG = (args && args.changelogFile) || 'docs/drh/PRODUCT_SPEC_DRH_CHANGELOG.md'
const REFERENCE = (args && args.referenceSpec) || 'docs/PRODUCT_SPEC.md'   // ANCRE de cohérence/qualité, PAS la cible (D12)
const CADRAGE = (args && args.cadrage) || 'docs/drh/CADRAGE-STRATEGIQUE-DRH.md'
const STRATEGIC = (args && args.strategicInput) || null
const EXCELLENCE = (args && args.excellenceThreshold) || 85
const SATURATION = (args && args.saturationDelta) || 3
const DOMAINS_OVERRIDE = (args && args.domainsOverride) || null
const COLUMNS = '| ID | Feature | Description | Provenance | Statut |'  // format figé (D3) + provenance (D12)

// ─── schémas (validation au tool-call) ───────────────────────────────────────
const MARKET_SCHEMA = {
  type: 'object', required: ['findings'],
  properties: {
    findings: { type: 'object', properties: {
      competitors: { type: 'array', items: { type: 'object', required: ['name'], properties: {
        name: { type: 'string' }, offering: { type: 'string' }, gaps: { type: 'string' }, model: { type: 'string' } } } },
      marketNeeds: { type: 'array', items: { type: 'string' } },     // besoins/douleurs réels du DRH/employeur
      norms: { type: 'array', items: { type: 'string' } },           // normes d'achat corporate (SSO, ISO/DPA, pricing, cycle)
      opportunities: { type: 'array', items: { type: 'string' } },   // trous concurrents à exploiter
    } },
    sources: { type: 'array', items: { type: 'string' } },
  },
}
const DOMAINMAP_SCHEMA = {
  type: 'object', required: ['domains'],
  properties: { domains: { type: 'array', items: {
    type: 'object', required: ['key', 'title', 'kind'],
    properties: {
      key: { type: 'string' }, title: { type: 'string' },
      kind: { enum: ['platform', 'situation-employeur', 'corporate-readiness'] },
      marketPriority: { enum: ['high', 'medium', 'low'] },
      marketDrivers: { type: 'array', items: { type: 'string' } },
      situations: { type: 'array', items: { type: 'string' } },
      existingIds: { type: 'array', items: { type: 'string' } },
    } } } },
}
const PROVENANCE = ['droit-travail', 'marche', 'concurrent-gap', 'vision-po', 'plateforme-reutilisee', 'corporate-readiness', 'hypothese']
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
        changeReason: { type: 'string' },
      } } },
    appended: { type: 'array', items: { type: 'string' } },
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

// ─── Phase 1 : Veille marché DRH & concurrence (LE DRIVER, D12) ──────────────
phase('Veille')
log('Veille marché DRH/entreprise + concurrence — le contenu cible vient du marché, pas de l\'offre avocat (D12)')
const ANGLES = [
  "Concurrents / outils existants pour la fonction DRH face au risque prud'homal et au droit social employeur (legaltech employeur, modules RH/SIRH, éditeurs droit social, cabinets en marque blanche) : périmètre, ce qu'ils font et NE font PAS, modèle éco, dossier-centric vs documentaire.",
  "Pratique & points de douleur du DRH / Directeur des Affaires Sociales (entreprise 200+ salariés) : gestion internalisée du contentieux prud'homal, chiffrage des transactions, sécurisation des ruptures/sanctions/inaptitudes, ROI attendu, ce qui est externalisé à l'avocat aujourd'hui.",
  "Normes d'achat corporate (procurement) pour un SaaS juridique en entreprise : SSO/OIDC, ISO 27001 / DPA / RGPD, audit logs, cycle de vente, paliers de prix 800-3000 €/mois, critères du DPO/Achats.",
]
const market = (await parallel(ANGLES.map((angle, i) => () =>
  agent(
    `Recherche web ciblée (marché DRH/entreprise, observation passive — traçage, pas d'engagement). Angle :
     ${angle}
     Périmètre = ${CADRAGE} (droit du travail côté employeur, D5/D6). Cite tes SOURCES (URLs réelles).
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
  `Tu construis/mets à jour la carte des domaines de la fiche produit VIVANTE LegalCase DRH (offre employeur).
   1) Lis ${OUTPUT} s'il existe : relève domaines et IDs déjà présents (à PRÉSERVER, jamais réattribuer).
   2) Lis ${CADRAGE} (§2 situations employeur, §3 corporate-readiness = features produit, §4 plateforme réutilisée)
      et ${REFERENCE} (ANCRE de cohérence/format — PAS la cible de contenu, cf. D12).
   3) Intègre les SIGNAUX MARCHÉ (le driver) : ${JSON.stringify(marketDigest)}.
   Pour chaque domaine : kind ('situation-employeur' / 'corporate-readiness' / 'platform'), marketPriority,
   marketDrivers (besoins/trous à couvrir), situations, existingIds.
   ${DOMAINS_OVERRIDE ? `Restreins-toi à : ${JSON.stringify(DOMAINS_OVERRIDE)}.` : ''}
   Respecte D5 (droit du travail côté employeur only — PAS la trajectoire juriste d'entreprise large),
   D7 (type d'acteur = attribut workspace, pas de gate), D11 (aucune infra séparée).
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
    `Tu ENRICHIS le domaine "${d.title}" (${d.kind}) de la fiche produit VIVANTE LegalCase DRH (offre employeur).
     MODE DOCUMENT VIVANT (D12) :
     • APPEND par défaut : ajoute des features qui ferment les trous MARCHÉ/concurrents de ce domaine.
     • MODIFIE une feature existante SEULEMENT avec une changeReason (info marché nouvelle ou directive PO).
     • NE SUPPRIME JAMAIS en silence : suppression → proposedDeletions avec justification.
     • PRÉSERVE les features et IDs existants : lis-les dans ${OUTPUT} (IDs de ce domaine : ${JSON.stringify(d.existingIds || [])}).
     SOURCES par ordre de PRIORITÉ (la cible est le marché DRH, pas l'offre avocat — D12) :
       1) MARCHÉ & concurrence (driver) : ${JSON.stringify({ needs: d.marketDrivers, digest: marketDigest })}
       2) Droit du travail côté EMPLOYEUR (ancrage) : situations ${JSON.stringify(d.situations || [])}
       3) Vision PO ce run : ${STRATEGIC ? JSON.stringify(STRATEGIC) : 'aucune nouvelle directive'}
       4) ${REFERENCE} = ANCRE de cohérence/qualité/format uniquement (le moteur droit du travail existe déjà côté avocat).
     Pattern dossier-centric (D3) : situation employeur → upload pièces → pipeline IA → outil décisionnel/simulateur → acte/courrier RH.
     INVARIANTS :
       - D7 : le rôle 'EMPLOYEUR' est un attribut de WORKSPACE fixé à la création, JAMAIS un sélecteur bloquant.
         L'employeur EST partie ; dossier centré-salarié ; même moteur d'outils LU côté employeur (« mon risque »).
       - D8 (anti-conflit) : cadrer CONFORMITÉ / anticipation du risque social / productivité — JAMAIS « gagner
         contre vos salariés ». Pas de feature qui « arme contre le salarié ».
       - « 1 outil décisionnel = 1 situation métier » (pas de doublon ; réutiliser l'outil avocat existant quand il existe).
       - corporate-readiness (SSO, ISO/DPA, audit logs, API) = features PRODUIT (D10), à inclure ; PAS d'infra (D11).
     Chaque feature : provenance (${PROVENANCE.join('/')}), status "Hypothèse", changeReason si nouvelle/modifiée.
     IDs stables F-DRH-<CLE>-NN ; nouvelles features = NN suivant, ne réutilise pas un NN retiré.
     Retourne DOMAINSPEC_SCHEMA.`,
    { label: `enrich:${d.key}`, phase: 'Enrichir', schema: DOMAINSPEC_SCHEMA }
  )
}))).filter(Boolean)
const addedThisRun = enriched.reduce((s, e) => s + (e.appended?.length || 0), 0)
const featuresTotal = enriched.reduce((s, e) => s + (e.features?.length || 0), 0)
log(`Enrichi: ${enriched.length} domaines, ${featuresTotal} features (+${addedThisRun} ajoutées ce run)`)

// ─── Phase 4 : Curer (barrière) ──────────────────────────────────────────────
phase('Curer')
const curated = await agent(
  `Passe de CURATION sur l'ensemble des domaines enrichis : ${JSON.stringify(enriched)}.
   1) Dédoublonne — invariant « 1 outil décisionnel = 1 situation métier » ; quand un outil avocat existant
      couvre déjà la situation, le RÉUTILISER côté employeur plutôt qu'en créer un nouveau.
   2) Résous les contradictions entre domaines.
   3) Vérifie la COHÉRENCE avec LegalCase existant (${REFERENCE}) : pattern, nommage, statut, philosophie produit,
      et l'invariant anti-conflit D8 (rien qui « arme contre le salarié »).
   4) Liste les incohérences/risques résiduels. N'applique une suppression qu'avec justification (appliedDeletions).
   Préserve les IDs. Retourne CURATION_SCHEMA.`,
  { label: 'curation', phase: 'Curer', schema: CURATION_SCHEMA }
)
log(`Curé: ${curated.domains?.length || 0} domaines, ${curated.incoherences?.length || 0} incohérences, ${curated.appliedDeletions?.length || 0} suppressions justifiées`)

// ─── Phase 5 : Maturité & seuil d'excellence (D12) ───────────────────────────
phase('Maturité')
const maturity = await agent(
  `Évalue la MATURITÉ de la fiche produit DRH (sections curées : ${JSON.stringify(curated.domains)}).
   Récupère le score du run PRÉCÉDENT en lisant le bloc "## Maturité" dans ${OUTPUT} (sinon previousOverall=0).
   Note 0-100 : marketCoverage (couverture des besoins DRH), competitiveDifferentiation (avantage vs outils RH/legal-ops),
   legalGrounding (justesse droit du travail côté employeur), coherenceWithExisting (cohérence + respect anti-conflit D8),
   completeness (exhaustivité du périmètre risque social employeur).
   overall = moyenne PONDÉRÉE en faveur du marché : marketCoverage et competitiveDifferentiation comptent double.
   deltaVsLastRun = overall - previousOverall.
   verdict = 'excellent' si overall >= ${EXCELLENCE} ET |deltaVsLastRun| <= ${SATURATION} (rendements décroissants) ;
   sinon 'continue' avec missingToExcellence. Retourne MATURITY_SCHEMA.`,
  { label: 'maturity', phase: 'Maturité', schema: MATURITY_SCHEMA }
)
log(`Maturité: overall ${maturity.overall}/100 (Δ ${maturity.deltaVsLastRun}) → ${maturity.verdict.toUpperCase()}`)

// ─── Phase 6 : Écrire le document vivant + changelog append-only ─────────────
phase('Écrire')
await agent(
  `Écris le DOCUMENT VIVANT ${OUTPUT} et APPENDE au changelog ${CHANGELOG} (crée les fichiers/dossier si besoin).

   ${OUTPUT} (document qui GRANDIT, on PRÉSERVE l'existant non touché — pas de régénération from-scratch) :
     • En-tête : "> **HYPOTHÈSE DE CADRAGE — hors backlog F-178.** Document vivant. MAJ ${DATE}. Cible = marché
       DRH/employeur (D12). Invariant anti-conflit D8 (conformité/productivité, jamais 'contre les salariés').
       NE PAS confondre avec un ordre de build. Verrou : voir CADRAGE-STRATEGIQUE-DRH.md."
     • Bloc "## Maturité" : scores ${JSON.stringify(maturity.scores)}, overall ${maturity.overall}, Δ ${maturity.deltaVsLastRun},
       verdict **${maturity.verdict}** ${maturity.verdict === 'excellent' ? '(— fiche produit excellente, rendements décroissants, tu peux arrêter de relancer)' : `(continuer ; manque : ${JSON.stringify(maturity.missingToExcellence || [])})`}.
     • Sections : 1) Plateforme & moteur réutilisés, 2) Situations employeur (un Bloc par domaine), 3) Corporate-readiness (features produit).
       Tables au format ${COLUMNS} (Provenance = ${PROVENANCE.join('/')}), statut "Hypothèse". Données : ${JSON.stringify(curated.domains)}.
     • Table de synthèse (domaine → nb features → priorité marché → provenance dominante) + section "Doutes résiduels"
       (incohérences ${JSON.stringify(curated.incoherences || [])} + sources marché ${JSON.stringify(marketDigest.sources)} + risque déonto D8 à valider terrain).

   ${CHANGELOG} (APPEND-ONLY — lis l'existant, AJOUTE une entrée en tête, ne réécris JAMAIS les entrées passées) :
     "## ${DATE} — run de maturation
      - overall ${maturity.overall}/100 (Δ ${maturity.deltaVsLastRun}) → ${maturity.verdict}
      - ajoutées: ${addedThisRun} features ; modifiées/justifiées et suppressions: voir détail
      - directive PO ce run: ${STRATEGIC ? JSON.stringify(STRATEGIC) : 'aucune'}"

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
    ? 'Fiche produit DRH jugée EXCELLENTE (rendements décroissants). Inutile de relancer sauf nouvelle info marché ou virage PO. Rappel verrou : valider la propension à payer DRH + la réaction des avocats (D8) avant tout engagement.'
    : `Relancer pour continuer à mûrir. Manque : ${JSON.stringify(maturity.missingToExcellence || [])}`,
}

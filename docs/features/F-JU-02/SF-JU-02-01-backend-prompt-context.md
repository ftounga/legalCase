# Mini-spec — F-JU-02 / SF-JU-02-01 Backend service + intégration prompt F-98

## Identifiant
`F-JU-02 / SF-JU-02-01`

## Feature parente
`F-JU-02` — Citations jurisprudentielles automatiques dans les conclusions générées (pont F-JU-01 → F-98)

## Statut
`draft`

## Date de création
2026-05-22

## Branche Git
`feat/SF-JU-02-01-backend-prompt-context`

---

## Objectif

Quand l'avocat déclenche la génération de conclusions F-98 sur un dossier, **injecter automatiquement** dans le prompt une nouvelle section `JURISPRUDENCE APPLICABLE PAR OUTIL` qui agrège les arrêts mappés (F-JU-01 `tool_jurisprudence_mappings`) des outils décisionnels effectivement utilisés sur ce dossier — sans aucun geste avocat.

---

## Comportement attendu

### Cas nominal

1. L'avocat utilise N outils décisionnels sur son dossier (ex. F-DT-30 indemnité Macron + F-DT-08 validité licenciement)
2. Il clique « générer conclusions »
3. `CaseConclusionCommandService` prépare le prompt via `CaseConclusionPromptBuilder.buildUserMessage`
4. **Nouveau** : `ConclusionsJurisprudenceContext.collectForCaseFile(caseFileId)` détecte les outils utilisés via les beans `ToolUsageContributor` et retourne une liste agrégée d'arrêts mappés
5. Le prompt inclut une nouvelle section `=== JURISPRUDENCE APPLICABLE PAR OUTIL ===` listant les arrêts (`arret_ref` + `chapeau_officiel` + outil source)
6. Claude génère les conclusions en intégrant ces arrêts dans le corps argumentatif
7. **Coexistence F-242** : la section existante `JURISPRUDENCE À L'APPUI` (saisie manuelle avocat) reste séparée. Le `JURISPRUDENCE_GUARD` est étendu : Claude peut citer les arrêts de **l'une OU l'autre** section.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Aucun outil utilisé sur le dossier | Section absente du prompt (no-op) |
| Aucun `ToolUsageContributor` enregistré (V1) | Section absente — comportement neutre |
| `ToolJurisprudenceService.findByToolAndBranch` lève exception | Skip l'outil + log WARN |
| Doublon d'arrêt entre outils (même `arret_ref`) | Déduplication automatique |

---

## Analyse de cohérence transversale

- [x] **Service réutilisable** : `ConclusionsJurisprudenceContext` consommé par F-98 conclusions ; en V2 réutilisable par F-186 export PDF synthèse (SF-JU-02-02)
- [x] **Préoccupations transversales** : aucune (additif pur sur prompt F-98)

## Conformité F-IA-04 / Pré-fill IA
- [x] **Non applicable** (SF backend, pas d'outil décisionnel modifié)

---

## Critères d'acceptation

- [ ] **CA-01** — Interface `ToolUsageContributor` (1 méthode `Optional<ToolUsage> detectUsage(UUID caseFileId)` + `String toolId()`) que chaque outil décisionnel implémente pour déclarer son usage sur un dossier. V1 : aucun outil n'implémente, le détecteur retourne vide.
- [ ] **CA-02** — Bean `ToolUsageAggregator` (component) qui agrège les `ToolUsageContributor` et expose `List<ToolUsage> detectAll(UUID caseFileId)`.
- [ ] **CA-03** — Service `ConclusionsJurisprudenceContext.collectForCaseFile(caseFileId)` qui (a) appelle l'aggregator, (b) pour chaque outil utilisé appelle `ToolJurisprudenceService.findByToolAndBranch`, (c) déduplique par `arret_ref`, (d) retourne `List<ToolJurisprudenceCitationByTool>`.
- [ ] **CA-04** — Extension `ConclusionPromptInput` avec nouveau champ `toolJurisprudenceByTool` (List<ToolJurisprudenceCitationByTool>).
- [ ] **CA-05** — Méthode `appendToolJurisprudenceCitations(sb, list)` dans `CaseConclusionPromptBuilder` qui produit la section `=== JURISPRUDENCE APPLICABLE PAR OUTIL ===`.
- [ ] **CA-06** — Section absente du prompt si liste vide (silence > placeholder).
- [ ] **CA-07** — `JURISPRUDENCE_GUARD` étendu pour autoriser citation depuis l'une OU l'autre section.
- [ ] **CA-08** — `CaseConclusionService.assemblePrompt` appelle `ConclusionsJurisprudenceContext.collectForCaseFile` et passe la liste dans `ConclusionPromptInput`.
- [ ] **CA-09** — Tests UT : aggregator vide → liste vide ; 2 outils utilisés → 2 entrées ; déduplication d'arrêt par `arret_ref` ; contributor exception → skip + continue ; prompt builder produit section attendue.

---

## Périmètre

### Hors scope V1
- ❌ Implémentation `ToolUsageContributor` pour les ~80 outils — chaque outil le fait au moment où il a des mappings effectifs (vague future ; pour V1 le service retourne vide donc no-op)
- ❌ Export PDF synthèse F-186 → SF-JU-02-02
- ❌ Personnalisation de la citation dans le prompt (style avocat) → V2

---

## Technique

### Tables impactées
- Aucune (lecture seule `tool_jurisprudence_mappings`)

### Migration Liquibase
- [x] Non applicable

### Classes Java introduites
- `ToolUsage` (record) : `String toolId, String brancheCalculId`
- `ToolUsageContributor` (interface)
- `ToolUsageAggregator` (component)
- `ToolJurisprudenceCitationByTool` (record) : `String toolId, List<ToolJurisprudenceCitationResponse> citations`
- `ConclusionsJurisprudenceContext` (service)
- Extension `CaseConclusionPromptBuilder` + `ConclusionPromptInput`

---

## Plan de test

### Tests UT
- `ToolUsageAggregatorTest` — vide / N contributors / null returns / exception skipped
- `ConclusionsJurisprudenceContextTest` — aggregator vide → vide ; N outils → N entrées ; déduplication par arret_ref ; ToolJurisprudence exception → skip + log
- `CaseConclusionPromptBuilderTest` (extension) — section présente si non vide, absente sinon, format texte attendu

### IT
- [x] Non applicable (couvert par UT + pattern miroir F-242)

---

## Notes
1. **`ToolUsageContributor` non implémenté en V1 par les outils** — même pattern que `ToolBranchRegistry` SF-JU-01-03. Le service retourne vide jusqu'à ce que les outils s'instrumentent.
2. **Détection de l'outil utilisé** : V1 = bean Spring par outil. V2 possible = scan automatique des tables `*_analyses` du dossier via réflexion (générique) — différé.
3. **Pas de modification du flux de génération F-98** — additif pur dans le builder de prompt.

### Coût estimé
- ~1,5 j dev backend.

# SF-FA-17-02 — Frontend partage judiciaire (art. 1364 CPC + 840+ Cciv)

## Objectif

Exposer dans le panel F-IA-04 un composant Angular `<app-partage-judiciaire-section>` qui consomme les endpoints POST/GET `/api/v1/case-files/{id}/partage-judiciaire-analysis` (contrat figé dans SF-FA-17-01, mergé PR #636) pour afficher un formulaire d'analyse + un bandeau verdict.

## Contrat API (importé de SF-FA-17-01)

### POST `/api/v1/case-files/{caseFileId}/partage-judiciaire-analysis`

Body :
- `pvDifficultesEtabli: boolean`
- `tentativeAmiableEpuiseuee: boolean`
- `typeBienIndivision: 'IMMEUBLE_DIVISIBLE' | 'IMMEUBLE_INDIVISIBLE' | 'MEUBLES_DIVERS' | 'MIXTE'`
- `nombreCoindivisaires: number` (≥ 2)
- `desaccordMotive: boolean`
- `valeurEstimeeBiensEur: number` (≥ 0, BigDecimal)

Réponse :
- `caseFileId`, `verdictRecevabilite: 'ELEVEE' | 'MOYENNE' | 'FAIBLE'`
- `dureeProcedureMois: number` (6-18)
- `fraisEstimesEur: number`
- `risqueLicitation: boolean`
- `scoreEligibilite: number`
- `baseJuridique`, `formule`, `messages: string[]`, `country`

GET retourne la dernière analyse (404 si aucune).

## Comportement nominal

1. Au mount + `workspaceCountry === 'FRANCE'` : GET → si 200 affiche le bandeau verdict ; si 404 reste en mode formulaire.
2. Si `workspaceCountry === 'BELGIQUE'` : pas d'appel HTTP — bannière info "Outil français uniquement, équivalent CJ art. 1207+ au backlog".
3. Avant POST : si `aiData?.dureeMariageAnnees` etc., utilisation du pré-fill IA gracieux (no-op si valeurs absentes — voir détail).
4. POST → bandeau verdict navy/or/rouge + chip risque licitation + `CaseDashboardRefreshService.triggerRefresh()`.
5. Erreur backend → MatSnackBar rouge.

## Pré-fill IA (OBLIGATOIRE)

Sources dans `FamilleExtractedData` (ajout de 2 champs optionnels — intégrés au model partagé) :
- `pvDifficultesEtablisDetected?: boolean | null` → pré-fill `pvDifficultesEtabli`.
- `tentativeAmiableEpuiseueeDetected?: boolean | null` → pré-fill `tentativeAmiableEpuiseuee`.
- `nombreCoindivisairesDetecte?: number | null` → pré-fill `nombreCoindivisaires`.
- `valeurBiensIndivisionEur?: number | null` → pré-fill `valeurEstimeeBiensEur`.

Pour V1, seuls les champs **présents** dans le pipeline IA `FamilleExtractedData` actuel sont pré-remplis (no-op gracieux sinon). Si aucun champ pré-remplissable n'est extrait par l'IA pour V1, le pré-fill se borne aux 2 booléens (`pvDifficultesEtabli`, `tentativeAmiableEpuiseuee`) et `nombreCoindivisaires` quand l'IA les détecte — pas FAIL si non extrait.

Provenance signal `provenancePvDifficultes`, `provenanceTentativeAmiable`, `provenanceNombreCoindivisaires`, `provenanceValeurBiens` ; badge "Pré-rempli depuis l'analyse" + handler `onXxxChange` qui remet à `null`.

## Validation F-IA-03 (OBLIGATOIRE)

Champ `coherenceAlerts = computed<Partial<Record<PartageJudiciaireAlertField, ...>>>()` typé sur `'PV_DIFFICULTES' | 'TENTATIVE_AMIABLE'` :
- `PV_DIFFICULTES` : alerte si avocat saisit `false` alors que `aiData.pvDifficultesEtablisDetected === true` OU F-96 critère `PARTAGE_JUDICIAIRE_PV` expectedValue `OUI`.
- `TENTATIVE_AMIABLE` : alerte si avocat saisit `false` alors que IA / F96 / QUESTION_IA disent l'inverse.
- Helper partagé `CoherenceAlertBuilder` obligatoire.
- Hiérarchie F96 > QUESTION_IA > IA > PIECE_MANQUANTE.

## Critères d'acceptation vérifiables

1. `FRANCE` + GET 200 → mode résultat hydraté.
2. `FRANCE` + GET 404 → mode formulaire.
3. `BELGIQUE` → aucun appel HTTP, bannière info BE visible.
4. POST nominal → snackbar succès + bandeau verdict + `triggerRefresh()`.
5. Erreur backend → snackbar rouge.
6. Verdict `ELEVEE` → classe CSS `--info` (navy/or).
7. Verdict `MOYENNE` → classe CSS `--warning` (or).
8. Verdict `FAIBLE` → classe CSS `--critical` (rouge).
9. `risqueLicitation = true` → chip rouge visible.
10. Pré-fill `pvDifficultesEtabli` depuis `aiData.pvDifficultesEtablisDetected = true` → signal provenance `'IA'`.
11. Modification manuelle après pré-fill efface la provenance.
12. Divergence IA `pvDifficultesEtablisDetected = true` vs avocat `false` → alerte `PV_DIFFICULTES`.
13. Convergence IA + saisie avocat → pas d'alerte.
14. Multi-sources F96 + IA convergents → alerte `source: 'MULTI'`.
15. `formValid()` false si `nombreCoindivisaires < 2` ou `valeurEstimeeBiensEur < 0`.
16. Entrée `TOOL_REGISTRY` `'F-FA-17-partage-judiciaire'` mappe sur `PartageJudiciaireSectionComponent` avec `aiData = synthesis?.familleExtractedData`.
17. JetBrains Mono pour `baseJuridique`, `formule`, `fraisEstimesEur`.

## Plan de test (Jest, ≥ 10 cibles)

- **Component** (`partage-judiciaire-section.component.spec.ts`, ≥ 14 tests) :
  - Gate FR / BE (2)
  - GET 200 / 404 (2)
  - Pré-fill (3 — pvDifficultes, tentativeAmiable, no-op)
  - F-IA-03 (3 — divergence simple, convergence, MULTI F96+IA)
  - POST + erreur (3)
  - Form validation (1)
- **Panel registry** (`decisional-tools-panel.component.spec.ts`) : entrée `'F-FA-17-partage-judiciaire'` → `PartageJudiciaireSectionComponent` (1).

## Composants impactés

- **Nouveaux** :
  - `frontend/src/app/core/models/partage-judiciaire.model.ts`
  - `frontend/src/app/core/services/partage-judiciaire.service.ts`
  - `frontend/src/app/case-files/partage-judiciaire-section/` (4 fichiers)
  - `docs/features/F-FA-17/SF-FA-17-02-frontend-partage-judiciaire.md`
- **Modifiés** :
  - `frontend/src/app/core/models/divorce-accepte.model.ts` (ajout champs `FamilleExtractedData` pré-fill — non bloquant si absent)
  - `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (entrée registry)

## Hors périmètre

- Backend (SF-FA-17-01 mergé PR #636).
- Belgique (CJ art. 1207+ — backlog jumeau).
- Adaptation prompt IA pour extraire `pvDifficultesEtablisDetected` etc. — feature future si jugée utile.

## Impact par domaine métier

Feature **sensible au domaine**, gate cohérent avec le backend :
- **Droit du travail / Immigration** : non applicable — outil masqué côté panel via règle de visibilité (priority 85, FAMILLE only).
- **Droit famille FR** : couvert ici.
- **Droit famille BE** : bannière info BE — équivalent juge de paix art. 1207+ CJ au backlog.

## Nouveau pattern UI ou service partagé

Aucun nouveau composant partagé / service / directive transversal. Réutilisation stricte de `CoherenceAlertBuilder` + `CoherencePopoverTriggerDirective` + `LegalCitationsPipe` + pattern visuel `protection-rp-section`.

## Analyse de cohérence transversale

- **Outils décisionnels existants** : pattern jumeau direct = `protection-rp-section` (FR-only, scoring, F-IA-03 mono-champ — étendu ici à 2 champs). Pattern voisin = `pse-section` (FR-only). Aucun outil pré-existant ne couvre la procédure de partage judiciaire. F-FA-04 (Indivision) et F-FA-05 (Partage immobilier amiable) sont distincts (étape amiable).
- **Patterns UI** : palette navy/or/rouge identique à `protection-rp` ; `<input type="date">` natif (mais ici aucune date dans le contrat, donc N/A) ; chip risque licitation utilise classe `protection-rp-chip--critical` réadaptée localement.

## Préoccupations transversales

- **Auth / Principal** : aucun changement.
- **Workspace context** : aucun changement (`workspaceCountry` lu en input).
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun changement (composant intégré dans le panel F-IA-04).
- **Outil décisionnel métier** : un nouvel outil = une situation = partage **judiciaire** (≠ amiable F-FA-05).

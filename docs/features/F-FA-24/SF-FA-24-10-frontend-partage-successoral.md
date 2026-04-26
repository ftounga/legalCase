# Mini-spec — F-FA-24 / SF-FA-24-10 Frontend partage successoral

## Identifiant

`F-FA-24 / SF-FA-24-10`

## Feature parente

`F-FA-24` — Droit des successions.

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-10-frontend-partage-successoral`

---

## Objectif

Composant Angular `<app-partage-successoral-section>` qui consomme l'API
SF-FA-24-09 (PR #680, backend mergé) et permet à l'avocat d'évaluer la
**modalité de partage successoral** (FR — art. 815-840 Cciv + 1364 CPC) :
amiable / judiciaire / partiel, avec verdict de recevabilité, mode
recommandé, délai d'instruction, frais estimés et risque de licitation.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre le panel d'outils décisionnels du dossier (F-IA-04).
2. Si dossier FR + `DROIT_FAMILLE` → outil affiché avec
   `tool_id = 'F-FA-24-partage-successoral'`.
3. À l'ouverture : GET `/api/v1/case-files/{id}/partage-successoral-analysis`.
   - 200 → mode résultat (banner + frais + délai + chips).
   - 404 → mode formulaire.
4. Mode formulaire : radio mode + nombreCoheritiers + booleans
   (consentementsTous, presenceImmeubles, accordsValuation,
   desaccordPersistant) + dateDeces + valeurMasseEur (optionnel).
5. POST → 200 → bascule en mode résultat + snackbar succès +
   `CaseDashboardRefreshService.triggerRefresh()`.

### Pré-fill IA (RÈGLE FONDAMENTALE)

- `aiData.dateDecesDetectee` (existante depuis SF-FA-24-08) →
  `dateDeces` + provenance IA.
- `aiData.nombreCoheritiersDetecte` (nouveau champ optionnel) →
  `nombreCoheritiers` + provenance IA.
- `aiData.modePartageDemandeDetecte` (nouveau champ optionnel,
  parse robuste : `AMIABLE/JUDICIAIRE/PARTIEL` → enum complet) →
  `modePartageDemande` + provenance IA.
- Provenance signal par champ + badge UI `auto_awesome` "Pré-rempli
  depuis l'analyse" + handler qui efface la provenance au changement
  manuel.

### Validation F-IA-03 au changement (RÈGLE FONDAMENTALE)

Champs audités (3) :
- `MODE_PARTAGE` : divergence sur la modalité (IA vs avocat).
- `CONSENTEMENTS` : divergence sur le consentement de tous (F-96 +
  question IA).
- `PRESENCE_IMMEUBLES` : divergence sur la présence d'immeubles
  (impacte la procédure : notaire obligatoire si oui).

Hiérarchie F-96 > QUESTION_IA > IA > PIECE_MANQUANTE via
`CoherenceAlertBuilder` partagé. Source `MULTI` si convergence ≥ 2
sources. Popover `<app-coherence-popover-trigger>` câblé.

### Cas d'erreur

- BE → bannière info "Outil français uniquement" (art. 815-840 Cciv +
  1364 CPC) — équivalent CJ art. 1207 BE en feature jumelle backlog.
- POST 400 → snackbar rouge avec message backend.
- POST 404 → snackbar rouge.

### Gate pays

Si `workspaceCountry !== 'FRANCE'` :
- Aucun appel HTTP au `ngOnInit`.
- Bannière info BE affichée à la place du formulaire.

---

## Critères d'acceptation vérifiables

1. Composant standalone `<app-partage-successoral-section>` créé sous
   `frontend/src/app/case-files/partage-successoral-section/`.
2. 4 fichiers : `.ts`, `.html`, `.scss`, `.spec.ts`.
3. Modèle TypeScript dans `frontend/src/app/core/models/partage-successoral.model.ts`
   reflétant exactement le contrat backend.
4. Service `frontend/src/app/core/services/partage-successoral.service.ts`
   avec `calculate(caseFileId, request)` POST + `get(caseFileId)` GET.
5. Entrée TOOL_REGISTRY `F-FA-24-partage-successoral` dans
   `decisional-tools-panel.component.ts` avec context complet
   (caseFileId + workspaceCountry + aiData + procedureChecks +
   aiQuestions + piecesManquantes).
6. Pré-fill IA fonctionnel sur ≥ 3 champs (mode + dateDeces +
   nombreCoheritiers).
7. Validation F-IA-03 sur ≥ 3 champs (MODE_PARTAGE, CONSENTEMENTS,
   PRESENCE_IMMEUBLES) via `CoherenceAlertBuilder`.
8. Gate FR + bannière info BE (pas masquage silencieux).
9. Tests Jest ≥ 12 — gate, prefill, F-IA-03, POST, GET, validation.
10. Self-check 5/5 : palette navy/or, datepicker `<input type="date">`,
    pas de MatDatepicker, `MatSnackBar` (pas alert/confirm),
    `CaseDashboardRefreshService.triggerRefresh()` après POST,
    JetBrains Mono pour `formule` et `baseJuridique`.

---

## Plan de test

### Unitaires (Jest) — ≥ 12 tests

1. FRANCE → isFrance() true, GET appelé au ngOnInit.
2. BELGIQUE → bannière info, aucun appel HTTP au ngOnInit.
3. GET 200 → mode résultat hydraté, provenance reset.
4. GET 404 → mode formulaire, prefill exécuté.
5. Pré-fill IA : modePartageDemande + dateDeces + nombreCoheritiers.
6. Pré-fill sans aiData → no-op.
7. onModeChange efface provenance IA.
8. formValid : tous champs requis OK → true.
9. formValid : mode null → false.
10. formValid : nombreCoheritiers < 2 → false.
11. POST envoie le body attendu + résultat hydraté + snackbar succès.
12. POST erreur 400 → snackbar rouge.
13. coherenceAlerts.MODE_PARTAGE présente si IA divergente de saisie.
14. coherenceAlerts.PRESENCE_IMMEUBLES F96 + IA → MULTI.
15. coherenceAlerts vides après calcul (showForm=false).
16. ngOnChanges(aiData) post-mount rafraîchit le pré-fill.
17. ngOnChanges(aiData) ne réécrase pas la saisie avocat.
18. verdictBannerClass : ELEVEE → info, MOYENNE → warn, FAIBLE → critical.
19. modeLabel + verdictLabel couvrent les enums.
20. toggleCollapse + editMode fonctionnent.

### Tests d'intégration

Hors périmètre (couvert par l'IT backend SF-FA-24-09).

---

## Tables / endpoints / composants impactés

### Frontend
- Nouveau composant : `partage-successoral-section/` (4 fichiers).
- Nouveau modèle : `core/models/partage-successoral.model.ts`.
- Nouveau service : `core/services/partage-successoral.service.ts`.
- Mise à jour TOOL_REGISTRY : `decisional-tools-panel.component.ts`.
- Extension `FamilleExtractedData` (3 champs optionnels nouveaux :
  `modePartageDemandeDetecte`, `nombreCoheritiersDetecte`,
  `dateDecesDetectee` — ce dernier déjà partagé avec d'autres SF
  F-FA-24).

### Endpoints consommés

- `POST /api/v1/case-files/{caseFileId}/partage-successoral-analysis`
- `GET /api/v1/case-files/{caseFileId}/partage-successoral-analysis`

### Backend

Hors périmètre (mergé via SF-FA-24-09 PR #680).

---

## Hors périmètre

- Backend (mergé via SF-FA-24-09 PR #680).
- Composant BE équivalent (CJ art. 1207 — feature jumelle au backlog).
- Calcul des soultes détaillé (possible feature future).
- Procédure d'attribution préférentielle (couverte par F-FA-17).

---

## Contrat API (importé de SF-FA-24-09)

### Request body (POST)

```json
{
  "modePartageDemande": "PARTAGE_AMIABLE | PARTAGE_JUDICIAIRE | PARTAGE_PARTIEL",
  "nombreCoheritiers": 3,
  "consentementsTous": true,
  "presenceImmeubles": false,
  "accordsValuation": true,
  "desaccordPersistant": false,
  "dateDeces": "2025-06-15",
  "valeurMasseEur": 350000
}
```

### Response (200)

```json
{
  "caseFileId": "uuid",
  "verdictRecevabilite": "ELEVEE | MOYENNE | FAIBLE",
  "modeRecommande": "PARTAGE_AMIABLE | PARTAGE_JUDICIAIRE | PARTAGE_PARTIEL",
  "basculeMode": false,
  "scoreEligibilite": 85,
  "delaiInstructionMois": 3,
  "fraisEstimesPct": 0.015,
  "fraisEstimesEur": 5250,
  "risqueLicitation": false,
  "baseJuridique": "Art. 815-840 Cciv + 1364 CPC",
  "formule": "Mode demandé ... → mode recommandé ...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

---

## Analyse de cohérence transversale

### Outils décisionnels du même domaine (DROIT_FAMILLE FR)
- F-FA-17 partage judiciaire : composant distinct (procédure judiciaire
  stricto sensu — cet outil traite le **choix de la modalité**).
- F-FA-24-02 dévolution légale : pré-requis amont (qui hérite ?).
- F-FA-24-08 réserve héréditaire : distinct (calcul réserve / quotité).

### Pays
- FRANCE seul — bannière info BE qui pointe vers la feature jumelle
  backlog.

### UI patterns
- Aucun nouveau pattern. Suit le pattern canonique
  `donation-section` (PR #678) :
  - palette navy/or (rouge réservé au verdict critique uniquement),
  - `<input type="date">` (pas MatDatepicker),
  - `MatSnackBar` (pas alert/confirm),
  - `CoherencePopoverTriggerDirective` + `CoherenceAlertBuilder`,
  - JetBrains Mono pour `formule` et `baseJuridique`,
  - Inter pour le reste.

### Nouveau pattern UI ou service partagé
- Aucun composant partagé créé.

---

## Impact par domaine métier

- **Droit du travail** : non applicable (succession = famille).
- **Immigration** : non applicable.
- **Famille FR** : pertinent (cette SF).
- **Famille BE** : feature jumelle au backlog (CJ art. 1207).

## Parité des domaines métier

Niveau 5/6 (scoring + comparateur de modalités) — domaine-spécifique
famille uniquement, parité réduite à la dimension pays (FR vs BE,
backlog).

---

## Préoccupations transversales

- Auth / Principal : aucun changement.
- Workspace context : gate FRANCE + bannière info BE.
- Plans / limites : non applicable.
- Navigation / routing : aucune.
- Outil décisionnel métier : oui — composant section affiché par
  panel F-IA-04 via tool_id `F-FA-24-partage-successoral`.

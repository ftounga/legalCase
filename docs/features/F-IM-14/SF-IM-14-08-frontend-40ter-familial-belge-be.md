# Mini-spec — F-IM-14 / SF-IM-14-08 40ter familial Belge BE — FRONTEND

## Objectif

Section Angular `<app-belgian-40ter-section>` — formulaire + verdict pour l'outil
"Regroupement familial d'un Belge" (art. 40ter Loi 15/12/1980), consommant
l'API `POST/GET /api/v1/case-files/{id}/belgian-40ter` (PR #511).

Contrat importé de SF-IM-14-04 (backend mergée).

## Comportement nominal

1. Avocat sur dossier BE ouvre l'outil (priorité 67, ALWAYS_ON).
2. `ngOnInit` → GET `/belgian-40ter`.
   - 200 → résultat affiché, formulaire fermé.
   - 404 → mode formulaire avec pré-fill IA optionnel.
3. Formulaire :
   - `lienFamilial` (mat-select 5 valeurs : `CONJOINT`, `PARTENAIRE_LEGAL_ENREGISTRE`,
     `DESCENDANT_MINEUR`, `DESCENDANT_MAJEUR_CHARGE`, `ASCENDANT_CHARGE_HANDICAP`)
   - `regroupantBelge` (slide-toggle, défaut true)
   - `revenusMensuelsNetsEur` (input number, requis, > 0)
   - `seuil120PctRisEur` (input number, défaut 1740, > 0)
   - 3 slide-toggles `assuranceMaladie`, `logementSuffisant`, `menaceOrdrePublic`
   - `dateDepotDemande` (`<input type="date">` natif, optionnel)
4. Submit → POST → résultat persisté + `triggerRefresh()`.
5. Affichage résultat :
   - Bandeau verdict (navy/or/rouge classique : ELEVEE=succès, MOYENNE=warning, FAIBLE=danger)
   - Score `scoreGlobal/100` en grand
   - Carte "Différentiel revenus" (badge vert si ≥ 0, rouge si < 0)
   - 6 check-items (lien / regroupant Belge / revenus / assurance / logement / ordre public)
   - Bonus différentiel +10 si > 20% du seuil
   - Chips `criteresNonRemplis`
   - `messages` cités avec `LegalCitationsPipe`
   - `baseJuridique` + `formule` en JetBrains Mono

## Comportement erreur

- POST 4xx/5xx → MatSnackBar `panelClass: 'snack-error'`, `analyzing` reset.
- GET 404 → mode formulaire (silencieux).
- workspaceCountry !== BELGIQUE → bannière info BE-only, pas de form.
- `revenusMensuelsNetsEur` vide ou ≤ 0 → submit désactivé.
- `seuil120PctRisEur` ≤ 0 → submit désactivé.
- `dateDepotDemande` dans le futur → submit désactivé.

## Critères d'acceptation vérifiables

- [x] Standalone component `BelgianFortyTerSectionComponent` (selector `app-belgian-40ter-section`)
- [x] Service `Belgian40terService` consomme `POST/GET /api/v1/case-files/{id}/belgian-40ter`
- [x] Modèle `belgian-40ter.model.ts` (`Belgian40terRequest`, `Belgian40terResponse`,
      `LienFamilial`, `VerdictProbabilite`, `LIENS_FAMILIAUX`)
- [x] `TOOL_REGISTRY` entrée `F-IM-14-40ter-familial-belge-be` (symétrique aux autres)
- [x] Gate `workspaceCountry === 'BELGIQUE'` (bannière info sinon)
- [x] Pré-fill IA gracieux : lienFamilial, regroupantBelge, revenusMensuelsNetsEur,
      dateDepotDemande (no-op si champ absent du `aiData`)
- [x] Cohérence palette : navy/or/rouge classique, `<input type="date">`, MatSnackBar
- [x] `triggerRefresh()` après POST succès
- [x] `LegalCitationsPipe` sur `messages` et `baseJuridique`
- [x] JetBrains Mono pour `baseJuridique` et `formule`
- [x] Tests Jest ≥ 12 (mount, validators, submit ok pos/neg différentiel, error,
      pré-fill IA, badge effacé, gate FR/BE, collapse, différentiel +/-)

## Plan de test minimal

### Unitaires (Jest, ≥ 12)
1. `mount` — composant créé, signals à défaut.
2. `BELGIQUE → isBelgium() true, GET appelé au ngOnInit`.
3. `FRANCE → bannière info, pas d'appel HTTP`.
4. `formValid false` si `revenusMensuelsNetsEur` vide ou ≤ 0.
5. `formValid false` si `seuil120PctRisEur` ≤ 0.
6. `formValid true` cas nominal.
7. `analyze() POST` — body envoyé conforme + résultat persisté + snackbar OK.
8. `analyze() POST` — différentiel positif → `differentielRevenus > 0`.
9. `analyze() POST` — différentiel négatif → `differentielRevenus < 0`.
10. `analyze() error` → snackbar 'snack-error' + analyzing reset.
11. `prefillFromAi` — 4 champs IA → 4 provenance 'IA'.
12. `prefillFromAi` — lienFamilial hors whitelist → skip gracieux.
13. `onXxxChange` → provenance IA effacée.
14. `coherence alert` (placeholder F-IA-03) — IA et avocat divergent → alerte.
15. `bannerClass` — ELEVEE/MOYENNE/FAIBLE → classes CSS conformes.
16. `toggleCollapse` — inverse l'état.
17. `differentiel positif` → badge vert ; `négatif` → badge rouge.

### Intégration
Backend déjà testé (PR #511, IT). Frontend = mock HttpTestingController.

### Isolation workspace
Le `workspaceCountry` est passé en input. Aucun appel direct workspaceService.

## Tables / endpoints / composants impactés

- **Endpoints** : `POST/GET /api/v1/case-files/{caseFileId}/belgian-40ter` (déjà mergés)
- **Composants nouveaux** :
  - `frontend/src/app/core/models/belgian-40ter.model.ts`
  - `frontend/src/app/core/services/belgian-40ter.service.ts`
  - `frontend/src/app/case-files/belgian-40ter-section/*.{ts,html,scss,spec.ts}`
- **Composants modifiés** :
  - `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
    (ajout entrée TOOL_REGISTRY `F-IM-14-40ter-familial-belge-be`)

## Hors périmètre

- Pas de modification du backend (PR #511 mergée).
- Pas de modification de `decisional-tools-panel` au-delà de l'entrée TOOL_REGISTRY.
- Pas de modification de PRODUCT_SPEC.md (statut F-IM-14 mis à jour post-merge).
- Pas d'ajout d'un champ `revenusNetsMensuels` à `ImmigrationExtractedData` —
  pré-fill graceful via cast `(aiData as any)?.revenusNetsMensuels` (no-op si absent).

## Contrat API (importé de SF-IM-14-04 backend)

`POST/GET /api/v1/case-files/{caseFileId}/belgian-40ter`

### Request
```json
{
  "lienFamilial": "CONJOINT|PARTENAIRE_LEGAL_ENREGISTRE|DESCENDANT_MINEUR|DESCENDANT_MAJEUR_CHARGE|ASCENDANT_CHARGE_HANDICAP",
  "regroupantBelge": true,
  "revenusMensuelsNetsEur": 2060.00,
  "seuil120PctRisEur": 1740.00,
  "assuranceMaladie": true,
  "logementSuffisant": true,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "2026-04-15"
}
```

### Response
```json
{
  "caseFileId": "uuid",
  "lienFamilial": "...",
  "regroupantBelge": true,
  "revenusMensuelsNetsEur": 2060.00,
  "seuil120PctRisEur": 1740.00,
  "assuranceMaladie": true,
  "logementSuffisant": true,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "2026-04-15",
  "country": "BELGIQUE",
  "lienValide": true,
  "regroupantBelgeOk": true,
  "revenusSuffisantsOk": true,
  "assuranceOk": true,
  "logementOk": true,
  "pasMenace": true,
  "differentielRevenus": 320.00,
  "scoreGlobal": 100,
  "verdictProbabiliteAcceptation": "ELEVEE",
  "criteresNonRemplis": [],
  "dateExpirationInstructionSiDemande": "2026-10-15",
  "formule": "40ter familial Belge BE : probabilité ELEVEE (score 100/100)...",
  "baseJuridique": "Loi 15/12/1980 art. 40ter + AR 08/10/1981 + AR 07/10/1981 (seuil 120 % RIS)",
  "messages": ["Carte F (membre de famille d'un Belge) délivrée après 5 ans...", "..."]
}
```

## Analyse de cohérence transversale

- **Outils décisionnels existants** scannés (annexe13-be, oqtf-avec-delai, oqtf-sans-delai,
  motif-grave-be, immigration-title-decision, immigration-recours, immigration-work-right) :
  → tous suivent le pattern canonique (signals, mat-select, slide-toggle, MatSnackBar,
  CaseDashboardRefreshService). Le présent composant l'applique strictement.
- **Préoccupation transversale "outil décisionnel métier"** : nouvel outil scoring (niveau 5)
  pour le domaine immigration BE. Symétrie 3 domaines déjà tracée par F-IM-14 (4 outils
  immigration BE déclinés en parallèle de F-DT/F-FA — voir F-IM-14 parent).
- **F-IA-03 cohérence popover** : pattern placeholder (pas d'alerte field-level pour
  cette SF — backlog à enrichir si besoin terrain). `LegalCitationsPipe` appliqué à
  `messages` et `baseJuridique` (cohérence SF-155-01).
- **Auth / Principal** : non touché (composant pur consommateur HTTP).
- **Workspace context** : input `workspaceCountry` propagé via TOOL_REGISTRY.
- **Plans / limites** : non touché.
- **Navigation / routing** : composant intégré au panel via TOOL_REGISTRY (zéro impact route).

## Nouveau pattern UI ou service partagé

Aucun. Composant strict consommateur, applique les patterns existants
(`legal-citations.pipe`, `coherence-popover`, `decisional-header-flag`, `case-dashboard-refresh.service`).

## Impact par domaine métier

- **Sensible au domaine** : oui — immigration BE.
- **Droit du travail / Famille** : non applicable (outil immigration uniquement).
- **France** : non applicable (40ter est BE-only — équivalent FR = CESEDA L.434-7,
  hors périmètre F-IM-14).
- **Belgique** : applicable (gate `workspaceCountry === 'BELGIQUE'`).

## Parité des domaines métier (scoring niveau 5)

Outil scoring 0-100 (niveau 5 sur les 7 niveaux d'outils décisionnels).

- **Droit du travail** : pas d'équivalent direct du scoring "regroupement familial" — non pertinent.
- **Famille** : `divorce-consentement-scoring-section` (F-FA-09) couvre le scoring du divorce
  par consentement mutuel — symétrie déjà acquise.
- **Immigration FR** : pas d'équivalent direct (regroupement familial FR différent).
  Backlog F-IM-XX si besoin terrain.
- **Immigration BE** : couvert par cette SF + 3 autres outils BE (9bis humanitaire,
  9ter médical, 40bis cohabitant UE) — feature parente F-IM-14.

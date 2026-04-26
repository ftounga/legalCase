# Mini-spec — F-IM-17 / SF-IM-17-02 Frontend régime algérien (accord franco-algérien 1968)

## Identifiant

`F-IM-17 / SF-IM-17-02`

## Feature parente

`F-IM-17` — Régime algérien (accord franco-algérien du 27/12/1968 + avenants 1985 / 1994 / 2001)

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-IM-17-02-frontend-regime-algerien`

## Contrat API

Importé de `SF-IM-17-01-backend-regime-algerien.md` (PR #653 mergée).

`POST /api/v1/case-files/{caseFileId}/regime-algerien-analysis`
`GET  /api/v1/case-files/{caseFileId}/regime-algerien-analysis`

5 voies CRA enum :
- `CRA_1_AN` — art. 5 accord 1968
- `CRA_10_ANS_LIEN_FRANCE` — art. 6 al. 1, 2, 3
- `CRA_10_ANS_RESIDENT_ANCIEN` — art. 7bis
- `CHANGEMENT_VERS_TRAVAILLEUR` — art. 7
- `REGROUPEMENT_FAMILIAL_ACCORD_1968` — art. 4

---

## Objectif

Section frontend dédiée pour évaluer la recevabilité d'une demande relevant
du régime franco-algérien — outil exclusif aux ressortissants algériens
(régime CESEDA non applicable).

---

## Comportement attendu

- Section `<app-regime-algerien-section>` collapsible montée par
  `decisional-tools-panel` via TOOL_REGISTRY clé `'F-IM-17-regime-algerien'`.
- **Gate workspaceCountry FR uniquement** : si BE → bannière info ("Régime
  bilatéral FR-DZ uniquement, pas d'équivalent BE") + form masqué (pas
  d'appel HTTP).
- **Gate nationalité algérienne** : si `nationaliteAlgerienne` n'est pas
  `true`, afficher bannière info ("Cet outil ne s'applique qu'aux
  ressortissants algériens (régime CESEDA distinct)") + form désactivé
  jusqu'à ce que l'avocat coche la case.
- Form : radio voie (5 options) + champs conditionnels selon voie + critères
  transversaux (état civil, casier, présence régulière).
- Au submit, POST → bandeau verdict navy (ELEVEE) / or (MOYENNE) / rouge
  (FAIBLE) + critères non remplis + documents requis + délai instruction
  + base juridique + formule.
- Pré-fill IA gracieux (no-op si `aiData` absent ou champ non extractible).

### Cas d'erreur

- Backend 400 (workspace BE / dossier non immigration / nationalité non
  algérienne / voie inconnue) → snackbar rouge avec le message backend.
- Backend 404 (GET initial sans POST préalable) → mode formulaire.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : Naturalisation (F-IM-13-02 référence),
  Mineurs (F-IM-19-02), Mesures éloignement (F-IM-20-02), Asile avancé
  (F-IM-12-02), Recours (F-IM-06), Title décision (F-IM-05), Changement
  statut (F-IM-11-02), AES (F-IM-09). Le régime algérien est **standalone
  exclusif** : pas d'intégration latérale, panel F-IA-04 décide via la
  visibility rule backend (priority 77, ALWAYS_ON DROIT_IMMIGRATION
  FRANCE).
- [x] **Autres pays** : Belgique → non applicable (accord bilatéral FR-DZ,
  sans équivalent BE) → bannière info systématique en BE.
- [x] **Autres domaines** : DROIT_FAMILLE / DROIT_DU_TRAVAIL → non applicable
  (régime de séjour pur).
- [x] **Autres UI patterns** : Pré-fill IA + validation F-IA-03 sur la voie
  (mappage IA `aiData.typeProcedureDetectee` ou texte libre vers enum). Le
  champ `nationalite` n'est pas extrait actuellement par
  `ImmigrationExtractedData` — pré-fill `nationaliteAlgerienne` reste
  défensif (no-op gracieux), pattern présent pour bénéficier d'un futur
  enrichissement extracteur.
- [x] **Autres flows transversaux** : aucun impact auth / workspace / plans
  (gate workspace standard hérité du panel parent).

### Classement

| Cible | Classement | Justification |
|-------|-----------|---------------|
| Naturalisation FR | non applicable | régime nationalité, peut cohabiter |
| Recours immigration FR | non applicable | générique, pas de cohérence à garantir |
| AES / Changement statut FR | non applicable | hors champ accord bilatéral |
| Belgique | non applicable | pas d'accord équivalent |
| F-IA-04 panel intégration | **immédiate** | entrée TOOL_REGISTRY ajoutée |
| Validation F-IA-03 sur la voie | **immédiate** | builder canonique partagé |

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : DROIT_IMMIGRATION FRANCE
uniquement. Hors périmètre pour DROIT_DU_TRAVAIL et DROIT_FAMILLE
(régime de séjour pur). Hors périmètre Belgique (accord bilatéral FR-DZ
sans équivalent BE).

---

## Parité des domaines métier

Outil de **niveau 5 (scoring / arbre décisionnel)**. Pas d'équivalent en
DROIT_DU_TRAVAIL ni DROIT_FAMILLE (notion de droit des étrangers). Pas
d'accord bilatéral équivalent en immigration belge.

---

## Critères d'acceptation

- [x] Composant `app-regime-algerien-section` standalone monté via
  TOOL_REGISTRY clé `'F-IM-17-regime-algerien'` (alignée backend
  migration 176).
- [x] Gate FR : workspaceCountry === 'BELGIQUE' → bannière info, pas
  d'appel HTTP.
- [x] Gate nationalité : `nationaliteAlgerienne !== true` → bannière info
  + bouton "Analyser" désactivé.
- [x] Radio voie 5 options + champs conditionnels selon voie.
- [x] Bandeau verdict navy/or/rouge selon ELEVEE/MOYENNE/FAIBLE.
- [x] Pré-fill IA `nationaliteAlgerienne` no-op gracieux (pattern présent
  même si extracteur ne l'expose pas encore).
- [x] Validation F-IA-03 sur la voie via `CoherenceAlertBuilder` partagé.
- [x] `CaseDashboardRefreshService.triggerRefresh()` invoqué après POST OK.
- [x] `MatSnackBar` utilisé (pas d'`alert`/`confirm`).
- [x] JetBrains Mono pour `baseJuridique` et `formule`, Inter pour le reste.
- [x] Tests Jest ≥ 12 (mount, gate FR, gate nationalité, formValid par
  voie, calculate POST, F-IA-03 voie convergent/divergent, F-IA-03 absent).
- [x] Self-check grep 5/5 (pré-fill, F-IA-03, refresh, snackbar, registry).

---

## Plan de test

### Unitaires Jest (`regime-algerien-section.component.spec.ts`) — ≥ 12

1. `FRANCE → isFrance() true, GET émis au ngOnInit`
2. `BELGIQUE → bannière, aucun appel HTTP`
3. `GET 200 → résultat hydraté + voie persistée + showForm=false`
4. `GET 404 → mode formulaire`
5. `nationaliteAlgerienne !== true → form désactivé`
6. `formValid false sans voie`
7. `formValid true CRA_1_AN dès que tous les champs requis sont posés`
8. `calculate POST CRA_1_AN avec champs ciblés + résultat + snackbar`
9. `calculate POST REGROUPEMENT_FAMILIAL avec ressources/logement`
10. `calculate ignoré si form invalide`
11. `calculate erreur 400 backend → snackbar rouge`
12. `coherenceAlerts vide quand pas de voie`
13. `coherenceAlerts.VOIE divergente IA vs saisie → présent`
14. `bannerClass mappe verdict ELEVEE/MOYENNE/FAIBLE`
15. `voieLabel retourne libellé humain ou code en fallback`

---

## Tables / endpoints / composants impactés

### Frontend
- **Nouveau composant** `frontend/src/app/case-files/regime-algerien-section/`
  - `regime-algerien-section.component.ts` (signal-based, 5 voies,
    pré-fill IA, F-IA-03)
  - `regime-algerien-section.component.html`
  - `regime-algerien-section.component.scss`
  - `regime-algerien-section.component.spec.ts`
- **Nouveau modèle** `frontend/src/app/core/models/regime-algerien.model.ts`
- **Nouveau service** `frontend/src/app/core/services/regime-algerien.service.ts`
- **Modification** `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
  → entrée TOOL_REGISTRY clé `'F-IM-17-regime-algerien'`.

### Backend
Aucune modification (tout fournit par SF-IM-17-01).

---

## Hors périmètre

- Modification du prompt IA Immigration pour extraire `nationalite === 'ALG'`
  → backlog F-IM-17 follow-up (le pré-fill `nationaliteAlgerienne` reste
  no-op gracieux jusque-là).
- Recours contre refus de CRA → couvert par F-IM-06.
- Régime équivalent BE → pas d'accord bilatéral.

---

## Préoccupations transversales

| Préoccupation | Impact | Action |
|---------------|--------|--------|
| Auth / Principal | Aucun (réutilise routes Angular auth) | — |
| Workspace context | Standard (workspaceCountry input panel) | — |
| Plans / limites | Aucun gate quota | — |
| Navigation / routing | Aucun (composant standalone monté via panel) | — |
| Outil décisionnel métier | Outil neuf, situation dédiée (régime FR-DZ exclusif) | conforme invariant |

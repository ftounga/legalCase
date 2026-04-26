# Mini-spec — F-FA-24 / SF-FA-24-08 Frontend réserve héréditaire + action en réduction

## Identifiant

`F-FA-24 / SF-FA-24-08`

## Feature parente

`F-FA-24` — Droit des successions (chantier ~9-11 SF — backend SF-FA-24-07 mergé PR #672).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-08-frontend-reserve-heriditaire`

---

## Objectif

Composant Angular `<app-reserve-heriditaire-section>` consommant l'API SF-FA-24-07 (POST + GET `/api/v1/case-files/{id}/reserve-heriditaire-analysis`) — outil décisionnel "Réserve héréditaire et action en réduction" (FR — art. 913 + 914-1 + 920-928 Cciv). Affiche le barème de la réserve, l'excédent réductible des libéralités, la prescription restante (5 ans art. 921) et la recevabilité de l'action en réduction.

---

## Contrat API (importé de SF-FA-24-07 backend)

- **POST/GET** `/api/v1/case-files/{caseFileId}/reserve-heriditaire-analysis`
- **Body POST** :
  - `nombreEnfants: number` (≥ 0)
  - `conjointSurvivant: boolean`
  - `montantSuccession: number` (> 0, EUR)
  - `montantLibsTotal: number` (≥ 0, EUR)
  - `dateOuvertureSuccession: string` (`YYYY-MM-DD`, pas future)
  - `qualiteDuDemandeur: 'HERITIER_RESERVATAIRE_DESCENDANT' | 'CONJOINT_SURVIVANT'`
- **Réponse** :
  - `caseFileId`, `country`, `nombreEnfants`, `conjointSurvivant`, `montantSuccession`, `montantLibsTotal`, `dateOuvertureSuccession`, `qualiteDuDemandeur`
  - `quotiteDisponiblePct`, `reservePct` (number, %)
  - `montantReserveEur`, `montantQuotiteDispoEur`, `excedentReductibleEur` (number EUR, 2 décimales)
  - `actionReductionRecevable: boolean`
  - `delaiPrescriptionRestantMois: number` (entier ≥ 0)
  - `verdictRecevabilite: 'RECEVABLE' | 'NON_RECEVABLE_PAS_EXCEDENT' | 'NON_RECEVABLE_PRESCRIPTION' | 'NON_RECEVABLE_QUALITE' | 'NON_RECEVABLE'`
  - `scoreEligibilite: number` (0-100)
  - `baseJuridique`, `formule`, `messages`

---

## Comportement attendu

### Cas nominal FR

1. L'avocat ouvre l'outil (collapsible) — GET au mount → si 200, hydrate la vue résultat.
2. Si 404 (jamais calculé) → mode formulaire, avec **pré-fill IA** des champs détectables (`aiData.nombreEnfantsSuccessionDetecte`, `aiData.conjointSurvivantDetected`, `aiData.montantSuccessionEurDetecte`, `aiData.montantLibsTotalEurDetecte`, `aiData.dateOuvertureSuccessionDetectee`, `aiData.qualiteDuDemandeurReserveDetecte`).
3. L'avocat saisit / valide → POST → bandeau résultat (recevabilité + score) + tableau visuel barème + barre de progression (libs vs quotité disponible) + chip `actionReductionRecevable` + chip `delaiPrescriptionRestantMois` (rouge < 12 mois).
4. Bouton "Modifier" remet le form en mode édition.
5. SnackBar succès — `CaseDashboardRefreshService.triggerRefresh()`.

### Bannière info BE

Si `workspaceCountry !== 'FRANCE'` → bannière info (palette navy/or, **pas masquage silencieux**) renvoyant vers feature jumelle `F-FA-24-BE`. Aucun appel HTTP.

### Validation F-IA-03 (RÈGLE FONDAMENTALE)

Au changement, comparer la saisie avocat aux 4 sources IA :
1. **F-96** (procedureChecks) — `critereCode` `RESERVE_CONJOINT_SURVIVANT` ou `RESERVE_NB_ENFANTS`
2. **QUESTION_IA** (aiQuestions) — mêmes codes
3. **IA** (aiData) — `conjointSurvivantDetected`, `nombreEnfantsSuccessionDetecte`
4. **PIECE_MANQUANTE** (piecesManquantes)

Champs auditables : `CONJOINT_SURVIVANT`, `NOMBRE_ENFANTS`, `MONTANT_SUCCESSION`, `MONTANT_LIBS`, `QUALITE_DEMANDEUR`. Helper partagé : `CoherenceAlertBuilder` (SF-155-05). Directive `<app-coherence-popover-trigger>` câblée par champ.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| GET 404 | mode formulaire (silencieux) |
| GET autre erreur | mode formulaire + log silencieux |
| POST 400 (validation) | snackbar rouge avec le message backend |
| POST autre erreur | snackbar rouge générique |
| `workspaceCountry !== 'FRANCE'` | bannière info, pas d'appel HTTP |
| Form invalide | bouton désactivé |

---

## Critères d'acceptation

- [x] `<app-reserve-heriditaire-section>` consomme `POST/GET /reserve-heriditaire-analysis`.
- [x] Gate FR : aucun appel HTTP si `workspaceCountry !== 'FRANCE'`, bannière info BE.
- [x] Pré-fill IA fonctionnel sur les 6 champs (provenance `'IA' | null` par champ + badge `auto_awesome`).
- [x] Validation F-IA-03 sur 5 fields avec `CoherenceAlertBuilder` + popover trigger.
- [x] Affichage résultat : tableau barème (configurations enfants + conjoint) + barre de progression libs / QD + chip recevabilité + chip prescription < 12 mois.
- [x] Calculs (montants EUR + %) en `JetBrains Mono`. `formule` + `baseJuridique` en `JetBrains Mono`. Tout le reste en `Inter`.
- [x] Palette navy/or — rouge réservé alerte critique (excédent + recevable, prescription < 12 mois).
- [x] `MatSnackBar` pour erreurs, `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [x] Entrée `TOOL_REGISTRY` `F-FA-24-reserve-heriditaire` symétrique aux autres outils famille.
- [x] Tests Jest ≥ 12 (gate pays, GET 200/404, POST succès/erreur, pré-fill IA, F-IA-03 alertes, helpers UI).
- [x] Self-check 5/5 (cf. règle CLAUDE.md).

---

## Plan de test

### Unitaires Jest (≥ 12)
1. FR → `isFrance()` true, GET appelé au `ngOnInit`.
2. BE → aucun appel HTTP, `isFrance()` false.
3. GET 200 → mode résultat hydraté.
4. GET 404 → mode formulaire.
5. Pré-fill IA conjoint / enfants / montantSuccession / qualité → provenance `IA`.
6. `onConjointSurvivantChange` efface badge IA.
7. `formValid` initialement false, true après remplissage complet.
8. POST envoie body attendu + result + snackbar succès + refresh.
9. POST 400 → snackbar rouge + `calculating` reset.
10. F-IA-03 : alerte CONJOINT_SURVIVANT divergent IA.
11. F-IA-03 : alerte NOMBRE_ENFANTS divergent F-96.
12. F-IA-03 : MULTI sources F96 + IA.
13. `toggleCollapse`, `editMode` helpers.
14. `verdictLabel` couvre les 5 valeurs.

### Self-check pré-commit (5/5)
Voir `frontend-coherence-audit.md` §6 — palette, datepicker `<input type="date">`, gate FR + bannière BE, refresh, snackbar. + pré-fill IA + F-IA-03 = règles fondamentales (FAIL si absent).

---

## Tables / endpoints / composants impactés

- **Endpoint** : `/api/v1/case-files/{caseFileId}/reserve-heriditaire-analysis` (POST, GET) — backend SF-FA-24-07 mergé.
- **Composant** : `frontend/src/app/case-files/reserve-heriditaire-section/` (4 fichiers).
- **Modèle** : `frontend/src/app/core/models/reserve-heriditaire.model.ts`.
- **Service** : `frontend/src/app/core/services/reserve-heriditaire.service.ts`.
- **TOOL_REGISTRY** : entrée `F-FA-24-reserve-heriditaire` ajoutée à `decisional-tools-panel.component.ts`.
- **AI extracted data** : 6 nouveaux champs optionnels ajoutés à `FamilleExtractedData` (no-op gracieux — pas de migration DB côté frontend, l'IA backend les remplira ultérieurement, le composant tolère leur absence).

---

## Hors périmètre

- Backend (déjà livré SF-FA-24-07).
- Régime BE (feature jumelle backlog `F-FA-24-BE`).
- Génération de courrier d'action en réduction (V2 — F-FA-24-09 ou ultérieur).
- Calcul détaillé par libéralité (ordre chronologique des donations) — V1 retourne juste l'excédent global.
- Imputation de la donation entre époux ou des avantages matrimoniaux (renvoi vers SF action en retranchement, hors V1).

---

## Impact par domaine métier

Cette SF est **strictement Droit de la famille FR** — composant frontend d'un outil dédié successions FR, single-country.
- **Droit du travail** : non applicable.
- **Immigration** : non applicable.
- **Famille FR** : ce qu'on livre.
- **Famille BE** : bannière info renvoyant vers feature jumelle backlog (F-FA-24-BE).

## Parité des domaines métier

Outil de niveau 5 (scoring de recevabilité). Pas d'équivalent dans Travail/Immigration. Pas de feature jumelle requise hors du chantier successions.

## Analyse de cohérence transversale

| Cible | Statut |
|-------|--------|
| Composants décisionnels FR famille (devolution-legale, testament-validite) | Pattern de référence — **intégré** (palette, gate, F-IA-03, pré-fill, TOOL_REGISTRY) |
| Composants BE famille | Hors scope SF — backlog F-FA-24-BE dédié |
| Composants Travail/Immigration | Non applicable |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern. Réutilise :
- `CoherenceAlertBuilder` (SF-155-05) pour F-IA-03.
- `CoherencePopoverTriggerDirective` (popover de divergence).
- `LegalCitationsPipe` (rendu citations en JetBrains Mono).
- `CaseDashboardRefreshService` (trigger refresh après POST).
- Pattern de référence : `devolution-legale-section` (PR #658).

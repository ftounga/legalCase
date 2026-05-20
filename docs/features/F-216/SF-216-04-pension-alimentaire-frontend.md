# SF-216-04 — Pension alimentaire enfant FR — frontend

## Objectif

Section décisionnelle Angular `pension-alimentaire-enfant-section` : formulaire de calcul pension alimentaire enfant, pré-remplissage IA des revenus + âges enfants + mode de résidence, affichage du montant indicatif par enfant + total.

## Comportement nominal

- Composant `PensionAlimentaireEnfantSectionComponent` (OnPush + ChangeDetectorRef.markForCheck() dans next/error du subscribe).
- Entrée `TOOL_REGISTRY` : `['F-FA-02-pension-alimentaire', { component: PensionAlimentaireEnfantSectionComponent }]`.
- Champs saisissables :
  - `revenusNetsParent1Eur` — pré-rempli `vie_commune_detection.revenusAnnuelsEpoux1` / 12
  - `revenusNetsParent2Eur` — pré-rempli `vie_commune_detection.revenusAnnuelsEpoux2` / 12
  - `nombreEnfants` — pré-rempli `filiation_detection_v2.nombreEnfantsDetecte`
  - `agesEnfants[]` — pré-rempli `filiation_detection_v2.agesEnfantsDetectes`
  - `modeResidence` — select (ALTERNEE / PRINCIPALE_PARENT1 / PRINCIPALE_PARENT2) — pré-rempli `modeResidenceEnfantsDetecte`
  - `dateOrdonnanceOrDecision` — date picker optionnel (pour indexation)
- Affichage résultat : tableau par enfant (montant individuel + total), badge barème Cass., date d'indexation si applicable.
- Gate `workspaceCountry=FRANCE` strict.

## Cas d'erreur

- 400/500 → toast erreur.
- Nombre d'enfants ≠ nombre d'âges → validation inline.

## Source juridique

art. 371-2 Cciv + barème indicatif Cass. (voir SF-216-03).

## Champs IA pré-remplis

| Champ formulaire | Source | Statut F-246 |
|---|---|---|
| `revenusNetsParent1Eur` | `vie_commune_detection.revenusAnnuelsEpoux1` / 12 | Branché F-246 |
| `revenusNetsParent2Eur` | `vie_commune_detection.revenusAnnuelsEpoux2` / 12 | Branché F-246 |
| `nombreEnfants` | `filiation_detection_v2.nombreEnfantsDetecte` | Branché F-246 |
| `agesEnfants[]` | `filiation_detection_v2.agesEnfantsDetectes` | Branché F-246 |
| `modeResidence` | `modeResidenceEnfantsDetecte` | **Nouveau — SF-216-03** |

## Plan de test

- UT helper `pension-alimentaire-enfant-prefill-rules.ts` : mapping source → cible.
- Self-check grep : `grep -r "F-FA-02-pension-alimentaire" frontend/src/` → TOOL_REGISTRY présent.
- Smoke test pré-commit.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/pension-alimentaire-enfant-section/`.
- `decisional-tools-panel.component.ts` — TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS.

## Critères d'acceptation

- AC1 : pré-remplissage revenus + enfants depuis IA.
- AC2 : workspace BE → bannière info, pas d'appel backend.
- AC3 : soumission formulaire → tableau montants affiché.
- AC4 : self-check grep TOOL_REGISTRY → OK.

## Hors périmètre

- Backend (SF-216-03).
- ARIPA recouvrement (SF-216-07/08).

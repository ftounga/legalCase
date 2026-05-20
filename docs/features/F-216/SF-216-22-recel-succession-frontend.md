# SF-216-22 — Recel de succession — frontend

## Objectif

Section `recel-succession-section` : formulaire recel succession avec pré-remplissage IA de la date d'ouverture et des indicateurs de recel, affichage de la qualification et de la sanction civile.

## Comportement nominal

- Composant `RecelSuccessionSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-RECEL-SUCCESSION', { component: RecelSuccessionSectionComponent }]`.
- Champs :
  - `dateOuvertureSuccession` — pré-rempli `succession_detection_v2.dateOuvertureSuccessionDetectee`
  - `typeRecel` — select enum, pré-rempli `typeRecelDetecte`
  - `preuveRecel` — select enum, pré-rempli `preuveRecelDetectee`
  - `bienCeleValeurEur` — numérique
  - `receleurQualite` — select enum
  - `actionIntentee` — checkbox
- Résultat : qualification, sanction civile, délai d'action, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `dateOuvertureSuccession` | `succession_detection_v2.dateOuvertureSuccessionDetectee` | Branché F-246 |
| `typeRecel` | `typeRecelDetecte` | **Nouveau — SF-216-21** |
| `preuveRecel` | `preuveRecelDetectee` | **Nouveau — SF-216-21** |

## Plan de test

- UT helper `recel-succession-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/recel-succession-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : pré-remplissage depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → verdict affiché.

## Hors périmètre

- Backend (SF-216-21).

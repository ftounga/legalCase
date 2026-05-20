# SF-216-20 — Indignité successorale — frontend

## Objectif

Section `indignite-successorale-section` : formulaire indignité successorale avec pré-remplissage IA de la date d'ouverture + condamnation pénale + pardon testamentaire.

## Comportement nominal

- Composant `IndigniteSuccessoraleSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-INDIGNITE-SUCCESSORALE', { component: IndigniteSuccessoraleSectionComponent }]`.
- Champs :
  - `dateOuvertureSuccession` — pré-rempli `succession_detection_v2.dateOuvertureSuccessionDetectee`
  - `motifIndignite` — select enum
  - `condamnationPrononcee` — checkbox, pré-coché `condamnationPenaleSuccessionDetectee`
  - `pardonTestamentaireDetecte` — checkbox, pré-coché `pardonTestamentaireDetecte`
  - `indigniteJudiciaireDemandee` — checkbox
- Résultat : type indignité, effet sur dévolution, délai d'action, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `dateOuvertureSuccession` | `succession_detection_v2.dateOuvertureSuccessionDetectee` | Branché F-246 |
| `condamnationPrononcee` | `condamnationPenaleSuccessionDetectee` | **Nouveau — SF-216-19** |
| `pardonTestamentaireDetecte` | `pardonTestamentaireDetecte` | **Nouveau — SF-216-19** |

## Plan de test

- UT helper `indignite-successorale-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/indignite-successorale-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : pré-remplissage date ouverture + condamnation depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → verdict affiché.

## Hors périmètre

- Backend (SF-216-19).

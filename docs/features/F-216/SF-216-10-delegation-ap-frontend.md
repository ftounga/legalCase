# SF-216-10 — Délégation d'autorité parentale FR — frontend

## Objectif

Section `delegation-ap-section` : arbre décisionnel délégation AP avec pré-remplissage IA de l'âge de l'enfant, du lien tiers et de l'accord parental ; affichage de la voie procédurale et des étapes concrètes.

## Comportement nominal

- Composant `DelegationApSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-DELEGATION-AP', { component: DelegationApSectionComponent }]`.
- Champs :
  - `typeDelégation` — select enum
  - `tiersLienFamilial` — select, pré-rempli `tiersLienFamilialDetecte`
  - `accordParents` — checkbox, pré-coché si `accordParentsDetecte=true`
  - `ageEnfant` — pré-rempli premier enfant de `filiation_detection_v2.agesEnfantsDetectes`
  - `dangerCaracterise` — checkbox, pré-cochée si `dangerCaracteriseDetecte`
- Résultat : voie procédurale, étapes sous forme de stepper, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `ageEnfant` | `filiation_detection_v2.agesEnfantsDetectes[0]` | Branché F-246 |
| `tiersLienFamilial` | `tiersLienFamilialDetecte` | **Nouveau — SF-216-09** |
| `accordParents` | `accordParentsDetecte` | **Nouveau — SF-216-09** |

## Plan de test

- UT helper `delegation-ap-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/delegation-ap-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : âge enfant pré-rempli depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → voie + étapes affichées.

## Hors périmètre

- Backend (SF-216-09).

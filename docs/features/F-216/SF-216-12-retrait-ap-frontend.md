# SF-216-12 — Retrait d'autorité parentale FR — frontend

## Objectif

Section `retrait-ap-section` : arbre décisionnel retrait AP avec pré-remplissage IA des flags danger, violences, condamnation pénale ; affichage du verdict et des conséquences juridiques.

## Comportement nominal

- Composant `RetraitApSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-RETRAIT-AP', { component: RetraitApSectionComponent }]`.
- Champs :
  - `typeRetrait` — select enum
  - `motifRetrait` — select enum
  - `condamnationPenaleDetectee` — checkbox, pré-coché si `condamnationPenaleDetectee=true`
  - `dangerCaracterise` — pré-coché si `protection_divorce_detection_v2.dangerCaracteriseDetecte=true`
  - `violencesConjugalesDetectees` — pré-coché si `protection_divorce_detection_v2.violencesAllegueesDetectees=true`
  - `ageEnfant` — pré-rempli `filiation_detection_v2.agesEnfantsDetectes[0]`
- Résultat : verdict, voie procédurale, conséquences, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `dangerCaracterise` | `protection_divorce_detection_v2.dangerCaracteriseDetecte` | Branché F-246 |
| `violencesConjugalesDetectees` | `protection_divorce_detection_v2.violencesAllegueesDetectees` | Branché F-246 |
| `ageEnfant` | `filiation_detection_v2.agesEnfantsDetectes[0]` | Branché F-246 |
| `condamnationPenaleDetectee` | `condamnationPenaleDetectee` | **Nouveau — SF-216-11** |
| `violencesLmvss2022Detectees` | `violencesLmvss2022Detectees` | **Nouveau — SF-216-11** |

## Plan de test

- UT helper `retrait-ap-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/retrait-ap-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : danger + violences pré-cochés depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → verdict + conséquences affichés.

## Hors périmètre

- Backend (SF-216-11).

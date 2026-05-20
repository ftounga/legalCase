# SF-216-14 — Audition du mineur par le JAF — frontend

## Objectif

Section `audition-mineur-section` : checklist conditions art. 388-1 Cciv, pré-remplissage IA de l'âge de l'enfant et de la demande formalisée, affichage du verdict.

## Comportement nominal

- Composant `AuditionMineurSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-AUDITION-ENFANT', { component: AuditionMineurSectionComponent }]`.
- Champs :
  - `ageEnfant` — pré-rempli `filiation_detection_v2.agesEnfantsDetectes[0]`
  - `capaciteDiscernement` — select enum
  - `demandeFormalisee` — checkbox, pré-coché `demandeAuditionFormaliseeDetectee`
  - `demandeParEnfantLuiMeme` — checkbox
  - `procedureEnCours` — select enum
- Résultat : conditions remplies, modalité recommandée, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `ageEnfant` | `filiation_detection_v2.agesEnfantsDetectes[0]` | Branché F-246 |
| `demandeFormalisee` | `demandeAuditionFormaliseeDetectee` | **Nouveau — SF-216-13** |

## Plan de test

- UT helper `audition-mineur-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/audition-mineur-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : âge enfant pré-rempli.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → verdict affiché.

## Hors périmètre

- Backend (SF-216-13).

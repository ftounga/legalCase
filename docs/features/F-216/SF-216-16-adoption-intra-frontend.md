# SF-216-16 — Adoption intra-familiale — frontend

## Objectif

Section `adoption-intra-section` : formulaire adoption enfant du conjoint avec pré-remplissage IA, alerte irréversibilité adoption plénière, affichage des formes possibles.

## Comportement nominal

- Composant `AdoptionIntraSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-ADOPTION-INTRA', { component: AdoptionIntraSectionComponent }]`.
- Champs :
  - `formeAdoptionDemandee` — select (PLENIERE / SIMPLE)
  - `ageAdoptant`, `ageAdopte` — pré-remplis si dans `filiation_detection_v2.agesEnfantsDetectes`
  - `mariageOuPacsAdoptantParent` — checkbox, pré-coché `mariageOuPacsAdoptantParentDetecte`
  - `consentementAutreParentBiologique` — pré-coché `consentementAutreParentDetecte`
  - `autreParentDecede` — pré-coché `autreParentDecedeDetecte`
  - `decheanceAutreParent` — checkbox
- Alerte irréversibilité affichée en rouge si forme = PLENIERE.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `ageAdopte` | `filiation_detection_v2.agesEnfantsDetectes[0]` | Branché F-246 |
| `mariageOuPacsAdoptantParent` | `mariageOuPacsAdoptantParentDetecte` | **Nouveau — SF-216-15** |
| `consentementAutreParentBiologique` | `consentementAutreParentDetecte` | **Nouveau — SF-216-15** |
| `autreParentDecede` | `autreParentDecedeDetecte` | **Nouveau — SF-216-15** |

## Plan de test

- UT helper `adoption-intra-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.
- Alerte irréversibilité plénière visible.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/adoption-intra-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : pré-remplissage lien mariage + décès autre parent depuis IA.
- AC2 : alerte irréversibilité si PLENIERE sélectionnée.
- AC3 : workspace BE → bannière info.

## Hors périmètre

- Backend (SF-216-15).

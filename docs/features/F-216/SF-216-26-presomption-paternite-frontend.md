# SF-216-26 — Présomption de paternité et désaveu — frontend

## Objectif

Section `presomption-paternite-section` : formulaire présomption de paternité / désaveu avec pré-remplissage IA des dates (naissance, mariage, dissolution), affichage du verdict.

## Comportement nominal

- Composant `PresomptionPaterniteSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-PRESOMPTION-PATERNITE', { component: PresomptionPaterniteSectionComponent }]`.
- Champs :
  - `dateNaissanceEnfant` — pré-rempli `filiation_detection_v2.dateNaissanceEnfantDetectee`
  - `dateConclusionMariage` — pré-rempli `dateConclusionMariageDetectee`
  - `dateDissolutionMariage` — pré-rempli `dateDissolutionMariageDetectee`
  - `possessionEtatConformeDetecte` — checkbox, pré-coché `filiation_detection_v2.possessionEtatConforme5AnsDetected`
  - `desaveuEnvisage` — checkbox, pré-coché `desaveuEnvisage`
- Résultat : présomption applicable/renversée, voie désaveu, délai, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `dateNaissanceEnfant` | `filiation_detection_v2.dateNaissanceEnfantDetectee` | Branché F-246 |
| `possessionEtatConformeDetecte` | `filiation_detection_v2.possessionEtatConforme5AnsDetected` | Branché F-246 |
| `dateConclusionMariage` | `dateConclusionMariageDetectee` | **Nouveau — SF-216-25** |
| `dateDissolutionMariage` | `dateDissolutionMariageDetectee` | **Nouveau — SF-216-25** |
| `desaveuEnvisage` | `desaveuEnvisage` | **Nouveau — SF-216-25** |

## Plan de test

- UT helper `presomption-paternite-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/presomption-paternite-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : dates de naissance + mariage pré-remplies depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → verdict affiché.

## Hors périmètre

- Backend (SF-216-25).

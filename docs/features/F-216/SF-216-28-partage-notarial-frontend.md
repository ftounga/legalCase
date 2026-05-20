# SF-216-28 — Partage successoral notarié — frontend

## Objectif

Section `partage-successoral-notarial-section` : formulaire partage notarié avec pré-remplissage IA de la date d'ouverture, nombre de cohéritiers, présence d'immeuble ; affichage du calendrier des étapes.

## Comportement nominal

- Composant `PartageSuccessoralNotarialSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-PARTAGE-NOTARIAL', { component: PartageSuccessoralNotarialSectionComponent }]`.
- Champs :
  - `dateOuvertureSuccession` — pré-rempli `succession_detection_v2.dateOuvertureSuccessionDetectee`
  - `nombreCoheritiers` — pré-rempli `succession_detection_v2.nombreCoheritiersDetecte`
  - `presenceImmeuble` — checkbox, pré-coché `presenceImmeubleSuccessionDetecte`
  - `consentementsTousDetecte` — checkbox
  - `valeurMasseSuccessoraleEur` — numérique, pré-rempli `montantSuccessionEur`
  - `notaireDesigne` — checkbox
  - `desaccordPersistant` — checkbox
- Résultat : notaire obligatoire/recommandé, calendrier étapes, alerte délai fiscal, orientation judiciaire si désaccord.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `dateOuvertureSuccession` | `succession_detection_v2.dateOuvertureSuccessionDetectee` | Branché F-246 |
| `nombreCoheritiers` | `succession_detection_v2.nombreCoheritiersDetecte` | Branché F-246 |
| `valeurMasseSuccessoraleEur` | `succession_detection_v2.montantSuccessionEur` | Branché F-246 |
| `presenceImmeuble` | `presenceImmeubleSuccessionDetecte` | **Nouveau — SF-216-27** |

## Plan de test

- UT helper `partage-successoral-notarial-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.
- Alerte délai fiscal visible si dépassé.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/partage-successoral-notarial-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : date + cohéritiers + masse pré-remplis depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → calendrier étapes affiché.

## Hors périmètre

- Backend (SF-216-27).

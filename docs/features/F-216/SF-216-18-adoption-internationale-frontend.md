# SF-216-18 — Adoption internationale — frontend

## Objectif

Section `adoption-internationale-section` : formulaire adoption internationale avec pré-remplissage IA des flags agrément, pays d'origine, alerte kafala, affichage de la voie procédurale.

## Comportement nominal

- Composant `AdoptionInternationaleSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-ADOPTION-INTERNATIONALE', { component: AdoptionInternationaleSectionComponent }]`.
- Champs :
  - `paysOrigineEnfant` — text, pré-rempli `paysOrigineAdopteDetecte`
  - `agrement2025` — checkbox, pré-coché `agrement2025DetecteValide`
  - `conventionLaHayeApplicable` — checkbox (calculé automatiquement selon pays si liste intégrée)
  - `voieProcedure` — select enum
  - `formeAdoptionDemandee` — select enum
  - `ageAdoptant`, `ageAdopte` — numériques
  - `exequaturRequis` — checkbox, pré-coché `exequaturRequisDetecte`
- Alerte kafala en rouge si pays d'origine = Maroc/Algérie/Tunisie.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `paysOrigineEnfant` | `paysOrigineAdopteDetecte` | **Nouveau — SF-216-17** |
| `agrement2025` | `agrement2025DetecteValide` | **Nouveau — SF-216-17** |
| `exequaturRequis` | `exequaturRequisDetecte` | **Nouveau — SF-216-17** |
| `ageAdopte` | `filiation_detection_v2.agesEnfantsDetectes[0]` | Branché F-246 |

## Plan de test

- UT helper `adoption-internationale-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.
- Alerte kafala visible pour pays concernés.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/adoption-internationale-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : pays + agrément pré-remplis depuis IA.
- AC2 : alerte kafala si Maroc/Algérie/Tunisie.
- AC3 : workspace BE → bannière info.

## Hors périmètre

- Backend (SF-216-17).

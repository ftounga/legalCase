# SF-216-24 — Donation entre époux — frontend

## Objectif

Section `donation-entre-epoux-section` : formulaire donation entre époux avec pré-remplissage IA du régime, clause attribution intégrale, enfants non communs.

## Comportement nominal

- Composant `DonationEntreEpouxSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-DONATION-ENTRE-EPOUX', { component: DonationEntreEpouxSectionComponent }]`.
- Champs :
  - `avantageMatrimonialType` — select enum
  - `regimeMatrimonial` — pré-rempli `regimes_vie_commune_detection_v2.regimeMatrimonialDetecte`
  - `clauseAttributionIntegrale` — checkbox, pré-coché `clauseAttributionIntegraleDetected`
  - `enfantsNonCommuns` — checkbox, pré-coché `enfantsNonCommunsDetected`
  - `mariageDissous` — checkbox
  - `revocabiliteDetectee` — pré-coché `revocabiliteDetectee`
  - `valeurBienDonneEur` — numérique
- Résultat : validité, révocabilité, effet dissolution, action en retranchement, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `regimeMatrimonial` | `regimes_vie_commune_detection_v2.regimeMatrimonialDetecte` | Branché F-246 |
| `clauseAttributionIntegrale` | `regimes_vie_commune_detection_v2.clauseAttributionIntegraleDetected` | Branché F-246 |
| `enfantsNonCommuns` | `regimes_vie_commune_detection_v2.enfantsNonCommunsDetected` | Branché F-246 |
| `revocabiliteDetectee` | `revocabiliteDetectee` | **Nouveau — SF-216-23** |

## Plan de test

- UT helper `donation-entre-epoux-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/donation-entre-epoux-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : régime + clause + enfants non communs pré-remplis depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → verdict affiché.

## Hors périmètre

- Backend (SF-216-23).

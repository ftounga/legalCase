# SF-216-30 — Donation-partage — frontend

## Objectif

Section `donation-partage-section` : formulaire donation-partage avec pré-remplissage IA du nombre de descendants, quotité disponible, donations antérieures.

## Comportement nominal

- Composant `DonationPartageSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-DONATION-PARTAGE', { component: DonationPartageSectionComponent }]`.
- Champs :
  - `nombreDescendants` — pré-rempli `succession_detection_v2.nbDescendantsDetecte`
  - `presencePetitsEnfantsParSubstitution` — checkbox, pré-coché `presencePetitsEnfantsSubstitutionDetectee`
  - `donationPartageConjonctive` — checkbox, pré-coché `donationPartageConjonctiveDetectee`
  - `valeurPartageTotal` — numérique
  - `respectQuotiteDisponible` — checkbox
  - `donationsAnterieuresAReinorporer` — checkbox
- Résultat : conditions, gel valeur, rapport exclu, alerte quotité, étapes notariales.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `nombreDescendants` | `succession_detection_v2.nbDescendantsDetecte` | Branché F-246 |
| `respectQuotiteDisponible` | `regimes_vie_commune_detection_v2.respectQuotiteDisponibleDetected` | Branché F-246 |
| `presencePetitsEnfantsParSubstitution` | `presencePetitsEnfantsSubstitutionDetectee` | **Nouveau — SF-216-29** |
| `donationPartageConjonctive` | `donationPartageConjonctiveDetectee` | **Nouveau — SF-216-29** |

## Plan de test

- UT helper `donation-partage-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/donation-partage-section/`.
- `decisional-tools-panel.component.ts`.

## Critères d'acceptation

- AC1 : descendants + quotité pré-remplis depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → gel valeur + rapport exclu affichés.

## Hors périmètre

- Backend (SF-216-29).

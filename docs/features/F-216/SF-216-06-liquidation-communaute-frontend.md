# SF-216-06 — Liquidation régime communauté légale FR — frontend

## Objectif

Section décisionnelle `liquidation-communaute-legale-section` : formulaire de liquidation de la communauté légale avec pré-remplissage IA des valeurs immobilières et du régime, affichage des quotes-parts et de la soulte éventuelle.

## Comportement nominal

- Composant `LiquidationCommunauteLegaleSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-04-liquidation-communaute', { component: LiquidationCommunauteLegaleSectionComponent }]`.
- Champs et pré-remplissage IA :
  - `regimeMatrimonialDetecte` — affiché en lecture seule, depuis `regimes_vie_commune_detection_v2.regimeMatrimonialDetecte`
  - `valeurImmeubleCommun1Eur` — pré-rempli `regimes_vie_commune_detection_v2.valeurImmeubleEur`
  - `capitalRestantDuEur` — pré-rempli `regimes_vie_commune_detection_v2.capitalRestantDuEur`
  - `valeurMobilierCommunEur`, `autresActifsCommunsEur` — saisie manuelle
  - `recompensesEpoux1Eur`, `recompensesEpoux2Eur` — pré-remplis si extraits, sinon saisie
  - `biensPropresEpoux1Eur`, `biensPropresEpoux2Eur` — saisie manuelle
- Résultat : tableau masse commune, quotes-parts, soulte, alertes.
- Alerte si `clauseAttributionIntegraleDetected=true` → message « clause attribution intégrale détectée — outil F-FA-16-communaute-universelle applicable ».
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `regimeMatrimonialDetecte` | `regimes_vie_commune_detection_v2.regimeMatrimonialDetecte` | Branché F-246 |
| `valeurImmeubleCommun1Eur` | `regimes_vie_commune_detection_v2.valeurImmeubleEur` | Branché F-246 |
| `capitalRestantDuEur` | `regimes_vie_commune_detection_v2.capitalRestantDuEur` | Branché F-246 |
| `valeurCommunauteEur` | `regimes_vie_commune_detection_v2.valeurCommunauteEurDetectee` | Branché F-246 |
| `recompensesEpoux1Eur` | `recompensesEpoux1Eur` | **Nouveau — SF-216-05** |
| `recompensesEpoux2Eur` | `recompensesEpoux2Eur` | **Nouveau — SF-216-05** |

## Plan de test

- UT helper `liquidation-communaute-legale-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.
- Smoke test : dossier avec régime COMMUNAUTE_LEGALE + immeuble → champs pré-remplis.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/liquidation-communaute-legale-section/`.
- `decisional-tools-panel.component.ts` — TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS.

## Critères d'acceptation

- AC1 : immeuble + crédit pré-remplis depuis IA.
- AC2 : alerte clause attribution intégrale si détectée.
- AC3 : workspace BE → bannière info.
- AC4 : self-check grep TOOL_REGISTRY → OK.

## Hors périmètre

- Backend (SF-216-05).
- Aspects fiscaux.

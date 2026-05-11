# SF-208-08 — Victime de violences L.425-6 frontend (Angular)

## Identifiant
`F-208 / SF-208-08`

## Statut
`draft` — 2026-05-11

## Branche Git
`feat/SF-208-frontend-immigration-fr-p1` (commune SF-208-05..08)

## Pattern de référence
`oqtf-avec-delai-section` + symétrie SF-208-05/06/07 + composants Analyzer (verdict 3 niveaux).

## Objectif
Composant `<app-victime-violences-l4256-section>` qui consomme `POST/GET /api/v1/case-files/{caseFileId}/victime-violences-l4256-analysis` (SF-208-04 backend mergée). tool_id `F-IM-24-victime-violences-l4256-fr`. Visibility `ALWAYS_ON`. **Niveau 5 (scoring)** — donc parité Famille/Travail à vérifier en analyse cohérence.

## Critères d'acceptation
- [ ] **CA-01** : service `VictimeViolencesL4256Service` + modèle DTO
- [ ] **CA-02** : composant standalone OnPush, palette canonique. Verdict scoring 3 niveaux : `ELIGIBLE_PLEIN_DROIT` (navy/or), `ELIGIBLE_SOUS_RESERVE` (or), `NON_ELIGIBLE` (rouge `#C0392B`)
- [ ] **CA-03** : inputs — `dateOrdonnanceProtection` (date), `juridiction` (string libre), `dureeProtectionMois` (number, défaut 6), `dateExpirationProtection` (date, optionnel), `enfantsAcharge` (number ≥ 0), `nationalite` (string libre)
- [ ] **CA-04** : verdict — `eligibiliteScore` (3 niveaux) + `criteresValides` (liste cases ✅) + `criteresManquants` (liste ⚠️) + `dureeTitreSejour` "1 an renouvelable (L.425-7)" en JetBrains Mono + messages
- [ ] **CA-05** : gate country FR-only (équivalent BE = protection L.40 / 9ter — feature jumelle future)
- [ ] **CA-06** : pré-fill IA — `aiData.cas_violences_intrafamiliales_detecte` (boolean) flag d'activation + `aiData.nationalite` + recherche `procedureChecks` ordonnance JAF dans pièces.
- [ ] **CA-07** : F-IA-03 sur `dateOrdonnanceProtection` (divergence vs piece-manquante / ordonnance détectée)
- [ ] **CA-08** : `getPrefillCount`
- [ ] **CA-09** : TOOL_REGISTRY `F-IM-24-victime-violences-l4256-fr` symétrique
- [ ] **CA-10** : tests Jest ≥ 15 dont 3 dédiés aux 3 verdicts

### Parité domaines (niveau 5)
- Famille FR : ordonnance de protection JAF Cciv 515-9+ → outil F-FA-14 existe-t-il ? Si non, à proposer en F-FA backlog (mais hors scope F-208).
- Travail FR : non applicable.
- Immigration BE : équivalent protection L.40 / 9ter — feature jumelle à proposer dans F-209 (P1 Immigration BE).

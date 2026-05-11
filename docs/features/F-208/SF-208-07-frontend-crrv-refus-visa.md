# SF-208-07 — CRRV recours refus de visa (2 mois) frontend (Angular)

## Identifiant
`F-208 / SF-208-07`

## Statut
`draft` — 2026-05-11

## Branche Git
`feat/SF-208-frontend-immigration-fr-p1` (commune SF-208-05..08)

## Pattern de référence
`oqtf-avec-delai-section` + symétrie SF-208-05/06.

## Objectif
Composant `<app-crrv-refus-visa-section>` qui consomme `POST/GET /api/v1/case-files/{caseFileId}/crrv-refus-visa-analysis` (SF-208-03 backend mergée). tool_id `F-IM-23-crrv-refus-visa-fr`. Visibility `ALWAYS_ON` (pas de flag IA dédié).

## Critères d'acceptation
- [ ] **CA-01** : service `CrrvRefusVisaService` + modèle DTO
- [ ] **CA-02** : composant standalone OnPush, palette canonique (URGENT ≤ 7 j)
- [ ] **CA-03** : inputs — `dateNotificationRefus` (date), `typeVisa` (select enum : COURT_SEJOUR / LONG_SEJOUR / REGROUPEMENT_FAMILIAL / ETUDIANT / AUTRE), `motifRefus` (textarea 200 c.), `recoursForme` (checkbox), `dateRecours` (date)
- [ ] **CA-04** : verdict — `statut` + `dateExpirationRecoursCrrv` (notif + 2 mois) + `dateExpirationRecoursTaNantes` (calculé si decisionCrrv saisie ultérieurement, V2) + messages "préalable obligatoire CRRV avant TA Nantes"
- [ ] **CA-05** : gate country FR-only
- [ ] **CA-06** : pré-fill IA — `aiData.dateNotificationDecision` + `aiData.typeVisa` (si extrait par Sonnet) + `aiData.motifRefusVisa`
- [ ] **CA-07** : F-IA-03 sur `dateNotificationRefus`
- [ ] **CA-08** : `getPrefillCount`
- [ ] **CA-09** : TOOL_REGISTRY `F-IM-23-crrv-refus-visa-fr` symétrique
- [ ] **CA-10** : tests Jest ≥ 15

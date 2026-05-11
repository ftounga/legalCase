# SF-208-06 — Dublin recours 7 j suspensif frontend (Angular)

## Identifiant
`F-208 / SF-208-06`

## Statut
`draft` — 2026-05-11

## Branche Git
`feat/SF-208-frontend-immigration-fr-p1` (commune SF-208-05..08)

## Pattern de référence
`oqtf-avec-delai-section` + symétrie SF-208-05 (jld-retention).

## Objectif
Composant `<app-dublin-recours-section>` qui consomme `POST/GET /api/v1/case-files/{caseFileId}/dublin-recours-analysis` (SF-208-02 backend mergée). tool_id `F-IM-22-dublin-recours-fr`.

## Critères d'acceptation (~symétrique SF-208-05)
- [ ] **CA-01** : service `DublinRecoursService` + modèle DTO miroir
- [ ] **CA-02** : composant standalone OnPush, palette canonique (rouge URGENT ≤ 2 j / EXPIRE)
- [ ] **CA-03** : inputs — `dateNotificationDecisionTransfert` (date), `etatMembreResponsable` (string ISO/nom), `motifTransfert` (select enum), `recoursForme` (checkbox), `dateRecours` (date, désactivé si !recoursForme)
- [ ] **CA-04** : verdict — `statut` (DISPONIBLE/URGENT/EXPIRE/RECOURS_FORME) + `dateExpirationRecours` + `dateLimiteTransfertEffectif` (6 mois Dublin III) + `effetSuspensif` "automatique" en or souligné
- [ ] **CA-05** : gate country (FR-only)
- [ ] **CA-06** : pré-fill IA — `aiData.dateNotificationDecision` + `aiData.nationalite` (info hint sur état membre)
- [ ] **CA-07** : F-IA-03 sur `dateNotificationDecisionTransfert` (divergence vs procedureChecks `NOTIFICATION_DECISION`)
- [ ] **CA-08** : `getPrefillCount`
- [ ] **CA-09** : TOOL_REGISTRY `F-IM-22-dublin-recours-fr` symétrique
- [ ] **CA-10** : tests Jest ≥ 15

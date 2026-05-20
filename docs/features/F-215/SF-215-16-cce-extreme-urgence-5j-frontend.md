# SF-215-16 — Recours CCE extrême urgence 5j — frontend

## Identifiant
`F-215 / SF-215-16`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-16-cce-extreme-urgence-5j-be-frontend`

---

## Objectif
Livrer `<app-cce-extreme-urgence-be-section>` conforme F-IA-04 : calculateur CCE extrême urgence 5j ouvrables, CONTEXTUAL sur `recours_cce_extreme_urgence`, BELGIQUE UNIQUEMENT. Cas d'urgence absolue — affichage proéminent.

---

## Comportement attendu

- Formulaire : dateActeExecutoire (date), typeActe (dropdown), recoursForme (checkbox), dateRecours (date conditionnelle).
- Bandeau rouge CRITIQUE avec `actionImmediate` si statut CRITIQUE ou EXPIRE.
- Délai affiché : `joursOuvrables Restants` en gros (typographie `JetBrains Mono`), avec étiquette « jours ouvrables BE ».
- `audienceEstimee` : info-box « Audience CCE estimée le [date] ».

---

## Conformité F-IA-04

### 1. Cohérence visuelle
- [x] CRITIQUE et EXPIRE = rouge prominent (urgence absolue — exception justifiée au rouge réservé alertes critiques).
- [x] `dateLimiteRecours` et `audienceEstimee` en `JetBrains Mono`.
- [x] Bandeau rouge CRITIQUE avec message d'action immédiate.

### 2-3. Pré-fill / F-IA-03
- [x] 2 champs réels : `provenanceDateActe`, `provenanceTypeActe`
- [x] Alert F-IA-03 si `dateActeExecutoire` diverge de `recoursExtremeUrgenceDateActe` IA

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-32-cce-extreme-urgence-5j-be`
- [x] `getPrefillCount` : 2 max
- [x] Tests Jest : 0, partiel, nominal
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 4 (calculateur délais). BE-only. Pas d'équivalent FR utilisable (JLD FR = procédure différente, déjà couvert F-208).

---

## Critères d'acceptation

- [ ] CONTEXTUAL `recours_cce_extreme_urgence=true`
- [ ] Bandeau rouge CRITIQUE si ≤ 2 jours ouvrables restants
- [ ] Affichage `joursOuvrables Restants` prominent JetBrains Mono
- [ ] `audienceEstimee` info-box
- [ ] `getPrefillCount` = 2 si 2 champs présents
- [ ] ≥ 12 tests Jest
- [ ] Self-check grep `F-IM-32-cce-extreme-urgence-5j-be`

## Dépendances
- SF-215-15 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

# SF-215-18 — Annexe 13quinquies OQT + interdiction d'entrée — frontend

## Identifiant
`F-215 / SF-215-18`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-18-annexe13quinquies-ie-be-frontend`

---

## Objectif
Livrer `<app-annexe13quinquies-be-section>` conforme F-IA-04 : outil Annexe 13quinquies OQT + IE (durée IE, délais CCE, levée précoce), CONTEXTUAL sur `interdiction_entree_be_detectee`, BELGIQUE UNIQUEMENT.

---

## Comportement attendu

- Formulaire : dateNotificationAnnexe (date), motifInterdictionEntree (dropdown), precedentSejour (checkbox), recoursForme (checkbox), dateRecours (date conditionnelle).
- Résultat : `dureeInterdiction` (badge X ans), `dateFinInterdiction` (JetBrains Mono), `datePossibleLevePrecoce` (JetBrains Mono avec étiquette « levée possible à partir du »), `dateLimiteRecoursAnnulation` (badge statut couleur), `conditionsLevee` (liste).
- Lien croisé vers `<app-cce-annulation-be-section>` si `statutRecours = URGENT`.

---

## Conformité F-IA-04

### 1. Cohérence visuelle
- [x] Durée IE badge : 3 ans = orange, 5 ans = rouge, 8 ans = rouge sombre.
- [x] Dates en JetBrains Mono.

### 2-3. Pré-fill / F-IA-03
- [x] 2 signals : `provenanceDateNotification`, `provenanceMotif`
- [x] Alert F-IA-03 si `dateNotificationAnnexe` diverge de `interdictionEntreeDateNotification` IA

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-33-annexe13quinquies-ie-be`
- [x] `getPrefillCount` : 2 max
- [x] Tests Jest : 0, partiel, nominal
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 4 (calculateur). BE-only. Équivalent FR : interdiction du territoire L.541-1+ (procédure différente, pas de pendant dans cet outil).

---

## Champs IA (2 réels)

| Champ | Source |
|-------|--------|
| `dateNotificationAnnexe` | `interdictionEntreeDateNotification` |
| `motifInterdictionEntree` | `interdictionEntreeMotif` |

---

## Critères d'acceptation

- [ ] CONTEXTUAL `interdiction_entree_be_detectee=true`
- [ ] Badge durée IE coloré (3/5/8 ans)
- [ ] `datePossibleLevePrecoce` affichée
- [ ] `statutRecours` coloré + lien croisé si URGENT
- [ ] `getPrefillCount` = 2
- [ ] ≥ 12 tests Jest
- [ ] Self-check grep `F-IM-33-annexe13quinquies-ie-be`

## Dépendances
- SF-215-17 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

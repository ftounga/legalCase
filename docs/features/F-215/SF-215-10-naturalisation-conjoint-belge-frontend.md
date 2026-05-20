# SF-215-10 — Naturalisation conjoint Belge art. 16 — frontend

## Identifiant
`F-215 / SF-215-10`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-10-naturalisation-conjoint-belge-be-frontend`

---

## Objectif
Livrer `<app-naturalisation-conjoint-belge-be-section>` conforme F-IA-04 : analyse éligibilité voie art. 16 CNB, CONTEXTUAL sur `naturalisation_be_envisagee`, BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- Formulaire : dateMarriage, dureeCohabitationMois (number), niveauLangue (dropdown), preuveIntegration (checkbox), menaceOrdrePublic (checkbox), condamnationPenale (checkbox).
- Verdict : badge ELIGIBLE vert / INELIGIBLE rouge. `dureeManquante` affiché. `criteresNonRemplis` listés.

---

## Conformité F-IA-04

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-29-naturalisation-conjoint-belge-be`
- [x] `getPrefillCount` : 3 max (dateMarriage + dureeCohabitation + niveauLangue)
- [x] Tests Jest : 0, partiel (1/3), nominal (3)
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 5. Symétrie avec art. 12bis ✅. Voie art. 16 = une situation distincte (mariage avec Belge).

---

## Champs IA

| Champ | Source | Note |
|-------|--------|------|
| `dateMarriage` | `naturalisationBeArt16DateMarriage` | Réel |
| `dureeCohabitationMois` | `naturalisationBeArt16DureeCohabitation` | Réel |
| `niveauLangue` | `naturalisationBeArt16NiveauLangue` | Réel |
| `preuveIntegration` | — | Aspirationnel |
| `cohabitationLegale` | — | Aspirationnel |

---

## Critères d'acceptation

- [ ] CONTEXTUAL `naturalisation_be_envisagee=true`
- [ ] Badge ELIGIBLE / INELIGIBLE coloré
- [ ] `dureeManquante` affiché si > 0
- [ ] `getPrefillCount` = 3 si 3 champs réels présents
- [ ] ≥ 12 tests Jest
- [ ] Self-check grep `F-IM-29-naturalisation-conjoint-belge-be`

## Dépendances
- SF-215-09 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

# SF-215-08 — Naturalisation art. 12bis — frontend

## Identifiant
`F-215 / SF-215-08`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-08-naturalisation-12bis-be-frontend`

---

## Objectif
Livrer `<app-naturalisation-12bis-be-section>` conforme F-IA-04 : scoring éligibilité voie 5 ans / voie 10 ans / aucune, avec pré-remplissage IA (3 champs réels), BELGIQUE UNIQUEMENT. Jumeau BE de `<app-naturalisation-section>` (F-IM-13 FR).

---

## Comportement attendu

### Cas nominal
- CONTEXTUAL sur `naturalisation_be_envisagee=true`, workspace BELGIQUE × DROIT_IMMIGRATION.
- Formulaire : dureeSejour (number), typeSejour (radio LIMITE/ILLIMITE), niveauLangue (dropdown), preuveIntegration (checkbox), preuveEmploi (checkbox), menaceOrdrePublic (checkbox), condamnationPenale (checkbox).
- Verdict : `voieEligible` badge VOIE_5_ANS vert / VOIE_10_ANS vert / AUCUNE rouge. `dureeManquante` affiché si > 0 (« X mois à attendre »). `criteresNonRemplis` listés.

---

## Conformité F-IA-04

### 1-3. Cohérence visuelle / Pré-fill IA / F-IA-03
- [x] VOIE_5_ANS / VOIE_10_ANS = vert, AUCUNE = rouge
- [x] 3 signals provenance réels : `provenanceDureeSejour`, `provenanceTypeSejour`, `provenanceNiveauLangue`
- [x] `preuveIntegration` / `preuveEmploi` / `menaceOrdrePublic` / `condamnationPenale` : aspirationnels → `PREFILL_COUNT_ALWAYS_ZERO` pour ces 4 champs
- [x] Alert F-IA-03 si `dureeSejour` diverge de `naturalisationBeDureeSejour` IA

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-28-naturalisation-12bis-be`
- [x] `getPrefillCount` : 3 max (durée + type + langue)
- [x] Tests Jest : 0, partiel, nominal (3)
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 5 (scoring éligibilité). Jumeau BE de `F-IM-13-naturalisation` FR ✅.

---

## Champs IA

| Champ | Source | Note |
|-------|--------|------|
| `dureeSejour` | `naturalisationBeDureeSejour` | Réel |
| `typeSejour` | `naturalisationBeTypeSejour` | Réel |
| `niveauLangue` | `naturalisationBeNiveauLangue` | Réel |
| `preuveIntegration` | — | Aspirationnel (`PREFILL_COUNT_ALWAYS_ZERO`) |
| `preuveEmploi` | — | Aspirationnel |
| `menaceOrdrePublic` | — | Aspirationnel |
| `condamnationPenale` | — | Aspirationnel |

---

## Critères d'acceptation

- [ ] Outil CONTEXTUAL `naturalisation_be_envisagee=true`
- [ ] Badge VOIE_5_ANS / VOIE_10_ANS / AUCUNE coloré
- [ ] `dureeManquante` affiché si ≠ 0
- [ ] `getPrefillCount` = 3 si durée+type+langue présents dans aiData, 0 sinon
- [ ] ≥ 12 tests Jest
- [ ] BUILD SUCCESS
- [ ] Self-check grep `F-IM-28-naturalisation-12bis-be`

---

## Dépendances
- SF-215-07 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

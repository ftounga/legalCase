# SF-215-06 — Regroupement art. 10bis — frontend

## Identifiant
`F-215 / SF-215-06`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-06-regroupement-10bis-be-frontend`

---

## Objectif
Livrer le composant Angular `<app-regroupement-10bis-be-section>` conforme F-IA-04, consommant SF-215-05, CONTEXTUAL sur `regroupement_10bis_detecte`, BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- Formulaire : lienFamilial (dropdown), revenusMensuelsNetsRegroupant (number), dureeSejour (number), dateFinCarteA (date), logementConforme (checkbox), assuranceMaladie (checkbox), menaceOrdrePublic (checkbox).
- Verdict identique à SF-215-04 (ELIGIBLE/SOUS_RESERVE/INELIGIBLE, score, différentiel, critères non remplis).
- Champ `dateFinCarteA` avec alerte visuelle si date expirée.

---

## Conformité F-IA-04

### 1-3. Cohérence visuelle / Pré-fill IA / Validation F-IA-03
Identiques à SF-215-04 (mêmes règles, champs différents).
- [x] 4 signals provenance : `provenanceLienFamilial`, `provenanceRevenus`, `provenanceDureeSejour`, `provenanceDateFinCarte`
- [x] Alert F-IA-03 sur `dateFinCarteA` si diverge de `be10bisDateFinCarteA` IA

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-27-regroupement-10bis-be`
- [x] `getPrefillCount` : 4 champs max
- [x] Tests Jest : 0, partiel, nominal
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 5 (scoring éligibilité). Symétrie avec 40ter BE ✅. Asymétrie assumée avec FR (procédures différentes).

---

## Champs IA (4 champs — livré SF-215-05)

| Champ | Source `ImmigrationExtractedData` |
|-------|-----------------------------------|
| `lienFamilial` | `be10bisLienFamilial` |
| `revenusMensuelsNetsRegroupant` | `be10bisRevenusMensuels` |
| `dureeSejour` | `be10bisDureeSejour` |
| `dateFinCarteA` | `be10bisDateFinCarteA` |

---

## Critères d'acceptation

- [ ] Outil affiché uniquement si `regroupement_10bis_detecte=true`
- [ ] `dateFinCarteA` passée → bandeau avertissement « carte A expirée »
- [ ] Pré-remplissage 4 champs avec badges provenance
- [ ] ≥ 12 tests Jest verts
- [ ] Self-check grep `F-IM-27-regroupement-10bis-be`

---

## Dépendances
- SF-215-05 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

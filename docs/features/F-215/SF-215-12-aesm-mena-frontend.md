# SF-215-12 — AESM + tutelle DGDE (MENA) — frontend

## Identifiant
`F-215 / SF-215-12`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-12-aesm-mena-be-frontend`

---

## Objectif
Livrer `<app-aesm-mena-be-section>` conforme F-IA-04 : outil composite MENA 2 volets (tutelle DGDE + AESM scoring), CONTEXTUAL sur `mineur_non_accompagne_be_detecte`, BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- Formulaire en 2 parties clairement délimitées :
  - **Partie 1 — Tutelle DGDE** : tuteurDesigné (checkbox), dateArrivéeBelgique (date).
  - **Partie 2 — AESM** : ageActuel (number), integrationScolaire (checkbox), dureeScolaire (number si applicable), projetVieElabore (checkbox), perspectiveAutonomie (checkbox), menaceOrdrePublic (checkbox).
- Verdict : bandeau d'urgence rouge si `prioriteUrgence=true` (ageActuel ≥ 17). Badge `verdictAESM` (FAVORABLE vert / SOUS_RESERVE orange / DEFAVORABLE rouge). Si `etapeTutelle` renseigné → bloc info orange « À faire : [etapeTutelle] ».

---

## Conformité F-IA-04

### 1-3. Cohérence visuelle / Pré-fill / F-IA-03
- [x] Bandeau urgence rouge si ageActuel ≥ 17 → message « Approche de la majorité — procédure urgente »
- [x] 3 signals provenance réels : `provenanceAge`, `provenanceDateArrivee`, `provenanceDureeScolaire`
- [x] 5 champs aspirationnels documentés `PREFILL_COUNT_ALWAYS_ZERO`
- [x] Alert F-IA-03 si `ageActuel` diverge de `menaAge` IA

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-30-aesm-mena-be`
- [x] `getPrefillCount` : 3 max (age + dateArrivée + dureeScolaire)
- [x] Tests Jest : 0, partiel, nominal
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 5. Symétrie avec 9bis adulte (F-IM-14-9bis) ✅ — situations distinctes (MENA ≠ adulte). Pas d'équivalent FR utilisable (MNA FR = ordonnance JE, procédure entièrement différente).

---

## Champs IA (3 réels)

| Champ | Source |
|-------|--------|
| `ageActuel` | `menaAge` |
| `dateArrivéeBelgique` | `menaDateArrivee` |
| `dureeScolaire` | `menaDureeScolaire` |

---

## Critères d'acceptation

- [ ] CONTEXTUAL `mineur_non_accompagne_be_detecte=true`
- [ ] Bandeau urgence si ageActuel ≥ 17
- [ ] Bloc info orange si `tuteurDesigné=false` avec étape à faire
- [ ] Badge verdict 3 couleurs
- [ ] `getPrefillCount` = 3 si 3 champs réels présents
- [ ] ≥ 15 tests Jest (composant composite + service + prefill-rules)
- [ ] Self-check grep `F-IM-30-aesm-mena-be`

## Dépendances
- SF-215-11 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

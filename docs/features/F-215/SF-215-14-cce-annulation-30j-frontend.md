# SF-215-14 — Recours CCE annulation 30j — frontend

## Identifiant
`F-215 / SF-215-14`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-14-cce-annulation-30j-be-frontend`

---

## Objectif
Livrer `<app-cce-annulation-be-section>` conforme F-IA-04 : calculateur délais CCE annulation 30j calendaires, CONTEXTUAL sur `recours_cce_envisage`, BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- Formulaire : dateNotificationDecision (date), typeDecision (dropdown), recoursForme (checkbox), dateRecours (date, conditionnel si recoursForme=true).
- Verdict : badge statut (DISPONIBLE vert / URGENT orange / EXPIRE rouge / RECOURS_FORME bleu), `dateLimiteRecours` en JJ/MM/YYYY typographie `JetBrains Mono`, `joursRestants` affiché (négatif en rouge), `delaisMemoire` info-box, `recommandation` info si URGENT/EXPIRE (lien croisé vers F-IM-32).

---

## Conformité F-IA-04

### 1. Cohérence visuelle
- [x] URGENT = orange, EXPIRE = rouge (critique). DISPONIBLE = vert. RECOURS_FORME = bleu info.
- [x] `dateLimiteRecours` en `JetBrains Mono`.
- [x] Bannière info si workspace FR.

### 2. Pré-fill IA
- [x] 2 signals : `provenanceDateNotification`, `provenanceTypeDecision`
- [x] 2 champs aspirationnels : recoursForme, dateRecours → `PREFILL_COUNT_ALWAYS_ZERO`

### 3. F-IA-03
- [x] Alert si `dateNotificationDecision` diverge de `recoursCceDateNotification` IA

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-31-cce-annulation-30j-be`
- [x] `getPrefillCount` : 2 max (dateNotification + typeDecision)
- [x] Tests Jest : 0, partiel, nominal
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 4 (calculateur délais). Non applicable — recours CCE est immigration BE-only. Équivalent FR = recours TA/CNDA déjà couvert (F-IM-06/08 FR).

---

## Champs IA (2 réels)

| Champ | Source |
|-------|--------|
| `dateNotificationDecision` | `recoursCceDateNotification` |
| `typeDecision` | `recoursCceTypeDecision` |

---

## Critères d'acceptation

- [ ] CONTEXTUAL `recours_cce_envisage=true`
- [ ] Badge 4 états colorés
- [ ] `dateLimiteRecours` JetBrains Mono
- [ ] `recommandation` lien croisé F-IM-32 si URGENT ou EXPIRE
- [ ] `getPrefillCount` = 2 si 2 champs présents
- [ ] ≥ 12 tests Jest
- [ ] Self-check grep `F-IM-31-cce-annulation-30j-be`

## Dépendances
- SF-215-13 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

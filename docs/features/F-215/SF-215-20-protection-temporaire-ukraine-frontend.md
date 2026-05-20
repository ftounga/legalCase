# SF-215-20 — Protection temporaire Ukraine BE — frontend

## Identifiant
`F-215 / SF-215-20`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-20-protection-temporaire-ukraine-be-frontend`

---

## Objectif
Livrer `<app-protection-temporaire-ukraine-be-section>` conforme F-IA-04 : outil PT Ukraine (éligibilité, durée restante, droits, chemin procédural), CONTEXTUAL sur `protection_temporaire_ukraine_detectee`, BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- Formulaire : dateArrivee (date), nationaliteUkrainienne (checkbox), residenceUkraineAvant24Fev2022 (checkbox), apatridesUkraine (checkbox), membreFamilleProtege (checkbox), titreSejourBE (dropdown).
- Résultat : badge ELIGIBLE vert / INELIGIBLE rouge, `dureeProtectionRestante` (X jours — JetBrains Mono), bandeau orange si `prochainRenouvellement` imminent, bloc info `droitsTravail` (mention « pas de single permit »), `cheminProcedure` (stepper ou liste numérotée).

---

## Conformité F-IA-04

### 1. Cohérence visuelle
- [x] ELIGIBLE vert / INELIGIBLE rouge. Bandeau orange si renouvellement < 90j.
- [x] `dureeProtectionRestante` en JetBrains Mono.
- [x] Bloc info droits travail proéminent (information critique pour le client).

### 2-3. Pré-fill / F-IA-03
- [x] 2 signals : `provenanceDateArrivee`, `provenanceNationalite`
- [x] Alert F-IA-03 si `dateArrivee` diverge de `ptUkraineDateArrivee` IA

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-34-protection-temporaire-ukraine-be`
- [x] `getPrefillCount` : 2 max (dateArrivee + nationaliteUkrainienne)
- [x] Tests Jest : 0, partiel, nominal
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 4 (checklist + calculateur). BE-only. Régime PT Ukraine = spécificité BE/UE sans équivalent FR comparable à ce stade.

---

## Champs IA (2 réels)

| Champ | Source |
|-------|--------|
| `dateArrivee` | `ptUkraineDateArrivee` |
| `nationaliteUkrainienne` | `ptUkraineNationalite` |

---

## Critères d'acceptation

- [ ] CONTEXTUAL `protection_temporaire_ukraine_detectee=true`
- [ ] Badge ELIGIBLE / INELIGIBLE
- [ ] Bandeau orange si `dureeProtectionRestante < 90`
- [ ] Bloc droits travail proéminent avec mention single permit non requis
- [ ] `cheminProcedure` étapes listées
- [ ] `getPrefillCount` = 2 si 2 champs présents
- [ ] ≥ 12 tests Jest
- [ ] Self-check grep `F-IM-34-protection-temporaire-ukraine-be`

---

## Dépendances
- SF-215-19 — statut : ready

## Analyse d'impact
- [x] Outil décisionnel métier
- [x] Smoke E2E avant push

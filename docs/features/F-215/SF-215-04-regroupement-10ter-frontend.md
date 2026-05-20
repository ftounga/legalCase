# SF-215-04 — Regroupement art. 10ter — frontend

## Identifiant
`F-215 / SF-215-04`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-04-regroupement-10ter-be-frontend`

---

## Objectif
Livrer le composant Angular `<app-regroupement-10ter-be-section>` conforme F-IA-04, consommant le backend SF-215-03, avec scoring d'éligibilité, `differentielRevenus` et pré-remplissage IA (4 champs), BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- Affiché uniquement si `regroupement_10ter_detecte = true` (CONTEXTUAL) sur workspace BELGIQUE × DROIT_IMMIGRATION.
- Formulaire : lienFamilial (dropdown), typeCarteRegroupant (radio CARTE_B / CARTE_C), revenusMensuelsNetsRegroupant (number input), dureeSejour (number input), logementConforme (checkbox), assuranceMaladie (checkbox), menaceOrdrePublic (checkbox).
- Après POST : verdict (badge ELIGIBLE vert / SOUS_RESERVE orange / INELIGIBLE rouge), scoreEligibilite (jauge 0-100), differentielRevenus (affiché signé, ex : « +450 € / mois par rapport au seuil »), criteresNonRemplis (liste bullets rouge).

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| POST 400 | MatSnackBar erreur |
| workspace FR | Bannière info |
| flag `regroupement_10ter_detecte` false | Outil non affiché |

---

## Conformité F-IA-04

### 1. Cohérence visuelle
- [x] ELIGIBLE = vert, SOUS_RESERVE = orange, INELIGIBLE = rouge. `differentielRevenus` : vert si positif, rouge si négatif.
- [x] Dates : sans date ici (formulaire entiers + enums + booléens).
- [x] Gate BE + bannière info si FR.
- [x] MatSnackBar erreurs.
- [x] `CaseDashboardRefreshService.triggerRefresh()` dans `next:`.

### 2. Pré-fill IA
- [x] `@Input() aiData?: ImmigrationExtractedData`
- [x] `prefillFromAi()` dans `ngOnInit()` ET `ngOnChanges()`
- [x] Signals : `provenanceLienFamilial`, `provenanceTypeCarte`, `provenanceRevenus`, `provenanceDureeSejour`
- [x] Badge `auto_awesome` par champ

### 3. Validation F-IA-03
- [x] Alert si `revenusMensuelsNets` saisi diverge de `be10terRevenusMensuels` IA
- [x] `CoherenceAlertBuilder` partagé

### 4. TOOL_REGISTRY + `getPrefillCount`
- [x] Entrée `F-IM-26-regroupement-10ter-be`
- [x] `getPrefillCount` : 4 champs max
- [x] Tests Jest : 0, partiel, nominal
- [x] Self-check grep pré-commit

### 5. Parité domaines
- Niveau 5 (scoring éligibilité). Équivalent FR : regroupement familial FR couvert par d'autres outils. Équivalent BE Belge : `F-IM-14-40ter-familial-belge-be` ✅. Divergence assumée : 10ter ≠ 40ter — situations distinctes.

---

## Champs IA (4 champs — livré SF-215-03)

| Champ | Source `ImmigrationExtractedData` |
|-------|-----------------------------------|
| `lienFamilial` | `be10terLienFamilial` |
| `typeCarteRegroupant` | `be10terTypeCarte` |
| `revenusMensuelsNetsRegroupant` | `be10terRevenusMensuels` |
| `dureeSejour` | `be10terDureeSejour` |

---

## Critères d'acceptation

- [ ] Outil affiché uniquement si flag `regroupement_10ter_detecte=true`
- [ ] Pré-remplissage 4 champs avec badges provenance
- [ ] Verdict ELIGIBLE / SOUS_RESERVE / INELIGIBLE avec badge coloré
- [ ] `differentielRevenus` affiché signé
- [ ] `criteresNonRemplis` listés en rouge
- [ ] `getPrefillCount` retourne 4 si tous les champs IA présents
- [ ] ≥ 12 tests Jest verts
- [ ] `npm run build` BUILD SUCCESS, aucune régression
- [ ] Self-check grep `F-IM-26-regroupement-10ter-be` dans TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS

---

## Hors périmètre
- Regroupement 10bis (SF-215-05/06)

---

## Dépendances
- SF-215-03 — statut : ready (doit être mergée avant)

---

## Plan de test
- `Regroupement10terBeSectionComponent.spec.ts`
- `Regroupement10terBeService.spec.ts`
- `regroupement-10ter-be-prefill-rules.spec.ts`

## Analyse d'impact
- [x] Outil décisionnel métier — `F-IM-26-regroupement-10ter-be` dans TOOL_REGISTRY
- [x] Smoke E2E avant push

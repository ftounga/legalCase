# Mini-spec — F-237 / SF-237-02 Test présence helper + convention wrappers

## Identifiant

`F-237 / SF-237-02`

## Feature parente

`F-237`

## Statut

`draft`

## Date de création

2026-05-11

## Branche Git

`feat/SF-237-02-helper-obligatoire-wrappers`

---

## Objectif

(a) Étiqueter explicitement les 14 composants TOOL_REGISTRY actuellement sans helper avec `static readonly PREFILL_COUNT_ALWAYS_ZERO = true` quand légitimes (wrappers count=0). (b) Pour les 3 vraies infractions identifiées SF-236-05 (divorce-faute, transaction, travail-procedure) : extraire le helper + le rendre conforme. (c) Le test SF-237-01 vérifie alors que tout composant TOOL_REGISTRY a soit le helper, soit l'étiquette wrapper.

---

## Comportement attendu

### Cas nominal

1. Lister les 14 composants TOOL_REGISTRY sans fichier `<component>-prefill-rules.ts`
2. Pour chacun, décider :
   - Wrapper légitime (count=0 par design) → ajouter `static readonly PREFILL_COUNT_ALWAYS_ZERO = true;`
   - Vraie infraction → extraire le helper (méthode F-236 SF-236-02)
3. Le test SF-237-01 valide tout le périmètre

### Cas d'erreur

Aucun — refactor pur.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 14 composants concernés
- [ ] **Autres pays** : non applicable
- [ ] **Autres domaines** : non applicable
- [ ] **Autres UI patterns** : non applicable
- [ ] **Autres flows transversaux** : non applicable

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Wrappers count=0 légitimes | Oui | Étiquetage `PREFILL_COUNT_ALWAYS_ZERO` |
| 3 vraies infractions SF-236-05 | Oui | Extraction helper conforme |

### Décision

- [x] Étendu à toutes les cibles applicables

---

## Conformité F-IA-04

Toutes les conformités F-IA-04 sont préservées (refactor isolé sans changement UI).

---

## Critères d'acceptation

- [ ] Les 14 composants sont soit étiquetés `PREFILL_COUNT_ALWAYS_ZERO`, soit ont leur helper extrait
- [ ] Le test SF-237-01 PASSE sur master après merge
- [ ] `npm test` global : 0 régression
- [ ] Documentation du `PREFILL_COUNT_ALWAYS_ZERO` ajoutée dans `contract-prefill-rules.md`

---

## Périmètre

### Hors scope (explicite)

- Modification du test d'intégrité (couvert par SF-237-01)
- Modification CLAUDE.md (couvert par SF-237-03)

---

## Plan de test

### Tests unitaires

- Chaque helper extrait a son test Jest (3 cas 0/M/N) selon contrat F-236

---

## Analyse d'impact

- [x] Aucune préoccupation transversale

---

## Dépendances

- SF-237-01 — doit être `done` (le test sait quoi vérifier avant qu'on étiquette/extraie)

---

## Notes et décisions

### Liste des 14 composants à classer

À produire au démarrage de la SF en grepant `static getPrefillCount` dans `*.component.ts` et en comparant à la liste des fichiers `*-prefill-rules.ts`. Estimation initiale :
- ~10-11 wrappers count=0 (case-deadlines, crrv-refus-visa, dublin-recours, jld-retention, victime-violences-l4256, et autres scoring d'affichage)
- 3 vraies infractions à extraire (divorce-faute, transaction, travail-procedure)

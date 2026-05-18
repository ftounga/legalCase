# Mini-spec — F-98 / SF-98-53 — Bandeau « conclusions à régénérer »

> Cadrages amont : `SF-98-00-coherence.md` (étape 0, invariant 8 « re-génération signalée ») + `SF-98-00b-ux-coherence.md` (étape 0 bis — « bandeau de régénération SF-98-53 s'intègre dans la section conclusions »). Pas de nouveau cadrage écran.

## Identifiant
`F-98 / SF-98-53`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18 (PR #1008 backend + PR #1010 frontend).

## Date de création
2026-05-18

## Branches Git (dev parallélisé back / front)
- `feat/SF-98-53-backend-staleness`
- `feat/SF-98-53-frontend-bandeau`

---

## Objectif
Signaler à l'avocat, dans la section « Conclusions », qu'une version générée est **potentiellement périmée** parce que l'analyse du dossier a évolué depuis sa génération — et l'inviter à régénérer.

---

## Comportement attendu

### Cas nominal
1. À la lecture d'une version de conclusions (`GET .../conclusions` ou `.../versions/{id}`), le backend calcule un booléen `stale` : la version est périmée si une analyse du dossier (`CaseAnalysis` au statut `DONE`) porte un `updatedAt` **postérieur** au `generatedAt` de la version.
2. `ConclusionResponse` expose `stale` (booléen).
3. Quand `stale = true` et que la version est `DONE`, la section « Conclusions » affiche un **bandeau d'avertissement** : « L'analyse du dossier a évolué depuis la génération de cette version — pensez à régénérer. » avec le bouton « Régénérer » déjà existant mis en évidence.
4. Une version fraîchement générée n'est jamais `stale` ; régénérer produit une nouvelle version non `stale`.

### Cas d'erreur / dégradation
| Situation | Comportement |
|---|---|
| Version non `DONE` | `stale = false` (rien à signaler) |
| Aucune analyse `DONE` / `generatedAt` nul | `stale = false` |
| Calcul en échec | Fail-open : `stale = false`, pas d'exception |

---

## Analyse de cohérence transversale
- [x] **Modifie** `ConclusionResponse` (+ `stale`) et le service de lecture des conclusions — extension additive, aucun contrat cassé.
- [x] **Transversal F-98** : le signal de péremption bénéficie à toutes les cellules de la matrice.
- [x] **Pas de nouvelle table** : `stale` est **calculé** à la lecture, jamais persisté.

### Décision
- [x] Étendu au point de lecture unique (`CaseConclusionCommandService` getConclusion / getVersion).

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document, pas un outil décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — `GET .../conclusions` et `.../versions/{id}` exposent `stale` ; `stale = true` si une `CaseAnalysis` `DONE` du dossier a `updatedAt > version.generatedAt`.
- [ ] **CA2** — `stale = false` pour une version non `DONE`, sans analyse, ou `generatedAt` nul.
- [ ] **CA3** — Calcul en échec → fail-open `stale = false`, pas d'exception propagée.
- [ ] **CA4** — Frontend : bandeau d'avertissement affiché quand `stale = true` sur une version `DONE` ; absent sinon.
- [ ] **CA5** — Le bandeau met en avant l'action « Régénérer » (bouton existant SF-98-01).
- [ ] **CA6** — Isolation workspace inchangée (lecture des analyses du dossier déjà contrôlé).

---

## Périmètre
### Hors scope
- Détection fine sur **chaque** input amont (outils décisionnels individuels, pièces, pistes) — V1 = signal sur l'analyse du dossier (`CaseAnalysis`), qui est le déclencheur dominant et le plus lisible. Une détection multi-source est une évolution ultérieure.
- Régénération automatique — le bandeau invite, l'avocat décide (cohérent avec l'invariant « la régénération est un choix explicite », cadrage écran).

---

## Technique

### Contrat API (FIGÉ — parallélisation back/front)
`ConclusionResponse` gagne **`stale: boolean`** (champ additif). Aucun nouvel endpoint.

### Tables impactées
| Table | Opération |
|---|---|
| `case_analyses` | SELECT (lecture du `updatedAt` de la dernière analyse `DONE`) |
| `case_conclusions` | SELECT (déjà lu) |

### Migration Liquibase
- [ ] **Non applicable** — `stale` est calculé, non persisté.

### Composants
- Backend : `CaseConclusionCommandService` (calcul `stale` dans `getConclusion` / `getVersion`), `ConclusionResponse` (+ `stale`). Lecture via `CaseAnalysisRepository` (déjà injecté).
- Frontend : `conclusion.model.ts` (+ `stale`), `ConclusionsSectionComponent` (bandeau d'avertissement conditionné à `stale()` + statut `DONE`).

---

## Plan de test
### Backend (UT + IT)
- [ ] `CaseConclusionCommandServiceTest` : `stale = true` si analyse `updatedAt` > `generatedAt` ; `false` si antérieure / pas d'analyse / version non `DONE` ; fail-open.
- [ ] `CaseConclusionControllerIT` : `stale` reflété dans `GET .../conclusions` et `.../versions/{id}`.
### Frontend (Jest)
- [ ] `conclusions-section.component.spec.ts` : bandeau affiché si `stale = true` + `DONE` ; absent sinon.
### Isolation workspace
- [x] Couverte par les contrôles existants (lecture des analyses du dossier déjà résolu en workspace).

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — extension additive de la lecture des conclusions.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-01 (génération), SF-98-52 (versions) — done.

## Notes et décisions
- `stale` **calculé à la volée**, jamais persisté — pas de migration, pas de risque d'incohérence de cache.
- V1 ciblée sur le signal `CaseAnalysis` : c'est l'évolution amont la plus fréquente et la plus lisible (« le dossier a été ré-analysé »).

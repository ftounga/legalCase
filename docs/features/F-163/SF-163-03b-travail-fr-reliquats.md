# Mini-spec — F-163 / SF-163-03b Travail FR — reliquats mode simulateur

## Identifiant

`F-163 / SF-163-03b`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-03b-travail-fr-reliquats`

---

## Objectif

> Étendre `SimulatorCalculatorRegistry` backend + `STANDALONE_READY_TOOL_IDS` frontend aux **~6 calculators Travail FR à signatures complexes** identifiés par l'audit 2026-05-11 comme reliquats (catégorie Z) : ils existent dans `TOOL_REGISTRY` frontend mais ne sont ni dans le registry backend, ni dans la whitelist standalone.

---

## Périmètre des reliquats à rattraper

L'audit a identifié pour le domaine Travail FR (catégorie Z, ~6 toolIds) :
- **F-DT-09-comparateur-indemnites** (FR) — comparateur d'indemnités utilisé quotidiennement
- **F-DT-14-rupture-conv-indemnite** — indemnité spécifique rupture conv
- **F-DT-20-rappel-salaire**
- **F-DT-25-indemnite-preavis**
- **F-132-rupture-amiable-info** (à valider : peut-être Catégorie Y wrapper info-only)
- **F-136-* autres reliquats Travail FR**

**L'agent doit faire l'inventaire exact** : pour chaque toolId dans `TOOL_REGISTRY` (domaine Travail FR) absent à la fois de `STANDALONE_READY_TOOL_IDS` ET de `SimulatorCalculatorRegistry`, vérifier qu'il a bien un calculator stateless dans le backend (catégorie Z). Si c'est en réalité un wrapper info-only ou un PDF generator, l'exclure et le documenter.

---

## Pattern à appliquer

**Backend** (pour chaque calculator catégorie Z) :
1. Identifier la signature exacte du calculator pur (`*Analyzer.analyze(...)` ou `*Calculator.calculate(...)`) dans `backend/src/main/java/fr/ailegalcase/casefile/`.
2. Identifier les dépendances (référentiel, service injecté) — si nécessaire, injecter ces dépendances dans `SimulatorCalculatorRegistry` constructor.
3. Ajouter `register("F-DT-XX-...", Request.class, req -> ...analyzer.analyze(req...))` dans `@PostConstruct` du registry.
4. Ajouter test IT dans `SimulatorCalculateControllerIT` : nominal 200 + non-persistance vérifiée.

**Frontend** (pour chaque composant catégorie Z) :
1. Pattern identique à SF-163-02b (ajout `@Input() standaloneMode: boolean = false`, bannière 🧪, bypass `prefillFromAi`/`coherenceAlerts`/`triggerRefresh`, endpoint switch vers `/api/v1/simulators/${toolId}/calculate`).
2. Ajouter chaque `tool_id` à `STANDALONE_READY_TOOL_IDS` dans `frontend/src/app/simulators/standalone-ready-tools.ts`.
3. Entrée TOOL_REGISTRY propage `standaloneMode` via `inputs(ctx)`.
4. ≥ 3 tests Jest nouveaux par composant.

---

## Critères d'acceptation

- [ ] **CA-01** : tous les calculators Travail FR catégorie Z identifiés sont enregistrés dans `SimulatorCalculatorRegistry` backend.
- [ ] **CA-02** : tous les composants Angular correspondants ont `@Input() standaloneMode` + bannière 🧪 + bypass IA/refresh.
- [ ] **CA-03** : `STANDALONE_READY_TOOL_IDS` étendue avec chaque toolId rattrapé.
- [ ] **CA-04** : 1 test IT backend nouveau par calculator (≥ 6 tests IT supplémentaires).
- [ ] **CA-05** : 3 tests Jest nouveaux par composant frontend (≥ 18 tests).
- [ ] **CA-06** : tests existants verts (régression 0).
- [ ] **CA-07** : `npm run build` production OK, `./mvnw test` global vert.
- [ ] **CA-08** : si un toolId initialement classé Z se révèle en réalité Y (info-only) ou X (PDF), le justifier explicitement dans le rapport agent + ne pas l'inclure.

---

## Hors scope

- Composants Famille (SF-163-03d/e) ou Immigration (SF-163-03c).
- Catégories X (PDF) et Y (info-only) — explicitement hors scope V1 (audit 2026-05-11).

---

## Dépendances

- **SF-163-02a/b/c/d** — done (pattern canonique).
- **SF-163-03** — done (dispatcher backend + registry MVP).

---

## Notes

- Conflits Git probables avec SF-163-03c/d/e (3 agents touchent les mêmes fichiers `SimulatorCalculatorRegistry.java`, `standalone-ready-tools.ts`, `decisional-tools-panel.component.ts`). Rebase + résolution propre (garder les listes mergées).

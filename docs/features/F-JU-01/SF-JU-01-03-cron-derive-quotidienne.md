# Mini-spec — F-JU-01 / SF-JU-01-03 Cron dérive quotidienne

## Identifiant
`F-JU-01 / SF-JU-01-03`

## Feature parente
`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels

## Statut
`draft`

## Date de création
2026-05-22

## Branche Git
`feat/SF-JU-01-03-cron-derive-quotidienne`

---

## Objectif

Cron quotidien `JurisprudenceDriftScheduler` qui détecte les mappings de `tool_jurisprudence_mappings` dont la `(tool_id, branche_calcul_id)` n'est plus déclarée par les outils décisionnels du code (suppression / renommage de branche côté calculator) → archive automatique le mapping orphelin + écrit un audit log `AUTO_ARCHIVE`.

---

## Comportement attendu

### Cas nominal

1. `@Scheduled(cron = "0 0 4 * * *", zone = "UTC")` — tous les jours à 4h UTC
2. Active uniquement si `jurisprudence.drift.enabled=true` (défaut **false**)
3. Lit l'agrégation des branches connues exposée par les beans `ToolBranchRegistry` du contexte Spring (chaque outil peut implémenter cette interface pour déclarer ses branches valides — V1, aucun outil n'implémente encore, le registry est donc vide)
4. Pour chaque mapping actif (`archived=false`) de `tool_jurisprudence_mappings` :
   - Si la clé `tool_id + ":" + branche_calcul_id` est dans le registry → OK, on continue
   - Sinon → mapping orphelin : `archived = true` + audit log `AUTO_ARCHIVE` (actor `CRON`, claude_reason `"Branche orpheline détectée par cron dérive"`)
5. Log INFO du total : `N mappings actifs, M orphelins archivés`

**Important V1** : tant qu'aucun outil n'implémente `ToolBranchRegistry`, le registry est vide → **TOUS les mappings seraient considérés orphelins**. C'est la raison pour laquelle l'activation est **désactivée par défaut** (`enabled=false`) : on l'activera après que les outils auront été instrumentés (V2 ou en même temps que SF-JU-01-04 frontend).

**Garde-fou V1** : si le registry est vide ET il existe des mappings actifs → **abort run** (le cron ne fait rien) avec log WARN « ToolBranchRegistry vide, cron dérive abandonne pour éviter archive massive ». Sécurité supplémentaire avant que les outils aient déclaré leurs branches.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Aucun bean `ToolBranchRegistry` enregistré | Registry vide → garde-fou abort silencieux |
| Bean `ToolBranchRegistry.knownBranches()` lève exception | log WARN, abort run |
| `enabled=false` | scheduler skip (log debug) |

---

## Analyse de cohérence transversale

- [x] **Autres outils** : transversal (lit toute la table `tool_jurisprudence_mappings`)
- [x] **Préoccupations transversales** : aucune (cron interne, écriture sur table globale)
- [x] **Décision** : intégré dans cette SF

## Conformité F-IA-04
- [x] **Non applicable** — SF backend pure.

## Champs IA à extraire
- [x] **Aucun pré-remplissage**.

---

## Critères d'acceptation

- [ ] **CA-01** — `ToolBranchRegistry` interface dans `fr.ailegalcase.jurisprudencemapping` avec méthode `Set<String> knownBranches()` (clés `"toolId:brancheCalculId"`).
- [ ] **CA-02** — Bean Spring qui collecte tous les `ToolBranchRegistry` du contexte et expose leur union via `ToolBranchRegistryAggregator.allKnownBranches()`.
- [ ] **CA-03** — `JurisprudenceDriftScheduler` `@Scheduled(cron = "0 0 4 * * *", zone = "UTC")` activé par `jurisprudence.drift.enabled` (défaut false).
- [ ] **CA-04** — `JurisprudenceDriftService.runDriftScan()` : pour chaque mapping actif, si non-présent dans le registry → archive + audit log `AUTO_ARCHIVE`.
- [ ] **CA-05** — **Garde-fou registry vide** : si le registry est vide ET il existe ≥ 1 mapping actif → abort + log WARN.
- [ ] **CA-06** — Tests UT couvrent : (a) registry vide + mappings → abort, (b) registry contient toutes les clés → 0 archive, (c) 1 mapping orphelin → archivé + audit log écrit, (d) scheduler skip si disabled.

---

## Périmètre

### Hors scope
- ❌ Détection de renommage trivial (heuristique fuzzy match) — V2 selon signal
- ❌ Implémentation `ToolBranchRegistry` pour les ~80 outils éligibles — chaque outil le fait au moment où ses mappings sont bootstrappés (SF-JU-01-05) ou ultérieurement

---

## Technique

### Classes Java introduites
- `ToolBranchRegistry` (interface) — exposée par les outils décisionnels
- `ToolBranchRegistryAggregator` (component) — agrège les beans `ToolBranchRegistry`
- `JurisprudenceDriftScheduler` (component) — `@Scheduled`
- `JurisprudenceDriftService` (service) — orchestrateur
- `JurisprudenceDriftRunSummary` (record) — compteurs pour log

### Migration Liquibase
- [x] **Non applicable** — réutilise tables SF-01.

---

## Plan de test

### Tests unitaires
- `JurisprudenceDriftServiceTest` — registry vide + 1 mapping = abort ; registry plein = 0 archive ; mapping orphelin = archivé + audit log ; mapping déjà archivé = ignoré ; aucun mapping actif = no-op safe
- `JurisprudenceDriftSchedulerTest` — skip si disabled, invoque si enabled, ne propage pas exception
- `ToolBranchRegistryAggregatorTest` — union de plusieurs beans `ToolBranchRegistry`, registry vide quand aucun bean

### IT
- [x] Non applicable (cron interne).

### Isolation workspace
- [x] Non applicable (table globale).

---

## Analyse d'impact
- [x] Aucune préoccupation transversale touchée
- [x] Aucun smoke test E2E concerné

---

## Dépendances

### SF bloquantes
- SF-JU-01-01 (Done — tables `tool_jurisprudence_mappings` + `jurisprudence_audit_log` créées)

### SF débloquées
- Aucune directement ; SF-JU-01-05 implémente `ToolBranchRegistry` pour les outils bootstrappés.

---

## Notes et décisions

1. **`enabled=false` par défaut + garde-fou registry vide** — double sécurité. On évite tout archive massif accidentel tant que les outils ne déclarent pas leurs branches.
2. **Cron quotidien (`0 0 4 * * *`)** plutôt qu'horaire — la dérive de code (suppression de branche) est rare. Quotidien suffit pour détecter sous 24h.
3. **Pas d'implémentation `ToolBranchRegistry` pour les outils en V1** — la SF livre l'infrastructure ; les outils s'y branchent au moment de leur bootstrap mappings (SF-JU-01-05) ou plus tard.
4. **Coût estimé** : ~1 j dev backend.

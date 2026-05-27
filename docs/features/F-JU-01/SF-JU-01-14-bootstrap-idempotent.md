# Mini-spec — F-JU-01 / SF-JU-01-14 Bootstrap idempotent : skip mapping existant

## Identifiant

`F-JU-01 / SF-JU-01-14`

## Feature parente

`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels (FR + BE) — full auto-pilot Claude

## Statut

`draft`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-01-14-bootstrap-idempotent`

## Exemptions

- **Étape 0 cadrage cohérence métier** : exempté — bugfix backend pur, ne modifie ni workflow ni invariant produit.
- **Étape 0bis cadrage cohérence écran** : exempté — aucun impact UI (le dashboard `/super-admin/jurisprudence-watch` continue de comportement inchangé côté frontend, seul le compteur `skipped` augmente sans erreur).

---

## Contexte (origine de la SF)

Référence : `docs/operations/hotfix-prod.md` → **HF-2026-05-27-03**.

Sur les 24 dernières heures, ≥ 200 erreurs `ERROR: duplicate key value violates unique constraint "uq_tool_jurisprudence_mappings_active"` ont été émises par `JurisprudenceBootstrapService` sur staging. Origine confirmée par `git grep` + lecture du service :

- `JurisprudenceBootstrapService.persistTopCandidates()` ligne 246 appelle `mappingRepository.save(mapping)` **sans vérification préalable d'existence**.
- La contrainte unique de la migration 282 porte sur `(tool_id, branche_calcul_id, arret_ref)`.
- Lorsqu'un super-admin re-déclenche un bootstrap (CSV identique ou ré-évaluation Claude qui retourne le même top-1), chaque entrée déjà mappée lève `DataIntegrityViolationException` et perd la persistance de cette entrée.

Effet de bord constaté : déclenche `HF-2026-05-27-01` (faux positifs `legalcase-production-backend-error-rate` via metric filter cross-namespace — corrigé séparément).

---

## Objectif

Rendre le bootstrap idempotent — un re-bootstrap sur des entrées déjà mappées ne génère ni `DataIntegrityViolationException`, ni log ERROR, ni perte de progression ; les entrées déjà présentes sont comptées comme **`skipped`** dans la progression du job.

---

## Comportement attendu

### Cas nominal — bootstrap d'une entrée jamais mappée

Comportement actuel inchangé : Claude retourne un `top-1`, `persistTopCandidates` insère le mapping + audit log `AUTO_ADD`, le compteur `created` augmente de 1.

### Cas nominal nouveau — bootstrap d'une entrée déjà mappée à l'arrêt cible

`persistTopCandidates` détecte qu'un mapping (`tool_id`, `branche_calcul_id`, `arret_ref` identique au top-1 Claude) existe déjà et **n'insère rien**. Le compteur `skipped` augmente de 1. Aucun log ERROR, aucun audit log additionnel.

### Cas nominal nouveau — bootstrap d'une entrée mappée à un autre arrêt

Claude propose un `top-1` différent du mapping actif existant pour la même `(tool_id, branche_calcul_id)`. Comportement V14 : **INSERT du nouveau mapping** (la contrainte unique porte sur l'`arret_ref` aussi, donc pas de conflit). Le compteur `created` augmente de 1. Pas d'archivage de l'ancien mapping (le bootstrap n'est pas un cron de mise à jour — c'est `JurisprudenceWatchService` mensuel qui gère les remplacements via audit log `REPLACE`).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Repository.existsBy… retourne `true` puis race-condition INSERT concurrent → contrainte unique lève toujours | Catch `DataIntegrityViolationException` au niveau `persistTopCandidates`, log en `INFO` (« mapping déjà inséré par run concurrent »), incrément `skipped`. Pas de propagation, pas d'arrêt du job. |
| Repository.existsBy… retourne `false` puis `save()` réussit | Comportement nominal — `created++`. |
| `save()` lève une autre `DataIntegrityViolationException` non liée à `uq_tool_jurisprudence_mappings_active` (ex : FK manquante) | Propagation au handler existant `runBootstrapJob` (transaction par-entrée SF-JU-01-09), `log.warn` ligne 204 conservé. |

---

## Critères d'acceptation

- **CA-1** : un re-bootstrap immédiat sur le même CSV (toutes entrées déjà mappées au même `arret_ref`) ne génère **aucune** ligne `ERROR` dans les logs ; le job termine en `COMPLETED` avec `entriesCreated=0`, `entriesSkipped=N`, `entriesFailed=0`.
- **CA-2** : un bootstrap sur un CSV mixte (50 % nouvelles entrées, 50 % déjà mappées au même `arret_ref`) termine avec `entriesCreated=50%`, `entriesSkipped=50%`, `entriesFailed=0`.
- **CA-3** : la métrique `BackendErrors` (filter `"ERROR"` sur le log group) ne croît pas pendant un re-bootstrap idempotent (lien indirect avec `HF-2026-05-27-01`).
- **CA-4** : aucune régression sur le comportement nominal d'un premier bootstrap (CA reprend le scénario `JurisprudenceBootstrapServiceTest` existant).

---

## Plan de test minimal

### Tests unitaires (Mockito)

Fichier : `backend/src/test/java/fr/ailegalcase/jurisprudencemapping/JurisprudenceBootstrapServiceTest.java`

- **T-1** : `persistTopCandidates_whenMappingAlreadyExists_skips` — mock `existsByToolIdAndBrancheCalculIdAndArretRef → true` → assert que `save()` n'est PAS appelé, que le retour est `0`, que `auditLogRepository.save()` n'est PAS appelé.
- **T-2** : `persistTopCandidates_whenMappingNew_persistsAndLogs` — mock `existsBy… → false` → assert que `save()` et `auditLogRepository.save()` sont appelés une fois chacun, retour `1`.
- **T-3** : `persistTopCandidates_whenRaceConditionDuplicate_swallowsAndCounts` — mock `existsBy… → false` puis `save() → throw DataIntegrityViolationException(message contient "uq_tool_jurisprudence_mappings_active")` → assert que l'exception est avalée, retour `0`.

### Test d'intégration

Fichier : `backend/src/test/java/fr/ailegalcase/jurisprudencemapping/JurisprudenceBootstrapServiceIT.java` (extension du IT existant ou nouveau).

- **IT-1** : 1er bootstrap avec 3 entrées → `entriesCreated=3` → 2e bootstrap avec les 3 mêmes entrées (même `arret_ref` Claude mocké) → `entriesCreated=0, entriesSkipped=3`, table `tool_jurisprudence_mappings` contient toujours 3 rows.

---

## Périmètre — fichiers impactés

| Fichier | Changement |
|---|---|
| `backend/src/main/java/fr/ailegalcase/jurisprudencemapping/ToolJurisprudenceMappingRepository.java` | +1 méthode `boolean existsByToolIdAndBrancheCalculIdAndArretRef(String, String, String)` (Spring Data dérivé du nom) |
| `backend/src/main/java/fr/ailegalcase/jurisprudencemapping/JurisprudenceBootstrapService.java` | Méthode `persistTopCandidates()` : insertion d'un guard `existsBy…` avant `save()`, retour `0` si déjà présent ; catch `DataIntegrityViolationException` autour du `save()` pour la race-condition (log INFO, retour 0). Signature inchangée, contrat externe inchangé. |
| `backend/src/main/java/fr/ailegalcase/jurisprudencemapping/JurisprudenceBootstrapService.java` (méthode `runBootstrapJob`) | Le retour `int` de `persistTopCandidates` continue d'alimenter `created/skipped` : aujourd'hui `created += retour`. Adapter pour : si retour 0 et exists=true → `skipped++` ; sinon `created++`. (Réfléchir au signal — voir Détail technique.) |
| `backend/src/test/java/fr/ailegalcase/jurisprudencemapping/JurisprudenceBootstrapServiceTest.java` | +3 tests (T-1, T-2, T-3) |

### Détail technique — signal `skipped` vs `created`

`persistTopCandidates()` retourne actuellement `int` toujours = 1. Pour distinguer `skipped` de `created`, deux options :
- **Option A** : changer le retour en `enum BootstrapPersistOutcome { CREATED, SKIPPED_EXISTS }`. Plus propre, plus typé.
- **Option B** : conserver `int`, retourner 0 si skip, 1 si created. Le caller incrémente `skipped` ou `created` selon le retour. Plus minimal, moins typé.

→ Décision : **Option B** retenue (minimisation de l'impact, pas de refactor d'API interne). Documenté dans le code via Javadoc.

### Hors périmètre

- **`JurisprudenceWatchService`** (cron mensuel) — les `save()` lignes 209/215/224/225/231 portent sur des updates (mapping déjà chargé) ou des archivages → pas de risque uq, non touché ici.
- **`JurisprudenceDriftService`** (cron quotidien) — les `save()` lignes 64/70 portent sur des updates (archived=true) → non touché.
- **Frontend dashboard** `/super-admin/jurisprudence-watch` — déjà compatible (affiche `entriesCreated`/`entriesSkipped`/`entriesFailed` via SF-JU-01-10 polling status, aucune modification).
- **Backfill de nettoyage** des erreurs déjà émises dans le passé — non concerné (logs jetables, pas de donnée corrompue en base).
- **Modification de la contrainte unique** — non, la contrainte est correcte et utile (empêche les doublons réels).

---

## Préoccupations transversales

- **Outil décisionnel métier** : ❌ aucun outil ajouté/modifié. Le service touché est une infrastructure interne (bootstrap des mappings) sans surface utilisateur.
- **Auth/Workspace** : ❌ inchangé (endpoint reste `SUPER_ADMIN` only).
- **Plans/limites** : ❌ inchangé.
- **Navigation/routing** : ❌ inchangé.

Aucun smoke test E2E requis (la skill `prod-health-check` re-comptera les erreurs au prochain audit ; absence d'erreur post-merge = succès observable).

---

## Risques

| Risque | Probabilité | Mitigation |
|---|---|---|
| Le test `existsBy…` ajoute une requête SQL supplémentaire par entrée (latence +~3-5ms × N) | Faible — le bootstrap est déjà I/O bound (JUDILIBRE + Claude), 5ms négligeable sur ~30s par cycle | Acceptable. Pas d'optim prématurée. |
| Une race condition entre 2 jobs concurrents (peu probable, le job est admin-only) | Très faible | Catch `DataIntegrityViolationException` sur le `save()` pour absorber le cas extrême. |

---

## Hors scope

- Refactor en `INSERT … ON CONFLICT DO NOTHING` natif PostgreSQL (préférerait `existsBy` + `save` Spring Data, plus portable et plus simple à tester).
- Métriques Prometheus dédiées (`bootstrap_skipped_total`) — peut être ajouté ultérieurement si signal utile.
- Refresh `lastVerifiedAt` sur mapping skippé — non, ça relève du cron veille mensuelle.

---

## DoD

- [ ] Tests unitaires T-1, T-2, T-3 verts.
- [ ] Test d'intégration IT-1 vert.
- [ ] `mvn verify` vert sur la branche.
- [ ] Manuellement (ou via test): re-déclencher un bootstrap sur staging post-merge → vérifier `entriesCreated=0`, `entriesSkipped=N`, **aucune ligne ERROR** dans les logs CloudWatch sur la fenêtre du job.
- [ ] `docs/operations/hotfix-prod.md` : item `HF-2026-05-27-03` annoté `Fixed by #<PR>` post-merge (le prochain run de `prod-health-check` le déplacera auto en `✅ TERMINÉ`).

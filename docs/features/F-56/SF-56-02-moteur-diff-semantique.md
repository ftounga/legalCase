# Mini-spec — F-56 / SF-56-02 Moteur de diff sémantique avec attribution des raisons

---

## Identifiant

`F-56 / SF-56-02`

## Feature parente

`F-56` — Diff sémantique avec attribution des raisons

## Statut

`ready`

## Date de création

2026-03-27

## Branche Git

`feat/SF-56-02-diff-semantique`

---

## Objectif

Remplacer la comparaison exacte par chaînes dans `AnalysisDiffService` par un appel Haiku qui produit 4 états (`unchanged` / `added` / `removed` / `enriched`) et une `reason` par item non-inchangé, en s'appuyant sur les snapshots de documents (SF-56-01) et les Q&R pour contextualiser les raisons ; les résultats sont mis en cache en base par `(fromId, toId)`.

---

## Comportement attendu

### Cas nominal

1. `GET /api/v1/case-files/{caseFileId}/analyses/diff?from={fromId}&to={toId}` est appelé.
2. `AnalysisDiffService` vérifie si le cache `analysis_diff_cache` contient déjà `(fromId, toId)`.
3. **Cache hit** → désérialise et retourne le résultat directement.
4. **Cache miss** :
   - Charge les snapshots de documents pour `fromId` et `toId` (`analysis_documents`).
   - Calcule le diff de documents : noms ajoutés / retirés entre les deux analyses.
   - Si `to.analysisType == ENRICHED` : charge les Q&R (`AiQuestion` ANSWERED + leurs `AiQuestionAnswer`).
   - Appelle `anthropicService.analyzeFast()` (Haiku) avec :
     - Toutes les sections (`faits`, `points_juridiques`, `risques`, `questions_ouvertes`, `timeline`) de `from` et `to`.
     - Le diff de documents en contexte.
     - Les Q&R en contexte si applicable.
   - Haiku retourne un JSON classifiant chaque item avec `state` et `reason`.
   - Fallback : si l'appel Haiku échoue ou retourne un JSON invalide → on revient à la comparaison exacte (4 états, sans reason).
   - Persiste le résultat dans `analysis_diff_cache`.
5. Retourne `AnalysisDiffResponse` avec les nouveaux champs `enriched` et `reason` sur chaque item.

### Format de réponse

Les items `added`, `removed`, `enriched` deviennent des `DiffItem(text, reason)`.
Les items `unchanged` restent de simples `String`.
La timeline suit le même principe avec `DiffTimelineItem(date, evenement, reason)`.

```
SectionDiff<DiffItem, String> {
  added:    [{ text, reason }]
  removed:  [{ text, reason }]
  unchanged:[String]
  enriched: [{ text, reason }]
}
```

### Sémantique des états

| État | Signification |
|------|--------------|
| `unchanged` | Présent dans `from` et `to`, sémantiquement identique |
| `added` | Présent uniquement dans `to` |
| `removed` | Présent uniquement dans `from` |
| `enriched` | Présent dans les deux, reformulé / enrichi (même concept, contenu différent) |

### Sémantique des raisons

| Contexte | Exemple de reason |
|----------|------------------|
| STANDARD→STANDARD, ajout document | `"Document 'contrat.pdf' ajouté"` |
| STANDARD→STANDARD, retrait document | `"Document 'avenant.pdf' retiré"` |
| STANDARD→ENRICHED, item enrichi | `"Enrichi suite à la réponse : 'Le contrat est à durée déterminée'"` |
| Cause inconnue | `null` |

### Prompt Haiku (structure)

```
Tu analyses la différence entre deux synthèses juridiques.

[Contexte — documents]
Documents ajoutés entre les deux analyses : [liste]
Documents retirés entre les deux analyses : [liste]

[Contexte — Q&R de l'avocat] (uniquement si ENRICHED)
Q1 : ... / R1 : ...

[Sections FROM]
faits FROM : [liste]
...

[Sections TO]
faits TO : [liste]
...

Retourne UNIQUEMENT un JSON valide :
{
  "faits": [{"text":"...","state":"unchanged|added|removed|enriched","reason":null|"..."}],
  "points_juridiques": [...],
  "risques": [...],
  "questions_ouvertes": [...],
  "timeline": [{"date":"...","evenement":"...","state":"...","reason":null|"..."}]
}
Les items removed viennent de FROM, les autres viennent de TO.
```

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| fromId == toId | 400 Bad Request (existant) | 400 |
| Analyse non trouvée / autre workspace | 404 (existant) | 404 |
| Analyse non DONE | 409 (existant) | 409 |
| Haiku timeout ou JSON invalide | Fallback diff exact (pas d'erreur HTTP) | 200 |
| Cache désérialisé corrompu | Supprimer entrée cache + recalculer | 200 |

---

## Critères d'acceptation

- [ ] Cache miss → appel Haiku → résultat persisté dans `analysis_diff_cache`
- [ ] Cache hit → aucun appel Haiku, résultat retourné en < 50ms
- [ ] Items `enriched` présents dans la réponse si Haiku en détecte
- [ ] `reason` non null sur les items `added/removed/enriched` quand le contexte le permet
- [ ] `reason` null sur les items `unchanged`
- [ ] Fallback diff exact si Haiku échoue (pas de 5xx retourné au client)
- [ ] Isolation workspace : un utilisateur d'un autre workspace ne peut pas déclencher le calcul
- [ ] Le diff STANDARD↔ENRICHED mentionne les Q&R dans les reasons
- [ ] Le diff STANDARD↔STANDARD avec changement de documents mentionne les documents dans les reasons

---

## Périmètre

### Hors scope

- Affichage frontend (SF-56-03)
- Invalidation du cache (les analyses historiques sont immuables, le cache est permanent)
- Diff de la timeline avec raison détaillée (best-effort, même logique)
- Appel synchrone de type "recalcul forcé" (pas de bouton rafraîchir pour l'instant)

---

## Technique

### Endpoint(s) impactés

| Méthode | URL | Notes |
|---------|-----|-------|
| GET | `/api/v1/case-files/{id}/analyses/diff` | Existant — réponse enrichie |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `analysis_diff_cache` | INSERT / SELECT | Nouvelle table |
| `analysis_documents` | SELECT | Snapshots SF-56-01 |
| `ai_questions` | SELECT | Q&R si ENRICHED |
| `ai_question_answers` | SELECT | Q&R si ENRICHED |

### Migration Liquibase

- [x] Oui — `031-create-analysis-diff-cache.xml`
  - Table `analysis_diff_cache` : `id UUID PK`, `from_id UUID NOT NULL`, `to_id UUID NOT NULL`, `result_json TEXT NOT NULL`, `created_at TIMESTAMPTZ NOT NULL`
  - Index unique sur `(from_id, to_id)`

### Nouveaux types Java

```java
// dans AnalysisDiffResponse
record DiffItem(String text, String reason) {}
record DiffTimelineItem(String date, String evenement, String reason) {}
record SectionDiff<A, U>(List<A> added, List<A> removed, List<U> unchanged, List<A> enriched) {}
```

### Nouveaux composants

- `AnalysisDiffCacheRepository` — JPA, find by fromId+toId, save
- `SemanticDiffService` — construit le prompt, appelle Haiku, parse la réponse, fallback
- `AnalysisDiffService` — modifié : orchestration cache + SemanticDiffService

### Composants Angular impactés (hors scope SF-56-02, préparation SF-56-03)

- `AnalysisDiffComponent` — devra consommer `enriched` et `reason`

---

## Plan de test

### Tests unitaires

- [ ] `SemanticDiffService` — parsing nominal : JSON Haiku → `SectionDiff` correct
- [ ] `SemanticDiffService` — fallback : JSON invalide → diff exact sans crash
- [ ] `SemanticDiffService` — fallback : exception Haiku → diff exact sans crash
- [ ] `SemanticDiffService` — contexte ENRICHED : Q&R inclus dans le prompt
- [ ] `SemanticDiffService` — contexte STANDARD : diff documents inclus dans le prompt
- [ ] `AnalysisDiffService` — cache hit : Haiku non appelé
- [ ] `AnalysisDiffService` — cache miss : Haiku appelé, cache persisté

### Tests d'intégration

- Pas de nouveau endpoint — les IT existantes du diff restent valides
- Non régression : `GET diff` retourne toujours 200 avec payload valide

### Isolation workspace

- [ ] Applicable — couvert par l'accès `caseFile.workspace.id == membre.workspace.id` (existant)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — la modification est limitée à `AnalysisDiffService` et aux nouvelles classes

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de navigation ni de guard modifié

---

## Dépendances

### Subfeatures bloquantes

- [SF-56-01] — statut : done

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- **Modèle** : `analyzeFast()` (Haiku via `anthropic.model-fast`) — ~$0.006/appel — acceptable
- **Cache permanent** : les analyses historiques sont immuables → pas d'invalidation nécessaire
- **Fallback obligatoire** : Haiku peut être down → le diff doit toujours fonctionner
- **Breaking change API** : `SectionDiff` passe de `SectionDiff<T>(added, removed, unchanged)` à `SectionDiff<A,U>(added, removed, unchanged, enriched)` avec `added/removed/enriched` = `DiffItem`. Le frontend sera mis à jour dans SF-56-03.
- **max_tokens** : 2048 suffisant pour la classification des items

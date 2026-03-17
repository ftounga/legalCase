# Mini-spec — F-08 / SF-08-03 — ChunkAnalysisService (producer + consumer RabbitMQ)

---

## Identifiant

`F-08 / SF-08-03`

## Feature parente

`F-08` — Analyse IA — chunk

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-08-chunk-analysis-service`

---

## Objectif

Déclencher automatiquement l'analyse IA de chaque chunk après le chunking : publier un message RabbitMQ par chunk (producer), consommer ces messages pour appeler Anthropic et persister le résultat dans `chunk_analyses` (consumer).

---

## Comportement attendu

### Cas nominal

Après que `ChunkingService` a persisté N chunks pour une extraction, un `ChunkingDoneEvent` est publié. `ChunkAnalysisService.onChunkingDone()` publie un message `ChunkAnalysisMessage(chunkId)` dans la queue `chunk.analysis` pour chaque chunk. Le consumer `@RabbitListener` consomme chaque message : crée un `ChunkAnalysis` en statut PENDING, passe à PROCESSING, appelle `AnthropicService.analyzeChunk()`, passe à DONE et persiste le résultat JSON.

### Machine d'états

```
PENDING → PROCESSING → DONE
                     → FAILED (en cas d'erreur Anthropic)
```

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Texte du chunk vide | Log warning — chunk ignoré, pas de ChunkAnalysis créé |
| Erreur Anthropic API (timeout, 429, 5xx) | ChunkAnalysis passé à FAILED — log error — pas de retry en V1 |
| ChunkId inconnu | Log error — message ignoré |

---

## Critères d'acceptation

- [ ] `ChunkingDoneEvent` publié par `ChunkingService` après `saveAll()` réussi
- [ ] `ChunkAnalysisService.onChunkingDone()` publie 1 message RabbitMQ par chunk
- [ ] Le consumer `@RabbitListener("chunk.analysis")` consomme les messages
- [ ] `ChunkAnalysis` créé en PENDING puis PROCESSING avant l'appel Anthropic
- [ ] `ChunkAnalysis` passé à DONE avec `analysis_result`, `model_used`, `prompt_tokens`, `completion_tokens` après succès
- [ ] `ChunkAnalysis` passé à FAILED en cas d'erreur Anthropic (log error)
- [ ] `@MockBean AnthropicService` dans les IT tests — pas d'appel API réel en test
- [ ] `@MockBean ChunkAnalysisService` dans `ChunkingServiceIT` — pas de cascade en test

---

## Périmètre

### Hors scope (explicite)

- Retry automatique en cas d'échec (V2+)
- Dead letter queue (V2+)
- Endpoint API pour consulter les analyses
- Analyse niveau document (F-09)

---

## Technique

### Composants créés

- `ChunkingDoneEvent` — record Spring event `(UUID extractionId, List<UUID> chunkIds)`
- `ChunkAnalysisMessage` — record sérialisable RabbitMQ `(UUID chunkId)`
- `ChunkAnalysisService` — producer `@TransactionalEventListener(AFTER_COMMIT)` + consumer `@RabbitListener`

### Publié depuis

`ChunkingService.chunk()` après `chunkRepository.saveAll()` — publie `ChunkingDoneEvent` avec la liste des IDs de chunks persistés.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| chunk_analyses | INSERT + UPDATE | 1 ligne par chunk, mises à jour statut |
| document_chunks | SELECT | Lecture chunk_text pour appel Anthropic |

### Configuration RabbitMQ

- Queue : `chunk.analysis` (déclarée en SF-08-01)
- Sérialisation messages : JSON via `Jackson2JsonMessageConverter`

---

## Plan de test

### Tests unitaires

- [ ] `ChunkAnalysisService` — mock RabbitTemplate + AnthropicService : vérification publication N messages pour N chunks

### Tests d'intégration (ChunkAnalysisServiceIT)

- [ ] `chunk()` valide → N `ChunkAnalysis` en statut DONE persistés en base
- [ ] `chunk()` texte vide → 0 `ChunkAnalysis` créé
- [ ] Erreur Anthropic → `ChunkAnalysis` en statut FAILED

### Isolation workspace

- [ ] Non applicable — le service ne filtre pas par workspace (pipeline interne)

---

## Dépendances

### Subfeatures bloquantes

- F-08 / SF-08-01 — infrastructure RabbitMQ + chunk_analyses — statut : done
- F-08 / SF-08-02 — AnthropicService — statut : done
- F-07 / SF-07-02 — ChunkingService (source de l'événement) — statut : done

---

## Notes et décisions

- `ChunkingDoneEvent` préféré à publier directement depuis le consumer RabbitMQ de chunking — sépare clairement les responsabilités
- `Jackson2JsonMessageConverter` configuré sur le `RabbitListenerContainerFactory` pour désérialiser automatiquement les messages JSON
- Pas de retry en V1 — les chunks FAILED sont visibles en base, récupérables manuellement
- `@MockBean AnthropicService` dans les IT — l'appel Anthropic retourne un JSON fixe via le mock

# Mini-spec — F-07 / SF-07-02 — ChunkingService async

---

## Identifiant

`F-07 / SF-07-02`

## Feature parente

`F-07` — Chunking

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-07-chunks`

---

## Objectif

Segmenter automatiquement le texte extrait en chunks de taille fixe après une extraction DONE, de manière asynchrone, et persister les chunks dans `document_chunks`.

---

## Comportement attendu

### Cas nominal

Lorsque l'extraction d'un document se termine avec le statut DONE, un événement `ExtractionDoneEvent` est publié. `ChunkingService.onExtractionDone()` est appelé de manière asynchrone. Le service découpe le texte extrait en chunks de 1000 caractères avec un overlap de 200 caractères, calcule le token_count approximatif, et persiste les chunks dans `document_chunks`.

### Paramètres de chunking (V1)

| Paramètre | Valeur | Notes |
|-----------|--------|-------|
| Taille du chunk | 1000 caractères | Taille maximale par chunk |
| Overlap | 200 caractères | Chevauchement entre chunks consécutifs |
| Token count | `chunk_text.length() / 4` | Approximation — pas de tokenizer externe |

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Texte extrait null ou vide | Aucun chunk créé — log warning |
| Extraction avec statut != DONE | Événement ignoré |
| Erreur persistance | Log error — chunks partiellement sauvegardés ou aucun |

---

## Critères d'acceptation

- [ ] Le chunking est déclenché automatiquement après une extraction DONE sans bloquer la réponse HTTP
- [ ] Un texte de 2500 caractères produit 3 chunks (0-999, 800-1799, 1600-2499)
- [ ] Chaque chunk a : `chunk_index` correct, `chunk_text` non vide, `token_count` > 0
- [ ] `chunk_metadata` contient `startChar` et `endChar`
- [ ] Un texte vide ou null ne produit aucun chunk (log warning, pas d'erreur)
- [ ] Le trigger est AFTER_COMMIT — l'extraction est garantie en base avant chunking
- [ ] `@MockBean ChunkingService` dans `ExtractionServiceIT` pour éviter le déclenchement en cascade

---

## Périmètre

### Hors scope (explicite)

- Chunking sémantique (découpage par phrase ou paragraphe) — V2+
- Endpoint API pour consulter les chunks
- Vectorisation / embeddings (F-08)
- Retry automatique en cas d'échec

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| document_chunks | INSERT (batch) | N chunks par extraction |
| document_extractions | SELECT | Lecture du texte extrait |

### Composants créés

- `ExtractionDoneEvent` — record Spring event (extractionId, extractedText)
- `ChunkingService` — listener `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`
- Publié depuis `ExtractionService.extract()` après mise à jour statut DONE

---

## Plan de test

### Tests unitaires

- [ ] `ChunkingService.chunk()` — texte de 2500 chars → 3 chunks avec indices et overlaps corrects
- [ ] `ChunkingService.chunk()` — texte vide → liste vide
- [ ] `ChunkingService.chunk()` — texte < 1000 chars → 1 chunk

### Tests d'intégration (ChunkingServiceIT)

- [ ] `chunk()` texte valide → N enregistrements `DocumentChunk` en base, statuts corrects
- [ ] `chunk()` texte vide → 0 enregistrements créés

### Isolation workspace

- [ ] Non applicable — le chunking n'expose pas de données cross-workspace

---

## Dépendances

### Subfeatures bloquantes

- F-07 / SF-07-01 — infrastructure document_chunks — statut : ready
- F-06 / SF-06-02 — ExtractionService (source de l'événement) — statut : done

### Questions ouvertes impactées

- Système de queue asynchrone : `@Async` Spring utilisé, cohérent avec F-06. Question toujours ouverte pour F-08+.

---

## Notes et décisions

- Même pattern que F-06 : `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`
- `chunk()` public et synchrone pour testabilité directe dans les IT tests
- L'overlap garantit la continuité du contexte entre chunks (important pour la qualité LLM en F-08)
- Les deux subfeatures SF-07-01 et SF-07-02 sont dans la même branche/PR car SF-07-01 n'est pas déployable seule (table sans service = inutile)

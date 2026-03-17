# Mini-spec — F-09 / SF-09-02 — DocumentAnalysisService (trigger + consumer RabbitMQ)

---

## Identifiant

`F-09 / SF-09-02`

## Feature parente

`F-09` — Analyse IA — document

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-09-document-analysis-service`

---

## Objectif

Déclencher automatiquement la synthèse IA d'un document quand tous ses chunks ont été analysés : vérifier après chaque chunk analysis DONE, publier un message RabbitMQ si prêt, consommer pour appeler Anthropic avec l'agrégat des analyses de chunks et persister le résultat dans `document_analyses`.

---

## Comportement attendu

### Cas nominal

Après que `ChunkAnalysisService` persiste un `ChunkAnalysis` en statut DONE, il vérifie si tous les chunks de l'extraction ont leur analyse DONE. Si oui, et qu'il n'existe pas déjà une `DocumentAnalysis` en cours pour cette extraction, il publie un `DocumentAnalysisMessage(extractionId)` dans la queue `document.analysis`. Le consumer charge toutes les `chunk_analyses` DONE, construit un prompt agrégé, appelle Anthropic, et persiste le `DocumentAnalysis`.

### Prompt document (V1)

Le prompt envoie à Claude la liste des analyses JSON des chunks sous la forme :
```
Chunk 0 : {"faits": [...], "points_juridiques": [...], ...}
Chunk 1 : {"faits": [...], ...}
...
```
Claude produit une synthèse globale du document au même format JSON.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| DocumentAnalysis déjà existante (PENDING/PROCESSING/DONE) | Message ignoré — pas de double analyse |
| Aucune chunk_analysis DONE pour l'extraction | Log warning — DocumentAnalysis non créée |
| Erreur Anthropic | DocumentAnalysis passée à FAILED — log error |

---

## Critères d'acceptation

- [ ] Après chaque chunk analysis DONE, `ChunkAnalysisService` vérifie si tous les chunks de l'extraction sont DONE
- [ ] Si tous DONE et aucune DocumentAnalysis existante : `DocumentAnalysisMessage(extractionId)` publié dans queue `document.analysis`
- [ ] Queue `document.analysis` et exchange `document.analysis.exchange` déclarés dans `RabbitMQConfig`
- [ ] Consumer crée `DocumentAnalysis` PENDING → PROCESSING → DONE avec le résultat Anthropic
- [ ] Consumer passe à FAILED en cas d'erreur Anthropic
- [ ] Idempotence : si DocumentAnalysis déjà en PENDING/PROCESSING/DONE → message ignoré
- [ ] `@MockBean DocumentAnalysisService` dans `ChunkAnalysisServiceIT` (si applicable)
- [ ] 5 tests unitaires couvrant : trigger (tous DONE → message publié), trigger (pas tous DONE → pas de message), trigger (déjà existante → pas de message), consumer DONE, consumer FAILED

---

## Périmètre

### Hors scope (explicite)

- Synthèse niveau dossier (F-10)
- Retry automatique en cas d'échec
- Endpoint API pour consulter les analyses de document

---

## Technique

### Composants créés / modifiés

- `DocumentAnalysisMessage` — record `(UUID extractionId)`
- `DocumentAnalysisService` — `@Profile("local")` — trigger + consumer
- `RabbitMQConfig` — ajout queue `document.analysis` + exchange + binding
- `ChunkAnalysisService` — ajout vérification post-DONE + publication message
- `ChunkAnalysisRepository` — ajout `countByChunkExtractionIdAndAnalysisStatus()`
- `DocumentChunkRepository` — ajout `countByExtractionId()`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| document_analyses | INSERT + UPDATE | 1 ligne par extraction |
| chunk_analyses | SELECT COUNT | vérification tous DONE |
| document_chunks | SELECT COUNT | nombre total de chunks |

### Configuration RabbitMQ

- Queue : `document.analysis` (durable)
- Exchange : `document.analysis.exchange` (direct)
- Routing key : `document.analysis`

---

## Plan de test

### Tests unitaires

- [ ] Trigger — tous chunks DONE, aucune DocumentAnalysis existante → message publié
- [ ] Trigger — pas tous chunks DONE → aucun message publié
- [ ] Trigger — DocumentAnalysis déjà existante → aucun message publié
- [ ] Consumer — texte valide → DocumentAnalysis DONE persistée
- [ ] Consumer — erreur Anthropic → DocumentAnalysis FAILED

### Isolation workspace

- [ ] Non applicable — pipeline interne

---

## Dépendances

### Subfeatures bloquantes

- F-09 / SF-09-01 — infrastructure document_analyses — statut : done
- F-08 / SF-08-03 — ChunkAnalysisService — statut : done

---

## Notes et décisions

- `countByExtractionId()` dans `DocumentChunkRepository` pour le nombre total de chunks
- `countByChunkExtractionIdAndAnalysisStatus()` dans `ChunkAnalysisRepository` pour le nombre de DONE
- Idempotence via `existsByExtractionIdAndAnalysisStatusIn(PENDING, PROCESSING, DONE)` — évite double déclenchement si plusieurs chunks terminent simultanément
- Même pattern de consumer que F-08 : pas de `@Transactional` global, saves individuels pour visibilité des statuts

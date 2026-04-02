# SF-102-01 — Optimisation performance pipeline IA

## Objectif

Réduire le temps de traitement des analyses de documents et de dossiers en éliminant les lookups DB redondants
par chunk, en augmentant la taille des chunks pour réduire le nombre d'appels Anthropic, et en configurant
explicitement le prefetch RabbitMQ.

---

## Contexte

Le pipeline analyse (chunk → document → dossier) effectue aujourd'hui pour chaque chunk consommé :
- 2× `extractionRepository.findCaseFileIdById()` (lignes 106 + 162)
- 1× `caseFileRepository.findWorkspaceIdById()`
- 1× `caseFileRepository.findLegalDomainById()`
- 1× `caseFileRepository.findCountryById()`
- 1× `caseFileRepository.findCreatedByUserIdById()`

Soit **6 requêtes DB par chunk**. Avec 50 chunks pour un gros document (~150 000 chars), cela représente
300 requêtes DB évitables.

De plus, le `CHUNK_SIZE` de 1000 chars est sous-optimal pour les gros documents (> 600 000 chars) : un document
de 2 Mo génère ~2000 chunks et donc ~2000 appels Anthropic. Passer à 3000 chars réduit ce nombre de 66%.

---

## Comportement nominal

### 1. Chunk size augmenté

- `CHUNK_SIZE` : 1000 → 3000 chars
- `OVERLAP` : 200 → 400 chars
- `tokenCount` calculé : `chunkText.length() / 4` (inchangé)
- Le seuil `directAnalysisThresholdChars` (600 000) est inchangé — les docs courts continuent de bypasser le chunking

### 2. Dénormalisation du ChunkAnalysisMessage

`ChunkAnalysisMessage` reçoit de nouveaux champs **nullable** :
- `caseFileId` (UUID)
- `workspaceId` (UUID)
- `legalDomain` (String)
- `country` (String)
- `userId` (UUID)

`onChunkingDone` est enrichi : il récupère le contexte du case file en **1 requête JPQL** (via `CaseFileRepository.findContextById()`)
et peuple ces champs pour chaque message publié.

`consumeChunkAnalysis` utilise les champs du message s'ils sont non-null, sinon fall back sur les requêtes DB
existantes (compatibilité messages en transit lors d'un rolling deploy).

### 3. CaseFileContext

Nouveau record `fr.ailegalcase.analysis.CaseFileContext(UUID workspaceId, String legalDomain, String country, UUID userId)`
utilisé comme projection JPQL dans `CaseFileRepository.findContextById(UUID id)`.

### 4. Prefetch RabbitMQ explicite

`SimpleRabbitListenerContainerFactory.setPrefetchCount(5)` configuré explicitement dans `RabbitMQConfig`
(valeur alignée sur la concurrence des consumers chunk et document).

---

## Cas d'erreur

- Message sans champs contexte (null) → fall back silencieux sur les requêtes DB (comportement identique à aujourd'hui)
- `findContextById` retourne `Optional.empty()` → les champs restent null → fall back sur DB
- Chunk SKIPPED (budget dépassé) : comportement inchangé — les nouveaux champs ne modifient pas la logique SKIPPED

---

## Critères d'acceptation

- [ ] `ChunkingService.CHUNK_SIZE = 3000`, `OVERLAP = 400`
- [ ] `ChunkAnalysisMessage` a 6 champs (chunkId + 5 contexte nullable)
- [ ] `ChunkAnalysisMessage.forChunk(UUID)` factory statique pour backward compat des tests
- [ ] `onChunkingDone` publie des messages avec champs contexte remplis quand disponibles
- [ ] `consumeChunkAnalysis` utilise `message.caseFileId()` (non-null) → aucune requête `findCaseFileIdById`
- [ ] `consumeChunkAnalysis` avec message null-contexte → fall back DB, même comportement
- [ ] `RabbitMQConfig` a `setPrefetchCount(5)` explicite
- [ ] Tests verts : `ChunkingServiceTest.U-05` mis à jour (tokenCount 750), tests enriched message path ajoutés

---

## Plan de test

### Unitaires

| ID | Cas | Assertion |
|----|-----|-----------|
| U-09 | `consumeChunkAnalysis` avec message enrichi (caseFileId non-null) | `findCaseFileIdById` NOT called, analyse DONE avec bon legalDomain/country |
| U-10 | `onChunkingDone` avec contexte disponible | message publié a les 5 champs non-null |
| U-11 | `onChunkingDone` quand `findContextById` retourne empty | message publié a champs null (pas d'erreur) |
| U-05 (update) | tokenCount du premier chunk d'un texte long | `750` (3000 / 4) |

### Tests existants à mettre à jour

- `ChunkingServiceTest.U-05` : assertion `250` → `750`
- `ChunkAnalysisServiceTest.U-02 à U-08` : `new ChunkAnalysisMessage(chunkId)` → `ChunkAnalysisMessage.forChunk(chunkId)`

### Intégration / régression

- Tous les tests existants doivent passer sans modification de comportement observable

---

## Tables / endpoints / composants impactés

- Aucune nouvelle table, aucune migration Liquibase
- `ChunkAnalysisMessage.java` — enrichi
- `ChunkAnalysisService.java` — onChunkingDone + consumeChunkAnalysis
- `ChunkingService.java` — constantes CHUNK_SIZE / OVERLAP
- `CaseFileRepository.java` — nouvelle méthode `findContextById`
- `CaseFileContext.java` (nouveau record)
- `RabbitMQConfig.java` — prefetchCount
- Tests : `ChunkingServiceTest`, `ChunkAnalysisServiceTest`

---

## Hors périmètre

- Fusion des COUNT queries dans `triggerDocumentAnalysisIfReady` (bénéfice négligeable, les COUNT sont des index lookups O(1))
- Modification du seuil `directAnalysisThresholdChars`
- Frontend
- Toute modification des autres niveaux du pipeline (document analysis, case analysis)

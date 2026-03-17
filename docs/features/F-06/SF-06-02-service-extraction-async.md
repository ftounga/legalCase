# Mini-spec — F-06 / SF-06-02 — Service extraction async

> Statut : done (rétroactif — implémenté le 2026-03-17)

---

## Identifiant

`F-06 / SF-06-02`

## Feature parente

`F-06` — Extraction de texte

## Statut

`done`

## Date de création

2026-03-17 (rétroactif)

## Branche Git

`feat/SF-06-02-service-extraction-async` *(non créée — commit direct sur master, écart de gouvernance corrigé rétroactivement)*

---

## Objectif

Extraire automatiquement le texte d'un document après son upload, de manière asynchrone, et persister le résultat dans `document_extractions`.

---

## Comportement attendu

### Cas nominal

Après la transaction d'upload commitée, un événement `DocumentUploadedEvent` est publié. `ExtractionService.onDocumentUploaded()` est appelé de manière asynchrone (thread séparé). Le service télécharge le fichier depuis MinIO, parse le texte selon le content-type, et persiste une entrée `DocumentExtraction` avec statut DONE et le texte extrait.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Erreur download MinIO | Extraction persistée avec statut FAILED + metadata erreur | N/A (async) |
| Type MIME non supporté | Extraction persistée avec statut FAILED | N/A (async) |
| Erreur parsing | Extraction persistée avec statut FAILED + metadata erreur | N/A (async) |

---

## Critères d'acceptation

- [x] L'extraction est déclenchée automatiquement après upload sans bloquer la réponse HTTP
- [x] Statut DONE avec texte non vide pour un TXT valide
- [x] Statut FAILED si le download échoue
- [x] Statut FAILED si le type n'est pas supporté par le parser
- [x] extraction_metadata contient charCount et durationMs (cas DONE) ou error (cas FAILED)
- [x] Le trigger est AFTER_COMMIT — le document est garantit en base avant extraction
- [x] @MockBean ExtractionService dans DocumentControllerIT — les tests d'upload ne déclenchent pas d'extraction réelle

---

## Périmètre

### Hors scope (explicite)

- Endpoint API pour consulter l'extraction (F-12)
- Chunking du texte extrait (F-07)
- Retry automatique en cas d'échec
- Queue système (F-08+) — @Async Spring suffisant pour F-06

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| document_extractions | INSERT + UPDATE | PENDING → PROCESSING → DONE/FAILED |

### Composants créés

- `ExtractionService` — listener @TransactionalEventListener(AFTER_COMMIT) + @Async
- `DocumentUploadedEvent` — record Spring event
- `StorageService.download(key)` — nouvelle méthode sur l'interface
- `S3StorageService.download()` — implémentation via `getObjectAsBytes()`

### Dépendances Maven ajoutées

- `org.apache.pdfbox:pdfbox:3.0.3` — parsing PDF
- `org.apache.poi:poi-ooxml:5.3.0` — parsing DOCX
- `org.apache.poi:poi-scratchpad:5.3.0` — parsing DOC (legacy)

---

## Plan de test

### Tests d'intégration (ExtractionServiceIT)

- [x] extract() TXT valide → DocumentExtraction DONE avec texte non vide
- [x] extract() → FAILED si StorageService.download() lance une exception
- [x] extract() → FAILED si content-type non supporté (image/png)

### Isolation workspace

- [ ] Non applicable — l'extraction n'expose pas de données cross-workspace

---

## Dépendances

### Subfeatures bloquantes

- F-06 / SF-06-01 — infrastructure document_extractions — statut : done
- F-05 / SF-05-01 — upload document (StorageService disponible) — statut : done

### Questions ouvertes impactées

- Système de queue asynchrone : @Async Spring utilisé pour F-06 uniquement. La question reste ouverte pour F-08+ (analyses LLM). `docs/OPEN_QUESTIONS.md` non modifié — décision partielle pour F-06 seulement.

---

## Notes et décisions

- `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` combinés : supporté depuis Spring 4.2. Le listener s'exécute dans un thread séparé après commit, garantissant que le document est visible en base.
- `extract()` est public et synchrone pour la testabilité directe dans les IT tests (pas besoin de gérer l'async dans les tests)
- `@EnableAsync` ajouté sur `LegalcaseBackendApplication`

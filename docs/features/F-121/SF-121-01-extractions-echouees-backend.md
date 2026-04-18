# Mini-spec — F-121 / SF-121-01 Détection backend des extractions échouées (texte vide, format non supporté, corruption)

## Identifiant
`F-121 / SF-121-01`

## Feature parente
`F-121` — Gestion visible des extractions échouées

## Statut
`draft`

## Date de création
`2026-04-18`

## Branche Git
`feat/SF-121-01-extractions-echouees-backend`

---

## Objectif

Détecter côté backend les 3 cas où une extraction ne produit pas de texte exploitable, les marquer en `ExtractionStatus.FAILED` avec un motif typé, et exposer le statut + motif via l'API `GET /case-files/{id}/documents` pour permettre au frontend (SF-121-02) d'afficher un badge "Non analysable" au lieu d'une barre de progression figée silencieusement.

Bug reproduit en staging 2026-04-17 sur dossier cabinet MEA AVOCATS : 11/12 PDF scannés ont un `extracted_text` vide mais un statut `DONE` → `ChunkingService` abandonne avec un simple `log.warn` → UI bloquée à 1/12 sans message utilisateur.

---

## Comportement attendu

### Cas nominal

**Scénario 1 — PDF natif avec texte** (comportement actuel préservé) :
- `parseText` renvoie un texte non vide → `ExtractionStatus.DONE` → `ExtractionDoneEvent` → chunking normal.

**Scénario 2 — PDF scanné (texte vide)** : **nouveau comportement** :
- `parseText` renvoie `""` ou chaîne blanche → `ExtractionStatus.FAILED` + `failureReason = EMPTY_TEXT` → **pas d'événement** `ExtractionDoneEvent` (la chaîne de chunking/analyse ne se déclenche pas).

**Scénario 3 — Format non supporté** : **nouveau comportement** :
- `parseText` throw `IllegalArgumentException` (contentType inconnu) → `FAILED` + `failureReason = UNSUPPORTED_FORMAT`.

**Scénario 4 — PDF corrompu / erreur PDFBox** : **nouveau comportement** :
- `parseText` throw autre exception → `FAILED` + `failureReason = CORRUPTED`.

**Scénario 5 — Autre erreur (S3, IOException, etc.)** : **nouveau comportement** :
- Exception au niveau `storageService.download` ou autre → `FAILED` + `failureReason = EXTRACTION_EXCEPTION`.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Extraction déjà `DONE` ré-extraite (cas rare) | Le nouveau statut écrase — comportement actuel préservé |
| Motif indéterminable (exception sans message) | `failureReason = EXTRACTION_EXCEPTION` (catégorie fourre-tout) |
| Re-try d'extraction ultérieur (F-122 OCR) | `failure_reason` peut être null après succès OCR — comportement à spécifier dans F-122 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable. L'extraction est dans le pipeline document, pas un outil décisionnel.
- [x] **Autres pays** — Non applicable. Pipeline indépendant du pays.
- [x] **Autres domaines** — Non applicable. Pipeline indépendant du domaine.
- [x] **Autres UI patterns** — Frontend déclenché en SF-121-02 — badge "Non analysable" + tooltip motif.
- [x] **Autres flows transversaux** — Notifications in-app (F-113) + email (EmailService) : cibles de SF-121-02, pas cette SF backend.

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — Ajout de `extractionStatus` + `failureReason` dans `DocumentResponse` DTO. Rétrocompat : champs optionnels.
- [x] **Record / DTO backend** — `DocumentResponse` étendu.
- [x] **Service / logique métier** — `ExtractionService.extract()` modifié. `ChunkingService` inchangé.
- [x] **Entité JPA + schéma DB** — `DocumentExtraction` : nouvelle colonne `failure_reason` (VARCHAR 50, nullable). Migration Liquibase 081.
- [x] **Tests existants** — `ExtractionServiceTest` existant à étendre. `DocumentControllerIT` existant à étendre.

### Cas spécifique : nouveau pattern UI ou service partagé

Cette SF introduit un **nouvel enum** `ExtractionFailureReason` utilisé partout où `ExtractionStatus` est référencé (DTOs, frontend, etc.).

- [x] **Où le pattern pourrait-il être réutilisé ?** F-122 OCR réutilisera l'enum (ex. `OCR_QUOTA_EXCEEDED` = nouveau motif) — extension naturelle
- [x] **Patterns concurrents ?** Aucun — actuellement seul `extractionMetadata` (JSON libre) contenait l'info de manière non-typée
- [x] **Le service peut-il servir à d'autres features ?** Non — enum spécifique au pipeline document
- [x] **Équivalent design existant ?** Non

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| Pipeline extraction (`ExtractionService`) | Oui | Intégré dans cette SF |
| Pipeline chunking (`ChunkingService`) | Partiel — n'est plus appelé sur FAILED | Aucune modif nécessaire (le `return` silencieux ligne 64 devient inopérant car on ne publie plus l'événement) |
| DTO API `DocumentResponse` | Oui | Étendu dans cette SF |
| Frontend list documents | Oui (SF suivante) | SF-121-02 consomme les nouveaux champs |
| Notifications in-app / email | Oui (SF suivante) | SF-121-02 |
| F-122 OCR (enum étendu) | Oui (V6 plus tard) | Backlog F-122, hors scope |

### Décision

- [x] Étendu aux cibles applicables cette SF (backend + DTO)
- [x] Subfeature parallèle prévue : **SF-121-02** (frontend + notifications)
- [x] Backlog F-122 étendra l'enum avec OCR_QUOTA_EXCEEDED

---

## Critères d'acceptation

- [ ] Enum `ExtractionFailureReason` créé avec 4 valeurs : `EMPTY_TEXT`, `UNSUPPORTED_FORMAT`, `CORRUPTED`, `EXTRACTION_EXCEPTION`
- [ ] Migration Liquibase `081-add-failure-reason-to-extractions.xml` ajoute colonne `failure_reason VARCHAR(50) NULL` sur `document_extractions`
- [ ] `DocumentExtraction` entité : champ `failureReason: ExtractionFailureReason` (nullable)
- [ ] `ExtractionService.extract()` :
  - [ ] Après `parseText(...)`, si `text == null || text.isBlank()` → `FAILED` + `failureReason = EMPTY_TEXT`
  - [ ] Si `IllegalArgumentException` (contentType inconnu) → `FAILED` + `failureReason = UNSUPPORTED_FORMAT`
  - [ ] Si `org.apache.pdfbox.io.IOException` ou autre exception de parsing → `FAILED` + `failureReason = CORRUPTED`
  - [ ] Autre exception (S3, IOException générique) → `FAILED` + `failureReason = EXTRACTION_EXCEPTION`
  - [ ] En cas de `FAILED`, **ne plus publier** `ExtractionDoneEvent` (comportement actuel préservé — garantit que `ChunkingService` n'est plus appelé)
- [ ] `DocumentResponse` DTO étend avec 2 nouveaux champs optionnels : `extractionStatus: String` et `failureReason: String?`
- [ ] `GET /api/v1/case-files/{id}/documents` renvoie ces champs — ils sont `null` pour les documents sans extraction ou dont l'extraction est `DONE`
- [ ] Rétrocompat : les extractions existantes avec `failure_reason=NULL` restent valides
- [ ] Tests unitaires `ExtractionServiceTest` : ≥ 4 nouveaux cas couvrant les 4 motifs de `ExtractionFailureReason`
- [ ] Tests d'intégration `DocumentControllerIT` : `GET /documents` inclut correctement `extractionStatus` et `failureReason`
- [ ] Suite backend complète passe — aucune régression

---

## Périmètre

### Hors scope (explicite)

- Frontend (badge, tooltip, liste documents) — **SF-121-02**
- Notifications in-app + email — **SF-121-02**
- Re-extraction / retry OCR — **F-122** (séparée)
- Modification de la progression globale du dossier (barre d'analyse) — SF-121-02 ou ultérieure, selon retour terrain

---

## Valeurs initiales

### Migration 081

Colonne ajoutée sur `document_extractions` :
- Nom : `failure_reason`
- Type : `VARCHAR(50)`
- Nullable : **oui** (toutes les extractions existantes et futures DONE restent `NULL`)
- Index : non (peu de requêtes par motif attendues)
- Défaut : `NULL`

### Enum ExtractionFailureReason

| Valeur | Quand | Exemple |
|---|---|---|
| `EMPTY_TEXT` | Parsing réussit mais texte vide après trim | PDF scanné sans OCR |
| `UNSUPPORTED_FORMAT` | contentType hors de la liste supportée | fichier `.odt`, `.pages`, etc. |
| `CORRUPTED` | PDFBox / Tika throw pendant le parsing | PDF endommagé, DOCX corrompu |
| `EXTRACTION_EXCEPTION` | Autre exception (S3 download, IOException, etc.) | catégorie fourre-tout |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs | Normalisation |
|---|---|---|---|---|
| `failure_reason` (DB) | Non | 50 | Chaîne = nom d'enum `ExtractionFailureReason` ou `NULL` | uppercase |
| `extractionStatus` (DTO) | Si extraction existe | — | `PENDING` / `PROCESSING` / `DONE` / `FAILED` | — |
| `failureReason` (DTO) | Uniquement si `extractionStatus = FAILED` | — | Valeur enum ou null | — |

---

## Technique

### Endpoint(s)

Pas de nouvel endpoint. `GET /api/v1/case-files/{id}/documents` (existant) renvoie désormais un payload enrichi de 2 champs.

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `document_extractions` | ALTER TABLE — ADD COLUMN `failure_reason` | Nullable, pas de valeur par défaut |

### Migration Liquibase

- [x] Oui — `081-add-failure-reason-to-extractions.xml`

### Composants / classes backend

| Classe | Modification |
|---|---|
| `ExtractionFailureReason.java` (NEW) | Enum 4 valeurs |
| `DocumentExtraction.java` | Ajout champ `failureReason: ExtractionFailureReason` (@Enumerated STRING, nullable) |
| `ExtractionService.java` | Refonte de `extract()` : détection texte vide + switch sur type d'exception pour choisir le motif |
| `DocumentResponse.java` (ou le record API) | Ajout de 2 champs optionnels |
| `DocumentController.java` ou query service | Peuple les 2 nouveaux champs dans la réponse |

### Composants frontend

Aucun dans cette SF — SF-121-02.

---

## Plan de test

### Tests unitaires

- [ ] `ExtractionServiceTest` : `extract_emptyText_marksFailedWithEmptyTextReason`
- [ ] `ExtractionServiceTest` : `extract_whitespaceOnly_marksFailedWithEmptyTextReason` (ex. PDF scanné avec que des espaces)
- [ ] `ExtractionServiceTest` : `extract_unsupportedContentType_marksFailedWithUnsupportedFormatReason`
- [ ] `ExtractionServiceTest` : `extract_corruptedPdf_marksFailedWithCorruptedReason`
- [ ] `ExtractionServiceTest` : `extract_storageException_marksFailedWithExtractionExceptionReason`
- [ ] `ExtractionServiceTest` : `extract_empty_doesNotPublishExtractionDoneEvent` (vérifie que le chunking ne sera pas appelé)
- [ ] `ExtractionServiceTest` : `extract_nominal_stillWorks` (non-régression — PDF normal DONE + event publié)

### Tests d'intégration

- [ ] `DocumentControllerIT` : `GET /documents` retourne `extractionStatus` et `failureReason` pour un document dont l'extraction est FAILED avec EMPTY_TEXT
- [ ] `DocumentControllerIT` : `GET /documents` renvoie `failureReason: null` pour un document DONE

### Isolation workspace

- [x] **Non applicable** — `GET /documents` est déjà isolé par workspace via `CaseFile.workspace`. L'ajout de 2 champs ne change rien à l'isolation.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing frontend — non applicable (pas de FE cette SF)
- [x] **Aucune préoccupation transversale majeure** — modifications isolées au module `document`

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `ChunkingService.chunk()` L62-65 `empty text skip` | Devient du code mort si tous les textes vides sont catchés avant — garde le warning log comme sécurité ceinture-bretelle | Tests existants préservés |
| `GET /case-files/{id}/documents` | Payload étendu de 2 champs. Consommateurs existants (frontend liste documents, PDF export) lisent `response.originalFilename`, pas les nouveaux champs | Tests existants `DocumentControllerIT` |
| `ExtractionDoneEvent` publication | Plus publié si FAILED — comportement attendu | Test dédié vérifie le non-publish |

### Smoke tests E2E concernés

- [x] **Aucun smoke test concerné** — la chaîne d'upload/extraction n'est pas testée en E2E actuellement. Pas de risque de régression côté E2E.

---

## Dépendances

### Subfeatures bloquantes

- Aucune — première SF de F-121.

### Subfeatures successeurs

- **SF-121-02** (frontend + notifications) — consommera `extractionStatus` + `failureReason` dans le DTO

### Questions ouvertes impactées

- [x] Aucune question bloquante de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

### Pourquoi EMPTY_TEXT et pas juste FAILED

Sans motif typé, le frontend ne peut pas proposer d'actions adaptées (ex. *"Utiliser l'OCR"* pour `EMPTY_TEXT` vs *"Reconvertir le fichier"* pour `CORRUPTED`). Typer les motifs prépare aussi F-122 qui ajoutera `OCR_QUOTA_EXCEEDED`.

### Pourquoi ne plus publier `ExtractionDoneEvent` en cas de FAILED

Le `ChunkingService.chunk()` avait un filet de sécurité (`if blank → skip`) qui reste en place pour robustesse (ceinture-bretelle) mais n'est plus censé se déclencher. C'est plus propre et plus sûr : le pipeline ne gaspille pas de ressources sur une extraction qu'on sait vide.

### Pourquoi stocker le motif en colonne typée plutôt que dans `extraction_metadata` JSON

- Requêtable : permet des dashboards du type "X documents EMPTY_TEXT ce mois" (hors scope mais possible)
- Plus simple pour le frontend : pas besoin de parser un JSON libre
- Index possible si besoin de requêtes fréquentes (pas dans cette SF)

### Pourquoi pas de migration rétroactive des extractions existantes

Les anciennes extractions DONE avec texte vide restent DONE (pas touchées). Si l'avocate MEA AVOCATS re-uploade son dossier après le déploiement, les nouvelles extractions suivront le bon comportement. Une migration rétroactive serait possible (UPDATE WHERE extracted_text IS NULL OR extracted_text = '' THEN status=FAILED, reason=EMPTY_TEXT) mais risquée (casse potentielle de dossiers fonctionnels si données sales). Décision : ne pas toucher l'existant.

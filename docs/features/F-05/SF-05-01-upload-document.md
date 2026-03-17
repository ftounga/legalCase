# Mini-spec — F-05 / SF-05-01 — Upload de document

> Statut : done (rétroactif — implémenté le 2026-03-17)

---

## Identifiant

`F-05 / SF-05-01`

## Feature parente

`F-05` — Upload de documents

## Statut

`done`

## Date de création

2026-03-17 (rétroactif)

## Branche Git

`feat/SF-05-01-upload-document` *(non créée — commit direct sur master, écart de gouvernance corrigé rétroactivement)*

---

## Objectif

Permettre à un avocat authentifié d'uploader un fichier dans un dossier, avec validation du type et de la taille, stockage sur objet S3-compatible (MinIO) et persistance en base.

---

## Comportement attendu

### Cas nominal

L'avocat envoie une requête `POST /api/v1/case-files/{id}/documents` avec un fichier multipart.
Le backend valide le fichier, l'uploade dans MinIO sous une clé structurée, persiste le document en base et retourne 201 avec le DTO du document créé.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Fichier absent ou vide | Message d'erreur explicite | 400 |
| Type MIME non supporté (ex : image/png) | Message d'erreur avec types autorisés | 400 |
| Fichier > 50 Mo | Message d'erreur taille | 400 |
| Utilisateur non authentifié | Refus | 401 |
| Case file inconnu | Not found | 404 |
| Case file appartenant à un autre workspace | Not found (pas d'information sur l'existence) | 404 |
| Erreur upload MinIO | Erreur serveur | 500 |

---

## Critères d'acceptation

- [x] POST avec PDF valide → 201 avec id, originalFilename, contentType, caseFileId, fileSize
- [x] POST avec DOCX valide → 201
- [x] POST avec type non supporté → 400
- [x] POST avec fichier vide → 400
- [x] POST sans auth → 401
- [x] POST vers case file inconnu → 404
- [x] POST vers case file d'un autre workspace → 404 (isolation)
- [x] Le fichier est physiquement présent dans MinIO avec la bonne storage_key
- [x] La storage_key suit le format {workspaceId}/{caseFileId}/{UUID}/{sanitized_filename}

---

## Périmètre

### Hors scope (explicite)

- Extraction de texte (F-06)
- Suppression de document
- Versionnement de document
- Limite de quota par workspace (F-16)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| created_at | now() | Automatique @PrePersist |
| uploaded_by_user_id | Utilisateur connecté | Résolution via AuthAccount |

Comportements à la création :
- storage_key généré côté backend : `{workspaceId}/{caseFileId}/{UUID}/{sanitized_filename}`
- Le filename est sanitizé (caractères spéciaux remplacés par `_`)

---

## Contraintes de validation

| Champ | Obligatoire | Taille max | Format / Valeurs autorisées | Notes |
|-------|-------------|------------|----------------------------|-------|
| file | Oui | 50 Mo | PDF, DOC, DOCX, TXT | Contrôle sur content-type |
| originalFilename | Oui | — | String non vide | Transmis par le client |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{id}/documents` | Oui | Upload multipart |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| documents | INSERT | Nouvelle ligne par upload |
| (MinIO bucket) | PUT object | Stockage physique |

### Migration Liquibase

- [x] Oui — `007-create-documents.xml`

---

## Plan de test

### Tests d'intégration

- [x] POST PDF valide → 201 (`DocumentControllerIT`)
- [x] POST DOCX valide → 201
- [x] POST type non supporté → 400
- [x] POST fichier vide → 400
- [x] POST sans auth → 401
- [x] POST case file inconnu → 404
- [x] POST case file autre workspace → 404 (isolation)

### Isolation workspace

- [x] Testée — un utilisateur ne peut pas uploader dans le case file d'un autre workspace

---

## Dépendances

### Subfeatures bloquantes

- F-03 / SF-03-01 — création dossier — statut : done
- F-04 / SF-04-02 — consultation dossier — statut : done

### Questions ouvertes impactées

- [x] Provider stockage objet → MinIO choisi pour dev (2026-03-17), prod TBD — `docs/OPEN_QUESTIONS.md` mis à jour

---

## Notes et décisions

- `StorageService` est une interface abstraite — le code est identique quelle que soit la solution de stockage (MinIO, AWS S3, Scaleway)
- `@MockBean StorageService` dans les IT tests : aucune dépendance à MinIO en CI
- `@PostConstruct initBucket()` est résilient : si MinIO est indisponible au démarrage, le backend démarre quand même avec un warning

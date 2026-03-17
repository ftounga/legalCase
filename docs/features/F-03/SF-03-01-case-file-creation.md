# Mini-spec — F-03 / SF-03-01 Création de dossier juridique

---

## Identifiant

`F-03 / SF-03-01`

## Feature parente

`F-03` — Création de dossier

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-03-01-case-file-creation`

---

## Objectif

Permettre à un utilisateur authentifié de créer un dossier juridique dans son workspace via `POST /api/v1/case-files`.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur envoie `POST /api/v1/case-files` avec `title`, `legalDomain`, `description` (optionnel).
2. Le serveur valide les champs.
3. Un `CaseFile` est créé avec `workspace_id` du workspace de l'utilisateur et `created_by_user_id`.
4. Retourne 201 avec le dossier créé.

Réponse attendue :
```json
{
  "id": "uuid",
  "title": "Licenciement M. Dupont",
  "legalDomain": "EMPLOYMENT_LAW",
  "description": "Contestation licenciement abusif",
  "status": "OPEN",
  "createdAt": "2026-03-17T10:00:00Z"
}
```

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Non authentifié | 401 JSON | 401 |
| `title` absent ou vide | `{"error":"Bad Request","message":"title is required"}` | 400 |
| `legalDomain` absent | `{"error":"Bad Request","message":"legalDomain is required"}` | 400 |
| `legalDomain` != `EMPLOYMENT_LAW` | `{"error":"Bad Request","message":"Only EMPLOYMENT_LAW is supported in V1"}` | 400 |
| `description` > 2000 caractères | `{"error":"Bad Request","message":"description must not exceed 2000 characters"}` | 400 |
| Utilisateur sans workspace | `{"error":"Not Found","message":"Workspace not found"}` | 404 |

---

## Critères d'acceptation

- [ ] `POST /api/v1/case-files` avec payload valide → 201 avec `id`, `title`, `legalDomain`, `status`, `createdAt`
- [ ] Le `workspace_id` du dossier créé correspond au workspace de l'utilisateur connecté
- [ ] `title` vide ou absent → 400
- [ ] `legalDomain` absent → 400
- [ ] `legalDomain` != `EMPLOYMENT_LAW` → 400
- [ ] `description` > 2000 caractères → 400
- [ ] Sans session → 401

---

## Périmètre

### Hors scope (explicite)

- Modification d'un dossier
- Liste des dossiers (F-04)
- Upload de documents (F-05)
- Changement de statut

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `status` | `OPEN` | Toujours à la création |
| `workspace_id` | workspace de l'utilisateur connecté | Résolu via WorkspaceService |
| `created_by_user_id` | user de l'utilisateur connecté | Résolu via AuthAccount |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Valeurs autorisées | Unicité |
|-------|-------------|-------------|-------------------|---------|
| `title` | Oui | 255 | non vide après trim | Non |
| `legalDomain` | Oui | — | `EMPLOYMENT_LAW` uniquement en V1 | Non |
| `description` | Non | 2000 | texte libre | Non |

---

## Technique

### Package

`fr.ailegalcase.casefile`

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files` | Oui | utilisateur authentifié |

### Composants

- `CaseFile` — entité JPA
- `CaseFileRepository` — `JpaRepository<CaseFile, UUID>`
- `CaseFileService` — logique de création avec validation
- `CaseFileController` — endpoint POST
- `CaseFileRequest` — DTO de requête
- `CaseFileResponse` — DTO de réponse

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_files` | INSERT | Migration 006 |
| `workspace_members` | SELECT | résolution workspace |
| `auth_accounts` | SELECT | résolution user |

### Migration Liquibase

- [ ] `006-create-case-files.xml`

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileService` — création valide → CaseFile retourné
- [ ] `CaseFileService` — title vide → exception 400
- [ ] `CaseFileService` — legalDomain invalide → exception 400
- [ ] `CaseFileService` — description trop longue → exception 400

### Tests d'intégration

- [ ] `POST /api/v1/case-files` → 201 avec payload valide
- [ ] `POST /api/v1/case-files` → 400 avec title absent
- [ ] `POST /api/v1/case-files` → 400 avec legalDomain invalide
- [ ] `POST /api/v1/case-files` → 401 sans auth

### Isolation workspace

- [ ] Le `workspace_id` du dossier créé = workspace de l'utilisateur connecté (testé en IT)

---

## Dépendances

### Subfeatures bloquantes

- SF-02-01 — statut : done (workspace de l'utilisateur)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- La validation métier est dans `CaseFileService`, pas dans le controller.
- `@Valid` + `@NotBlank` utilisés sur le DTO pour les validations simples (titre, legalDomain présents).
- La contrainte `EMPLOYMENT_LAW` uniquement est vérifiée dans le service (règle métier, pas de contrainte Bean Validation).
- `workspace_id` résolu via `WorkspaceService.getCurrentWorkspace()` — réutilise le même lookup que SF-02-02.

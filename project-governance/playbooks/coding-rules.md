# Coding Rules — AI LegalCase

## Principes généraux

- Lisibilité > concision
- Explicite > implicite
- Simple > générique
- Ne pas anticiper des besoins non encore spécifiés
- Aucune abstraction prématurée

---

## Backend — Spring Boot

### Structure des packages

```
fr.ailegalcase.
  ├── workspace/          → gestion workspaces et membres
  ├── casefile/           → dossiers juridiques
  ├── document/           → upload, extraction, chunking
  ├── analysis/           → pipeline IA (chunk, document, dossier)
  ├── auth/               → sécurité, OAuth2, identité
  ├── billing/            → abonnements, usage
  └── shared/             → utilitaires partagés (pas de logique métier)
```

### Nommage

| Élément | Convention | Exemple |
|---------|-----------|---------|
| Package | `snake_case` minuscule | `fr.ailegalcase.casefile` |
| Classe | `PascalCase` | `CaseFileService` |
| Méthode | `camelCase` | `findAllByWorkspace` |
| Constante | `UPPER_SNAKE_CASE` | `MAX_CHUNK_SIZE` |
| Table SQL | `snake_case` pluriel | `case_files` |
| Colonne SQL | `snake_case` | `workspace_id` |

### Layering obligatoire

```
Controller → Service → Repository
```

- Le controller ne contient aucune logique métier
- Le service contient toute la logique métier
- Le repository ne contient que des requêtes de données
- Pas de logique métier dans les entités JPA

### Multi-tenant — Règle absolue

Toute requête accédant à des données doit filtrer par `workspace_id`.
Un utilisateur ne peut accéder qu'aux données de ses workspaces.
Ce filtre est appliqué au niveau service, pas uniquement au niveau SQL.

```java
// Correct
caseFileRepository.findByIdAndWorkspaceId(id, workspaceId);

// Interdit
caseFileRepository.findById(id); // sans vérification workspace
```

### Endpoints REST

- Nommage pluriel et en kebab-case : `/api/case-files`, `/api/workspaces`
- Versioning : `/api/v1/...`
- Réponses cohérentes : 200, 201, 400, 403, 404, 409, 500
- Pas de logique dans les DTOs
- Séparer DTO de requête et DTO de réponse

### Gestion des erreurs

- Utiliser un `@ControllerAdvice` global
- Ne jamais exposer de stacktrace en réponse
- Messages d'erreur en anglais côté API, traduits côté frontend
- Toujours logger l'erreur technique, renvoyer un message générique au client

### Jobs asynchrones

- Tout traitement IA est asynchrone
- Toujours créer un enregistrement `analysis_jobs` avant de lancer le job
- Mettre à jour le statut du job à chaque étape (PENDING → RUNNING → DONE / FAILED)
- Gérer les cas d'échec avec `error_message` et retry si applicable

---

## Frontend — Angular

### Structure des modules

```
src/app/
  ├── core/               → services globaux, guards, interceptors
  ├── shared/             → composants, pipes, directives réutilisables
  ├── features/
  │   ├── auth/
  │   ├── workspace/
  │   ├── case-files/
  │   ├── documents/
  │   └── analysis/
  └── layout/             → shell, navigation, header
```

### Nommage

| Élément | Convention | Exemple |
|---------|-----------|---------|
| Composant | `kebab-case` | `case-file-detail` |
| Classe | `PascalCase` | `CaseFileDetailComponent` |
| Service | `PascalCase` + `Service` | `CaseFileService` |
| Observable | `camelCase` + `$` | `caseFiles$` |
| Interface | `PascalCase` | `CaseFile` |

### Règles Angular

- Un composant = une responsabilité
- Les composants ne font pas d'appels HTTP directs → passer par un service
- Les services ne manipulent pas le DOM
- Utiliser `AsyncPipe` pour les observables dans les templates
- Pas de logique dans les templates au-delà des conditions simples

### Gestion des erreurs HTTP

- Interceptor global pour les 401 (redirect login) et 403 (message utilisateur)
- Afficher un message explicite à l'utilisateur, ne jamais afficher de détail technique

---

## Base de données — PostgreSQL

### Migrations

- Toutes les migrations via Liquibase
- Nommage : `{NNN}-{description}.xml` (ex: `001-init-schema.xml`, `002-add-casefile.xml`)
- Emplacement : `src/main/resources/db/changelog/migrations/`
- Inclus via `db.changelog-master.xml` avec `<includeAll>`
- Une migration = un changement cohérent
- Jamais de migration destructive sans migration de sauvegarde préalable
- Pas de modification d'une migration déjà appliquée en production

### Contraintes obligatoires

- Toutes les FK ont une contrainte `REFERENCES` explicite
- Toutes les tables ont `created_at TIMESTAMP NOT NULL DEFAULT NOW()`
- Les colonnes `workspace_id` ont toujours une FK vers `workspaces(id)`
- Index obligatoires sur toutes les colonnes `workspace_id`

### Conventions SQL

```sql
-- Correct
SELECT cf.id, cf.title
FROM case_files cf
WHERE cf.workspace_id = :workspaceId
  AND cf.status = 'ACTIVE';

-- Interdit
SELECT * FROM case_files WHERE id = 1;
```

---

## Git

### Nommage des branches

| Type | Format | Exemple |
|------|--------|---------|
| Feature / subfeature | `feat/SF-XX-nom-court` | `feat/SF-01-create-case-file` |
| Bugfix | `fix/description-courte` | `fix/workspace-isolation-missing` |
| Refactor | `refactor/description` | `refactor/analysis-service` |

### Commits

- Format : `type(scope): description courte`
- Types : `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
- Exemples :
  - `feat(casefile): add case file creation endpoint`
  - `test(auth): add workspace member isolation tests`
  - `fix(analysis): handle job failure status update`
- Un commit = une modification cohérente
- Pas de commits "WIP" ou "fix fix fix"

---

## Ce qui est interdit

- Logique métier dans les controllers ou les entités
- Accès aux données sans filtre `workspace_id`
- Commit direct sur `main`
- Migration SQL modifiant une colonne existante sans étape de compatibilité
- Stacktrace exposée dans une réponse API
- Lancer un traitement IA de façon synchrone

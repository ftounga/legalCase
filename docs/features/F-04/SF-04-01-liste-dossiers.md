# Mini-spec — F-04 / SF-04-01 — Liste paginée des dossiers

> Statut : done (rétroactif — implémenté le 2026-03-17, PR #10)

---

## Identifiant

`F-04 / SF-04-01`

## Feature parente

`F-04` — Liste & consultation des dossiers

## Statut

`done`

## Date de création

2026-03-17 (rétroactif)

## Branche Git

`feature/SF-04-01-list-case-files` *(PR #10 mergée sur master)*

---

## Objectif

Permettre à un avocat authentifié de consulter la liste paginée de ses dossiers juridiques via `GET /api/v1/case-files`, isolée par workspace.

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files` retourne une `Page<CaseFileResponse>` (format Spring Data) contenant les dossiers du workspace de l'utilisateur connecté, triés par défaut, 20 par page.

Réponse attendue :
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Licenciement M. Dupont",
      "legalDomain": "EMPLOYMENT_LAW",
      "description": "...",
      "status": "OPEN",
      "createdAt": "2026-03-17T10:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non authentifié | Refus | 401 |
| Workspace vide (aucun dossier) | Page vide — `content: []`, `totalElements: 0` | 200 |
| Utilisateur sans workspace | Not found | 404 |

---

## Critères d'acceptation

- [x] GET liste → 200 avec `content` tableau et `totalElements`
- [x] GET liste workspace vide → 200, `content: []`, `totalElements: 0`
- [x] GET sans auth → 401
- [x] Les dossiers retournés appartiennent uniquement au workspace de l'utilisateur connecté

---

## Périmètre

### Hors scope (explicite)

- Filtrage par statut ou domaine juridique
- Tri personnalisé
- Consultation détaillée d'un dossier (SF-04-02)
- Frontend (implémenté dans PR #12 — `feat/frontend-f01-f02-f03-f04`)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files` | Oui | Liste paginée, `@PageableDefault(size=20)` |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_files | SELECT | Filtré par workspace_id — isolation |
| workspace_members | SELECT | Résolution workspace de l'utilisateur |
| auth_accounts | SELECT | Résolution user depuis OidcUser |

### Migration Liquibase

- [x] Non applicable — table `case_files` déjà créée en SF-03-01 (006-create-case-files.xml)

---

## Plan de test

### Tests d'intégration

- [x] GET liste vide → 200, `content: []`, `totalElements: 0` (`CaseFileControllerIT`)
- [x] GET liste avec dossiers → 200, items présents, statut OPEN
- [x] GET sans auth → 401

### Isolation workspace

- [x] Implicite — `findByWorkspace(workspace, pageable)` filtre par workspace résolu depuis l'utilisateur connecté
- [ ] ⚠️ Non testée explicitement avec deux workspaces distincts — dette technique à corriger

---

## Dépendances

### Subfeatures bloquantes

- F-03 / SF-03-01 — création dossier — statut : done

---

## Notes et décisions

- `Pageable` injecté via `@PageableDefault(size=20)` — taille par défaut 20, surchargeable via `?size=N&page=P`
- Isolation assurée par résolution workspace depuis l'AuthAccount de l'OidcUser — pas de filtre SQL explicite `workspace_id` en paramètre URL
- Le frontend (CaseFileListComponent avec dashboard et mat-table) est couvert par PR #12 séparée

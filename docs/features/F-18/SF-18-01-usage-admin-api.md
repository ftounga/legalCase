# Mini-spec — SF-18-01 API REST résumé consommation LLM workspace

## Identifiant
`SF-18-01`

## Feature parente
`F-18` — Page d'administration

## Statut
`ready`

## Date de création
2026-03-19

## Branche Git
`feat/SF-18-01-usage-admin-api`

---

## Objectif
Permettre à un OWNER ou ADMIN de récupérer la consommation LLM agrégée de son workspace — totaux globaux, détail par dossier et détail par utilisateur — via un endpoint dédié.

---

## Comportement attendu

### Cas nominal
`GET /api/v1/admin/usage`

1. Le service résout l'utilisateur connecté via `AuthAccountRepository`
2. Il récupère son workspace primaire via `WorkspaceMemberRepository.findByUserAndPrimaryTrue`
3. Il vérifie que le `memberRole` est `OWNER` ou `ADMIN` → sinon 403
4. Il charge tous les `usage_events` liés aux `case_files` du workspace (via `case_file_id`)
5. Il agrège et retourne :
   - `totalTokensInput`, `totalTokensOutput`, `totalCost` (totaux workspace)
   - `byUser` : liste `[userId, userEmail, totalTokensInput, totalTokensOutput, totalCost]`
   - `byCaseFile` : liste `[caseFileId, caseFileTitle, totalTokensInput, totalTokensOutput, totalCost]`

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Token absent | Non autorisé | 401 |
| Rôle LAWYER ou MEMBER | Accès refusé | 403 |
| Workspace primaire introuvable | Not found | 404 |
| Aucun usage event | Réponse valide avec totaux à 0 et listes vides | 200 |

---

## Critères d'acceptation

- [ ] Quand un OWNER appelle `GET /api/v1/admin/usage`, il reçoit 200 avec les totaux du workspace
- [ ] Quand un ADMIN appelle l'endpoint, il reçoit 200 (même droit qu'OWNER)
- [ ] Quand un LAWYER ou MEMBER appelle l'endpoint, il reçoit 403
- [ ] Les données `byUser` ne contiennent que les utilisateurs ayant des events dans ce workspace
- [ ] Les données `byCaseFile` ne contiennent que les dossiers de ce workspace
- [ ] Un OWNER d'un autre workspace ne voit pas les données de ce workspace (isolation)
- [ ] Si aucun event n'existe, les totaux sont 0 et les listes sont vides (pas de 404)

---

## Périmètre hors-scope

- Filtrage par période (pas de paramètre `from`/`to` en V1)
- Pagination
- Export CSV
- Détail event par event (déjà couvert par `GET /api/v1/case-files/{id}/usage`)

---

## Éléments techniques

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/admin/usage` | Oui | ADMIN |

### Nouveaux records / classes

- `WorkspaceUsageSummaryResponse` — record racine avec totaux + listes
- `UserUsageSummary` — record `[userId, userEmail, tokensInput, tokensOutput, cost]`
- `CaseFileUsageSummary` — record `[caseFileId, caseFileTitle, tokensInput, tokensOutput, cost]`

### Nouveau service

- `AdminUsageService` — méthode `getWorkspaceSummary(OidcUser, provider)`

### Repository — nouvelles queries

`UsageEventRepository` :
- `findByCaseFileIdIn(Collection<UUID> caseFileIds)` — récupère tous les events du workspace

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `usage_events` | SELECT | Filtré sur les `case_file_id` du workspace |
| `case_files` | SELECT | Pour récupérer les titres et filtrer par workspace |
| `workspace_members` | SELECT | Pour vérifier le rôle OWNER/ADMIN |

### Migration Liquibase
- [ ] Non applicable — aucune nouvelle table

### Nouveau controller

- `AdminUsageController` — `@RequestMapping("/api/v1/admin/usage")`

---

## Plan de test

### Tests unitaires

- [ ] `AdminUsageService` — OWNER : retourne summary avec totaux corrects
- [ ] `AdminUsageService` — ADMIN : retourne summary (même comportement)
- [ ] `AdminUsageService` — LAWYER : lève 403
- [ ] `AdminUsageService` — MEMBER : lève 403
- [ ] `AdminUsageService` — workspace sans events : totaux à 0, listes vides
- [ ] `AdminUsageService` — agrégation `byUser` correcte (2 users distincts)
- [ ] `AdminUsageService` — agrégation `byCaseFile` correcte (2 dossiers distincts)

### Tests d'intégration

- [ ] `GET /api/v1/admin/usage` → 200 avec payload valide (OWNER)
- [ ] `GET /api/v1/admin/usage` → 200 (ADMIN)
- [ ] `GET /api/v1/admin/usage` → 403 (LAWYER)
- [ ] `GET /api/v1/admin/usage` → 401 (non authentifié)
- [ ] `GET /api/v1/admin/usage` → 200 avec listes vides si aucun event

### Isolation workspace

- [ ] Applicable — un OWNER du workspace A ne voit pas les events du workspace B

---

## Dépendances

### Subfeatures bloquantes
- F-15 (usage_events) — `done`

### Questions ouvertes impactées
- Aucune

---

## Notes et décisions

- L'agrégation est faite en mémoire (Java streams) car les volumes V1 sont limités.
- Le `userEmail` est récupéré en joinant sur `auth_accounts` → `users`.
- L'endpoint est dans un controller séparé `AdminUsageController` pour garder `UsageEventController` focalisé sur le niveau dossier.

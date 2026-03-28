# SF-43-01 — Backend : filtre par plage de dates sur GET /api/v1/admin/audit-logs

**Feature parente :** F-43 — Filtre par plage de dates — journal d'actions
**Statut :** En cours
**Estimation :** < 1 jour

---

## Objectif

Ajouter deux paramètres optionnels `from` et `to` (ISO 8601, ex: `2026-01-01T00:00:00Z`) sur `GET /api/v1/admin/audit-logs`. Sans ces paramètres, le comportement actuel (top 50) est conservé.

---

## Comportement nominal

| Paramètres | Comportement |
|---|---|
| aucun | Top 50 entrées les plus récentes (comportement actuel inchangé) |
| `from` seulement | Toutes les entrées avec `createdAt >= from`, triées DESC |
| `to` seulement | Toutes les entrées avec `createdAt <= to`, triées DESC |
| `from` + `to` | Toutes les entrées avec `from <= createdAt <= to`, triées DESC |

---

## Cas d'erreur

| Situation | Réponse |
|---|---|
| `from` ou `to` non parsable | 400 Bad Request (géré automatiquement par Spring) |
| `from > to` | 400 Bad Request (validé dans le service) |
| Rôle insuffisant | 403 Forbidden (comportement inchangé) |

---

## Critères d'acceptation

- [ ] Sans params → retourne les 50 dernières entrées (régression nulle)
- [ ] `?from=2026-03-01T00:00:00Z` → filtre les entrées antérieures à cette date
- [ ] `?from=X&to=Y` → filtre les entrées hors de la plage
- [ ] `?from=X&to=Y` avec `from > to` → 400 Bad Request
- [ ] Isolation workspace respectée dans toutes les branches

---

## Plan de test

### Unitaires (AuditLogAdminServiceTest)
- `U-07` : sans params → appelle `findTop50ByWorkspaceIdOrderByCreatedAtDesc`
- `U-08` : `from` + `to` → appelle `findByWorkspaceIdAndCreatedAtBetween`
- `U-09` : `from > to` → lance `ResponseStatusException` 400

---

## Composants impactés

- `AuditLogRepository` : 3 nouvelles méthodes derived query (`from` seul, `to` seul, `from`+`to`)
- `AuditLogAdminService.getAuditLogs()` : signature étendue avec `Instant from`, `Instant to` (nullables)
- `AuditLogAdminController.getAuditLogs()` : ajout `@RequestParam(required = false) Instant from/to`

---

## Hors périmètre

- Filtre par action côté serveur (actuellement côté client)
- Pagination des résultats filtrés (F-44)
- Modification de l'export CSV (qui reste toujours sans filtre de date)

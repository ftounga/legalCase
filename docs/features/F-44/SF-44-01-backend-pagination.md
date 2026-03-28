# SF-44-01 — Backend : pagination et filtre action serveur sur GET /api/v1/admin/audit-logs

**Feature parente :** F-44 — Pagination et tri côté serveur — journal d'actions
**Statut :** En cours
**Estimation :** < 1 jour

---

## Objectif

Remplacer le retour `List<AuditLogResponse>` (limité à 50) par `Page<AuditLogResponse>` avec support `Pageable` (page, size, sort) et ajout d'un paramètre optionnel `action` pour le filtre côté serveur.

---

## Comportement nominal

- `GET /api/v1/admin/audit-logs?page=0&size=20` → première page, 20 entrées, tri `createdAt DESC` par défaut
- `GET /api/v1/admin/audit-logs?page=1&size=20&sort=createdAt,asc` → deuxième page, tri ascendant
- `GET /api/v1/admin/audit-logs?action=DOCUMENT_DELETED` → filtre sur le type d'action
- `?from`, `?to`, `?action` combinables avec la pagination
- Réponse : `Page<AuditLogResponse>` Spring (`content`, `totalElements`, `totalPages`, etc.)

---

## Cas d'erreur

| Situation | Réponse |
|---|---|
| `from > to` | 400 Bad Request (inchangé) |
| Rôle insuffisant | 403 Forbidden (inchangé) |

---

## Critères d'acceptation

- [ ] Sans params → page 0, size 20, tri `createdAt DESC` — plus de limite à 50
- [ ] `?page=1&size=10` → deuxième page de 10 entrées
- [ ] `?action=DOCUMENT_DELETED` → filtre uniquement les entrées de ce type
- [ ] `?from=X&to=Y&action=A&page=0&size=20` → combinaison complète fonctionnelle
- [ ] Isolation workspace respectée
- [ ] Export CSV (`/export.csv`) inchangé (toujours sans pagination)

---

## Plan de test

### Unitaires (AuditLogAdminServiceTest)
- `U-10` : sans params → appelle le repo avec Pageable page=0, size=20
- `U-11` : `action=DOCUMENT_DELETED` → Specification filtre sur action
- `U-12` : `from` + `to` + `action` + Pageable → combinaison complète

---

## Composants impactés

- `AuditLogRepository` : implémente `JpaSpecificationExecutor<AuditLog>`, supprime les 4 méthodes derived-query de date (remplacées par Specification)
- `AuditLogAdminService.getAuditLogs()` : construit une `Specification` dynamique, retourne `Page<AuditLogResponse>`
- `AuditLogAdminController.getAuditLogs()` : ajoute `@RequestParam(required=false) String action` + `@PageableDefault Pageable`
- Les méthodes `findTop50...` et `findAllBy...` (pour export CSV) sont conservées

---

## Hors périmètre

- Filtre texte (userEmail, caseFileTitle, documentName) côté serveur
- Modification de l'export CSV

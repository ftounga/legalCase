# SF-42-01 — Backend : endpoint export CSV journal d'actions

**Feature parente :** F-42 — Export CSV journal d'actions
**Statut :** En cours
**Estimation :** < 1 jour

---

## Objectif

Ajouter un endpoint `GET /api/v1/admin/audit-logs/export.csv` qui retourne l'intégralité des entrées du journal du workspace au format CSV (sans limite de 50 lignes), accessible aux rôles OWNER et ADMIN uniquement.

---

## Comportement nominal

1. L'utilisateur (OWNER ou ADMIN) appelle `GET /api/v1/admin/audit-logs/export.csv`
2. Le backend résout le workspace du principal appelant
3. Toutes les entrées du workspace sont récupérées, triées par `createdAt` DESC (sans limite)
4. La réponse est un fichier CSV avec :
   - Content-Type : `text/csv; charset=UTF-8`
   - Content-Disposition : `attachment; filename="audit-log.csv"`
   - BOM UTF-8 pour compatibilité Excel
   - En-tête : `Date,Action,Utilisateur,Dossier,Document`
   - Une ligne par entrée : `createdAt ISO,action,userEmail,caseFileTitle,documentName`
   - Les champs contenant une virgule ou des guillemets sont entourés de `"`, les guillemets internes sont doublés

---

## Cas d'erreur

| Situation | Réponse |
|-----------|---------|
| Utilisateur non membre | 404 Not Found |
| Rôle MEMBER (pas OWNER/ADMIN) | 403 Forbidden |
| Journal vide | CSV avec seulement l'en-tête (pas d'erreur) |

---

## Critères d'acceptation

- [ ] `GET /api/v1/admin/audit-logs/export.csv` répond 200 avec Content-Type `text/csv`
- [ ] La réponse contient une ligne d'en-tête et une ligne par entrée
- [ ] Toutes les entrées du workspace sont exportées (pas de limite à 50)
- [ ] Les champs avec caractères spéciaux (virgules, guillemets) sont correctement échappés RFC 4180
- [ ] Un MEMBER reçoit 403
- [ ] Isolation workspace : les entrées d'un autre workspace ne sont pas incluses

---

## Plan de test

### Unitaires (AuditLogAdminServiceTest)
- `exportCsv_returnsCsvWithAllEntries` — 3 entrées, vérifie header + 3 lignes
- `exportCsv_escapesSpecialChars` — caseFileTitle contenant une virgule et des guillemets
- `exportCsv_emptyJournal_returnsHeaderOnly` — journal vide → seulement la ligne d'en-tête

### Intégration (AuditLogAdminControllerTest)
- `GET /export.csv` avec OWNER → 200, Content-Type text/csv
- `GET /export.csv` avec MEMBER → 403
- Isolation : les logs d'un autre workspace ne figurent pas dans l'export

---

## Composants impactés

- `AuditLogRepository` : nouvelle méthode `findAllByWorkspaceIdOrderByCreatedAtDesc(UUID)`
- `AuditLogAdminService` : nouvelle méthode `exportCsv(OidcUser, Principal)` → `String` (CSV complet)
- `AuditLogAdminController` : nouveau endpoint `GET /export.csv` → `ResponseEntity<byte[]>`

---

## Hors périmètre

- Filtrage par dates ou par action dans l'export (F-43)
- Pagination serveur (F-44)
- Modification de l'endpoint `GET /api/v1/admin/audit-logs` existant

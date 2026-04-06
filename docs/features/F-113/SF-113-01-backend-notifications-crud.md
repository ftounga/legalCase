# Mini-spec — F-113 / SF-113-01 Backend : table + API CRUD notifications in-app

---

## Identifiant

`F-113 / SF-113-01`

## Feature parente

`F-113` — Centre de notifications in-app

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-113-01-notifications-crud`

---

## Objectif

Créer la table de notifications in-app et l'API REST CRUD pour lister, compter et marquer comme lues les notifications d'un utilisateur dans son workspace.

---

## Comportement attendu

### Cas nominal

1. Chaque notification est associée à un `user_id` (destinataire) et un `workspace_id`
2. `GET /api/v1/notifications` retourne les notifications de l'utilisateur courant dans son workspace primaire, paginées, triées par date décroissante
3. `GET /api/v1/notifications/unread-count` retourne le nombre de notifications non lues
4. `PATCH /api/v1/notifications/{id}/read` marque une notification comme lue (set `read_at` et `is_read = true`)
5. `PATCH /api/v1/notifications/read-all` marque toutes les notifications non lues du workspace comme lues
6. Tous les endpoints filtrent par `workspace_id` de l'utilisateur courant — isolation stricte

### Types de notifications (enum)

| Type | Description |
|------|------------|
| `ANALYSIS_DONE` | Analyse standard ou enrichie terminée |
| `ANALYSIS_FAILED` | Analyse échouée |
| `DEADLINE_APPROACHING` | Délai légal approchant (J-7) |
| `PROCEDURE_REQUALIFIED` | Point procédural requalifié par l'IA |
| `REFERENTIAL_ALERT` | Alerte de mise à jour référentiel |

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Notification inexistante | Message d'erreur | 404 |
| Notification d'un autre workspace | Accès refusé | 403 |
| Utilisateur non authentifié | Redirect login | 401 |

---

## Critères d'acceptation

- [ ] Table `in_app_notifications` créée via migration Liquibase 052
- [ ] Entity `InAppNotification` avec les champs : id, userId, workspaceId, type (enum), title, message, link, isRead, createdAt, readAt
- [ ] `GET /api/v1/notifications` retourne `Page<InAppNotificationResponse>` paginée, filtrée par workspace + user
- [ ] `GET /api/v1/notifications/unread-count` retourne `{ count: N }`
- [ ] `PATCH /api/v1/notifications/{id}/read` marque la notification comme lue, retourne 200
- [ ] `PATCH /api/v1/notifications/read-all` marque toutes les non-lues comme lues, retourne 200
- [ ] Un utilisateur ne peut pas voir/modifier les notifications d'un autre workspace (403)
- [ ] Tests d'intégration sur chaque endpoint
- [ ] Tests unitaires sur le service
- [ ] Tous les tests existants restent verts

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| is_read | false | Toute notification est non lue à la création |
| read_at | null | Renseigné au moment du marquage lu |
| created_at | now() | Renseigné par @PrePersist |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées |
|-------|-------------|-------------|----------------------------|
| type | Oui | — | ANALYSIS_DONE, ANALYSIS_FAILED, DEADLINE_APPROACHING, PROCEDURE_REQUALIFIED, REFERENTIAL_ALERT |
| title | Oui | 255 | Texte non vide |
| message | Non | 1000 | Texte libre |
| link | Non | 500 | Chemin relatif (ex: /case-files/uuid/synthesis) |

---

## Périmètre

### Hors scope (explicite)

- Alimentation des notifications depuis les événements (SF-113-02)
- Frontend icône cloche + panneau (SF-113-03)
- Endpoint de création publique (les notifications sont créées uniquement côté service, pas via API)
- Préférences de notification utilisateur
- Suppression de notifications

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/notifications` | Oui | MEMBER |
| GET | `/api/v1/notifications/unread-count` | Oui | MEMBER |
| PATCH | `/api/v1/notifications/{id}/read` | Oui | MEMBER |
| PATCH | `/api/v1/notifications/read-all` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `in_app_notifications` | CREATE (DDL) + SELECT + UPDATE | Nouvelle table |

### Migration Liquibase

- [x] Oui — `052-create-in-app-notifications.xml`
- Colonnes : id (UUID PK), user_id (UUID FK NOT NULL), workspace_id (UUID FK NOT NULL), type (VARCHAR(50) NOT NULL), title (VARCHAR(255) NOT NULL), message (VARCHAR(1000)), link (VARCHAR(500)), is_read (BOOLEAN DEFAULT false NOT NULL), created_at (TIMESTAMP WITH TIME ZONE NOT NULL), read_at (TIMESTAMP WITH TIME ZONE)
- Index : (user_id, workspace_id, is_read), (workspace_id, created_at DESC)

### Classes Java

- `InAppNotification` — Entity JPA
- `InAppNotificationRepository` — JpaRepository + JpaSpecificationExecutor
- `InAppNotificationService` — Logique métier (create, list, count, markRead, markAllRead)
- `InAppNotificationController` — REST endpoints
- `InAppNotificationResponse` — Record DTO
- `NotificationType` — Enum
- `UnreadCountResponse` — Record { long count }

---

## Plan de test

### Tests unitaires

- [ ] `InAppNotificationService` — createNotification persiste en base
- [ ] `InAppNotificationService` — markRead met is_read=true et read_at=now
- [ ] `InAppNotificationService` — markAllRead met à jour toutes les non-lues du user+workspace
- [ ] `InAppNotificationService` — getUnreadCount retourne le bon nombre

### Tests d'intégration

- [ ] `GET /api/v1/notifications` → 200 avec liste paginée
- [ ] `GET /api/v1/notifications` → retourne uniquement les notifications du workspace courant
- [ ] `GET /api/v1/notifications/unread-count` → 200 avec { count: N }
- [ ] `PATCH /api/v1/notifications/{id}/read` → 200, notification marquée lue
- [ ] `PATCH /api/v1/notifications/{id}/read` → 404 si notification inexistante
- [ ] `PATCH /api/v1/notifications/{id}/read` → 403 si notification d'un autre workspace
- [ ] `PATCH /api/v1/notifications/read-all` → 200, toutes marquées lues

### Isolation workspace

- [x] Applicable — test : un utilisateur du workspace A ne voit pas les notifications du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [x] **Workspace context** — filtrage par workspace_id sur tous les endpoints
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [ ] Aucune préoccupation transversale

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| Aucun composant existant modifié | Nouvelle table + nouveaux endpoints uniquement | Tests existants doivent rester verts |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (justification : backend uniquement, pas de modification des routes/guards existants)

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- [ ] Aucune

---

## Notes et décisions

- Les notifications sont créées uniquement via `InAppNotificationService.create()` côté backend — pas d'endpoint POST public
- La pagination suit le pattern existant (`@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`)
- Le workspace est résolu via le member primaire de l'utilisateur, comme partout ailleurs dans le projet
- Pas de soft delete — les notifications restent indéfiniment (nettoyage éventuel en V6+)

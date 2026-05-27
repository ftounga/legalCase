# Mini-spec — F-113 / SF-113-04 Truncation defensive title/message/link dans `InAppNotificationService.create()`

## Identifiant

`F-113 / SF-113-04`

## Feature parente

`F-113` — Centre de notifications in-app

## Statut

`draft`

## Date de création

2026-05-27

## Branche Git

`feat/SF-113-04-title-varchar-truncation`

## Exemptions

- **Étape 0 cadrage cohérence métier** : exempté — bugfix backend pur, ne modifie ni workflow ni invariant produit.
- **Étape 0bis cadrage cohérence écran** : exempté — aucun impact UI (le centre de notifications affichait déjà rien dans le cas qui foire ; après le fix, il affichera le titre tronqué + "…").

---

## Contexte (origine de la SF)

Référence : `docs/operations/hotfix-prod.md` → **HF-2026-05-27-02**.

Sur les 24 dernières heures (audit `prod-health-check` 2026-05-27T17:50Z), 8 events ERROR / 2 inserts échoués détectés en production à 08:00:06 UTC (cron `DeadlineAlertService` : `@Scheduled(cron = "0 0 8 * * *")`).

```
ERROR --- [scheduling-1] o.h.engine.jdbc.spi.SqlExceptionHelper : ERROR: value too long for type character varying(255)
DataIntegrityViolationException: insert into in_app_notifications (..., title, ...) values (?, ?, ..., ?)
```

Migration `052-create-in-app-notifications.xml` :
- `title VARCHAR(255) NOT NULL`
- `message VARCHAR(1000)`
- `link VARCHAR(500)`

**Source du débordement** : `DeadlineAlertService.notifyMembers()` ligne 132 :
```java
"Délai J-" + daysRemaining + " : " + deadline.getLabel()
```

`deadline.getLabel()` provient de `case_deadlines.label` (`VARCHAR(255)`, migration 037). Un avocat qui saisit un label de ~245+ chars produit un titre de notification > 255 chars → `DataIntegrityViolationException` → notification **silencieusement perdue** côté utilisateur final (le try-catch ligne 135-137 de `DeadlineAlertService` avale l'erreur en `log.warn`).

**Impact** : 2 notifications de délai perdues le 2026-05-27 (cabinet de prod). L'avocat n'a pas vu le rappel de son délai.

---

## Objectif

Rendre `InAppNotificationService.create()` **défensif** : tronquer chaque champ texte à la longueur max de la colonne DB (255 / 1000 / 500), avec ellipsis `…` si troncation effective. Couvre tous les callers actuels et futurs en un seul point centralisé.

---

## Comportement attendu

### Cas nominal — title ≤ 255 chars

Comportement actuel inchangé : `InAppNotification` persisté avec `title` exact.

### Cas nominal nouveau — title > 255 chars

Le service tronque automatiquement à 254 chars + `…` (= 255 chars total, valide). Logging au niveau INFO indiquant la troncation effective et le titre original (pour audit a posteriori).

### Idem pour `message` (1000) et `link` (500)

Même logique de troncation défensive sur les 3 colonnes texte (cohérence et défense en profondeur — actuellement seul `title` provoque le bug observé, mais `message` peut aussi déborder si une exception trace est passée en message).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `title = null` | `DataIntegrityViolationException` propagée (la colonne est `NOT NULL` — bug appelant à corriger) |
| `message = null` ou `link = null` | OK, colonnes nullable, pas de troncation |
| `title = ""` | OK, vide n'est pas null ni > 255 |

---

## Critères d'acceptation

- **CA-1** : un caller passant un `title` de 300 chars persiste un mapping avec `title.length() == 255` se terminant par `…`. Aucune exception levée.
- **CA-2** : un caller passant un `title` de 250 chars persiste un mapping avec `title` inchangé (250 chars, pas d'ellipsis).
- **CA-3** : un caller passant un `title` de 255 chars persiste le mapping avec `title` inchangé (cas limite — pas de troncation à 255).
- **CA-4** : un caller passant un `message` de 1500 chars persiste un mapping avec `message.length() == 1000` se terminant par `…`.
- **CA-5** : un caller passant un `link` de 600 chars persiste un mapping avec `link.length() == 500` se terminant par `…`.
- **CA-6** : un caller passant `title = null` lève toujours `DataIntegrityViolationException` (régression check : pas de masquage du bug appelant).
- **CA-7** : pas de régression sur les 5 callers existants (`AnalysisNotificationService`, `DeadlineAlertService`, `ExtractionNotificationService`, `ReferentialCheckService`, `RequalificationAlertService`) — tests d'intégration existants restent verts.

---

## Plan de test minimal

### Tests unitaires (Mockito)

Fichier : `backend/src/test/java/fr/ailegalcase/notification/InAppNotificationServiceTest.java` (à créer ou à compléter).

- **T-1** : `create_titleExactly255_unchanged` — input `title` 255 chars → assert `notification.getTitle().length() == 255`, pas de `…`
- **T-2** : `create_titleLongerThan255_truncatedWithEllipsis` — input `title` 300 chars → assert `notification.getTitle().length() == 255` ET se termine par `…`
- **T-3** : `create_messageLongerThan1000_truncated` — input `message` 1500 chars → assert `notification.getMessage().length() == 1000` ET ellipsis
- **T-4** : `create_linkLongerThan500_truncated` — input `link` 600 chars → assert `notification.getLink().length() == 500` ET ellipsis
- **T-5** : `create_messageNull_persistedAsNull` — input `message = null` → assert `notification.getMessage() == null` (pas de NPE)
- **T-6** : `create_titleNull_propagatesException` — input `title = null` → assert pas de tentative de truncation (NPE sur length()) ou délégation à JPA qui levera la contrainte NOT NULL

### Test d'intégration (optionnel)

Si un IT existant teste un caller (ex. `DeadlineAlertServiceIT`), ajouter un scenario avec un `deadline.label` de 250 chars → assert que la notification est créée (pas d'exception).

---

## Périmètre — fichiers impactés

| Fichier | Changement |
|---|---|
| `backend/src/main/java/fr/ailegalcase/notification/InAppNotificationService.java` | Méthode `create()` : avant `setTitle/setMessage/setLink`, appliquer `truncateWithEllipsis(value, maxLen)`. Helper privé statique. |
| `backend/src/test/java/fr/ailegalcase/notification/InAppNotificationServiceTest.java` | À créer si absent — 6 UT (T-1 à T-6) |

### Constantes de longueur

```java
private static final int TITLE_MAX = 255;
private static final int MESSAGE_MAX = 1000;
private static final int LINK_MAX = 500;
private static final String ELLIPSIS = "…";
```

### Algorithme de truncation

```java
private static String truncateWithEllipsis(String value, int maxLen) {
    if (value == null) return null;
    if (value.length() <= maxLen) return value;
    // -1 pour réserver la place de l'ellipsis (1 char)
    return value.substring(0, maxLen - 1) + ELLIPSIS;
}
```

Note : `ELLIPSIS` = "…" (U+2026) = **3 bytes UTF-8, mais 1 char Java**. La colonne PostgreSQL `VARCHAR(255)` compte les **caractères**, pas les bytes — donc tronquer à `maxLen - 1` est correct. Vérification : `"a".repeat(254) + "…"` → 255 chars Java → 255 chars PostgreSQL → OK.

### Hors périmètre

- Élargissement de `title` à VARCHAR(500) via migration — non, on garde la défense en profondeur côté code (plus robuste, ne déplace pas le problème vers VARCHAR(500)).
- Tronquer côté caller (ex. `DeadlineAlertService`) — non, centralisation côté service plus défensif (couvre les 5 callers + tout futur caller).
- Refresh des libellés de toutes les notifications passées pour les normaliser — non, les notifs passées sont en lecture seule.

---

## Préoccupations transversales

- **Auth/Principal** : ❌ inchangé.
- **Workspace context** : ❌ inchangé (la méthode `create()` reçoit déjà `userId` et `workspaceId`).
- **Plans/limites** : ❌ inchangé.
- **Navigation/routing** : ❌ inchangé.
- **Outil décisionnel métier** : ❌ aucun outil touché.

Aucun smoke test E2E requis.

---

## Risques

| Risque | Probabilité | Mitigation |
|---|---|---|
| Une notification très importante (alerte critique) se retrouve tronquée et l'utilisateur ne comprend pas le contexte complet | Faible (les notifs sont des résumés courts par design) | Le `message` (1000 chars) capture le détail. La troncation préserve les 254 premiers chars + `…` qui indique visuellement la troncation. |
| Performance : un `String.substring` + concat sur chaque create | Négligeable (1 notif = quelques µs, vs IO DB qui domine) | Pas d'optim nécessaire. |

---

## Hors scope

- Tronquer côté frontend pour affichage compact (la base contient déjà le titre tronqué — pas de double troncation côté UI).
- Métriques Prometheus sur les troncations (à ajouter si signal utile).
- Audit log dédié des troncations (le log INFO suffit pour V1).

---

## DoD

- [ ] Tests unitaires T-1 à T-6 verts.
- [ ] `mvn -Dtest=InAppNotificationServiceTest test` vert.
- [ ] `docs/operations/hotfix-prod.md` : HF-2026-05-27-02 annoté `Fixed by #<PR>` post-merge.
- [ ] À valider au prochain run `prod-health-check` : plus de nouvelle ligne ERROR `value too long for type character varying(255)` sur les notifs in-app prod.

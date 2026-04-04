# Mini-spec — F-110 / SF-110-04 : Mise à jour IA automatique (cron + email + badge)

## Identifiant
`F-110 / SF-110-04`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`ready`

## Date de création
`2026-04-04`

## Branche Git
`feat/SF-110-04-referentials-cron-update`

---

## Objectif
Un cron configurable (défaut 6 mois) déclenche une vérification Claude Haiku des référentiels système. Si divergence détectée : création d'une alerte `ReferentialAlert`, email immédiat aux OWNERs de tous les workspaces, badge rouge persistant dans le sidenav, relance email à J+15 si aucune réponse. L'OWNER peut appliquer ou ignorer. Aucune application automatique.

---

## Comportement attendu

### Cas nominal
1. Cron déclenché (défaut : `0 0 0 1 */6 *`, configurable via `app.referentials.check-cron`).
2. Pour chaque entrée système (`workspaceId=NULL`), appel Claude Haiku avec prompt web search.
3. Claude retourne `UP_TO_DATE` → rien.
4. Claude retourne `OUTDATED: {json} | explication` → crée `ReferentialAlert` (status=PENDING) si pas déjà PENDING pour cette entrée.
5. Email aux OWNERs de tous les workspaces.
6. Badge rouge dans le sidenav si `GET /api/v1/referentials/alerts/pending-count > 0`.
7. Cron quotidien J+15 : relance email si alerte PENDING > 15j et `reminderSentAt IS NULL`.
8. OWNER clique "Appliquer" → `POST /api/v1/referentials/alerts/{id}/apply` → workspace override créé, status=APPLIED.
9. OWNER clique "Ignorer" → `POST /api/v1/referentials/alerts/{id}/dismiss` → status=DISMISSED.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| Claude indisponible pendant cron | Log warn, skip l'entrée (fail-open) | N/A |
| Aucun OWNER dans le workspace | Log warn, pas d'email | N/A |
| apply/dismiss par MEMBER | 403 | 403 |
| alertId introuvable | 404 | 404 |
| Alerte déjà traitée | 409 | 409 |

---

## Critères d'acceptation

- [x] Cron configurable via `app.referentials.check-cron` (défaut `0 0 0 1 */6 *`)
- [x] Vérification Claude Haiku par entrée système (fail-open)
- [x] Table `referential_alerts` : id, entry_id, status, proposed_value_json, ai_message, detected_at, resolved_at, reminder_sent_at
- [x] Pas de doublon : alerte PENDING existante pour même entrée → pas de nouvelle alerte
- [x] Email envoyé à tous les OWNERs à la détection
- [x] `GET /api/v1/referentials/alerts/pending-count` retourne `{"count": N}`
- [x] Sidenav Angular : badge rouge si count > 0 (polling 5 min)
- [x] Cron quotidien J+15 : relance email + `reminderSentAt` mis à jour
- [x] `POST /api/v1/referentials/alerts/{id}/apply` → workspace override, status=APPLIED
- [x] `POST /api/v1/referentials/alerts/{id}/dismiss` → status=DISMISSED
- [x] Entrées avec alerte PENDING mises en évidence dans ReferentialsComponent (bandeau rouge + boutons)
- [x] MEMBER : ne voit pas les boutons apply/dismiss

---

## Périmètre

### Hors scope
- Configuration par workspace du cron
- Historique des alertes passées (APPLIED/DISMISSED) non affiché
- Push notification
- Alerte par workspace indépendant (première action OWNER s'applique globalement)

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/referentials/alerts/pending-count` | Oui | MEMBER |
| POST | `/api/v1/referentials/alerts/{alertId}/apply` | Oui | OWNER ou ADMIN |
| POST | `/api/v1/referentials/alerts/{alertId}/dismiss` | Oui | OWNER ou ADMIN |

### Nouvelles classes backend
- `ReferentialAlert` (entité JPA)
- `ReferentialAlertRepository`
- `ReferentialCheckService` (cron `app.referentials.check-cron`)
- `ReferentialAlertService` (apply/dismiss/pendingCount)
- `ReferentialReminderScheduler` (cron quotidien J+15)
- `ReferentialAlertController`
- Migration `050-create-referential-alerts.xml`
- `findAllSystemEntries()` dans `LegalReferentialRepository`
- `findByMemberRole()` dans `WorkspaceMemberRepository`
- `sendReferentialAlert()` + `sendReferentialAlertReminder()` dans `EmailService`

### Composants Angular modifiés
- `ReferentialsComponent` : bandeau alerte + boutons apply/dismiss + `applyAlert()` + `dismissAlert()`
- `ShellComponent` : badge rouge sur lien sidenav (polling 5 min), `ngOnDestroy` nettoyage interval

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| referential_alerts | CREATE | Nouvelle table (migration 050) |
| legal_referentials | INSERT (apply) | Crée workspace override |

---

## Plan de test

### Backend — unitaires
- [x] `ReferentialCheckService` — réponse `UP_TO_DATE` → aucune alerte créée
- [x] `ReferentialCheckService` — réponse `OUTDATED` → alerte créée + notifyOwners appelé
- [x] `ReferentialCheckService` — exception Claude → fail-open, pas d'alerte
- [x] `ReferentialCheckService` — alerte déjà PENDING → pas de doublon
- [x] `ReferentialAlertService.apply()` → workspace override créé, status=APPLIED
- [x] `ReferentialAlertService.dismiss()` → status=DISMISSED
- [x] `ReferentialAlertService.apply()` sur alerte déjà APPLIED → 409
- [x] `ReferentialAlertService.pendingCount()` → retourne 0 si aucune alerte
- [x] `ReferentialAlertService.pendingCount()` → retourne le bon compte

### Backend — intégration
- [x] `GET /api/v1/referentials/alerts/pending-count` → 0 si aucune alerte
- [x] `POST .../apply` → 403 MEMBER
- [x] `POST .../dismiss` → 403 MEMBER
- [x] `POST .../apply` → 200 OWNER
- [x] `POST .../dismiss` → 200 OWNER

### Frontend — unitaires
- [x] `pendingAlertsCount = 0` si API retourne 0
- [x] `pendingAlertsCount = 3` si API retourne 3

---

## Analyse d'impact

### Préoccupations transversales
- [x] **Auth / Principal** — POST apply/dismiss vérifient le rôle OWNER/ADMIN
  - Composants impactés : `ReferentialAlertController` (nouveau), `ReferentialController` (existant, non modifié)
- [x] **Workspace context** — apply crée workspace override avec workspaceId du caller
  - Composants résolvant le workspace : `ReferentialAlertController` (via `OAuthProviderResolver`)

| Composant | Impact potentiel | Non-régression |
|-----------|-----------------|----------------|
| `ReferentialController` | non impacté | GET/PUT existants inchangés |
| `ReferentialsComponent` | bandeau alerte + boutons | MEMBER : boutons absents |
| `ShellComponent` | badge sidenav (polling) | Lien existant non modifié, 20 tests verts |

### Smoke tests
Aucune route nouvelle ni guard modifié — smoke tests non impactés.

---

## Dépendances
- SF-110-01 — Done
- SF-110-02 — Done
- SF-110-03 — Done

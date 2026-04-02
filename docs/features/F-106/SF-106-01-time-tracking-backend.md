# Mini-spec — F-106 / SF-106-01 — Suivi du temps facturable — backend

## Identifiant
`F-106 / SF-106-01`

## Feature parente
`F-106` — Suivi du temps facturable par dossier

## Statut
`draft`

## Date de création
2026-04-02

## Branche Git
`feat/SF-106-01-time-tracking-backend`

---

## Objectif

Exposer les endpoints backend permettant de démarrer/arrêter un timer par dossier, de configurer le taux horaire par utilisateur et par workspace, et de générer un rapport mensuel des heures facturables avec export CSV.

---

## Comportement attendu

### Cas nominal

**Taux horaire**
- L'utilisateur configure son taux horaire via `PUT /api/v1/workspace/billing-rate` (body : `{ "ratePerHour": 250.00 }`).
- La table `user_billing_rates` conserve l'historique : chaque PUT insère une nouvelle ligne avec `effective_from = today`. Le taux actif est celui dont `effective_from` est le plus récent.
- `GET /api/v1/workspace/billing-rate` retourne le taux actif pour l'utilisateur courant dans le workspace courant, ou `null` si non configuré.

**Timer**
- `POST /api/v1/case-files/{caseFileId}/time-entries/start` : crée une entrée avec `started_at = now()`, `stopped_at = null`. Un utilisateur ne peut avoir qu'un seul timer actif à la fois (toutes subfeatures confondues).
- `POST /api/v1/time-entries/{id}/stop` : renseigne `stopped_at = now()` et calcule `duration_seconds = EXTRACT(EPOCH FROM stopped_at - started_at)`.
- `GET /api/v1/case-files/{caseFileId}/time-entries` : liste toutes les entrées du dossier pour le workspace courant, triées par `started_at DESC`.

**Rapport mensuel**
- `GET /api/v1/workspace/time-report?month=2026-04` : retourne, pour chaque dossier du workspace, la somme des `duration_seconds` sur le mois, le taux horaire de l'utilisateur en vigueur à la date de `started_at`, et le montant calculé.
- `GET /api/v1/workspace/time-report/export?month=2026-04` : retourne un CSV (`Content-Type: text/csv`, `Content-Disposition: attachment; filename=rapport-temps-2026-04.csv`) avec les colonnes : `Dossier,Utilisateur,Heures,Taux horaire (€),Montant (€)`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Démarrer un timer sur un dossier d'un autre workspace | Accès refusé | 403 |
| Démarrer un timer quand un autre est déjà actif | Conflit — message explicite | 409 |
| Arrêter un timer appartenant à un autre utilisateur | Accès refusé | 403 |
| Arrêter un timer déjà arrêté | Requête invalide | 400 |
| `{id}` de time_entry inexistant | Non trouvé | 404 |
| `ratePerHour` négatif ou nul | Requête invalide | 400 |
| Paramètre `month` absent ou mal formaté (pas `YYYY-MM`) | Requête invalide | 400 |
| Dossier non trouvé ou supprimé (soft delete) | Non trouvé | 404 |

---

## Critères d'acceptation

- [ ] `PUT /api/v1/workspace/billing-rate` insère une ligne dans `user_billing_rates` avec `effective_from = today`
- [ ] `GET /api/v1/workspace/billing-rate` retourne le taux le plus récent ou `null`
- [ ] `POST .../time-entries/start` crée une entrée active (`stopped_at = null`)
- [ ] Un second start sans stop préalable retourne 409
- [ ] `POST /api/v1/time-entries/{id}/stop` renseigne `stopped_at` et calcule `duration_seconds`
- [ ] `GET .../time-entries` est isolé par `workspace_id`
- [ ] Le rapport mensuel agrège correctement heures × taux effectif à la date de l'entrée
- [ ] L'export CSV est valide et téléchargeable
- [ ] Toutes les routes vérifient l'isolation workspace (403 si workspace différent)
- [ ] Un timer oublié (pas de stop) n'apparaît pas dans le rapport (ou apparaît avec `duration = null`, selon règle ci-dessous)

**Règle timer ouvert dans le rapport :** un timer sans `stopped_at` est ignoré du rapport mensuel (il sera comptabilisé au mois de sa clôture).

---

## Périmètre

### Hors scope
- Frontend timer et rapport (SF-106-02 et SF-106-03)
- Insight IA dans la synthèse (SF-106-04)
- Rappel / alerte "timer oublié ouvert"
- Intégration facturation Stripe
- Gestion des notes libres sur une entrée de temps (V5)
- Suppression / modification manuelle d'une entrée de temps (V5)

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|-------|-------------|-----------------|-------|
| `ratePerHour` | Oui | Décimal > 0, max 9999.99 | Rejeté si ≤ 0 |
| `caseFileId` | Oui | UUID valide, dans le workspace | 403 si hors workspace |
| `month` | Oui | `YYYY-MM` | Validé par regex |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| PUT | `/api/v1/workspace/billing-rate` | Oui | MEMBER |
| GET | `/api/v1/workspace/billing-rate` | Oui | MEMBER |
| POST | `/api/v1/case-files/{caseFileId}/time-entries/start` | Oui | MEMBER |
| POST | `/api/v1/time-entries/{id}/stop` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/time-entries` | Oui | MEMBER |
| GET | `/api/v1/workspace/time-report` | Oui | OWNER / ADMIN |
| GET | `/api/v1/workspace/time-report/export` | Oui | OWNER / ADMIN |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `time_entries` | CREATE | Nouvelle table — migration 043 |
| `user_billing_rates` | CREATE | Nouvelle table — migration 043 |
| `case_files` | SELECT | Vérification workspace + existence |
| `workspace_members` | SELECT | Résolution workspace courant |

### Migration Liquibase
- [x] Oui — `043-create-time-tracking.xml`

```sql
-- user_billing_rates
CREATE TABLE user_billing_rates (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id),
  workspace_id    UUID NOT NULL REFERENCES workspaces(id),
  rate_per_hour   NUMERIC(10,2) NOT NULL CHECK (rate_per_hour > 0),
  effective_from  DATE NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_billing_rates_user_workspace ON user_billing_rates(user_id, workspace_id);

-- time_entries
CREATE TABLE time_entries (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  case_file_id     UUID NOT NULL REFERENCES case_files(id),
  workspace_id     UUID NOT NULL REFERENCES workspaces(id),
  user_id          UUID NOT NULL REFERENCES users(id),
  started_at       TIMESTAMP NOT NULL,
  stopped_at       TIMESTAMP,
  duration_seconds INTEGER,
  created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_time_entries_case_file ON time_entries(case_file_id);
CREATE INDEX idx_time_entries_workspace_month ON time_entries(workspace_id, started_at);
CREATE INDEX idx_time_entries_user_active ON time_entries(user_id) WHERE stopped_at IS NULL;
```

### Composants Spring Boot

- `TimeEntryController` — endpoints start/stop/list
- `TimeReportController` — endpoints rapport + export CSV
- `BillingRateController` — endpoints GET/PUT taux horaire
- `TimeEntryService` — logique start/stop, validation timer actif
- `TimeReportService` — agrégation mensuelle, calcul montant, génération CSV
- `BillingRateService` — résolution du taux effectif à une date donnée
- `TimeEntryRepository`, `UserBillingRateRepository`
- DTOs : `TimeEntryResponse`, `BillingRateRequest`, `BillingRateResponse`, `TimeReportResponse`, `TimeReportLineResponse`

---

## Plan de test

### Tests unitaires

- [ ] `TimeEntryService` — start nominal : entrée créée avec `stopped_at = null`
- [ ] `TimeEntryService` — start avec timer actif existant → `ConflictException`
- [ ] `TimeEntryService` — stop nominal : `duration_seconds` calculé correctement
- [ ] `TimeEntryService` — stop d'une entrée déjà stoppée → `BadRequestException`
- [ ] `BillingRateService` — taux le plus récent retourné correctement parmi plusieurs lignes
- [ ] `BillingRateService` — taux effectif à une date passée : retourne la ligne en vigueur à cette date
- [ ] `TimeReportService` — agrégation mensuelle : somme correcte, montant = heures × taux
- [ ] `TimeReportService` — entrée sans `stopped_at` exclue du rapport
- [ ] `TimeReportService` — génération CSV : séparateurs, entêtes, encodage UTF-8

### Tests d'intégration

- [ ] `PUT /api/v1/workspace/billing-rate` → 200, ligne insérée en base
- [ ] `PUT /api/v1/workspace/billing-rate` avec `ratePerHour = 0` → 400
- [ ] `GET /api/v1/workspace/billing-rate` → taux actif retourné
- [ ] `GET /api/v1/workspace/billing-rate` sans taux configuré → `null`
- [ ] `POST .../time-entries/start` → 201, `stopped_at = null`
- [ ] `POST .../time-entries/start` (2e appel sans stop) → 409
- [ ] `POST .../time-entries/start` sur dossier d'un autre workspace → 403
- [ ] `POST /api/v1/time-entries/{id}/stop` → 200, `duration_seconds` > 0
- [ ] `POST /api/v1/time-entries/{id}/stop` déjà stoppé → 400
- [ ] `POST /api/v1/time-entries/{id}/stop` appartenant à un autre user → 403
- [ ] `GET .../time-entries` → liste isolée par workspace
- [ ] `GET /api/v1/workspace/time-report?month=2026-04` → agrégation correcte
- [ ] `GET /api/v1/workspace/time-report?month=invalid` → 400
- [ ] `GET /api/v1/workspace/time-report/export?month=2026-04` → Content-Type text/csv

### Isolation workspace

- [ ] Un utilisateur du workspace A ne peut pas démarrer un timer sur un dossier du workspace B (403)
- [ ] Le rapport mensuel du workspace A n'inclut pas les entrées du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Workspace context** — chaque endpoint résout le workspace courant via `@AuthenticationPrincipal`
- [ ] Auth / Principal — pas de modification du Principal
- [ ] Plans / limites — pas de gate billing sur cette subfeature
- [ ] Navigation / routing frontend — backend only

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `CaseFileController` | Nouveaux endpoints imbriqués `/case-files/{id}/time-entries/*` — pas de modification des endpoints existants | Tests existants doivent rester verts |
| `WorkspaceController` | Nouveaux endpoints `/workspace/billing-rate` et `/workspace/time-report` — pas de modification | Tests existants doivent rester verts |

### Smoke tests E2E concernés
- [ ] `e2e/smoke/auth.spec.ts` — login local → vérifier que les nouveaux endpoints n'introduisent pas de régression
- [ ] `e2e/smoke/workspace.spec.ts` — switch workspace → isolation vérifiée

---

## Dépendances

### Subfeatures bloquantes
Aucune.

### Questions ouvertes impactées
- [x] **Taux horaire : par utilisateur, stocké dans `user_billing_rates` (historique)** — tranché le 2026-04-02

---

## Notes et décisions

- **Timer actif unique par utilisateur** : vérification via `SELECT 1 FROM time_entries WHERE user_id = ? AND stopped_at IS NULL`. Si une ligne existe → 409.
- **Taux effectif à une date** : `SELECT rate_per_hour FROM user_billing_rates WHERE user_id = ? AND workspace_id = ? AND effective_from <= :date ORDER BY effective_from DESC LIMIT 1`.
- **Rapport mensuel** : si l'utilisateur n'a pas de taux configuré à la date de l'entrée, le montant est `null` (pas d'erreur — juste non calculé).
- **Export CSV** : généré en mémoire via `StringBuilder`, pas de librairie externe. Encodage UTF-8 avec BOM pour compatibilité Excel.
- **Rôle rapport** : OWNER/ADMIN uniquement — un collaborateur ne voit pas les heures des autres membres.

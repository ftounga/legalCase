# Mini-spec — F-JU-01 / SF-JU-01-05 Bootstrap auto Claude + dashboard admin

## Identifiant
`F-JU-01 / SF-JU-01-05`

## Feature parente
`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels

## Statut
`draft`

## Date de création
2026-05-22

## Branche Git
`feat/SF-JU-01-05-bootstrap-dashboard`

---

## Objectif

Livrer (a) le **bootstrap automatique Claude** déclenché manuellement par un super-admin pour pré-remplir `tool_jurisprudence_mappings` à partir d'une liste explicite `(toolId, brancheCalculId, motCleRecherche)` ; (b) le **dashboard admin** `/super-admin/jurisprudence-watch` exposant la liste des flags PENDING et l'audit log + 3 actions 1-clic (REPLACE / ADD / IGNORE).

---

## Comportement attendu

### Bootstrap (backend)

Endpoint `POST /api/v1/super-admin/jurisprudence-watch/bootstrap` (rôle SUPER_ADMIN), payload `{ entries: [{toolId, brancheCalculId, motCleRecherche, juridictionFiltre?, dateMin?}] }` (1 à 200 entrées par batch).

Pour chaque entrée :
1. Appel `JudilibreApiClient` avec `motCleRecherche` + filtres
2. Top-N=20 arrêts retournés
3. `ClaudeJurisprudenceEvaluator` (réutilisé SF-02) construit un prompt « sélectionne les 1-3 arrêts les plus structurants pour cette branche »
4. INSERT 0 à 3 lignes dans `tool_jurisprudence_mappings`
5. INSERT 0 à 3 lignes dans `jurisprudence_audit_log` action `AUTO_ADD` actor `SUPER_ADMIN`, claude_reason + claude_confidence

Garde-fou : si Claude renvoie 0 mapping pour une entrée → entry skipped, log INFO. Si Claude renvoie > 3 → on tronque à 3.

Retour : `BootstrapResponse { entriesProcessed, mappingsCreated, entriesSkipped, durationMs }`.

### Dashboard admin (frontend + backend)

**Backend** :
- `GET /api/v1/super-admin/jurisprudence-watch/flags?statut={PENDING|REVIEWED|IGNORED}&page=0&size=20` — paginé, tri `createdAt DESC`
- `POST /api/v1/super-admin/jurisprudence-watch/flags/{flagId}/arbitrate {decision: REPLACE|ADD|IGNORE, comment?}` — applique la décision, met le flag à REVIEWED, écrit audit log MANUAL_*
- `GET /api/v1/super-admin/jurisprudence-watch/audit-log?page=0&size=50` — paginé, tri `createdAt DESC`

**Frontend** : route `/super-admin/jurisprudence-watch`, composant standalone avec 2 sections :
1. Flags PENDING (liste paginée) avec 3 boutons inline par flag (Remplacer / Ajouter / Ignorer) + textarea commentaire optionnel
2. Audit log (liste paginée, consultation seule)

---

## Critères d'acceptation

- [ ] **CA-01** — Endpoint `POST /bootstrap` (SUPER_ADMIN) accepte 1-200 entrées, retourne `BootstrapResponse`. 403 si non SUPER_ADMIN.
- [ ] **CA-02** — Endpoint `GET /flags` (SUPER_ADMIN) paginé, filtré par statut.
- [ ] **CA-03** — Endpoint `POST /flags/{flagId}/arbitrate` (SUPER_ADMIN) applique la décision (REPLACE archive ancien + crée nouveau, ADD crée nouveau, IGNORE marque flag IGNORED) + audit log MANUAL_*.
- [ ] **CA-04** — Endpoint `GET /audit-log` (SUPER_ADMIN) paginé.
- [ ] **CA-05** — Composant Angular `JurisprudenceWatchDashboardComponent` standalone à la route `/super-admin/jurisprudence-watch`, sections Flags + Audit, actions inline.
- [ ] **CA-06** — Tests UT backend service + UT contrôleur (4-6 tests) sur les nouveaux endpoints.
- [ ] **CA-07** — Tests Jest frontend (3-5 tests) sur le composant dashboard.

---

## Périmètre / Hors scope V1

- ❌ Pas d'IT (couverts par patterns existants — manque de temps en session)
- ❌ Pas d'ergonomie avancée (recherche, filtre par outil, export CSV) — V2
- ❌ Pas de bootstrap automatique sur les ~80 outils — chaque entrée doit être déclenchée explicitement par l'admin via un payload contrôlé
- ❌ Email mensuel récap pour Audit a déjà été livré SF-02

---

## Technique

### Endpoints
| Méthode | URL | Rôle |
|---|---|---|
| POST | `/api/v1/super-admin/jurisprudence-watch/bootstrap` | SUPER_ADMIN |
| GET | `/api/v1/super-admin/jurisprudence-watch/flags` | SUPER_ADMIN |
| POST | `/api/v1/super-admin/jurisprudence-watch/flags/{flagId}/arbitrate` | SUPER_ADMIN |
| GET | `/api/v1/super-admin/jurisprudence-watch/audit-log` | SUPER_ADMIN |

### Tables
- `tool_jurisprudence_mappings` (INSERT + UPDATE archived)
- `jurisprudence_watch_flags` (UPDATE statut/decision/reviewed_at/reviewed_by)
- `jurisprudence_audit_log` (INSERT MANUAL_* / AUTO_ADD bootstrap)

### Classes Java introduites
- `JurisprudenceBootstrapRequest` + `JurisprudenceBootstrapResponse` + `JurisprudenceBootstrapEntry` (records)
- `JurisprudenceArbitrateRequest` (record)
- `JurisprudenceWatchFlagResponse` + `JurisprudenceAuditLogResponse` (records)
- `JurisprudenceWatchAdminController`
- `JurisprudenceWatchAdminService`
- `JurisprudenceBootstrapService`

### Frontend
- Service `JurisprudenceWatchAdminClientService`
- Composant `JurisprudenceWatchDashboardComponent` (standalone)
- Route ajoutée dans `super-admin` routing

---

## Notes
1. Bootstrap déclenché manuellement (pas de cron) — l'admin pousse une liste contrôlée. Évite tout coût LLM imprévu.
2. Dashboard frontend minimal V1 (lecture + 3 actions). UX avancée différée V2.
3. Pas d'IT pour rester dans le temps imparti. Tests UT couvrent service + logique d'arbitrage.

### Coût estimé
- ~2,5 j dev backend + frontend.

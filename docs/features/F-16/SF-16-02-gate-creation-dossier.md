# Mini-spec — F-16 / SF-16-02 — Gate création dossier

## Identifiant
`F-16 / SF-16-02`

## Feature parente
`F-16` — Gestion des abonnements

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-16-02-gate-creation-dossier`

---

## Objectif

Bloquer la création d'un dossier avec HTTP 402 si le workspace a atteint son quota de dossiers OPEN selon son plan (Starter : 3, Pro : 20).

---

## Comportement attendu

### Cas nominal

- Avant de créer un dossier, compter les dossiers avec `status = OPEN` du workspace.
- Si le compte est strictement inférieur à la limite du plan → création autorisée (flux normal inchangé).
- La limite est lue depuis la `Subscription` active du workspace (`plan_code`).

### Limites par plan

| Plan | Dossiers OPEN max |
|------|-------------------|
| STARTER | 3 |
| PRO | 20 |

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Quota OPEN atteint | `{"error": "QUOTA_EXCEEDED", "message": "Limite de dossiers actifs atteinte pour votre plan."}` | 402 |
| Subscription absente pour le workspace | Création autorisée (fail open — cas dégradé) | 201 |

---

## Critères d'acceptation

- [ ] `CaseFileRepository` expose `countByWorkspaceIdAndStatus(UUID workspaceId, String status)`
- [ ] `PlanLimitService` retourne la limite de dossiers OPEN selon le `plan_code`
- [ ] `CaseFileService.create` vérifie le quota avant de persister
- [ ] Retourne 402 avec body JSON si quota atteint
- [ ] Si pas de subscription trouvée → création autorisée (fail open)
- [ ] Tests unitaires : quota non atteint, quota atteint, pas de subscription
- [ ] Test d'intégration : POST → 402 quand quota atteint

---

## Périmètre

### Hors scope (explicite)

- Gate upload document (SF-16-03)
- Gate re-analyse enrichie (SF-16-04)
- API REST d'exposition du plan
- Modification du plan

---

## Technique

### Endpoint impacté

| Méthode | URL | Modification |
|---------|-----|-------------|
| POST | `/api/v1/case-files` | Ajout du contrôle quota avant persistance |

### Tables lues

| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | SELECT | Lire `plan_code` du workspace |
| `case_files` | COUNT | Compter les dossiers OPEN du workspace |

### Migration Liquibase

- [x] Non applicable

### Nouveaux artefacts

| Artefact | Package | Description |
|----------|---------|-------------|
| `PlanLimitService` | `fr.ailegalcase.billing` | Retourne les limites selon le `plan_code` |

---

## Plan de test

### Tests unitaires

- [ ] `PlanLimitService.getMaxOpenCaseFiles("STARTER")` → 3
- [ ] `PlanLimitService.getMaxOpenCaseFiles("PRO")` → 20
- [ ] `CaseFileService.create` — quota non atteint (2/3) → création OK
- [ ] `CaseFileService.create` — quota atteint (3/3 Starter) → 402
- [ ] `CaseFileService.create` — pas de subscription → création autorisée (fail open)

### Tests d'intégration

- [ ] POST `/api/v1/case-files` avec 3 dossiers OPEN existants (STARTER) → 402
- [ ] POST `/api/v1/case-files` avec 2 dossiers OPEN existants (STARTER) → 201

### Isolation workspace

- [ ] Applicable — le comptage est filtré par `workspace_id`

---

## Dépendances

### Subfeatures bloquantes

- SF-16-01 — statut : done (table subscriptions disponible)

---

## Notes et décisions

- Fail open si subscription absente : évite de bloquer des workspaces legacy ou mal initialisés.
- `PlanLimitService` centralisé dans `billing` pour être réutilisé par SF-16-03 et SF-16-04.
- Code HTTP 402 (Payment Required) est le standard pour les dépassements de quota SaaS.

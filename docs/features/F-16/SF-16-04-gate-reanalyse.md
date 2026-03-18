# Mini-spec — F-16 / SF-16-04 — Gate re-analyse enrichie

## Identifiant
`F-16 / SF-16-04`

## Feature parente
`F-16` — Gestion des abonnements

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-16-04-gate-reanalyse`

---

## Objectif

Bloquer la demande de re-analyse enrichie avec HTTP 402 si le workspace est en plan STARTER (la re-analyse enrichie est réservée au plan PRO).

---

## Comportement attendu

### Cas nominal

- Dans `ReAnalysisCommandService.triggerReAnalysis()`, vérifier le plan du workspace avant de publier le message RabbitMQ.
- Si le plan est PRO → flux normal inchangé (publication RabbitMQ, 202 Accepted).
- La limite est lue depuis la `Subscription` active du workspace via `PlanLimitService`.

### Limites par plan

| Plan | Re-analyse enrichie |
|------|---------------------|
| STARTER | Interdite (402) |
| PRO | Autorisée |

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Plan STARTER | 402 avec message "La re-analyse enrichie est réservée au plan Pro." | 402 |
| Subscription absente | Re-analyse autorisée (fail open) | 202 |

---

## Critères d'acceptation

- [ ] `PlanLimitService` expose `isEnrichedAnalysisAllowed(String planCode)` et `isEnrichedAnalysisAllowedForWorkspace(UUID workspaceId)`
- [ ] `ReAnalysisCommandService.triggerReAnalysis` vérifie le droit avant la publication RabbitMQ
- [ ] Retourne 402 si plan STARTER
- [ ] Fail open si pas de subscription
- [ ] Le message RabbitMQ n'est PAS publié en cas de 402 (pas de job orphelin)
- [ ] Tests unitaires : PRO → autorisé, STARTER → 402, pas de subscription → fail open

---

## Périmètre

### Hors scope (explicite)

- Gate upload document (SF-16-03)
- Gate création dossier (SF-16-02)
- Rétrogradation de plan (modification de subscription)

---

## Technique

### Endpoint impacté

| Méthode | URL | Modification |
|---------|-----|-------------|
| POST | `/api/v1/case-files/{id}/re-analyze` | Ajout contrôle plan avant publication RabbitMQ |

### Tables lues

| Table | Opération | Notes |
|-------|-----------|-------|
| `subscriptions` | SELECT | Via `PlanLimitService` |

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

- [ ] `PlanLimitService.isEnrichedAnalysisAllowed("PRO")` → true
- [ ] `PlanLimitService.isEnrichedAnalysisAllowed("STARTER")` → false
- [ ] `ReAnalysisCommandService.triggerReAnalysis` — plan PRO → 202, RabbitMQ publié
- [ ] `ReAnalysisCommandService.triggerReAnalysis` — plan STARTER → 402, RabbitMQ non publié
- [ ] `ReAnalysisCommandService.triggerReAnalysis` — pas de subscription → fail open, RabbitMQ publié

### Isolation workspace

- [ ] Applicable — le dossier est résolu dans le contexte du workspace (`resolveCaseFile` dans le service)

---

## Dépendances

### Subfeatures bloquantes

- SF-16-01 — statut : done
- SF-16-02 — statut : done
- SF-16-03 — statut : in-review (`PlanLimitService` disponible)

---

## Notes et décisions

- Le 402 est levé AVANT la publication RabbitMQ pour éviter tout job ENRICHED_ANALYSIS orphelin.
- `isEnrichedAnalysisAllowed` est ajouté à `PlanLimitService` pour centraliser toutes les règles de plan.
- Le fail open s'applique : absence de subscription = accès autorisé (cohérent avec SF-16-02 et SF-16-03).

# Mini-spec — F-34 / SF-34-02 Visibilité budget tokens mensuel — page admin

## Identifiant
`F-34 / SF-34-02`

## Feature parente
`F-34` — Budget tokens mensuel par workspace

## Statut
`ready`

## Date de création
2026-03-22

## Branche Git
`feat/SF-34-02-monthly-budget-admin`

---

## Objectif

Afficher dans la page admin workspace la consommation de tokens du mois courant
vs le budget du plan, avec un indicateur visuel (barre de progression + alerte si > 80 %).

---

## Comportement attendu

### Section "Consommation ce mois" dans `/workspace/admin`

- Tokens consommés ce mois / Budget du plan
- Barre de progression colorée : vert < 60 %, orange 60–80 %, rouge > 80 %
- Si budget = 0 (sans souscription) → afficher "illimité" sans barre

### Cas nominal

- FREE : budget 500 000
- STARTER : budget 3 000 000
- PRO : budget 20 000 000
- Sans souscription : "illimité"

---

## Critères d'acceptation

- [ ] Section "Consommation ce mois" visible dans la page admin
- [ ] Tokens consommés = somme mois courant via `usage_events`
- [ ] Budget correctement affiché selon le plan
- [ ] Barre de progression colorée selon le seuil (vert / orange / rouge)
- [ ] Budget = 0 → affiche "illimité", pas de barre

---

## Périmètre

### Hors scope

- Pas d'email d'alerte
- Pas de consommation par utilisateur ou par dossier (déjà dans la page existante)

---

## Technique

### Endpoint(s)

`GET /api/v1/admin/usage` — extension de la réponse existante (deux champs supplémentaires).
Backwards-compatible : les clients qui ignorent les nouveaux champs ne sont pas impactés.

### Tables impactées

Aucune (lecture `usage_events`).

### Migration Liquibase

- [x] Non applicable

### Composants backend modifiés

- `PlanLimitService` — ajout `getMonthlyTokenBudgetForWorkspace(UUID workspaceId)` (0 = illimité)
- `WorkspaceUsageSummaryResponse` — ajout `monthlyTokensUsed` (long), `monthlyTokensBudget` (long)
- `AdminUsageService` — calcul monthly (startOfMonth UTC), injection `PlanLimitService`

### Composants frontend modifiés

- `workspace-usage-summary.model.ts` — ajout `monthlyTokensUsed`, `monthlyTokensBudget`
- `workspace-admin.component.ts` — `forkJoin` étendu avec `adminUsageService.getSummary()`
- `workspace-admin.component.html` — section "Consommation ce mois" avec barre de progression
- `workspace-admin.component.scss` — styles barre + couleurs seuil

---

## Plan de test

### Tests unitaires

- [ ] U-01 : `AdminUsageService` retourne `monthlyTokensUsed` et `monthlyTokensBudget` corrects
- [ ] U-02 : `PlanLimitService.getMonthlyTokenBudgetForWorkspace()` — PRO → 20 000 000
- [ ] U-03 : sans souscription → 0 (illimité)

### Tests d'intégration

- [ ] Non applicable

### Isolation workspace

- [ ] Requête filtrée par `workspace_id` via JOIN `case_files` (hérité de SF-34-01)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — nouveau getter dans PlanLimitService

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|----------------------|
| `WorkspaceUsageSummaryResponse` | Ajout champs (JSON backwards-compatible) | U-01 |
| `AdminUsageService` | Injection PlanLimitService | U-01 |
| `workspace-admin.component` | Nouveau forkJoin + section UI | Manuel |

---

## Dépendances

- SF-34-01 — statut : done ✓

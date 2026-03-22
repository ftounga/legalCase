# Mini-spec — F-34 / SF-34-01 Budget tokens mensuel — gate pipeline

---

## Identifiant

`F-34 / SF-34-01`

## Feature parente

`F-34` — Budget tokens mensuel par workspace

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-34-01-monthly-token-gate`

---

## Objectif

Bloquer les appels Anthropic si un workspace a dépassé son budget mensuel de tokens,
afin d'éviter les dérapages de coût LLM quelle que soit la taille du dossier.

---

## Comportement attendu

### Budgets par plan (tokens input + output cumulés sur le mois calendaire courant)

| Plan              | Budget mensuel |
|-------------------|---------------|
| FREE              | 500 000       |
| STARTER           | 3 000 000     |
| PRO               | 20 000 000    |
| Sans souscription | illimité (fail open) |

### Points de gate

1. **ChunkAnalysisService** — avant appel `anthropicService.analyzeChunk()` :
   si budget dépassé → skip silencieux (chunk marqué SKIPPED, log WARN, pipeline stoppé naturellement)
2. **ReAnalysisCommandService** — avant publication RabbitMQ :
   si budget dépassé → HTTP 402 "Budget tokens mensuel dépassé."

### Cas nominal

- Budget non dépassé : comportement pipeline inchangé
- Budget dépassé : chunks non traités → triggerDocumentAnalysisIfReady ne se déclenche jamais

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| Workspace PRO, budget mensuel dépassé | Chunk skippé (pipeline stoppé) | N/A (async) |
| Workspace PRO, budget dépassé + re-analyse | Rejet explicite | 402 |
| Sans souscription | Autorisé (fail open) | 200 |

---

## Critères d'acceptation

- [ ] Budget par plan correct (FREE 500K, STARTER 3M, PRO 20M)
- [ ] Calcul via usage_events (sum tokens_input + tokens_output) filtré par workspace + mois courant
- [ ] Chunk skippé silencieusement si budget dépassé (AnalysisStatus.SKIPPED, nouveau statut)
- [ ] Re-analyse bloquée avec 402 si budget dépassé
- [ ] Sans souscription : pass (orElse false)
- [ ] Tous les tests existants passent

---

## Périmètre

### Hors scope

- Pas d'alerte email/notification (SF-34-02)
- Pas de page admin (SF-34-02)
- Pas de reset manuel
- DocumentAnalysisService et CaseAnalysisService non gatés (le pipeline stalle naturellement si les chunks sont SKIPPED)

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Modification interne du pipeline.

### Tables impactées

Aucune modification de schéma.
- Ajout de la valeur `SKIPPED` dans l'enum `AnalysisStatus` (valeur Java uniquement, pas de DDL)
- Lecture de `usage_events` (existante)

### Migration Liquibase

- [x] Non applicable

### Composants backend modifiés

- `AnalysisStatus` — ajout valeur `SKIPPED`
- `UsageEventRepository` — nouvelle query : somme tokens par workspace + mois courant
- `CaseFileRepository` — `findWorkspaceIdById(UUID caseFileId): Optional<UUID>`
- `PlanLimitService` — constantes budget + `isMonthlyTokenBudgetExceeded(UUID workspaceId)`
- `ChunkAnalysisService` — gate avant `analyzeChunk()`
- `ReAnalysisCommandService` — gate avant publication RabbitMQ (HTTP 402)

---

## Plan de test

### Tests unitaires

- [ ] U-01 : `PlanLimitService.isMonthlyTokenBudgetExceeded()` — FREE sous budget → false
- [ ] U-02 : `PlanLimitService.isMonthlyTokenBudgetExceeded()` — FREE au-dessus → true
- [ ] U-03 : PRO sous budget → false
- [ ] U-04 : PRO au-dessus → true
- [ ] U-05 : sans souscription → false (fail open)
- [ ] U-06 : `ReAnalysisCommandService` — budget dépassé → 402
- [ ] U-07 : `ReAnalysisCommandService` — budget non dépassé → RabbitMQ publié (existant mis à jour)

### Tests d'intégration

- [ ] Non applicable — pas de nouvel endpoint

### Isolation workspace

- [ ] Requête filtrée par workspace_id via case_files JOIN — pas de fuite inter-workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — nouveau gate dans ChunkAnalysisService + ReAnalysisCommandService

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `ChunkAnalysisService` | Nouveau skip si budget dépassé | U-01 à U-05 via PlanLimitService |
| `ReAnalysisCommandService` | Nouveau rejet 402 | U-06/U-07 |
| `PlanLimitService` | Nouvelles constantes + méthode | U-01 à U-05 |

### Smoke tests E2E

- [ ] Aucun smoke test concerné — le gate n'est déclenché qu'après dépassement budget

---

## Dépendances

- SF-33-01 — statut : done ✓

---

## Notes et décisions

**Pourquoi SKIPPED dans ChunkAnalysisService et pas FAILED ?**
FAILED implique une erreur technique. SKIPPED est sémantiquement correct : le chunk n'a pas été traité
pour une raison de politique, pas de bug. Le pipeline stalle naturellement sans déclencher d'erreur.

**Pourquoi pas gater DocumentAnalysisService et CaseAnalysisService ?**
Si tous les chunks d'une extraction sont SKIPPED, `triggerDocumentAnalysisIfReady` compare
`totalChunks != doneAnalyses` — comme aucun chunk n'est DONE, le document analysis n'est jamais publié.
Gate unique suffisant.

**Reset mensuel :** mois calendaire courant (du 1er au dernier jour du mois en UTC).
Aucune table supplémentaire nécessaire.

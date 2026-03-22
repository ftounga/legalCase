# Mini-spec — F-33 / SF-33-01 Limite de re-analyses par dossier

---

## Identifiant

`F-33 / SF-33-01`

## Feature parente

`F-33` — Limite de re-analyses par dossier

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-33-01-reanalysis-limit`

---

## Objectif

Limiter le nombre de re-analyses enrichies (ENRICHED_ANALYSIS) par dossier sur le plan PRO à 5, afin de protéger contre les dérapages de coût LLM sur les gros dossiers.

---

## Comportement attendu

### Cas nominal

- Plan PRO : maximum 5 re-analyses par dossier (ENRICHED_ANALYSIS réussies)
- FREE / STARTER : déjà bloqués par le gate existant (`isEnrichedAnalysisAllowed`) — inchangé
- Le compteur s'appuie sur `usage_events` (un enregistrement par ENRICHED_ANALYSIS réussie) — source de vérité fiable
- Si la limite est atteinte : `ReAnalysisCommandService.triggerReAnalysis()` retourne HTTP 402

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| PRO, 5 re-analyses déjà effectuées | Rejet avec message explicite | 402 |
| PRO, moins de 5 re-analyses | Autorisé, comportement inchangé | 200 |
| Workspace sans subscription | Autorisé (orElse true — comportement existant) | 200 |

---

## Critères d'acceptation

- [ ] PRO : 5ème re-analyse autorisée, 6ème rejetée (402)
- [ ] Le compteur utilise `usage_events` avec `event_type = ENRICHED_ANALYSIS` filtrés par `case_file_id`
- [ ] FREE / STARTER : comportement inchangé (bloqués par le gate existant)
- [ ] Message d'erreur 402 : "Limite de re-analyses atteinte pour ce dossier."
- [ ] Tous les tests existants passent

---

## Périmètre

### Hors scope

- Pas de modification frontend (le frontend gère déjà le 402 silencieusement)
- Pas de compteur affiché dans l'UI
- Pas de limite sur FREE/STARTER (déjà bloqués en amont)
- Pas de reset mensuel du compteur

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Modification de `POST /api/v1/case-files/:id/re-analysis` (via `ReAnalysisCommandService`).

### Tables impactées

Aucune modification de schéma. Lecture de `usage_events` (existante).

### Migration Liquibase

- [x] Non applicable

### Composants backend modifiés

- `UsageEventRepository` — ajout `countByCaseFileIdAndEventType(UUID, JobType)`
- `PlanLimitService` — ajout constante `PRO_MAX_RE_ANALYSES_PER_CASE_FILE = 5`, méthode `isReAnalysisLimitReached(UUID caseFileId, UUID workspaceId)`
- `ReAnalysisCommandService` — appel du nouveau gate avant envoi RabbitMQ

---

## Plan de test

### Tests unitaires

- [ ] U-01 : PRO, 4 re-analyses existantes → autorisé
- [ ] U-02 : PRO, 5 re-analyses existantes → 402
- [ ] U-03 : `PlanLimitService.isReAnalysisLimitReached()` — PRO sous limite → false
- [ ] U-04 : `PlanLimitService.isReAnalysisLimitReached()` — PRO à limite → true
- [ ] U-05 : workspace sans subscription → autorisé (orElse false)

### Tests d'intégration

- [ ] Non applicable — pas de nouvel endpoint

### Isolation workspace

- [ ] Non applicable — le compteur est filtré par `case_file_id`, qui appartient au workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — nouveau gate sur `ReAnalysisCommandService`

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `ReAnalysisCommandService` | Nouveau rejet 402 si limite atteinte | U-01/U-02 |
| `PlanLimitService` | Nouvelle méthode, constantes inchangées | U-03/U-04/U-05 |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — le gate n'est déclenché qu'après 5 re-analyses

---

## Dépendances

### Subfeatures bloquantes

- SF-32-01 — statut : done ✓

---

## Notes et décisions

**Pourquoi `usage_events` comme compteur ?**
Chaque ENRICHED_ANALYSIS réussie enregistre exactement un `UsageEvent`. C'est la seule table qui compte les succès sans ambiguïté — `analysis_jobs` est réutilisé à chaque re-analyse, et `case_analyses` ne distingue pas initial vs enrichi.

**Pourquoi 5 ?**
Un avocat sur un dossier de droit du travail répond aux questions une fois, re-analyse, et affine 1-2 fois au maximum dans un usage normal. 5 est généreux sans être illimité.

**Pas de reset mensuel :** La limite est par dossier sur sa durée de vie. Un dossier avec 5 re-analyses est considéré comme stabilisé.

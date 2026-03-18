# Mini-spec — F-15 / SF-15-04 — Frontend — Affichage de la consommation LLM

## Identifiant
`F-15 / SF-15-04`

## Feature parente
`F-15` — Suivi consommation LLM

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-15-04-usage-frontend`

---

## Objectif

Afficher dans la page de détail d'un dossier la liste des événements de consommation LLM (tokens, coût estimé, type d'analyse) en appelant `GET /api/v1/case-files/{id}/usage`.

---

## Comportement attendu

### Cas nominal

- Une section "Consommation LLM" est visible dans la page `case-file-detail`, après la section Synthèse.
- Elle est chargée au montage du composant (`ngOnInit`), pas de déclenchement conditionnel.
- Elle affiche une `mat-table` avec les colonnes : **Type**, **Tokens input**, **Tokens output**, **Coût estimé (€)**, **Date**.
- En bas du tableau : une ligne de **totaux** (somme tokens input, somme tokens output, somme coûts).
- Si la liste est vide : message "Aucun événement de consommation pour ce dossier."
- La colonne **Date** est formatée `DD/MM/YYYY HH:mm`.
- La colonne **Coût estimé** est formatée avec 6 décimales en euros (`€0.000123`).
- La colonne **Type** affiche le `eventType` brut (ex. `CHUNK_ANALYSIS`, `DOCUMENT_ANALYSIS`, `CASE_ANALYSIS`).
- Pagination : 10 événements par page (`mat-paginator`).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Backend répond 403 | Snackbar erreur "Accès non autorisé." (4s) |
| Backend répond 5xx ou timeout | Snackbar erreur "Impossible de charger la consommation." (4s) |

---

## Critères d'acceptation

- [ ] Section "Consommation LLM" affichée dans `case-file-detail` au montage
- [ ] Tableau avec colonnes Type, Tokens input, Tokens output, Coût estimé, Date
- [ ] Ligne de totaux en bas du tableau (hors paginator)
- [ ] Date formatée `DD/MM/YYYY HH:mm`
- [ ] Coût formaté à 6 décimales en euros
- [ ] Message vide si liste vide
- [ ] Pagination 10 éléments/page
- [ ] Snackbar erreur sur 403
- [ ] Snackbar erreur sur 5xx / timeout
- [ ] Tests unitaires couvrant les critères ci-dessus

---

## Périmètre

### Hors scope (explicite)

- Filtrage par type d'événement
- Export CSV/Excel
- Agrégation par période (semaine/mois)
- Affichage dans la page admin F-18 (couvert par F-18)

---

## Technique

### Endpoint consommé

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/usage` | Oui | LAWYER |

**Réponse** : `UsageEventResponse[]` — champs : `id`, `eventType`, `tokensInput`, `tokensOutput`, `estimatedCost`, `createdAt`

### Nouveaux artefacts

| Artefact | Fichier |
|----------|---------|
| `UsageEvent` (interface) | `frontend/src/app/core/models/usage-event.model.ts` |
| `UsageEventService` | `frontend/src/app/core/services/usage-event.service.ts` |

### Composants modifiés

| Composant | Modification |
|-----------|-------------|
| `CaseFileDetailComponent` | + signal `usageEvents`, + computed `usageTotals`, + `loadUsage()` appelé au `ngOnInit` |
| Template | + section "Consommation LLM" avec mat-table et mat-paginator |
| SCSS | + styles `.usage-section`, `.totals-row` |

### Migration Liquibase

- [x] Non applicable (pas de nouvelle table)

---

## Plan de test

### Tests unitaires

- [ ] `UsageEventService.getUsageEvents()` — appel HTTP correct avec `caseFileId`
- [ ] Section vide si `usageEvents()` retourne `[]`
- [ ] Tableau affiché si `usageEvents()` non vide
- [ ] Totaux calculés correctement via signal computed
- [ ] Snackbar erreur affichée sur HTTP 403
- [ ] Snackbar erreur affichée sur HTTP 500

### Isolation workspace

- [ ] Applicable — l'API backend filtre par `workspace_id` du token (déjà sécurisé en SF-15-03)

---

## Dépendances

### Subfeatures bloquantes

- SF-15-03 — statut : done

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Chargement au `ngOnInit`, pas conditionné à l'état des jobs — la consommation est visible même si l'analyse est en cours.
- Totaux calculés côté frontend via `computed()` Angular signal — pas de nouvel endpoint agrégat.
- `estimatedCost` est un `BigDecimal` JSON sérialisé en `number` côté Angular — formatage via `DecimalPipe`.
- `eventType` affiché brut (pas de traduction en V1).

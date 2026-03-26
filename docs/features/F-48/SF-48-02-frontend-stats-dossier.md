# Mini-spec — F-48 / SF-48-02 — Frontend bloc métriques dossier

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-48 / SF-48-02`

## Feature parente

`F-48` — Tableau de bord dossier

## Statut

`ready`

## Date de création

2026-03-26

## Branche Git

`feat/SF-48-02-frontend-stats-dossier`

---

## Objectif

Afficher un bloc de 3 métriques (documents, analyses, tokens) dans la page dossier (`case-file-detail`), alimenté par `GET /api/v1/case-files/{id}/stats`.

---

## Comportement attendu

### Cas nominal

Lors du chargement de la page dossier (`/case-files/:id`), l'appel à `/stats` est déclenché en parallèle des autres appels existants. Un bloc "Métriques" apparaît avec :

- **Documents** : valeur numérique entière (ex. `3`)
- **Analyses terminées** : valeur numérique entière (ex. `2`)
- **Tokens consommés** : valeur formatée avec séparateur de milliers (ex. `12 540`)

Le bloc est visible immédiatement au chargement (pas besoin d'interagir). Les métriques se rafraîchissent automatiquement quand un événement SSE de fin d'analyse arrive pour ce dossier (le composant appelle déjà `loadAnalysisJobs` + `loadSynthesis` sur événement SSE — on ajoute `loadStats` au même endroit).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur réseau sur `/stats` | Bloc non affiché (silencieux, pas de toast) |
| Stats à zéro (nouveau dossier) | Bloc affiché avec valeurs 0 |

---

## Critères d'acceptation

- [ ] Le bloc métriques apparaît dans la page dossier avec `documentCount`, `analysisCount`, `totalTokens`
- [ ] `totalTokens` est formaté avec séparateur de milliers (pipe `number` Angular : `'1.0-0'`)
- [ ] Le bloc se rafraîchit après un événement SSE DONE (call `loadStats`)
- [ ] En cas d'erreur `/stats` : bloc absent, pas de toast d'erreur
- [ ] Le bloc respecte le design system : `mat-card`, couleurs palette, polices Inter/JetBrains Mono pour les valeurs

---

## Périmètre

### Hors scope (explicite)

- Coût en euros
- Durée d'analyse
- Affichage dans la page Administration (F-18)
- Graphiques ou historiques

---

## Technique

### Nouveau service Angular

`CaseFileStatsService` (`core/services/case-file-stats.service.ts`) :
```typescript
getStats(caseFileId: string): Observable<CaseFileStats>
// GET /api/v1/case-files/{id}/stats
```

### Nouveau modèle

`CaseFileStats` (`core/models/case-file-stats.model.ts`) :
```typescript
export interface CaseFileStats {
  documentCount: number;
  analysisCount: number;
  totalTokens: number;
}
```

### Composant modifié

`case-file-detail.component.ts` :
- Ajoute signal `stats = signal<CaseFileStats | null>(null)`
- Appelle `loadStats(id)` dans `ngOnInit`
- Appelle `loadStats(id)` dans le handler SSE DONE (à côté de `loadAnalysisJobs`)
- Injecte `CaseFileStatsService`

`case-file-detail.component.html` :
- Nouveau bloc `mat-card` "Métriques" avec 3 valeurs

`case-file-detail.component.scss` :
- Styles du bloc métriques (grille 3 colonnes, valeur en JetBrains Mono)

### Endpoint consommé

| Méthode | URL |
|---------|-----|
| GET | `/api/v1/case-files/{id}/stats` |

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `CaseFileDetailComponent` — ajout signal `stats`, appel service, affichage HTML
- `CaseFileStatsService` — nouveau service HTTP

---

## Plan de test

### Tests unitaires

- [ ] `CaseFileStatsService` — `getStats` appelle `GET /api/v1/case-files/{id}/stats` (spec Karma existante à étendre ou nouveau fichier)

### Tests d'intégration

Non applicable (rendu HTML — couvert par les tests unitaires du service + vérification visuelle).

### Isolation workspace

- [x] Non applicable — le backend garantit l'isolation, le frontend consomme uniquement l'endpoint.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout d'un bloc dans un composant existant, pas de modification de routing, auth ou plans.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné.

---

## Dépendances

### Subfeatures bloquantes

- SF-48-01 — statut : **done** (mergée 2026-03-26)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Formatage `totalTokens` : pipe Angular `number:'1.0-0'` avec locale `fr` (séparateur espace)
- Rafraîchissement SSE : `loadStats` appelé dans le handler `events$` du `GlobalAnalysisNotificationService` quand `status === 'DONE'`
- Pas de skeleton loader — les valeurs apparaissent quand la réponse arrive, rien avant

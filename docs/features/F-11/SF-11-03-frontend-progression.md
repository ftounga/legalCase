# Mini-spec — F-11 / SF-11-03 — Frontend — Affichage progression Analyse IA

## Identifiant
`F-11 / SF-11-03`

## Feature parente
`F-11` — Suivi des jobs asynchrones

## Statut
`done`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-11-03-frontend-progression`

---

## Objectif

Afficher la progression des analyses IA dans la page de détail d'un dossier (`case-file-detail`). L'affichage se met à jour automatiquement tant que des jobs sont en cours (polling toutes les 5 secondes).

---

## Comportement attendu

### Cas nominal

- La section "Analyse IA" apparaît uniquement si au moins un job existe pour le dossier.
- Chaque job est affiché sur une ligne avec : libellé (jobType), badge de statut, barre de progression (`mat-progress-bar`), pourcentage.
- Le polling démarre automatiquement si au moins un job est `PENDING` ou `PROCESSING`.
- Le polling s'arrête dès que tous les jobs sont `DONE` ou `FAILED`.
- Le polling s'arrête dans `ngOnDestroy` pour éviter les fuites mémoire.
- Après un upload réussi, les jobs sont rechargés.

### Cas d'erreur

- Si l'appel `GET /api/v1/case-files/{id}/analysis-jobs` échoue → section absente (silencieux, pas de snackbar).

---

## Critères d'acceptation

- [x] Section "Analyse IA" absente si aucun job (`analysisJobs().length === 0`)
- [x] Section "Analyse IA" présente si jobs non vides
- [x] Libellé correct par jobType : CHUNK_ANALYSIS → "Analyse des segments", DOCUMENT_ANALYSIS → "Analyse des documents", CASE_ANALYSIS → "Synthèse du dossier"
- [x] Badge correct par statut : DONE/PROCESSING → `badge--success`, PENDING → `badge--pending`, FAILED → `badge--error`
- [x] `mat-progress-bar [value]="job.progressPercentage"` en mode `determinate`
- [x] Polling toutes les 5s si PENDING ou PROCESSING
- [x] Polling stoppé quand tous DONE/FAILED
- [x] `ngOnDestroy` stoppe le polling
- [x] Upload réussi déclenche un rechargement des jobs
- [x] Tests unitaires couvrant les critères ci-dessus

---

## Périmètre

### Hors scope (explicite)

- SSE/WebSocket (hors V1)
- Affichage du détail des analyses (F-12)
- Gestion des erreurs avec snackbar pour les jobs (silencieux)

---

## Technique

### Composants Angular

| Composant | Fichier |
|-----------|---------|
| `CaseFileDetailComponent` | `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` |
| Template | `case-file-detail.component.html` |
| Styles | `case-file-detail.component.scss` |
| Spec | `case-file-detail.component.spec.ts` |

### Nouveaux services/modèles

| Artefact | Fichier |
|----------|---------|
| `AnalysisJobService` | `frontend/src/app/core/services/analysis-job.service.ts` |
| `AnalysisJob` (interface) | `frontend/src/app/core/models/analysis-job.model.ts` |

### Imports Angular Material ajoutés

- `MatProgressBarModule`

---

## Plan de test

### Tests unitaires

- [x] Section Analyse IA absente si `analysisJobs()` vide
- [x] Section Analyse IA présente si jobs non vides (3 lignes affichées)
- [x] `jobTypeLabel()` — libellé correct pour chaque valeur de jobType
- [x] `jobStatusClass()` — classe CSS correcte pour chaque statut

---

## Dépendances

### Subfeatures bloquantes
- SF-11-02 — statut : done

---

## Notes et décisions

- Polling 5s (pas SSE) — conforme à la décision V1 (hors scope dans SF-11-02)
- Erreur silencieuse sur `getJobs` — pas de snackbar, la section est simplement absente
- `ngOnDestroy` obligatoire pour éviter les appels HTTP en background après navigation
- Tri des jobs géré côté serveur (SF-11-02) — le frontend affiche dans l'ordre reçu

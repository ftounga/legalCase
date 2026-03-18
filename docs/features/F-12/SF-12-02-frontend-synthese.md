# Mini-spec — F-12 / SF-12-02 — Frontend — Affichage de la synthèse

## Identifiant
`F-12 / SF-12-02`

## Feature parente
`F-12` — Restitution de l'analyse

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-12-02-frontend-synthese`

---

## Objectif

Afficher la synthèse globale du dossier dans la page de détail (`case-file-detail`) une fois le job `CASE_ANALYSIS` terminé.

---

## Comportement attendu

### Cas nominal

- La section "Synthèse" apparaît uniquement si le job `CASE_ANALYSIS` est `DONE` (signal `analysisJobs` existant).
- Quand le job passe à `DONE`, le composant appelle `GET /api/v1/case-files/{id}/case-analysis` et affiche le résultat.
- La synthèse est structurée en 5 sous-sections :
  - **Timeline** — frise chronologique des événements (date + libellé), masquée si vide
  - **Faits** — liste à puces, masquée si vide
  - **Points juridiques** — liste à puces, masquée si vide
  - **Risques** — liste à puces, masquée si vide
  - **Questions ouvertes** — liste à puces, masquée si vide

### Déclenchement du chargement

- Lors de l'arrêt du polling (tous les jobs DONE/FAILED), si `CASE_ANALYSIS` est `DONE` → charger la synthèse automatiquement.
- Pas de bouton manuel.

### Cas d'erreur

- Si l'appel `GET /api/v1/case-files/{id}/case-analysis` échoue → section "Synthèse" absente (silencieux).

---

## Critères d'acceptation

- [x] Section "Synthèse" absente si CASE_ANALYSIS n'est pas DONE
- [x] Section "Synthèse" présente si CASE_ANALYSIS est DONE et synthèse chargée
- [x] Timeline affichée avec date et libellé d'événement
- [x] Timeline masquée si tableau vide
- [x] Faits, points juridiques, risques, questions ouvertes affichés en liste à puces
- [x] Sous-section masquée si liste vide
- [x] Chargement déclenché automatiquement à la fin du polling quand CASE_ANALYSIS DONE
- [x] Erreur silencieuse (pas de snackbar)
- [x] Tests unitaires couvrant les critères ci-dessus

---

## Périmètre

### Hors scope (explicite)

- Bouton de relance de l'analyse
- Affichage des analyses par document (niveau `document_analyses`)
- Export PDF de la synthèse (hors V1)

---

## Technique

### Nouveaux artefacts

| Artefact | Fichier |
|----------|---------|
| `CaseAnalysisResult` (interface) | `frontend/src/app/core/models/case-analysis.model.ts` |
| `CaseAnalysisService` | `frontend/src/app/core/services/case-analysis.service.ts` |

### Composants modifiés

| Composant | Modification |
|-----------|-------------|
| `CaseFileDetailComponent` | + signal `synthesis`, + `loadSynthesis()`, + déclenchement depuis `managePolling` |
| Template | + section "Synthèse" conditionnelle |
| SCSS | + styles `.synthesis-card`, `.synthesis-section`, `.timeline-entry` |

---

## Plan de test

### Tests unitaires

- [x] Section Synthèse absente si `synthesis()` est null
- [x] Section Synthèse présente si `synthesis()` non null
- [x] Timeline masquée si `timeline` vide
- [x] Sous-section masquée si liste vide (ex: faits vides)

---

## Dépendances

### Subfeatures bloquantes
- SF-12-01 — statut : done

---

## Notes et décisions

- Déclenchement au `stopPolling()` uniquement si CASE_ANALYSIS DONE — pas de polling sur la synthèse
- Erreur silencieuse : la section est simplement absente si le backend répond 404
- Séparation des sections par `<mat-divider>` pour la lisibilité

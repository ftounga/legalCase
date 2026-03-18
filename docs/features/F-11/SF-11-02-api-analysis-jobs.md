# Mini-spec — F-11 / SF-11-02 — API REST statut des jobs

## Identifiant
`F-11 / SF-11-02`

## Feature parente
`F-11` — Suivi des jobs asynchrones

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-11-02-api-analysis-jobs`

---

## Objectif

Exposer un endpoint REST permettant au frontend de consulter l'état d'avancement des analyses d'un dossier (statut et pourcentage par niveau).

---

## Comportement attendu

### Cas nominal

`GET /api/v1/case-files/{caseFileId}/analysis-jobs`

Retourne la liste des jobs d'analyse pour le dossier, triée par ordre naturel des niveaux (CHUNK_ANALYSIS → DOCUMENT_ANALYSIS → CASE_ANALYSIS).

Réponse 200 :
```json
[
  {
    "jobType": "CHUNK_ANALYSIS",
    "status": "DONE",
    "totalItems": 12,
    "processedItems": 12,
    "progressPercentage": 100
  },
  {
    "jobType": "DOCUMENT_ANALYSIS",
    "status": "PROCESSING",
    "totalItems": 3,
    "processedItems": 1,
    "progressPercentage": 33
  },
  {
    "jobType": "CASE_ANALYSIS",
    "status": "PENDING",
    "totalItems": 1,
    "processedItems": 0,
    "progressPercentage": 0
  }
]
```

Si aucun job n'existe encore (pipeline pas démarré) → réponse 200 avec liste vide `[]`.

`progressPercentage` = `floor(processedItems * 100 / totalItems)`. Si `totalItems == 0` → `progressPercentage = 0`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `caseFileId` inexistant | 404 Not Found | 404 |
| Dossier appartenant à un autre workspace | 404 (isolation, pas de fuite d'information) | 404 |
| Utilisateur non authentifié | 401 Unauthorized | 401 |

---

## Critères d'acceptation

- [x] `GET /api/v1/case-files/{caseFileId}/analysis-jobs` retourne 200 avec la liste des jobs
- [x] Liste vide si aucun job existant pour le dossier
- [x] `progressPercentage` calculé côté serveur (`floor(processedItems * 100 / totalItems)`)
- [x] Jobs triés dans l'ordre CHUNK_ANALYSIS → DOCUMENT_ANALYSIS → CASE_ANALYSIS
- [x] 404 si le dossier n'existe pas
- [x] 404 si le dossier appartient à un autre workspace (isolation)
- [x] 401 si non authentifié
- [x] Tests d'intégration couvrant les cas ci-dessus

---

## Périmètre

### Hors scope (explicite)

- Mise à jour temps réel (SSE/WebSocket — hors V1)
- Endpoint de modification des jobs
- Affichage frontend (SF-11-03)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/analysis-jobs` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `analysis_jobs` | SELECT | Filtre par case_file_id |
| `case_files` | SELECT | Vérification existence + workspace |

### Migration Liquibase
Non applicable.

### Composants Angular (si applicable)
Aucun dans cette subfeature.

---

## Plan de test

### Tests unitaires
- [x] `AnalysisJobResponse.from()` — calcul progressPercentage nominal (floor 33%) et totalItems=0 → 0

### Tests d'intégration

- [x] `GET /api/v1/case-files/{id}/analysis-jobs` → 200 avec liste de jobs ordonnée et progressPercentage calculé
- [x] `GET /api/v1/case-files/{id}/analysis-jobs` → 200 liste vide si aucun job
- [x] `GET /api/v1/case-files/{unknown}/analysis-jobs` → 404
- [x] `GET /api/v1/case-files/{autreWorkspace}/analysis-jobs` → 404 (isolation workspace)
- [x] `GET /api/v1/case-files/{id}/analysis-jobs` sans auth → 401

### Isolation workspace
- [x] Testée — un utilisateur du workspace A ne peut pas voir les jobs du workspace B

---

## Dépendances

### Subfeatures bloquantes
- SF-11-01 — statut : done

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- `progressPercentage` calculé serveur (pas client) pour éviter la logique de division dans le frontend
- Isolation workspace : retourne 404 (pas 403) pour ne pas révéler l'existence du dossier — pattern identique à `CaseFileService.getById`
- Tri par `jobType.ordinal()` — l'ordre des valeurs dans l'enum `JobType` définit l'ordre d'affichage

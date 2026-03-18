# Mini-spec — F-11 / SF-11-01 — Infrastructure analysis_jobs

## Identifiant
`F-11 / SF-11-01`

## Feature parente
`F-11` — Suivi des jobs asynchrones

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-11-01-infra-analysis-jobs`

---

## Objectif

Créer la table `analysis_jobs` et l'intégrer dans les services du pipeline IA pour tracer en temps réel l'avancement des analyses par niveau (chunk → document → dossier).

---

## Comportement attendu

### Cas nominal

Un job `analysis_jobs` est créé ou réinitialisé à chaque déclenchement d'un niveau du pipeline :

| Étape pipeline | Acteur | Action sur analysis_jobs |
|---------------|--------|--------------------------|
| Après chunking | `ChunkingService` | Crée un job CHUNK_ANALYSIS (total = nb chunks, processed = 0, status = PENDING) |
| Après chaque chunk DONE | `ChunkAnalysisService` | Incrémente `processed_items`; si `processed == total` → status = DONE |
| Déclenchement document analysis | `DocumentAnalysisService` | Crée un job DOCUMENT_ANALYSIS (total = nb documents, processed = 0, status = PENDING) |
| Après chaque document DONE | `DocumentAnalysisService` | Incrémente `processed_items`; si `processed == total` → status = DONE |
| Déclenchement case analysis | `DocumentAnalysisService.triggerCaseAnalysisIfReady` | Crée un job CASE_ANALYSIS (total = 1, processed = 0, status = PENDING) |
| CaseAnalysis DONE | `CaseAnalysisService` | `processed_items = 1`, status = DONE |
| CaseAnalysis FAILED | `CaseAnalysisService` | status = FAILED, `error_message` renseigné |

Unicité : une seule ligne par `(case_file_id, job_type)`. Si le job existe déjà, il est réinitialisé (upsert).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Échec dans ChunkAnalysisService | status du job reste PENDING ou devient FAILED si toutes les tentatives ont échoué |
| Échec dans CaseAnalysisService | status = FAILED |

---

## Critères d'acceptation

- [ ] Table `analysis_jobs` créée via migration Liquibase
- [ ] Entité JPA `AnalysisJob` avec enum `JobType` (CHUNK_ANALYSIS, DOCUMENT_ANALYSIS, CASE_ANALYSIS)
- [ ] `AnalysisJobRepository` avec méthode `findByCaseFileId` et upsert via `findByCaseFileIdAndJobType`
- [ ] `ChunkingService` crée le job CHUNK_ANALYSIS après publication des messages
- [ ] `ChunkAnalysisService` incrémente `processed_items` après chaque DONE et passe à DONE quand `processed == total`
- [ ] `DocumentAnalysisService` crée le job DOCUMENT_ANALYSIS au déclenchement et incrémente après chaque DONE
- [ ] `DocumentAnalysisService.triggerCaseAnalysisIfReady` crée le job CASE_ANALYSIS avant de publier le message
- [ ] `CaseAnalysisService` met à jour le job à DONE ou FAILED selon le résultat
- [ ] Tests unitaires couvrent création, incrémentation et fin de job pour chaque niveau
- [ ] Aucune régression sur les tests existants (F-08, F-09, F-10)

---

## Périmètre

### Hors scope (explicite)

- Endpoint REST pour lire les jobs (SF-11-02)
- Affichage frontend (SF-11-03)
- Notifications temps réel (SSE/WebSocket — hors V1)
- Gestion de la reprise en cas de crash partiel

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| status | PENDING | Toujours PENDING à la création |
| processed_items | 0 | Initialisé à 0 |
| total_items | N | Nombre total d'items à traiter |
| error_message | null | Renseigné uniquement en cas de FAILED |

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées |
|-------|-------------|-------------------|
| job_type | Oui | CHUNK_ANALYSIS, DOCUMENT_ANALYSIS, CASE_ANALYSIS |
| status | Oui | PENDING, PROCESSING, DONE, FAILED |
| total_items | Oui | ≥ 1 |
| processed_items | Oui | 0 ≤ processed_items ≤ total_items |

---

## Technique

### Endpoint(s)
Aucun dans cette subfeature.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `analysis_jobs` | INSERT / UPDATE | Upsert sur (case_file_id, job_type) |
| `chunk_analyses` | SELECT (count) | Pour calculer processed_items CHUNK |
| `document_analyses` | SELECT (count) | Pour calculer processed_items DOCUMENT |

### Migration Liquibase
- [x] Oui — `013-create-analysis-jobs.xml`

### Composants Angular
Aucun dans cette subfeature.

---

## Plan de test

### Tests unitaires

- [ ] `ChunkingService` — crée job CHUNK_ANALYSIS avec total = nb chunks après publication
- [ ] `ChunkAnalysisService` — incrémente processed_items après DONE
- [ ] `ChunkAnalysisService` — passe status = DONE quand processed == total
- [ ] `DocumentAnalysisService` — crée job DOCUMENT_ANALYSIS au déclenchement
- [ ] `DocumentAnalysisService` — incrémente processed_items après DONE document
- [ ] `DocumentAnalysisService.triggerCaseAnalysisIfReady` — crée job CASE_ANALYSIS avant publication
- [ ] `CaseAnalysisService` — status = DONE après résultat OK
- [ ] `CaseAnalysisService` — status = FAILED après erreur Anthropic

### Tests d'intégration
Non applicable dans cette subfeature (pas d'endpoint).

### Isolation workspace
Non applicable directement — la FK `case_file_id → case_files` garantit l'isolation par chaîne.

---

## Dépendances

### Subfeatures bloquantes
- SF-08-01, SF-08-02, SF-08-03 — statut : done
- SF-09-01, SF-09-02 — statut : done
- SF-10-01, SF-10-02 — statut : done

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- Upsert choisi plutôt qu'insert strict pour permettre la ré-exécution du pipeline sans nettoyer manuellement les jobs
- `AnalysisStatus` existant (PENDING/PROCESSING/DONE/FAILED) est réutilisé comme type de statut pour le job
- `processed_items` est recalculé via un count en base plutôt que par incrément mémoire pour garantir la cohérence en cas de retry

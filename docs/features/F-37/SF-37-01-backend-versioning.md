# Mini-spec — F-37 / SF-37-01 Backend versioning des synthèses

---

## Identifiant

`F-37 / SF-37-01`

## Feature parente

`F-37` — Versioning des synthèses

## Statut

`in-progress`

## Date de création

2026-03-23

## Branche Git

`feat/SF-37-01-backend-versioning`

---

## Objectif

Associer un numéro de version et un type (STANDARD / ENRICHED) à chaque enregistrement `CaseAnalysis`, lier chaque `AiQuestion` à sa `CaseAnalysis`, et exposer des endpoints permettant de lister les versions et de récupérer une version précise.

---

## Comportement attendu

### Cas nominal

1. Chaque déclenchement d'analyse (STANDARD ou ENRICHED) crée un nouvel enregistrement `CaseAnalysis` avec `version = max_version_existante + 1` pour ce dossier, et `analysis_type` correspondant.
2. `GET /api/v1/case-files/{id}/case-analysis` retourne toujours la dernière version DONE (comportement inchangé — rétrocompatibilité).
3. `GET /api/v1/case-files/{id}/case-analysis/versions` retourne la liste de toutes les versions DONE triées par version décroissante, avec `id`, `version`, `analysisType`, `updatedAt`.
4. `GET /api/v1/case-files/{id}/case-analysis/versions/{version}` retourne la synthèse complète d'une version précise.
5. Les questions IA générées pour une analyse sont liées à leur `CaseAnalysis` via `case_analysis_id`.
6. `GET /api/v1/case-files/{id}/ai-questions?analysisId={uuid}` retourne les questions d'une version précise.
7. `GET /api/v1/case-files/{id}/ai-questions` sans paramètre retourne les questions de la dernière version DONE (rétrocompatibilité).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Version demandée inexistante | 404 NOT_FOUND |
| `analysisId` inconnu ou hors workspace | 404 NOT_FOUND |
| Aucune version DONE pour le dossier | `GET /versions` retourne liste vide (200) |

---

## Critères d'acceptation

- [ ] Colonne `version` (INT, NOT NULL) sur `case_analyses` — auto-incrémentée par dossier
- [ ] Colonne `analysis_type` (VARCHAR(20), NOT NULL, valeurs : STANDARD / ENRICHED) sur `case_analyses`
- [ ] Migration Liquibase : rows existantes → `version=1`, `analysis_type='STANDARD'`
- [ ] Colonne `case_analysis_id` (UUID, nullable, FK → case_analyses.id) sur `ai_questions`
- [ ] Migration Liquibase : questions existantes liées à la dernière CaseAnalysis DONE de leur dossier (NULL si aucune)
- [ ] `CaseAnalysisService` calcule le prochain numéro de version au moment de la création
- [ ] `EnrichedAnalysisService` idem avec `analysis_type = ENRICHED`
- [ ] `AiQuestionService` peuple `case_analysis_id` lors de la génération des questions
- [ ] `GET /case-analysis` (existant) inchangé fonctionnellement
- [ ] `GET /case-analysis/versions` liste toutes les versions DONE
- [ ] `GET /case-analysis/versions/{version}` retourne la synthèse complète de la version
- [ ] `GET /ai-questions?analysisId={uuid}` filtre par version
- [ ] `GET /ai-questions` sans paramètre retourne les questions de la dernière version DONE
- [ ] Isolation workspace vérifiée sur tous les nouveaux endpoints
- [ ] Tests unitaires passants

---

## Périmètre

### Hors scope

- Modification du frontend (SF-37-02)
- Suppression ou archivage des anciennes versions
- Pagination des versions (max ~10 versions par dossier en V1)
- Endpoint de réponse aux questions par version (existant — inchangé)

---

## Technique

### Endpoints

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/v1/case-files/{id}/case-analysis` | Dernière synthèse DONE (inchangé) |
| GET | `/api/v1/case-files/{id}/case-analysis/versions` | Liste des versions DONE |
| GET | `/api/v1/case-files/{id}/case-analysis/versions/{version}` | Synthèse complète d'une version |
| GET | `/api/v1/case-files/{id}/ai-questions` | Questions dernière version (inchangé) |
| GET | `/api/v1/case-files/{id}/ai-questions?analysisId={uuid}` | Questions d'une version précise |

### Tables impactées

| Table | Modification |
|-------|-------------|
| `case_analyses` | +`version` INT NOT NULL, +`analysis_type` VARCHAR(20) NOT NULL |
| `ai_questions` | +`case_analysis_id` UUID nullable FK → case_analyses.id ON DELETE SET NULL |

### Migration Liquibase

- [x] Applicable — fichier `026-case-analysis-versioning.xml`
- Backfill `case_analyses` : `version = 1`, `analysis_type = 'STANDARD'` pour toutes les lignes existantes
- Backfill `ai_questions` : `case_analysis_id` = dernière `CaseAnalysis` DONE du même `case_file_id` (sous-requête), NULL si aucune

### Composants Java impactés

- `CaseAnalysis.java` — +champs `version`, `analysisType`
- `CaseAnalysisService.java` — calcul `version = SELECT MAX(version) + 1 FROM case_analyses WHERE case_file_id = ?`
- `EnrichedAnalysisService.java` — idem + `analysisType = ENRICHED`
- `AiQuestionService.java` — peuple `case_analysis_id` sur chaque question créée
- `CaseAnalysisResponse.java` — +champs `version`, `analysisType`
- `CaseAnalysisVersionSummary.java` — nouveau record (id, version, analysisType, updatedAt)
- `CaseAnalysisQueryService.java` — nouvelles méthodes `listVersions`, `getByVersion`
- `CaseAnalysisReadController.java` — nouvelles routes versions
- `AiQuestionQueryService.java` — filtre optionnel `analysisId`
- `AiQuestionController.java` — param `analysisId` optionnel
- `AiQuestionRepository.java` — nouvelle query par `case_analysis_id`
- `CaseAnalysisRepository.java` — nouvelles queries

---

## Plan de test

### Tests unitaires (JUnit/Mockito)

- [ ] `CaseAnalysisService` : version = 1 si aucun enregistrement existant
- [ ] `CaseAnalysisService` : version = max + 1 si enregistrements existants
- [ ] `CaseAnalysisQueryService.listVersions` : retourne liste triée desc, filtre DONE uniquement
- [ ] `CaseAnalysisQueryService.getByVersion` : 404 si version inexistante
- [ ] `AiQuestionQueryService` : avec `analysisId` → filtre correct ; sans → dernière version

### Tests d'intégration

- [ ] Non applicable (logique applicative pure)

### Isolation workspace

- [ ] `GET /versions` vérifie que le dossier appartient au workspace de l'utilisateur
- [ ] `GET /versions/{version}` idem
- [ ] `GET /ai-questions?analysisId=` idem

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouveaux champs additifs, rétrocompatibilité garantie sur les endpoints existants

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de modification de routing ni de guard

---

## Dépendances

### Subfeatures bloquantes

- SF-36-01 — statut : done
- SF-36-02 — statut : done
- SF-36-03 — statut : done

### Questions ouvertes impactées

- [ ] Aucune

---

## Notes et décisions

- `case_analysis_id` sur `ai_questions` est nullable (ON DELETE SET NULL) pour éviter une cascade destructive si une analyse est supprimée à l'avenir.
- Le calcul de version se fait en Java (`SELECT MAX + 1`) sans contrainte unique base pour simplifier (les analyses concurrentes sur un même dossier sont impossibles grâce au gate 409).
- `GET /ai-questions` sans paramètre résout la "dernière version" en une sous-requête sur `case_analyses` : aucun changement visible pour le frontend existant.

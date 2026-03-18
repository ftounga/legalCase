# Mini-spec — F-13 / SF-13-01 — Infrastructure ai_questions + génération async

## Identifiant
`F-13 / SF-13-01`

## Feature parente
`F-13` — Questions IA interactives

## Statut
`in-progress`

## Date de création
`2026-03-18`

## Branche Git
`feat/SF-13-01-infra-ai-questions`

---

## Objectif

Créer la table `ai_questions`, les entités associées, et déclencher automatiquement la génération de questions complémentaires par le LLM après la fin de `CASE_ANALYSIS`.

---

## Comportement attendu

### Déclenchement

Après `CASE_ANALYSIS` DONE dans `CaseAnalysisService` :
→ publication d'un message `AiQuestionGenerationMessage(caseFileId)` sur la queue `ai.question.generation`

### Génération

`AiQuestionService.consumeQuestionGeneration(caseFileId)` :
1. Charge la dernière `CaseAnalysis` DONE du dossier
2. Met à jour le job `QUESTION_GENERATION` → PROCESSING
3. Appelle le LLM avec le prompt de génération de questions
4. Parse la réponse JSON `{"questions": ["Q1 ?", "Q2 ?", ...]}`
5. Persiste chaque question dans `ai_questions` (order_index 0, 1, 2...)
6. Met à jour le job → DONE (processedItems = nombre de questions)

Format LLM attendu :
```json
{"questions": ["Question 1 ?", "Question 2 ?", "Question 3 ?"]}
```

Entre 3 et 8 questions.

### Job de suivi

`JobType` étendu avec `QUESTION_GENERATION` (après `CASE_ANALYSIS` dans l'enum — respecte l'ordre ordinal).

Suivi via `analysis_jobs` existant : 1 ligne `(caseFileId, QUESTION_GENERATION)`, totalItems = 1, processedItems = 1 quand DONE.

---

## Critères d'acceptation

- [x] Table `ai_questions` créée via Liquibase
- [x] Entité `AiQuestion` + repository
- [x] `JobType.QUESTION_GENERATION` ajouté à l'enum
- [x] `CaseAnalysisService` publie sur la queue après CASE_ANALYSIS DONE
- [x] `AiQuestionService` génère et persiste les questions
- [x] Job `QUESTION_GENERATION` suivi via `analysis_jobs`
- [x] En cas d'erreur LLM → job FAILED, aucune question persistée
- [x] Tests unitaires couvrant la génération et le cas d'erreur

---

## Périmètre

### Hors scope (explicite)

- API REST de consultation des questions (SF-13-02)
- Affichage frontend (SF-13-03)
- Réponses de l'avocat (F-14)

---

## Technique

### Migration Liquibase

`014-create-ai-questions.xml`

```sql
ai_questions
├── id             UUID PK
├── case_file_id   UUID FK → case_files NOT NULL
├── question_text  TEXT NOT NULL
├── order_index    INT NOT NULL
├── status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
├── created_at     TIMESTAMP WITH TIME ZONE NOT NULL
└── updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
```

Index sur `case_file_id`.

### Nouveaux composants

| Composant | Rôle |
|-----------|------|
| `AiQuestion` | Entité JPA |
| `AiQuestionRepository` | Spring Data |
| `AiQuestionGenerationMessage` | Message RabbitMQ |
| `AiQuestionService` | Listener + génération |
| `RabbitMQConfig` | +queue `ai.question.generation` |
| `JobType.QUESTION_GENERATION` | Nouvel enum value |

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `JobType.java` | +QUESTION_GENERATION |
| `RabbitMQConfig.java` | +queue/exchange/binding ai.question.generation |
| `CaseAnalysisService.java` | Publie message après DONE |

---

## Plan de test

### Tests unitaires

- [x] Génération nominale → N questions persistées, job DONE
- [x] Erreur LLM → job FAILED, aucune question
- [x] Pas de CaseAnalysis DONE → skip (log warn)
- [x] CaseAnalysisService : publie le message après DONE

---

## Dépendances

### Subfeatures bloquantes
- SF-12-01 — statut : done (CaseAnalysis disponible)

### Débloque
- F-13 / SF-13-02 (API REST)

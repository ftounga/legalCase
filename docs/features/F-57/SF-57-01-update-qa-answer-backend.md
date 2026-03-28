# Mini-spec — F-57 / SF-57-01 : Backend modification réponse Q&A

---

## Identifiant

`F-57 / SF-57-01`

## Feature parente

`F-57` — Modification des réponses Q&A

## Statut

`in-progress`

## Date de création

2026-03-28

## Branche Git

`feat/SF-57-01-update-qa-answer-backend`

---

## Objectif

Vérifier et couvrir par des tests que le endpoint `POST /api/v1/ai-questions/{questionId}/answer` accepte de ré-répondre à une question déjà ANSWERED, en créant une nouvelle entrée `AiQuestionAnswer` (historique conservé).

---

## Comportement attendu

### Cas nominal

L'avocat appelle `POST /api/v1/ai-questions/{questionId}/answer` sur une question déjà `ANSWERED`.
Une nouvelle entrée est créée dans `ai_question_answers` avec le nouveau texte et un `created_at` récent.
La question reste `ANSWERED`, `answered_at` est mis à jour à now().
`findFirstByAiQuestionIdOrderByCreatedAtDesc` retourne la nouvelle réponse.
Le garde SF-56-05 (`existsByAiQuestion_CaseFile_IdAndCreatedAtAfter`) détecte la nouvelle entrée.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `answerText` vide ou absent | 400 | 400 |
| Question inexistante | 404 | 404 |
| Question appartenant à un autre workspace | 404 | 404 |

---

## Critères d'acceptation

- [ ] POST sur une question déjà ANSWERED → 201, nouvelle ligne créée dans `ai_question_answers`
- [ ] `findFirstByAiQuestionIdOrderByCreatedAtDesc` retourne la nouvelle réponse (pas l'ancienne)
- [ ] `answeredAt` sur la question est mis à jour à now()
- [ ] Le garde SF-56-05 reconnaît la nouvelle réponse comme activité valide (createdAt > lastEnrichedAt)
- [ ] Isolation workspace : question d'un autre workspace → 404

---

## Périmètre

### Hors scope

- Suppression d'une réponse
- Affichage de l'historique des réponses
- Tout changement de code (comportement déjà fonctionnel — uniquement tests)

---

## Contraintes de validation

| Champ | Obligatoire | Notes |
|-------|-------------|-------|
| answerText | Oui | Non vide — validé par @NotBlank existant |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/ai-questions/{questionId}/answer` | Oui | LAWYER (membre du workspace) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| ai_question_answers | INSERT | Nouvelle entrée à chaque appel |
| ai_questions | UPDATE | answered_at mis à jour |

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

- [ ] `AiQuestionAnswerCommandService` — ré-answering : nouvelle entrée créée, `answeredAt` mis à jour

### Tests d'intégration

- [ ] `POST /answer` sur question déjà ANSWERED → 201 + 2 entrées en base
- [ ] Garde SF-56-05 : après ré-réponse, `existsByAiQuestion_CaseFile_IdAndCreatedAtAfter` = true

### Isolation workspace

- [ ] Question d'un autre workspace → 404

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — aucun changement de code, uniquement tests

---

## Dépendances

### Subfeatures bloquantes

- SF-56-05 — statut : done

---

## Notes et décisions

Le comportement de ré-answering est déjà implémenté dans `AiQuestionAnswerCommandService.answer()` :
aucun guard bloquant si la question est déjà ANSWERED, une nouvelle entrée est toujours créée.
Le `findFirstByAiQuestionIdOrderByCreatedAtDesc` garantit que la dernière réponse est toujours utilisée.
SF-57-01 se limite donc à ajouter la couverture de test manquante.

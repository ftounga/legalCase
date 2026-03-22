# Mini-spec — F-28 / SF-28-01 Résumés compacts dans les system prompts du pipeline IA

---

## Identifiant

`F-28 / SF-28-01`

## Feature parente

`F-28` — Scalabilité pipeline IA — résumés compacts

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-28-01-prompts-compacts`

---

## Objectif

Ajouter des contraintes de longueur explicites dans les system prompts de `DocumentAnalysisService` et `CaseAnalysisService` pour que chaque niveau du pipeline produise des résumés bornés, empêchant l'explosion de l'input au niveau supérieur lorsque le nombre de documents augmente.

---

## Comportement attendu

### Cas nominal

Sans cette feature, l'input de `CaseAnalysisService` est la concaténation brute des `document_analyses`, qui peut atteindre 30 000+ chars pour 3 documents (et croît linéairement). Le JSON produit par Claude n'est pas borné.

Avec cette feature :
- `DocumentAnalysisService` reçoit un system prompt imposant : max 5 faits, 3 points juridiques, 3 risques, 3 questions ouvertes
- `CaseAnalysisService` reçoit un system prompt imposant : max 5 entrées timeline, 7 faits, 5 points juridiques, 5 risques, 5 questions ouvertes
- La sortie de chaque niveau est ~3× plus courte qu'avant
- L'input de `CaseAnalysisService` reste sous ~10 000 chars même avec 10+ documents

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Claude ignore la contrainte et produit plus d'items | Le parsing JSON retourne les N premiers items, le reste est ignoré — comportement acceptable |
| Claude retourne un JSON tronqué (max_tokens) | Comportement inchangé — `stripMarkdownCodeBlock` + `catch ignored` déjà en place |

---

## Critères d'acceptation

- [ ] Le system prompt de `DocumentAnalysisService` spécifie un nombre maximum d'items par champ JSON
- [ ] Le system prompt de `CaseAnalysisService` spécifie un nombre maximum d'items par champ JSON
- [ ] Avec 3 documents de test, l'input de `CaseAnalysisService` est < 15 000 chars (vérifié en logs)
- [ ] La synthèse retournée à l'API contient bien les champs timeline, faits, pointsJuridiques, risques, questionsOuvertes non vides
- [ ] La génération de questions fonctionne en bout de chaîne

---

## Périmètre

### Hors scope (explicite)

- Pas de changement de schéma DB
- Pas de changement d'API REST ni de frontend
- Pas de modification du chunking ou de l'analyse de chunks
- Pas de pagination ou découpage de l'input (abordé en V2 si nécessaire)
- Pas de modification de `AiQuestionService` (son input est déjà borné par la case analysis)

---

## Contraintes de validation

Aucun champ soumis à validation métier — modification purement interne des prompts LLM.

---

## Technique

### Endpoints

Aucun endpoint créé ou modifié.

### Tables impactées

Aucune migration. Les données stockées dans `document_analyses.analysis_result` et `case_analyses.analysis_result` seront plus courtes mais de même format JSON.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

Aucun.

---

## Plan de test

### Tests unitaires

- [ ] `DocumentAnalysisService` — vérifier que le system prompt contient les contraintes de longueur (test sur la constante `SYSTEM_PROMPT`)
- [ ] `CaseAnalysisService` — idem

### Tests d'intégration

- [ ] Non applicable — pas d'API modifiée

### Isolation workspace

- [ ] Non applicable — modification interne au pipeline, pas d'accès aux données d'un autre workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité aux system prompts des deux services

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routes, auth, workspace ni navigation

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

**Valeurs choisies pour les limites :**

| Service | Champ | Limite |
|---------|-------|--------|
| DocumentAnalysisService | faits | 5 max |
| DocumentAnalysisService | points_juridiques | 3 max |
| DocumentAnalysisService | risques | 3 max |
| DocumentAnalysisService | questions_ouvertes | 3 max |
| CaseAnalysisService | timeline | 5 entrées max |
| CaseAnalysisService | faits | 7 max |
| CaseAnalysisService | points_juridiques | 5 max |
| CaseAnalysisService | risques | 5 max |
| CaseAnalysisService | questions_ouvertes | 5 max |

Ces valeurs sont des seuils raisonnables pour un dossier de droit du travail. Ajustables ultérieurement sans migration.

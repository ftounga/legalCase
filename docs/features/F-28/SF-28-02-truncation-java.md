# Mini-spec — F-28 / SF-28-02 Truncation Java des tableaux de synthèse

---

## Identifiant

`F-28 / SF-28-02`

## Feature parente

`F-28` — Scalabilité pipeline IA — résumés compacts

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-28-02-truncation-java`

---

## Objectif

Ajouter une truncation déterministe côté Java sur les tableaux retournés par Claude dans `DocumentAnalysisService` et `CaseAnalysisService`, garantissant que les limites définies en SF-28-01 sont respectées quelle que soit la réponse du modèle.

---

## Comportement attendu

### Cas nominal

SF-28-01 ajoute des contraintes dans les prompts (soft constraint). SF-28-02 ajoute une truncation Java après parsing (hard constraint).

Après appel à Claude et parsing JSON :
- `DocumentAnalysisService` : chaque tableau est tronqué avant persistance — `faits` limité à 5, `points_juridiques` à 3, `risques` à 3, `questions_ouvertes` à 3
- `CaseAnalysisService` : idem — `timeline` limité à 5, `faits` à 7, `points_juridiques` à 5, `risques` à 5, `questions_ouvertes` à 5

La truncation s'applique sur la chaîne JSON **avant stockage en base**. Le JSON persisté est reconstruit après truncation.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Claude retourne un JSON valide avec plus d'items que la limite | Les N premiers items sont conservés, le reste est silencieusement ignoré |
| Claude retourne un JSON tronqué (max_tokens) | `stripMarkdownCodeBlock` + catch déjà en place — comportement inchangé |
| Claude retourne un JSON sans l'un des champs | Tableau vide conservé — comportement inchangé |

---

## Critères d'acceptation

- [ ] `DocumentAnalysisService` tronque `faits` à 5, `points_juridiques` à 3, `risques` à 3, `questions_ouvertes` à 3 avant persistance
- [ ] `CaseAnalysisService` tronque `timeline` à 5, `faits` à 7, `points_juridiques` à 5, `risques` à 5, `questions_ouvertes` à 5 avant persistance
- [ ] Si Claude retourne exactement N items ou moins, aucun item n'est perdu
- [ ] Si Claude retourne plus de N items, seuls les N premiers sont conservés
- [ ] Le JSON reconstruit après truncation est valide et parseable

---

## Périmètre

### Hors scope (explicite)

- Pas de changement de schéma DB
- Pas de changement d'API REST ni de frontend
- Pas de modification du chunking ou de l'analyse de chunks
- Pas de modification de `AiQuestionService`
- Pas de modification des limites définies en SF-28-01 (même valeurs)

---

## Contraintes de validation

Aucun champ soumis à validation métier — truncation purement interne après parsing.

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

### Approche technique

Deux options :

**Option A — Truncation dans les services (choisie)**
Après `anthropicService.analyze(...)`, parser le JSON, tronquer, resérialiser, stocker.

```java
// Dans DocumentAnalysisService
String raw = result.content();
String truncated = DocumentAnalysisTruncator.truncate(raw);
analysis.setAnalysisResult(truncated);
```

**Option B — Truncation dans CaseAnalysisResponse/DocumentAnalysisResponse**
Au moment du parsing de la réponse. Rejetée : ces classes sont aussi utilisées pour lire les données déjà en base — on ne veut pas tronquer à la lecture.

L'Option A est préférable : la truncation se fait **à l'écriture**, pas à la lecture.

---

## Plan de test

### Tests unitaires

- [ ] `DocumentAnalysisService` — si Claude retourne 8 faits, seuls 5 sont persistés
- [ ] `DocumentAnalysisService` — si Claude retourne 2 faits (< limite), tous sont conservés
- [ ] `CaseAnalysisService` — si Claude retourne 10 faits, seuls 7 sont persistés
- [ ] `CaseAnalysisService` — si Claude retourne 3 faits (< limite), tous sont conservés
- [ ] Truncation sur chaque champ : timeline, faits, points_juridiques, risques, questions_ouvertes

### Tests d'intégration

- [ ] Non applicable — pas d'API modifiée

### Isolation workspace

- [ ] Non applicable — modification interne au pipeline

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à la persistance des résultats d'analyse

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routes, auth, workspace ni navigation

---

## Dépendances

### Subfeatures bloquantes

SF-28-01 — statut : done

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

**Limites identiques à SF-28-01 :**

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

**Sérialisation après truncation :** utiliser `ObjectMapper` pour resérialiser en JSON après avoir tronqué les listes. Le JSON stocké reste un objet JSON valide avec les mêmes champs.

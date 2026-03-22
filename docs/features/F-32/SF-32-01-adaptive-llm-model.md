# Mini-spec — F-32 / SF-32-01 Modèle LLM adaptatif par étape pipeline

---

## Identifiant

`F-32 / SF-32-01`

## Feature parente

`F-32` — Optimisation coût LLM — modèle adaptatif par étape

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-32-01-adaptive-llm-model`

---

## Objectif

Utiliser Claude Haiku (modèle rapide et bon marché) pour les étapes d'analyse chunk et document, et conserver Claude Sonnet pour les étapes synthèse dossier, génération de questions et re-synthèse enrichie. Réduction estimée ~80% des coûts LLM sans impact fonctionnel visible.

---

## Comportement attendu

### Cas nominal

- `ChunkAnalysisService.consumeChunkAnalysis()` utilise le modèle rapide (`model-fast`)
- `DocumentAnalysisService.consumeDocumentAnalysis()` utilise le modèle rapide (`model-fast`)
- `CaseAnalysisService`, `QuestionGenerationService`, `EnrichedAnalysisService` continuent d'utiliser le modèle principal (`model`)
- Le champ `model_used` dans `chunk_analyses` et `document_analyses` reflète le modèle réellement utilisé (Haiku)
- Le champ `model_used` dans `case_analyses` reflète Sonnet
- Aucun changement fonctionnel visible pour l'utilisateur

### Configuration

Deux propriétés dans `application-local.yml` (et application.yml) :
```yaml
anthropic:
  model: claude-sonnet-4-6          # synthèse dossier, questions, enrichi
  model-fast: claude-haiku-4-5-20251001  # chunk, document
```

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `model-fast` non configuré | Fallback sur `model` (Sonnet) — pas de régression |
| Haiku retourne une erreur API | Même retry logic que Sonnet (backoff 5/15/30/60s) |

---

## Critères d'acceptation

- [ ] `AnthropicService` expose une méthode `analyzeFast()` utilisant `model-fast`
- [ ] `analyzeChunk()` délègue à `analyzeFast()`
- [ ] `DocumentAnalysisService` appelle `analyzeFast()` au lieu de `analyze()`
- [ ] `CaseAnalysisService`, `QuestionGenerationService`, `EnrichedAnalysisService` inchangés
- [ ] `model_used` stocké en base reflète le vrai modèle utilisé (Haiku ou Sonnet)
- [ ] Si `model-fast` absent de la config : fallback Sonnet sans erreur au démarrage
- [ ] Tous les tests existants passent

---

## Périmètre

### Hors scope

- Pas de changement de logique métier
- Pas de migration de base de données
- Pas de modification frontend
- Pas de changement du modèle pour question generation (reste Sonnet)

---

## Technique

### Endpoints

Aucun.

### Tables impactées

Aucune modification de schéma. Le champ `model_used` existant stockera désormais l'identifiant Haiku pour chunk/document.

### Migration Liquibase

- [x] Non applicable

### Composants backend modifiés

- `AnthropicService` — ajout `modelFast`, méthode `analyzeFast()`
- `ChunkAnalysisService` — `analyzeChunk()` → `analyzeFast()`
- `DocumentAnalysisService` — `analyze()` → `analyzeFast()`
- `application-local.yml` — ajout `anthropic.model-fast`

---

## Plan de test

### Tests unitaires

- [ ] U-01 : `analyzeChunk()` appelle `analyzeFast()` (mock vérifie le bon model)
- [ ] U-02 : `DocumentAnalysisService` appelle `analyzeFast()` (mock)
- [ ] U-03 : fallback Sonnet si `model-fast` non configuré

### Tests d'intégration

- [ ] Non applicable — pas d'endpoint modifié

### Isolation workspace

- [ ] Non applicable

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification interne du pipeline, pas de routing, pas d'auth, pas de workspace

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — aucun changement de routing ou de comportement visible

---

## Dépendances

### Subfeatures bloquantes

Aucune.

---

## Notes et décisions

**Pourquoi Haiku pour chunk/document ?**
Ces étapes font des tâches simples et répétitives : extraire des faits d'un paragraphe, agréger des JSON. La qualité Sonnet est surdimensionnée et coûteuse. Haiku est suffisant et ~10x moins cher.

**Pourquoi garder Sonnet pour case/questions/enriched ?**
Ces étapes produisent le résultat final vu par l'avocat. La qualité de synthèse, la cohérence du raisonnement juridique, et la pertinence des questions nécessitent le meilleur modèle.

**Fallback** : si `model-fast` n'est pas dans la config, `@Value` avec valeur par défaut égale à `model`. Pas de crash.

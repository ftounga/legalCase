# Mini-spec — F-30 / SF-30-01 Traitement parallèle des chunks via concurrence RabbitMQ

---

## Identifiant

`F-30 / SF-30-01`

## Feature parente

`F-30` — Parallélisme pipeline IA — concurrence RabbitMQ

## Statut

`ready`

## Date de création

2026-03-22

## Branche Git

`feat/SF-30-01-concurrence-rabbitmq`

---

## Objectif

Configurer `concurrency = "3"` sur le `@RabbitListener` de `ChunkAnalysisService` pour traiter 3 chunks en parallèle au lieu de 1, réduisant le temps d'analyse de ~6 min à ~2 min pour 3 documents (13 chunks).

---

## Comportement attendu

### Cas nominal

Avant : les chunks sont traités séquentiellement — un seul consumer actif, chaque chunk attend la fin du précédent.

Après : 3 consumers tournent en parallèle sur la même queue `chunk.analysis`. RabbitMQ distribue les messages entre les 3 consumers. 3 appels Anthropic se font simultanément.

Impact sur le temps de traitement pour 3 documents (13 chunks, ~24s/chunk) :
- Avant : 13 × 24s ≈ 5 min
- Après : ⌈13/3⌉ × 24s ≈ 2 min

Les niveaux suivants (document analysis, case analysis, questions) restent inchangés — ils ne sont déclenchés qu'une fois tous les chunks d'un document terminés.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Anthropic rate limit (529) déclenché par les 3 appels simultanés | Retry avec backoff exponentiel déjà en place dans `AnthropicService.analyze()` — comportement inchangé |
| Un des 3 consumers échoue (exception) | Le message est rejeté par RabbitMQ, les 2 autres consumers continuent — comportement inchangé |

---

## Critères d'acceptation

- [ ] `ChunkAnalysisService` est configuré avec `concurrency = "3"` sur son `@RabbitListener`
- [ ] Avec 13 chunks, au moins 2 chunks sont visibles en traitement simultané dans les logs
- [ ] Le pipeline complet (extraction → questions) se termine correctement avec la concurrence activée
- [ ] Aucune régression sur les tests existants

---

## Périmètre

### Hors scope (explicite)

- Pas de modification de `DocumentAnalysisService`, `CaseAnalysisService`, `AiQuestionService` (un seul message par dossier à ce niveau, la concurrence n'apporte rien)
- Pas de changement de schéma DB
- Pas de changement d'API REST ni de frontend
- Pas de modification du retry backoff
- Pas de tuning fin du prefetch count RabbitMQ (valeur par défaut suffisante)

---

## Contraintes de validation

Aucun champ soumis à validation métier.

---

## Technique

### Endpoints

Aucun endpoint créé ou modifié.

### Tables impactées

Aucune migration.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

Aucun.

### Modification

```java
// ChunkAnalysisService.java — avant
@RabbitListener(queues = RabbitMQConfig.CHUNK_ANALYSIS_QUEUE)

// après
@RabbitListener(queues = RabbitMQConfig.CHUNK_ANALYSIS_QUEUE, concurrency = "3")
```

Une ligne. Aucun autre changement.

---

## Plan de test

### Tests unitaires

- [ ] Aucun nouveau test unitaire requis — la logique métier est inchangée
- [ ] Vérifier que les tests existants de `ChunkAnalysisServiceTest` passent toujours

### Tests d'intégration

- [ ] Non applicable — pas d'API modifiée

### Isolation workspace

- [ ] Non applicable

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification d'une annotation sur un consumer RabbitMQ interne

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

**Pourquoi concurrency = 3 et pas plus ?**

Avec le Tier 1 Anthropic (~50 req/min), 3 appels simultanés restent très en dessous des limites. Monter à 5 serait possible mais on préfère démarrer conservateur pour éviter les 529. Ajustable sans redéploiement via `application.properties` si nécessaire.

**Pourquoi seulement ChunkAnalysisService ?**

Les autres services (DocumentAnalysis, CaseAnalysis, Questions) reçoivent un message unique par dossier — la concurrence n'y apporterait rien. Le goulot d'étranglement est exclusivement au niveau des chunks.

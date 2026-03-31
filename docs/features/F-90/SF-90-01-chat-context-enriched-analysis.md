# Mini-spec — F-90 / SF-90-01 Chat comme contexte de l'analyse enrichie

---

## Identifiant

`F-90 / SF-90-01`

## Feature parente

`F-90` — Chat comme contexte de l'analyse enrichie

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-90-01-chat-context-enriched-analysis`

---

## Objectif

Injecter un résumé Haiku de l'historique du chat libre dans le prompt de l'analyse enrichie, en complément des Q&A existants, et étendre le garde de re-analyse pour autoriser le lancement dès qu'un nouveau message chat OU une nouvelle réponse Q&A existe depuis la dernière analyse enrichie.

---

## Comportement attendu

### Cas nominal

1. L'avocat a échangé des messages dans le chat du dossier.
2. Il lance une analyse enrichie (via `POST /api/v1/case-files/{id}/re-analysis`).
3. Le garde `ReAnalysisCommandService` vérifie : `hasNewAnswers || hasNewChatMessages` depuis la dernière analyse enrichie DONE. Si les deux sont faux → 409.
4. Dans `EnrichedAnalysisService.prepareEnrichedAnalysis()` :
   - Les messages chat du dossier sont récupérés (`ChatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc`)
   - Si au moins un message existe : appel **synchrone Haiku** (`AnthropicService.analyzeFast`) pour produire un résumé analytique du chat (max 512 tokens en sortie)
   - Le résumé est injecté dans le prompt sous une nouvelle section `[Échanges libres avec l'assistant]`
   - Si aucun message chat : la section est omise (pas de placeholder)
5. L'analyse enrichie est produite avec le contexte étendu.

### Prompt de résumé Haiku

```
System : Tu es un assistant juridique. Résume en points clés analytiques les échanges suivants entre un avocat et l'IA sur un dossier juridique. Extrais uniquement les informations factuelles, les clarifications importantes et les observations de l'avocat. Ignore les reformulations et questions triviales. Sois concis (max 10 points).

User : [liste des messages : Q: ... / R: ...]
```

### Format injecté dans le prompt enrichi

```
[Synthèse précédente]
...

[Questions et réponses de l'avocat]
...

[Échanges libres avec l'assistant — points clés]
...résumé Haiku...
```

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Aucune nouvelle réponse Q&A ET aucun nouveau message chat depuis la dernière enrichie | 409 — message existant inchangé | 409 |
| Appel Haiku échoue (timeout, erreur API) | Fail-open : la section chat est omise, l'analyse enrichie continue sans elle | — |
| Chat vide (aucun message) | Section omise, comportement identique à avant SF-90-01 | — |
| Résumé Haiku vide ou blanc | Section omise | — |

---

## Critères d'acceptation

- [ ] `ReAnalysisCommandService` : garde étendu — `hasNewAnswers || hasNewChatMessages` depuis `lastEnriched.getUpdatedAt()`
- [ ] `ChatMessageRepository` : méthode `existsByCaseFileIdAndCreatedAtAfter(UUID, Instant)` ajoutée
- [ ] `EnrichedAnalysisService.buildEnrichedPrompt()` accepte un paramètre `chatSummary` nullable
- [ ] Si des messages chat existent : appel `AnthropicService.analyzeFast` pour résumé (max 512 tokens)
- [ ] Si résumé non-vide : section `[Échanges libres avec l'assistant — points clés]` injectée dans le prompt
- [ ] Si aucun message ou appel Haiku échoue : section absente, pas d'exception propagée (fail-open)
- [ ] Tests unitaires : `buildEnrichedPrompt` avec et sans chatSummary
- [ ] Tests unitaires : garde avec hasNewChatMessages seul → autorisé
- [ ] Tests IT : `POST /re-analysis` → 409 si ni nouvelle réponse ni nouveau message chat
- [ ] Tests IT : `POST /re-analysis` → 202 si nouveau message chat uniquement (sans nouvelle réponse Q&A)

---

## Périmètre

### Hors scope

- Snapshot des messages chat par analyse (pas de table `analysis_chat_snapshots` — le chat est lu en live)
- Affichage frontend du résumé chat dans la synthèse
- Comptage des tokens du résumé dans les métriques d'usage (déjà tracé via `usageEventService` sur l'analyse enrichie)
- Modification de l'UI du chat ou de la page synthèse

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Modification |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{id}/re-analysis` | Oui | Garde étendu — aucun changement de signature |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `chat_messages` | SELECT | Lecture des messages pour résumé — pas d'écriture |

### Migration Liquibase

- [ ] Non applicable — aucun changement de schéma

### Composants impactés

| Fichier | Modification |
|---------|-------------|
| `ReAnalysisCommandService` | Garde étendu : `hasNewAnswers \|\| hasNewChatMessages` |
| `ChatMessageRepository` | Ajout `existsByCaseFileIdAndCreatedAtAfter` |
| `EnrichedAnalysisService.buildEnrichedPrompt()` | Paramètre `chatSummary` nullable, section conditionnelle |
| `EnrichedAnalysisService.prepareEnrichedAnalysis()` | Appel Haiku préalable + injection résumé |

---

## Plan de test

### Tests unitaires backend

- [ ] `buildEnrichedPrompt(caseFileId, previousResult, "résumé chat")` → prompt contient `[Échanges libres]`
- [ ] `buildEnrichedPrompt(caseFileId, previousResult, null)` → prompt ne contient pas `[Échanges libres]`
- [ ] `buildEnrichedPrompt(caseFileId, previousResult, "  ")` → section absente (blank ignoré)
- [ ] Garde : `hasNewAnswers=false, hasNewChatMessages=true` → pas de 409
- [ ] Garde : `hasNewAnswers=false, hasNewChatMessages=false` → 409

### Tests d'intégration backend

- [ ] `POST /re-analysis` avec nouveau message chat (sans nouvelle réponse Q&A) → 202
- [ ] `POST /re-analysis` sans nouveau message chat ni réponse Q&A → 409

### Isolation workspace

- [x] Applicable — déjà garantie par le garde existant (workspace résolu via `workspaceMemberRepository`)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — le garde touche `aiQuestionAnswerRepository` et ajoute `chatMessageRepository`. `PlanLimitService` non modifié.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|------------------------|
| `ReAnalysisCommandService` | Garde assoupli — les tests IT existants vérifiant le 409 doivent s'assurer qu'il n'y a ni nouvelle réponse Q&A **ni** nouveau message chat | IT existant à vérifier |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routing, d'auth ou de workspace context

---

## Dépendances

### Subfeatures bloquantes

- Aucune

---

## Notes et décisions

- **Pas de snapshot chat** : contrairement aux Q&A et documents, les messages chat ne sont pas snapshotés par analyse. Le chat est lu en live au moment de `prepareEnrichedAnalysis`. Justification : le chat est un contexte auxiliaire, pas une source de vérité versionnée.
- **Fail-open sur Haiku** : si l'appel résumé échoue, l'analyse enrichie se lance quand même sans la section chat. L'avocat n'est pas bloqué.
- **512 tokens max en sortie** du résumé Haiku : suffisant pour 10 points analytiques, negligeable sur le budget total de l'analyse enrichie (qui peut consommer 8192 tokens).
- **Appel Haiku synchrone** dans `prepareEnrichedAnalysis` (qui est déjà `@Transactional`) : acceptable car Haiku est rapide (<2s en général) et le résumé est produit avant de publier le message RabbitMQ.

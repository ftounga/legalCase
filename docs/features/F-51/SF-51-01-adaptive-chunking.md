# Mini-spec — F-51 / SF-51-01 Pipeline IA adaptatif — chunking conditionnel

## Identifiant

`F-51 / SF-51-01`

## Feature parente

`F-51` — Pipeline IA adaptatif — chunking conditionnel

## Statut

`ready`

## Date de création

2026-03-25

## Branche Git

`feat/SF-51-01-adaptive-chunking`

---

## Objectif

Bypasser le niveau chunk du pipeline IA quand le document extrait tient dans la fenêtre de contexte de Claude, afin de réduire le nombre d'appels Anthropic et le temps d'analyse.

---

## Comportement attendu

### Cas nominal — document court (< 200 000 chars ≈ 50 000 tokens)

1. `ExtractionService` produit un `DocumentExtraction` avec le texte extrait
2. `ChunkingService` reçoit l'event `ExtractionDoneEvent`
3. Il mesure la longueur du texte extrait
4. Longueur < seuil (600 000 chars ≈ 150 000 tokens) → **pas de chunks créés**, publication directe sur l'exchange `document-analysis` avec un flag `directAnalysis = true`
5. `DocumentAnalysisService` reçoit le message, détecte `directAnalysis = true`, utilise le texte brut de l'extraction comme input au lieu d'agréger des chunk analyses
6. L'analyse document se termine normalement
7. Le job `CHUNK_ANALYSIS` n'est pas créé (aucun chunk à suivre)
8. Le job `DOCUMENT_ANALYSIS` est créé et mis à jour normalement

### Cas nominal — document long (>= 200 000 chars)

Pipeline existant inchangé : chunking → chunk analysis → document analysis.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Texte extrait null ou vide | Comportement inchangé (déjà géré en amont) |
| Extraction introuvable dans DocumentAnalysisService | Log error + skip (comportement existant) |

---

## Critères d'acceptation

- [ ] Un document < 200 000 chars ne génère aucun `DocumentChunk` en base
- [ ] Un document < 200 000 chars ne génère aucun `ChunkAnalysis` en base
- [ ] Un document < 200 000 chars produit bien un `DocumentAnalysis` DONE
- [ ] Le job `DOCUMENT_ANALYSIS` est créé et passe à DONE
- [ ] Le job `CHUNK_ANALYSIS` n'est pas créé pour un document court
- [ ] Un document >= 200 000 chars suit le pipeline existant (chunks créés, chunk analyses créées)
- [ ] Le seuil est configurable via une propriété Spring (`app.pipeline.direct-analysis-threshold-chars`, défaut 600000)

---

## Périmètre

### Hors scope

- Modification de l'UI (aucun impact visible pour l'utilisateur)
- Modification des cas de re-analyse (même logique appliquée)
- Modification de CaseAnalysisService

---

## Technique

### Composants modifiés

| Composant | Modification |
|-----------|-------------|
| `ChunkingService` | Vérifie la taille du texte. Si < seuil : publie sur `document-analysis` exchange avec `directAnalysis=true`. Ne crée pas de chunks. |
| `DocumentAnalysisMessage` | Ajoute un champ `boolean directAnalysis` |
| `DocumentAnalysisService` | Si `directAnalysis=true` : utilise `extraction.getExtractedText()` comme input. Sinon comportement existant. |
| `application.yml` | Nouvelle propriété `app.pipeline.direct-analysis-threshold-chars=200000` |

### Tables impactées

| Table | Impact |
|-------|--------|
| `document_chunks` | Vide pour les documents courts (normal) |
| `chunk_analyses` | Vide pour les documents courts (normal) |
| `document_analyses` | Inchangé — créé dans les deux cas |
| `analysis_jobs` | `CHUNK_ANALYSIS` non créé pour les documents courts |

### Migration Liquibase

- [ ] Non applicable — aucun changement de schéma

---

## Plan de test

### Tests unitaires

- [ ] `ChunkingService` — document court → aucun chunk créé, message `directAnalysis=true` publié
- [ ] `ChunkingService` — document long → chunks créés normalement
- [ ] `DocumentAnalysisService` — `directAnalysis=true` → utilise texte brut, pas de chunk analyses
- [ ] `DocumentAnalysisService` — `directAnalysis=false` → comportement existant inchangé

### Tests d'intégration

- [ ] Tests existants `ChunkAnalysisServiceTest` et `DocumentAnalysisServiceTest` passent sans modification

### Isolation workspace

- [ ] Non applicable — pas de changement de contrôle d'accès

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Aucune préoccupation transversale** — subfeature isolée au pipeline interne, pas de changement d'auth, workspace, plans ou routing frontend

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routing ou auth

---

## Notes et décisions

- Seuil retenu : **600 000 chars** ≈ 150 000 tokens. Laisse 50k tokens de marge pour system prompt + réponse (~10k) dans la fenêtre de 200k tokens de Claude.
- Décision tranchée le 2026-03-25 : le chunking doit être conditionnel à la taille du document, pas systématique.
- Documents juridiques courants (contrats, lettres de licenciement, conventions) : ~10k-20k chars → toujours en mode direct.
- Réduction estimée : de ~50 appels Anthropic à 1 pour 3 documents de taille standard.

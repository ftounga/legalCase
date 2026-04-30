# Mini-spec — F-161 / SF-161-02 — Backend : bump `max_tokens` Anthropic pour absorber les nouveaux caps

## Identifiant

`F-161 / SF-161-02`

## Feature parente

`F-161` — Augmentation des limites de synthèse (faits, risques, points juridiques, timeline)

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-161-02-bump-max-tokens`

---

## Objectif

Élever `max_tokens` Anthropic des 2 services de synthèse (`CaseAnalysisService.consumeAnalysis` + `EnrichedAnalysisService.consumeReAnalysis`) de **16384 → 64000** pour que la sortie JSON ne soit plus tronquée silencieusement après l'élévation des caps F-161 SF-161-01. Hotfix proactif avant remontée terrain.

---

## Comportement attendu

### Cas nominal

1. Dossier prud'homal complexe (50+ pages, multi-CDD, 24 bulletins, 12 pièces).
2. Pipeline de synthèse niveau dossier (`CaseAnalysisService.consumeAnalysis`) ou enrichi (`EnrichedAnalysisService.consumeReAnalysis`).
3. L'IA produit jusqu'aux nouvelles caps F-161 (80 faits / 80 points / 40 risques / 60-80 timeline / etc.) + `score_risque` + `delais_detectes` + `source_explanations` + champs récap.
4. Output ≈ 17 K-36 K tokens selon richesse → **sous le plafond 64000**, pas de troncature.
5. `stop_reason="end_turn"` (pas `"max_tokens"`).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| L'IA dépasse 64000 (théorique sur dossier extraordinaire) | `stop_reason="max_tokens"` → `AnthropicService` log WARN → JSON tronqué → `AnalysisJsonTruncator` retourne tel quel (catch parse error). Comportement identique à aujourd'hui mais à 64000 au lieu de 16384. |
| API Anthropic refuse 64000 | Erreur 400 explicite côté API → propagation exception → analyse FAILED, log clair (pas de troncature silencieuse). |
| Dossier simple | Output ~2-4 K tokens, plafond non atteint, comportement inchangé. |

---

## Contrat

Aucun changement d'API publique. Modification interne du paramètre `maxTokens` passé à `AnthropicService.analyzeWithSystemCache(...)`.

### Avant

```java
// CaseAnalysisService.java ligne 175
result = anthropicService.analyzeWithSystemCache(prepared.systemPrompt(), prepared.prompt(), 16384);

// EnrichedAnalysisService.java ligne 216
result = anthropicService.analyzeWithSystemCache(prepared.systemPrompt(), prepared.prompt(), 16384);
```

### Après

```java
// CaseAnalysisService.java
result = anthropicService.analyzeWithSystemCache(prepared.systemPrompt(), prepared.prompt(), 64000);

// EnrichedAnalysisService.java
result = anthropicService.analyzeWithSystemCache(prepared.systemPrompt(), prepared.prompt(), 64000);
```

### Modèle utilisé

`claude-sonnet-4-6` (configuré via `anthropic.model` dans `application.yml`). Famille Sonnet 4.x supporte jusqu'à 64000 tokens output. Pas de header beta requis.

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres call sites `analyzeWithSystemCache` ou `analyze`** : grep dans `backend/src/main/java`.
  - `DocumentAnalysisService` : `analyze(prompt, userMessage, ?)` — niveau document, output beaucoup plus petit (caps 5-7 faits / 3-4 points / 3 risques / 0 timeline). max_tokens actuel ~4096 ou 8192 — **non concerné** car caps document level inchangées en SF-161-01.
  - `AnthropicService` autres usages (chat, génération de questions, dedup) — niveaux bornés (~1024-2048 tokens), pas de risque de truncation.
- **`AnalysisJsonTruncator`** : déjà défensif en cas de truncation (catch parse error → retourne tel quel). Pas de changement requis.
- **Logs WARN existants** : `AnthropicService.doAnalyze` ligne 193-196 logue déjà `stop_reason=max_tokens` quand truncation. Garde-fou observable en place — pas besoin d'instrumentation supplémentaire.
- **Coût IA** : `max_tokens` est un **plafond**, pas un quota — on paie le réel généré. Bump 16384→64000 n'a **aucun impact coût** tant que l'IA ne génère pas plus.
- **Latence** : output 30 K vs 15 K tokens ≈ +30-45 s sur le streaming Anthropic (à 600-800 tok/s). Latence acceptable car analyse async.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `CaseAnalysisService.consumeAnalysis` | Oui | Modifié dans cette SF |
| `EnrichedAnalysisService.consumeReAnalysis` | Oui | Modifié dans cette SF |
| `DocumentAnalysisService` | Non | Document level inchangé en SF-161-01, caps étroits, pas de risque |
| `AiQuestionService` | Non | Génération de questions, output borné petit |
| `AnthropicService` autres call sites | Non | Tous bornés petits |
| `AnalysisJsonTruncator` | Non | Code défensif déjà en place |

### Décision

- [x] Modifié pour les 2 cibles directes (initial + enriched)
- [x] Document level non touché (caps étroits, pas de risque)
- [x] Aucun nouveau pattern partagé créé

---

## Impact par domaine métier

**Transversal aux 3 domaines** (Travail / Immigration / Famille) — la richesse de l'output est intrinsèque au dossier, pas au domaine. Symétrique des nouvelles caps SF-161-01.

---

## Critères d'acceptation

- [ ] **C1** — `CaseAnalysisService.java` ligne 175 : `16384` → `64000`
- [ ] **C2** — `EnrichedAnalysisService.java` ligne 216 : `16384` → `64000`
- [ ] **C3** — Le commentaire au-dessus de chaque call est mis à jour pour expliquer le pourquoi (F-161 SF-161-02, sinon truncation silencieuse sur dossiers riches post-bump caps SF-161-01)
- [ ] **C4** — Build backend vert (`./mvnw test`)
- [ ] **C5** — Aucune régression sur les tests existants (3132/3132 verts attendus)
- [ ] **C6** — Aucun call site non modifié n'utilise encore `16384` dans ces 2 fichiers

---

## Périmètre

### Hors scope

- Document level `max_tokens` (caps inchangées, pas de risque)
- Bump `max_tokens` sur `AiQuestionService` / chat / dedup (output borné petit)
- Refonte de `AnthropicService` ou de la signature `analyzeWithSystemCache`
- Streaming output Anthropic (optimisation latence, hors scope)
- Constante partagée `MAX_OUTPUT_TOKENS` (over-engineering pour 2 lignes)

---

## Technique

### Fichiers modifiés

- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisService.java` (1 ligne + commentaire)
- `backend/src/main/java/fr/ailegalcase/analysis/EnrichedAnalysisService.java` (1 ligne + commentaire)

### Pattern de référence

`F-161 SF-161-01` — élévation des caps. Cette SF est le complément technique direct (le filet `max_tokens` doit suivre le filet `caps`).

### Endpoints / tables

Aucun changement.

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires

- [ ] Aucun test spécifique requis : la valeur `64000` est un littéral passé à un mock dans les tests existants. Les call sites ne sont pas testés directement (le mock `anthropicService.analyzeWithSystemCache(any(), any(), anyInt())` accepte tout entier).
- [ ] Vérification implicite : tous les tests existants passent (3132/3132).

### Tests d'intégration

Non applicable (pas de pipeline IA réel testé en local — clé API requise, hors scope).

### Vérification staging

Après déploiement staging, sur 1 dossier complexe :
- Re-analyser → vérifier dans les logs absence de WARN `Anthropic response TRUNCATED — stop_reason=max_tokens`
- Re-synthèse enrichie → idem

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non (`max_tokens` ≠ budget mensuel F-15 ; budget F-15 calcule sur tokens réellement consommés)
- [ ] Navigation / routing — non
- [ ] Outil décisionnel métier — non
- [x] Aucune préoccupation transversale critique

### Smoke tests E2E

Aucun smoke test ne dépend de `max_tokens`. Pas de régression attendue.

### Coût IA

Aucun impact direct. `max_tokens=64000` est un plafond ; on paie les tokens réellement générés. Avec les caps F-161 SF-161-01, l'output réel reste < 36 K en pic, identique à la situation où `max_tokens=16384` aurait suffi sans truncation.

### Latence

+30-45 s sur dossiers extrêmement riches (output 30 K vs 15 K à 600-800 tok/s sur Sonnet 4.6). Acceptable car analyse async.

---

## Dépendances

### Subfeatures bloquantes

- **F-161 SF-161-01 — Terminée** (PR #722 mergée 2026-04-30 commit 61f7a7ac). Cette SF-161-02 est le complément technique direct.

---

## Notes et décisions

- **Décision** : bump à **64000** (vs 32768 intermédiaire). Rationale : worst case estimé 34-36 K tokens output sur dossiers extraordinaires (80 faits + 80 points + annexes verbatim). 32 K ne couvrirait pas. 64000 = max documenté Sonnet 4.x, marge confortable.
- **Décision** : pas de constante partagée. 2 call sites = pas d'over-engineering. Si un 3ème call site apparaît, on extrait à ce moment-là.
- **Décision** : on garde la valeur littérale dans le code (vs config yaml). C'est un paramètre technique de bornage, pas un réglage produit.
- **Note** : si malgré 64 K on observe encore des truncations sur dossiers extraordinaires, l'option suivante est de découper la synthèse en plusieurs appels (bloc-par-bloc) — mais c'est une refonte majeure, pas un hotfix. SF-161-02 est suffisant pour les cas réalistes.
- **Note** : `AnthropicService` log déjà `WARN Anthropic response TRUNCATED` quand `stop_reason=max_tokens`. Si on revoit cette alerte en staging/prod, c'est un signal pour itérer.

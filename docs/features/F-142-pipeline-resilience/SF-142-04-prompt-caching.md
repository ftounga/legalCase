# SF-142-04 — Prompt caching Anthropic (cache_control: ephemeral)

## Objectif
Activer le prompt caching Anthropic sur les 3 services à gros system prompt
(`CaseAnalysisService`, `EnrichedAnalysisService`, `AiQuestionService`) pour
réduire la latence prefill de ~85 % sur les appels successifs dans une même
session (TTL 5 min).

## Comportement nominal
- Chaque appel Anthropic des 3 services envoie le system prompt sous forme
  de tableau `[{type:"text", text:"...", cache_control:{type:"ephemeral"}}]`
  au lieu d'une string brute.
- Au 1er appel, Anthropic met le prompt en cache (5 min TTL).
- Aux appels suivants avec le même system prompt, Anthropic sert la réponse
  avec `cache_read_input_tokens` (tarif /10) → latence prefill ~85 % en moins.
- Transparent côté application : aucune modification du JSON de réponse.

## Cas d'erreur
- **Prompt < 1024 tokens (Sonnet) ou < 2048 (Haiku)** : Anthropic ignore
  silencieusement `cache_control`. Pas d'erreur, simplement pas de gain. Nos
  3 services ont des system prompts > 2 000 tokens → éligibles.
- **Bloc cache_control malformé** : Anthropic renvoie 400. Le format utilisé
  suit exactement la doc officielle → pas de risque.
- **Concurrent-safe** : Anthropic gère le cache côté serveur, aucune
  coordination côté client nécessaire.

## Critères d'acceptation
- [x] `AnthropicService.analyzeWithSystemCache()` nouvelle méthode publique
- [x] `doAnalyze()` privée accepte `cacheSystem: boolean`
- [x] `CaseAnalysisService` ligne 165 utilise `analyzeWithSystemCache()`
- [x] `EnrichedAnalysisService` ligne 207 utilise `analyzeWithSystemCache()`
- [x] `AiQuestionService` ligne 96 utilise `analyzeWithSystemCache()`
- [x] Méthode `analyze()` historique conservée (rétro-compat + tests
  DocumentPieceDetectionService / ChatService non migrés)
- [x] Tests unitaires verts (47 tests CaseAnalysis/Enriched/AiQuestion/Sentry)

## Plan de test
- **Unit** : tests existants doivent passer (mocks swappés)
- **Intégration** : appel réel staging, observer logs
  `cache_creation_input_tokens` (1er appel) puis `cache_read_input_tokens`
  (appels suivants) dans la response Anthropic via inspection manuelle
- **Isolation workspace** : non concerné (pas de requête DB)

## Hors périmètre (reporté)
- **Parallélisation virtual threads** mentionnée dans F-142 principal :
  analyse du code révèle que chacun des 3 services fait **un seul** appel
  Anthropic par dossier (synthèse agrégée sur un prompt unique). Les chunks
  sont déjà parallélisés via RabbitMQ (consumers concurrents). Pas de boucle
  séquentielle à paralléliser dans ces services. Le gain théorique "10 chunks
  × 20s → 20s" décrit dans F-142 ne s'applique pas à l'état actuel du code.
  → virtual threads non ajoutés, scope SF-142-04 réduit au prompt caching.
- `DocumentPieceDetectionService` et `ChatService` : prompts courts par
  nature (détection pièce = quelques KB, chat = contexte variable). Gain
  marginal, risque de confusion en migrant → laissés avec `.analyze()`.

## Tables / endpoints / composants impactés
- `backend/.../analysis/AnthropicService.java` : +1 méthode publique, +1 param
- `backend/.../analysis/CaseAnalysisService.java` : 1 ligne modifiée
- `backend/.../analysis/EnrichedAnalysisService.java` : 1 ligne modifiée
- `backend/.../analysis/AiQuestionService.java` : 1 ligne modifiée
- 4 tests mis à jour (mocks `.analyze()` → `.analyzeWithSystemCache()`)

## Impact par domaine métier
Transversale — infrastructure IA, aucune adaptation par domaine (droit du
travail / immigration / famille). S'applique identiquement aux 3 domaines
et aux 2 pays (FR / BE).

## Analyse de cohérence transversale
- **Outils décisionnels (F-DT/F-IM/F-FA)** : n'appellent pas AnthropicService
  directement → non applicable.
- **DocumentPieceDetectionService / ChatService** : cf. Hors périmètre
  ci-dessus, laissés non migrés volontairement.
- **F-148 vision** : `analyzeWithImages()` est un cas différent (images
  base64 par appel, pas de system prompt répétable à cacher). Non applicable.

## Nouveau pattern UI ou service partagé
Aucun. Ajout mineur d'un paramètre à une méthode privée existante
(`doAnalyze(..., cacheSystem)`) et d'une variante publique. Pas de service
partagé créé.

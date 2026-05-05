# SF-190-02 — Streaming SSE de la synthèse enrichie

## Objectif

Étendre le pattern de streaming SSE de F-185 SF-185-01 (déjà en prod sur `CaseAnalysisService`) à `EnrichedAnalysisService` pour que la **re-analyse enrichie** affiche aussi sa progression section par section sur la page synthèse, exploitable par la barre granulaire de SF-190-01.

## Contexte

F-185 SF-185-01 a livré le streaming pour `CaseAnalysisService.consumeAnalysis` :
- `analyzeWithSystemCacheStreaming` Anthropic + `PartialJsonSectionExtractor`
- Persistance dans `case_analyses.partial_state` (transaction `REQUIRES_NEW`)
- Émission événement SSE `JobType.CASE_ANALYSIS` `PARTIAL`
- Endpoint `GET /api/v1/case-files/{id}/case-analysis/partial` retourne le snapshot

`EnrichedAnalysisService.consumeReAnalysis` reste **synchrone** : appel `analyzeWithSystemCache` complet, pas de partial_state, pas d'événement intermédiaire. Pour l'avocat, la re-analyse enrichie ressemble à une longue attente sans signal.

Cette SF applique le même pattern à enriched. Toute l'infra est déjà en place — il s'agit de réutiliser `PartialJsonSectionExtractor` (machine à états sans dépendance externe) et la colonne `partial_state` (déjà créée par migration 201 SF-185-01).

## Comportement nominal

1. Quand l'avocat clique « Re-analyser » → message `RE_ANALYSIS_QUEUE` consommé par `EnrichedAnalysisService`.
2. Au lieu d'un appel `analyzeWithSystemCache` synchrone, on utilise `analyzeWithSystemCacheStreaming` (déjà disponible dans `AnthropicService`) avec callback `onDelta`.
3. À chaque section JSON top-level close détectée par `PartialJsonSectionExtractor.append(delta)`, on persiste `partial_state` + on émet `AnalysisStatusEvent(caseFileId, PARTIAL, JobType.ENRICHED_ANALYSIS)` via `persistPartialAndNotify` (méthode dédiée dans `EnrichedAnalysisService`, transaction `REQUIRES_NEW`, miroir exact de celle existante dans `CaseAnalysisService`).
4. À la fin du streaming (callback terminé), `finalizeEnrichedAnalysis` purge `partial_state = null` et bascule en `DONE` (logique inchangée).
5. Côté frontend, `subscribeToPartialEvents` doit aussi écouter `event.jobType === 'ENRICHED_ANALYSIS'` pour les statuts `PARTIAL` (refetch via `getPartial`) et `DONE` (refresh `loadVersions`).
6. `CaseAnalysisPartialResponse` (DTO backend + interface frontend) gagne un champ `analysisType` pour que `applyPartial` n'écrase plus le type avec `'STANDARD'` quand le partial est en réalité enriched.
7. **Fallback gracieux** identique à SF-185-01 : si streaming échoue (HTTP, parsing), on retombe sur `analyzeWithSystemCache` synchrone.

## Cas d'erreur / edge cases

- Le streaming Anthropic échoue → fallback synchrone (zéro régression).
- `PartialJsonSectionExtractor` reçoit du JSON malformé → log warn + skip de la section (logique existante, sans changement).
- Une re-analyse enrichie arrive en parallèle d'une analyse standard sur le même dossier → `getPartial` retourne la plus récente (latest in-flight via `findFirstByCaseFileIdAndAnalysisStatusInOrderByVersionDesc` existant). L'enriched a une version supérieure, donc elle prend le dessus.
- Le frontend reçoit un PARTIAL event pour ENRICHED_ANALYSIS sur un dossier dont l'avocat regarde la version courante DONE → `applyPartial` remplace la synthèse affichée par l'état partiel enrichi (UX désirée : l'avocat voit la nouvelle version se construire).

## Critères d'acceptation

- [ ] `EnrichedAnalysisService.consumeReAnalysis` appelle `analyzeWithSystemCacheStreaming` quand disponible, fallback `analyzeWithSystemCache` sinon.
- [ ] Méthode `persistPartialAndNotify(analysisId, caseFileId, sections)` dans `EnrichedAnalysisService`, `@Transactional REQUIRES_NEW`, persiste `partial_state` + bascule status `PROCESSING → PARTIAL` + émet event `ENRICHED_ANALYSIS PARTIAL` après commit.
- [ ] `CaseAnalysisPartialResponse` (record backend) gagne un champ `analysisType` (AnalysisType enum).
- [ ] `CaseAnalysisQueryService.getPartialAnalysis` populate `analysisType` depuis l'entité.
- [ ] Frontend `CaseAnalysisPartialResponse` interface synchronisée avec le DTO backend (champ `analysisType?: 'STANDARD' \| 'ENRICHED'`).
- [ ] `applyPartial` lit `partial.analysisType` au lieu de hardcoder `'STANDARD'`.
- [ ] `subscribeToPartialEvents` traite `ENRICHED_ANALYSIS PARTIAL` exactement comme `CASE_ANALYSIS PARTIAL`, et `ENRICHED_ANALYSIS DONE` comme `CASE_ANALYSIS DONE`.
- [ ] Tests backend UT + Jest passent.

## Plan de test minimal

- **Backend UT** (`EnrichedAnalysisServiceTest`) :
  - U-01 : `consumeReAnalysis` invoque `analyzeWithSystemCacheStreaming` avec un callback ; pendant le streaming, `partial_state` est défini sur l'analyse enrichie ; à DONE, `partial_state` est purgé.
  - U-02 : si `analyzeWithSystemCacheStreaming` retourne null (mock test), fallback sur `analyzeWithSystemCache` — résultat persisté.
- **Backend IT** (existant `CaseAnalysisQueryServiceIT` ou similaire) : si déjà couvert pour standard, le `analysisType` field est ajouté à l'assertion. Sinon, ajout d'un cas spécifique enriched.
- **Frontend Jest** :
  - U-1 : `applyPartial({...analysisType: 'ENRICHED'})` → `synthesis().analysisType === 'ENRICHED'`.
  - U-2 : `applyPartial({...analysisType: undefined})` → `synthesis().analysisType === 'STANDARD'` (default).
  - U-3 : event `ENRICHED_ANALYSIS PARTIAL` déclenche `getPartial()` et `applyPartial()` (miroir CASE_ANALYSIS).
  - U-4 : event `ENRICHED_ANALYSIS DONE` déclenche `loadVersions()`.

## Tables / endpoints / composants impactés

- **Backend** :
  - `EnrichedAnalysisService.java` (modif `consumeReAnalysis` + ajout `persistPartialAndNotify`)
  - `CaseAnalysisPartialResponse.java` (ajout champ `analysisType`)
  - `CaseAnalysisQueryService.java` (populate `analysisType`)
  - Tests : `EnrichedAnalysisServiceTest.java` (+ existant ajusté si IT touche le DTO)
- **Frontend** :
  - `case-analysis.model.ts` (ajout `analysisType` sur interface `CaseAnalysisPartialResponse`)
  - `synthesis.component.ts` (`applyPartial` + `subscribeToPartialEvents`)
  - Tests : `synthesis.component.spec.ts`
- **Aucune migration** (`partial_state` existe déjà depuis 201).

## Hors périmètre

- Refonte du parseur incrémental `PartialJsonSectionExtractor` — pas concerné.
- Q&A async pour enriched (déjà couvert par F-185 SF-185-02).
- Auto-trigger après chaque doc — décision SF-185-04 reste en place.

## Analyse de cohérence transversale

- **Préoccupations transversales** :
  - **Workspace context** : aucun changement, le `getPartial` filtre déjà sur workspace via `findByIdAndDeletedAtIsNull` + check workspace owner.
  - **Plans / limites** : aucun changement.
  - **Routing / guards** : aucun changement.
- **Nouveau pattern UI** : aucun nouveau composant — réutilisation de la barre + chips de SF-190-01 sur le même bandeau, qui fonctionne dès lors que `partial_state` est alimenté côté backend.
- **Pattern backend partagé** : `persistPartialAndNotify` est dupliqué entre `CaseAnalysisService` et `EnrichedAnalysisService`. **Choix volontaire** : la duplication est minime (~25 lignes), localise les changements futurs, et chaque service publie sur son propre `JobType` enum value (logique métier différente). Si une 3ᵉ analyse de ce type émerge, on extraira un `PartialStatePersister` partagé. Pas de dette de convergence immédiate — pattern déjà testé en SF-185-01 et l'extracteur lui-même reste partagé (c'est lui le vrai cœur de la logique).
- **Impact par domaine métier** : transversal — infra UX du pipeline IA, identique 3 domaines × 2 pays.

## Contrat API

Endpoint inchangé : `GET /api/v1/case-files/{id}/case-analysis/partial`.

Réponse étendue (champ ajouté) :

```json
{
  "analysisId": "uuid",
  "version": 2,
  "analysisType": "ENRICHED",   // ← nouveau (STANDARD | ENRICHED)
  "status": "PARTIAL",
  "sections": { "faits": [...], "risques": [...] },
  "updatedAt": "2026-05-05T12:34:56Z"
}
```

Backward-compatible : le frontend existant ignore les champs supplémentaires.

Événements SSE déjà existants — ajout d'une nouvelle valeur de `jobType` traitée :
- `ENRICHED_ANALYSIS` × `PARTIAL` (nouveau, miroir de `CASE_ANALYSIS PARTIAL`)
- `ENRICHED_ANALYSIS` × `DONE` (déjà émis aujourd'hui, frontend doit l'écouter)

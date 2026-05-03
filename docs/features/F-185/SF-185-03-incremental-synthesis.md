# SF-185-03 — Synthèse incrémentale par document

## Objectif (1 phrase)
Déclencher automatiquement une synthèse provisoire dès le 1er document analysé (et après chaque DocumentAnalysis suivant), pour que l'avocat ait une 1ʳᵉ vision du dossier en 30-60 s au lieu d'attendre que tous les documents soient traités puis de cliquer manuellement.

## Comportement nominal

1. Avocat upload N documents → chacun déclenche un pipeline `DocumentAnalysis` (chunk → document) async.
2. Quand un `DocumentAnalysis` passe à DONE :
   - **NOUVEAU** : `DocumentAnalysisService.finalizeAnalysis` publie un `CaseAnalysisMessage(caseFileId, provisional=true)` après commit.
   - Garde-fou : skip si une autre `CaseAnalysis` est déjà PENDING/PROCESSING/PARTIAL pour le dossier (évite spam si plusieurs docs DONE simultanés).
3. Le worker `CaseAnalysisService.consumeCaseAnalysis` consomme et traite normalement, en marquant l'analyse `is_provisional = true` lors de la création.
4. La synthèse partielle apparaît au fil du streaming (réutilise SF-185-01) puis bascule en DONE provisoire.
5. Frontend `SynthesisComponent` affiche un bandeau or/navy *"Synthèse provisoire — générée automatiquement après l'analyse des premiers documents. Le verdict final sera disponible après votre déclenchement manuel d'une analyse complète."*
6. L'avocat clique manuellement "Analyser le dossier" → `CaseAnalysisCommandService.triggerCaseAnalysis` publie `CaseAnalysisMessage(caseFileId)` (constructeur 1-arg, default `provisional=false`) → analyse définitive remplace la provisoire.

## Cas d'erreur

- RabbitMQ indisponible au moment du commit → log warn, fail-open (l'avocat peut toujours déclencher manuellement).
- Plusieurs DocumentAnalysis DONE simultanés → garde-fou `existsByCaseFileIdAndAnalysisStatusIn` filtre les doublons en upstream ; même si 2 messages passent (race), `prepareCaseAnalysis` accepte mais crée juste 2 versions distinctes (peu coûteux, transparent).
- `RabbitTemplate.convertAndSend` lance → fail-open : log warn, l'analyse manuelle reste possible.

## Critères d'acceptation

1. ✅ Migration Liquibase 202 ajoute `case_analyses.is_provisional BOOLEAN NOT NULL DEFAULT FALSE`.
2. ✅ `CaseAnalysisMessage` accepte un flag `provisional` ; constructeur 1-arg conservé pour rétrocompat.
3. ✅ `CaseAnalysisService.prepareCaseAnalysis` propage `message.provisional()` sur la nouvelle row `case_analyses`.
4. ✅ `DocumentAnalysisService.finalizeAnalysis` publie un `CaseAnalysisMessage(caseFileId, true)` après commit, sous garde-fou anti-spam.
5. ✅ `CaseAnalysisResponse` expose `isProvisional` (camelCase) → frontend reçoit le flag.
6. ✅ `SynthesisComponent` affiche le bandeau "Synthèse provisoire" si `isProvisional=true` ET pas de streaming en cours.
7. ✅ Aucune régression sur les analyses manuelles existantes (`isProvisional=false` par défaut).
8. ✅ Tests unitaires backend (DocumentAnalysisServiceTest 2 nouveaux cas) + tests Jest existants verts.

## Plan de test

### Backend (UT)
- T-01 : `DocumentAnalysis` DONE → `rabbitTemplate.convertAndSend` appelé avec `CaseAnalysisMessage(caseFileId, true)`.
- T-02 : `DocumentAnalysis` DONE mais analyse déjà en vol (existsByCaseFileIdAndAnalysisStatusIn = true) → `rabbitTemplate.convertAndSend` jamais appelé.

### Smoke
- Upload 5 docs sur staging → après ~30 s (fin du 1er DocumentAnalysis), bandeau "Synthèse provisoire" + sections qui apparaissent.
- Cliquer "Analyser le dossier" → bandeau disparaît, verdict affiché normalement.

## Tables / endpoints / composants impactés

- **Backend** :
  - Migration `202-add-case-analyses-is-provisional.xml` (1 colonne)
  - Entité `CaseAnalysis` (champ `isProvisional`)
  - `CaseAnalysisMessage` (record étendu)
  - `CaseAnalysisService.prepareCaseAnalysis` (propage le flag)
  - `CaseAnalysisService.PreparedCaseAnalysis` (record étendu)
  - `CaseAnalysisResponse` (1 champ + back-compat constructor)
  - `DocumentAnalysisService` (auto-trigger + garde-fou)
- **Frontend** :
  - `case-analysis.model.ts` (champ `isProvisional` optionnel)
  - `synthesis.component.html` (bandeau)
  - `synthesis.component.scss` (style or/navy)
  - `decisional-tools-progress-banner.component.ts` (1 entrée Record manquante détectée par CI)

## Hors périmètre

- UX "complétude N/M documents" précise (juste un bandeau générique pour V1 — la valeur principale c'est l'auto-trigger).
- Versioning spécial des analyses provisoires (les versions s'incrémentent normalement).
- Suppression auto des analyses provisoires anciennes (housekeeping pour V2).

## Analyse de cohérence transversale

Réutilise :
- Pattern `provisional` flag ⇆ pattern `analysisType STANDARD/ENRICHED` (cohérent avec la sémantique d'analyses).
- Migration TEXT/colonne typée à la même structure que `partial_state` SF-185-01.
- Bandeau SCSS strictement parallèle au `streaming-callout` SF-185-01.

## Impact par domaine métier

Transversal — aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE), c'est de l'infra UX du pipeline IA.

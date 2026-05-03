# SF-185-04 — Rollback auto-trigger CaseAnalysis (SF-185-03)

## Objectif (1 phrase)
Revenir au comportement antérieur à SF-185-03 — `CaseAnalysis` déclenchée **uniquement** sur clic manuel "Analyser le dossier", supprimer toute infrastructure de synthèse provisoire devenue inutile.

## Motivation
SF-185-03 livrait un déclenchement automatique de `CaseAnalysis(provisional=true)` après chaque `DocumentAnalysis DONE`. Décision produit du 2026-05-03 : ce comportement est **conceptuellement faible** — une analyse de dossier n'a de sens que sur la jointure de plusieurs documents, alors que l'auto-trigger fire dès le 1er doc DONE et produit une synthèse mono-doc trompeuse. Le coût Anthropic supplémentaire ne se justifie pas si la valeur ajoutée est marginale. Retour au déclenchement manuel — modèle "pull" plus prévisible et économe.

## Comportement nominal après rollback

1. Avocat upload N documents → chacun déclenche un pipeline `DocumentAnalysis` (chunk → document) async — **inchangé**.
2. Quand un `DocumentAnalysis` passe à DONE → **plus aucun side-effect côté CaseAnalysis** (l'auto-trigger SF-185-03 est supprimé).
3. Avocat clique manuellement "Analyser le dossier" → `CaseAnalysisCommandService.triggerCaseAnalysis` publie `CaseAnalysisMessage(caseFileId)` → analyse lancée (1 seul appel par clic) — **comportement antérieur SF-185-03 restauré**.
4. Frontend `SynthesisComponent` n'affiche plus de bandeau "Synthèse provisoire" — chaque analyse est définitive.

## Ce qui est conservé de F-185
- ✅ **SF-185-01** : streaming SSE de la synthèse + persistance partielle (`partial_state`, `partial_analysis`) — indépendant de l'auto-trigger, valeur produit propre.
- ✅ **SF-185-02** : SSE event `QUESTION_GENERATION_DONE/FAILED` post-async — indépendant.

## Cas d'erreur
- Aucun nouveau cas d'erreur — on retire du code, on n'en ajoute pas.

## Critères d'acceptation

### Backend
1. ✅ `DocumentAnalysisService.finalizeAnalysis` ne publie plus de `CaseAnalysisMessage` après commit (méthode `triggerProvisionalCaseAnalysisAfterCommit` supprimée).
2. ✅ Injections `RabbitTemplate` + `CaseAnalysisRepository` retirées de `DocumentAnalysisService`.
3. ✅ Champ `isProvisional` retiré de l'entité `CaseAnalysis`.
4. ✅ Flag `provisional` retiré du record `CaseAnalysisMessage` (retour à 1-arg).
5. ✅ Flag `provisional` retiré de `CaseAnalysisService.prepareCaseAnalysis` + record `PreparedCaseAnalysis`.
6. ✅ Champ `isProvisional` retiré de `CaseAnalysisResponse` + factory `from(...)`.
7. ✅ Migration **203** ajoutée pour DROP `case_analyses.is_provisional` (rollback de la 202).
8. ✅ Tests SF-185-03 (`finalizeAnalysis_done_triggersProvisionalCaseAnalysis` + variante "skip") supprimés.
9. ✅ Tests `DocumentAnalysisServiceTest` existants verts après mise à jour signature constructeur.

### Frontend
10. ✅ Bandeau `provisional-callout` retiré de `synthesis.component.html`.
11. ✅ Style `.provisional-callout` retiré de `synthesis.component.scss`.
12. ✅ Champ `isProvisional?: boolean` retiré de `CaseAnalysisResult` (`case-analysis.model.ts`).
13. ✅ Aucune régression sur l'affichage de la synthèse définitive (déclenchée par clic).

### Pipeline
14. ✅ `mvn compile` + tests `DocumentAnalysisServiceTest` + `CaseAnalysisServiceTest` verts.
15. ✅ `npm run build` frontend vert.

## Plan de test

### Backend (UT)
- T-01 : `DocumentAnalysisServiceTest.finalizeAnalysis_done_persistsAnalysis` — toujours vert (cas DONE persisté, rien à propos de RabbitMQ).
- T-02 : `CaseAnalysisServiceTest.triggerCaseAnalysis_publishesMessage` — toujours vert (clic manuel).
- T-03 (suppression) : retirer les 2 tests SF-185-03 qui n'ont plus d'objet.

### Smoke staging
- Upload 3 docs sur staging → après ~30 s (1er DocumentAnalysis DONE), **aucun bandeau "Synthèse provisoire"**.
- Cliquer "Analyser le dossier" → analyse définitive lancée, verdict affiché normalement.
- Vérifier qu'aucune row `case_analyses` n'est créée automatiquement (uniquement après clic).

## Tables / endpoints / composants impactés

- **Backend** :
  - Migration `203-drop-case-analyses-is-provisional.xml` (1 colonne droppée)
  - Entité `CaseAnalysis` (suppression du champ `isProvisional`)
  - `CaseAnalysisMessage` (record réduit)
  - `CaseAnalysisService.prepareCaseAnalysis` + `PreparedCaseAnalysis` (flag retiré)
  - `CaseAnalysisResponse` (1 champ + back-compat constructor)
  - `DocumentAnalysisService` (auto-trigger + méthode helper retirés, 2 injections retirées)
  - `DocumentAnalysisServiceTest` (constructeur simplifié, 2 tests retirés)
- **Frontend** :
  - `case-analysis.model.ts` (champ `isProvisional` retiré)
  - `synthesis.component.html` (bandeau retiré)
  - `synthesis.component.scss` (style retiré)

## Hors périmètre

- Conservation possible de l'infrastructure pour un futur réenclenchement conditionnel (rejeté — code mort = bruit).
- Modification de SF-185-01 ou SF-185-02 (indépendantes, conservées telles quelles).

## Analyse de cohérence transversale

- **Auth/Principal** : non concerné.
- **Workspace context** : non concerné (rollback ne touche pas à l'isolation).
- **Plans/limites** : non concerné.
- **Navigation/routing** : non concerné.
- **Outil décisionnel métier** : non concerné — F-185 est de l'infra UX du pipeline IA.
- **Préoccupation transversale "nouveau pattern partagé"** : non concerné — on retire du code, on n'introduit pas de pattern.

## Impact par domaine métier

Transversal — aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE), c'est de l'infra UX du pipeline IA.

## Risques

- **Migration descendante** : la 203 drop la colonne `is_provisional`. Toutes les rows existantes en staging avaient `is_provisional=false` (la feature SF-185-03 est en prod depuis quelques heures, peu de provisoires créées) → pas de perte de donnée significative. Côté prod : SF-185-03 n'a jamais été déployée → aucun impact.
- **Compat descendante CaseAnalysisMessage** : le record passe de 2-arg à 1-arg. Le constructeur 1-arg existait déjà (back-compat SF-185-03), il devient le seul. Pas de rupture.

# Mini-spec — F-89 / SF-89-02 Stats par version — backend + version cards

---

## Identifiant

`F-89 / SF-89-02`

## Feature parente

`F-89` — Refonte UX de la comparaison d'analyses

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-89-02-stats-par-version`

---

## Objectif

Afficher dans les version cards de la page `/diff` le nombre de faits, points juridiques, risques et entrées timeline de chaque analyse, en stockant ces compteurs dans la table `case_analyses` (colonnes nullable) et en les peuplant à la création de chaque analyse.

---

## Comportement attendu

### Cas nominal

1. À chaque fois qu'une analyse DONE est persistée, le `CaseAnalysisService` calcule et stocke 5 compteurs sur l'entité `CaseAnalysis` :
   - `faitsCount` = taille de `analysisResult.faits`
   - `pointsJuridiquesCount` = taille de `analysisResult.pointsJuridiques`
   - `risquesCount` = taille de `analysisResult.risques`
   - `questionsOuvertesCount` = taille de `analysisResult.questionsOuvertes`
   - `timelineCount` = taille de `analysisResult.timeline`

2. L'endpoint `GET /api/v1/case-files/{id}/case-analysis/versions` retourne dans chaque `VersionSummary` les 5 compteurs (nullable — `Integer`, pas `int`).

3. Les analyses **existantes** (avant migration) ont ces colonnes à `NULL` — la version card les affiche sans les compteurs (pas d'erreur, pas de texte manquant).

4. Côté Angular, la version card affiche :
   - Si les compteurs sont non-null : `12 faits · 8 points · 3 risques`
   - Si null : rien (card inchangée visuellement)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Colonnes NULL (ancienne analyse) | Frontend n'affiche pas les stats — pas d'erreur |
| Analyse FAILED ou SKIPPED | Compteurs non calculés, colonnes NULL — normal |
| Erreur lors du calcul des compteurs | L'analyse est quand même sauvegardée — compteurs restent NULL (fail-open) |

---

## Critères d'acceptation

- [ ] Migration `039` ajoute 5 colonnes nullable `INTEGER` sur `case_analyses`
- [ ] `CaseAnalysis` entity expose les 5 champs `Integer` (nullable)
- [ ] `CaseAnalysisService` (ou équivalent) peuple les 5 compteurs lors de la persistance de l'analyse DONE
- [ ] `CaseAnalysisResponse.VersionSummary` contient les 5 compteurs (type `Integer`, nullable)
- [ ] `CaseAnalysisQueryService.listVersions()` mappe les compteurs dans le VersionSummary
- [ ] `CaseAnalysisVersionSummary` Angular (model frontend) contient les 5 champs optionnels
- [ ] La version card affiche `N faits · N points · N risques` si les compteurs sont non-null
- [ ] La version card n'affiche rien de cassé si les compteurs sont null
- [ ] Tests IT : `GET /versions` retourne les compteurs pour une analyse DONE récente
- [ ] Tests IT : `GET /versions` retourne null pour une analyse sans compteurs (simulation)

---

## Périmètre

### Hors scope

- Backfill des anciennes analyses (pas de migration UPDATE — colonnes nullable acceptées)
- `questionsOuvertesCount` dans l'affichage frontend (affiché en option, pas prioritaire)
- Compteurs sur les analyses FAILED/SKIPPED
- Modifier d'autres endpoints que `/versions`

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Modification |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/case-analysis/versions` | Oui | Ajout de 5 champs nullable dans la réponse |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | ALTER — ajout colonnes | `faits_count`, `points_juridiques_count`, `risques_count`, `questions_ouvertes_count`, `timeline_count` — toutes `INTEGER NULL` |

### Migration Liquibase

- [x] Oui — `039-add-analysis-counts.xml`

```xml
<addColumn tableName="case_analyses">
  <column name="faits_count"               type="INTEGER"/>
  <column name="points_juridiques_count"   type="INTEGER"/>
  <column name="risques_count"             type="INTEGER"/>
  <column name="questions_ouvertes_count"  type="INTEGER"/>
  <column name="timeline_count"            type="INTEGER"/>
</addColumn>
```

### Composants Angular impactés

- `CaseAnalysisVersionSummary` (model) — ajout de 5 champs `number | null`
- `AnalysisDiffComponent` — version card affiche les stats si non-null

---

## Plan de test

### Tests unitaires backend

- [ ] `CaseAnalysisQueryService` — `listVersions()` mappe les compteurs non-null correctement
- [ ] `CaseAnalysisQueryService` — `listVersions()` mappe les compteurs null sans erreur

### Tests d'intégration backend

- [ ] `GET /api/v1/case-files/{id}/case-analysis/versions` → réponse contient `faitsCount`, `pointsJuridiquesCount`, `risquesCount` non-null pour une analyse récente
- [ ] `GET /api/v1/case-files/{id}/case-analysis/versions` → champs null acceptés sans erreur 403/404

### Tests frontend

- [ ] Version card affiche `12 faits · 8 points · 3 risques` quand compteurs non-null
- [ ] Version card n'affiche pas de ligne stats quand compteurs null

### Isolation workspace

- [x] Applicable — déjà garantie par `listVersions()` existant (workspace check)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification additive (champs nullable), aucun changement de routing, d'auth ou de plans

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-89-01 — statut : done

---

## Notes et décisions

- Les compteurs sont peuplés **à la fin du pipeline IA** quand le résultat est parsé et sauvegardé. Le point d'injection exact est à déterminer à l'implémentation selon l'endroit où `analysisResult` est écrit sur l'entité.
- `Integer` (nullable) côté Java, `number | null` côté TypeScript — les deux représentent l'absence de données pour les vieilles analyses.
- On n'affiche que `faits`, `points` et `risques` dans la card (les 3 plus significatifs). `questionsOuvertes` et `timeline` sont stockés mais pas affichés pour ne pas surcharger la card.

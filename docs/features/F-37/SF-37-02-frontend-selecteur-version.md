# Mini-spec — F-37 / SF-37-02 Frontend sélecteur de version

---

## Identifiant

`F-37 / SF-37-02`

## Feature parente

`F-37` — Versioning des synthèses

## Statut

`in-progress`

## Date de création

2026-03-23

## Branche Git

`feat/SF-37-02-frontend-selecteur-version`

---

## Objectif

Afficher un sélecteur de version dans l'écran Synthèse permettant à l'avocat de naviguer entre les versions de synthèse, chaque version ayant ses propres questions IA isolées.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre l'écran Synthèse (`/case-files/:id/synthesis`).
2. Un sélecteur de version apparaît dans le header : `v1`, `v2 Enrichie`, `v3`… (ordre décroissant — plus récente en premier).
3. Par défaut, la version la plus récente est sélectionnée.
4. Lors du changement de version, la synthèse **et** les questions IA affichées sont rechargées depuis les endpoints de la version sélectionnée.
5. Le badge du header (`Synthèse enrichie` / `Synthèse initiale`) reflète l'`analysisType` de la version sélectionnée, pas les réponses aux questions.
6. Le chat libre reste **inchangé** (non lié aux versions).
7. Si une seule version existe, le sélecteur est visible mais désactivé (pas de dropdown à un seul item).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `GET /versions` retourne liste vide | Affichage "Synthèse non disponible" (comportement actuel préservé) |
| Version sélectionnée introuvable (404) | Snackbar d'erreur, retour à la version la plus récente |
| Une seule version | Sélecteur affiché mais non interactif (label seul) |

---

## Critères d'acceptation

- [ ] Sélecteur de version affiché dans le header de l'écran Synthèse
- [ ] Les versions sont listées en ordre décroissant (plus récente en tête)
- [ ] Chaque entrée affiche le numéro de version et un badge « Enrichie » si `analysisType === 'ENRICHED'`
- [ ] Sélection d'une version déclenche le rechargement de la synthèse ET des questions via les nouveaux endpoints
- [ ] Le badge header (`Synthèse enrichie` / `Synthèse initiale`) reflète `analysisType` et non la présence de réponses
- [ ] Le chat reste indépendant du sélecteur (aucun rechargement au changement de version)
- [ ] Si une seule version : sélecteur non interactif (label seul, pas de dropdown)
- [ ] `CaseAnalysisResult` étendu avec `version: number` et `analysisType: 'STANDARD' | 'ENRICHED'`
- [ ] Nouveau modèle `CaseAnalysisVersionSummary` dans `case-analysis.model.ts`
- [ ] `CaseAnalysisService` : nouvelles méthodes `getVersions()` et `getByVersion()`
- [ ] `AiQuestionService` : nouvelle méthode `getQuestionsByAnalysisId()`

---

## Périmètre

### Hors scope

- Sélecteur de version dans `CaseFileDetailComponent` (reste sur la dernière version — pas de changement)
- Modification du bouton « Voir la synthèse » (navigue toujours vers la dernière version)
- Indicateur `outdatedDocuments` lié aux versions (compare toujours avec la dernière synthèse)
- Suppression ou archivage d'une version depuis l'UI
- Pagination du sélecteur

---

## Technique

### Endpoints consommés

| Méthode | URL | Usage |
|---------|-----|-------|
| GET | `/api/v1/case-files/{id}/case-analysis/versions` | Charger la liste des versions |
| GET | `/api/v1/case-files/{id}/case-analysis/versions/{version}` | Charger une version précise |
| GET | `/api/v1/case-files/{id}/ai-questions?analysisId={uuid}` | Questions d'une version |
| GET | `/api/v1/case-files/{id}/case-analysis` | Dernière version au chargement initial (rétrocompatibilité) |
| GET | `/api/v1/case-files/{id}/ai-questions` | Questions dernière version au chargement initial |

### Tables impactées

Aucune (pure logique frontend).

### Migration Liquibase

- [x] Non applicable

### Modèles Angular

```typescript
// Ajout dans case-analysis.model.ts
export interface CaseAnalysisVersionSummary {
  id: string;
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
  updatedAt: string;
}

// Extension de CaseAnalysisResult
export interface CaseAnalysisResult {
  // champs existants…
  version: number;
  analysisType: 'STANDARD' | 'ENRICHED';
}
```

### Composants Angular impactés

- `SynthesisComponent` — ajout du sélecteur de version, logique de rechargement par version
- `CaseAnalysisService` — +`getVersions(caseFileId)`, +`getByVersion(caseFileId, version)`
- `AiQuestionService` — +`getQuestionsByAnalysisId(caseFileId, analysisId)`
- `case-analysis.model.ts` — +`version`, +`analysisType` sur `CaseAnalysisResult` ; +`CaseAnalysisVersionSummary`

---

## Plan de test

### Tests unitaires (Karma)

- [ ] `SynthesisComponent` : au chargement → version la plus récente sélectionnée
- [ ] `SynthesisComponent` : changement de version → `loadSynthesisForVersion()` et `loadQuestionsForVersion()` appelés
- [ ] `SynthesisComponent` : `analysisType === 'ENRICHED'` → badge header "Synthèse enrichie"
- [ ] `SynthesisComponent` : `analysisType === 'STANDARD'` → badge header "Synthèse initiale"
- [ ] `SynthesisComponent` : une seule version → sélecteur non interactif
- [ ] `SynthesisComponent` : versions vides → state "Synthèse non disponible"
- [ ] `SynthesisComponent` : chat non rechargé lors du changement de version

### Tests d'intégration

- [ ] Non applicable (pure logique frontend)

### Isolation workspace

- [ ] Non applicable — isolation garantie par le backend

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification localisée à `SynthesisComponent` et `CaseAnalysisService`

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de routing modifié

---

## Dépendances

### Subfeatures bloquantes

- SF-37-01 — statut : `in-progress` (endpoints backend requis)

### Questions ouvertes impactées

- [ ] Aucune

---

## Notes et décisions

- Le sélecteur utilise un `<select>` natif avec les tokens du design system, pas `MatSelect`, pour éviter les dépendances supplémentaires. À revoir si l'UI devient plus complexe.
- Le badge `Synthèse enrichie / initiale` change de sémantique : il reflète désormais l'`analysisType` backend et non la présence de réponses aux questions. Ce changement est voulu.
- Au chargement, on appelle `getVersions()` puis on sélectionne la v1 de la liste (index 0 = plus récente). Cela évite un double appel avec l'endpoint `GET /case-analysis`.

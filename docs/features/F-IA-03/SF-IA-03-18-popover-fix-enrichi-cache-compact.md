# Mini-spec — F-IA-03 / SF-IA-03-18 Popover : fix synthèse enrichie + cache + design compact

## Identifiant

`F-IA-03 / SF-IA-03-18`

## Feature parente

`F-IA-03` — Contrôle de cohérence sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-IA-03-18-popover-enrichi-fix-cache-compact`

---

## Objectif

Correctif suite à validation staging post SF-IA-03-17. Trois problèmes observés :

1. **Synthèse enrichie ne régénère pas les explications** : seul `CaseAnalysisService` déclenche `SourceExplanationGenerator`, pas `EnrichedAnalysisService`. Résultat : même après avoir relancé une synthèse enrichie, les explications restent vides → popover tombe en fallback "Ré-analysez le dossier".
2. **Cache frontend jamais invalidé** : `SourceExplanationService.getForCaseFile()` garde la première réponse HTTP en cache. Si l'utilisateur ouvre le dossier avant la ré-analyse, il voit toujours une réponse vide.
3. **Popover trop grand et pas globalement cliquable** : design 360 × ~300 px avec marges cumulées et vide quand contenu minimal. Seul le bouton est cliquable, pas l'ensemble de la carte.

---

## Comportement attendu

### 1. Synthèse enrichie déclenche la régénération

`EnrichedAnalysisService.finalizeEnrichedAnalysis()` appelle `SourceExplanationGenerator.generate(caseFile, enrichedAnalysis)` puis `SourceExplanationService.persist(enrichedAnalysis, data)` en mode fail-open (pattern identique à CaseAnalysisService).

Ainsi chaque nouvelle analyse (STANDARD ou ENRICHED) produit ses propres explications.

### 2. Cache frontend invalidé automatiquement

Le cache `SourceExplanationService.cache` est invalidé dans 2 cas :
- `ngOnChanges` des composants qui consomment `sourceExplanations` : quand l'input `aiData` change (signe d'une nouvelle analyse terminée côté parent), invalider + re-fetch.
- Après qu'un `CaseDashboardRefreshService.triggerRefresh()` est émis : le service abonne un handler qui invalide toutes les entrées de son cache.

### 3. Popover compact + card cliquable

- Largeur réduite à **300 px** (vs 360 px).
- Padding réduit à **12 px** (vs 16 px).
- Zones séparées par un simple filet `1px solid #F3F4F6` sans padding-bottom cumulé.
- Si `explanation=null` : **ne pas afficher la zone SOURCE** (plus de vide "Ré-analysez…") ; afficher directement le motif + bouton générique **"Voir dans la synthèse"**.
- Toute la card `.popover-card` devient cliquable quand une action est disponible (`cursor: pointer`, `role="button"`, accessible clavier via `tabindex="0"` + handler `Enter/Space`).

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Pas d'explication pour ce sourceKey | Popover affiche motif + bouton "Voir dans la synthèse" (navigation vers `/synthesis` sans scroll spécifique) |
| `EnrichedAnalysisService` KO (Haiku down ou timeout) | Fail-open : pas d'explanations générées, analyse enrichie reste DONE |
| Cache frontend invalidé pendant une requête en cours | Nouvelle requête remplace l'ancienne, front affiche la réponse la plus récente |

---

## Analyse de cohérence transversale

- [x] **Autres outils** : 10 outils concernés — correction appliquée via pattern partagé (composant popover, service cache, EnrichedAnalysisService backend).
- [x] **Autres pays** : FR + BE natif.
- [x] **Autres domaines** : 3 domaines natifs.
- [x] **Nouveau pattern UI/service partagé** : aucun nouveau. Modification des services/composants existants (SourceExplanationService cache + CoherencePopoverComponent styles + EnrichedAnalysisService hook).

### Décision

- [x] Étendu aux 3 cibles (10 outils × FR+BE × 3 domaines) nativement via les composants partagés.

---

## Critères d'acceptation

- [ ] `EnrichedAnalysisService.finalizeEnrichedAnalysis()` appelle `SourceExplanationGenerator.generate()` + `SourceExplanationService.persist()` en post-traitement fail-open.
- [ ] Test backend : enriched analysis DONE + Haiku mock renvoie 2 explanations → persistées avec `caseAnalysisId = enrichedAnalysis.id`.
- [ ] `SourceExplanationService` : méthode `invalidate(caseFileId)` existe (déjà) et est appelée **automatiquement** quand un composant détecte `aiData` qui change (nouvelle analyse arrivée).
- [ ] Chaque composant qui consomme `sourceExplanations` appelle `invalidate()` + re-fetch dans son `ngOnChanges` quand `aiData` bascule (firstChange=false).
- [ ] Popover : largeur ≤ 300 px, padding 12 px, filets fins entre zones.
- [ ] Popover : si `explanation=null`, la zone SOURCE est retirée (pas de message "Ré-analysez"), bouton "Voir dans la synthèse" affiché.
- [ ] Popover : toute la card est cliquable (curseur + handler click) quand action ≠ NONE. Accessibilité clavier préservée.
- [ ] Non-régression : 862 tests backend verts (+2-3 nouveaux), 974 specs frontend verts (adaptations sur popover spec).
- [ ] Build prod vert.

---

## Périmètre

### Hors scope

- Modification du prompt Haiku (inchangé depuis SF-IA-03-17).
- Modification du design général des autres composants.
- Remplissage rétroactif des dossiers analysés avant SF-IA-03-17 (backlog).

---

## Technique

### Backend

- `EnrichedAnalysisService.java` — ajouter le hook `SourceExplanationGenerator` dans le bloc `if (failure == null)` du `finalizeEnrichedAnalysis()`, avec try/catch fail-open.
- `EnrichedAnalysisService` injecte `SourceExplanationGenerator` + `SourceExplanationService` + `CaseFileRepository`.
- Aucune migration. Aucun nouveau DTO.

### Frontend

- `SourceExplanationService` : inchangé (la méthode `invalidate()` existe déjà).
- Chaque composant consommateur (`anciennete-section`, `licenciement-section`, `indemnite-comparatif-section`, `rupture-conv-section`, `partage-immobilier-section`, `calendrier-garde-section`, `divorce-checklist-section`, `immigration-title-decision-section`, `immigration-recours-section`, `immigration-work-right-section`) : dans `ngOnChanges`, si `changes['aiData']?.currentValue !== changes['aiData']?.previousValue` (et `!firstChange`), appeler `sourceExplanationService.invalidate(this.caseFileId)` puis `loadSourceExplanations()`.
- `CoherencePopoverComponent` : styles revus (300 px, 12 px padding, filets fins), fallback sans zone SOURCE + bouton "Voir dans la synthèse" générique, card cliquable.
- `CoherenceSourceNavigator` : nouvelle action `OPEN_SYNTHESIS` (générique, déjà implicite via `OPEN_QUESTIONS`/`OPEN_F96_LIST`, mais explicite pour le fallback).

---

## Plan de test

### Tests backend

- [ ] `EnrichedAnalysisServiceTest` (ou nouveau) : mock Haiku OK → explanations persistées pour l'enrichedAnalysis.
- [ ] Fail-open : Haiku throw → enriched analysis reste DONE.

### Tests frontend

- [ ] `CoherencePopoverComponent` : rendu fallback sans explanation → pas de zone SOURCE + bouton "Voir dans la synthèse".
- [ ] `CoherencePopoverComponent` : click sur la card entière → navigator appelé (quand action).
- [ ] Non-régression directive + 10 composants consommateurs.

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale** (auth/workspace/plans/navigation inchangés).

### Composants impactés

| Composant | Impact |
|---|---|
| `EnrichedAnalysisService` | Nouveau hook fail-open |
| `CoherencePopoverComponent` | Styles + template fallback |
| 10 composants outils | `ngOnChanges` enrichi (invalidation cache) |

---

## Dépendances

- `SF-IA-03-17 Done` — composant popover 3 zones.
- `SF-IA-03-15a Done` — infra SourceExplanationGenerator + Service.

---

## Notes

- **Pourquoi invalider le cache côté composant plutôt que côté service centralisé** : chaque composant sait quand son input `aiData` change (signal de ré-analyse). Un mécanisme global écoutant SSE `AnalysisStatusEvent` serait plus propre mais plus lourd pour cette SF.
- **Pourquoi card entière cliquable** : UX standard pour popovers d'information. Le bouton reste pour accessibilité clavier.

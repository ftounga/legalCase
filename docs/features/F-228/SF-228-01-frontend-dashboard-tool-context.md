# Mini-spec — F-228 / SF-228-01 Frontend — Harmoniser le ctx de `case-dashboard.openGenericTool()` avec celui de `decisional-tools-panel`

## Identifiant

`F-228 / SF-228-01`

## Statut

`draft` — 2026-05-09

## Branche Git

`feat/SF-228-01-frontend-dashboard-tool-context`

## Pattern de référence

`decisional-tools-panel.component.ts:670-683` — constructeur d'inputs `pistesRetenues` filtrés par `toolIdCible` côté panel F-IA-04.

---

## Objectif

Faire en sorte que tout outil décisionnel ouvert **depuis le dashboard décisionnel** (`<app-case-dashboard>`) reçoive les **mêmes inputs IA et alignements** (`pistesRetenues`, `piecesObtenues`, `risquesValides`, `aiQuestions`, `aiData`) que celui ouvert depuis le panel F-IA-04 (`<app-decisional-tools-panel>`).

---

## Comportement attendu

### Cas reproducteur (bug observé staging Immigration Chen 17, 2026-05-08)

1. Avocat lance une analyse + Synthèse enrichie
2. Sur la synthèse, valide 4 pistes stratégiques (`F-176` → `RetainedPisteAlignmentService`)
3. **1ère ouverture** : clique sur la tile "Titre de séjour recommandé" depuis le panel F-IA-04 → popup s'ouvre avec les 4 pistes affichées (bloc dédié `<app-immigration-title-decision-section>` SF-192-02) ✅
4. Ferme le popup
5. **2ème ouverture** : re-clique sur la même tile depuis le case-dashboard → popup s'ouvre **sans les pistes** ❌

### Root cause (vérifiée 2026-05-09)

`case-dashboard.component.ts:241-248` construit le ctx passé à `entry.inputs(ctx)` avec **6 champs uniquement** :
```ts
const inputs = entry.inputs({
  caseFileId: this.caseFileId,
  synthesis: this.synthesis,
  workspaceCountry: this.workspaceCountry,
  caseFileTitle: '',
  procedureChecks: this.procedureChecks,
  aiQuestions: this.aiQuestions,
});
```

Champs absents : `pistesRetenues`, `piecesObtenues`, `risquesValides`, `aiQuestionsAlignment` (F-196), `aiData`.

Côté `decisional-tools-panel.component.ts:670-683`, le ctx est complet : `pistesRetenues` filtré par `toolIdCible` est passé. C'est pour ça que la 1ère ouverture (depuis le panel) marche.

**Le bug touche TOUS les outils décisionnels ouverts depuis le dashboard, pas que F-IM-05.** F-228 est la correction transversale (vs un patch ciblé F-IM-05 qui laisserait le bug latent sur les autres outils).

### Cas nominal (après fix)

1-3 : identique
4. Ferme le popup
5. **2ème ouverture** depuis case-dashboard → popup s'ouvre **avec les 4 pistes affichées** ✅
6. Idem pour les pièces obtenues, risques validés, questions complémentaires, aiData : disponibles dans tout outil décisionnel ouvert depuis le dashboard

---

## Critères d'acceptation

- [ ] **CA-01** : `case-dashboard.component.ts` charge `RetainedPisteAlignmentService.getForLatestAnalysis(caseFileId)` au mount + sur SSE `refresh$`. Stocké dans `retainedPistes = signal<RetainedPisteAlignment[]>([])`. Fail-open silencieux (timeout 5s, 404, 5xx → `[]`).
- [ ] **CA-02** : idem pour `PieceManquanteAlignmentService.getForCaseFile(caseFileId)` → signal `piecesAlignment` (extrait `piecesObtenues` filtrées par `toolIdCible` au moment du `entry.inputs(ctx)`).
- [ ] **CA-03** : idem pour `RisqueAlignmentService.getForCaseFile(caseFileId)` → signal `risquesAlignment` (extrait `risquesValides` filtrés par `toolIdCible`).
- [ ] **CA-04** : idem pour `AiQuestionAlignmentService.getForCaseFile(caseFileId)` → signal `aiQuestionsAlignment`. Récupère aussi `piecesManquantes` via les questions répondues (cohérence F-196).
- [ ] **CA-05** : `aiData` lu depuis `synthesis.travailExtractedData` / `synthesis.immigrationExtractedData` / `synthesis.familleExtractedData` — déjà disponible via `this.synthesis` signal, pas de chargement supplémentaire requis.
- [ ] **CA-06** : ces 5 sources passées au ctx de `entry.inputs(ctx)` (`case-dashboard.component.ts:241`) avec **strict respect des types** déclarés dans `TOOL_REGISTRY` côté panel (interface partagée — vérifier symétrie key par key avec `decisional-tools-panel.component.ts:670+`).
- [ ] **CA-07** : tests Jest `case-dashboard.component.spec.ts` — pour chaque source (4 services), simuler chargement → `openGenericTool('F-IM-05-arbre-decisionnel-titre')` → vérifier que les inputs reçoivent les pistes filtrées + autres alignements.
- [ ] **CA-08** : test Jest dédié — fermeture du popup ne reset pas les signals d'alignement ; re-ouverture passe les mêmes inputs.
- [ ] **CA-09** : tests existants `case-dashboard.component.spec.ts` restent verts (~zéro régression sur les 4 tiles "résumé" qui ne passent pas par TOOL_REGISTRY).
- [ ] **CA-10** : pas de duplication de loaders avec le panel — extraire dans un helper `loadDecisionToolAlignments(caseFileId, services): Observable<{retainedPistes, piecesAlignment, risquesAlignment, aiQuestionsAlignment}>` partagé entre les 2 composants pour éviter la divergence. Chemin : `frontend/src/app/case-files/decision-tools-shared/decision-tool-alignments.loader.ts`.

---

## Périmètre

### Hors scope V1

- (a) Refactor TOOL_REGISTRY pour exposer une interface contractuelle stricte (`DecisionToolInputContext`) — V2, pour l'instant on duplique types.
- (b) Mécanisme de cache croisé entre case-dashboard et panel — V2, pour V1 chaque composant charge ses données indépendamment (~50 ms supplémentaires acceptables).
- (c) Migration des autres tiles "résumé" (`F-194-pieces-summary` etc.) — c'est le scope de F-229.

---

## Technique

### Fichiers à modifier

1. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.ts` — ajouter 4 signals + 4 loaders + enrichir `openGenericTool()` ctx.
2. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.spec.ts` — ajouter tests CA-07/08/09.
3. **Nouveau** `frontend/src/app/case-files/decision-tools-shared/decision-tool-alignments.loader.ts` — helper partagé qui retourne un `forkJoin` des 4 services avec fail-open par stream.
4. `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — refactor pour utiliser le helper partagé (parité). **Ne pas casser les tests existants** (135/135 verts → doivent rester 135/135).

### Aucune migration backend, aucun nouvel endpoint

Les 4 services existent déjà (F-176, F-194, F-195, F-196).

---

## Plan de test

### Tests Jest (~5 nouveaux + refacto existants)

- `case-dashboard.component.spec.ts` :
  - T-CA-07-A : `openGenericTool('F-IM-05-arbre-decisionnel-titre')` → inputs.pistesRetenues = pistes filtrées par `toolIdCible`
  - T-CA-07-B : idem pour piecesObtenues, risquesValides, aiQuestions
  - T-CA-08 : ouverture → close → ré-ouverture → mêmes inputs
  - T-CA-09 : tile résumé `RETAINED_PISTES_SUMMARY` cliquée → router navigate (comportement inchangé hors scope F-229)
- `decision-tool-alignments.loader.spec.ts` (nouveau) :
  - chaque service renvoie data → loader produit `{retainedPistes, piecesAlignment, risquesAlignment, aiQuestionsAlignment}`
  - 1 service timeout/4xx → fail-open par stream, les 3 autres remontent normalement

### Test manuel post-deploy staging

1. Dossier Immigration Chen 17 (cas reproducteur original)
2. Ouvrir le titre de séjour recommandé depuis le case-dashboard
3. Vérifier les 4 pistes affichées
4. Fermer + ré-ouvrir → les 4 pistes toujours affichées

---

## Dépendances

- F-176 SF-176-02 ✅ (RetainedPisteAlignmentService)
- F-194 SF-194-01 ✅ (PieceManquanteAlignmentService)
- F-195 SF-195-01 ✅ (RisqueAlignmentService)
- F-196 SF-196-01 ✅ (AiQuestionAlignmentService)

---

## Impact par domaine métier

Transversal — flux UI, aucune adaptation par domaine ni par pays.

---

## Analyse de cohérence transversale

- **Auth/Principal** : N/A (frontend).
- **Workspace context** : N/A (les services existants gèrent déjà l'isolation).
- **Plans/limites** : N/A.
- **Navigation/routing** : N/A — pas de modification de route.
- **Outil décisionnel métier** : ✅ **TOUS les outils décisionnels concernés** transversalement. Le symptôme observé sur F-IM-05 (Immigration Chen 17) est représentatif d'une classe de bugs latents sur l'ensemble du dashboard. **Classement de chaque outil** :
  - F-DT-XX (Travail FR/BE) : impactés (ouverture depuis dashboard sans alignements). Couvert par cette SF.
  - F-IM-XX (Immigration FR/BE) : impactés. Couvert par cette SF.
  - F-FA-XX (Famille FR/BE) : impactés. Couvert par cette SF.
- **Nouveau pattern partagé** : ✅ `DecisionToolAlignmentsLoader` (helper partagé). 2 consommateurs (case-dashboard + decisional-tools-panel) → centralisation justifiée. Évite la divergence future si on ajoute un 3ᵉ alignement (V2).

---

## Risques

- **Régression panel F-IA-04** : le refactor ajoute une indirection via le helper partagé. Mitigation = tests existants 135/135 doivent rester verts.
- **Légère duplication temporaire** des signals de chargement (panel ET dashboard les chargent indépendamment). Acceptable V1.

---

## Notes

- **Décision 2026-05-09** : duplicater les loaders côté case-dashboard plutôt que de partager les signals via service singleton — le panel et le dashboard sont 2 consommateurs indépendants, le caching croisé est V2 si signal terrain.
- **Décision 2026-05-09** : créer le helper partagé `DecisionToolAlignmentsLoader` plutôt que de copier-coller 4 `.subscribe()` dans le case-dashboard — symétrie avec le panel + extensibilité V2.
- **Origine** : bug staging Immigration Chen 17 rapporté par utilisateur 2026-05-08 — "Lorsque j'ai fermé la page, la popup du titre de séjour recommandé, dès que j'ai recliqué sur le bloc titre de séjour recommandé dans le dashboard décisionnel, je ne vois plus les pièces stratégiques à l'intérieur."

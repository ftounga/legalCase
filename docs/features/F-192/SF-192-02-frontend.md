# Mini-spec — F-192 / SF-192-02 Frontend — Sortie outils enrichie + badge card panel + tile dashboard

## Identifiant

`F-192 / SF-192-02`

## Feature parente

`F-192` — Propagation des pistes stratégiques retenues vers outils décisionnels + dashboard + autres blocs synthèse

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-192-02-frontend-retained-pistes-output`

## Contrat API importé de SF-192-01-backend

- `GET /api/v1/case-files/{id}/retained-pistes-alignment` → `RetainedPisteAlignmentResponse[]`
- `RetainedPisteAlignment { pisteId, texte, baseJuridique?, horizonTemporel?, conditions[], toolIdCible?, matchStatus: 'ALIGNED' | 'DIVERGENT' | 'NOT_ANALYZED' | 'NO_TARGET_TOOL' }`
- `CaseFileDashboardResponse.tiles[]` inclut désormais une tile `{ toolId: 'RETAINED_PISTES_SUMMARY', theme: 'DIAGNOSTIC', label: 'Stratégies retenues', primaryValue, secondaryValue?, alertLevel? }` quand ≥ 1 piste RETAINED

---

## Objectif

Côté frontend, afficher l'alignement entre les pistes 🟢 Retenue et les outils décisionnels Immigration (TITRE DE SÉJOUR RECOMMANDÉ + RECOURS IMMIGRATION) avec 3 cas (convergence / divergence / sans pistes), afficher un badge sur la card du panel F-IA-04, et rendre la nouvelle tile résumé dans le dashboard agrégé F-167. **Toutes ces lectures sont issues de la dernière `CaseAnalysis` DONE** : elles ne se rafraîchissent qu'après un run de Synthèse enrichie (cohérence F-176 stricte — cf. SF-192-01).

---

## Comportement attendu

### Cas nominal

1. Au montage du dossier, `CaseFileDetailComponent` (ou un service partagé) appelle `RetainedPisteAlignmentService.getForCaseFile(caseFileId)` → lit l'alignement persisté sur la dernière `CaseAnalysis` DONE → renvoie `RetainedPisteAlignment[]`. Cache local côté composant (signal).
2. `TOOL_REGISTRY` étendu : chaque entrée pertinente reçoit dans `inputs(ctx)` une nouvelle source `pistesRetenues: RetainedPisteAlignment[]` filtrée sur `toolIdCible === <toolId courant>`.
3. **Sortie de TITRE DE SÉJOUR RECOMMANDÉ** (`<app-immigration-title-decision-section>`) après clic « Analyser » :
   - **Cas Convergence** : pour chaque piste où `matchStatus = ALIGNED`, le titre correspondant dans `decision().recommendedTitles` reçoit un badge supplémentaire `🎯 Retenu par vous` à côté du badge `⭐ Recommandé` existant
   - **Cas Divergence** : sous la liste des titres recommandés, un nouveau bloc `🎯 Stratégies retenues par vous (non recommandées)` liste les pistes `matchStatus = DIVERGENT` avec leur texte + base juridique + conditions + un message d'explication court (« Cette stratégie n'apparaît pas dans le top recommandé. Vérifiez les conditions ci-dessus. »)
   - **Cas Aucune piste** : aucun bloc supplémentaire, comportement identique à aujourd'hui
   - **Cas NOT_ANALYZED** : la piste apparaît dans le bloc « Stratégies retenues » avec mention `⏳ Outil pas encore lancé — relancez l'analyse pour comparer.`
4. **Sortie de RECOURS IMMIGRATION** (`<app-immigration-recours-section>`) : pattern miroir.
5. **Card du panel F-IA-04** (`decision-tool-card`) : nouveau badge optionnel `🎯 Aligné stratégie retenue (N)` ou `🎯 Divergence stratégie retenue (N)` à côté du badge `auto_awesome` existant. Calculé via static helper `getRetainedPistesBadge(input: { pistesRetenues, alignmentResult? }): { kind: 'aligned' | 'divergent' | 'none', count: number }` exposé par chaque composant outil concerné. Pattern miroir de `getPrefillCount(input)` (SF-177-12).
6. **Tile dashboard** : `<app-dashboard-tile>` rend la tile `RETAINED_PISTES_SUMMARY` comme les 85 autres. Particularité : clic → scroll vers le bloc Pistes stratégiques de la synthèse via `router.navigate(['/case-files', id, 'synthesis'], { fragment: 'pistes-strategiques' })` ou équivalent.

### Cycle de rafraîchissement (cohérent F-176)

L'alignement et la tile **ne se rafraîchissent pas** quand l'avocat clique 🟢 Retenue/❌ Écartée sur une piste — la PUT statut reste un acte invisible côté UI (cohérent avec F-176 actuel). Le rafraîchissement intervient **après le run de Synthèse enrichie** :

- L'event SSE `ENRICHED_ANALYSIS DONE` (via `subscribeToPartialEvents` F-185/F-190) déclenche un re-fetch automatique de la nouvelle `CaseAnalysis`
- Le composant rafraîchit alors `pistesRetenues` via `RetainedPisteAlignmentService.getForCaseFile(id)` qui retournera l'alignement persisté frais
- `CaseDashboardRefreshService.triggerRefresh()` (déjà invoqué post-analyse) rafraîchit la tile dashboard
- Le badge sur card panel se met à jour automatiquement via le re-render des composants outils

**Avant le clic Synthèse enrichie** : aucun signal nouveau côté tile / badge / sortie outil. Comportement strictement F-176 actuel. C'est volontaire — éviter de masquer le moment de commit.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Endpoint `/retained-pistes-alignment` 404 (case file autre workspace) | Erreur silencieuse, `pistesRetenues = []`, blocs sortie vides, badges absents |
| Endpoint timeout / 500 | Idem fail-open, log warn console |
| `toolIdCible = null` (NO_TARGET_TOOL) | La piste n'apparaît dans aucune sortie d'outil, mais reste comptée dans la tile dashboard `RETAINED_PISTES_SUMMARY` |
| Aucune piste RETAINED | Tile absente du dashboard, blocs sortie outils vides, badges card absents — comportement actuel inchangé |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils décisionnels** : V1 instrumente F-IM-05 (TITRE) + F-IM-06 (RECOURS). Les autres outils Immigration (F-IM-07 droit travail déductif, F-IM-08 OQTF, F-IM-09 AES, F-IM-12 asile, F-IM-13 naturalisation, F-IM-14 BE, F-IM-17 régime algérien, F-IM-19 mineurs, F-IM-20 mesures éloignement) ne reçoivent pas de blocs sortie pistes V1 — ils n'ont pas de liste de recommandations classées (verdict booléen / scoring fixe).
- [x] **Autres pays** : `<app-immigration-title-decision-section>` est déjà couvert FR + BE via `country` interne. Le nouveau bloc consomme `pistesRetenues` indifférente au pays — pas de logique conditionnelle pays.
- [x] **Autres domaines** : V1 hors scope (cf. SF-192-01 backend — V2 si signal terrain).
- [x] **Autres UI patterns** : nouveau badge `🎯` réutilise le composant `decision-tool-card` (SF-177-13). Nouveau bloc UI sortie outil suit le pattern existant `<section class="td-section">` + `<header class="td-header">`. Pas de nouveau pattern à introduire — réutilise tooltips MatTooltip + couleurs DESIGN_SYSTEM.md.
- [x] **Autres flows transversaux** : navigation depuis tile dashboard vers fragment synthèse — vérifier que le routing `synthesis#pistes-strategiques` fonctionne (anchor scrollIntoView dans SynthesisComponent) ou utiliser `Router.navigate + scrollPositionRestoration`.

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** : nouveau modèle `frontend/src/app/core/models/retained-piste-alignment.model.ts` miroir du DTO backend
- [x] **Service** : nouveau `frontend/src/app/core/services/retained-piste-alignment.service.ts` (HttpClient.get + signal cache)
- [x] **Composants** : extensions `<app-immigration-title-decision-section>` + `<app-immigration-recours-section>` + `<app-decision-tool-card>` + tile dashboard rendering
- [x] **TOOL_REGISTRY** : 2 entrées étendues avec `pistesRetenues` dans `inputs(ctx)`
- [x] **Tests existants** : couverture Jest des 2 composants outils + decision-tool-card + dashboard tile

### Cas spécifique : nouvelle feature d'outil décisionnel

Cette SF n'introduit **pas** un nouvel outil décisionnel, elle étend la sortie de 2 outils existants. Mais les checks de la règle "nouveau composant Angular décisionnel" s'appliquent partiellement :

- [x] **Cohérence IA (F-IA-03)** : pas de nouveau champ avocat dans le formulaire — pas de nouvelle alerte F-IA-03 à introduire. Le bloc sortie est **affichage pur** (pas d'input). N'invalide pas la couverture F-IA-03 existante des 2 composants.
- [x] **Refresh dashboard (F-IA-02)** : **non concerné** par le PUT statut piste (cohérence F-176 stricte). Le `triggerRefresh()` post-analyse existant (déclenché à la fin du run de Synthèse enrichie) suffit à rafraîchir tile + badges + sortie outils. Le SynthesisComponent ne doit PAS appeler `triggerRefresh` au PUT statut.
- [x] **Pré-remplissage IA** : entrée formulaire inchangée — `prefillFromAi()` existant non touché. Les pistes retenues ne pré-remplissent PAS les champs `motif`/`situationFamiliale` (le fait déjà via `triggerEvents` + `aiData`).
- [x] **Persistance** : pas de nouveau champ persisté côté outil. La piste retenue est persistée par F-176 (table `strategic_options`).
- [x] **Static `getPrefillCount(input)`** : non concerné directement, mais ajout d'un static `getRetainedPistesBadge(input)` miroir.
- [x] **Validation F-IA-03 au changement** : pas de nouveau champ saisi → règle non applicable au bloc sortie pistes.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Nouveau service `RetainedPisteAlignmentService`** : utilisable par autres composants (synthesis, dashboard tile). Singleton injecté en root.
- [x] **Nouveau pattern de bloc « Stratégies retenues » en sortie outil** : extensible aux outils Famille/Travail V2. Documenter le pattern dans la mini-spec SF-192-02 pour réutilisation V2.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `<app-immigration-title-decision-section>` F-IM-05 | Oui | Intégré V1 — bloc sortie + badge card + entrée TOOL_REGISTRY |
| `<app-immigration-recours-section>` F-IM-06 | Oui | Intégré V1 — pattern miroir |
| `<app-immigration-work-right-section>` F-IM-07 | Non | Non applicable — déductif pur, pas de liste à comparer |
| Composants Famille (`<app-divorce-faute-section>`, etc.) | Oui mais V2 | Backlog F-192 V2 |
| Composants Travail | Oui mais V2 | Backlog F-192 V2 |
| `<app-decision-tool-card>` (panel F-IA-04) | Oui | Étendu V1 — nouveau badge `🎯` |
| `<app-case-dashboard>` tile rendering | Oui | Étendu V1 — pas de nouveau composant, juste mapping label `RETAINED_PISTES_SUMMARY` + clic → navigation synthèse |
| `<app-synthesis>` bloc Pistes stratégiques (F-176 SF-176-02) | Non — strictement inchangé V1 | Le PUT statut piste reste un acte sans rafraîchissement UI. Cohérence F-176 stricte. |

### Décision

- [x] Étendu à toutes les cibles applicables V1 (Immigration FR+BE)
- [x] SF parallèles : SF-192-01 backend (déjà rédigée) + SF-192-03 PDF
- [x] Backlog V2 pour Famille/Travail (cf. SF-192-01)

---

## Critères d'acceptation

- [ ] **CA-01** : `RetainedPisteAlignmentService.getForCaseFile(id)` charge l'alignement au montage du dossier, signal exposé pour consommation par les composants outils + dashboard
- [ ] **CA-02 sortie TITRE convergence** : sur un dossier avec une piste RETAINED `baseJuridique = L.421-14 CESEDA` et `matchStatus = ALIGNED`, après clic Analyser sur l'outil, le titre Passeport talent — Chercheur dans `decision().recommendedTitles` affiche **2 badges** : `⭐ Recommandé` et `🎯 Retenu par vous`
- [ ] **CA-03 sortie TITRE divergence** : sur un dossier avec une piste RETAINED `matchStatus = DIVERGENT`, après clic Analyser, un nouveau bloc `🎯 Stratégies retenues par vous (non recommandées)` apparaît sous la liste recommandée, avec le texte + baseJuridique + conditions + message d'explication
- [ ] **CA-04 sortie TITRE sans pistes** : sur un dossier sans piste RETAINED, comportement actuel inchangé (aucun bloc supplémentaire)
- [ ] **CA-05 sortie TITRE NOT_ANALYZED** : si une piste RETAINED a `matchStatus = NOT_ANALYZED`, elle apparaît dans le bloc « Stratégies retenues » avec mention « ⏳ Outil pas encore lancé »
- [ ] **CA-06 sortie RECOURS** : pattern miroir CA-02/03/04/05 sur `<app-immigration-recours-section>`
- [ ] **CA-07 badge card panel** : la card de F-IM-05 dans `<app-decisional-tools-panel>` affiche `🎯 Aligné stratégie retenue (N)` ou `🎯 Divergence stratégie retenue (N)` quand pertinent, à côté du badge `auto_awesome` existant
- [ ] **CA-08 tile dashboard** : la tile `RETAINED_PISTES_SUMMARY` est rendue dans le thème DIAGNOSTIC quand ≥ 1 piste RETAINED, avec `primaryValue = N retenues`, `secondaryValue = X en divergence`, `alertLevel` reflété visuellement
- [ ] **CA-09 tile clic** : clic sur la tile `RETAINED_PISTES_SUMMARY` → navigation vers `/case-files/{id}/synthesis` avec scroll vers le bloc Pistes stratégiques
- [ ] **CA-10 fail-open** : si l'endpoint backend timeout, les blocs sortie outils restent vides (pas de spinner persistant, pas d'erreur affichée à l'avocat — log console suffisant)
- [ ] **CA-11 OnPush + ChangeDetectorRef** : conformément à `feedback_onpush_subscribe_markforcheck.md`, les `subscribe()` qui mutent l'état affiché injectent `ChangeDetectorRef` et appellent `markForCheck()` dans `next:` ET `error:`
- [ ] **CA-12 PUT statut sans refresh frontend** : après `PUT /strategic-options/{id} { statut: RETAINED }` depuis SynthesisComponent, **aucun appel** `CaseDashboardRefreshService.triggerRefresh()` n'est déclenché — la tile/badge/sortie outil restent reflétant l'état de la dernière analyse DONE jusqu'au prochain run de Synthèse enrichie (cohérence F-176 stricte)
- [ ] **CA-13 refresh au run synthèse enrichie** : à la réception de l'event SSE `ENRICHED_ANALYSIS DONE`, le composant re-fetche `getForCaseFile(id)` et la tile dashboard / badges sont mis à jour
- [ ] **CA-14 visuel charte** : badge `🎯` palette navy/or DESIGN_SYSTEM.md (icône `push_pin` Material), pas de rouge dominant, alignement avec badge `auto_awesome` existant

---

## Périmètre

### Hors scope (explicite)

- (a) Composants Famille / Travail (V2)
- (b) Animation pulse au mount du badge (V2 si signal terrain)
- (c) Drag-and-drop entre statuts pistes (déjà hors scope F-176)
- (d) Personnalisation de la couleur du badge `🎯` selon `alertLevel` (V1 = couleur fixe or, V2 si retour terrain)
- (e) Mode édition inline du texte de la piste depuis la sortie outil (l'avocat doit retourner dans la synthèse pour éditer)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| pistesRetenues signal | `[]` | Vide tant que l'endpoint n'a pas répondu |
| alignmentLoaded signal | `false` | Devient `true` après réponse 200 ou erreur (fail-open) |
| `getRetainedPistesBadge(input).kind` | `'none'` | Tant que pas de piste RETAINED matchée pour le toolId |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `matchStatus` reçu du backend | Oui | — | Enum strict 4 valeurs | Non | — |
| `toolIdCible` reçu | Non | 64 | Match clé `TOOL_REGISTRY` ou null | Non | — |
| Badge label affiché | Oui | 50 caractères max | Texte trimé | Non | trim() |

Notes :
- Si le backend renvoie un `matchStatus` inconnu (futur ajout), traité comme `NOT_ANALYZED` côté frontend (defensive coding).
- Si `toolIdCible` est une clé inconnue de `TOOL_REGISTRY`, traité comme `null` côté frontend (alignement non rendu sur card).

---

## Technique

### Endpoint(s) consommés

| Méthode | URL | Source |
|---------|-----|--------|
| GET | `/api/v1/case-files/{id}/retained-pistes-alignment` | Nouveau (SF-192-01) |
| GET | `/api/v1/case-files/{id}/dashboard` | Existant — réponse étendue (SF-192-01) avec tile `RETAINED_PISTES_SUMMARY` |

### Composants Angular impactés

- `RetainedPisteAlignmentService` (nouveau) — `core/services/retained-piste-alignment.service.ts`
- `retained-piste-alignment.model.ts` (nouveau) — interface miroir DTO
- `<app-immigration-title-decision-section>` (étendu) — `@Input() pistesRetenues?`, computed `retainedPistesAligned`/`retainedPistesDivergent`, template ajout bloc HTML, badge fusion, static `getRetainedPistesBadge`
- `<app-immigration-recours-section>` (étendu) — pattern miroir
- `<app-decision-tool-card>` (étendu) — `@Input() retainedPistesBadge?: { kind, count }`, template ajout pill or `🎯`
- `<app-decisional-tools-panel>` (`TOOL_REGISTRY` étendu) — `pistesRetenues` ajouté dans `inputs(ctx)` pour F-IM-05 + F-IM-06
- `<app-case-dashboard>` (`<app-dashboard-tile>` rendering) — mapping toolId `RETAINED_PISTES_SUMMARY` → label "Stratégies retenues" + handler clic → `router.navigate(['/case-files', id, 'synthesis'], { fragment: 'pistes-strategiques' })`
- `<app-synthesis>` (étendu si nécessaire) — appel `CaseDashboardRefreshService.triggerRefresh()` après PUT statut piste vers RETAINED

### Migration / config

- [x] Aucune migration backend (couverte par SF-192-01)
- [x] Aucun changement TypeScript de routing (`/synthesis` existe déjà avec fragment supporté ou à ajouter via `<a id="pistes-strategiques">` dans le template)

---

## Plan de test

### Tests Jest

- [ ] `RetainedPisteAlignmentServiceTest` — `getForCaseFile(id)` GET réussi → signal mis à jour
- [ ] `RetainedPisteAlignmentServiceTest` — GET 404 → signal `[]`, pas d'erreur thrown
- [ ] `RetainedPisteAlignmentServiceTest` — GET 500 → signal `[]`, log warn
- [ ] `ImmigrationTitleDecisionSectionComponentTest` — pistesRetenues vides → bloc sortie absent
- [ ] `ImmigrationTitleDecisionSectionComponentTest` — 1 piste ALIGNED → badge `🎯 Retenu par vous` rendu sur titre top-1
- [ ] `ImmigrationTitleDecisionSectionComponentTest` — 1 piste DIVERGENT → bloc séparé `Stratégies retenues par vous (non recommandées)` rendu
- [ ] `ImmigrationTitleDecisionSectionComponentTest` — 1 piste NOT_ANALYZED → mention `⏳ Outil pas encore lancé`
- [ ] `ImmigrationTitleDecisionSectionComponentTest` — `getRetainedPistesBadge` 0 piste → kind `'none'`
- [ ] `ImmigrationTitleDecisionSectionComponentTest` — `getRetainedPistesBadge` 2 ALIGNED → kind `'aligned'`, count 2
- [ ] `ImmigrationTitleDecisionSectionComponentTest` — `getRetainedPistesBadge` 1 ALIGNED + 1 DIVERGENT → kind `'divergent'` (priorité divergence), count 2
- [ ] `ImmigrationRecoursSectionComponentTest` — pattern miroir (3 cas convergence/divergence/sans pistes)
- [ ] `DecisionToolCardComponentTest` — `retainedPistesBadge.kind = 'aligned'` → pill `🎯 Aligné stratégie retenue (N)` rendu
- [ ] `DecisionToolCardComponentTest` — `retainedPistesBadge.kind = 'divergent'` → pill `🎯 Divergence stratégie retenue (N)` rendu
- [ ] `DecisionToolCardComponentTest` — `kind = 'none'` → pas de pill
- [ ] `CaseDashboardComponentTest` — tile `RETAINED_PISTES_SUMMARY` présente → label "Stratégies retenues", primaryValue rendu, alertLevel correct
- [ ] `CaseDashboardComponentTest` — clic sur tile → `Router.navigate(['/case-files', id, 'synthesis'], { fragment: 'pistes-strategiques' })` appelé
- [ ] `CaseDashboardComponentTest` — pas de tile `RETAINED_PISTES_SUMMARY` → aucun rendu

### Tests d'intégration

- [ ] Pas d'IT côté frontend (couverts via SF-192-01 backend IT). Smoke E2E couvert via tests Jest des composants.

### Isolation workspace

- [x] Non applicable côté frontend pur — SF-192-01 backend gère l'isolation. Vérifier que le service frontend ne cache pas les données entre dossiers (signal scoped par caseFileId).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing frontend — **touché légèrement** : navigation vers `/case-files/:id/synthesis#pistes-strategiques` depuis tile dashboard. Vérifier que le fragment scroll fonctionne.
- [x] **Aucune préoccupation transversale critique majeure**

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `<app-immigration-title-decision-section>` | Extension @Input + bloc sortie + badge → 11 tests Jest existants à valider sans régression | Lancer suite Jest complète du composant |
| `<app-immigration-recours-section>` | Idem pattern miroir | Lancer suite Jest complète du composant |
| `<app-decision-tool-card>` | Nouveau @Input → tests existants à valider | Lancer suite Jest decision-tool-card (33 tests) |
| `<app-case-dashboard>` | Nouvelle tile rendering + clic handler | Lancer suite Jest dashboard (~20 tests) |
| `<app-synthesis>` | Possible ajout `triggerRefresh()` après PUT piste RETAINED | Lancer suite Jest synthesis (~120 tests F-162/F-176) |
| `TOOL_REGISTRY` | 2 entrées étendues `inputs(ctx)` | Lancer suite Jest decisional-tools-panel |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/auth.spec.ts` — pas concerné
- [ ] `e2e/smoke/workspace.spec.ts` — pas concerné
- [ ] `e2e/smoke/navigation.spec.ts` — vérifier nav vers `/synthesis#pistes-strategiques` fonctionne (étendre si pertinent)

---

## Dépendances

### Subfeatures bloquantes

- F-176 SF-176-01 / SF-176-02 — Terminée
- F-167 SF-167-01..05 — Terminée
- F-IA-04 — Terminée
- F-177 SF-177-13 — Terminée (pill or pattern réutilisé)
- **SF-192-01 backend** — contrat API figé (cf. section "Contrat API importé")

### Questions ouvertes impactées

- [ ] Aucune

---

## Impact par domaine métier

- **Droit du travail** : V1 hors scope (V2 si signal terrain — Famille + Travail traités ensemble en SF V2)
- **Droit immigration** : V1 couvert intégralement. FR + BE via les 2 composants existants `<app-immigration-title-decision-section>` (gate `country` interne) et `<app-immigration-recours-section>`
- **Droit famille** : V1 hors scope

L'asymétrie V1 reflète celle de SF-192-01 backend : la stratégie de matching baseJuridique → toolId est V1 sur Immigration. L'extension Famille/Travail nécessite un mapping métier (Code civil / Code du travail → outils décisionnels) qui mérite une SF dédiée.

---

## Parité des domaines métier

Cette SF n'introduit pas un outil décisionnel niveau ≥ 5 — extension UI d'outils existants. La règle de parité ne s'applique pas formellement, mais la décision V1 = Immigration only doit être **réévaluée** lors d'une éventuelle V2 :
- Si l'usage F-176 Pistes stratégiques sur dossiers Famille/Travail dépasse 30 % du total après 2 mois de prod, ouvrir F-192 V2.

---

## Notes et décisions

- **Décision 2026-05-06** : V1 = Immigration only (cohérent avec SF-192-01 backend). Famille/Travail V2.
- **Décision 2026-05-06 (rectif)** : **gating Synthèse enrichie strict** côté frontend aussi — le PUT statut piste ne rafraîchit pas tile/badge/sortie outil. Cohérence F-176 stricte. Le rafraîchissement intervient au run de Synthèse enrichie via les events SSE existants. Modèle mental clair pour l'avocat : « tague tes pistes, puis relance la synthèse enrichie pour voir l'app intégrer tes choix ».
- **Décision 2026-05-06** : badge `🎯 Aligné/Divergence` réutilise le pill or installé par SF-177-13 (cohérence visuelle premium). Icône `push_pin` Material, taille identique au pill `auto_awesome`.
- **Décision 2026-05-06** : la tile dashboard ne fait PAS apparaître le détail des pistes (juste le compteur). Le clic redirige vers la synthèse pour le détail. Évite de dupliquer l'UI complète.
- **Décision 2026-05-06** : pas de loader spinner sur le bloc sortie outil — fail-open silencieux. L'avocat ne perçoit pas l'absence de pistes comme une erreur (cas le plus fréquent = pas de pistes RETAINED).
- **Décision 2026-05-06** : pattern « Stratégies retenues par vous (non recommandées) » : utiliser un bloc `<section class="td-subsection retained-pistes-divergent">` clairement détaché du bloc recommandé, pour éviter la confusion visuelle. Border-left or pour rappel premium.

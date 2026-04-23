# Mini-spec — F-IA-04 / SF-IA-04-03 Intégration du panel dans `case-file-detail`

## Identifiant
`F-IA-04 / SF-IA-04-03`

## Feature parente
`F-IA-04` — Moteur d'affichage conditionnel des outils décisionnels

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IA-04-03-integration-case-file-detail`

---

## Objectif

Brancher `<app-decisional-tools-panel>` (livré en SF-IA-04-02) dans `case-file-detail.component.html` en remplaçant les 13 blocs `@if` hardcodés (domaine / pays / `typeRupture`). Supprimer les 4 `computed` signals devenus obsolètes. Étendre le registre du panel pour porter des inputs hétérogènes par outil. Ajouter une migration 106 qui aligne les règles de visibilité sur le comportement UX actuel (rétrocompatibilité stricte).

---

## Comportement attendu

### Cas nominal
1. `case-file-detail` passe `<app-decisional-tools-panel>` en remplacement des 13 blocs `@if` (lignes 479–601), avec un contexte riche en inputs : `caseFileId`, `synthesis`, `workspaceCountry`, `caseFileTitle`, `procedureChecks`, `aiQuestions`.
2. Le panel fetch la visibilité via `GET /api/v1/case-files/{id}/decision-tools-visibility` (inchangé SF-IA-04-01).
3. Chaque tool_id résolu via `TOOL_REGISTRY` instancie un composant avec **ses inputs spécifiques** (la closure `inputs(ctx)` remplace le mécanisme uniforme `{ caseFileId, synthesis }`).
4. Les outils du `catalog` (CONTEXTUAL non déclenchés) s'affichent comme chips désactivés (comportement SF-IA-04-02 inchangé).
5. La rétrocompat UX est préservée grâce à la migration 106 qui ajuste les seeds (voir section dédiée).

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Endpoint 404 / 500 | snackbar erreur + 3 listes vides (comportement SF-IA-04-02) |
| `tool_id` inconnu du registre | log warning + skip |
| Composant instancié plante au render | Angular throw remonte à l'`ErrorHandler` global existant — pas de try/catch local |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|---|---|---|
| 13 blocs `@if` de `case-file-detail.component.html` | Oui — c'est le cœur du job | **Intégré** |
| Composants décisionnels avec inputs hétérogènes (12 composants sur 14) | Oui | **Intégré** via closures `inputs: (ctx) => {...}` dans le registre |
| `computed` signals `showValiditeLicenciement`, `showValiditeRuptureConv`, `showRuptureConvIndemnite`, `showRuptureAmiableInfo` | Oui — deviennent obsolètes | **Supprimés** |
| `app-rupture-amiable-info-section` (composant existe, pas dans le registre SF-IA-04-02) | Oui | **Intégré** au registre + seed CONTEXTUAL BE (migration 106) |
| Comparateur F-DT-09 actuellement affiché par défaut (`else` du `showRuptureConvIndemnite`) | Oui | **Migration 106 passe F-DT-09 ALWAYS_ON** pour préserver le défaut UX |
| F-DT-08 licenciement avec permissive default (`if (!type) return true`) | Oui | **Migration 106 passe F-DT-08 ALWAYS_ON** |
| Immigration : `app-immigration-*` affichés inconditionnellement aujourd'hui | Oui | **Migration 106 passe F-IM-01 et F-IM-06 ALWAYS_ON** |
| Famille : `app-partage-immobilier`, `app-calendrier-garde`, `app-divorce-checklist` affichés inconditionnellement | Oui | **Migration 106 passe F-FA-05, F-FA-06, F-FA-07 ALWAYS_ON** |
| Composants seedés mais sans front Angular (F-DT-01, F-DT-03, F-DT-05, F-FA-01, F-FA-02, F-FA-04, F-152, F-153) | Oui | Pas applicable dans cette SF — le panel skip avec warning (forward-compat SF-IA-04-02). Documenté ici pour transparence. |
| Dashboard F-IA-02 / `app-case-dashboard` | N/A — pas modifié ici. Intégration panel dans dashboard = SF-IA-04-04 | SF parallèle |
| Smoke tests E2E | Oui — route `/case-file/:id` est scannée par `smoke/navigation.spec.ts` | **Lancer `cd e2e && npm test`** avant push |

### Nouveau pattern UI

- Nouveau format d'entrée `TOOL_REGISTRY: Map<toolId, { component, inputs(ctx) }>` remplaçant le simple `Map<toolId, Type<unknown>>` de SF-IA-04-02. Évolution rétrocompatible-cassée du format ; **un seul consommateur existant** (`DecisionToolsPanelComponent`) à adapter.
- Le contexte `DecisionToolContext` est typé en TS pour éviter les accès implicites (`ctx.synthesis?.licenciementValidityDetection`).
- Pas de service séparé — le registre reste dans `DecisionToolsPanelComponent`.

### Décision
- [x] Étendu aux cibles applicables (registre avec inputs par outil, migration 106, suppression des computed signals)
- [x] SF parallèle : SF-IA-04-04 pour l'intégration dashboard
- [x] Hors scope : V2 pour l'admin UI des règles

---

## Impact par domaine métier

Cette SF touche les **3 domaines** (travail / immigration / famille) et les **2 pays** (FR + BE). Le mapping de la migration 106 :

| Tool | Domaine | Pays | Avant | Après 106 | Raison |
|---|---|---|---|---|---|
| F-DT-08 licenciement-validity | Travail | FR + BE | CONTEXTUAL (3 règles) | ALWAYS_ON (2 règles) | rétrocompat : permissive default frontend |
| F-DT-09 comparateur-indemnites | Travail | FR + BE | CONTEXTUAL (3 règles) | ALWAYS_ON (2 règles) | rétrocompat : outil générique par défaut |
| F-DT-10 rupture-conv-validity | Travail | FR | CONTEXTUAL | **Inchangé** | spécialiste RUPTURE_CONV |
| F-132-rupture-conv-indemnite | Travail | FR | CONTEXTUAL | **Inchangé** | spécialiste RUPTURE_CONV |
| F-132-rupture-amiable-info | Travail | BE | Absent | **NEW CONTEXTUAL** (RUPTURE_AMIABLE) | manque à SF-IA-04-01 |
| F-IM-01 checklist-pieces | Immigration | transversal | CONTEXTUAL (16 règles) | ALWAYS_ON (1 règle) | rétrocompat : toujours affiché |
| F-IM-06 recours | Immigration | transversal | CONTEXTUAL (6 règles) | ALWAYS_ON (1 règle) | rétrocompat : toujours affiché |
| F-FA-05 partage-immobilier | Famille | transversal | CONTEXTUAL (2 règles) | ALWAYS_ON (1 règle) | rétrocompat |
| F-FA-06 calendrier-garde | Famille | transversal | CONTEXTUAL (6 règles) | ALWAYS_ON (1 règle) | rétrocompat |
| F-FA-07 checklist-divorce | Famille | transversal | CONTEXTUAL | ALWAYS_ON (1 règle) | rétrocompat |

**Parti pris** : la philosophie F-IA-04 (CONTEXTUAL strict = affichage seulement sur détection IA) sera appliquée aux **outils futurs** (F-DT-11, F-DT-12, etc.). Les outils livrés AVANT F-IA-04 conservent leur comportement historique `toujours affiché` via ALWAYS_ON. Les deux spécialistes récents (F-DT-10, F-132) restent CONTEXTUAL parce qu'ils **complètent** un ALWAYS_ON parent (F-DT-08) sans le remplacer.

---

## Parité des domaines métier

Cette SF est **transversale** (moteur d'affichage), niveau 0-1 (pas un scoring/comparateur/détecteur). La règle "parité 3 domaines" ne s'applique pas directement. Néanmoins la migration 106 ajuste les 3 domaines en parallèle et la même logique (rétrocompat ALWAYS_ON pour les outils pré-F-IA-04) vaut pour les 3.

---

## Critères d'acceptation

- [ ] `DecisionToolsPanelComponent.TOOL_REGISTRY` passe du type `Map<string, Type<unknown>>` au type `Map<string, { component: Type<unknown>; inputs: (ctx: DecisionToolContext) => Record<string, unknown> }>`
- [ ] Interface TypeScript `DecisionToolContext { caseFileId: string; synthesis: any | null; workspaceCountry: string; caseFileTitle: string; procedureChecks: any[]; aiQuestions: any[] }` définie dans `decisional-tools-panel.component.ts`
- [ ] Entrée **NEW** `F-132-rupture-amiable-info` → `RuptureAmiableInfoSectionComponent` (14 → 15 entrées)
- [ ] Chaque entrée du registre fournit les inputs exacts que le composant consommait dans le template `case-file-detail` (mapping fidèle — vérifié ligne à ligne)
- [ ] Le panel expose 5 nouveaux `@Input` optionnels : `workspaceCountry`, `caseFileTitle`, `procedureChecks`, `aiQuestions` (`synthesis` et `caseFileId` déjà présents)
- [ ] `componentInputs(toolId)` (remplacé : plus de méthode sans paramètre) appelle la closure du registre avec le contexte courant
- [ ] `case-file-detail.component.html` lignes 479–601 (13 blocs `@if` + 7 appels de composants décisionnels) remplacés par un seul `<app-decisional-tools-panel>` placé après `<app-case-dashboard>`
- [ ] `case-file-detail.component.ts` : suppression de `LICENCIEMENT_TYPES`, `showValiditeLicenciement`, `showValiditeRuptureConv`, `showRuptureConvIndemnite`, `showRuptureAmiableInfo` (devenus inutiles)
- [ ] Migration **106-adjust-decision-tool-visibility-rules.xml** qui :
  - DELETE les règles CONTEXTUAL pour F-DT-08 (3), F-DT-09 (3), F-IM-01 (16), F-IM-06 (6), F-FA-05 (2), F-FA-06 (6), F-FA-07 (1) = 37 lignes
  - INSERT les règles ALWAYS_ON pour ces 7 outils (F-DT-08 × 2 pays, F-DT-09 × 2 pays, puis 5 règles transversales) = 9 lignes
  - INSERT F-132-rupture-amiable-info CONTEXTUAL BE type_rupture=RUPTURE_AMIABLE = 1 ligne
  - Total net = 53 − 37 + 9 + 1 = **26 règles** après migration
- [ ] Tests backend : `DecisionToolVisibilityServiceIT` inchangé côté logique. Ajouter 1 test nouveau : `caseFile_BE_travail_with_RUPTURE_AMIABLE_returns_F132_rupture_amiable_info`.
- [ ] Tests frontend Jest :
  - Mise à jour du spec existant (`decisional-tools-panel.component.spec.ts`) pour le nouveau registre
  - Test ajouté : `forwards tool-specific inputs to rendered component` (mock `F-DT-08-licenciement-validity` et vérifie que `workspaceCountry`, `procedureChecks`, `aiData` sont passés)
- [ ] Tests intégration `case-file-detail.component.spec.ts` : suppression des tests sur les 4 computed signals obsolètes
- [ ] Tests E2E `e2e/smoke/navigation.spec.ts` passent
- [ ] Build prod Angular vert
- [ ] Build Maven vert (1110+ tests)

---

## Périmètre

### Hors scope
- Intégration dans `app-case-dashboard` (F-IA-02) → **SF-IA-04-04**
- Activation manuelle d'un tool du catalog (clic désactivé aujourd'hui) → **V2**
- Gestion d'une exclusivité explicite entre F-DT-09 et F-132 (les deux s'affichent ensemble quand RUPTURE_CONV + FR) — c'est acceptable en V1, l'avocat voit l'outil générique + l'outil spécialiste
- Ajout de tools backend sans composant front (F-DT-01, F-DT-03, F-DT-05, F-FA-01, F-FA-02, F-FA-04, F-152, F-153) → skippés avec warning, branchement front = features futures

### Déjà fait ailleurs
- Fetch endpoint, empty state, chips catalogue : livrés en SF-IA-04-02

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `caseFileId` | Oui | UUID |
| `synthesis` | Non | objet synthèse ou `null` |
| `workspaceCountry` | Non | `'FRANCE'` \| `'BELGIQUE'`, défaut `'FRANCE'` |
| `caseFileTitle` | Non | string, défaut `''` |
| `procedureChecks` | Non | array, défaut `[]` |
| `aiQuestions` | Non | array, défaut `[]` |

---

## Technique

### Fichiers modifiés

**Frontend**
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — refactor du registre + nouveaux inputs
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.html` — `inputs: componentInputsFor(item.toolId)` au lieu de `inputs: componentInputs()`
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts` — mise à jour tests
- `frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` — remplacement lignes 479–601
- `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` — suppression des 4 computed + helper set LICENCIEMENT_TYPES
- `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` — nettoyage des tests des computed supprimés

**Backend**
- `backend/src/main/resources/db/changelog/migrations/106-adjust-decision-tool-visibility-rules.xml` — nouveau
- `backend/src/test/java/fr/ailegalcase/casefile/DecisionToolVisibilityServiceIT.java` — 1 test ajouté (RUPTURE_AMIABLE BE)

### Aucune route, aucun endpoint modifié.

---

## Plan de test

### Tests unitaires Jest (frontend)
- `renders F-DT-08-licenciement-validity with tool-specific inputs (workspaceCountry, procedureChecks, piecesManquantes)`
- `renders F-132-rupture-amiable-info for BE case files with RUPTURE_AMIABLE`
- `panel still renders 3 layers when visibility returns all filled` (conservé SF-IA-04-02)
- `unknown toolId skipped with warning` (conservé)

### Tests intégration Spring (backend)
- `caseFile_BE_travail_with_RUPTURE_AMIABLE_returns_F132_rupture_amiable_info_in_contextual`
- `caseFile_FR_travail_without_analysis_returns_F_DT_08_and_F_DT_09_always_on`
- `caseFile_FR_immigration_without_analysis_returns_F_IM_01_F_IM_05_F_IM_06_F_IM_07_always_on` (nouveau — preuve de la rétrocompat)

### Tests E2E smoke
- `cd e2e && npm test` → 3 suites (`auth`, `workspace`, `navigation`) doivent passer

### Isolation workspace
L'endpoint `GET /case-files/{id}/decision-tools-visibility` a déjà le filtre workspace_id (SF-IA-04-01). Non re-testé.

---

## Analyse d'impact

### Préoccupations transversales

- [ ] Auth / Principal : N/A
- [ ] Workspace context : N/A (déjà géré par l'endpoint)
- [ ] Plans / limites : N/A
- [ ] Navigation / routing : **Oui** — la route `/case-file/:id` est profondément modifiée. **Lancer les smoke tests E2E avant push**.
- [ ] Outil décisionnel : **Oui** — cette SF touche le moteur d'affichage de **tous** les outils décisionnels. Migration 106 scannée ci-dessus.

### Composants impactés listés

- `CaseFileDetailComponent` — template (lignes 479–601) + TS (4 computed signals supprimés)
- `DecisionToolsPanelComponent` — registre refactoré + inputs étendus
- `RuptureAmiableInfoSectionComponent` — ajouté au registre
- 13 composants décisionnels déjà affichés (inchangés — juste leurs inputs passent désormais par closure au lieu du template parent)
- Migration DB 106 impactant 7 tools × 2 pays maximum

### Smoke tests E2E
- [ ] À lancer **avant push** : `cd e2e && npm test`
- [ ] Couverture : login, switch workspace, navigation dossier → doit charger `case-file-detail` sans régression visible

---

## Dépendances
- **SF-IA-04-01** done (endpoint backend) ✓
- **SF-IA-04-02** done (composant panel + 14 entrées registre) ✓
- Aucune dépendance inter-SF restante avant SF-IA-04-03

---

## Notes et décisions

### Pourquoi pas de `null` safety défensive dans les closures d'inputs ?
Les composants cibles savent déjà gérer `synthesis?.licenciementValidityDetection === undefined` (testé depuis F-DT-08 original). Le registre se contente de forwarder, pas de valider.

### Pourquoi la migration 106 ne supprime pas TOUT le seed 105 et redémarre à zéro ?
Pour minimiser le diff opérationnel en staging/prod. 37 DELETE + 10 INSERT est plus compact et lisible qu'un DELETE global + 26 INSERT. Les rollbacks Liquibase restent fonctionnels.

### Pourquoi F-DT-10 et F-132 restent CONTEXTUAL alors que F-DT-08/F-DT-09 passent ALWAYS_ON ?
F-DT-08 et F-DT-09 étaient **toujours affichés** avant F-IA-04 (modulo un filtre permissive). F-DT-10 (rupture-conv validity) et F-132 (rupture-conv indemnite) sont **nouveaux outils spécialistes** ajoutés respectivement en F-DT-10 et F-132, qui n'apparaissent que sur détection `typeRupture = RUPTURE_CONVENTIONNELLE`. Préserver CONTEXTUAL reflète le design voulu par ces features.

### Pourquoi accepter l'affichage parallèle F-DT-09 + F-132 sur RUPTURE_CONV FR ?
- Avant F-IA-04 : template exclusif (F-132 OU F-DT-09)
- Après : les deux s'affichent en parallèle (F-DT-09 = comparateur générique, F-132 = spécialiste rupture-conv)
- Rupture UX légère, **acceptable** pour cette SF. Si l'avocat la flag comme gênante, on ouvrira une SF de "masquage par exclusion" ou on ajoutera un attribut `hides: [toolIds]` au modèle `decision_tool_visibility_rules` (V2).

### Pourquoi pas d'exclusive rule pour `rupture-amiable-info` vs comparateur F-DT-09 sur BE ?
Même rationale : les deux peuvent cohabiter. L'info rupture-amiable est courte (2 paragraphes), la coexistence ne nuit pas.

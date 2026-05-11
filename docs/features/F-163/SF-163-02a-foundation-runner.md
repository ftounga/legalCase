# Mini-spec — F-163 / SF-163-02a Foundation simulateur autonome (runner + 1 composant pilote)

## Identifiant

`F-163 / SF-163-02a`

## Feature parente

`F-163` — Outils décisionnels en mode simulateur autonome (hors dossier)

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-163-02a-foundation-runner`

---

## Objectif

> Livrer la **foundation technique** du mode simulateur autonome : (1) route `/simulators/:toolId` + composant `SimulatorRunnerPageComponent` qui charge dynamiquement le composant décisionnel approprié ; (2) Input `[standaloneMode]` ajouté à `DecisionToolsPanelComponent.DecisionToolContext` + propagé via `TOOL_REGISTRY.inputs(ctx)` ; (3) **1 composant pilote refactoré** (`LicenciementSectionComponent`, F-DT-08) qui démontre le pattern complet `[standaloneMode]` ; (4) `SimulatorsCatalogPageComponent` modifié pour naviguer vers le runner sur les outils whitelisted (clic → runner si dans whitelist, sinon dialog actuel).

Les ~106 autres composants restent en mode case-file scoped — SF-163-02b/c/d livreront leur refactor par vagues.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur clique sur la card **« Licenciement — validité (FR) »** dans `/simulators`.
2. `SimulatorsCatalogPageComponent.onCardClick()` détecte que `F-DT-08-licenciement-validity` est dans la `STANDALONE_READY_TOOL_IDS` whitelist → `router.navigate(['/simulators', toolId])`.
3. `SimulatorRunnerPageComponent` est instancié, lit `paramMap.get('toolId')`, croise avec `DecisionToolsPanelComponent.TOOL_REGISTRY` pour récupérer la `DecisionToolRegistryEntry`.
4. Si l'outil est dans la whitelist standalone : instancie dynamiquement le composant via `ngComponentOutlet` avec `inputs = { standaloneMode: true, caseFileId: null, workspaceCountry: <user country> }`.
5. Si l'outil **n'est PAS** dans la whitelist : affiche un message « Cet outil sera disponible en mode simulateur dans une prochaine version » + bouton retour `/simulators`.
6. Le composant pilote `LicenciementSectionComponent` détecte `standaloneMode=true` :
   - Affiche une bannière info navy : **« 🧪 Mode simulateur — les données saisies ne sont pas sauvegardées »**
   - Bypasse `prefillFromAi()` (`aiData` est `null`).
   - Désactive `coherenceAlerts` (computed retourne `{}` si standalone).
   - Permet la saisie du formulaire normalement.
   - Au submit du formulaire : POST `/api/v1/simulators/{toolId}/calculate` (contrat SF-163-03) au lieu de `/api/v1/case-files/{id}/licenciement`.
   - N'appelle PAS `CaseDashboardRefreshService.triggerRefresh()`.
   - Affiche le verdict in-memory uniquement.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `toolId` introuvable dans TOOL_REGISTRY | Page d'erreur 404 + bouton retour `/simulators` | n/a |
| `toolId` valide mais hors whitelist standalone | Message « Disponible bientôt » + retour | n/a |
| Backend `/api/v1/simulators/{toolId}/calculate` 500 | `MatSnackBar` erreur + le formulaire reste éditable | 500 |
| Backend 404 sur toolId inconnu côté backend | `MatSnackBar` « Calcul indisponible pour cet outil » | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils décisionnels** : les ~106 outils non couverts par cette SF gardent leur comportement actuel (case-file scoped). Whitelist `STANDALONE_READY_TOOL_IDS` initialement à 1 entrée (`F-DT-08-licenciement-validity`), étendue par SF-163-02b/c/d.
- [x] **Autres pays** : FR (composant pilote). BE traité dans SF-163-02d.
- [x] **Autres domaines** : Famille / Immigration traités dans SF-163-02c/d.
- [x] **Autres UI patterns** : `ngComponentOutlet` dynamique est déjà utilisé dans `decisional-tools-panel.component.html` (pattern réutilisé). MatSnackBar pour erreurs (charte standard).
- [x] **Autres flows transversaux** : `AuthGuard` réutilisé sur la nouvelle route. Pas de nouveau guard.

### Cas spécifique : nouveau pattern UI partagé

L'`Input [standaloneMode]` est un **nouveau pattern** ajouté à `DecisionToolContext` (et donc consommable par toutes les entrées `TOOL_REGISTRY`).

- [x] **Où peut être réutilisé** : tous les composants décisionnels (~106). SF-163-02b/c/d les rendront tous compatibles.
- [x] **Patterns concurrents** : aucun — pas de mécanisme existant pour outil hors dossier.
- [x] **Service partagé** : la directive `<app-coherence-popover-trigger>` doit accepter de ne rien afficher si la valeur d'alerte est vide (déjà le cas, comportement par défaut).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `LicenciementSectionComponent` (F-DT-08) | Oui | Refactor dans cette SF (pilote) |
| Autres composants TOOL_REGISTRY | Oui | SF-163-02b/c/d (3 vagues) |
| `DecisionToolContext` interface | Oui | Étendu dans cette SF avec `standaloneMode?: boolean` |
| `SimulatorsCatalogPageComponent` | Oui | Modifié dans cette SF (whitelist + navigation) |
| Backend dispatcher `/api/v1/simulators/{toolId}/calculate` | Oui | SF-163-03 (parallèle) |

### Décision

- [x] Étendu à la cible pilote (`LicenciementSectionComponent`) dans cette SF
- [x] SF parallèle SF-163-03 (backend dispatcher) figée par contrat
- [x] SF suivantes SF-163-02b/c/d planifiées (3 domaines, vagues parallèles après merge)

---

## Conformité F-IA-04

Le composant pilote `LicenciementSectionComponent` reste conforme aux 5 blocs F-IA-04 en **mode case-file** (inchangé). En **mode standalone** :

- [x] **Cohérence visuelle** : bannière info navy ajoutée, palette inchangée.
- [x] **Pré-fill IA** : bypassé (pas d'`aiData` en standalone) — comportement attendu.
- [x] **F-IA-03** : `coherenceAlerts` retourne `{}` (computed gate `!standaloneMode`).
- [x] **TOOL_REGISTRY symétrique** : `inputs(ctx)` reçoit `standaloneMode` via contexte, propagé au composant.
- [x] **Parité domaines** (niveau 5) : inchangé (le composant est niveau 5 scoring validité licenciement, mais le standalone ne change pas la logique métier — juste désactive les sources IA).

---

## Critères d'acceptation

- [ ] **CA-01** : route `/simulators/:toolId` accessible, protégée par `AuthGuard`, redirige `/login` si non auth.
- [ ] **CA-02** : `SimulatorRunnerPageComponent` lit `:toolId` du paramMap, croise avec `TOOL_REGISTRY`, instancie le bon composant via `ngComponentOutlet`.
- [ ] **CA-03** : `STANDALONE_READY_TOOL_IDS` exporté depuis `SimulatorRunnerPageComponent` (constante `Set<string>`) — init avec `['F-DT-08-licenciement-validity']`.
- [ ] **CA-04** : si `toolId` hors whitelist → message « Disponible bientôt » + bouton retour.
- [ ] **CA-05** : si `toolId` absent de TOOL_REGISTRY → page 404 + bouton retour.
- [ ] **CA-06** : `SimulatorsCatalogPageComponent.onCardClick()` modifié : si `toolId` dans whitelist → `router.navigate(['/simulators', toolId])` ; sinon → dialog actuel (rétrocompat).
- [ ] **CA-07** : `LicenciementSectionComponent` accepte `@Input() standaloneMode: boolean = false`.
- [ ] **CA-08** : en mode standalone, le composant affiche la bannière 🧪 et bypasse `prefillFromAi()` + `coherenceAlerts` + `triggerRefresh()`.
- [ ] **CA-09** : en mode standalone, le POST de validation pointe sur `/api/v1/simulators/F-DT-08-licenciement-validity/calculate` (au lieu de `/api/v1/case-files/{id}/licenciement`).
- [ ] **CA-10** : tests Jest existants `licenciement-section.component.spec.ts` toujours verts (mode case-file inchangé).
- [ ] **CA-11** : ≥ 4 nouveaux tests Jest couvrant : (a) standalone mode rendu, (b) bannière visible, (c) endpoint correct au submit, (d) pas d'appel triggerRefresh.
- [ ] **CA-12** : ≥ 4 nouveaux tests Jest sur `SimulatorRunnerPageComponent` : (a) toolId valide whitelisted, (b) toolId valide hors whitelist, (c) toolId inexistant, (d) navigation depuis catalogue.
- [ ] **CA-13** : isolation workspace — la résolution `workspaceCountry` du runner lit le workspace courant (même mécanisme que `SimulatorsCatalogService.getCatalog()`).

---

## Périmètre

### Hors scope (explicite)

- **Refactor des autres composants décisionnels** : SF-163-02b/c/d.
- **Dispatcher backend complet** : SF-163-03 (parallèle, contrat figé ci-dessous).
- **Historique des simulations** : non persisté V1, c'est la définition même du standalone mode.
- **Export PDF du résultat de simulation** : V2 (cas Fiche prud'homale par exemple) — hors scope ce sprint.

---

## Contrat backend consommé (figé par SF-163-03)

`POST /api/v1/simulators/{toolId}/calculate` — auth requise (MEMBER min)

**Request body** : payload spécifique à l'outil (ex. pour `F-DT-08-licenciement-validity` : structure existante `LicenciementRequest`).

**Response 200** : structure spécifique à l'outil (ex. `LicenciementResponse` existant).

**Erreurs** :
- 401 si non authentifié
- 404 si `toolId` inconnu du dispatcher
- 422 si payload invalide pour l'outil

> Le frontend de cette SF code seulement le pilote licenciement. Si SF-163-03 livre le dispatcher avec licenciement mais pas un autre outil, le frontend reste cohérent (la whitelist `STANDALONE_READY_TOOL_IDS` ne contient que les outils dont le backend est prêt).

---

## Technique

### Composants Angular (à créer)

- `frontend/src/app/simulators/simulator-runner-page.component.ts` (standalone, route `/simulators/:toolId`)
- `frontend/src/app/simulators/simulator-runner-page.component.html`
- `frontend/src/app/simulators/simulator-runner-page.component.scss`
- `frontend/src/app/simulators/simulator-runner-page.component.spec.ts`
- `frontend/src/app/simulators/standalone-ready-tools.ts` (export `STANDALONE_READY_TOOL_IDS: ReadonlySet<string>`)

### Composants Angular (à modifier)

- `frontend/src/app/case-files/licenciement-section/licenciement-section.component.ts` — ajout `@Input() standaloneMode: boolean = false`, conditions sur `prefillFromAi()` / `coherenceAlerts` / `triggerRefresh()` / endpoint POST.
- `frontend/src/app/case-files/licenciement-section/licenciement-section.component.html` — ajout bannière conditionnelle.
- `frontend/src/app/case-files/licenciement-section/licenciement-section.component.spec.ts` — ≥ 4 nouveaux tests.
- `frontend/src/app/core/services/licenciement.service.ts` — méthode `analyzeStandalone(payload)` qui POST sur le dispatcher (ou paramètre booléen sur la méthode existante).
- `frontend/src/app/simulators/simulators-catalog-page.component.ts` — `onCardClick` consulte `STANDALONE_READY_TOOL_IDS` pour décider navigate vs dialog.
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — `DecisionToolContext` étendu avec `standaloneMode?: boolean` ; `TOOL_REGISTRY` entry de Licenciement propage cet input.
- `frontend/src/app/app.routes.ts` — ajout route lazy `/simulators/:toolId`.

### Migration Liquibase

- [ ] Oui
- [x] Non applicable

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `SimulatorRunnerPageComponent` — toolId valide whitelisted → instancie le composant via ngComponentOutlet.
- [ ] `SimulatorRunnerPageComponent` — toolId valide hors whitelist → message « Disponible bientôt ».
- [ ] `SimulatorRunnerPageComponent` — toolId inexistant → 404.
- [ ] `SimulatorsCatalogPageComponent` — clic sur card whitelisted → navigate, clic hors whitelist → dialog.
- [ ] `LicenciementSectionComponent` — standalone=true → bannière 🧪 visible.
- [ ] `LicenciementSectionComponent` — standalone=true → `prefillFromAi()` non invoqué.
- [ ] `LicenciementSectionComponent` — standalone=true → POST sur `/api/v1/simulators/F-DT-08-licenciement-validity/calculate`.
- [ ] `LicenciementSectionComponent` — standalone=true → `triggerRefresh()` non invoqué.
- [ ] `LicenciementSectionComponent` — mode case-file (standalone=false par défaut) → 100% des tests existants verts.

### Isolation workspace

- [x] Applicable — runner résout le workspace courant via `SimulatorsCatalogService` ou équivalent.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — nouvelle route `/simulators/:toolId`.
- [ ] Autres préoccupations : aucune

### Smoke tests E2E concernés

- [x] `e2e/smoke/navigation.spec.ts` — vérifier que la nouvelle route ne shadow aucune route existante.

---

## Dépendances

### Subfeatures bloquantes

- **SF-163-01** — done (catalogue `/simulators` existe).
- **SF-163-03** — en parallèle, contrat figé ci-dessus.

### Notes et décisions

- **Décision** : whitelist `STANDALONE_READY_TOOL_IDS` exportée côté frontend uniquement (pas un appel backend) — plus simple, et le backend retourne 404 sur un toolId inconnu de toute façon (double sécurité). La whitelist sera étendue par SF-163-02b/c/d au fur et à mesure que les composants seront refactorés.
- **Décision** : `ngComponentOutlet` est préféré à un `switch` géant dans le template — réutilise le pattern existant de `DecisionToolsPanelComponent`.
- **Décision** : la bannière 🧪 est définie dans le composant (pas dans le runner) — chaque composant l'affiche conditionnellement à `standaloneMode`. Cohérent avec le pattern où la responsabilité de l'UX standalone reste dans le composant lui-même.

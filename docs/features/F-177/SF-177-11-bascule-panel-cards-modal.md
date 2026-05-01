# Mini-spec — F-177 / SF-177-11 Bascule du panel F-IA-04 vers cards + modal

## Identifiant

`F-177 / SF-177-11`

## Feature parente

`F-177` — Refonte panel F-IA-04 (cards verdict synthétique + ouverture modal)

## Statut

`draft`

## Date de création

2026-05-01

## Branche Git

`feat/SF-177-11-bascule-panel-cards-modal`

---

## Objectif

Câbler le panel F-IA-04 (`<app-decisional-tools-panel>`) sur les briques livrées par SF-177-01 (`<app-decision-tool-card>`) et SF-177-02 (`DecisionToolModalService`) — chaque outil rendu dans le panel s'affiche désormais comme une card cliquable qui ouvre le composant outil dans un MatDialog 90vw/90vh, à la place de l'expand inline `*ngComponentOutlet`.

---

## Position dans le découpage F-177

SF-177-11 est la **bascule finale** du panel — anticipée dès SF-177-03 (mini-spec ligne 50). Elle consomme :

- **SF-177-01** (mergée, PR #721) — composant `<app-decision-tool-card>`
- **SF-177-02** (mergée, PR #723) — `DecisionToolModalService.open()`
- **SF-177-03/03b/05/07** (mergées, PRs #725/#727/#728/#726) — instrumentation pattern B (statics `TOOL_LABEL`/`TOOL_ICON` + `@Input forceExpanded`) sur tous les composants outils des 3 domaines × 2 pays

Hors-scope F-177 résiduel après SF-177-11 :

- **SF-177-09** dashboard agrégé (absorbe F-167) — réutilise `<app-decision-tool-card>` en haut de page dossier
- **SF-177-10** polish SCSS legacy (absorbe F-168 bis) — nettoyage racine de 4 composants legacy

---

## Comportement attendu

### Cas nominal

1. Le panel reçoit la `VisibleToolSet` (always-on + contextual + catalog) du backend (inchangé).
2. Pour chaque outil résolu via `TOOL_REGISTRY`, le panel rend une `<app-decision-tool-card>` au lieu d'instancier le composant outil via `*ngComponentOutlet`.
3. La card affiche : icône + titre lus via `getToolMetadata(component)` (statics `TOOL_LABEL` / `TOOL_ICON`) — fallback `{ label: toolId, icon: 'extension' }` si un composant n'expose pas ses statics (forward-compat).
4. Au clic (ou Enter / Space) sur la card, le panel appelle `openTool(toolId, entry)` qui :
   - calcule les inputs via `componentInputsFor(entry)` (logique existante, inchangée)
   - ajoute `forceExpanded: true` au map d'inputs
   - délègue à `DecisionToolModalService.open()` avec `{ toolId, title, icon, component, inputs }`
5. Le modal MatDialog (90vw / 90vh, `panelClass: 'decision-tool-modal-panel'`) instancie le composant outil dans son `mat-dialog-content` via `*ngComponentOutlet`. Le composant honore `forceExpanded = true` (pattern B des SF d'instrumentation) et s'affiche déplié.
6. Pas de bouton "Enregistrer" dans le footer du modal (`onSave` non fourni → bouton caché — décision SF-177-02). Les composants outils gèrent leur propre persistence interne (boutons existants dans leur template).
7. La fermeture du modal (Esc, clic backdrop, bouton fermer header, "Annuler") rend le focus à la card cliquée.

### Cas d'erreur / forward-compat

| Situation | Comportement attendu |
|-----------|---------------------|
| Composant outil sans `TOOL_LABEL`/`TOOL_ICON` statics | Card affiche `{ toolId, icon: 'extension' }` — pas de crash, juste UI dégradée jusqu'à instrumentation |
| Composant outil sans `@Input forceExpanded` | `*ngComponentOutlet [inputs]` lève une erreur `setInput` Angular 19 → **bloquant**. Tous les composants en `TOOL_REGISTRY` doivent avoir l'input. Cette SF instrumente le seul composant manquant repéré (`RuptureAmiableInfoSectionComponent`, voir §Instrumentation résiduelle ci-dessous) |
| `toolId` inconnu (pas en `TOOL_REGISTRY`) | Skip silencieux + `console.warn` (logique `resolveEntry()` existante, inchangée) |
| Catalog (outils non encore activables) | Inchangé — chips `<div class="catalog-chip">` non bascules en cards (rendu inchangé) |
| Loading / empty / HTTP error | Inchangés — états `loading()`/`isEmpty()`/snackbar préservés |

---

## Instrumentation résiduelle obligatoire

Scan effectué le 2026-05-01 sur les composants présents dans `TOOL_REGISTRY` :

```bash
grep -L "TOOL_LABEL" frontend/src/app/case-files/*/*.component.ts
```

Tous les composants `TOOL_REGISTRY` exposent `TOOL_LABEL` / `TOOL_ICON` / `@Input forceExpanded`, **à une exception près** :

- `RuptureAmiableInfoSectionComponent` (toolId `F-132-rupture-amiable-info`, registry ligne 486)

→ SF-177-11 instrumente ce composant (pattern B identique à SF-177-03) pour éviter le crash `setInput` au runtime. C'est un mini-incrément cohérent avec le périmètre de la bascule (sinon la bascule planterait sur les dossiers BE rupture amiable). Justification de l'inclusion : sans cette instrumentation, le critère d'acceptation "100 % des cards ouvrent le modal sans erreur" est faux.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : tous les ~96 composants `*-section` du panel — la bascule s'applique de façon uniforme. Aucun n'est traité spécialement.
- [x] **Autres pays** : France + Belgique — la bascule est agnostique (le panel reste un fan-out par `TOOL_REGISTRY`)
- [x] **Autres domaines** : Travail + Famille + Immigration — agnostique
- [x] **Autres UI patterns** : voir §"nouveau pattern UI ou service partagé" ci-dessous
- [x] **Autres flows transversaux** : Auth/Workspace/Plans → non concernés. Navigation/routing → non concerné (le panel reste sur la même route, juste son rendu interne change)

### Niveaux de vérification

- [x] **Modèle TypeScript** : 2 nouvelles méthodes publiques `cardMetadataFor()` et `openTool()` dans `DecisionToolsPanelComponent`. Pas de modification de signatures publiques existantes.
- [ ] **Record / DTO backend** : non applicable
- [x] **Service / logique métier** : `DecisionToolModalService` injecté (existant, SF-177-02). `componentInputsFor()` réutilisé tel quel.
- [ ] **Entité JPA + schéma DB** : non applicable
- [x] **Tests existants** : tests panel `decisional-tools-panel.component.spec.ts` ne testent pas le rendu HTML des outils (ils testent `componentInputsFor`, `resolveEntry`, `themedTools`, refresh) — donc pas de régression. 2 tests neufs SF-177-11 ajoutés.

### Cas spécifique : nouveau pattern UI ou service partagé

SF-177-11 ne crée pas de nouveau composant ni de nouveau service. Elle consomme des briques existantes (SF-177-01/02). Pas d'analyse "pattern concurrent" requise.

### Cas spécifique : nouvelle feature d'outil décisionnel

SF-177-11 ne crée pas d'outil décisionnel — elle change le mode de rendu de tous les outils existants. Pas de F-IA-03 / F-IA-02 / pré-fill IA / persistance / masquage à traiter ici (déjà portés par les SF par outil).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Composants outils instrumentés (95) | Oui | Consommés par la bascule via `TOOL_REGISTRY` (rendu déclenché par clic) |
| `RuptureAmiableInfoSectionComponent` non instrumenté | Oui | **Instrumenté dans cette SF** (pattern B), correctif d'un oubli SF-177-03b |
| Dashboard agrégé (`<app-case-dashboard>`) | Oui | SF-177-09 (suite de la feature, ne dépend pas de SF-177-11) |
| Polish SCSS root 4 composants legacy | Oui | SF-177-10 (suite de la feature, indépendant) |
| Smoke tests E2E | Non | Pas de smoke E2E touchant le panel actuellement — la bascule reste vérifiable manuellement et par les tests Jest |

### Décision

- [x] Étendu à toutes les cibles applicables : la bascule s'applique uniformément aux 96 composants en `TOOL_REGISTRY` ; `RuptureAmiableInfo` instrumenté en plus
- [x] Subfeatures parallèles : SF-177-09 (dashboard) et SF-177-10 (polish SCSS) — backlog F-177 résiduel, pas de blocage
- [ ] Backlog VN : aucune cible reportée
- [ ] Non applicable aux autres cibles

---

## Critères d'acceptation

- [ ] Template `decisional-tools-panel.component.html` rend `<app-decision-tool-card>` au lieu de `*ngComponentOutlet` pour chaque outil résolu (always-on + contextual)
- [ ] Card reçoit `[toolId]`, `[theme]`, `[icon]`, `[title]` — calculés via `cardMetadataFor()`
- [ ] Card émet `(open)` → `openTool(toolId, entry)` invoqué
- [ ] `cardMetadataFor()` retourne `{ label, icon }` lus via `getToolMetadata(component)` quand statics présents
- [ ] `cardMetadataFor()` retourne fallback `{ label: toolId, icon: 'extension' }` quand statics absents
- [ ] `openTool()` calcule les inputs via `componentInputsFor()` puis ajoute `forceExpanded: true`
- [ ] `openTool()` délègue à `DecisionToolModalService.open()` avec `{ toolId, title, icon, component, inputs }` (pas de `onSave`)
- [ ] `RuptureAmiableInfoSectionComponent` expose `TOOL_LABEL` (`"RUPTURE AMIABLE"`), `TOOL_ICON` (`"handshake"`), `@Input forceExpanded = false`, applique `collapsed.set(false)` quand `forceExpanded === true` (ngOnInit + ngOnChanges)
- [ ] Section catalog inchangée (chips non bascules)
- [ ] États loading / empty / HTTP error inchangés
- [ ] Tests Jest panel : 1 test `cardMetadataFor()` couvre les 2 chemins (statics présents → label/icon ; statics absents → fallback)
- [ ] Tests Jest panel : 1 test `openTool()` mocke `DecisionToolModalService` et vérifie l'appel avec `forceExpanded: true` + le bon component / title / icon / toolId
- [ ] Test Jest dédié `RuptureAmiableInfoSectionComponent` : honore `forceExpanded = true` (collapsed devient false au mount + au passage de l'input)
- [ ] Tous les tests existants restent verts (`npm test` du frontend)
- [ ] Build Angular réussit (`npm run build`)
- [ ] Aucun warning/error nouveau dans la console au runtime (vérifié manuellement via `npm start` sur 1 dossier — golden path)

---

## Périmètre

### Hors scope (explicite)

- **SF-177-09** dashboard agrégé (réutilisera `<app-decision-tool-card>` en haut de page dossier)
- **SF-177-10** polish SCSS root des 4 composants legacy
- **Verdict synthétique sur card non-cliquée** : SF-177-01 expose les inputs (`summary`, `prefillCount`, `coherenceAlertCount`, `metierAlertLevel`) mais SF-177-11 ne les câble pas — le wiring nécessite que chaque composant outil expose son `summary` calculé depuis son `*Analysis`. C'est un incrément ultérieur (à planifier comme SF-177-12 / 13 / 14 par domaine si décidé). La card affiche pour l'instant uniquement icon + title (pattern visuel "tile" sans verdict). Les 3 badges (pré-fill, F-IA-03, métier) sont également hors scope.
- **Bouton "Enregistrer" dans le modal** : décision SF-177-02 → bouton caché si `onSave` undefined. Les composants outils conservent leurs propres CTA internes ("Calculer", "Sauvegarder", etc.).
- **Tests E2E** : pas de smoke test E2E ajouté (couverture par les tests Jest panel + tests Jest des composants).

---

## Valeurs initiales

Aucune entité créée.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Notes |
|-------|-------------|----------------------------|-------|
| `cardMetadataFor()` retour | Oui | `{ label: string; icon: string }` | Jamais `null` (fallback explicite) |
| `openTool()` arg `entry` | Oui | `DecisionToolRegistryEntry` | Provenance : itération `themedTools()` → garanti non-null |

---

## Technique

### Endpoint(s)

Aucun (frontend pur).

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `DecisionToolsPanelComponent` (modifié) — 2 méthodes ajoutées (`cardMetadataFor`, `openTool`), template basculé sur `<app-decision-tool-card>`, import `DecisionToolCardComponent` + `DecisionToolModalService` + `getToolMetadata`
- `RuptureAmiableInfoSectionComponent` (modifié) — pattern B ajouté (3 statics + 1 input + 2 hooks lifecycle)

---

## Plan de test

### Tests unitaires (Jest)

#### `decisional-tools-panel.component.spec.ts`

- [ ] **SF-177-11 T-01 cardMetadataFor — statics présents** : pour un toolId dont le composant a `TOOL_LABEL`/`TOOL_ICON` (ex. `F-DT-07-anciennete-conges-prime` → `AncienneteSectionComponent` → `'ANCIENNETÉ ET CONGÉS'` / `'calendar_month'`), vérifier `cardMetadataFor(entry, toolId)` renvoie ces valeurs.
- [ ] **SF-177-11 T-02 cardMetadataFor — fallback** : injecter un faux entry dont le composant n'a pas les statics → renvoie `{ label: toolId, icon: 'extension' }`.
- [ ] **SF-177-11 T-03 openTool** : mocker `DecisionToolModalService.open`, appeler `openTool('F-DT-07-anciennete-conges-prime', entry)`, vérifier appel avec :
  - `toolId === 'F-DT-07-anciennete-conges-prime'`
  - `title === 'ANCIENNETÉ ET CONGÉS'`
  - `icon === 'calendar_month'`
  - `component === AncienneteSectionComponent`
  - `inputs.forceExpanded === true`
  - `inputs.caseFileId === component.caseFileId`
  - pas de `onSave` (undefined)

#### `rupture-amiable-info-section.component.spec.ts`

- [ ] **SF-177-11 T-04 forceExpanded mount** : crée le composant avec `forceExpanded = true` → `collapsed()` est `false` après `ngOnInit()`
- [ ] **SF-177-11 T-05 forceExpanded change** : crée avec `forceExpanded = false`, replie manuellement, puis bascule l'input à `true` → `collapsed()` redevient `false`
- [ ] **SF-177-11 T-06 statics** : `RuptureAmiableInfoSectionComponent.TOOL_LABEL === 'RUPTURE AMIABLE'` et `TOOL_ICON === 'handshake'`

### Tests d'intégration

Non applicable (pas de backend).

### Isolation workspace

Non applicable (rendering pur).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — non
- [ ] **Workspace context** — non
- [ ] **Plans / limites** — non
- [ ] **Navigation / routing frontend** — non (la route `/case-files/:id` est inchangée — seul le mode de rendu interne du panel change)
- [x] **Aucune préoccupation transversale strictement listée** — bascule UI interne à un panel existant, pas de surface critique modifiée

> Bien que le périmètre touche tous les outils du panel, il s'agit d'un changement de **mode de présentation** (inline expand → modal). La logique métier (`componentInputsFor`, `resolveEntry`, `themedTools`, `triggerRefresh`) est inchangée.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `RuptureAmiableInfoSectionComponent` | Ajout pattern B (3 statics + 1 input + lifecycle hooks) | Spec dédiée (T-04/05/06) |
| Tous les composants outils (95) | Reçus dans un MatDialog au lieu d'un container inline → contraintes CSS différentes (largeur 90vw au lieu de demi-grille). Aucun changement de leur code propre. | Vérification manuelle golden path (1 outil par domaine × pays = 6 ouvertures) |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — les `e2e/smoke/*.spec.ts` couvrent auth/workspace/navigation, pas le rendu interne du panel.

---

## Impact par domaine métier

Cette SF est **transversale par construction**. Elle affecte de façon uniforme :

- **Travail FR** (~13 outils) — bascule cards + modal
- **Travail BE** (~3 outils) — idem
- **Immigration FR** (~13 outils) — idem
- **Immigration BE** (~5 outils) — idem
- **Famille FR** (~25 outils) — idem
- **Famille BE** (~2 outils) — idem

Aucun comportement spécifique par domaine ou par pays. Le `RuptureAmiableInfoSectionComponent` instrumenté concerne le contexte BE (rupture amiable), il s'inscrit dans la parité automatique de la bascule.

---

## Dépendances

### Subfeatures bloquantes

- [x] **SF-177-01** — `<app-decision-tool-card>` — done (PR #721 mergée)
- [x] **SF-177-02** — `DecisionToolModalService` — done (PR #723 mergée)
- [x] **SF-177-03 / 03b** — instrumentation Travail FR + BE — done (PRs #725 / #727 mergées)
- [x] **SF-177-05** — instrumentation Immigration FR + BE — done (PR #728 mergée)
- [x] **SF-177-07** — instrumentation Famille FR + BE — done (PR #726 mergée)

### Subfeatures débloquées

- SF-177-09 (dashboard agrégé) — peut consommer le même `DecisionToolModalService` librement
- SF-177-10 (polish SCSS) — désormais visible visuellement dans le modal une fois bascule

### Questions ouvertes impactées

- [x] Aucune question ouverte impactée

---

## Notes et décisions

- **Inclusion de l'instrumentation `RuptureAmiableInfo` dans cette SF** : décision pragmatique pour éviter un crash `setInput` Angular 19 sur le seul composant `TOOL_REGISTRY` non-instrumenté. Périmètre minimal (3 statics + 1 input + 2 hooks lifecycle). Sans cette inclusion, le critère d'acceptation "toutes les cards ouvrent le modal sans erreur" est faux pour les dossiers BE rupture amiable.
- **Pas de wiring du verdict synthétique sur la card** : décision actée — la card affiche aujourd'hui icon + title uniquement, sans `summary`/badges. Le wiring nécessite que chaque composant outil expose son verdict synthétique calculé depuis son `*Analysis`. Reportable en SF-177-12+ par domaine si jugé nécessaire après MEP.
- **Footer modal sans bouton Enregistrer** : conforme à la décision SF-177-02 (bouton caché si `onSave` non fourni). Les composants outils conservent leurs CTA internes ("Calculer", "Enregistrer interne", etc.). Le footer modal sert principalement à fermer (Annuler) ou à confirmer le save quand un outil le demande explicitement (pas le cas dans cette bascule).
- **`forceExpanded: true` propagé via `inputs`** : l'option d'utiliser un composant wrapper séparé (`<app-decision-tool-modal-host>` avec injection contextuelle) a été écartée car le pattern B des SF-03/03b/05/07 utilise déjà un input simple — réutilisation directe = zéro friction.
- **Card ne porte pas encore le `theme` visuel** : l'input `[theme]` est passé pour préparer un éventuel styling par thème métier (couleur d'accent), mais la SCSS de la card n'utilise pas encore cette information (cf. `decision-tool-card.component.scss`). Pas de décision visuelle prise ici — c'est la palette neutre de SF-177-01.

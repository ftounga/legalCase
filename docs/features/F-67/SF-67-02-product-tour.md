# Mini-spec — F-67 / SF-67-02 Product tour guidé

---

## Identifiant

`F-67 / SF-67-02`

## Feature parente

`F-67` — Wizard d'onboarding guidé

## Statut

`ready`

## Date de création

2026-03-29

## Branche Git

`feat/SF-67-02-product-tour`

---

## Objectif

Remplacer le dialog informatif SF-67-01 par un product tour interactif : une carte flottante non-bloquante qui se positionne à côté des éléments UI réels (boutons, zones) à chaque étape, avec surlignage de l'élément cible. 5 étapes couvrant le chemin critique + la synthèse.

---

## Comportement attendu

### Déclenchement

- `CaseFilesListComponent` appelle `TourService.start(workspaceId)` au `ngOnInit` si `TourService.shouldShow(workspaceId)` → le wizard dialog SF-67-01 n'est plus déclenché (remplacé)
- `TourOverlayComponent` est rendu dans `AppComponent` et s'affiche dès que `TourService.isActive()` est `true`

### Carte flottante

- Position fixe (CSS `position: fixed`, z-index élevé), non-bloquante (pas de backdrop)
- Contenu : icône + titre + description + boutons "Passer" et "Suivant" / "Commencer"
- Se repositionne automatiquement à chaque `NavigationEnd` et `window.resize`

### Étapes et cibles

| # | Titre | Cible `data-tour-target` | Route |
|---|-------|--------------------------|-------|
| 0 | Bienvenue | _(aucune — carte centrée en haut)_ | `/case-files` |
| 1 | Créer un dossier | `new-dossier-btn` | `/case-files` |
| 2 | Ajouter des documents | `upload-trigger-btn` | `/case-files/:id` |
| 3 | Lancer une analyse | `analyze-btn` | `/case-files/:id` |
| 4 | Lire la synthèse | `synthesis-link` | `/case-files/:id` |

### Positionnement

1. Chercher `document.querySelector('[data-tour-target="X"]')`
2. Si trouvé → `getBoundingClientRect()` → positionner la carte en dessous (ou au-dessus si peu de place) → appliquer la classe CSS `tour-highlight` sur l'élément (ring coloré)
3. Si non trouvé (mauvaise route, pas de dossier) → carte en position par défaut (coin bas-droite), pas de surlignage

### Progression et état

- `TourService` : signal `isActive`, signal `currentStep` (0–4), méthode `start(workspaceId)`, `next()`, `skip()`
- `next()` sur la dernière étape → `stop()` (= markDone + isActive = false)
- `skip()` → `stop()` immédiatement
- localStorage : clé `onboarding_tour_done_<workspaceId>` (distincte de SF-67-01 pour coexistence propre)

### Cas d'erreur / limites

| Situation | Comportement |
|-----------|-------------|
| Cible non trouvée (pas sur la bonne route) | Carte visible sans surlignage |
| localStorage indisponible | Tour non déclenché (fail-open) |
| Workspace ID absent | Tour non déclenché |
| Resize / scroll | Repositionnement automatique |

---

## Critères d'acceptation

- [ ] La carte flottante apparaît au 1er accès à `/case-files`
- [ ] Chaque étape surligne l'élément cible quand il est présent dans le DOM
- [ ] "Suivant" avance, "Passer" termine le tour
- [ ] "Commencer" (étape 4) termine le tour
- [ ] La carte se repositionne après navigation et resize
- [ ] Si la cible est absente, la carte s'affiche quand même (pas de crash)
- [ ] Après completion ou skip : localStorage marqué → tour ne réapparaît plus
- [ ] Le dialog SF-67-01 est remplacé (plus déclenché)
- [ ] La classe `tour-highlight` est retirée de tous les éléments à la fin du tour

---

## Périmètre

### Hors scope

- Animations de transition entre étapes
- Flèche/pointeur CSS depuis la carte vers l'élément
- Guide conditionnel selon domaine juridique
- Relance du tour depuis les settings

---

## Technique

### Composants et services

| Fichier | Rôle |
|---------|------|
| `TourService` | Signals `isActive`/`currentStep`, `start()`, `next()`, `skip()`, `shouldShow()`, `markDone()` |
| `TourOverlayComponent` | Carte flottante, positionnement via `getBoundingClientRect`, abonnement `Router.events` + `resize` |
| `AppComponent` | Intègre `<app-tour-overlay>` via `@if (tourService.isActive())` |
| `CaseFilesListComponent` | Appelle `tourService.start()` au lieu d'ouvrir le dialog |
| `CaseFilesListComponent` HTML | Attribut `data-tour-target="new-dossier-btn"` sur le bouton "Nouveau dossier" |
| `CaseFileDetailComponent` HTML | Attributs `data-tour-target` sur upload, analyser, synthèse |

### Attributs `data-tour-target` à ajouter

| Composant | Élément | Valeur |
|-----------|---------|--------|
| `case-files-list.component.html` | Bouton "Nouveau dossier" | `new-dossier-btn` |
| `case-file-detail.component.html` | Bouton "Ajouter des documents" | `upload-trigger-btn` |
| `case-file-detail.component.html` | Bouton "Analyser le dossier" | `analyze-btn` |
| `case-file-detail.component.html` | Lien "Voir la synthèse" | `synthesis-link` |

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires — `TourService`

- [ ] U-01 : `shouldShow` — clé absente → true
- [ ] U-02 : `shouldShow` — clé présente → false
- [ ] U-03 : `shouldShow` — workspaceId null → false
- [ ] U-04 : `start()` → `isActive() = true`, `currentStep() = 0`
- [ ] U-05 : `next()` → `currentStep() = 1`
- [ ] U-06 : `next()` sur étape 4 → `isActive() = false`, localStorage posé
- [ ] U-07 : `skip()` → `isActive() = false`, localStorage posé

### Tests composant — `TourOverlayComponent`

- [ ] U-08 : étape 0 — titre "Bienvenue" affiché
- [ ] U-09 : étape 4 — bouton "Commencer" affiché
- [ ] U-10 : clic "Passer" → `tourService.skip()` appelé
- [ ] U-11 : clic "Suivant" → `tourService.next()` appelé

### Tests contrat — non-régression attributs `data-tour-target`

Ces tests vérifient que les attributs `data-tour-target` sont toujours présents dans les templates. Ils échouent si quelqu'un supprime l'attribut d'un bouton cible.

- [ ] C-01 : `CaseFilesListComponent` — DOM contient `[data-tour-target="new-dossier-btn"]`
- [ ] C-02 : `CaseFileDetailComponent` — DOM contient `[data-tour-target="upload-trigger-btn"]`
- [ ] C-03 : `CaseFileDetailComponent` — DOM contient `[data-tour-target="analyze-btn"]` quand `canAnalyze()` est vrai
- [ ] C-04 : `CaseFileDetailComponent` — DOM contient `[data-tour-target="synthesis-link"]` quand `synthesis()` est non null

### Test E2E smoke — `e2e/smoke/tour.spec.ts`

- [ ] E-01 : Premier accès `/case-files` → carte flottante visible
- [ ] E-02 : Étape 0 → titre "Bienvenue" visible
- [ ] E-03 : Bouton "Suivant" → étape 1, élément `[data-tour-target="new-dossier-btn"]` a la classe `tour-highlight`
- [ ] E-04 : Bouton "Passer" → carte disparaît
- [ ] E-05 : Rechargement `/case-files` → carte non réaffichée

### Isolation workspace

Non applicable — feature 100% frontend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing** — `TourOverlayComponent` réagit aux `NavigationEnd`, abonnement dans `AppComponent`

### Composants impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|------------------------|
| `AppComponent` | Intègre `TourOverlayComponent` | Smoke test E-01 |
| `CaseFilesListComponent` | Remplace dialog par `tourService.start()` | U-04, E-01 |
| `CaseFilesListComponent` HTML | Ajout `data-tour-target` | C-01 |
| `CaseFileDetailComponent` HTML | Ajout 3 `data-tour-target` | C-02, C-03, C-04 |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — vérifier que la carte flottante ne bloque pas la navigation existante

---

## Dépendances

- SF-67-01 (done) — `OnboardingWizardService.shouldShow/markDone` réutilisé comme base (nouvelle clé localStorage)
- F-46 (done) — infra Playwright disponible dans `e2e/smoke/`

---

## Notes et décisions

- `data-tour-target` est l'API de contrat entre le tour et les composants. Toute suppression de cet attribut est détectée par les tests C-01 à C-04.
- La clé localStorage est `onboarding_tour_done_<workspaceId>` (≠ SF-67-01 `onboarding_wizard_done_`) pour que les deux coexistent proprement pendant la transition.
- SF-67-01 (`OnboardingWizardDialogComponent`) est conservé dans le code mais n'est plus déclenché — suppression en V3 si confirmé.
- Le positionnement ne gère pas les cas où l'élément est partiellement hors-viewport (hors scope V2).

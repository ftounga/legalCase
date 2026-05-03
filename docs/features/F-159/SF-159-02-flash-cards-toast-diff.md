# Mini-spec — F-159 / SF-159-02 Animation flash sur cartes enrichies + toast contextuel "N champs pré-remplis"

## Identifiant

`F-159 / SF-159-02`

## Feature parente

`F-159` — Progression visible des opérations IA dans le dashboard décisionnel

## Statut

`ready`

## Date de création

2026-05-03

## Branche Git

`feat/SF-159-02-flash-cards-toast-diff`

---

## Objectif

Quand une analyse IA (CASE_ANALYSIS / ENRICHED_ANALYSIS) se termine et que le panel décisionnel se rafraîchit, signaler visuellement à l'avocat **quelles cartes ont été enrichies** (animation flash 1,5 s sur les cards dont le `prefillCount` a augmenté) et confirmer la valeur ajoutée par un **toast contextuel cliquable** ("N champs pré-remplis dans M outils") qui ouvre le détail des outils impactés.

---

## Contexte hérité de SF-159-01

SF-159-01 a livré :
- Le bandeau sticky de progression `<app-decisional-tools-progress-banner>` qui s'affiche pendant qu'une analyse tourne et disparaît à la réception du SSE `*_DONE`.
- Le bug fix `AnalysisSseService` (stream qui ne se ferme plus après le 1er événement), qui a ré-activé le wiring `events$ → loadSynthesis()` existant ligne 374-387 de `case-file-detail.component.ts`. Conséquence : après un `CASE_ANALYSIS_DONE` ou `ENRICHED_ANALYSIS_DONE`, `loadSynthesis()` est appelé, `triggerRefresh()` est invoqué si la version a changé, et `loadVisibility()` recalcule la visibilité + les `prefillCount` du panel.

**SF-159-02 se branche sur ce flux existant.** Aucun nouveau bus d'événement, aucun nouveau SSE. On observe la transition `prefillCount` avant/après `loadVisibility()` et on en déduit le delta affiché à l'avocat.

---

## Comportement attendu

### Cas nominal — animation flash sur cartes enrichies

1. L'avocat déclenche une analyse (bouton "Analyser" / "Re-synthétiser" / upload). Le bandeau SF-159-01 s'affiche.
2. À la réception du SSE `CASE_ANALYSIS_DONE` ou `ENRICHED_ANALYSIS_DONE`, le wiring existant déclenche un `triggerRefresh()` qui invalide la visibilité du panel → `loadVisibility(false)` est appelé.
3. **Avant** que la nouvelle visibilité écrase l'état, le panel transmet au service un snapshot `Map<toolId, prefillCount>` représentant l'état en place immédiatement avant l'analyse.
4. **Après** que `loadVisibility()` a abouti, le panel transmet le nouveau snapshot.
5. Le service `DecisionalToolsProgressService` calcule le diff par toolId :
   - Si `prefillCount(toolId) > prefillCountPrécédent(toolId)` (incluant `null → N`) → le toolId est ajouté à `flashedToolIds` (signal exposé au panel).
   - Le set est purgé automatiquement 1500 ms après ajout.
6. Le panel passe `[flashing]="flashedToolIds().has(item.toolId)"` à `<app-decision-tool-card>`. La card applique la classe CSS `.tool-card--flashing` qui anime un pulse navy/or (`box-shadow` + `border-left-color`) sur 1,5 s.

### Cas nominal — toast contextuel "N champs pré-remplis"

1. Au calcul du diff (étape 5 ci-dessus), si la somme des deltas positifs sur tous les toolIds est `> 0` → un `MatSnackBar` est affiché :
   - Message principal : `"N champs pré-remplis dans M outil(s)"` (singulier/pluriel ajustés).
   - Action : `"Voir le détail"`.
   - Durée : 8 s (lecture confortable + temps de cliquer).
   - `panelClass: ['snack-success']` (réutilise le style existant `GlobalAnalysisNotificationService`).
2. Au clic sur l'action `"Voir le détail"` → ouverture d'un `MatDialog` simple (`<app-prefill-diff-dialog>`) qui liste pour chaque outil enrichi : icône + label outil + delta (`+3 champs`).
3. Le dialog se ferme par bouton "Fermer" ou clic en dehors.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `loadVisibility()` échoue après l'analyse | Pas de toast, pas de flash (le service ne reçoit pas de nouveau snapshot). Le snackbar d'erreur existant (`Impossible de charger les outils du dossier…`) reste affiché. |
| Analyse qui n'a enrichi aucun outil (delta global = 0) | Pas de toast, pas de flash. Silencieux — pas de feedback négatif (l'avocat sait que l'analyse a tourné via le bandeau SF-159-01 + le toast global "Synthèse terminée" du `GlobalAnalysisNotificationService`). |
| Premier chargement de la page (snapshot précédent inexistant) | Pas de flash, pas de toast — un premier load n'est pas un "enrichissement", c'est un état initial. Les flashs ne démarrent qu'à partir du 2e snapshot. |
| L'avocat quitte le dossier pendant l'animation | `takeUntilDestroyed` cleanup (déjà en place via DI scope panel). Le timer du flash est aussi annulé via `clearTimeout` dans la méthode interne. |
| Plusieurs analyses successives en moins de 1,5 s | Le set `flashedToolIds` est cumulatif (Set), chaque ajout démarre son propre timer 1,5 s. Pas de race condition — tests unitaires couvrent le cas. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : transversal — concerne tous les outils du panel (FR + BE, 3 domaines), pas d'adaptation par outil.
- [x] **Autres pays / domaines** : aucun impact différencié.
- [x] **Pattern `MatSnackBar` action button** : déjà utilisé par `GlobalAnalysisNotificationService` (action `"Fermer"`). Ici, action `"Voir le détail"` ouvre un dialog — pattern Angular Material standard, pas de réutilisation à harmoniser.
- [x] **Pattern animation flash CSS** : nouveau pattern. Scanné — aucune autre carte de l'app n'utilise un flash similaire à ce stade. Pattern **encapsulé dans `<app-decision-tool-card>`** via Input `[flashing]` + classe CSS scoped. Pas de directive partagée extraite (YAGNI — un seul consommateur).
- [x] **Pattern diff snapshot avant/après refresh** : nouveau. Scanné — aucune autre zone du panel ne fait ce calcul. Concerne uniquement F-IA-04 dashboard. Encapsulé dans `DecisionalToolsProgressService` (déjà existant, scope panel) plutôt que de créer un nouveau service.
- [x] **Pattern dialog "détail diff"** : composant standalone `<app-prefill-diff-dialog>` créé dans le dossier panel. Pas d'utilisation prévue ailleurs — pas d'extraction `shared/`.

### Cas spécifique : nouveau pattern UI

Le composant `<app-prefill-diff-dialog>` est un dialog standalone de 1 écran, scope panel. **Pas de pattern concurrent à harmoniser** — `DecisionToolModalService` existe pour ouvrir les outils en plein écran, mais c'est un dialog 90vw/90vh utilitaire qui ne sert pas le même usage. Le diff dialog est petit (~400px), informatif, court-circuit OK.

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.
- [x] Pas de SF parallèle nécessaire.
- [x] Pas d'ajout backlog.

---

## Critères d'acceptation

- [ ] `DecisionalToolsProgressService` expose `flashedToolIds: Signal<Set<string>>` (initialement `Set()` vide) et la méthode `recordSnapshot(snapshot: Map<string, number>)`.
- [ ] La 1re invocation de `recordSnapshot` enregistre l'état initial, **ne déclenche ni flash ni toast**.
- [ ] La 2e invocation et suivantes calculent le diff vs snapshot précédent, ajoutent les toolIds au prefill augmenté dans `flashedToolIds`, déclenchent un timer `setTimeout(1500)` qui retire chaque toolId à expiration.
- [ ] Si la somme des deltas positifs est `> 0`, un `MatSnackBar` "N champs pré-remplis dans M outil(s)" est affiché avec action "Voir le détail" (durée 8 s, panelClass `snack-success`).
- [ ] Au clic sur l'action, un `MatDialog` ouvre `<app-prefill-diff-dialog>` listant pour chaque outil enrichi : icône, label, delta `+N champs`.
- [ ] Le panel appelle `recordSnapshot()` à chaque `loadVisibility()` succès (initial + après refresh).
- [ ] `<app-decision-tool-card>` accepte un nouvel Input `@Input() flashing = false` qui ajoute la classe `.tool-card--flashing` quand `true`.
- [ ] CSS `.tool-card--flashing` : pulse `box-shadow` navy (#0B2147 alpha) → or (#C9A54B alpha) → return to base, durée 1500 ms, 1 itération, `ease-out`. `prefers-reduced-motion: reduce` désactive l'animation (accessibilité).
- [ ] Tests Jest sur le service : (a) 1er snapshot silencieux, (b) 2e snapshot avec delta → flash + toast, (c) delta = 0 → ni flash ni toast, (d) cleanup timer après expiration, (e) cleanup timer à `ngOnDestroy` du scope panel.
- [ ] Tests Jest sur la card : Input `flashing` ajoute / retire la classe CSS.
- [ ] Tests Jest sur le dialog : reçoit la liste de diffs, rend les items, bouton fermer.
- [ ] Tests Jest sur l'intégration panel : `loadVisibility()` succès appelle `recordSnapshot()`, le `prefillCount` est correctement passé en Input à la card.
- [ ] Smoke manuel staging : déclencher analyse → bandeau visible → DONE → flash visible sur les cards enrichies + toast cliquable → clic action → dialog liste les outils → fermeture dialog.
- [ ] Suite Jest frontend complète verte. Suite backend inchangée (aucune modif backend).

---

## Périmètre

### Hors scope (→ futur, non prévu)

- Barre de progression réelle (% complétion) — confirmé hors-scope total F-159 (cf. SF-159-01).
- Animation flash sur **tout** changement de la card (alerte cohérence, alerte métier, summary). Ici on flash **uniquement** quand `prefillCount` augmente — c'est le signal le plus directement lié à l'analyse IA qui vient de tourner.
- Sons / vibrations / notifications navigateur.
- Persistance du diff côté backend (l'avocat qui rate le toast n'a pas de "centre de notifications").

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Notes |
|---|---|---|---|
| `flashing` (Input card) | Non | `boolean` | Default `false` |
| `snapshot` (paramètre service) | Oui | `Map<string, number>` | toolId → prefillCount actuel (ou 0 si null) |
| Durée animation flash | — | 1500 ms | Cohérent avec la lecture humaine d'un signal visuel |
| Durée toast | — | 8000 ms | + temps de cliquer sur l'action |
| Seuil affichage toast | — | delta total `> 0` | Pas de toast pour delta nul |

---

## Technique

### Backend

**Aucune modification backend.** SF-159-02 est purement frontend.

### Frontend — fichiers touchés

| Fichier | Modification |
|---|---|
| `case-files/decisional-tools-panel/decisional-tools-progress.service.ts` | Ajouter état diff : `flashedToolIds` signal, `recordSnapshot(snapshot)`, gestion timer 1500 ms par toolId, calcul du delta, déclenchement snackbar + ouverture dialog au clic action. Inject `MatSnackBar` + `MatDialog`. |
| `case-files/decisional-tools-panel/prefill-diff-dialog/prefill-diff-dialog.component.{ts,html,scss}` | NOUVEAU — composant standalone qui reçoit `MAT_DIALOG_DATA: { entries: { toolId, label, icon, delta }[] }` et liste les entrées. |
| `case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Après `loadVisibility()` succès, construire `Map<toolId, prefillCount>` à partir de la nouvelle visibilité résolue + `prefillCountFor()` + `cardMetadataFor()` puis appeler `progressService?.recordSnapshot(map, metadataMap)`. |
| `case-files/decisional-tools-panel/decisional-tools-panel.component.html` | Passer `[flashing]="(progressService?.flashedToolIds()?.has(item.toolId)) ?? false"` à `<app-decision-tool-card>`. |
| `case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.ts` | Ajouter `@Input() flashing = false` + getter `flashingClass`. |
| `case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.html` | Ajouter `flashing ? 'tool-card--flashing' : ''` au `[ngClass]`. |
| `case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.scss` | Ajouter keyframes `decisionToolFlash` + `.tool-card--flashing` + media query `prefers-reduced-motion`. |

### Tests Jest — fichiers touchés ou créés

| Fichier | Modification |
|---|---|
| `case-files/decisional-tools-panel/decisional-tools-progress.service.spec.ts` | Étendre — 5 nouveaux cas (1er snapshot silencieux, 2e snapshot delta, delta nul, cleanup timer, cleanup destroy). |
| `case-files/decisional-tools-panel/decision-tool-card/decision-tool-card.component.spec.ts` | Étendre — 2 cas Input `flashing` (true/false → classe CSS). |
| `case-files/decisional-tools-panel/prefill-diff-dialog/prefill-diff-dialog.component.spec.ts` | NOUVEAU — 3 cas (rendu liste, bouton fermer, accessibilité). |
| `case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts` | Étendre — 1 cas : `loadVisibility()` succès appelle `recordSnapshot` avec le bon map. |

### Migration Liquibase

Aucune.

---

## Plan de test

### Tests unitaires Angular

- ~10 tests nouveaux ou étendus (5 service + 2 card + 3 dialog) + 1 panel.

### Tests d'intégration

Aucun nouveau IT backend (pas de modif backend).

### Smoke manuel staging — checklist

1. Ouvrir un dossier avec analyse jamais lancée → vérifier pas de bandeau, pas de toast, pas de flash.
2. Cliquer "Analyser" → bandeau apparaît (SF-159-01).
3. Attendre fin analyse → bandeau disparaît, **flash visible 1,5 s** sur les cards qui ont reçu de nouveaux pré-remplissages, **toast** "N champs pré-remplis dans M outils" affiché.
4. Cliquer "Voir le détail" → dialog s'ouvre avec liste des outils enrichis.
5. Fermer le dialog (bouton + clic en dehors) → dialog disparaît.
6. Cliquer "Re-synthétiser" → si la nouvelle synthèse n'enrichit rien (rare) → pas de toast, pas de flash.
7. Vérifier `prefers-reduced-motion: reduce` (DevTools rendering panel) → flash désactivé, classe ajoutée mais animation = none.
8. Aucune erreur console.

### Isolation workspace

N/A — feature purement UX, ne touche pas l'accès données.

---

## Analyse d'impact — préoccupations transversales

- [ ] **Auth / Principal** : non touché.
- [ ] **Workspace context** : non touché.
- [ ] **Plans / limites** : non touché.
- [ ] **Navigation / routing** : non touché.
- [ ] **Outil décisionnel métier** : non — c'est de l'infra UX autour du panel, pas un nouvel outil ni modification de la logique métier d'un existant.
- [x] **Aucune préoccupation transversale critique** — smoke manuel staging suffit.

### Décision

- [x] Aucune préoccupation transversale critique. Pas de smoke E2E ajouté.

---

## Impact par domaine métier

Cette SF est **purement transversale infrastructure UX**. Aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE) — le flash et le toast s'affichent identiquement sur tous les dossiers, sur tous les outils dont `prefillCount` augmente.

---

## Dépendances

### Subfeatures bloquantes

- SF-159-01 : Done ✅ (panel déjà branché à `events$`, `loadVisibility()` ré-appelé après chaque DONE).

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Pourquoi pas un nouvel événement SSE** : le backend émet déjà `CASE_ANALYSIS_DONE` / `ENRICHED_ANALYSIS_DONE` qui déclenchent `loadVisibility()`. Ajouter un événement "PREFILL_INCREASED" serait redondant et créerait un couplage backend/frontend pour de la pure UX.
- **Pourquoi diff côté frontend** : le `prefillCount` est déjà calculé côté frontend par les statics `getPrefillCount` des composants outil (pattern SF-177-12). Le backend n'a pas connaissance de ce compteur — c'est une dérivée de la synthèse + des inputs IA. Le diff doit donc se faire frontend.
- **Pourquoi 1500 ms d'animation** : assez long pour que l'œil capte (>800 ms reconnu en UX), assez court pour ne pas distraire si plusieurs cards flashent.
- **Pourquoi la card et pas le panel** : encapsulation. La card sait afficher son état visuel ; le panel orchestre. Inputs `[flashing]` est le contrat propre.
- **Pourquoi étendre `DecisionalToolsProgressService` et pas créer un nouveau service** : même lifecycle (scope panel via DI), même cible (les outils du panel), même réactivité signal-based. Créer `DecisionalToolsDiffService` séparé serait du splitting prématuré (CLAUDE.md "Don't add … abstractions beyond what the task requires").

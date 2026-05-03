# Mini-spec — F-159 / SF-159-01 Bandeau de progression IA + auto-refresh dashboard fiable

## Identifiant

`F-159 / SF-159-01`

## Feature parente

`F-159` — Progression visible des opérations IA dans le dashboard décisionnel

## Statut

`ready`

## Date de création

2026-05-03

## Branche Git

`feat/SF-159-01-progress-banner-refresh`

---

## Objectif

Rendre la progression des analyses IA visible sur le dashboard décisionnel via un bandeau persistant pendant l'exécution, **et corriger en passant 2 bugs sous-jacents** qui font que le dashboard ne se met pas toujours à jour automatiquement après une analyse.

---

## Périmètre élargi vs. backlog initial

Le backlog F-159 prévoyait uniquement le **bandeau visible**. L'investigation du code révèle **1 bug sous-jacent** qui empêche le dashboard de se rafraîchir de façon fiable :

**`AnalysisSseService` close après le 1er événement reçu** (`observer.complete(); source.close()` lignes 28-29 d'`analysis-sse.service.ts`). Si CASE_ANALYSIS_DONE arrive avant ENRICHED_ANALYSIS_DONE (ou inversement), le second événement est **silencieusement raté**. Conséquences en cascade :
- Le toast global du `GlobalAnalysisNotificationService` ne s'affiche pas pour les événements suivants ;
- L'`events$` consommé par `case-file-detail.component` (ligne 374, qui appelle correctement `loadSynthesis()` ligne 382 pour les jobTypes CASE/ENRICHED) **ne reçoit plus rien** après le 1er event → la mise à jour dashboard ne se déclenche plus, sauf si le polling de fallback prend le relais (il tourne uniquement quand `hasPendingOrProcessing` est détecté au boot, donc rate les transitions silencieuses) ;
- Le bandeau F-159 lui-même ne pourrait pas se mettre à jour de façon fiable s'il est branché à `events$`.

Le wiring `events$ → loadSynthesis()` est déjà en place et correct dans `case-file-detail.component.ts:374-387`. **Le seul correctif backend/service nécessaire est dans `analysis-sse.service.ts` : laisser le stream ouvert jusqu'à fermeture explicite.** Une fois ce fix appliqué, le bandeau peut se brancher sereinement sur `events$`.

SF-159-01 traite donc 2 sujets dans une seule PR cohérente : le fix SSE et le bandeau visible.

---

## Comportement attendu

### Cas nominal — bandeau de progression

1. L'avocat clique "Analyser" (re-synthèse, première analyse, ou upload document).
2. Le `<decisional-tools-panel>` affiche immédiatement un **bandeau sticky** en haut du panel :
   - Texte adaptatif selon le `jobType` :
     - `CASE_ANALYSIS` → "Analyse du dossier en cours…"
     - `ENRICHED_ANALYSIS` → "Re-synthèse enrichie en cours…"
     - `DOCUMENT_ANALYSIS` → "Analyse des documents en cours…"
   - Si plusieurs jobs simultanés (ex : CASE + DOC), un seul bandeau qui agrège : "Analyses en cours… (2)"
   - `<mat-progress-bar mode="indeterminate">` navy/or aligné DESIGN_SYSTEM
   - Pas de bouton "Masquer" — le bandeau disparaît automatiquement
3. À réception du SSE `*_DONE` ou `*_FAILED` pour le job correspondant :
   - Le job est retiré du compteur de bandeau
   - Si plus aucun job actif → bandeau disparaît avec une transition fade-out 200 ms
   - Si DONE et `jobType ∈ {CASE_ANALYSIS, ENRICHED_ANALYSIS}` → `loadSynthesis()` est appelé → si version changée → `triggerRefresh()` → dashboard recharge

### Cas nominal — auto-refresh dashboard fiable

1. À l'ouverture du dossier, `loadAnalysisJobs(id)` est appelé. Si un job est `PROCESSING` → bandeau actif immédiatement (pas besoin que l'avocat ait cliqué).
2. **Le SSE stream reste ouvert** tant que des jobs sont actifs OU pendant 5 minutes par défaut (équivalent au timeout SSE backend) — il ne se ferme plus après le 1er événement.
3. Le wiring existant `case-file-detail.component.ts:374` (souscription à `globalNotificationService.events$` qui appelle `loadSynthesis()` pour CASE_ANALYSIS / ENRICHED_ANALYSIS DONE) **fonctionne enfin pour tous les événements**, plus seulement le premier. Plus besoin de dépendre du polling pour les transitions tardives.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| SSE EventSource déconnecté (réseau perdu) | Bandeau reste visible jusqu'à reconnexion ou 5 min ; polling existant prend le relais (fallback déjà en place) |
| Job `*_FAILED` reçu | Bandeau retire le job ; toast d'erreur affiché par `GlobalAnalysisNotificationService` (existant) ; pas de triggerRefresh |
| Avocat ferme l'onglet | EventSource cleanup via `ngOnDestroy` (déjà en place via `takeUntilDestroyed`) |
| Plusieurs DONE rapprochés (ex : 3 documents analysés en moins de 1 s) | Tous les events sont consommés (vu que SSE ne ferme plus) ; `triggerRefresh` debounce 300 ms (existant) évite le spam |

---

## Analyse de cohérence transversale

- [x] **Autres outils / pays / domaines** : transversal — affecte tous les dossiers FR + BE et les 3 domaines métier sans adaptation.
- [x] **Pattern `EventSource` SSE** : `analysis-sse.service.ts` est l'unique consommateur. Pas d'autre flux SSE dans le projet à harmoniser.
- [x] **Pattern `CaseDashboardRefreshService.triggerRefresh()`** : déjà appelé par ~30 composants section + `case-file-detail.component`. Ce changement ne touche pas leur appel — il ajoute juste une nouvelle source d'invocation (l'écoute `events$`).
- [x] **`<mat-progress-bar mode="indeterminate">`** : pattern Material standard, déjà utilisé ailleurs dans le projet (cf. `case-file-detail.component`).
- [x] **Composants partagés** : nouveau composant standalone `<app-decisional-tools-progress-banner>` créé, mais utilisé uniquement par `<app-decisional-tools-panel>` — pas de zone de réutilisation à scanner ailleurs.

### Décision

- [x] Étendu à toutes les cibles applicables.

---

## Critères d'acceptation

- [ ] `AnalysisSseService.stream()` ne ferme plus l'EventSource après le 1er événement reçu — peut recevoir N événements jusqu'à fermeture explicite (unsubscribe ou erreur réseau).
- [ ] Test Jest sur `AnalysisSseService` qui injecte 3 événements DONE consécutifs (CASE / DOC / ENRICHED) et vérifie que les 3 sont émis sur l'Observable.
- [ ] Le wiring existant `case-file-detail.component.ts:374-387` (souscription `events$` → `loadSynthesis()` pour CASE/ENRICHED DONE) reste inchangé — vérifier qu'il fonctionne désormais pour les événements 2..N.
- [ ] Nouveau service `DecisionalToolsProgressService` (signal-based, scope `case-file-detail`) avec :
  - `activeJobTypes: Signal<Set<AnalysisJobType>>`
  - `isActive: Signal<boolean>` (computed sur `.size > 0`)
  - méthode `start(jobType)` appelée par les déclencheurs (boutons "Analyser", "Re-synthétiser", "Upload")
  - écoute `events$` pour retirer les jobTypes terminés
  - méthode `syncFromJobs(jobs[])` appelée au boot pour pré-remplir avec les PROCESSING en cours
- [ ] Nouveau composant standalone `<app-decisional-tools-progress-banner>` :
  - `@Input() activeJobTypes: AnalysisJobType[]`
  - HTML : sticky top du panel, mat-progress-bar indeterminate, texte adaptatif, pulse navy/or
  - Caché si `activeJobTypes.length === 0`
- [ ] Bandeau intégré dans `<app-decisional-tools-panel>` au-dessus des sections thématiques.
- [ ] Tests Jest sur `DecisionalToolsProgressService` (start, sync, retrait via events$, isActive computed).
- [ ] Tests Jest sur le composant banner (texte adaptatif, masquage, accessibilité aria-live="polite").
- [ ] Smoke test manuel staging : déclencher analyse → bandeau visible → DONE → bandeau disparaît + dashboard mis à jour sans rechargement de page.
- [ ] 3217+ tests backend verts (aucune modif backend), suite Jest frontend complète verte.

---

## Périmètre

### Hors scope (→ SF-159-02)

- Animation flash 1-2 s sur les cartes outils impactées au refresh
- Toast contextuel "N champs pré-remplis" cliquable pour voir le diff
- Affichage d'une barre de progression réelle (% complétion) — ici on garde l'`indeterminate`
- Émission d'un événement SSE `*_PROCESSING` côté backend (cas couvert par détection au boot via `analysis-jobs` API + appel `start()` côté frontend)

### Hors scope total

- Refonte du polling existant (on le garde comme fallback)
- Ajout d'autres jobTypes (QUESTION_GENERATION, etc.)

---

## Technique

### Backend

**Aucune modification backend.** F-159 est purement frontend — on s'appuie sur les SSE existants (`*_DONE`/`*_FAILED`) et l'API `GET /api/v1/case-files/{id}/analysis-jobs` déjà en place.

### Frontend — fichiers touchés

| Fichier | Modification |
|---|---|
| `core/services/analysis-sse.service.ts` | Retirer `observer.complete()` + `source.close()` après chaque événement — laisser le stream ouvert ; ne fermer qu'à `onerror` ou unsubscribe |
| `core/services/global-analysis-notification.service.ts` | Vérifier que `track()` continue de fonctionner avec un stream qui n'auto-complete plus (le `complete:` callback ne firera qu'à la fermeture explicite) ; ne pas oublier de cleanup à l'unsubscribe pour éviter les leaks |
| `case-files/decisional-tools-panel/decisional-tools-progress.service.ts` | NOUVEAU — signal-based, scope panel |
| `case-files/decisional-tools-panel/decisional-tools-progress-banner.component.{ts,html,scss}` | NOUVEAU — composant standalone |
| `case-files/decisional-tools-panel/decisional-tools-panel.component.{ts,html,scss}` | Ajouter import + intégration du banner ; provide `DecisionalToolsProgressService` |
| `case-files/case-file-detail/case-file-detail.component.ts` | Appeler `progressService.start(jobType)` quand bouton "Analyser"/"Re-synthétiser"/"Upload" cliqué (la souscription `events$` existante reste inchangée) |

### Tests Jest — fichiers touchés ou créés

| Fichier | Modification |
|---|---|
| `core/services/analysis-sse.service.spec.ts` (NOUVEAU si absent) | Test : 3 événements consécutifs reçus sans fermeture intermédiaire |
| `core/services/global-analysis-notification.service.spec.ts` | NOUVEAU si absent — test : `events$` émet plusieurs événements consécutifs |
| `case-files/decisional-tools-panel/decisional-tools-progress.service.spec.ts` | NOUVEAU — couvre start/sync/events$ retrait/computed isActive |
| `case-files/decisional-tools-panel/decisional-tools-progress-banner.component.spec.ts` | NOUVEAU — couvre rendering, texte adaptatif, masquage |
| `case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts` | Étendre — vérifier intégration du banner |

### Migration Liquibase

Aucune.

---

## Plan de test

### Tests unitaires Angular

- 4 nouveaux : SSE multi-events, events$ multi-events (Notification service), ProgressService 4 cas, Banner 3 cas (rendu/texte/masquage).
- ~3 tests adaptés dans `decisional-tools-panel.component.spec.ts` (intégration banner).

### Tests d'intégration

Aucun nouveau IT backend (pas de modif backend). Pas de E2E ajouté — smoke manuel staging suffit pour ce niveau de polish UX.

### Smoke manuel staging — checklist

1. Ouvrir un dossier avec analyse jamais lancée → vérifier pas de bandeau.
2. Cliquer "Analyser" → bandeau apparaît immédiatement avec texte "Analyse du dossier en cours…".
3. Attendre fin analyse (~30-60 s) → bandeau disparaît, dashboard se met à jour sans recharger la page.
4. Cliquer "Re-synthétiser" → bandeau "Re-synthèse enrichie en cours…" apparaît.
5. Pendant la re-synthèse, uploader un nouveau document → bandeau passe à "Analyses en cours… (2)".
6. Vérifier que les 2 DONE successifs déclenchent bien la mise à jour dashboard.
7. Vérifier qu'aucune erreur console / aucun snackbar manqué.

### Isolation workspace

N/A — feature purement UX, ne touche pas l'accès données.

---

## Analyse d'impact — préoccupations transversales

- [ ] **Auth / Principal** : non touché.
- [ ] **Workspace context** : non touché.
- [ ] **Plans / limites** : non touché.
- [ ] **Navigation / routing** : non touché — pas de nouvelle route, pas de guard modifié.
- [ ] **Outil décisionnel métier** : non — c'est de l'infra UX autour du panel, pas un nouvel outil ni modification d'un existant.

### Décision

- [x] Aucune préoccupation transversale critique. Smoke manuel staging suffit.

---

## Impact par domaine métier

Cette SF est **purement transversale infrastructure UX**. Aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE) — le bandeau s'affiche identiquement sur tous les dossiers, le fix SSE et l'écoute events$ valent pour tous.

---

## Dépendances

- F-39 SSE infra : Terminée ✅ (utilisé)
- F-124 `CaseDashboardRefreshService.triggerRefresh()` : Terminée ✅ (utilisé)
- F-167 nouveau dashboard `<app-case-dashboard>` : Terminée ✅ (consommateur du refresh)
- F-178 backlog (orthogonal) : Terminée ✅ (sans rapport)

---

## Précédence

- Précède **SF-159-02** : animation flash sur cartes + toast diff "N champs pré-remplis" (~1 j).

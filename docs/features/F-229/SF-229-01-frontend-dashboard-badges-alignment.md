# Mini-spec — F-229 / SF-229-01 Frontend — Aligner les tiles dashboard sur les cibles canoniques de la grille de badges F-162

## Identifiant

`F-229 / SF-229-01`

## Statut

`draft` — 2026-05-09

## Branche Git

`feat/SF-229-01-frontend-dashboard-badges-alignment`

## Pattern de référence

`synthesis.component.ts:278-395` — `synthesisBadges` computed (grille de badges F-162) + handlers `scrollToBlock` / `openPopup` / `navigateTo` / `openTypeLitigeOverrideDialog`.

---

## Objectif

Toutes les tiles "résumé" du dashboard décisionnel qui pointent vers une section de la synthèse doivent utiliser **exactement la même cible** que le badge F-162 équivalent (popup, page dédiée ou anchor scroll). Et fixer le scroll vers les ancres après navigation router (le mécanisme `#section-xxx` ne déclenche pas `scrollIntoView` aujourd'hui).

---

## Comportement attendu

### Mismatches identifiés (audit 2026-05-09)

| Tile dashboard | Action actuelle | Badge F-162 équivalent | Cible canonique F-162 | Verdict |
|---|---|---|---|---|
| `RETAINED_PISTES_SUMMARY` (F-192) | `navigate /synthesis#section-pistes` | `pistes` | anchor `#section-pistes` (scroll inline) | ⚠️ Le scroll ne se déclenche pas après navigation router |
| `F-194-pieces-summary` | `navigate /synthesis#section-pieces` | `pieces` | **popup** `SynthesisShortBlockDialogComponent` | ❌ Mismatch — devrait ouvrir popup |
| `F-195-risques-summary` | `navigate /synthesis#section-risques` | `risques` | **page dédiée** `/synthesis/risques` | ❌ Mismatch — devrait aller sur la sous-page |
| `F-196-questions-summary` | `navigate /synthesis#section-questions` | `questions` | anchor `#section-questions` (scroll inline) | ⚠️ Le scroll ne se déclenche pas après navigation router |

### Cas nominal (après fix)

| Tile dashboard | Action cible |
|---|---|
| `RETAINED_PISTES_SUMMARY` | `BadgeNavigationService.go('pistes')` → navigate `/synthesis` + fragment `section-pistes` → SynthesisComponent souscrit à `route.fragment` et déclenche `scrollToBlock` au mount |
| `F-194-pieces-summary` | `BadgeNavigationService.go('pieces')` → ouvre directement `SynthesisShortBlockDialogComponent` (data 'pieces', identique au badge) |
| `F-195-risques-summary` | `BadgeNavigationService.go('risques')` → navigate `/case-files/:id/synthesis/risques` |
| `F-196-questions-summary` | `BadgeNavigationService.go('questions')` → navigate `/synthesis` + fragment `section-questions` → scroll au mount |

---

## Critères d'acceptation

- [ ] **CA-01** : nouveau service `BadgeNavigationService` (`frontend/src/app/case-files/synthesis-badges/badge-navigation.service.ts`) qui expose `go(key, caseFileId)` pour 7 keys initiales : `pistes` / `pieces` / `risques` / `questions` / `timeline` / `faits` / `points-juridiques`. Chaque key implémente la cible F-162 canonique (route + popup + dialog). Injection : `Router` + `MatDialog` + `MatSnackBar` (pas d'erreur silencieuse).
- [ ] **CA-02** : `case-dashboard.component.ts:openGenericTool()` utilise `BadgeNavigationService.go(key, caseFileId)` pour les 4 tiles concernées (`RETAINED_PISTES_SUMMARY`, `F-194-pieces-summary`, `F-195-risques-summary`, `F-196-questions-summary`). Les blocs `if (toolId === 'XXX') { router.navigate(...) }` actuels (lignes 192-233) sont remplacés par un seul switch `BadgeNavigationService.go`.
- [ ] **CA-03** : la grille de badges F-162 dans `synthesis.component.ts:278-395` consomme **aussi** `BadgeNavigationService.go(key)` (refactor des handlers actuels — `scrollToBlock` / `openPopup` / `navigateTo` deviennent privés, le service les orchestre). Les badges restent visuellement identiques.
- [ ] **CA-04** : `SynthesisComponent.ngOnInit` souscrit à `route.fragment` et déclenche `scrollToBlock(fragment)` quand : (a) `synthesis` signal rempli (≠ null), (b) le DOM est rendu (`afterNextRender` ou `setTimeout(0)`). Retry 5 fois × 200 ms hérité de SF-162-01.
- [ ] **CA-05** : tests Jest `badge-navigation.service.spec.ts` (nouveau) — chaque key déclenche le bon comportement (router.navigate / dialog.open). Mock `Router` + `MatDialog`.
- [ ] **CA-06** : tests Jest `case-dashboard.component.spec.ts` — clic sur les 4 tiles résumé invoque `BadgeNavigationService.go` avec la bonne key (1 test par tile = 4 nouveaux tests).
- [ ] **CA-07** : tests Jest `synthesis.component.spec.ts` — (a) clic sur badge `pistes` → `BadgeNavigationService.go('pistes')` invoqué, (b) `route.fragment = section-pistes` au mount + synthesis non null → `scrollToBlock('section-pistes')` invoqué. ~5 nouveaux tests.
- [ ] **CA-08** : aucune régression sur les tests existants `case-dashboard.component.spec.ts` (~50 actuels) ni `synthesis.component.spec.ts` (~120 actuels).
- [ ] **CA-09** : audit visuel — cliquer chaque tile résumé du dashboard et chaque badge F-162 doit ouvrir EXACTEMENT la même chose. Aucune divergence comportementale.

---

## Périmètre

### Hors scope V1

- (a) Tiles outils décisionnels (`F-IM-05-arbre-decisionnel-titre` etc.) — ouverture modal inchangée, traité par F-228.
- (b) Badge `type-litige` (F-197) — pas dans le dashboard, reste sur synthesis uniquement.
- (c) Refonte `case-dashboard` model unifié backend `DashboardTile.targetAction` (route/popup/dialog) — V2 si besoin.

---

## Technique

### Fichiers à modifier / créer

1. **Nouveau** `frontend/src/app/case-files/synthesis-badges/badge-navigation.service.ts` — service singleton `providedIn: 'root'` avec méthode publique `go(key: BadgeKey, caseFileId: string): void` + types `BadgeKey`.
2. **Nouveau** `frontend/src/app/case-files/synthesis-badges/badge-navigation.service.spec.ts`.
3. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.ts` — remplacer les 4 blocs `if (toolId === ...)` (lignes 192-233) par un seul appel `BadgeNavigationService.go(...)`.
4. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.spec.ts` — adapter tests existants + ajouter CA-06.
5. `frontend/src/app/case-files/synthesis/synthesis.component.ts` — refactor `scrollToBlock` / `openPopup` / `openTypeLitigeOverrideDialog` pour passer par `BadgeNavigationService.go(key)` côté handlers de badges. **Ne pas changer le rendu visuel des badges.** Ajouter `route.fragment` subscription dans `ngOnInit`.
6. `frontend/src/app/case-files/synthesis/synthesis.component.spec.ts` — adapter tests + ajouter CA-07.

### Service `BadgeNavigationService` — signature

```ts
type BadgeKey = 'pistes' | 'pieces' | 'risques' | 'questions' | 'timeline' | 'faits' | 'points-juridiques';

@Injectable({ providedIn: 'root' })
export class BadgeNavigationService {
  constructor(
    private router: Router,
    private dialog: MatDialog,
  ) {}

  go(key: BadgeKey, caseFileId: string, contextData?: BadgeContextData): void {
    switch (key) {
      case 'pistes':       this.router.navigate(['/case-files', caseFileId, 'synthesis'], { fragment: 'section-pistes' }); return;
      case 'pieces':       this.openShortBlockDialog('pieces', contextData); return;
      case 'risques':      this.router.navigate(['/case-files', caseFileId, 'synthesis', 'risques']); return;
      case 'questions':    this.router.navigate(['/case-files', caseFileId, 'synthesis'], { fragment: 'section-questions' }); return;
      case 'timeline':     this.router.navigate(['/case-files', caseFileId, 'synthesis', 'timeline']); return;
      case 'faits':        this.router.navigate(['/case-files', caseFileId, 'synthesis', 'faits']); return;
      case 'points-juridiques': this.router.navigate(['/case-files', caseFileId, 'synthesis', 'points-juridiques']); return;
    }
  }
}
```

`BadgeContextData` typé pour pieces/questions-ouvertes (data du popup) — passé par l'appelant qui possède la synthesis.

### Aucune migration backend, aucun nouvel endpoint

Tout reste côté frontend.

---

## Plan de test

### Tests Jest (~10-12 nouveaux)

- `badge-navigation.service.spec.ts` (nouveau, ~7 tests) :
  - `go('pistes')` → router.navigate avec fragment
  - `go('pieces', ctx)` → dialog.open SynthesisShortBlockDialogComponent
  - `go('risques')` → router.navigate sur sous-page
  - `go('questions')` → router.navigate avec fragment
  - `go('timeline'/'faits'/'points-juridiques')` → router.navigate sous-page

- `case-dashboard.component.spec.ts` (~4 nouveaux) :
  - clic `RETAINED_PISTES_SUMMARY` → BadgeNavigationService.go('pistes') invoqué
  - idem pour les 3 autres

- `synthesis.component.spec.ts` (~5 nouveaux) :
  - badge `pistes` cliqué → BadgeNavigationService.go('pistes')
  - badge `pieces` cliqué → BadgeNavigationService.go('pieces', ctx) avec items synthesis
  - badge `risques` cliqué → go('risques')
  - route.fragment 'section-pistes' au mount + synthesis non null → scrollToBlock('section-pistes')
  - route.fragment quand synthesis null → scrollToBlock attend signal

### Test manuel post-deploy staging

1. Dossier Immigration Chen 17 (suite F-228)
2. Cliquer chaque tile résumé du dashboard décisionnel — vérifier comportement conforme F-162 (popup pour Pièces, sous-page pour Risques, scroll fluide pour Pistes/Questions)
3. Naviguer manuellement `/case-files/:id/synthesis#section-pistes` → vérifier scroll automatique vers le bloc

---

## Dépendances

- F-228 SF-228-01 — pas bloquant (zones disjointes ; F-228 traite les tiles outils, F-229 traite les tiles "résumé"). Mais F-228 doit être mergée avant pour éviter merge conflict sur `case-dashboard.component.ts`.
- F-162 SF-162-01..06 ✅ (grille badges + popup `SynthesisShortBlockDialogComponent` + sous-pages timeline/faits/points-juridiques/risques)

---

## Impact par domaine métier

Transversal — navigation UI, aucune adaptation par domaine.

---

## Analyse de cohérence transversale

- **Auth/Principal** : N/A.
- **Workspace context** : N/A.
- **Plans/limites** : N/A.
- **Navigation/routing** : ✅ concerné.
  - Composants touchés : `case-dashboard.component.ts`, `synthesis.component.ts` (handlers de badges + ngOnInit), nouveau `BadgeNavigationService`.
  - Smoke tests E2E `e2e/smoke/navigation.spec.ts` à passer après push (BLOQUANT).
- **Outil décisionnel métier** : N/A (tiles outils non concernées par F-229).
- **Pattern partagé** : ✅ **NOUVEAU** — `BadgeNavigationService`.

### Nouveau pattern UI ou service partagé — analyse d'impact

- **Cibles à harmoniser** : grille badges F-162 (synthesis.component) + tiles dashboard "résumé" (case-dashboard.component). 2 consommateurs initiaux.
- **Patterns concurrents** : aucun — F-229 EST l'harmonisation des 2 mécanismes divergents (route+anchor vs popup+page).
- **Évolutions V2** : si on rajoute un point d'entrée (notification email, breadcrumb, page d'accueil), `BadgeNavigationService.go(key)` est le seul appel à connaître.
- **Justification du service partagé** : 7 destinations × 2 consommateurs = 14 sites où la cible doit rester synchrone. Sans service, dette de convergence quasi-immédiate (cas F-194/F-195/F-196 : 3 mismatches détectés en 30 j de divergence).

---

## Risques

- **Régression badges F-162** : refactor des handlers. Mitigation = tests existants `synthesis.component.spec.ts` (~120) restent verts + 5 nouveaux pour couvrir le service.
- **Scroll fragment** : le `scrollToBlock` peut rater si le DOM n'est pas prêt au moment du fragment subscription. Retry hérité de SF-162-01 (5 × 200 ms) couvre la plupart des cas.
- **Smoke E2E navigation** : à lancer avant push. Si rouge → BLOQUANT.

---

## Notes

- **Décision 2026-05-09** : extraction `BadgeNavigationService` plutôt que duplication des handlers. Justifié par l'audit qui a révélé 3 mismatches en 30 jours.
- **Décision 2026-05-09** : `route.fragment` subscription dans `SynthesisComponent.ngOnInit` plutôt que `withInMemoryScrolling({ anchorScrolling: 'enabled' })` au niveau Router : le retry 5×200 ms gère le cas où le DOM n'est pas encore rendu (signaux async), `anchorScrolling` natif ne le gère pas et casse régulièrement avec les signaux Angular.
- **Décision 2026-05-09** : `BadgeNavigationService` ne gère pas les outils décisionnels (modal) — c'est un autre mécanisme (TOOL_REGISTRY) traité par F-228. Le service reste focused sur la navigation entre badges et leurs cibles (popup/sous-page/anchor).
- **Origine** : préoccupations utilisateur 2026-05-08 sur Immigration Chen 17 — "tile pistes retenues clic = rien", "ancrage marche pas", "il faut utiliser les liens des sections F-162 partout dans l'app".

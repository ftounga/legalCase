# Mini-spec — F-IA-03 / SF-IA-03-19 Navigation effective vers la source

## Identifiant

`F-IA-03 / SF-IA-03-19`

## Feature parente

`F-IA-03`

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-IA-03-19-navigation-effective-source`

---

## Objectif

Câbler la **réception** des navigations du popover d'incohérence côté composants cibles (`SynthesisComponent`, `CaseFileDetailComponent`) pour que le clic sur "Voir la question", "Voir le point procédural", "Voir les pièces manquantes", "Ouvrir le chat", "Ouvrir contrat.pdf" scroll effectivement vers l'élément et le met en évidence.

---

## Contexte

SF-IA-03-17/18 ont câblé la navigation sortante (router.navigate avec query params). Mais les composants cibles **n'écoutent pas** ces query params :
- `SynthesisComponent.ngOnInit` lit uniquement `paramMap.get('id')`, ignore `queryParamMap`.
- `CaseFileDetailComponent` idem.
- Route `/case-files/:id/documents/:docId` **n'existe pas** → 404 Angular.

---

## Comportement attendu

### SynthesisComponent — réception 4 query params

Dans `ngOnInit`, ajouter `this.route.queryParamMap.subscribe()` pour détecter :

| Query param | Action |
|---|---|
| `?qa={questionId}` | Scroll vers `#qa-{questionId}`, highlight pulse 2s |
| `?check={f96Code}` | Scroll vers `#check-{f96Code}`, highlight pulse 2s |
| `?piece={index}` | Scroll vers `#piece-{index}`, highlight pulse 2s |
| `?chat={messageId}` ou `?section=chat` | Scroll vers `#section-chat`, highlight pulse 2s |
| `?section=questions` | Scroll vers `#section-questions` (ancre du bloc Q&A) |
| `?section=checklist` | Scroll vers `#section-checklist` |
| `?section=pieces` | Scroll vers `#section-pieces` |

Templates enrichis :
- Chaque `div.question-item` reçoit `[attr.id]="'qa-' + q.id"`
- Chaque `div.checklist-item` reçoit `[attr.id]="'check-' + check.code"` (si code présent)
- Chaque `li` de pièce manquante reçoit `[attr.id]="'piece-' + $index"`
- Les `mat-expansion-panel` des sections Q&A / F-96 / Pièces reçoivent `id="section-questions"`, `id="section-checklist"`, `id="section-pieces"`. Chat : `id="section-chat"`.

Si la section est fermée (panel collapsed), la scroll-logic l'**ouvre** avant de scroll (via setter du signal `expandedXxx`).

### CaseFileDetailComponent — réception `section`

Dans `ngOnInit`, abonner `queryParamMap` :

| Query param | Action |
|---|---|
| `?section=documents` | Scroll vers `#section-documents` (déjà existant) |
| `?section=documents&doc={docId}` | Idem + highlight de la ligne du document si l'id matche |

### Navigator — redirection pour OPEN_DOCUMENT

Comme la route `/case-files/:id/documents/:docId` n'existe pas, **redéfinir** `OPEN_DOCUMENT` pour naviguer vers `/case-files/{id}?section=documents&doc={docId}` (retour au détail dossier, scroll + highlight ligne du document).

### Highlight visuel

CSS partagé (dans `synthesis.component.scss` et équivalent dans `case-file-detail.component.scss`) :
```scss
.source-highlight {
  animation: source-highlight-pulse 2s ease-out;
}
@keyframes source-highlight-pulse {
  0%   { background-color: rgba(201, 166, 70, 0.25); }
  100% { background-color: transparent; }
}
```

Dans le TS : après `scrollIntoView`, ajouter la classe 10 ms plus tard, la retirer après 2 s (Angular change detection safe via `setTimeout`).

### Timing / défers

Quand on arrive sur `/synthesis?qa=xxx`, les données sont chargées async. Il faut :
1. Attendre que `questions()` soit peuplé (effect sur signal ou `setTimeout(200 ms)` après `loadSynthesis()` `next`).
2. Puis appliquer scroll + highlight.

Solution simple : dans `ngOnInit`, souscrire à `queryParamMap` ET retenir la cible dans un signal `pendingScrollTarget`. Un `effect()` surveillant la fois `pendingScrollTarget` ET le signal de données pertinent (ex. `questions()`) déclenche le scroll quand les données sont disponibles.

---

## Analyse de cohérence transversale

- [x] **Autres outils** : la navigation vers la source est un pattern transversal utilisé par tous les popovers (10 outils). L'action s'effectue dans 2 composants cibles centralisés (`SynthesisComponent`, `CaseFileDetailComponent`), donc correction universelle.
- [x] **Autres pays** : FR + BE natif — aucune logique pays dans le scroll/highlight.
- [x] **Autres domaines** : Travail / Famille / Immigration natifs.
- [x] **Nouveau pattern UI/service partagé** : oui, un utilitaire `scrollAndHighlight(id)` peut être factorisé dans un service partagé (`core/services/scroll-highlight.service.ts`). Candidat à réutilisation future (dashboard stepper, autres liens internes). **Mais pour rester borné, cette SF ne crée pas le service partagé — les 2 composants dupliquent la logique courte (~10 lignes chacun). Si un 3e consommateur apparaît, on factorisera.**
- [x] **Flow transversal** : aucun auth/workspace/plans/navigation-guard touché.

### Décision

- [x] Étendu aux 2 composants cibles (synthesis + case-file-detail).
- [x] Non applicable ailleurs pour cette SF — la factorisation en service partagé est différée au 3e consommateur.

---

## Critères d'acceptation

- [ ] `SynthesisComponent.ngOnInit` souscrit à `ActivatedRoute.queryParamMap` et retient la cible dans un signal `pendingScrollTarget`.
- [ ] Un `effect()` dans `SynthesisComponent` surveille le signal + les données (`questions()`, `procedureChecks()`, `piecesManquantes()`) et déclenche scroll + highlight quand la cible est rendue.
- [ ] Templates : ids ajoutés sur `qa-{id}`, `check-{code}`, `piece-{index}`, `section-questions`, `section-checklist`, `section-pieces`, `section-chat`.
- [ ] Si la section est collapsée, le scroll l'ouvre via un signal `expanded*` avant de scroll.
- [ ] `CaseFileDetailComponent.ngOnInit` écoute `?section=documents` et scroll vers `#section-documents`.
- [ ] `CoherenceSourceNavigator.OPEN_DOCUMENT` redirigé vers `/case-files/{id}?section=documents&doc={docId}` (route documents dédiée non créée, hors scope).
- [ ] CSS `source-highlight-pulse` défini et appliqué 2s sur l'élément cible.
- [ ] Tests : queryParam `qa=xxx` simulé dans le spec → `document.getElementById('qa-xxx')` est scrollé / reçoit classe highlight.
- [ ] Non-régression : 862 backend, 974 front, build prod vert.

---

## Hors scope

- Création d'une route dédiée `/case-files/:id/documents/:docId` avec document viewer — à planifier en SF dédiée (scope GED + preview).
- Factorisation d'un service `ScrollHighlightService` partagé — différée au 3e consommateur.
- Chat : pour l'instant, `OPEN_CHAT` scroll vers le panneau `#section-chat`. L'ouverture automatique de l'input et le focus sur un message précis restent hors scope.

---

## Technique

### Composants frontend modifiés

- `SynthesisComponent` — `.ts` (subscription queryParams + effect + scroll helper), `.html` (ids), `.scss` (keyframes).
- `CaseFileDetailComponent` — `.ts` + `.scss`.
- `CoherenceSourceNavigator` — `OPEN_DOCUMENT` redéfini.

### Backend

Aucun changement.

---

## Plan de test

### Tests frontend

- [ ] `synthesis.component.spec.ts` : naviguer avec `?qa=abc` → le signal `pendingScrollTarget` vaut `{kind: 'qa', id: 'abc'}`. Mock de `document.getElementById` → appel au bon id.
- [ ] `synthesis.component.spec.ts` : `?check=FR_CONVOCATION` → cible `check-FR_CONVOCATION`.
- [ ] `synthesis.component.spec.ts` : `?piece=0` → cible `piece-0`.
- [ ] `synthesis.component.spec.ts` : `?section=chat` → cible `section-chat`.
- [ ] `case-file-detail.component.spec.ts` : `?section=documents` → cible `section-documents`.
- [ ] `coherence-source-navigator.service.spec.ts` : OPEN_DOCUMENT → router.navigate appelé avec les bons params.
- [ ] Non-régression 974+.

---

## Notes et décisions

- **Pourquoi pas un service partagé** : 2 consommateurs seulement, code ~10 lignes, factorisation prématurée. Si F-92 ou F-94 ajoutent le même pattern en backlog, on extrait.
- **Pourquoi highlight CSS pure** : pas de state management nécessaire, le `setTimeout(remove)` dans Angular change detection reste safe.
- **Documents viewer** : hors scope. Le fallback vers liste + scroll ligne est acceptable pour cette SF et délivre déjà l'essentiel.

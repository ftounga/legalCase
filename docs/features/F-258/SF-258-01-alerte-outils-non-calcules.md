# Mini-spec — F-258 / SF-258-01 Alerte « outils pré-remplis non calculés »

## Identifiant
`F-258 / SF-258-01`

## Feature parente
`F-258`. Cadrages GO : `SF-258-00-coherence.md` (étape 0) + `SF-258-00b-ux-coherence.md` (étape 0 bis).

## Statut
`ready`

## Date de création
2026-06-08

## Branche Git
`feat/SF-258-01-alerte-outils-non-calcules`

## Type
Feature frontend (cohérence UX). Cadrages 0 + 0 bis = GO.

---

## Objectif (une phrase)
Afficher, au-dessus du bouton « Générer le projet de conclusions », un **encart d'avertissement non bloquant** quand des outils décisionnels **proposés et pré-remplis** n'ont pas encore été **calculés**, avec un moyen d'aller les calculer.

## Comportement nominal
À l'affichage de la section « Projet de conclusions » (onglet « Décision ») :
1. Le composant récupère les **outils proposés** (`GET …/decision-tools-visibility` → `alwaysOn + contextual`) et les **outils calculés** (`GET …/dashboard` → `tiles[].toolId`).
2. Il calcule `manquants = proposés − calculés` et `N = manquants.length`.
3. **Si `N > 0`** : un encart d'avertissement s'affiche au-dessus du bouton : « **N outil(s) pertinent(s) ne sont pas encore calculé(s)** — les conclusions ne les prendront pas en compte. » + bouton **« Voir les outils à calculer »**.
4. **Si `N = 0`** : aucun encart.
5. Le bouton **« Générer le projet de conclusions » reste actif et inchangé** (génération non bloquée).
6. **« Voir les outils à calculer »** → émet un `@Output()` que le parent (`case-file-detail`) traite en faisant défiler/focalisant le panneau d'outils décisionnels.

## Cas d'erreur / bords
- Échec d'un des deux appels (visibilité ou dashboard) → **pas d'encart** (on n'invente pas de N ; dégradation silencieuse, log console). L'avocat n'est pas bloqué.
- `proposés` vide → `N = 0` → pas d'encart.
- Recalcul : l'encart est rafraîchi au `ngOnInit` du composant et lors d'un refresh explicite (cf. invariant « dynamique » — MVP : re-fetch à l'(ré)affichage ; mise à jour temps-réel fine hors scope).
- `catalog` **jamais** compté (seuls `alwaysOn + contextual`).

## Solution technique
### Frontend — `conclusions-section.component.ts`
1. Injecter `CaseFileService` (`getDecisionToolsVisibility`) et `CaseDashboardService` (`get`).
2. Au `ngOnInit` (et via une méthode `refreshMissingTools()`), `forkJoin` des deux appels → calcule `missingToolIds` + `missingToolsCount`.
3. Exposer `missingToolsCount` (signal/propriété) + `@Output() viewToolsRequested = new EventEmitter<void>()`.
4. Échec → `missingToolsCount = 0` (pas d'encart), log.

### Frontend — `conclusions-section.component.html`
5. Au-dessus du bloc bouton (`NOT_GENERATED`), `@if (missingToolsCount > 0)` → encart `class="conclusions__tools-warning"` (style avertissement, **non modal**) : texte + bouton « Voir les outils à calculer » `(click)="viewToolsRequested.emit()"`.

### Frontend — `case-file-detail.component`
6. Brancher `(viewToolsRequested)` du `app-conclusions-section` → méthode qui `scrollIntoView` vers le panneau d'outils décisionnels (`@ViewChild` ou ancre).

## Critères d'acceptation (vérifiables)
1. `N = (alwaysOn + contextual) − tiles.toolId`, le `catalog` n'est jamais compté (test).
2. `N > 0` → encart affiché avec le bon nombre ; `N = 0` → pas d'encart (test).
3. Le bouton « Générer » reste actif quel que soit N (non bloquant) (test).
4. « Voir les outils à calculer » émet `viewToolsRequested` (test).
5. Échec d'un appel → pas d'encart, pas de crash (test).
6. Build + Jest verts.

## Plan de test minimal
- **Jest `conclusions-section`** : (a) visibilité {alwaysOn:[A,B], contextual:[C], catalog:[D]} + dashboard tiles {A} → N=2 (B,C), catalog D ignoré ; (b) tous calculés → N=0, pas d'encart ; (c) clic « Voir les outils » → émet l'output ; (d) appel en erreur → N=0 sans crash ; (e) bouton Générer toujours actif.
- **Jest `case-file-detail`** (léger) : `viewToolsRequested` déclenche le scroll (ou au moins la méthode).
- **Isolation workspace** : N/A (lecture, pas de mutation).
- **Manuel staging** : dossier avec outils proposés non calculés → encart visible ; calculer les outils → encart disparaît au re-affichage.

## Tables / endpoints / composants impactés
- **Endpoints** (consommation, existants) : `GET …/decision-tools-visibility`, `GET …/dashboard`.
- **Frontend** : `conclusions-section.component.{ts,html,spec}`, `case-file-detail.component.{ts,html}` (branchement output + scroll).
- **Backend** : aucun. **Pas de migration.**

### Préoccupation transversale : **Navigation / écran** (mineure)
Ajout d'un encart + une action de scroll intra-écran (pas de nouvelle route, pas de guard). Pas d'impact Auth/Workspace/Plans. Smoke E2E auth/nav non requis (pas de route modifiée).

## Hors périmètre
- Mise à jour **temps réel** de N (push après calcul d'un outil) — MVP = re-fetch au (ré)affichage.
- Pré-calcul automatique des outils (option écartée par le PO).
- Blocage de la génération (option écartée par le PO).
- Nouvel endpoint backend dédié (réutilisation des 2 endpoints existants).

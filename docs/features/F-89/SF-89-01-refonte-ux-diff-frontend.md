# Mini-spec — F-89 / SF-89-01 Refonte UX comparaison d'analyses — améliorations frontend

---

## Identifiant

`F-89 / SF-89-01`

## Feature parente

`F-89` — Refonte UX de la comparaison d'analyses

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-89-01-refonte-ux-diff-frontend`

---

## Objectif

Améliorer la page de comparaison d'analyses (`/case-files/:id/diff`) sur 5 axes visuels et UX sans modifier le backend : suppression du bouton "Comparer", grands compteurs numériques, callout pour le champ `reason`, border-left colorée sur les sections selon le type dominant, empty state guidant.

---

## Comportement attendu

### Cas nominal

1. **Auto-trigger du diff** : dès que `fromId` et `toId` sont tous deux sélectionnés et différents (`canCompute() === true`), le diff se lance automatiquement via un `effect()`. Le bouton "Comparer" est supprimé.

2. **Grands compteurs numériques** : la `stats-bar` (6px, 180px) est remplacée par une rangée de 4 compteurs dans la sticky summary bar :
   - `+N ajouts` en vert `#27AE60`, chiffre en `font-size: 28px; font-weight: 700`
   - `−N suppressions` en rouge `#C0392B`
   - `~N enrichis` en bleu `#2980B9`
   - `= N inchangés` en gris `#94A3B8`
   - Chaque compteur reste cliquable pour filtrer (comportement identique aux anciens `count-chip`)
   - Si un compteur vaut 0, il est affiché en gris atténué (non cliquable)

3. **Callout pour le champ `reason`** : le champ `item-reason` (justification IA) est redesigné en callout :
   - Fond légèrement teinté selon le type (vert pâle pour added, rouge pâle pour removed, bleu pâle pour enriched)
   - `border-left: 2px solid` de la couleur du type
   - `border-radius: 4px`
   - `padding: 6px 10px`
   - Précédé d'un petit label "Raison IA :" en `font-size: 11px; font-weight: 600; text-transform: uppercase`
   - Remplace l'ancien style `font-style: italic; opacity: 0.85`

4. **Border-left colorée sur les section cards** : chaque `mat-card.section-card` reçoit une `border-left: 4px solid` selon le type dominant de la section :
   - Majoritairement ajouts (added > removed && added > enriched) → `#27AE60`
   - Majoritairement suppressions → `#C0392B`
   - Majoritairement enrichissements → `#2980B9`
   - Inchangé (0 changements) → `var(--color-divider)`
   - Égalité ou mixte → `var(--color-primary)`

5. **Empty state guidant** : quand aucune sélection n'est faite ou incomplète, remplacer l'icône `compare` générique par un texte en deux étapes :
   - Titre : "Choisissez deux versions à comparer"
   - Sous-titre : "Sélectionnez la version de référence (colonne gauche), puis la version à analyser (colonne droite). Le diff s'affichera automatiquement."
   - Icône : `compare_arrows` conservée mais en `color: var(--color-primary)` au lieu de `var(--color-divider)`

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `fromId === toId` après changement | `canCompute() === false` → l'effect ne se déclenche pas, le diff existant reste affiché |
| Erreur API lors du diff auto-déclenché | Snackbar d'erreur identique à l'existant, `diffLoading(false)` |
| Une seule version disponible | Empty state inchangé (ce cas est géré en amont) |

---

## Critères d'acceptation

- [ ] Le bouton "Comparer" n'existe plus dans le template
- [ ] Le diff se lance automatiquement dès que `canCompute()` devient `true`
- [ ] Les 4 compteurs numériques remplacent la stats-bar — chiffres en 28px gras
- [ ] Un compteur à 0 est affiché en gris atténué et non cliquable
- [ ] Le filtre par type fonctionne en cliquant sur chaque compteur (identique à avant)
- [ ] Le champ `reason` est affiché en callout (label "Raison IA :", border-left, fond teinté)
- [ ] Chaque section card a une border-left colorée selon le type dominant
- [ ] L'empty state affiche un texte guidant en deux lignes (titre + sous-titre)
- [ ] Aucune régression sur le filtrage, le collapse/expand des sections, l'unchanged toggle

---

## Périmètre

### Hors scope

- Stats par version dans les version cards (SF-89-02)
- Redesign du sélecteur de version (version cards / timeline) — hors scope SF-89-01
- Redesign de la section Chronologie en vraie timeline verticale — V4
- Keyboard navigation
- Modifications backend

---

## Technique

### Endpoint(s)

Aucun endpoint nouveau ou modifié.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular impactés

- `AnalysisDiffComponent` (`analysis-diff.component.ts`) — ajout d'un `effect()` pour auto-trigger, suppression de `onVersionChange()` appelée par bouton
- `analysis-diff.component.html` — suppression du bloc `compare-action`, redesign du bloc `diff-summary`, redesign des `item-reason`, ajout `border-left` dynamique sur section cards, redesign empty state
- `analysis-diff.component.scss` — suppression `.compare-action`, `.stats-bar`, remplacement par `.diff-counters`, redesign `.item-reason` en callout, ajout `.section-card--dominant-*`

---

## Plan de test

### Tests unitaires

- [ ] `AnalysisDiffComponent` — `effect()` auto-trigger : canCompute true → loadDiff() appelé
- [ ] `AnalysisDiffComponent` — `effect()` : canCompute false → loadDiff() non appelé
- [ ] `AnalysisDiffComponent` — `sectionDominantType()` : added > removed → 'added'
- [ ] `AnalysisDiffComponent` — `sectionDominantType()` : 0 changements → 'neutral'
- [ ] `AnalysisDiffComponent` — compteur à 0 → chip non cliquable

### Tests d'intégration

Non applicable (frontend-only, pas de nouvel endpoint).

### Isolation workspace

- [x] Non applicable — aucun accès données nouveau, isolation garantie par les services existants

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité au composant `AnalysisDiffComponent`

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de routing ni d'auth

---

## Dépendances

### Subfeatures bloquantes

Aucune.

---

## Notes et décisions

- L'auto-trigger via `effect()` : on utilise `effect(() => { if (this.canCompute()) this.loadDiff(); })` dans le constructeur, avec un guard pour éviter le double-déclenchement au chargement initial (les deux valeurs sont initialisées à `null` donc `canCompute()` démarre à `false`)
- Les compteurs à 0 : `pointer-events: none; opacity: 0.4` — on ne les masque pas, on les grisse pour donner la vue complète du diff en un coup d'œil
- `sectionDominantType()` : méthode helper qui retourne `'added' | 'removed' | 'enriched' | 'neutral' | 'mixed'` pour chaque section, utilisée dans le template via `[class]` binding
- La border-left est appliquée sur le `:host` de la `mat-card` via une classe CSS dynamique, pas via `ngStyle`, pour rester conforme au design system

# Mini-spec — F-59 / SF-59-01 : UX diff — filtres, stats bar et sections collapsibles

---

## Identifiant

`F-59 / SF-59-01`

## Feature parente

`F-59` — UX diff — filtres, stats bar et sections collapsibles

## Statut

`done`

## Date de création

2026-03-28

## Branche Git

`feat/SF-59-01-diff-ux-filtres-stats`

---

## Objectif

Améliorer l'ergonomie de l'écran de comparaison inter-analyses (`AnalysisDiffComponent`) pour permettre à l'avocat de se concentrer rapidement sur les changements pertinents.

---

## Comportement attendu

### Cas nominal

1. À l'ouverture, les 2 dernières versions sont présélectionnées automatiquement (v[n-1] → v[n])
2. Une barre de stats sticky affiche le total ajouts/suppressions/enrichis/inchangés avec des segments colorés et leur pourcentage
3. Des chips cliquables permettent de filtrer les items : Tout / Ajouts / Suppressions / Enrichis
4. Un toggle "Afficher/Masquer les inchangés" contrôle la visibilité des items inchangés
5. Après chargement d'un diff, les sections sans changements (0 ajouté/supprimé/enrichi) sont automatiquement repliées
6. Chaque section est collapsible via un clic sur son en-tête (chevron indicateur)
7. La raison de chaque item (ajouté/supprimé/enrichi) est affichée en ligne sous l'item sous forme de note contextuelle

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| Moins de 2 versions disponibles | Pas de pré-sélection, état initial vide |
| Diff vide (0 items dans toutes les sections) | Toutes les sections repliées, stats bar à 0 |

---

## Critères d'acceptation

- [x] Pré-sélection automatique des 2 dernières versions au chargement
- [x] Stats bar sticky avec 4 segments colorés (vert/rouge/bleu/gris) + pourcentage
- [x] Chips filtres actifs/inactifs (Tout/Ajouts/Suppressions/Enrichis)
- [x] Toggle inchangés fonctionnel (masqués par défaut, visible en mode "Tout")
- [x] Sections uniquement-inchangées auto-repliées après chargement du diff
- [x] Sections collapsibles au clic, chevron indique l'état
- [x] Raison affichée en ligne sous chaque item (si non-null)

---

## Périmètre

### Hors scope

- Modifications backend (aucune)
- Nouveau endpoint ou modèle de données
- Export ou partage du diff

---

## Technique

### Fichiers impactés

- `analysis-diff.component.ts` — signals `activeFilter`, `unchangedVisible`, `collapsedSections`, computed `totalAdded/Removed/Enriched/Unchanged`, méthodes `setFilter`, `toggleUnchanged`, `isSectionCollapsed`, `toggleSection`, `statPct`, pré-sélection dans `ngOnInit`, auto-collapse dans `loadDiff`
- `analysis-diff.component.html` — stats bar sticky, chips filtres, toggle unchanged, en-têtes collapsibles avec chevron, raisons inline
- `analysis-diff.component.scss` — `.diff-summary-wrapper` sticky, `.stats-bar` segments, `.section-header` clickable, `.section-chevron`, `.item-reason` (--added/--removed/--enriched), `.count-chip` bouton

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires (Karma) — existants alignés

- [x] `canCompute() is false when no versions selected` — reset fromId/toId avant assertion
- [x] `canCompute() is false when fromId equals toId`
- [x] `canCompute() is true when two different versions selected`
- [x] `onVersionChange() calls getDiff when canCompute is true`

### Tests à compléter si nécessaire

- Pré-sélection : versions[1].id assigné à fromId, versions[0].id à toId après chargement

### Isolation workspace

- [x] Non applicable — composant UI pur, pas d'accès direct aux données workspace

---

## Analyse d'impact

### Préoccupations transversales

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing — non touché

### Smoke tests E2E

- [ ] Aucun concerné

---

## Dépendances

- F-54 (diff sémantique) et F-56 (raisons sémantiques) — doivent être mergées (✅ done)

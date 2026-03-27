# Mini-spec — F-56 / SF-56-03 Frontend diff 4 états + tooltip reason

---

## Identifiant

`F-56 / SF-56-03`

## Feature parente

`F-56` — Diff sémantique avec attribution des raisons

## Statut

`ready`

## Date de création

2026-03-27

## Branche Git

`feat/SF-56-03-diff-frontend`

---

## Objectif

Mettre à jour `AnalysisDiffComponent` pour consommer la nouvelle API (4 états, `DiffItem` avec `reason`) : afficher l'état `enriched` en bleu, et afficher la `reason` dans un tooltip Material au survol de chaque item non-inchangé.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur sélectionne deux versions → le diff est chargé.
2. Chaque item est affiché avec sa couleur d'état :
   - **added** → vert (existant `#F0FAF3 / #27AE60`)
   - **removed** → rouge (existant `#FFF5F5 / #C0392B`)
   - **unchanged** → gris (existant)
   - **enriched** → bleu (`#EBF3FB / #2980B9`)
3. Au survol d'un item `added`, `removed` ou `enriched` dont la `reason` est non nulle :
   - Tooltip `matTooltip` affiché avec le texte de la reason.
4. Items `unchanged` : pas de tooltip (reason toujours null).
5. La barre de résumé affiche un compteur `enriched` (chip bleu) si > 0.
6. Les badges par section affichent un `~N` en bleu si enriched > 0.

### Cas d'erreur

- `reason` null → tooltip désactivé (`[matTooltipDisabled]="!item.reason"`)
- Listes `enriched` vides → aucun affichage supplémentaire (comportement identique à l'état actuel)

---

## Critères d'acceptation

- [ ] Items `enriched` affichés en bleu (fond `#EBF3FB`, bord gauche `#2980B9`)
- [ ] Icône distincte pour `enriched` : `auto_awesome`
- [ ] Tooltip visible au survol des items `added/removed/enriched` avec reason non nulle
- [ ] Tooltip absent sur les items `unchanged` et les items sans reason
- [ ] Chip `enriched` dans la barre de résumé si `totalEnriched() > 0`
- [ ] Badge `~N` par section si `sectionEnrichedCount() > 0`
- [ ] Modèles TypeScript mis à jour (`DiffItem`, `TimelineDiffItem`, `SectionDiff` non générique, `TimelineSectionDiff`)
- [ ] Aucune régression sur les états existants (added/removed/unchanged)

---

## Périmètre

### Hors scope

- Backend (SF-56-02, déjà mergé)
- Modification du flux de sélection des versions
- Tests E2E automatisés (pas de Playwright configuré)

---

## Technique

### Composants Angular impactés

- `analysis-diff.component.ts` — modèles internes, computed signals, helper methods
- `analysis-diff.component.html` — rendu items enriched + tooltip
- `analysis-diff.component.scss` — style `diff-item--enriched`
- `case-analysis.model.ts` — mise à jour des interfaces

### Modèles TypeScript cibles

```typescript
interface DiffItem {
  text: string;
  reason: string | null;
}

interface SectionDiff {
  added: DiffItem[];
  removed: DiffItem[];
  unchanged: DiffItem[];
  enriched: DiffItem[];
}

interface TimelineDiffItem {
  date: string;
  evenement: string;
  reason: string | null;
}

interface TimelineSectionDiff {
  added: TimelineDiffItem[];
  removed: TimelineDiffItem[];
  unchanged: TimelineDiffItem[];
  enriched: TimelineDiffItem[];
}

interface AnalysisDiff {
  from: VersionInfo;
  to: VersionInfo;
  faits: SectionDiff;
  pointsJuridiques: SectionDiff;
  risques: SectionDiff;
  questionsOuvertes: SectionDiff;
  timeline: TimelineSectionDiff;
}
```

### Style enriched

```scss
&--enriched {
  background: #EBF3FB;
  border-left-color: #2980B9;
  color: #1A3A5C;
}
```

---

## Plan de test

### Tests unitaires

- [ ] `AnalysisDiffComponent` — `totalEnriched()` : somme correcte sur toutes les sections
- [ ] `AnalysisDiffComponent` — `sectionEnrichedCount()` : compte correct par section

### Tests d'intégration

- Non applicable (pas d'IT frontend configurées)

### Isolation workspace

- Non applicable — composant de présentation pur

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — aucun guard modifié, aucune route ajoutée

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- [SF-56-02] — statut : done

---

## Notes et décisions

- `MatTooltipModule` déjà importé dans le composant — aucune dépendance à ajouter
- `SectionDiff` perd sa généricité TypeScript — TypeScript côté frontend utilise deux types distincts (`SectionDiff` et `TimelineSectionDiff`)
- Le tooltip est affiché avec `matTooltipPosition="above"` et `matTooltipClass="diff-reason-tooltip"`

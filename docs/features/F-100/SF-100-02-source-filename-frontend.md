# Mini-spec — F-100 / SF-100-02 — Noms de documents dans les sources IA (frontend)

## Identifiant
`F-100 / SF-100-02`

## Feature parente
`F-100` — Noms de documents dans les sources IA

## Statut
`ready`

## Date de création
2026-04-01

## Branche Git
`feat/SF-100-02-source-filename-frontend`

---

## Objectif

Afficher le nom de fichier réel dans les badges source des faits/risques/points juridiques, rétroactivement pour les analyses existantes.

---

## Comportement attendu

### Cas nominal

1. `CaseAnalysisResult` reçoit `analysisDocuments?: {index: number, name: string}[]`.
2. `SynthesisComponent` construit un computed signal `sourceMap: Map<string, string>` mappant `"Document 0"` → `"contrat.pdf"` à partir de `analysisDocuments`.
3. Méthode `resolveSource(source: string | null): string | null` :
   - Si source correspond à `/^Document (\d+)$/i` → retourne `sourceMap.get(source)` ou `source` si non trouvé
   - Sinon → retourne source tel quel (futures analyses avec nom direct)
4. Le template utilise `resolveSource(item.source)` partout où `item.source` est affiché (faits, risques, points juridiques).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `analysisDocuments` absent / null | `sourceMap` vide — `resolveSource` retourne la source brute |
| `"Document 5"` mais index absent du snapshot | Retourne `"Document 5"` (fallback gracieux) |
| Source null | Retourne null, badge masqué (comportement inchangé) |

---

## Critères d'acceptation

- [ ] Badge source affiche `"contrat.pdf"` au lieu de `"Document 0"` pour les nouvelles analyses
- [ ] Badge source affiche `"contrat.pdf"` au lieu de `"Document 0"` pour les anciennes analyses (via snapshot)
- [ ] Badge source toujours absent si `source === null`
- [ ] Si `analysisDocuments` absent, affichage dégradé = comportement actuel (pas de régression)

---

## Périmètre

### Hors scope

- Export PDF/DOCX (résolution des sources dans les exports — feature séparée)
- Modification du backend

---

## Technique

### Composants Angular

- `CaseAnalysisResult` (model) — nouveau champ `analysisDocuments?: {index: number, name: string}[]`
- `SynthesisComponent` (ts) — computed `sourceMap` + méthode `resolveSource()`
- `SynthesisComponent` (html) — `resolveSource(item.source)` dans les 3 sections

---

## Plan de test

### Tests unitaires / composant

- [ ] `resolveSource("Document 0")` → nom du snapshot (`"contrat.pdf"`)
- [ ] `resolveSource("contrat.pdf")` → retourne tel quel
- [ ] `resolveSource(null)` → null
- [ ] `resolveSource("Document 5")` si index absent → `"Document 5"` (fallback)
- [ ] Badge DOM affiche le nom résolu pour les faits

### Isolation workspace

- [x] Non applicable — affichage uniquement

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — changement d'affichage uniquement

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-100-01 — doit être mergée avant pour que le champ `analysisDocuments` soit disponible en API

---

## Notes et décisions

- Le computed `sourceMap` est recalculé à chaque changement de `synthesis()` — pas de side-effect
- La regex `/^Document (\d+)$/i` couvre les valeurs "Document 0", "document 1", etc. retournées par le LLM

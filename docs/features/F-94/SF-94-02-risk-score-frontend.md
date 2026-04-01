# Mini-spec — F-94 / SF-94-02 — Score de risque global — Frontend

## Identifiant
`F-94 / SF-94-02`

## Feature parente
`F-94` — Score de risque global du dossier

## Statut
`in-review`

## Date de création
2026-04-01

## Branche Git
`feat/SF-94-02-risk-score-frontend`

---

## Objectif

Afficher le score de risque global (`riskLevel` + `riskScore`) calculé par le backend dans la liste des dossiers et dans la synthèse d'analyse.

---

## Comportement attendu

### Cas nominal

1. Le modèle `CaseFile` expose `riskLevel: string | null` et `riskScore: number | null`.
2. Le modèle `CaseAnalysisResult` expose `riskLevel: string | null` et `riskScore: number | null`.
3. Dans `CaseFilesListComponent` : colonne Statut — si `riskLevel` non null, un badge coloré est affiché à côté du badge de statut.
4. Dans `SynthesisComponent` : `title-row` du header — si `riskLevel` non null, un badge coloré est affiché.
5. Le badge affiche le niveau traduit + le score entre parenthèses si disponible : ex. `Élevé (82)`.
6. Couleurs des badges :
   - `FAIBLE` → vert (`#27AE60`) sur fond `#e8f8f0`
   - `MOYEN` → or (`#C9973A`) sur fond `#fef6e6`
   - `ELEVE` → rouge (`#C0392B`) sur fond `#fdf3f2`

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `riskLevel` null | Badge absent — aucune erreur |
| `riskScore` null | Badge affiche uniquement le niveau sans parenthèses |
| `riskLevel` valeur inconnue | Valeur brute affichée comme fallback |

---

## Critères d'acceptation

- [x] `CaseFile` : champs `riskLevel` et `riskScore` présents dans le modèle TypeScript
- [x] `CaseAnalysisResult` : champs `riskLevel` et `riskScore` présents dans le modèle TypeScript
- [x] `CaseFilesListComponent` : badge `.risk-badge` affiché si `riskLevel` non null
- [x] `SynthesisComponent` : badge `.risk-badge` affiché dans le header si `riskLevel` non null
- [x] Couleurs conformes au design system (Success vert, Or juridique, Error rouge)
- [x] Police Inter, padding multiples de 4px
- [x] Badge absent si `riskLevel` null
- [x] Tests unitaires couvrant R-03 et R-04

---

## Périmètre

### Hors scope
- Calcul ou modification du score (backend — SF-94-01)
- Filtrage ou tri par score de risque
- Export PDF du score (affichage simple uniquement)

---

## Technique

### Composants Angular impactés

| Composant | Modification |
|-----------|-------------|
| `case-file.model.ts` | Ajout `riskLevel`, `riskScore` |
| `case-analysis.model.ts` | Ajout `riskLevel`, `riskScore` dans `CaseAnalysisResult` |
| `CaseFilesListComponent` | Méthodes `riskLabel()`, `riskClass()` + template + SCSS |
| `SynthesisComponent` | Méthodes `riskLabel()`, `riskClass()` + template + SCSS |

### Migration Liquibase
- Non applicable — frontend uniquement

---

## Plan de test

### Tests unitaires

- [x] `SynthesisComponent` R-03 — synthesis avec riskLevel=ELEVE → badge `.risk-badge` présent, texte "Élevé (82)"
- [x] `SynthesisComponent` R-04 — synthesis avec riskLevel=null → badge absent
- [x] `CaseFilesListComponent` — dossier avec riskLevel=MOYEN → badge présent
- [x] `CaseFilesListComponent` — dossier sans riskLevel → badge absent

### Tests d'intégration
- Non applicable — frontend uniquement

### Isolation workspace
- Non applicable — les données sont déjà filtrées par le service existant

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout de champs d'affichage sur des modèles existants, aucun impact sur auth/workspace/routing

### Smoke tests E2E concernés

- Aucun smoke test concerné — pas de changement de routing ou d'auth

---

## Dépendances

### Subfeatures bloquantes

- SF-94-01 — statut : done

---

## Notes et décisions

- Les méthodes `riskLabel()` et `riskClass()` sont dupliquées dans les deux composants (pas de service partagé) — acceptable pour deux usages distincts, pas de logique métier.

# Mini-spec — F-IA-03 / SF-IA-03-09 Cohérence IA sur F-IM-05 Titre séjour

## Identifiant

`F-IA-03 / SF-IA-03-09`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-09-coherence-titre-sejour`

---

## Objectif

Étendre le moteur de cohérence à F-IM-05 Titre séjour sur 2 champs : `motif` (enum 5 valeurs) et `nationaliteUe` (boolean). Sources : détection IA (`typeTitreSejourCode` et `nationaliteUe` extraits par SF-IM-05-04), F-96, questions IA. Warning uniquement sur le motif (choix subjectif), warning sur nationalité (factuel).

---

## Comportement attendu

### Champs surveillés

| Champ user | Codes IA-03 | Source IA primaire |
|---|---|---|
| `motif` (TRAVAIL / ETUDES / FAMILLE / ASILE / AUTRE) | `IM05_MOTIF` | `typeTitreSejourCode` IA via table CODE_TO_MOTIF |
| `nationaliteUe` (boolean) | `IM05_NATIONALITE_UE` | `nationaliteUe` IA boolean |

### Hiérarchie `motif`

| Étape | Condition | Niveau |
|---|---|---|
| A | user vide | rien |
| B | F-96 VERIFIED + `expected_value` ∈ {TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE} → diverge | `warning` |
| C | Question IA "oui" + `expected_value` ∈ enum → diverge | `warning` |
| D | IA `typeTitreSejourCode` → mapping CODE_TO_MOTIF → diverge | `warning` |
| E | sinon | rien |

MULTI si ≥ 2 sources convergent.

### Hiérarchie `nationaliteUe`

| Étape | Condition | Niveau |
|---|---|---|
| A | (toujours évaluable, boolean) | — |
| B | IA `nationaliteUe` != user | `warning` |
| C | sinon | rien |

Pas de F-96 / question IA sur nationaliteUe pour cette SF (signal suffisant via détection IA directe).

### Pourquoi warning et non blocker

F-IM-05 est un **arbre décisionnel suggestif** : le motif et la nationalité choisis par l'avocat alimentent une recommandation. Divergence = signal intéressant, pas un blocage juridique. Cohérent avec le pattern F-DT-07 (factuel, warning only).

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `aiData` null | aucune alerte |
| IA code non mappable (CARTE_RESIDENT) | pas d'alerte motif (titre générique, pas de mapping) |
| Résultat F-IM-05 sauvegardé | alertes gelées |
| `expected_value` hors enum | ignoré |

---

## Critères d'acceptation

- [ ] Prompts F-96 et questions IA étendus pour le critère `IM05_MOTIF` avec `expected_value` parmi les 5 motifs.
- [ ] Nouveau computed `coherenceAlerts: Partial<Record<IM05AlertField, alert>>` sur `ImmigrationTitleDecisionSectionComponent`.
- [ ] 2 nouveaux `@Input` (procedureChecks, aiQuestions) + `synthesis` déjà utilisé
- [ ] Surveillance motif : hiérarchie A-E avec MULTI
- [ ] Surveillance nationaliteUe : IA boolean uniquement
- [ ] Warning only, pas de blocker
- [ ] Alertes gelées quand décision chargée
- [ ] Rétrocompat totale SF-IM-05-04 préservée
- [ ] Tests frontend (hiérarchie motif + nationaliteUe + MULTI + fallback).

---

## Périmètre

### Hors scope (explicite)

- Surveillance de `duree`, `situationFamiliale`, `country` (non extraits par IA, choix produit).
- Surveillance du titre recommandé post-génération (signal post-hoc, ROI faible quand le questionnaire est cohérent).
- Sources pièces manquantes : non alignées avec F-IM-05.
- Niveau `info` / justification obligatoire.
- Extension à F-IM-06/07.

---

## Valeurs initiales

Aucune entité créée.

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| `expected_value` pour IM05_MOTIF | Oui si critere_code = IM05_MOTIF | un de TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE | upper-case |

---

## Technique

### Endpoint(s)

Aucun — prompts uniquement.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] **Non applicable**.

### Composants Angular

- `ImmigrationTitleDecisionSectionComponent` :
  - 2 nouveaux `@Input` (`procedureChecks`, `aiQuestions`)
  - Signaux miroirs + ngOnChanges
  - Types `IM05CoherenceAlert`, `IM05AlertField = 'MOTIF' | 'NATIONALITE_UE'`, `IM05AlertSource`
  - Computed `coherenceAlerts`, `alertsSummary`
  - Helpers `buildMotifAlert`, `buildNationaliteAlert`
  - Badges + tooltip + bandeau récap
  - Alertes gelées quand `!showForm() || decision()`

### Prompts

- `CaseAnalysisService`, `EnrichedAnalysisService`, `AiQuestionService` : code `IM05_MOTIF` ajouté avec `expected_value` parmi 5 motifs.

---

## Plan de test

### Tests unitaires frontend

Motif :
- [ ] F-96 VERIFIED expected=ETUDES, user TRAVAIL → warning F96.
- [ ] Question IA "oui" expected=FAMILLE, user TRAVAIL → warning QUESTION_IA.
- [ ] IA code VLS_TS_ETUDIANT (→ETUDES), user TRAVAIL → warning IA.
- [ ] IA code CARTE_RESIDENT (pas de mapping), user TRAVAIL → pas d'alerte.
- [ ] F-96 + IA alignés contre user → MULTI.
- [ ] Aucune source → pas d'alerte.

Nationalité UE :
- [ ] IA nationaliteUe=true, user false → warning.
- [ ] IA nationaliteUe=true, user true → pas d'alerte.
- [ ] IA nationaliteUe=null → pas d'alerte.

Transverses :
- [ ] Compteur agrège motif + nationaliteUe.
- [ ] Décision chargée → alertes gelées.
- [ ] `expected_value` hors enum → ignoré.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| `ImmigrationTitleDecisionSectionComponent` | 2 Inputs + computed + UI, SF-IM-05-04 intacte |
| Prompts IA | rallongement modéré |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IM-05-04` (Done) — fournit `typeTitreSejourCode` normalisé et `nationaliteUe` boolean + table CODE_TO_MOTIF côté front.
- `SF-IA-03-05` (Done) — infrastructure `expected_value`.

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi surveillance motif en warning et pas blocker** : le motif est un choix subjectif de l'avocat (la demande d'origine peut être changée). Une divergence avec l'IA est un signal utile mais pas un blocage.
- **Pourquoi surveillance nationaliteUe séparée** : c'est un booléen factuel, pas un enum. Pas de mapping nécessaire. Simple comparaison IA vs user.
- **Pourquoi pas de surveillance du titre recommandé en sortie** : post-hoc, signal faible quand le questionnaire est déjà cohérent (la recommandation est déterministe des inputs). Si les inputs sont corrects, la recommandation l'est. Si les inputs sont incorrects, on les surveille déjà.
- **Pourquoi CARTE_RESIDENT ne déclenche pas d'alerte motif** : cohérent avec CODE_TO_MOTIF qui n'a pas de mapping (titres génériques multi-motifs). Ne pas forcer une alerte sur un code ambigu.

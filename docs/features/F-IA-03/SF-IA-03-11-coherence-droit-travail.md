# Mini-spec — F-IA-03 / SF-IA-03-11 Cohérence IA sur F-IM-07 Droit au travail

## Identifiant

`F-IA-03 / SF-IA-03-11`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-11-coherence-droit-travail`

---

## Objectif

Étendre le moteur de cohérence à F-IM-07 Droit au travail sur 1 champ unique : `titreType` (enum 16 valeurs). Bonus : ajouter le pré-remplissage depuis `typeTitreSejourCode` IA (aujourd'hui absent — SF-IM-07-03 n'a pas de prefill). Closure du domaine immigration (3/3 outils).

---

## Comportement attendu

### Champ surveillé

| Champ user | Code IA-03 | Source IA |
|---|---|---|
| `titreType` (enum 16 codes immigration) | `IM07_TITRE_TYPE` | `typeTitreSejourCode` IA (SF-IM-05-04) |

### Pré-remplissage (bonus)

Aujourd'hui F-IM-07 n'a aucun pré-remplissage. On ajoute :
- Si `aiData.typeTitreSejourCode` est un code valide compatible avec le pays workspace → `titreType` pré-sélectionné
- Provenance note "Pré-rempli depuis l'analyse IA" sous le champ
- Effacée à la modification manuelle

### Hiérarchie de cohérence

| Étape | Condition | Niveau |
|---|---|---|
| A | user vide | rien |
| B | F-96 VERIFIED + `expected_value` parmi 16 codes → diverge | `warning` |
| C | Question IA "oui" + `expected_value` → diverge | `warning` |
| D | IA `typeTitreSejourCode` → diverge | `warning` |
| E | sinon | rien |

MULTI si ≥ 2 sources convergent.

### Pourquoi warning only

F-IM-07 est **purement dérivé** : l'avocat choisit le titre, l'outil déduit le droit au travail. L'avocat peut délibérément tester un titre différent pour comparer. Divergence avec l'IA = signal utile, pas un blocage.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `aiData` null | pas d'alerte, pas de prefill |
| Code IA d'un pays différent du workspace | pas de prefill, pas d'alerte (incohérence amont) |
| Résultat F-IM-07 chargé | alertes gelées |
| `expected_value` hors enum | ignoré |

---

## Critères d'acceptation

- [ ] Prompts F-96 et questions IA étendus avec code `IM07_TITRE_TYPE` + `expected_value` parmi 16 codes.
- [ ] Nouveau `@Input aiData?: ImmigrationExtractedData | null` sur `ImmigrationWorkRightSectionComponent`.
- [ ] 2 nouveaux `@Input` (`procedureChecks`, `aiQuestions`).
- [ ] Pré-remplissage `titreType` depuis `aiData.typeTitreSejourCode` quand cohérent avec le pays.
- [ ] Provenance note effacée à la modification manuelle.
- [ ] Computed `coherenceAlert` (enum simple, un seul champ) + `alertsSummary`.
- [ ] Hiérarchie A-E avec MULTI, warning only.
- [ ] Alerte gelée quand résultat chargé.
- [ ] Rétrocompat : formulaire fonctionne sans aiData.
- [ ] Tests frontend (prefill + cohérence + MULTI + freeze).

---

## Périmètre

### Hors scope (explicite)

- Surveillance de `country` (imposé par le workspace, pas un choix produit).
- Modification du calcul du droit au travail (dérivé déterministe du titre — inchangé).
- Niveau `info` + justification obligatoire.

---

## Valeurs initiales

Aucune entité créée.

---

## Contraintes de validation

| Champ | Format | Normalisation |
|-------|--------|---------------|
| `expected_value` IM07_TITRE_TYPE | 1 des 16 codes | upper-case, filtré enum |

---

## Technique

### Endpoint(s)

Aucun — prompts uniquement.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] **Non applicable**.

### Composants Angular

- `ImmigrationWorkRightSectionComponent` :
  - 3 nouveaux `@Input` (`aiData`, `procedureChecks`, `aiQuestions`)
  - Signaux miroirs
  - Types `IM07CoherenceAlert`, `IM07AlertSource` (F96 | QUESTION_IA | IA | MULTI)
  - `prefillFromAi()` + signal `provenanceTitreType`
  - Computed `coherenceAlert`, `alertsSummary`
  - Helper `buildTitreAlert` (hiérarchie A-E + MULTI)
  - Alertes gelées quand `!showForm() || result()`
  - Badge + tooltip + bandeau récap + provenance note
- `CaseFileDetailComponent` : passer les 3 nouveaux inputs

### Prompts

- `CaseAnalysisService`, `EnrichedAnalysisService`, `AiQuestionService` : code `IM07_TITRE_TYPE` ajouté avec `expected_value` obligatoire parmi 16 codes.

---

## Plan de test

### Tests unitaires frontend

Prefill :
- [ ] `aiData.typeTitreSejourCode=VLS_TS_ETUDIANT`, workspace FR → `titreType` pré-rempli, provenance visible.
- [ ] Code IA d'un autre pays → pas de prefill (workspace FR, code IA BE).
- [ ] Code IA inconnu → pas de prefill.
- [ ] Résultat chargé → pas de prefill IA.
- [ ] Modif manuelle → provenance effacée.

Cohérence :
- [ ] F-96 VERIFIED expected=CARTE_RESIDENT, user VLS_TS_SALARIE → warning F96.
- [ ] Question "oui" expected=CST_VPF, user VLS_TS_SALARIE → warning QUESTION_IA.
- [ ] IA typeTitreSejourCode mismatch → warning IA.
- [ ] 3 sources convergent → MULTI.
- [ ] Match → pas d'alerte.
- [ ] expected_value hors enum → ignoré.
- [ ] Résultat chargé → alertes gelées.
- [ ] aiData null → pas d'alerte, pas de prefill.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| `ImmigrationWorkRightSectionComponent` | 3 Inputs + prefill + computed + UI |
| Prompts IA | rallongement mineur |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IM-05-04` (Done) — fournit `typeTitreSejourCode` normalisé et l'infrastructure IA.
- `SF-IA-03-05` (Done) — infrastructure `expected_value`.

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi SF-IA-03-11 malgré le ROI faible signalé initialement** : complétude du domaine immigration. Clôt F-IA-03 sur les 9 outils identifiés. Coût faible (~0,5 j) pour la valeur de cohérence systémique.
- **Pourquoi surveiller `titreType` malgré la redondance avec F-IM-05** : les deux outils sont indépendants dans l'UI — l'avocat peut ouvrir F-IM-07 sans passer par F-IM-05. La cohérence locale est utile.
- **Bonus prefill** : F-IM-07 n'avait aucun pré-remplissage. L'ajouter au passage améliore l'UX sans surcoût significatif.
- **Réutilisation des 16 codes IMMIGRATION_TITLE_CODES** côté front (pas de nouveau set à maintenir).

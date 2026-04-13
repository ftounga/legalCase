# Mini-spec — F-IA-03 / SF-IA-03-05 Cohérence IA sur F-DT-09 Indemnités

## Identifiant

`F-IA-03 / SF-IA-03-05`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-05-coherence-indemnites`

---

## Objectif

Étendre le moteur de cohérence à F-DT-09 Indemnités, avec 3 codes (`DT09_TYPE_RUPTURE`, `DT09_ANCIENNETE_ANNEES`, `DT09_SALAIRE_REFERENCE`). Introduire une extension générique du modèle — colonne `expected_value` sur `procedure_checks` et `ai_questions` — pour gérer les critères à valeur énumérée (type de rupture à 5 valeurs) tout en préservant la rétrocompatibilité avec les critères binaires de F-DT-08.

---

## Comportement attendu

### Cas nominal

1. À chaque analyse IA, le prompt demande à Claude, pour les points F-96 et les questions IA portant sur un critère **à valeur énumérée ou numérique**, de renseigner un champ `expected_value` en plus de `critere_code`.
2. Les champs sont persistés en base (migration 071).
3. `IndemniteComparatifSectionComponent` reçoit les 4 `@Input` (synthesis.compensationEstimate, procedureChecks, aiQuestions, piecesManquantesDetails) et applique la hiérarchie A-F du moteur.
4. Le computed `coherenceAlerts` adapte la comparaison au type du champ (enum exact pour TYPE_RUPTURE, seuil numérique pour ANCIENNETE et SALAIRE).

### Convention `expected_value`

| Nature du champ | `expected_value` | Signal clair |
|---|---|---|
| Binaire (codes DT08) | **omis** (null) | VERIFIED = OUI, NON_COMPLIANT = NON ; answer "oui" = OUI, "non" = NON (rétrocompat SF-IA-03-01/02/03) |
| Énuméré (ex: DT09_TYPE_RUPTURE) | valeur attendue (ex: `RUPTURE_CONVENTIONNELLE`) | Seulement les affirmations positives portent un signal : F-96 VERIFIED ou question répondue "oui" → type attendu = `expected_value`. NON_COMPLIANT et "non" sont ignorés (signal ambigu — "ce n'est pas X" ne dit pas ce que c'est). |
| Numérique (DT09_ANCIENNETE, DT09_SALAIRE) | nombre sous forme texte | Non utilisé dans cette SF : pour les champs numériques on se limite à la source IA avec seuil (pattern SF-IA-03-04). Le champ existe pour usage futur. |

### Hiérarchie des sources par champ

**`DT09_TYPE_RUPTURE`** (enum, enjeu décisionnel) :
| Étape | Condition | Niveau |
|---|---|---|
| A | user vide | aucune alerte |
| B | F-96 VERIFIED avec `expected_value` = valeur connue | `blocker` si user ≠ valeur attendue |
| C | question IA "oui" avec `expected_value` = valeur connue | `blocker` si user ≠ valeur attendue |
| D | `compensationEstimate.typeRupture` = valeur connue | `blocker` si user ≠ valeur IA |
| E | sinon | rien |

Si plusieurs sources convergent contre l'user → `MULTI`.

**`DT09_ANCIENNETE_ANNEES`** (numérique) :
- Source IA seule : `compensationEstimate.ancienneteAnnees` (+ `ancienneteMois/12`)
- Seuil : écart ≥ 0,5 an → `warning`
- F-96 et questions IA ignorés pour cette SF (pattern SF-IA-03-04)

**`DT09_SALAIRE_REFERENCE`** (numérique) :
- Source IA seule : `compensationEstimate.salaireReference`
- Seuil : écart relatif ≥ 5 % → `warning`

### Cas d'erreur

| Situation | Comportement |
|-----------|---------------------|
| `expected_value` absent pour un critère binaire | rétrocompat : statut/answer interprété comme avant |
| `expected_value` présent mais valeur inconnue pour enum | ignoré (fail-open) |
| F-96 NON_COMPLIANT sur critère enum | ignoré (signal ambigu) |
| Question IA "non" sur critère enum | ignoré |
| IA détecte type non couvert (DEMISSION…) | pas d'alerte (incohérence amont) |
| Aucune source disponible | pas d'alerte |

---

## Critères d'acceptation

- [ ] Migration `071-add-expected-value-to-checks-and-questions.xml` ajoute `expected_value VARCHAR(50) NULL` sur `procedure_checks` et `ai_questions`.
- [ ] `ProcedureCheckResponse` et `AiQuestionResponse` exposent `expectedValue`.
- [ ] Prompts `points_procedure` et questions IA étendus pour autoriser `expected_value` (convention : seulement pour critères énumérés). Les prompts existants DT08 restent valides (expected_value optionnel).
- [ ] `ProcedureCheckService.parsePointsProcedure()` et `AiQuestionService.parseQuestions()` parsent et persistent `expected_value`.
- [ ] Rétrocompat totale : les 40+ tests existants de SF-IA-03-01/02/03 passent sans modification de logique.
- [ ] `IndemniteComparatifSectionComponent` reçoit les 4 `@Input` et applique la hiérarchie par champ.
- [ ] `DT09_TYPE_RUPTURE` : blocker si F-96 VERIFIED+expected_value / question IA "oui"+expected_value / IA détection contredisent le choix user.
- [ ] `DT09_ANCIENNETE_ANNEES` : warning si écart IA ≥ 0,5 an, seuil strict.
- [ ] `DT09_SALAIRE_REFERENCE` : warning si écart IA ≥ 5 %.
- [ ] Compteur `alertsSummary: {total, blockers}` agrège les 3 champs.
- [ ] Badges visibles en haut et à côté de chaque champ, tooltip décrivant la source et la preuve.
- [ ] Pas de backfill : les anciennes procedure_checks / ai_questions restent fonctionnelles sans `expected_value`.
- [ ] Tests backend (extension prompt, parsing dual format, rétrocompat).
- [ ] Tests frontend (matrice 3 champs × sources × match/mismatch, fallback, MULTI, legacy).

---

## Périmètre

### Hors scope (explicite)

- Cohérence IA sur ANCIENNETE/SALAIRE via F-96 ou questions IA (trop de complexité, peu de valeur). Rétrograder si besoin ultérieur.
- Exploitation des pieces manquantes sur enum TYPE_RUPTURE (pattern à définir plus tard — "convention RC manquante" ne contredit pas explicitement un type).
- Extension à d'autres outils (F-FA-*, F-IM-*) — subfeatures suivantes.
- Niveau `info` + justification obligatoire sur blocker.
- Backfill des anciennes entrées sans `expected_value`.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `procedure_checks.expected_value` | `null` | rempli par parsing IA si présent |
| `ai_questions.expected_value` | `null` | idem |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Normalisation |
|-------|-------------|-------------|----------------------------|---------------|
| `expected_value` | Non | 50 | texte libre côté DB ; côté front, validé contre les valeurs connues du critère | trim, upper-case pour cohérence |

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---------|-----|------------|
| GET | `/api/v1/case-files/{id}/analyses/{analysisId}/procedure-checks` | `expectedValue` ajouté au payload |
| GET | `/api/v1/case-files/{id}/ai-questions` | `expectedValue` ajouté au payload |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `procedure_checks` | ALTER — ajout colonne `expected_value VARCHAR(50) NULL` | migration 071 |
| `ai_questions` | ALTER — idem | même changeset |

### Migration Liquibase

- [x] Oui — `071-add-expected-value-to-checks-and-questions.xml`

Réversible : DROP COLUMN nullable non backfillée.

### Composants Angular

- `ProcedureCheck` + `AiQuestion` models : `expectedValue?: string | null`
- `IndemniteComparatifSectionComponent` :
  - 4 nouveaux `@Input` (synthesis inchangé, procedureChecks, aiQuestions, piecesManquantes)
  - Signaux miroirs + ngOnChanges
  - Types `IndemniteCoherenceAlert` et `IndemniteAlertField = 'TYPE_RUPTURE' | 'ANCIENNETE' | 'SALAIRE'`
  - Computed `coherenceAlerts: Record<IndemniteAlertField, IndemniteCoherenceAlert>`
  - Computed `alertsSummary: {total, blockers}`
  - Helpers : `buildTypeRuptureSources`, tolérance numérique (réutilisation dateDaysDiff / percentDiff seraient possibles mais numériques différents ici)
- `CaseFileDetailComponent` passe les 3 listes à la section indemnité (déjà passe `synthesis`)

---

## Plan de test

### Tests unitaires backend

- [ ] `ProcedureCheckService.parsePointsProcedure()` : objet avec `expected_value` → persiste.
- [ ] Format legacy sans `expected_value` → `expectedValue` = null (rétrocompat).
- [ ] `expected_value` blank → null.
- [ ] `AiQuestionService.parseQuestions()` : format objet avec `expected_value` → persiste.
- [ ] Rétrocompat suite complète verte (10+ tests SF-IA-03-03).

### Tests unitaires frontend

TYPE_RUPTURE :
- [ ] F-96 VERIFIED + expected_value=RC, user LICENCIEMENT → blocker F96, expected RC.
- [ ] F-96 VERIFIED + expected_value=RC, user RC → aucune alerte.
- [ ] F-96 NON_COMPLIANT + expected_value=RC → ignoré (ambigu sur enum).
- [ ] Question IA "oui" + expected_value=ECO, user LICENCIEMENT → blocker QUESTION_IA.
- [ ] Question IA "non" sur enum → ignoré.
- [ ] Priorité F-96 > Question IA > IA.
- [ ] F-96 et IA s'alignent contre user → MULTI.
- [ ] IA seule (compensationEstimate) contredit user → blocker IA.
- [ ] IA extrait type non couvert (DEMISSION) → pas d'alerte.

ANCIENNETE :
- [ ] IA = 10 ans, user 10 → pas d'alerte.
- [ ] IA = 10 ans, user 10.4 → pas d'alerte (écart < 0.5).
- [ ] IA = 10 ans, user 11 → warning.
- [ ] IA null → pas d'alerte.
- [ ] user = 0 → pas d'alerte (non saisi).

SALAIRE :
- [ ] IA = 4000, user 4100 (2.5 %) → pas d'alerte.
- [ ] IA = 4000, user 4300 (7.5 %) → warning.
- [ ] user = 0 → pas d'alerte.

Transverses :
- [ ] Compteur `{total, blockers}` agrège les 3 champs.
- [ ] Aucune source → aucune alerte.
- [ ] Legacy result (sans typeRupture) → comportement SF-IA-03-04 strict.

### Tests d'intégration

- [ ] `GET /procedure-checks` renvoie `expectedValue` si persisté.
- [ ] `GET /ai-questions` idem.

### Isolation workspace

- [x] Applicable — héritée des endpoints existants.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — extension localisée.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `ProcedureCheckService` | parsing étendu avec `expected_value` optionnel | tests SF-IA-03-02 conservés |
| `AiQuestionService` | idem | tests SF-IA-03-03 conservés |
| `LicenciementSectionComponent` | reçoit `expectedValue` dans procedureChecks/aiQuestions — doit ignorer (critères DT08 binaires) | tests SF-IA-03-01/02/03 intacts |
| Prompts IA | rallongement modéré | vérifier longueur |

### Smoke tests E2E concernés

- [ ] Aucun concerné.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-09-04` (Done) — expose le sélecteur `typeRupture` côté frontend et le persiste.
- `SF-IA-03-02` (Done) — pattern F-96 avec `critere_code`.
- `SF-IA-03-03` (Done) — pattern questions IA + pièces.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi `expected_value` sur les deux tables** : même sémantique (valeur attendue par une source), cohérence. Le même champ peut servir aux futures extensions (dossiers famille avec choix multiples, immigration avec types de titre).
- **Pourquoi ignorer NON_COMPLIANT et "non" sur enum** : signal ambigu — "ce n'est pas X" ne précise pas ce que c'est. Mieux vaut ne rien dire que produire un faux blocker.
- **Pourquoi blocker sur IA seule pour TYPE_RUPTURE** (contrairement à SF-IA-03-01 où IA sur critère bloquant = blocker mais sur non bloquant = warning) : TYPE_RUPTURE est **toujours** décisionnel (choix de barème). L'analogie "bloquant/non bloquant" de DT08 ne s'applique pas ici.
- **Pourquoi pas F-96/questions pour ANCIENNETE/SALAIRE** : valeurs numériques. Une question "l'ancienneté est-elle de 12 ans ?" avec `expected_value=12` nécessite une logique de tolérance côté client qui ajoute du bruit sans gain clair. Rester sur source IA + seuil suffit.
- **Pas de backfill** : les points/questions déjà en base resteront avec `expected_value = null` et continueront d'être interprétés en binaire. Les nouvelles analyses bénéficient du nouveau modèle.

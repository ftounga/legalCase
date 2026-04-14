# Mini-spec — F-IA-03 / SF-IA-03-07 Cohérence IA sur F-FA-06 Calendrier garde

## Identifiant

`F-IA-03 / SF-IA-03-07`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-07-coherence-calendrier-garde`

---

## Objectif

Étendre le moteur de cohérence à F-FA-06 Calendrier de garde : détecter les incohérences entre le mode de garde choisi par l'avocat et les preuves disponibles (F-96, questions IA, détection IA coarse et détaillée). Un seul code enum surveillé (`FA06_MODE_GARDE`) avec 6 valeurs.

---

## Comportement attendu

### Code surveillé

`FA06_MODE_GARDE` énuméré avec les 6 valeurs déjà définies dans SF-FA-06-04 :
- FR : `ALTERNEE_FR`, `DVH_CLASSIQUE_FR`, `DVH_ELARGI_FR`
- BE : `ALTERNEE_BE`, `SECONDAIRE_BE`, `SECONDAIRE_ELARGI_BE`

### Hiérarchie des sources

Pour le champ `gardeCode` sélectionné par l'avocat :

| Étape | Condition | Niveau |
|---|---|---|
| A | user vide | rien |
| B | F-96 VERIFIED + `expected_value` parmi 6 → user diverge | `blocker` |
| C | Question IA "oui" + `expected_value` parmi 6 → user diverge | `blocker` |
| D | IA `pensionAlimentaireEstimate.modeGardeDetaille` (6 valeurs) → user diverge | `blocker` |
| E | IA `pensionAlimentaireEstimate.modeGarde` binaire (ALTERNEE/EXCLUSIVE) → catégorie incompatible avec le mode user | `warning` |
| F | Sinon | rien |

Mapping catégorie IA coarse (étape E) :
- `ALTERNEE` correspond à `ALTERNEE_FR` ou `ALTERNEE_BE`
- `EXCLUSIVE` correspond à `DVH_CLASSIQUE_FR`, `DVH_ELARGI_FR`, `SECONDAIRE_BE`, `SECONDAIRE_ELARGI_BE`
- Mismatch de catégorie → warning
- Match de catégorie → pas d'alerte (signal IA coarse confirme au niveau catégorie, ne peut pas trancher au niveau précis)

Si plusieurs sources convergent contre l'user → `MULTI`.

### Cas particulier : étape D absorbée par le pré-remplissage

SF-FA-06-04 pré-remplit déjà `gardeCode` depuis `modeGardeDetaille` IA quand il est compatible avec le pays. Donc en pratique, l'alerte D ne se déclenche que si l'avocat a modifié manuellement le champ après pré-remplissage, ou si le mode IA est du pays opposé. L'alerte D reste utile comme garde-fou.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Résultat F-FA-06 déjà généré | alerte gelée (cohérence calculée seulement pendant l'édition du formulaire) |
| `expected_value` hors enum 6 valeurs | ignoré |
| IA mode du pays opposé vs user du pays workspace | traité comme étape E si la catégorie coarse diverge ; sinon ignoré |
| Aucune source | rien |

---

## Critères d'acceptation

- [ ] Prompts F-96 et questions IA étendus pour le critère `FA06_MODE_GARDE` avec `expected_value` obligatoire parmi 6 valeurs.
- [ ] `CalendrierGardeSectionComponent` reçoit 3 nouveaux `@Input` (procedureChecks, aiQuestions, synthesis — déjà partiellement passés).
- [ ] Computed `coherenceAlert` unique pour le champ mode.
- [ ] Badge + tooltip à côté du select mode, bandeau récap si alerte.
- [ ] Alerte gelée quand résultat chargé (même principe que SF-IA-03-04).
- [ ] Rétrocompat : tous les tests F-IA-03 antérieurs restent verts ; SF-FA-06-04 préservée.
- [ ] Tests frontend (hiérarchie A-F, mapping coarse, MULTI, fallback, résultat sauvegardé).

---

## Périmètre

### Hors scope (explicite)

- Cohérence sur les noms des parents (texte libre).
- Autres champs F-FA-06 (`dateDebut`, `nbEnfants`) — n'existent pas dans le composant actuel.
- Extension à F-FA-05 (Partage immobilier) — SF ultérieure.
- Niveau `info` + justification obligatoire.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Normalisation |
|-------|-------------|------------------|---------------|
| `critere_code` reconnu | Non | `FA06_MODE_GARDE` | upper-case |
| `expected_value` | Oui si critere_code = FA06_MODE_GARDE | un de 6 | upper-case, filtré contre l'enum |

---

## Technique

### Endpoint(s)

Aucun — purement frontend + prompts.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable**.

### Composants Angular

- `CalendrierGardeSectionComponent` :
  - 3 nouveaux `@Input` (procedureChecks, aiQuestions, synthesis)
  - Signaux miroirs + ngOnChanges
  - Types `GardeCoherenceAlert`, `GardeAlertSource`
  - Computed `coherenceAlert: GardeCoherenceAlert | null`
  - Computed `alertsSummary: {total, blockers}`
  - Helpers : `buildGardeAlert()`
  - Badge + tooltip + bandeau récap
  - Alerte gelée après `loadExisting` succès ou `generate` succès
- `CaseFileDetailComponent` : passer `[procedureChecks]`, `[aiQuestions]`, `[synthesis]`

### Prompts

- `CaseAnalysisService` + `EnrichedAnalysisService` : ajout du code `FA06_MODE_GARDE` (énuméré) à la section `points_procedure`, avec `expected_value` obligatoire.
- `AiQuestionService` : idem pour les questions.

---

## Plan de test

### Tests unitaires frontend

Hiérarchie :
- [ ] F-96 VERIFIED + expected_value=DVH_ELARGI_FR, user ALTERNEE_FR → blocker F96.
- [ ] Question IA "oui" + expected_value=SECONDAIRE_BE, user ALTERNEE_BE → blocker QUESTION_IA.
- [ ] IA modeGardeDetaille=ALTERNEE_FR, user DVH_CLASSIQUE_FR → blocker IA.
- [ ] IA modeGardeDetaille=ALTERNEE_FR, user ALTERNEE_FR → pas d'alerte.

Mapping coarse :
- [ ] IA modeGarde=ALTERNEE, user ALTERNEE_FR → pas d'alerte (catégorie OK).
- [ ] IA modeGarde=ALTERNEE, user DVH_CLASSIQUE_FR → warning (catégorie mismatch).
- [ ] IA modeGarde=EXCLUSIVE, user DVH_ELARGI_FR → pas d'alerte.
- [ ] IA modeGarde=EXCLUSIVE, user ALTERNEE_FR → warning.

Priorités :
- [ ] F-96 override IA detection → F-96 gagne.
- [ ] 2 sources convergent → MULTI.

Transverses :
- [ ] Aucune source → aucune alerte.
- [ ] Résultat sauvegardé → alertes gelées.
- [ ] Code inconnu ignoré.
- [ ] Compteur `alertsSummary` cohérent.

### Tests backend

Aucun nouveau parser (SF-IA-03-05 a déjà ajouté `expected_value`). Seuls les prompts changent — test d'observation lors du smoke.

### Isolation workspace

- [x] Non applicable — aucun accès données.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants / endpoints existants

| Composant | Impact | Test |
|---|---|---|
| `CalendrierGardeSectionComponent` | 3 Inputs ajoutés, computed, badges | tests existants + SF-FA-06-04 conservés |
| Prompts IA | rallongement modéré | vérifier longueur |

### Smoke tests E2E

- [ ] Aucun concerné.

---

## Dépendances

### Subfeatures bloquantes

- `SF-FA-06-04` (Done) — fournit `modeGardeDetaille` IA + pré-remplissage.
- `SF-IA-03-05` (Done) — infrastructure `expected_value` sur procedure_checks et ai_questions.
- `F-FA-06` (Done).

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi blocker sur IA détaillée (étape D)** : cohérence avec SF-IA-03-05 TYPE_RUPTURE. Le mode de garde est un choix juridique structurant (barème pension + organisation famille), pas une simple valeur factuelle.
- **Pourquoi warning sur IA coarse (étape E)** : le signal ne permet de trancher qu'au niveau catégorie (alternée vs exclusive), pas au niveau précis. Niveau warning plus approprié.
- **Pourquoi l'alerte D est presque toujours absorbée par SF-FA-06-04** : le pré-remplissage intelligent neutralise la divergence au chargement. L'alerte D couvre le cas où l'avocat modifie manuellement le champ ou où le mode IA vient d'un autre pays.
- **Pas de nouveau prefix de code** : on réutilise `FA06_MODE_GARDE` simple, clair, cohérent avec le nommage existant des subfeatures F-IA-03.

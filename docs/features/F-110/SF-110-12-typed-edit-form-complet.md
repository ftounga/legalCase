# Mini-spec — F-110 / SF-110-12 : Formulaires typés complets — 6 types restants ou partiels

## Identifiant
`F-110 / SF-110-12`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`draft`

## Date de création
`2026-04-18`

## Branche Git
`feat/SF-110-12-typed-edit-form-4-types`

---

## Objectif

Éliminer tout **JSON brut** dans la dialog d'édition des référentiels (`referential-edit-dialog.component.ts`), pour les 6 types encore concernés :

**4 types sur le fallback `default`** (JSON textarea complet) :
- `INDEMNITE_BAREMES`
- `GARDE_MODES`
- `DIVORCE_ETAPES`
- `DIVORCE_PIECES`

**2 types avec JSON résiduel** (typage partiel introduit par SF-110-06 / SF-REF-01-03) :
- `CONVENTION_BAREMES` — seul `congesLegauxJours` était typé ; `congesSupp[]` et `primes[]` restaient en JSON (`convJson`)
- `IMMIGRATION_JALONS` — entièrement en JSON (explicitement noté "complexité trop élevée" dans SF-110-06, à traiter maintenant)

SF-110-11 (mergée le 2026-04-17, PR #359) a traité le **niveau 1 — affichage** (formatValue). Cette SF traite le **niveau 2 — édition complète** : formulaires 100 % typés pour les 6 types, suivant le pattern SF-110-06 (BAREME_MACRON, PENSION_TAUX, etc.) et SF-REF-01-03 (IMMIGRATION_TITLES, IMMIGRATION_RECOURS, etc.).

Après cette SF, aucun type connu ne tombe plus sur le `default` JSON. Le fallback `default` reste uniquement en **filet de sécurité** pour tout type inconnu futur ou tout schéma incohérent dans un type connu.

La validation IA Haiku côté backend (SF-110-03 / SF-110-10 — warning dialog si divergence vs sources officielles) doit rester opérationnelle : les formulaires soumettent toujours un `valueJson` sérialisé, le pipeline backend reste inchangé.

---

## Comportement attendu

### Cas nominal

Pour chacun des 4 types, ouvrir la dialog d'édition affiche un formulaire structuré au lieu d'un textarea JSON.

#### `INDEMNITE_BAREMES` — 2 sous-formes selon la structure détectée

**Forme A — MACRON (barème détaillé par année)** : détectée si `entries: [...]` est présent.

| Champ | Type | Validation |
|---|---|---|
| Entrées (tableau dynamique) | 30 lignes `{ an, min, max }` éditables | `an ∈ [0, 49]`, `min/max ∈ [0, 50]`, `min ≤ max` |
| Bouton `+` / `−` | Ajouter / supprimer une ligne | au moins 1 ligne requise |

**Forme B — CCT 109 (fourchette simple)** : détectée si `minSemaines` et `maxSemaines` sont présents.

| Champ | Type | Validation |
|---|---|---|
| Min (semaines) | `number` | `required, min=0, max=104` |
| Max (semaines) | `number` | `required, min=0, max=104, ≥ min` |

> Les deux formes sont exclusives — détecter à l'ouverture via présence de `entries`.

#### `GARDE_MODES`

| Champ | Type | Validation | Source JSON |
|---|---|---|---|
| Type de répartition | `select` (3 options fixes : `ALTERNEE_1_SUR_2`, `DVH_CLASSIQUE`, `DVH_ELARGI`) | `required` | `repartitionType` |
| Périodes Parent A | `textarea` (une ligne par période, split `\n`) | `required, ≥ 1 ligne` | `periodesA: string[]` |
| Périodes Parent B | `textarea` (idem) | `required, ≥ 1 ligne` | `periodesB: string[]` |
| Vacances | `input` texte | `required` | `vacances: string` |
| Jours Parent A | `number` | `required, min=0, max=365` | `joursA: number` |
| Jours Parent B | `number` | `required, min=0, max=365, (joursA+joursB) ≤ 366` | `joursB: number` |

#### `DIVORCE_ETAPES`

| Champ | Type | Validation | Source JSON |
|---|---|---|---|
| Ordre | `number` | `required, min=1, max=20` | `ordre: number` |
| Description | `input` texte | `required, maxLength=500` | `description: string` |
| Délai | `input` texte libre (ex. `"—"`, `"15 jours"`, `"2-4 semaines"`) | `required, maxLength=50` | `delai: string` |
| Obligatoire | `mat-slide-toggle` | — | `obligatoire: boolean` |

#### `DIVORCE_PIECES`

| Champ | Type | Validation | Source JSON |
|---|---|---|---|
| Description | `input` texte | `required, maxLength=500` | `description: string` |
| Obligatoire | `mat-slide-toggle` | — | `obligatoire: boolean` |

#### `CONVENTION_BAREMES` — complétion (remplace `convJson` résiduel)

Schéma complet : `{ congesLegauxJours, congesSupp: [{ min, jours }], primes: [{ min, pct }] }`

| Champ | Type | Validation | Source JSON |
|---|---|---|---|
| Congés légaux (jours) | `number` | `required, min=0, max=60` | `congesLegauxJours` (déjà typé, inchangé) |
| Paliers congés supplémentaires | Tableau dynamique `{ min, jours }` + boutons +/− | `min ∈ [1, 50]`, `jours ∈ [0, 30]`, tri par `min` croissant | `congesSupp[]` |
| Paliers primes d'ancienneté (%) | Tableau dynamique `{ min, pct }` + boutons +/− | `min ∈ [1, 50]`, `pct ∈ [0, 100]`, tri par `min` croissant | `primes[]` |

> Remplace le champ `convJson` précédent (textarea JSON résiduel). Tous les paliers sont éditables individuellement.

#### `IMMIGRATION_JALONS`

Schéma complet : tableau racine de jalons `[{ label, offsetDays }, ...]`

| Champ | Type | Validation | Source JSON |
|---|---|---|---|
| Jalons | Tableau dynamique `{ label, offsetDays }` + boutons +/− | Au moins 1 jalon requis. `label: required, maxLength=200`. `offsetDays: required, min=0, max=1825` (5 ans max) | Tableau racine |

> Pattern tableau dynamique identique à `INDEMNITE_BAREMES` MACRON et aux 2 tableaux de `CONVENTION_BAREMES`.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `INDEMNITE_BAREMES` — `entries` absent ET `minSemaines`/`maxSemaines` absents | Fallback sur `default` JSON editor (sécurité) |
| JSON incohérent à l'ouverture (ex. `joursA` non numérique) | Fallback sur `default` JSON editor |
| Utilisateur soumet avec champs vides requis | Validation Angular Reactive Forms, bouton Enregistrer désactivé |
| Soumission → validation IA Haiku échoue (SF-110-10) | Dialog de warning IA s'affiche comme pour les autres types — comportement inchangé |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable. Le fix ne touche pas les outils décisionnels (F-DT-*, F-FA-*, F-IM-*), seulement l'écran admin d'édition des référentiels.
- [x] **Autres pays** — Les 4 types couvrent déjà FR + BE via le champ `country` de `LegalReferential` (pas de logique pays dans le formulaire lui-même).
- [x] **Autres domaines** — `INDEMNITE_BAREMES` couvre DROIT_DU_TRAVAIL ; `GARDE_MODES`, `DIVORCE_ETAPES`, `DIVORCE_PIECES` couvrent DROIT_FAMILLE. Tous déjà existants.
- [x] **Autres UI patterns** — Pattern `@switch(sectionType)` dans la dialog d'édition est déjà en place. On ajoute simplement 4 `case` avec les formulaires adaptés.
- [x] **Autres flows transversaux** — Validation IA Haiku (SF-110-03/10) opérationnelle, pas impactée. Le chemin backend `PUT /api/v1/referentials/{id}` reste inchangé.

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — Pas de changement. `ReferentialEditDialogResult` reste `{ label, valueJson, force }`.
- [x] **Record / DTO backend** — Aucun changement backend.
- [x] **Service / logique métier** — Aucun changement.
- [x] **Entité JPA + schéma DB** — Aucun changement.
- [x] **Tests existants** — `referential-edit-dialog.component.spec.ts` contient 11 tests (EDT-01 à EDT-08). À étendre avec 4 suites (une par nouveau type typé).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] Aucun nouveau pattern partagé introduit — extension du pattern existant `@switch(sectionType)` avec `buildForm()` et `serializeValueJson()`.
- [x] Pas de pattern concurrent — le design system Material (mat-form-field, mat-slide-toggle, mat-select) est déjà utilisé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| 4 types SF-REF-01-03 restants | Oui | Intégré dans cette SF |
| Validation IA Haiku post-soumission | Oui (préservation) | Réutilisée sans changement |
| Affichage (formatValue) des 4 types | Non applicable | Déjà fait par SF-110-11 |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (les 4 types manquants)
- [x] Non applicable aux autres cibles (justification ci-dessus)

---

## Critères d'acceptation

- [ ] Ouvrir la dialog d'édition sur une entrée `INDEMNITE_BAREMES` avec schéma MACRON affiche un tableau éditable de 30 lignes (an/min/max), pas de JSON brut
- [ ] Ouvrir la dialog d'édition sur une entrée `INDEMNITE_BAREMES` avec schéma CCT 109 affiche 2 champs `minSemaines` / `maxSemaines`, pas de JSON brut
- [ ] Ouvrir la dialog d'édition sur une entrée `GARDE_MODES` affiche 6 champs typés (select + 2 textareas + input + 2 numbers), pas de JSON brut
- [ ] Ouvrir la dialog d'édition sur une entrée `DIVORCE_ETAPES` affiche 4 champs typés (number + 2 inputs + toggle), pas de JSON brut
- [ ] Ouvrir la dialog d'édition sur une entrée `DIVORCE_PIECES` affiche 2 champs typés (input + toggle), pas de JSON brut
- [ ] Ouvrir la dialog d'édition sur une entrée `CONVENTION_BAREMES` affiche : 1 number `congesLegauxJours` + 1 tableau `congesSupp[]` éditable + 1 tableau `primes[]` éditable. Plus aucun champ `convJson` textarea.
- [ ] Ouvrir la dialog d'édition sur une entrée `IMMIGRATION_JALONS` affiche un tableau éditable de jalons `{ label, offsetDays }`, pas de JSON brut
- [ ] Soumettre une modification sur chacun des 6 types produit un `valueJson` identique en structure au JSON d'origine (clés identiques, ordre équivalent, types préservés)
- [ ] La validation IA Haiku (SF-110-10) est déclenchée sur les nouvelles soumissions comme pour les autres types (dialog warning en cas de divergence)
- [ ] Le `default` du `switch` reste en fallback pour tout type inconnu et pour les schémas incohérents d'un des 6 types
- [ ] Tests unitaires (≥ 6 suites EDT-09 à EDT-14) : construction du form, sérialisation aller-retour, fallback schéma incohérent
- [ ] Aucune régression sur les 4 types déjà typés intégralement (LITIGATION_TYPE, BAREME_MACRON, PENSION_TAUX, PRESTATION_COEFF, IMMIGRATION_PIECES) ni sur les 4 types de SF-REF-01-03 typés intégralement (IMMIGRATION_TITLES, IMMIGRATION_RECOURS, IMMIGRATION_WORK_RIGHTS, LICENCIEMENT_CRITERES) — tests existants (EDT-01 à EDT-08) restent verts
- [ ] **Aucun type connu ne tombe plus sur le `default` JSON** — le fallback ne sert plus qu'aux types inconnus futurs et aux schémas incohérents

---

## Périmètre

### Hors scope (explicite)

- Aucun changement backend (endpoint, DTO, entité, migration)
- Pas de modification de `formatValue` ni de `sectionIcon` (déjà fait par SF-110-11)
- Pas d'ajout de nouveau type de référentiel
- Pas de modification de la logique de validation IA Haiku (SF-110-03 / SF-110-10)
- Pas de fallback pour `INDEMNITE_BAREMES` dont la 3ème forme (autre que MACRON ou CCT109) serait introduite plus tard — on passe en `default` JSON
- Pas d'amélioration UX avancée (drag-to-reorder des lignes Macron, preview live, etc.) — scope strictement au remplacement du JSON brut par des champs
- Pas de mise à jour de `formatValue` pour `CONVENTION_BAREMES` ou `IMMIGRATION_JALONS` — les rendus actuels restent inchangés (cf. SF-110-11)

---

## Valeurs initiales

Pas d'entité créée par cette SF. Les formulaires pré-remplissent depuis `data.entry.valueJson` au moment de l'ouverture.

---

## Contraintes de validation

Couvertes dans les tableaux du § **Comportement attendu** ci-dessus.

Règles de sérialisation :

- Toujours renvoyer un JSON valide via `JSON.stringify(...)` dans `serializeValueJson()`
- Pour `INDEMNITE_BAREMES` MACRON : `entries` trié par `an` croissant
- Pour `GARDE_MODES` : `periodesA`/`periodesB` issus du `split('\n')` puis trim + filtre des lignes vides (pattern identique à `IMMIGRATION_PIECES`)
- Pour `DIVORCE_ETAPES` / `DIVORCE_PIECES` : pas de transformation, sérialisation directe des champs

---

## Technique

### Endpoint(s)

Aucun — extension frontend pure.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Non applicable

### Composants Angular modifiés

- `frontend/src/app/referentials/referential-edit-dialog/referential-edit-dialog.component.ts` : ajout de 4 `case` dans `buildForm()` et dans `serializeValueJson()`, détection du schéma MACRON/CCT109 pour `INDEMNITE_BAREMES`
- `frontend/src/app/referentials/referential-edit-dialog/referential-edit-dialog.component.html` : ajout de 4 blocs de template avec les champs typés
- `frontend/src/app/referentials/referential-edit-dialog/referential-edit-dialog.component.scss` : styles complémentaires si besoin (tableau Macron)
- `frontend/src/app/referentials/referential-edit-dialog/referential-edit-dialog.component.spec.ts` : 4 suites de tests (EDT-09 à EDT-12)

---

## Plan de test

### Tests unitaires

- [ ] `EDT-09 INDEMNITE_BAREMES MACRON` : construction du form avec 30 entries → 30 lignes. Modification de `entries[0].max = 2` → `serializeValueJson` renvoie `{"entries":[{"an":0,"min":0,"max":2},...]}` identique au reste.
- [ ] `EDT-09b INDEMNITE_BAREMES CCT109` : construction du form avec `minSemaines=3, maxSemaines=17` → 2 champs. Modification max → 20 → `serializeValueJson` renvoie `{"minSemaines":3,"maxSemaines":20}`.
- [ ] `EDT-09c INDEMNITE_BAREMES schéma inconnu` : fallback `default` JSON editor.
- [ ] `EDT-10 GARDE_MODES` : construction avec les 6 champs pré-remplis → modif `joursA` → serialize renvoie JSON avec tous les champs (vérifier `periodesA: string[]`, pas string).
- [ ] `EDT-11 DIVORCE_ETAPES` : construction, modif `ordre` + toggle `obligatoire` → serialize correct, validation `ordre ≥ 1`.
- [ ] `EDT-12 DIVORCE_PIECES` : construction, toggle `obligatoire` → serialize correct.
- [ ] `EDT-13 CONVENTION_BAREMES complet` : construction avec BTP (congésSupp=3 paliers, primes=4 paliers) → 3+4 lignes éditables. Modif + ajout d'un palier prime → serialize préserve `congesLegauxJours`, `congesSupp` trié par min, `primes` trié par min.
- [ ] `EDT-14 IMMIGRATION_JALONS` : construction avec RENOUVELLEMENT_TITRE_SEJOUR (2 jalons) → 2 lignes éditables. Ajout d'un jalon + modif `offsetDays` → serialize renvoie un tableau racine `[{ label, offsetDays }, ...]` correct.
- [ ] Non-régression EDT-01 à EDT-08 : tests existants verts (exécution de la suite complète). Particulièrement vérifier que le test EDT existant sur `CONVENTION_BAREMES` (qui testait `convJson`) est **mis à jour** pour refléter le nouveau schéma typé.

### Tests d'intégration

- [ ] Aucun — pas de changement backend. Les IT existants sur `PUT /api/v1/referentials/{id}` couvrent déjà la chaîne de sérialisation.

### Isolation workspace

- [x] **Non applicable** — les référentiels `is_system=true` sont globaux. Les overrides par workspace existent déjà via `workspace_id` sur `LegalReferential` et sont vérifiés dans le controller (inchangés par cette SF).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché (isolation gérée côté backend, pas impactée)
- [ ] Plans / limites — non touché
- [ ] Navigation / routing frontend — non touché
- [x] **Aucune préoccupation transversale** — subfeature isolée à la dialog d'édition des référentiels

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `referentials.component.ts` (liste, `formatValue`) | Aucun — SF-110-12 ne touche que la dialog | Tests existants |
| Backend `LegalReferentialController` PUT | Aucun — même payload `valueJson` | IT existants |
| Validation IA Haiku (SF-110-10 warning dialog) | Aucun — même payload soumis | Testé manuellement sur chacun des 4 types |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — subfeature admin, hors du parcours utilisateur principal.

---

## Dépendances

### Subfeatures bloquantes

- `SF-110-11` — statut : **done** (fix formatValue) — prérequis pour que l'écran de liste soit lisible avant ouverture de l'édition.

### Questions ouvertes impactées

- [x] Aucune question bloquante de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

### Détection MACRON vs CCT109 pour `INDEMNITE_BAREMES`

Deux schémas coexistent pour le même type :

- MACRON : `{ entries: [{ an, min, max }] }` — FRANCE
- CCT 109 : `{ minSemaines, maxSemaines }` — BELGIQUE

La dialog détecte à l'ouverture via `Array.isArray(parsed?.entries)` → forme A, sinon `parsed?.minSemaines != null` → forme B, sinon fallback `default`. Le `country` de l'entrée n'est pas utilisé (pas passé à la dialog) — la détection par schéma est plus robuste.

### Sérialisation Parent A / Parent B pour `GARDE_MODES`

Pattern hérité de `IMMIGRATION_PIECES` (SF-110-06) : textarea une ligne par entrée, split + trim + filter. Uniforme et testé.

### Champ `delai` de `DIVORCE_ETAPES`

Gardé en input texte libre (pas `number`) car les valeurs observées dans le seed sont hétérogènes : `"—"`, `"15 jours"`, `"2-4 semaines"`. Un select serait trop restrictif, un number perdrait l'expressivité métier.

### Validation IA (rappel de contexte)

Depuis SF-110-03 + SF-110-10, toute modification d'un référentiel `is_system=true` déclenche côté backend un appel Claude Haiku qui compare la nouvelle valeur aux sources officielles et lève un warning dialog si divergence. Cette SF ne modifie en rien ce mécanisme — les nouveaux formulaires typés soumettent le même `valueJson` que le textarea, le warning s'affiche donc à l'identique.

# Mini-spec — F-110 / SF-110-11 : Fix formatValue pour les 9 types SF-REF-01-03

## Identifiant
`F-110 / SF-110-11`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`draft`

## Date de création
`2026-04-17`

## Branche Git
`feat/SF-110-11-fix-formatvalue-9-types`

---

## Objectif

Dans l'écran "Guides & barèmes", 9 types de référentiels ajoutés par SF-REF-01-03 affichent du JSON brut (ex. `{"entries":[{"an":0,...}]}`) au lieu d'un rendu lisible. Étendre `formatValue()` et `sectionIcon()` pour couvrir ces 9 types, en réappliquant le pattern du commit `f61ddc7` (fix BAREME_MACRON/PENSION_TAUX d'avril).

---

## Comportement attendu

### Cas nominal

Pour chaque type ci-dessous, `formatValue()` renvoie une phrase lisible par un humain à la place d'un `JSON.stringify()`.

| Type | Schéma JSON | Rendu attendu |
|---|---|---|
| `IMMIGRATION_TITLES` | `{ motif, conditions, pieces: string[], delaiMoyenJours }` | `Motif : [motif] · Conditions : [conditions] · Délai moyen : [n] jours · [n] pièces` |
| `IMMIGRATION_RECOURS` | `{ delaiJours, juridiction, textesApplicables: string[], piecesStandard: string[] }` | `Juridiction : [juridiction] · Délai : [n] jours · [n] texte(s) · [n] pièce(s)` |
| `IMMIGRATION_WORK_RIGHTS` | `{ droitTravail, conditions, obligationsEmployeur: string[] }` | `Droit au travail : [OUI/NON/CONDITIONNEL] · Conditions : [conditions] · [n] obligation(s) employeur` |
| `CONVENTION_BAREMES` | `{ congesLegauxJours, ... }` | `Congés légaux : [n] jours` (le reste du JSON non affiché par défaut, structure variable) |
| `LICENCIEMENT_CRITERES` | `{ poids, bloquant, description }` | `Poids : [n] · [Bloquant / Non bloquant] — [description]` |
| `INDEMNITE_BAREMES` — MACRON | `{ entries: [{an, min, max}] }` | `Barème sur [n] années — [min min] à [max max] mois de salaire` |
| `INDEMNITE_BAREMES` — CCT109 | `{ minSemaines, maxSemaines }` | `De [min] à [max] semaines de salaire` |
| `GARDE_MODES` | `{ repartitionType, periodesA[], periodesB[], vacances, joursA, joursB }` | `[repartitionType] — Parent A : [joursA] j / Parent B : [joursB] j — Vacances : [vacances]` |
| `DIVORCE_ETAPES` | `{ ordre, description, delai, obligatoire }` | `Étape [ordre] · [description] · Délai : [delai]` (préfixe ⚠ si `obligatoire=true`) |
| `DIVORCE_PIECES` | `{ description, obligatoire }` | `[description]` (préfixe ⚠ si `obligatoire=true`) |

### Icônes (sectionIcon)

| Type | Material icon |
|---|---|
| `IMMIGRATION_TITLES` | `badge` |
| `IMMIGRATION_RECOURS` | `gavel` |
| `IMMIGRATION_WORK_RIGHTS` | `work` |
| `CONVENTION_BAREMES` | `groups` |
| `LICENCIEMENT_CRITERES` | `rule` |
| `INDEMNITE_BAREMES` | `payments` |
| `GARDE_MODES` | `child_care` |
| `DIVORCE_ETAPES` | `list_alt` |
| `DIVORCE_PIECES` | `description` |

### Cas d'erreur

| Situation | Comportement |
|---|---|
| JSON malformé | Fallback actuel `return entry.valueJson;` conservé (try/catch existant) |
| Champ obligatoire manquant (ex. `motif` absent de `IMMIGRATION_TITLES`) | Afficher `—` à la place pour le champ absent, ne pas casser le rendu |
| Type inconnu futur | `default: JSON.stringify(val, null, 2)` conservé (fail-open) |

---

## Critères d'acceptation

- [ ] 9 nouveaux `case` ajoutés dans `formatValue()` (`referentials.component.ts:268-297`), couvrant tous les types listés dans `SECTION_LABELS` (ligne 43).
- [ ] 9 icônes ajoutées dans `sectionIcon()` (ligne 299-308).
- [ ] Aucun des 9 types n'affiche plus de JSON brut (pas de `{` visible dans la sortie).
- [ ] Rendu robuste aux champs manquants (ne plante pas, affiche `—`).
- [ ] Tests unitaires frontend : 1 test par type (10 au total avec le cas JSON malformé).
- [ ] Validation IA non touchée (déjà type-agnostic via `ReferentialValidationService`).
- [ ] Aucun changement backend.

---

## Périmètre

### Hors scope (explicite)

- Pas de redesign du rendu (pas de tableau multi-colonnes, pas d'accordéon). Une phrase par entrée, comme pour les 6 types déjà gérés.
- Pas de traduction i18n. Français uniquement, comme le reste de l'écran.
- Pas de changement des formulaires d'édition (déjà corrects depuis SF-110-06).
- Pas de changement de la validation IA (déjà opérationnelle pour tous les types par design).

---

## Valeurs initiales

N/A — aucune entrée nouvelle, uniquement du formatage de lecture.

---

## Contraintes de validation

N/A — pas d'input utilisateur dans cette SF.

---

## Technique

### Endpoint(s)

Aucun changement. L'endpoint `GET /api/v1/workspace/referentials` est déjà utilisé et renvoie `valueJson` brut comme aujourd'hui.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Non.

### Composants Angular

- `referentials.component.ts` : `formatValue()` étendu + `sectionIcon()` étendu. Le reste inchangé.
- `referentials.component.spec.ts` : 10 nouveaux tests.

---

## Plan de test

### Tests unitaires backend

Aucun (pas de changement backend).

### Tests unitaires frontend

- [ ] `formatValue(IMMIGRATION_TITLES)` avec entry complète → contient "Motif", "Conditions", "pièces", pas de `{`.
- [ ] `formatValue(IMMIGRATION_RECOURS)` avec entry complète → contient "Juridiction", "Délai", pas de `{`.
- [ ] `formatValue(IMMIGRATION_WORK_RIGHTS)` → contient "Droit au travail : OUI", pas de `{`.
- [ ] `formatValue(CONVENTION_BAREMES)` → contient "Congés légaux", pas de `{`.
- [ ] `formatValue(LICENCIEMENT_CRITERES)` bloquant=true → contient "Bloquant".
- [ ] `formatValue(INDEMNITE_BAREMES)` variante MACRON → contient "Barème sur N années".
- [ ] `formatValue(INDEMNITE_BAREMES)` variante CCT109 → contient "semaines".
- [ ] `formatValue(GARDE_MODES)` → contient "Parent A", joursA/joursB.
- [ ] `formatValue(DIVORCE_ETAPES)` obligatoire=true → contient "⚠" et description.
- [ ] `formatValue(DIVORCE_PIECES)` obligatoire=false → pas de "⚠".

### Tests d'intégration

Aucun.

### Isolation workspace

- [x] Non applicable — affichage pur côté client, endpoint déjà filtré par workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — fix isolé d'une fonction pure.

### Analyse de cohérence transversale

Scan des autres contextes où le même problème pourrait exister :

| Cible | Statut |
|---|---|
| `LegalReferentialService` backend — a-t-il un `formatValue` dual ? | **Non applicable** — le backend renvoie `valueJson` brut, le formatage est purement frontend. |
| Autre écran affichant des valeurs de référentiel ? | **Non applicable** — les outils métier (F-DT-09, F-FA-02, etc.) consomment `valueJson` parsé et l'utilisent pour calculs, pas pour affichage direct. Seul l'écran `referentials.component` affiche la valeur au sens littéral. |
| Tooltip / export / PDF des référentiels ? | **Non applicable** — pas d'export ni tooltip sur cet écran. |
| `referential-edit-dialog` lit-il aussi `valueJson` ? | **Déjà traité** dans SF-110-06 (formulaires typés par `sectionType`). Rien à faire. |

Aucune dette de convergence identifiée — le formatage d'affichage est localisé à `referentials.component.ts:formatValue()`.

### Nouveau pattern UI ou service partagé

Pas de nouveau pattern. On étend un switch existant.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `referentials.component` — 6 cas existants | Aucun changement | Tests existants doivent rester verts |
| `referential-edit-dialog` | Aucun | Aucun |
| `ReferentialValidationService` backend | Aucun | Aucun |

### Smoke tests E2E concernés

- [ ] Aucun concerné (pas d'impact auth/workspace/navigation).

---

## Dépendances

### Subfeatures bloquantes

- SF-110-02 (Done) — écran "Guides & barèmes" frontend.
- SF-REF-01-03 (Done) — 9 nouveaux types + formulaires d'édition.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi un fix simple et pas un redesign du rendu** : cohérence avec les 6 types déjà traités (une ligne par entrée). Un redesign demanderait une SF dédiée et sort du scope du bug.
- **Pourquoi conserver le `default: JSON.stringify`** : si un futur type est ajouté sans être branché ici, on garde un affichage non idéal mais non bloquant, visible immédiatement en review plutôt que masqué par une chaîne vide.
- **Pourquoi pas toucher la validation IA** : elle est type-agnostic (`ReferentialValidationService.validate()` passe le JSON brut à Haiku). L'ajout des 9 types n'a aucun impact dessus — testé de l'épreuve du temps depuis SF-110-03.

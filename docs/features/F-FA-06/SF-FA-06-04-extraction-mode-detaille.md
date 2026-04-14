# Mini-spec — F-FA-06 / SF-FA-06-04 Extraction IA du mode de garde détaillé

## Identifiant

`F-FA-06 / SF-FA-06-04`

## Feature parente

`F-FA-06` — Calendrier de garde et droit de visite

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-FA-06-04-extraction-mode-detaille`

---

## Objectif

Étendre l'extraction IA du mode de garde : ajouter un champ `mode_garde_detaille` parmi les 6 valeurs précises de F-FA-06, en complément du `mode_garde` binaire existant (conservé pour la pension alimentaire). Pré-remplir `CalendrierGardeSectionComponent` avec cette valeur détaillée. Préalable technique à SF-IA-03-07.

---

## Comportement attendu

### Cas nominal

1. Le prompt IA `pension_alimentaire_data` demande désormais à Claude deux champs :
   - `mode_garde` : `EXCLUSIVE` / `ALTERNEE` / null (inchangé, utilisé par `PensionAlimentaireCalculator`)
   - `mode_garde_detaille` : `ALTERNEE_FR` / `DVH_CLASSIQUE_FR` / `DVH_ELARGI_FR` / `ALTERNEE_BE` / `SECONDAIRE_BE` / `SECONDAIRE_ELARGI_BE` / null (nouveau)
2. `CaseAnalysisResponse.PensionAlimentaireEstimate` ou objet voisin expose le nouveau champ `modeGardeDetaille`.
3. `CalendrierGardeSectionComponent` reçoit `@Input() aiModeGardeDetaille?: string | null` et pré-remplit `gardeCode` si la valeur est cohérente avec le pays du workspace.
4. Si le mode IA détecté appartient au pays opposé (ex: `ALTERNEE_BE` détecté sur un dossier FR), une note visible invite l'avocat à vérifier. Valeur par défaut conservée.

### Règles de pré-remplissage intelligent

- IA détecte un mode du même pays que le workspace → sélectionné.
- IA détecte un mode du pays opposé → note visible "IA a détecté mode `X` (autre pays). Vérifier." Default du pays conservé.
- IA null → aucun pré-remplissage, comportement actuel préservé.
- Résultat F-FA-06 déjà sauvegardé → `gardeCode` chargé depuis la base, pas de pré-remplissage IA.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| IA renvoie une valeur hors enum | ignorée (fail-open) |
| IA renvoie une casse non normalisée (`alternee_fr`) | upper-case et validée |
| Clé `mode_garde_detaille` absente | champ null, rétrocompat totale |
| Format string/objet legacy | rétrocompat mode_garde binaire inchangée |

---

## Critères d'acceptation

- [ ] Prompt `FAMILLE_INSTRUCTION` dans `LegalDomainPromptBuilder` étend `pension_alimentaire_data` avec `mode_garde_detaille` (6 valeurs ou null).
- [ ] `CaseAnalysisResponse.PensionAlimentaireEstimate` exposé contient `modeGardeDetaille` optional.
- [ ] Parsing fail-open : valeur hors enum ou absente → null.
- [ ] `PensionAlimentaireCalculator` inchangé et tests existants verts.
- [ ] `CalendrierGardeSectionComponent` reçoit `@Input aiModeGardeDetaille`, pré-remplit `gardeCode` si valeur compatible avec le pays.
- [ ] Note visible si l'IA détecte un mode du pays opposé.
- [ ] `CaseFileDetailComponent` passe la valeur IA au composant.
- [ ] Tests backend (parsing, valeur invalide, null, rétrocompat).
- [ ] Tests frontend (prefill nominal, pays opposé avec note, fallback default, résultat sauvegardé non surchargé).
- [ ] Aucune régression F-FA-06 existant.

---

## Périmètre

### Hors scope (explicite)

- La cohérence IA elle-même (alerte sur divergence user vs IA) → SF-IA-03-07.
- Refactor du `PensionAlimentaireCalculator` pour utiliser le mode détaillé : le calcul pension reste basé sur le binaire ALTERNEE/EXCLUSIVE.
- Enrichissement F-FA-06 avec `dateDebut`, `nbEnfants`, âges : hors scope, features produit séparées.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `pensionAlimentaireEstimate.modeGardeDetaille` | null | peuplé par parsing IA si valide |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Normalisation |
|-------|-------------|----------------------------|---------------|
| `mode_garde_detaille` (IA) | Non | un de 6 : `ALTERNEE_FR`, `DVH_CLASSIQUE_FR`, `DVH_ELARGI_FR`, `ALTERNEE_BE`, `SECONDAIRE_BE`, `SECONDAIRE_ELARGI_BE` | upper-case, filtré contre l'enum |

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---|---|---|
| GET | `/api/v1/case-files/{id}/case-analysis` | `pensionAlimentaireEstimate.modeGardeDetaille` ajouté |

### Tables impactées

Aucune — tout est dans le JSON `analysis_result`.

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable**.

### Composants Angular

- `CaseAnalysisResult` (model) — nouveau champ `modeGardeDetaille` sur `PensionAlimentaireEstimate`.
- `CalendrierGardeSectionComponent` :
  - nouvel `@Input() aiModeGardeDetaille?: string | null`
  - signal `aiModeGardeDetailleSignal`
  - logique de pré-remplissage dans `ngOnInit` / `ngOnChanges`
  - signal `modeDetailleNote?: string | null` pour la note mode pays opposé
- `CaseFileDetailComponent` : passe `[aiModeGardeDetaille]="synthesis()?.pensionAlimentaireEstimate?.modeGardeDetaille"`

---

## Plan de test

### Tests unitaires backend

- [ ] `CaseAnalysisResponse.extractPensionAlimentaireEstimate()` parse le nouveau champ `mode_garde_detaille` upper-case.
- [ ] Valeur hors enum → null.
- [ ] Champ absent → null (rétrocompat).
- [ ] Casse mixte `alternee_fr` → `ALTERNEE_FR`.
- [ ] Calcul pension inchangé (binaire toujours utilisé).

### Tests unitaires frontend

- [ ] IA `ALTERNEE_FR`, workspace FR → `gardeCode` pré-rempli `ALTERNEE_FR`, pas de note.
- [ ] IA `ALTERNEE_BE`, workspace FR → default FR pré-rempli, note visible.
- [ ] IA null → comportement actuel, pas de note.
- [ ] Résultat F-FA-06 existant chargé → pré-remplissage IA ignoré.
- [ ] Note disparaît quand l'avocat change `gardeCode`.

### Isolation workspace

- [x] Non applicable — lecture seule via endpoints existants.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `PensionAlimentaireCalculator` | aucun — binaire inchangé | tests existants |
| `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION` | prompt rallongé | vérifier longueur |
| `CaseAnalysisResponse` | nouveau champ dans estimate | constructeur record rallongé |
| `CalendrierGardeSectionComponent` | flux pre-fill | tests existants |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-01` (Done) — pipeline d'extraction IA.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi un champ séparé `mode_garde_detaille` et pas étendre `mode_garde`** : le calcul de la pension alimentaire dépend du binaire ALTERNEE/EXCLUSIVE (cf. `PensionAlimentaireCalculator`). Étendre à 6 valeurs casserait le calcul sans refactor. Plus simple et sûr de garder les deux.
- **Pourquoi dans `pension_alimentaire_data` et pas ailleurs** : cohérence — c'est la section qui parle déjà du mode de garde. Évite de créer un nouveau bloc JSON.
- **Pourquoi pas de migration** : la donnée reste dans le raw JSON `analysis_result`, pattern identique à `typeRupture` dans `compensation_data`.

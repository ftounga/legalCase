# Mini-spec — F-207 / SF-207-06b-frontend Outil RCC BE conditions (UI)

## Identifiant

`F-207 / SF-207-06b-frontend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-06b-frontend-rcc-be-conditions`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern miroir : `refere-tribunal-travail-be-section` (#1149, le plus récent, verdict 3 états + score affiché).

## Objectif

Section frontend de l'outil RCC BE conditions (backend SF-207-06 #1151). Formulaire 5 champs + verdict 3 états + régime applicable + régimes éligibles cumulés + liste conditions manquantes. BE-only.

## Contrat API (figé #1151)

`POST` + `GET /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-conditions`

Inputs :
```ts
{
  dateNaissance: string;                          // ISO, requis
  anneesCarriereProfessionnelle: number;           // 0-60, requis
  metierLourd: boolean;
  longueCarriere: boolean;
  entrepriseEnDifficulte: boolean;
  dateLicenciementEnvisagee?: string | null;       // default today
}
```

Réponse 200 :
```ts
{
  verdict: 'ELIGIBLE' | 'INCERTAIN' | 'NON_ELIGIBLE';
  regimeApplicable: 'GENERAL' | 'METIERS_LOURDS' | 'LONGUE_CARRIERE' | 'ENTREPRISE_DIFFICULTE' | null;
  regimesEligibles: string[];
  ageALaDateLicenciement: number;
  anneesCarriereCalculees: number;
  conditionsManquantes: string[];
  baseJuridique: string;
  formuleCalcul: string;
}
```

## Comportement

Section `rcc-be-conditions-section.component` — pattern F-IA-04 + helpers `humanizeRegime`, `humanizeCondition`, `humanizeVerdict`.

### Formulaire

- `dateNaissance` (date, requis).
- `anneesCarriereProfessionnelle` (number, 0-60, requis) avec helper `mat-hint`.
- 3 checkboxes : `metierLourd`, `longueCarriere`, `entrepriseEnDifficulte`.
- `dateLicenciementEnvisagee` (date, optionnel — placeholder « (par défaut = aujourd'hui) »).
- Bouton « Évaluer l'éligibilité au RCC ».

### Pré-fill IA

| Champ | Source aiData |
|---|---|
| `dateNaissance` | `aiData.dateNaissanceSalarie` |
| `anneesCarriereProfessionnelle` | `aiData.anneesCarriereSalarie` |
| `metierLourd` | `aiData.metierLourdDetecte` (true uniquement) |
| `entrepriseEnDifficulte` | `aiData.entrepriseEnDifficulteDetectee` (true uniquement) |

`longueCarriere` n'est pas pré-rempli (jugement avocat sur la prise en compte effective de la carrière complète).

`getPrefillCount` 0-4.

### Verdict

Badge :
- Vert `ELIGIBLE`
- Ambre `INCERTAIN`
- Rouge `NON_ELIGIBLE`

Affichage :
- `regimeApplicable` (humanisé) + libellé large (« Régime applicable : RCC général (CCT 17) »).
- Si `regimesEligibles.length > 1` : liste à puces des régimes cumulés humanisés.
- Calculs annexes : `ageALaDateLicenciement` + `anneesCarriereCalculees` en évidence.
- Si `conditionsManquantes.length > 0` : liste à puces humanisée.
- `baseJuridique` + `formuleCalcul` en `JetBrains Mono`.

### TOOL_REGISTRY

`rcc-be-conditions` après `refere-tribunal-travail-be` dans `decisional-tools-panel.component.ts`. Theme `VALIDITE` (analyse d'éligibilité — cohérent avec autres analyseurs).

### Visibility seed

Migration `XXX-add-rcc-be-conditions-visibility.xml` (prochain après 262) : ALWAYS_ON BELGIQUE / DROIT_DU_TRAVAIL, priority 94. Justification : RCC est un sujet récurrent en consultation BE Travail (préparation départ retraite anticipé).

## Critères d'acceptation

- [ ] Section rend formulaire 5 champs + verdict + détail régime ; gate `BELGIQUE` strict.
- [ ] Pré-fill 4 champs (date naissance, années carrière, metier lourd, entreprise en difficulté).
- [ ] `getPrefillCount` 0/1/2/3/4.
- [ ] Verdict 3 états avec couleur correcte ; régime applicable humanisé.
- [ ] Régimes éligibles cumulés affichés si > 1.
- [ ] Conditions manquantes humanisées (libellés FR).
- [ ] `ageALaDateLicenciement` + `anneesCarriereCalculees` affichés.
- [ ] `MatSnackBar` erreur ; refresh dashboard.
- [ ] Migration visibility ALWAYS_ON ; `DecisionToolVisibilityIntegrityIT` vert.

## Composants

Standard sous `frontend/src/app/case-files/rcc-be-conditions-section/` + model + service + modifs panel + case-analysis.model.ts (4 fields BE) + migration backend visibility.

## Tests Jest

- prefill-rules : 5+ tests (0/1/2/3/4 + bornes années carrière).
- component : 10+ tests (rendu, pré-fill, verdict 3 états, régimes cumulés, conditions manquantes, snackbar erreur).

## Dépendances

- Backend SF-207-06 (#1151 mergé).
- Pattern frontend `refere-tribunal-travail-be-section` (#1149).

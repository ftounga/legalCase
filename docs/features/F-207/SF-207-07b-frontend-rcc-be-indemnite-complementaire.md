# Mini-spec — F-207 / SF-207-07b-frontend Outil RCC BE indemnité complémentaire (UI)

## Identifiant

`F-207 / SF-207-07b-frontend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-07b-frontend-rcc-be-indemnite-complementaire`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern miroir : `rcc-be-conditions-section` (#1152, sa SF jumelle de la même feature parent RCC).

## Objectif

Section frontend du calculateur d'indemnité complémentaire RCC (backend SF-207-07 #1155). Formulaire 5 champs montants + dates ; affichage mensualité + total + nb mois restants. Calculateur pur — pas de verdict (montant € = output principal). BE-only.

## Contrat API

`POST` + `GET /api/v1/case-files/{caseFileId}/decision-tools/rcc-be-indemnite-complementaire`

Inputs :
```ts
{
  remunerationNetteReference: number;             // > 0, requis
  allocationOnemMensuelle: number;                 // ≥ 0, requis
  dateNaissanceSalarie: string;                    // ISO, requis
  dateDebutRcc: string;                            // ISO, requis (≥ dateNaissance + 50 ans)
  ageLegalPension?: number | null;                 // [60..75], default 66
  planchersSectoriel?: number | null;
}
```

Réponse 200 :
```ts
{
  indemniteMensuelleEmployeur: number;             // €
  indemniteMensuelleAvantPlancher: number;
  planchersSectorielApplique: boolean;
  moisRestantsJusquaPension: number;
  montantTotalEmployeur: number;                   // mensualité × mois restants
  remunerationNetteReference: number;
  allocationOnemMensuelle: number;
  baseJuridique: string;
  formuleCalcul: string;
}
```

## Comportement

Section `rcc-be-indemnite-complementaire-section.component` — pattern F-IA-04.

### Formulaire

- `remunerationNetteReference` (number € avec icône, > 0, requis).
- `allocationOnemMensuelle` (number € ≥ 0, requis).
- `dateNaissanceSalarie` (date, requis).
- `dateDebutRcc` (date, requis).
- `ageLegalPension` (number, 60-75, optionnel — placeholder « (par défaut 66) »).
- `planchersSectoriel` (number € optionnel — hint « CCT sectorielle plus favorable »).
- Bouton « Calculer l'indemnité ».

### Pré-fill IA

| Champ | Source aiData |
|---|---|
| `remunerationNetteReference` | `aiData.remunerationNetteReferenceRccDetectee` |
| `allocationOnemMensuelle` | `aiData.allocationOnemMensuelleEstimee` |
| `dateNaissanceSalarie` | `aiData.dateNaissanceSalarie` (déjà extrait SF-207-06) |
| `dateDebutRcc` | `aiData.dateDebutRccEnvisagee` |

`ageLegalPension` et `planchersSectoriel` non pré-remplis.

`getPrefillCount` 0-4.

### Affichage des résultats

Pas de verdict coloré (calculateur pur). Cartes empilées :

1. **Carte « Indemnité mensuelle employeur »** (mise en évidence, fond vert pâle) : `indemniteMensuelleEmployeur` en gros (24-28 px), libellé « € par mois ». Si `planchersSectorielApplique=true` → badge ambre « Plancher sectoriel appliqué » avec montant brut CCT 17 en sous-titre.
2. **Carte « Durée »** : `moisRestantsJusquaPension` mois (X années Y mois). Si 0 → alerte info « RCC débute après l'âge légal de pension : aucune indemnité due ».
3. **Carte « Coût total employeur »** : `montantTotalEmployeur` formaté € (mensualité × mois restants).
4. Encart « Hypothèses » : récap des inputs (remun nette, alloc ONEM, plancher sectoriel s'il existe).
5. `baseJuridique` + `formuleCalcul` en `JetBrains Mono`.

Format euros : `Intl.NumberFormat('fr-BE', { style: 'currency', currency: 'EUR' })` côté UI.

### TOOL_REGISTRY

`rcc-be-indemnite-complementaire` inséré après `rcc-be-conditions` (séquence métier : conditions d'abord, indemnité ensuite). Theme `INDEMNITES`.

### Visibility seed

Migration `XXX-add-rcc-be-indemnite-visibility.xml` (prochain après 264) : ALWAYS_ON BELGIQUE / DROIT_DU_TRAVAIL priority 95.

## Critères

- [ ] Section rend formulaire 6 champs + 3 cartes résultat ; gate `BELGIQUE` strict.
- [ ] Pré-fill 4 champs ; modification → provenance `null`.
- [ ] `getPrefillCount` 0/1/2/3/4.
- [ ] Format euros `fr-BE` partout.
- [ ] Badge ambre si `planchersSectorielApplique`.
- [ ] Alerte info si `moisRestantsJusquaPension=0`.
- [ ] `MatSnackBar` erreur ; refresh dashboard.
- [ ] Migration visibility ; `DecisionToolVisibilityIntegrityIT` vert.
- [ ] Tests Jest : prefill (5+), component (10+).

## Composants

Standard sous `frontend/src/app/case-files/rcc-be-indemnite-complementaire-section/` + model + service + modifs panel + case-analysis.model.ts (3 fields) + migration backend visibility.

## Dépendances

- Backend SF-207-07 (#1155 mergé).
- Pattern frontend `rcc-be-conditions-section` (#1152).

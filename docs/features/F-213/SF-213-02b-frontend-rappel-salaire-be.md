# Mini-spec — F-213 / SF-213-02b-frontend Outil rappel de salaire BE (UI)

## Identifiant

`F-213 / SF-213-02b-frontend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-02b-frontend-rappel-salaire-be`

---

## Objectif

Section frontend du calculateur de rappel de salaire BE — pré-remplie par l'IA, visibility **ALWAYS_ON** (très fréquent, transversal), **BELGIQUE / DROIT_DU_TRAVAIL uniquement**.

---

## Contrat API (figé SF-213-02 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/rappel-salaire-be`
- `GET` même path
- Body : `{ montantBrut, dateDebutPeriode, dateFinPeriode, dateRupture?, dateActionEnvisagee?, typeArriereEnum }`
- Réponse 200 : `{ montantBrut, interetsCourus, tauxMoratoire, totalReclame, dateLimitePrescription, joursRestantsAvantPrescription, statutPrescription, baseJuridique, formuleCalcul }`

---

## Comportement attendu

### Section composant

`rappel-salaire-be-section.component` :
- Champs : `montantBrut` (€), `dateDebutPeriode`, `dateFinPeriode`, `dateRupture` (optionnel), `typeArriereEnum` (select : Pendant contrat / Post-rupture / Mixte).
- Pré-fill IA : `montantArrieresSalaireBrut`, `dateDebutArrieresSalaire`, `dateFinArrieresSalaire`, `dateRuptureContrat` — **BELGIQUE UNIQUEMENT**.
- Badges provenance par champ.
- Bouton « Calculer le rappel » → POST → affichage :
  - Montant brut + intérêts (taux 10 % affiché explicitement) + total réclamable.
  - Prescription : badge couleur `statutPrescription` + date limite + jours restants.
  - `formuleCalcul` en `JetBrains Mono`.
- `CaseDashboardRefreshService.triggerRefresh()` sur succès POST.
- `MatSnackBar` erreurs.

### Pré-fill rules

| Champ | Source | Règle |
|---|---|---|
| `montantBrut` | `aiData.montantArrieresSalaireBrut` | `positiveNumberOrNull()` |
| `dateDebutPeriode` | `aiData.dateDebutArrieresSalaire` | `isoDateOrNull()` |
| `dateFinPeriode` | `aiData.dateFinArrieresSalaire` | `isoDateOrNull()` |
| `dateRupture` | `aiData.dateRuptureContrat` | `isoDateOrNull()` |
| `typeArriereEnum` | dérivé `dateRuptureContrat` | si présente → `POST_RUPTURE`, sinon `PENDANT_CONTRAT` |

`getPrefillCount(input)` : 0-4 (typeArriereEnum non compté séparément).

### Entrée TOOL_REGISTRY

- `tool_id` : `rappel-salaire-be`
- Visibility : **ALWAYS_ON**
- Ordre : position 16 dans séquence BE

### Migration visibility

`XXX-add-rappel-salaire-be-visibility.xml` : `ALWAYS_ON`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] `<input type="date">` pour dates
- [x] `JetBrains Mono` pour `formuleCalcul` et `baseJuridique`
- [x] `MatSnackBar` erreurs
- [x] `CaseDashboardRefreshService.triggerRefresh()` dans `next:`
- [x] Pré-fill IA + signaux provenance
- [x] Validation F-IA-03
- [x] `getPrefillCount()` parité runtime

---

## Critères d'acceptation

- [ ] ALWAYS_ON — visible pour tout dossier BE Travail.
- [ ] Pré-fill IA actif pour montant, dates et type.
- [ ] Taux moratoire 10 % affiché explicitement avec référence légale.
- [ ] Prescription `PRESCRIT` / `IMMINENT` / `NON_PRESCRIT` avec badge couleur.
- [ ] `DecisionToolVisibilityIntegrityIT` reste vert.

---

## Plan de test (Jest)

- [ ] `rappel-salaire-be-section-prefill-rules.spec.ts` — mapping `typeArriereEnum` depuis présence `dateRupture`.
- [ ] `rappel-salaire-be-section.component.spec.ts` — pré-fill, calcul affiché, badge prescription.

---

## Technique

- `frontend/src/app/case-files/rappel-salaire-be-section/rappel-salaire-be-section.component.{ts,html,scss,spec.ts}`
- `frontend/src/app/case-files/rappel-salaire-be-section/rappel-salaire-be-section-prefill-rules.{ts,spec.ts}`
- `frontend/src/app/core/models/rappel-salaire-be.model.ts`
- Migration backend : `XXX-add-rappel-salaire-be-visibility.xml`

---

## Dépendances

- SF-213-02 backend mergée.

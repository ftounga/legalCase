# Mini-spec — F-213 / SF-213-03b-frontend Outil préavis statut unique BE (UI)

## Identifiant

`F-213 / SF-213-03b-frontend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend du calculateur préavis statut unique BE. Visibility **CONTEXTUAL** (`dateContrat >= 2014-01-01`). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.**

---

## Contrat API (SF-213-03 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-statut-unique-preavis`
- `GET` même path
- Réponse 200 : `{ dureePreavisEnSemaines, dateFinPreavis, indemnitéCompensatoire, formuleCalcul, baseJuridique, avertissement }`

---

## Comportement attendu

`licenciement-be-statut-unique-preavis-section.component` :
- Champs : `ancienneteAnnees`, `ancienneteMoisSupplementaires`, `salaireHebdomadaireBrut` (€), `dateNotificationLicenciement`, `partieStatutUniqueSeulement` (checkbox).
- Pré-fill IA : ancienneté, salaire hebdo, date notification.
- Résultat : durée préavis (semaines) + date fin + indemnité compensatoire + formule + avertissement Claeys si applicable.
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

Migration `XXX-add-licenciement-be-statut-unique-preavis-visibility.xml` :
- `CONTEXTUAL`, `trigger_field='date_contrat_post_2014'`, `trigger_value='true'`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] `<input type="date">` pour dateNotification
- [x] `JetBrains Mono` pour formuleCalcul
- [x] `MatSnackBar` erreurs
- [x] Pré-fill IA + provenance
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] CONTEXTUAL visible uniquement si `dateContrat >= 2014-01-01`.
- [ ] Durée préavis en semaines + date fin + ICP affichés.
- [ ] Avertissement Claeys affiché si contrat mixte.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Plan de test (Jest)

- [ ] Prefill rules : ancienneté, salaire, date.
- [ ] Composant : pré-fill effectif, résultat affiché, snackbar erreur.

---

## Technique

- `frontend/src/app/case-files/licenciement-be-statut-unique-preavis-section/` — 4 fichiers composant + 2 prefill-rules
- `frontend/src/app/core/models/licenciement-be-statut-unique-preavis.model.ts`
- Migration : `XXX-add-licenciement-be-statut-unique-preavis-visibility.xml`

---

## Dépendances

- SF-213-03 backend mergée.

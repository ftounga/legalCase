# Mini-spec — F-213 / SF-213-09b-frontend Outil acte équipollent à rupture BE (UI)

## Identifiant

`F-213 / SF-213-09b-frontend`

## Feature parente

`F-213` — P2 Travail BE

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend analyseur acte équipollent à rupture BE. Visibility **ALWAYS_ON** (modification unilatérale non détectable automatiquement). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.**

---

## Contrat API (SF-213-09 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-acte-equivalent`
- `GET` même path
- Réponse 200 : `{ verdict, fondamentJuridique, icpIndicatif, risqueAcceptationTacite, delaiRecommandeProtestationJours, baseJuridique, avertissement }`

---

## Comportement attendu

`licenciement-be-acte-equivalent-section.component` :
- Champs : `typeModification` (select), `ampleurModification` (select), `elementEssentielDuContrat` (checkbox), `dateModification`, `salarieAProteste` (checkbox), `remunerationHebdomadaireBrute` (€, optionnel), `dureePreavisCalculeeSemaines` (int, optionnel).
- Pré-fill IA : typeModification, dateModification, ampleur.
- Résultat : verdict badge couleur + ICP indicatif si disponible + avertissement acceptation tacite si délai > 30 j + `delaiRecommandeProtestationJours=30` affiché.
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

`XXX-add-licenciement-be-acte-equivalent-visibility.xml` :
- `ALWAYS_ON`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] Badge orange `A_ANALYSER`, rouge `ACTE_EQUIPOLLENT_PROBABLE`, vert `PAS_ACTE_EQUIPOLLENT`
- [x] `MatSnackBar` erreurs
- [x] Pré-fill IA + provenance
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] ALWAYS_ON BE Travail.
- [ ] Verdict ACTE_EQUIPOLLENT_PROBABLE → badge rouge + ICP indicatif.
- [ ] Avertissement 30 j protestation affiché.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Technique

- `frontend/src/app/case-files/licenciement-be-acte-equivalent-section/` — composant + prefill-rules
- `frontend/src/app/core/models/licenciement-be-acte-equivalent.model.ts`
- Migration : `XXX-add-licenciement-be-acte-equivalent-visibility.xml`

---

## Dépendances

- SF-213-09 backend mergée.

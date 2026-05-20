# Mini-spec — F-213 / SF-213-05b-frontend Outil protection grossesse BE (UI)

## Identifiant

`F-213 / SF-213-05b-frontend`

## Feature parente

`F-213` — P2 Travail BE

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend analyseur protection grossesse BE. Visibility **CONTEXTUAL** (`grossesse_ou_maternite_detectee=true`). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.**

---

## Contrat API (SF-213-05 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-grossesse`
- `GET` même path
- Réponse 200 : `{ verdict, licenciementDansLaPeriodeProtegee, dateDebutProtection, dateFinProtection, indemniteForfaitaire, chargePreuveEmployeur, baseJuridique, avertissement }`

Verdicts : `PROTECTION_APPLICABLE` (rouge) / `PROTECTION_PRESUMEE` (rouge foncé) / `HORS_PERIODE_PROTECTION` (vert) / `PROTECTION_APPLICABLE_NON_NOTIFIEE` (ambre).

---

## Comportement attendu

`licenciement-be-protection-grossesse-section.component` :
- Champs : `dateDebutGrossesse`, `dateAccouchement` (optionnel), `dateCongeMaterniteDebut` (optionnel), `dateCongeMaterniteFinale` (optionnel), `dateLicenciement`, `grossesseNotifieeParEcrit` (checkbox), `remunerationMensuelleBrute` (€).
- Pré-fill IA : dateDebutGrossesse, dateAccouchement, dateLicenciement (→ `dateRuptureContrat`), grossesseNotifieeParEcrit, rémunération mensuelle.
- Résultat : verdict avec badge couleur + période de protection [début, fin] + indemnité forfaitaire 6 mois + message chargePreuveEmployeur + base juridique.
- `avertissement` affiché si dateFinProtection indéterminée.
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

`XXX-add-licenciement-be-protection-grossesse-visibility.xml` :
- `CONTEXTUAL`, `trigger_field='grossesse_ou_maternite_detectee'`, `trigger_value='true'`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] `<input type="date">` pour toutes les dates
- [x] `JetBrains Mono` pour baseJuridique
- [x] Badge rouge pour `PROTECTION_APPLICABLE` / `PROTECTION_PRESUMEE`
- [x] `MatSnackBar` erreurs
- [x] Pré-fill IA + provenance
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] CONTEXTUAL pour `grossesse_ou_maternite_detectee=true`.
- [ ] Verdict PROTECTION_APPLICABLE → badge rouge + indemnité 6 mois.
- [ ] `chargePreuveEmployeur` mis en évidence.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Technique

- `frontend/src/app/case-files/licenciement-be-protection-grossesse-section/` — composant + prefill-rules
- `frontend/src/app/core/models/licenciement-be-protection-grossesse.model.ts`
- Migration : `XXX-add-licenciement-be-protection-grossesse-visibility.xml`

---

## Dépendances

- SF-213-05 backend mergée.

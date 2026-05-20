# Mini-spec — F-213 / SF-213-08b-frontend Outil protection délégué syndical BE (UI)

## Identifiant

`F-213 / SF-213-08b-frontend`

## Feature parente

`F-213` — P2 Travail BE

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend analyseur protection délégué BE. Visibility **CONTEXTUAL** (`position_protegee=DELEGUE`). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.** BE-only.

---

## Contrat API (SF-213-08 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-protection-deleguee`
- `GET` même path
- Réponse 200 : `{ verdict, licenciementDansProtection, indemniteForfaitaire, anneesForfait, dateLimiteDemandeReintegration, joursRestantsReintegration, delaiReintegrationDepasse, baseJuridique, avertissement }`

---

## Comportement attendu

`licenciement-be-protection-deleguee-section.component` :
- Champs : `statutProtege` (select), `ancienneteAnnees`, `remunerationAnnuelleBrute` (€), `dateLicenciement`, `dateElectionOuMandat`, `demandeReintegrationDansTrente` (checkbox), `employeurRefuseReintegration` (checkbox, conditionnel), `circonstancesAggravantes` (checkbox).
- Pré-fill IA : statut protégé, date mandat, rémunération, date licenciement.
- Résultat : verdict badge rouge si `LICENCIEMENT_INTERDIT` + indemnité forfaitaire (années affichées) + délai réintégration avec badge rouge si expiré ou ≤ 7 j.
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

`XXX-add-licenciement-be-protection-deleguee-visibility.xml` :
- `CONTEXTUAL`, `trigger_field='position_protegee'`, `trigger_value='DELEGUE'`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] Badge rouge `LICENCIEMENT_INTERDIT`
- [x] Délai réintégration en rouge si expiré
- [x] `MatSnackBar` erreurs
- [x] Pré-fill IA + provenance
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] CONTEXTUAL pour `position_protegee=DELEGUE`.
- [ ] Indemnité forfaitaire 2/4 ans affichée.
- [ ] Délai réintégration 30 j calculé + badge urgence.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Technique

- `frontend/src/app/case-files/licenciement-be-protection-deleguee-section/` — composant + prefill-rules
- `frontend/src/app/core/models/licenciement-be-protection-deleguee.model.ts`
- Migration : `XXX-add-licenciement-be-protection-deleguee-visibility.xml`

---

## Dépendances

- SF-213-08 backend mergée.

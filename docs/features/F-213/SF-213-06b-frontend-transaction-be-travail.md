# Mini-spec — F-213 / SF-213-06b-frontend Outil transaction fin contrat BE (UI)

## Identifiant

`F-213 / SF-213-06b-frontend`

## Feature parente

`F-213` — P2 Travail BE

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend validateur de transaction BE. Visibility **CONTEXTUAL** (`transaction_proposee=true`). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.**

---

## Contrat API (SF-213-06 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/transaction-be-travail`
- `GET` même path
- Réponse 200 : `{ verdict, raisonInvalidite, ratioPourcentage, avertissement, checklistRenonciations, baseJuridique }`

---

## Comportement attendu

`transaction-be-travail-section.component` :
- Champs : `montantTransactionBrut` (€), `indemniteLegaleEtimee` (€, optionnel), `concessionsEmployeurDescrites` (checkbox), `renonciationOrdrePunlicDetectee` (checkbox), `mentionContestation` (checkbox), `renonciationsListees` (textarea liste libre).
- Pré-fill IA : montant, concessions, mentions ordre public, mention contestation.
- Résultat : verdict badge + ratio % + checklist renonciations + avertissement si ratio < 50 %.
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

`XXX-add-transaction-be-travail-visibility.xml` :
- `CONTEXTUAL`, `trigger_field='transaction_proposee'`, `trigger_value='true'`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] Pré-fill IA + provenance
- [x] Badge rouge `INVALIDE`, ambre `INVALIDE_PARTIELLE`, vert `VALIDE`
- [x] Avertissement ratio < 50 % en bannière ambre
- [x] `MatSnackBar` erreurs
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] CONTEXTUAL pour `transaction_proposee=true`.
- [ ] Checklist renonciations affichée.
- [ ] Avertissement ratio < 50 % visible.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Technique

- `frontend/src/app/case-files/transaction-be-travail-section/` — composant + prefill-rules
- `frontend/src/app/core/models/transaction-be-travail.model.ts`
- Migration : `XXX-add-transaction-be-travail-visibility.xml`

---

## Dépendances

- SF-213-06 backend mergée.

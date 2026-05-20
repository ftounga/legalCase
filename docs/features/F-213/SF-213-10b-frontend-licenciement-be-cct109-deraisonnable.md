# Mini-spec — F-213 / SF-213-10b-frontend Outil CCT 109 licenciement déraisonnable BE (UI)

## Identifiant

`F-213 / SF-213-10b-frontend`

## Feature parente

`F-213` — P2 Travail BE

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend du calculateur score CCT 109 BE. Visibility **CONTEXTUAL** (`type_rupture=LICENCIEMENT_ORDINAIRE`). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.**

---

## Contrat API (SF-213-10 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable`
- `GET` même path
- Réponse 200 : `{ echelonCCT109, nombreSemaines, indemniteCCT109, justificationEchelon, cumulAvecICP, baseJuridique, avertissement }`

---

## Comportement attendu

`licenciement-be-cct109-deraisonnable-section.component` :
- Champs : `motifCommunique` (checkbox), `motifLieAPersonne` (select), `discriminationSuspectee` (checkbox), `represaillesSuspectees` (checkbox), `proceduresRespectees` (checkbox), `remunerationHebdomadaireBrute` (€), `argumentsPatronal` (textarea, optionnel).
- Pré-fill IA : motifCommunique, motifLieAPersonne, discriminationSuspectee, represaillesSuspectees.
- Résultat : échelon CCT 109 en badge couleur gradué (vert 0 / jaune 3 / orange 8 / rouge 12 / rouge foncé 17 semaines) + montant indemnité + justification échelon + rappel cumul ICP en bannière info.
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

`XXX-add-licenciement-be-cct109-deraisonnable-visibility.xml` :
- `CONTEXTUAL`, `trigger_field='type_rupture'`, `trigger_value='LICENCIEMENT_ORDINAIRE'`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] Badge gradué : vert → rouge foncé selon échelon
- [x] Bannière info cumul ICP
- [x] `JetBrains Mono` pour `justificationEchelon`
- [x] `MatSnackBar` erreurs
- [x] Pré-fill IA + provenance
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] CONTEXTUAL pour `type_rupture=LICENCIEMENT_ORDINAIRE`.
- [ ] Échelon 3/8/12/17 affiché avec badge gradué.
- [ ] Justification textuelle de l'échelon affichée.
- [ ] Bannière cumul ICP.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Technique

- `frontend/src/app/case-files/licenciement-be-cct109-deraisonnable-section/` — composant + prefill-rules
- `frontend/src/app/core/models/licenciement-be-cct109-deraisonnable.model.ts`
- Migration : `XXX-add-licenciement-be-cct109-deraisonnable-visibility.xml`

---

## Dépendances

- SF-213-10 backend mergée.

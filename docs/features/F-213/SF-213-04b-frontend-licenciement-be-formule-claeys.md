# Mini-spec — F-213 / SF-213-04b-frontend Outil formule Claeys BE (UI)

## Identifiant

`F-213 / SF-213-04b-frontend`

## Feature parente

`F-213` — P2 Travail BE

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Section frontend du calculateur formule Claeys BE. Visibility **CONTEXTUAL** (`dateContrat < 2014-01-01`). **BELGIQUE / DROIT_DU_TRAVAIL uniquement.** BE-only — aucun équivalent FR.

---

## Contrat API (SF-213-04 backend)

- `POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-formule-claeys`
- `GET` même path
- Réponse 200 : `{ preavisClaeysMois, preavisClaeysSemaines, preavisStatutUniquesSemaines, preavisTotalSemaines, indemniteClaeysBrute, indemniteTotaleBrute, formuleClaeys, baseJuridique, avertissement }`

---

## Comportement attendu

`licenciement-be-formule-claeys-section.component` :
- Champs : `ancienneteAnneesPreStatutUnique`, `ancienneteMoisPreStatutUnique`, `remunerationAnnuelleBruteEnMilliers` (K€), `appliquerClauseSauvegarde` (toggle), `ancienneteAnneesPostStatutUnique` (conditionnel), `salaireHebdomadaireBrut` (conditionnel).
- Pré-fill IA : ancienneté pré, rémunération, clause sauvegarde.
- Affichage conditionnel des champs post-2014 si `appliquerClauseSauvegarde = true`.
- Résultat : préavis Claeys (mois) + semaines + éventuellement cumul total + indemnités.
- `avertissement` (caractère jurisprudentiel de la formule) affiché en bannière ambre.
- `CaseDashboardRefreshService.triggerRefresh()` sur POST.

### Visibility seed

`XXX-add-licenciement-be-formule-claeys-visibility.xml` :
- `CONTEXTUAL`, `trigger_field='date_contrat_pre_2014'`, `trigger_value='true'`, `BELGIQUE`, `DROIT_DU_TRAVAIL`.

---

## Conformité F-IA-04

- [x] Gate `workspaceCountry === 'BELGIQUE'`
- [x] Champs conditionnels post-2014 masqués si toggle off
- [x] `JetBrains Mono` pour `formuleClaeys`
- [x] Bannière ambre pour `avertissement`
- [x] `MatSnackBar` erreurs
- [x] Pré-fill IA + provenance
- [x] `getPrefillCount()` parité

---

## Critères d'acceptation

- [ ] CONTEXTUAL pour `dateContrat < 2014-01-01`.
- [ ] Champs post-2014 conditionnels.
- [ ] `avertissement` affiché en bannière ambre.
- [ ] Cumul Claeys + statut unique affiché si clause sauvegarde.
- [ ] `DecisionToolVisibilityIntegrityIT` vert.

---

## Technique

- `frontend/src/app/case-files/licenciement-be-formule-claeys-section/` — composant + prefill-rules
- `frontend/src/app/core/models/licenciement-be-formule-claeys.model.ts`
- Migration : `XXX-add-licenciement-be-formule-claeys-visibility.xml`

---

## Dépendances

- SF-213-04 backend mergée.

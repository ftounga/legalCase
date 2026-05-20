# SF-212-29 — Backend : outil décisionnel « congé maternité / paternité »

> Feature F-212. Outil : `F-DT-77-conge-paternite-maternite`. Fondement : L. 1225-1 à L. 1225-34 CT (maternité) ; L. 1225-35 à L. 1225-40 CT (paternité) ; L. 331-3+ CSS (IJ maternité) ; loi 16/03/2021 (réforme congé paternité — 25 jours, dont 4 obligatoires).

## Objectif

Fournir le moteur backend qui calcule la durée du congé maternité ou paternité, les indemnités journalières CPAM, la protection contre le licenciement et les délais d'action.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/conge-maternite-paternite` + `GET`.

**Congé maternité** :
- Durée légale : 16 sem (1er / 2e enfant), 26 sem (3e et +), 34 sem (jumeaux), 46 sem (triplés et +) — L. 1225-17.
- IJ CPAM : 100 % du SJR plafonné (L. 331-3 CSS).
- **Protection licenciement** (L. 1225-4) : interdiction de licencier pendant le congé et dans les 10 semaines suivant le retour (sauf faute grave ou impossibilité de maintien).
- Retour poste identique ou équivalent + entretien professionnel (L. 1225-27).

**Congé paternité** (depuis loi 16/03/2021) :
- Durée : 25 jours calendaires (32 j naissances multiples), dont 4 jours obligatoires (L. 1225-35 al. 2).
- IJ CPAM : même régime que maternité.
- Protection licenciement : L. 1225-4-1.

Verdict `CalculCongeMaternitePaternite` : durées + IJ estimées + protection licenciement + alertes si non-respect.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- `country` ≠ `FRANCE` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-30)

```
POST /api/v1/case-files/{caseFileId}/conge-maternite-paternite
Request {
  typeConge: enum MATERNITE|PATERNITE,
  rangEnfant: int,                           // 1=premier, 2=deuxième, 3=troisième+
  naissance_multiple: boolean,
  salaireMensuelBrutEuros: double,
  dateDebutConge: LocalDate,
  licenciementPendantConge: boolean,
  retourPosteDifferent: boolean
}
Response 200 {
  ...inputs (snapshot),
  dureeCongeJours: int,
  dateFinCongeTheorique: LocalDate,
  ijJournaliereEstimeeEuros: double,
  ijTotaleEstimeeEuros: double,
  protectionLicenciementJusqua: LocalDate,  // + 10 sem post-retour maternité / L. 1225-4-1 paternité
  alerteLicenciementIllegal: boolean,
  alerteRetourPosteIllegal: boolean,
  basesJuridiques: [String],
  messages: [String],
  country: "FRANCE",
  calculatedAt: Instant
}
GET …/conge-maternite-paternite → 200 | 204
```

`critereCode` F-IA-03 : `DT77_TYPE_CONGE`, `DT77_DUREE_CONGE`, `DT77_PROTECTION_LICENCIEMENT`, `DT77_RETOUR_POSTE`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `conge_maternite_paternite_detail` :
`congeMaternitePaterniétéType`, `congeMaterniteRangEnfant`, `congeMaterniteNaisanceMultiple`, `congeMaterniteDate`, `congeMaterniteсалaire`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Maternité 1er enfant → `dureeCongeJours = 112` (16 sem).
2. Maternité 3e enfant → `dureeCongeJours = 182` (26 sem).
3. Paternité simple → `dureeCongeJours = 25`.
4. Paternité naissance multiple → `dureeCongeJours = 32`.
5. `protectionLicenciementJusqua` = retour + 70 jours (10 sem) pour maternité.
6. `alerteLicenciementIllegal = true` si `licenciementPendantConge = true`.
7. 422 hors `FRANCE`.
8. Isolation workspace → 404.
9. `tool_id=F-DT-77-conge-paternite-maternite` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `CongeMaternitePaternitéCalculatorTest`** : chaque rang enfant ; multiple ; paternité ; alertes.
- **IT `CongeMaternitePaternitéControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `conge_maternite_paternite_analyses`.
- **Seed** : `tool_id=F-DT-77-conge-paternite-maternite`, `trigger_field=conge_maternite_paternite_detecte`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

Frontend (→ SF-212-30). Congé parental d'éducation post-maternité (P3, F-218 `F-DT-78`).

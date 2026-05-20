# Mini-spec — F-213 / SF-213-10-backend Outil CCT 109 BE — score licenciement manifestement déraisonnable

## Identifiant

`F-213 / SF-213-10-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-10-backend-licenciement-be-cct109-deraisonnable`

---

## Objectif

Calculateur du **score d'indemnisation CCT 109** pour licenciement manifestement déraisonnable (3 / 8 / 12 / 17 semaines de rémunération) — **vue dédiée et autonome** distincte de `F-DT-08-licenciement-validity` (qui analyse la validité globale) et de `F-DT-09-comparateur-indemnites` (qui compare plusieurs régimes). Cet outil se concentre sur le **scoring motivé** de la CCT 109 art. 9 et la justification de l'échelon retenu. **BELGIQUE UNIQUEMENT.**

---

## Source juridique BE

- **CCT n° 109 du 12 février 2014** relative à la motivation du licenciement, **art. 9** :
  - L'employeur qui licencie sans motif valable ou sans respecter les procédures doit payer une indemnité dont le montant varie selon le degré de gravité :
    - **3 semaines** : minimum légal (licenciement sans motif valable mais pas manifestement déraisonnable).
    - **8 semaines** : licenciement manifestement déraisonnable de gravité ordinaire.
    - **12 semaines** : licenciement manifestement déraisonnable de gravité haute.
    - **17 semaines** : maximum légal (faits graves, discrimination masquée, licenciement de représailles avéré).
  - Le score est appliqué en **complément** de l'ICP (indemnité compensatoire de préavis) — les deux se cumulent.
- **CCT 109 art. 10** : liste des motifs considérés valables (raisons économiques reconnues, réorganisation justifiée, faute du salarié prouvée, etc.).
- **Arrêt Conseil d'État n° 245.236 du 27/06/2019** : confirme la compatibilité CCT 109 avec la Constitution belge.
- **Charge de la preuve** : le salarié prouve le caractère manifestement déraisonnable ; l'employeur prouve le motif valable.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable`

Inputs (body) :
- `motifCommunique` (boolean) — le motif a-t-il été communiqué au salarié par écrit ?
- `motifLieAPersonne` (enum) — `SANS_MOTIF` | `MOTIF_VAGUE` | `MOTIF_PRECIS_DISPUTE` | `MOTIF_PROUVE_VALIDE`.
- `discriminationSuspectee` (boolean, défaut false) — indice de discrimination masquée ?
- `represaillesSuspectees` (boolean, défaut false) — licenciement suite à une plainte / activité syndicale / grossesse ?
- `proceduresRespectees` (boolean, défaut true) — procédures d'audition / notification respectées ?
- `remunerationHebdomadaireBrute` (BigDecimal, €) — pour calcul indemnité.
- `argumentsPatronal` (String, optionnel) — motifs invoqués par l'employeur (contexte IA).

Logique (`LicenciementBeCct109Score Calculator`) :

**Score :**

| Conditions | Échelon CCT 109 |
|---|---|
| `motifLieAPersonne = MOTIF_PROUVE_VALIDE` && `proceduresRespectees` | `NON_DERAISONNABLE` — pas d'indemnité CCT 109 |
| `!motifCommunique` OU `motifLieAPersonne = SANS_MOTIF` | Minimum **3 semaines** |
| `motifLieAPersonne = MOTIF_VAGUE` OU `!proceduresRespectees` | **3 semaines** |
| `motifLieAPersonne = MOTIF_PRECIS_DISPUTE` && au moins 1 facteur aggravant mineur | **8 semaines** |
| `discriminationSuspectee` OU `represaillesSuspectees` (1 seul) | **12 semaines** |
| `discriminationSuspectee` && `represaillesSuspectees` OU circonstances multiples aggravantes | **17 semaines** |

Calcul indemnité :
- `indemniteCCT109 = remunerationHebdomadaireBrute × nombreSemaines`.
- Note : cumul avec ICP possible — rappel dans `avertissement`.

Output :
```json
{
  "echelonCCT109": "8_SEMAINES" | "3_SEMAINES" | "12_SEMAINES" | "17_SEMAINES" | "NON_DERAISONNABLE",
  "nombreSemaines": 8,
  "indemniteCCT109": 4000.00,
  "justificationEchelon": "Motif précis mais disputé — un facteur aggravant mineur détecté (procédure incomplète)",
  "cumulAvecICP": true,
  "baseJuridique": "CCT n°109 du 12/02/2014 art. 9 ; arrêt CE n°245.236/2019",
  "avertissement": "Cette indemnité se cumule avec l'ICP (indemnité compensatoire de préavis). Calculer l'ICP séparément via l'outil Préavis statut unique ou Formule Claeys."
}
```

Persistance : `licenciement_be_cct109_deraisonnable_analyses` — unique sur `case_file_id`.

`GET` → dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation |
| `motifLieAPersonne` manquant | 400 | Obligatoire |
| `remunerationHebdomadaireBrute` ≤ 0 | 400 | Invalide |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `motifCommunique` | boolean | `motifRuptureCommuniqueParEcrit` — **BELGIQUE UNIQUEMENT** | |
| `motifLieAPersonne` | enum | dérivé `motifRupture` + analyse de la lettre de licenciement | |
| `discriminationSuspectee` | boolean | `discriminationSuspectee` — **BELGIQUE UNIQUEMENT** | |
| `represaillesSuspectees` | boolean | `represaillesSuspectees` — **BELGIQUE UNIQUEMENT** | |

`critereCode` : `BE_CCT109_MOTIF`, `BE_CCT109_DISCRIMINATION`, `BE_CCT109_REPRESAILLES`, `BE_CCT109_PROCEDURES`.

---

## Critères d'acceptation

- [ ] Motif valable + procédures → `NON_DERAISONNABLE`.
- [ ] Sans motif → 3 semaines.
- [ ] Discrimination → 12 semaines.
- [ ] Discrimination + représailles → 17 semaines.
- [ ] Indemnité calculée = salaire hebdo × semaines.
- [ ] `avertissement` cumul ICP toujours présent.
- [ ] Workspace France → 404.
- [ ] `CritereCodeIntegrityIT` vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-10b.
- Analyse de la **validité globale** du licenciement — `F-DT-08` existant.
- **Comparateur** des régimes d'indemnités — `F-DT-09` existant.
- CCT 109 procédure de **motivation** (demande de motif dans les 2 mois) — hors scope V1, mentionné dans avertissement.

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable` | OIDC | MEMBER |

### Tables

`licenciement_be_cct109_deraisonnable_analyses` — unique `case_file_id`.

### Composants backend

- `LicenciementBeCct109Deraisonnable{Analysis,Repository,Request,Result,Response,Service,Calculator,Controller}.java`
- `LicenciementBeCct109EchelonEnum.java`
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE
- Migration `XXX-create-licenciement-be-cct109-deraisonnable-analyses.xml`

---

## Plan de test

### Unitaires

- [ ] Motif valable + procédures → `NON_DERAISONNABLE`.
- [ ] Sans motif → 3 semaines.
- [ ] Motif vague sans procédure → 3 semaines.
- [ ] Motif disputé + 1 aggravant → 8 semaines.
- [ ] Discrimination → 12 semaines.
- [ ] Discrimination + représailles → 17 semaines.
- [ ] Indemnité : 500 € × 8 = 4 000 €.
- [ ] `avertissement` cumul ICP non null.

### Intégration

- [ ] `POST` BE → 200, `POST` FR → 404.

---

## Dépendances

- Aucune SF bloquante.

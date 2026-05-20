# Mini-spec — F-213 / SF-213-02-backend Outil rappel de salaire BE — calculateur indemnité + intérêts

## Identifiant

`F-213 / SF-213-02-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-02-backend-rappel-salaire-be`

---

## Objectif

Calculateur du rappel de salaire (arriérés de rémunération) selon le droit belge — montant brut réclamable, prescription applicable (1 an post-rupture / 5 ans pendant le contrat), et intérêts moratoires légaux **10 %** (taux spécifique BE). **BELGIQUE UNIQUEMENT** — prescription et taux moratoires distincts du FR.

---

## Source juridique BE

- **Loi du 12 avril 1965** relative à la protection de la rémunération des travailleurs, **art. 10** : taux d'intérêts moratoires = **10 %** (taux légal BE des créances salariales, distinct du taux civil belge et du taux légal FR).
- **Loi du 3 juillet 1978** art. 15 : prescription **1 an** post-rupture pour créances ex-contrat, **5 ans** pour arriérés pendant le contrat.
- **CC art. 2262bis** (Belgique) : prescription subsidiaire de droit commun (cas marginaux, hors scope V1).
- **Jurisprudence** : point de départ prescription arriérés salaire = date d'exigibilité de chaque paie (pas de la rupture pour les salaires dus pendant le contrat).

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/rappel-salaire-be`

Inputs (body) :
- `montantBrut` (BigDecimal, €) — arriéré de salaire brut réclamé, obligatoire.
- `dateDebutPeriode` (ISO date) — début de la période d'arriéré, obligatoire.
- `dateFinPeriode` (ISO date) — fin de la période d'arriéré (= date d'exigibilité principale), obligatoire.
- `dateRupture` (ISO date, optionnel) — si renseignée, le calculateur applique le régime 1 an post-rupture.
- `dateActionEnvisagee` (ISO date, défaut today) — date d'action envisagée.
- `typeArriereEnum` (enum) — `PENDANT_CONTRAT` | `POST_RUPTURE` | `MIXTE`.

Logique (`RappelSalaireBeCalculator`) :

**Prescription :**

| `typeArriereEnum` | Délai | Point de départ |
|---|---|---|
| `PENDANT_CONTRAT` | **5 ans** | `dateFinPeriode` (date d'exigibilité) |
| `POST_RUPTURE` | **1 an** | `dateRupture` |
| `MIXTE` | 1 an pour la part post-rupture, 5 ans pour la part pendant contrat | Calculé séparément |

- `dateLimitePrescription` calculée.
- `joursRestantsAvantPrescription = dateLimitePrescription - dateActionEnvisagee`.
- `statutPrescription` : `PRESCRIT` / `IMMINENT` (≤ 30 j) / `NON_PRESCRIT`.

**Intérêts moratoires (Loi 12/04/1965 art. 10) :**
- `tauxMoratoire = 10 %` annuel.
- `dureeAnnes = (dateActionEnvisagee - dateFinPeriode).toYears()` (prorata en décimales).
- `interetsCourus = montantBrut * tauxMoratoire * dureeAnnes`.
- `totalReclame = montantBrut + interetsCourus`.

Output (`RappelSalaireBeResponse`) :
```json
{
  "montantBrut": 5000.00,
  "interetsCourus": 500.00,
  "tauxMoratoire": "10% (Loi 12/04/1965 art. 10)",
  "totalReclame": 5500.00,
  "dateLimitePrescription": "2027-05-20",
  "joursRestantsAvantPrescription": 365,
  "statutPrescription": "NON_PRESCRIT",
  "baseJuridique": "Loi 12/04/1965 art. 10 ; Loi 03/07/1978 art. 15",
  "formuleCalcul": "5000 € × 10% × 1 an = 500 € intérêts ; total = 5500 €"
}
```

Persistance : table `rappel_salaire_be_analyses` — unique sur `case_file_id`.

`GET` du même path renvoie dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation BE-only |
| `caseFileId` hors workspace | 404 | Standard |
| `montantBrut` ≤ 0 | 400 | « Montant invalide » |
| `dateFinPeriode` < `dateDebutPeriode` | 400 | « Période invalide » |
| `typeArriereEnum` manquant | 400 | « typeArriereEnum obligatoire » |
| `typeArriereEnum = POST_RUPTURE` sans `dateRupture` | 400 | « dateRupture requise pour POST_RUPTURE » |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

Extension `TravailExtractedData` (branche BE) :

| Champ | Type | Champ `TravailExtractedData` BE | Notes |
|---|---|---|---|
| `montantBrut` | BigDecimal | `montantArrieresSalaireBrut` — **BELGIQUE UNIQUEMENT** | Extrait des fiches de paie manquantes |
| `dateDebutPeriode` | date | `dateDebutArrieresSalaire` — **BELGIQUE UNIQUEMENT** | |
| `dateFinPeriode` | date | `dateFinArrieresSalaire` — **BELGIQUE UNIQUEMENT** | |
| `dateRupture` | date | `dateRuptureContrat` (déjà existant depuis SF-207-01) | Réutilisation |
| `typeArriereEnum` | enum | dérivé de `dateRupture` : si `dateRupture` présente → `POST_RUPTURE` | Sinon `PENDANT_CONTRAT` |

`critereCode` émis : `BE_RAPPEL_SALAIRE_MONTANT`, `BE_RAPPEL_SALAIRE_PERIODE`, `BE_RAPPEL_SALAIRE_TYPE`.

---

## Critères d'acceptation

- [ ] `POST` calcule intérêts 10 % correctement (formule affichée).
- [ ] `POST` prescription 5 ans pour `PENDANT_CONTRAT`.
- [ ] `POST` prescription 1 an pour `POST_RUPTURE` depuis `dateRupture`.
- [ ] `POST` `PRESCRIT` si dépassé.
- [ ] `POST` workspace France → 404.
- [ ] `GET` renvoie dernière analyse ou 404.
- [ ] `CritereCodeIntegrityIT` reste vert.

---

## Périmètre

### Hors scope

- Frontend — SF-213-02b.
- Intérêts légaux civils (≠ taux 10 % spécifique loi salaires) — hors scope.
- Pécule de vacances BE (calcul séparé, outil distinct P3 : `pecule-vacances-be`).
- Rappel de salaire lié aux **heures supplémentaires** — couvert par F-DT-19 existant.

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/rappel-salaire-be` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/rappel-salaire-be` | OIDC | MEMBER |

### Tables

| Table | Opération | Notes |
|---|---|---|
| `rappel_salaire_be_analyses` | INSERT / UPDATE / SELECT | Unique `case_file_id`. `result_data` JSON. |

### Composants backend

- `RappelSalaireBeAnalysis.java`, `RappelSalaireBeRepository.java`
- `RappelSalaireBeTypeArriereEnum.java`
- `RappelSalaireBeRequest.java`, `RappelSalaireBeResult.java`, `RappelSalaireBeResponse.java`
- `RappelSalaireBeService.java`, `RappelSalaireBeCalculator.java`, `RappelSalaireBeController.java`
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` BE
- Migration `XXX-create-rappel-salaire-be-analyses.xml`

---

## Plan de test

### Unitaires (`RappelSalaireBeCalculatorTest`)

- [ ] Intérêts 10 % sur 1 an exact → 500 € pour 5000 € brut.
- [ ] Intérêts proratisés sur 6 mois.
- [ ] Prescription 5 ans pour `PENDANT_CONTRAT`.
- [ ] Prescription 1 an post-rupture pour `POST_RUPTURE`.
- [ ] `PRESCRIT` si `joursRestants ≤ 0`.
- [ ] `IMMINENT` si `joursRestants ∈ ]0;30]`.
- [ ] Erreur si `dateFinPeriode` < `dateDebutPeriode`.

### Intégration (`RappelSalaireBeControllerIT`)

- [ ] `POST` workspace BE → 200.
- [ ] `POST` workspace FR → 404.
- [ ] `GET` après POST → 200.
- [ ] Validation Bean : `montantBrut` négatif → 400.

---

## Dépendances

- `dateRuptureContrat` disponible dans `TravailExtractedData` depuis SF-207-01.

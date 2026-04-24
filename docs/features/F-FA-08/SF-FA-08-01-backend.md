# Mini-spec — F-FA-08 / SF-FA-08-01 Divorce altération définitive lien conjugal FR — BACKEND

## Objectif

Outil décisionnel FR pour **divorce pour altération définitive du lien conjugal** (art. 237 Cciv, réforme loi 23/03/2019). Le plus fréquent des divorces contentieux. Conditions essentiellement objectives : **cessation de communauté de vie ≥ 1 an** (réduite de 2 ans depuis 2019).

## Règles (art. 237-238 Cciv)

- Cessation effective de communauté de vie + **absence de volonté de reprise**
- Durée ≥ 1 an au moment du dépôt de l'assignation (art. 238 Cciv après loi 23/03/2019)
- Pas de recherche de tort (divorce objectif)
- Conséquences : prestation compensatoire possible (art. 270 Cciv), pas de dommages-intérêts art. 266 (réservé au divorce pour faute)

## Inputs

- `dateCessationVieCommune` : LocalDate
- `preuvesSeparationDocumentaires` : boolean (attestations hébergement séparé, bail, factures, etc.)
- `tentativesReconciliation` : boolean (signal fort contre la cessation définitive)
- `dureeMariageAnnees` : int
- `revenusAnnuelsEpoux1Eur` : BigDecimal
- `revenusAnnuelsEpoux2Eur` : BigDecimal
- `patrimoineCommunSignificatif` : boolean
- `dateAssignation` : LocalDate nullable (pour calcul du délai 1 an)

## Outputs

- `dureeSeparationAnnees` : double (entre dateCessation et dateAssignation ou today)
- `delaiObjectifOk` : boolean (≥ 1 an)
- `absencePreuveReconciliation` : boolean
- `conditionsReunies` : boolean
- `criteresNonRemplis` : liste human-readable
- `prestationCompensatoireFourchetteMin/Max` : estimation basée sur différentiel revenus + durée mariage
- `scoreGlobal` : 0-100
- `verdictProbabilite` : ELEVEE / MOYENNE / FAIBLE
- `formule`
- `baseJuridique` : "Art. 237-238 Cciv (loi 23/03/2019)"
- `messages` : importance preuves séparation, pas de DI art. 266 (réservé faute), prérequis F-FA-12 mesures provisoires

## Architecture

Pattern F-IM-09 AES (scoring). Single-country FR. Migration 127. Table `divorce_alteration_analyses`. UUID visibility `f1a04001-0000-0000-0000-ee00000fa081`, ALWAYS_ON FR DROIT_FAMILLE, priority 70, tool_id `F-FA-08-divorce-alteration`.

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/divorce-alteration`

## Tests
~14 UT + 8 IT. Gate FR + DROIT_FAMILLE.

## Impact domaine
DROIT_FAMILLE FR. BE = F-FA-11 (procédure distincte loi belge art. 229 CC).

## Hors scope
- Frontend (SF ultérieure avec scan cohérence template canonique)
- Mesures provisoires art. 254 (= F-FA-12)
- Cas violence conjugale → F-FA-15

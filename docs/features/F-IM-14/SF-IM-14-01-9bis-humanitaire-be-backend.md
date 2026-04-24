# Mini-spec — F-IM-14 / SF-IM-14-01 9bis humanitaire BE — BACKEND

## Objectif

Outil BE "Régularisation 9bis humanitaire" — art. 9bis Loi 15/12/1980 sur les étrangers. Procédure de régularisation pour circonstances exceptionnelles demandée à partir du territoire belge (pas depuis l'étranger).

## Règles

Conditions (faisceau d'indices) :
- Présence en Belgique (pas de durée minimale légale, mais ≥ 3 ans signal)
- Circonstances exceptionnelles empêchant le retour au pays pour demander un visa
- Enracinement durable (liens familiaux, sociaux, professionnels, scolaires)
- Pas de menace à l'ordre public
- Instruction : Office des étrangers (OE), délai variable 3-24 mois

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/belgian-9bis`

### Inputs
- `dateEntreeBelgique` : LocalDate
- `dureePresenceMois` : int
- `circonstancesExceptionnelles` : boolean
- `liensFamiliauxBe` : boolean
- `liensProfessionnels` : boolean (contrat travail, emploi)
- `scolariteEnfantsBe` : boolean (enfants scolarisés)
- `menaceOrdrePublic` : boolean
- `dateDepotDemande` : LocalDate nullable

### Outputs
- `presence3AnsOk` : boolean
- `liensConstitutifsOk` : boolean (famille ou travail ou scolarité)
- `pasMenace` : boolean
- `scoreGlobal` : int 0-100 (présence +25, liens +40, pas menace +35)
- `verdictProbabilite` : ELEVEE (≥75) / MOYENNE (40-74) / FAIBLE (<40)
- `criteresNonRemplis` : liste
- `dateExpirationInstructionPrevisionnelle` = dateDepotDemande + 18 mois (moyenne)
- `baseJuridique` : "Loi 15/12/1980 art. 9bis + AR 17/05/2007"
- `messages` incluant alternative art. 9ter si motif médical

## Architecture

Pattern `AesFamilleCalculator` (F-IM-09-02 récent). Migration 123. Table `belgian_9bis_analyses`. UUID visibility `f1a04001-0000-0000-0000-ee0000000141`, ALWAYS_ON BE, priority 64, tool_id `F-IM-14-9bis-humanitaire-be`.

## Tests
~15 UT + ~8 IT. Gate country==BELGIQUE + DROIT_IMMIGRATION.

## Impact domaine
DROIT_IMMIGRATION BE uniquement.

## Critères
- 4 conditions cumulatives évaluées
- Scoring pondéré
- Cross-country FR → 400
- Workspace étranger → 404

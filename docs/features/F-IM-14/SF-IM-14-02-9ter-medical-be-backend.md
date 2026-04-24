# Mini-spec — F-IM-14 / SF-IM-14-02 9ter médical BE — BACKEND

## Objectif

Outil BE "Régularisation 9ter médical" — art. 9ter Loi 15/12/1980. Procédure pour étranger souffrant d'une **maladie grave** nécessitant des soins impossibles ou inaccessibles au pays d'origine.

## Règles

Conditions cumulatives :
- Maladie grave (certificat médical type obligatoire — CM 1 et 2)
- Soins nécessaires
- Soins impossibles ou inaccessibles dans le pays d'origine
- Instruction OE + médecin-fonctionnaire délégué (avis)
- Délai instruction moyen : 6-24 mois

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/belgian-9ter`

### Inputs
- `dateDebutSymptomes` : LocalDate nullable
- `maladieGraveCertifiee` : boolean (certificat médical type produit)
- `soinsNecessairesDisponiblesBe` : boolean
- `soinsInaccessiblesPaysOrigine` : boolean (preuve)
- `menaceOrdrePublic` : boolean
- `dateDepotDemande` : LocalDate nullable

### Outputs
- `certificatMedicalType1Ok` : boolean (= maladieGraveCertifiee)
- `soinsRequisOk` : boolean (= soinsNecessairesDisponiblesBe)
- `inaccessibiliteOk` : boolean (= soinsInaccessiblesPaysOrigine)
- `pasMenace` : boolean
- `scoreGlobal` : int 0-100 (chaque condition = 25 pts)
- `verdictProbabilite` : ELEVEE / MOYENNE / FAIBLE
- `criteresNonRemplis` : liste
- `dateExpirationInstructionPrevisionnelle` = dateDepotDemande + 12 mois
- `baseJuridique` : "Loi 15/12/1980 art. 9ter + AR 17/05/2007 art. 7-8"
- `messages` : "Certificat médical type à actualiser tous les 6 mois", "Examen médecin-fonctionnaire délégué obligatoire", "En cas de refus, recours CCE annulation dans 30 jours"

## Architecture

Pattern AES. Migration 124. Table `belgian_9ter_analyses`. UUID `f1a04001-0000-0000-0000-ee0000000142`, ALWAYS_ON BE, priority 65.

## Tests
~12 UT + 8 IT. Gate BE.

## Impact domaine
DROIT_IMMIGRATION BE uniquement.

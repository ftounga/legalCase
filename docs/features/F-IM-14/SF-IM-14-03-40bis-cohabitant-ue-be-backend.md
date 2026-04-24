# Mini-spec — F-IM-14 / SF-IM-14-03 40bis cohabitant UE BE — BACKEND

## Objectif

Outil BE "Regroupement familial d'un citoyen UE" — art. 40bis Loi 15/12/1980. Membre de famille d'un ressortissant UE (hors Belge — voir 40ter pour Belge) séjournant en Belgique.

## Règles

Conditions :
- Lien familial : conjoint, partenaire enregistré, descendant/ascendant à charge
- Regroupant = citoyen UE (autre que Belge) exerçant son droit de libre circulation (travailleur, étudiant, inactif avec ressources + assurance)
- Ressources suffisantes du regroupant
- Assurance maladie couvrant l'UE
- Logement suffisant (pas de surface minimale mais preuve)
- Pas de menace OP

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/belgian-40bis`

### Inputs
- `lienFamilial` : enum (CONJOINT, PARTENAIRE_ENREGISTRE, DESCENDANT_MINEUR, DESCENDANT_MAJEUR_CHARGE, ASCENDANT_CHARGE)
- `regroupantCitoyenUe` : boolean
- `regroupantActiviteCategorie` : enum (TRAVAILLEUR, ETUDIANT, INACTIF_AVEC_RESSOURCES, AUTRE)
- `ressourcesSuffisantes` : boolean
- `assuranceMaladieUe` : boolean
- `logementSuffisant` : boolean
- `menaceOrdrePublic` : boolean
- `dateDepotDemande` : LocalDate nullable

### Outputs
- Booleans par condition (`lienValide`, `regroupantValide`, `ressourcesOk`, `assuranceOk`, `logementOk`, `pasMenace`)
- `scoreGlobal` : 0-100 (15 points par condition)
- `verdictProbabilite`
- `criteresNonRemplis`
- `dateExpirationInstruction` = depot + 6 mois (délai OE)
- `baseJuridique` : "Loi 15/12/1980 art. 40bis + AR 08/10/1981 art. 52"
- `messages` : "Carte F délivrée si accepté", "En cas de refus : recours CCE annulation 30j"

## Architecture

Migration 125. Table `belgian_40bis_analyses`. UUID `f1a04001-0000-0000-0000-ee0000000143`, ALWAYS_ON BE, priority 66.

## Tests
~14 UT + 8 IT. Gate BE. Distinct de 40ter (regroupant Belge, règles + strictes).

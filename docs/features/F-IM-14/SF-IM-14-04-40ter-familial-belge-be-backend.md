# Mini-spec — F-IM-14 / SF-IM-14-04 40ter familial Belge BE — BACKEND

## Objectif

Outil BE "Regroupement familial d'un Belge" — art. 40ter Loi 15/12/1980. Conditions **plus strictes** que 40bis (regroupant UE non-Belge) : minima de ressources imposés légalement.

## Règles

Conditions :
- Lien familial (conjoint, partenaire légalement enregistré, descendant/ascendant)
- Regroupant = ressortissant Belge
- **Revenus stables et réguliers** ≥ 120 % du RIS (Revenu d'intégration sociale) — minimum indexé ~1740 € net/mois (2025)
- Assurance maladie
- Logement suffisant
- Pas de menace OP

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/belgian-40ter`

### Inputs
- `lienFamilial` : enum (CONJOINT, PARTENAIRE_LEGAL_ENREGISTRE, DESCENDANT_MINEUR, DESCENDANT_MAJEUR_CHARGE, ASCENDANT_CHARGE_HANDICAP)
- `regroupantBelge` : boolean
- `revenusMensuelsNetsEur` : BigDecimal (revenu net du regroupant)
- `seuil120PctRisEur` : BigDecimal (valeur indicative saisie par avocat, default 1740)
- `assuranceMaladie` : boolean
- `logementSuffisant` : boolean
- `menaceOrdrePublic` : boolean
- `dateDepotDemande` : LocalDate nullable

### Outputs
- `lienValide` : boolean
- `regroupantBelgeOk` : boolean
- `revenusSuffisantsOk` : boolean (revenusMensuelsNetsEur >= seuil120PctRisEur)
- `assuranceOk`, `logementOk`, `pasMenace`
- `differentielRevenus` : BigDecimal (revenus - seuil, peut être négatif)
- `scoreGlobal` : 0-100 (18 points par condition + 10 pts si differentiel positif > 20%)
- `verdictProbabilite`
- `criteresNonRemplis`
- `dateExpirationInstruction` = depot + 6 mois
- `baseJuridique` : "Loi 15/12/1980 art. 40ter + AR 08/10/1981 + AR 7/10/1981 seuil RIS"
- `messages` : "Carte F délivrée après 5 ans", "Attention si ressources juste au seuil → contester refus si calcul OE erroné", "Recours CCE annulation 30 jours"

## Architecture

Migration 126. Table `belgian_40ter_analyses`. UUID `f1a04001-0000-0000-0000-ee0000000144`, ALWAYS_ON BE, priority 67.

## Tests
~15 UT + 8 IT. Gate BE.

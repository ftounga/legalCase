# Mini-spec — F-FA-12 / SF-FA-12-01 Mesures provisoires (art. 254 Cciv) — BACKEND

## Objectif

Outil décisionnel FR de soutien à l'audience d'orientation et sur mesures provisoires (AOMP). Le JAF statue à cette audience sur les mesures provisoires applicables pendant la procédure de divorce contentieux : **pension alimentaire ad interim, attribution provisoire du logement, résidence provisoire des enfants, contribution aux charges du mariage, mesures conservatoires** (art. 254 à 256 Cciv).

**Prérequis** des outils F-FA-08 (altération), F-FA-09 (faute) et F-FA-10 (accepté) côté FR. Pour BE, voir F-FA-11.

## Règles (art. 254-256 Cciv + jurisprudence Cass. 1ère civ.)

- L'audience d'orientation et sur mesures provisoires (AOMP) est saisie par requête conjointe ou unilatérale.
- Le JAF peut prononcer toutes mesures provisoires utiles : pension ad interim, attribution provisoire du logement, résidence des enfants, contribution aux charges du mariage, mesures conservatoires patrimoniales (art. 220-1 Cciv).
- En cas de violences alléguées, la priorité de protection s'impose : attribution du logement à l'époux non-violent, ordonnance de protection (art. 515-9) cumulable.
- Résidence alternée : possible si âge des enfants ≥ 6 ans (jurisprudence majoritaire) et concordance des parents (à défaut, mesure exclusive chez l'un des parents, idéalement le parent gardien principal).
- Pension alimentaire ad interim : méthode simplifiée fondée sur le différentiel de revenus (≈ moitié du différentiel) — la pension définitive est calculée par F-FA-02 (table de référence pension alimentaire).
- Contribution aux charges du mariage : moitié des revenus les plus élevés (méthode pratique des juges).

## Inputs

- `dateAudienceAOMP` : LocalDate (date de l'audience JAF, future ou passée)
- `revenusEpouxDemandeurEur` : BigDecimal (revenus mensuels nets)
- `revenusEpouxDefendeurEur` : BigDecimal (revenus mensuels nets)
- `logementCommunDescription` : String (description libre — adresse, type, surface)
- `logementProprietaire` : enum `EN_INDIVISION` / `PROPRIETE_DEMANDEUR` / `PROPRIETE_DEFENDEUR` / `LOCATION_COMMUNE`
- `enfantsMineurs` : List<EnfantMineurInfo{prenom, age}> (peut être vide)
- `souhaitResidenceEnfants` : enum `ALTERNEE` / `EXCLUSIVE_DEMANDEUR` / `EXCLUSIVE_DEFENDEUR`
- `violencesAlleguees` : boolean
- `patrimoineCommunIsignificatif` : boolean
- `demandeMesureConservatoire` : boolean

## Outputs

- `differentielRevenus` : BigDecimal = `revenusEpouxDemandeur - revenusEpouxDefendeur` (signé)
- `pensionAlimentairePropose` : BigDecimal = `max(0, |differentielRevenus|/2)` (méthode simplifiée — vraie pension via F-FA-02)
- `attributionLogementRecommande` : enum `DEMANDEUR` / `DEFENDEUR` / `INDIVISION_MAINTENUE`
- `residenceEnfantsRecommande` : enum `ALTERNEE` / `EXCLUSIVE_DEMANDEUR` / `EXCLUSIVE_DEFENDEUR` / `SANS_OBJET` (si pas d'enfants)
- `contributionCharges` : BigDecimal = `max(revenusDemandeur, revenusDefendeur) / 2`
- `mesureConservatoireRecommande` : boolean
- `scoreCohesionMesures` : 0-100 (mesure l'écart entre demande et recommandation)
- `verdictAcceptabilite` : ELEVEE / MOYENNE / FAIBLE
- `formule`
- `baseJuridique` : "Art. 254-256 Cciv + jurisprudence Cass. 1ère civ."
- `messages` : recommandations procédurales, alerte violences, alerte âge enfants, alerte mesure conservatoire

## Logique

### Attribution du logement
- Si `violencesAlleguees == true` → l'autre époux (alerte critique).
- Sinon, si `logementProprietaire == PROPRIETE_DEMANDEUR` → DEMANDEUR.
- Sinon, si `logementProprietaire == PROPRIETE_DEFENDEUR` → DEFENDEUR.
- Sinon (indivision / location commune) → l'époux avec les revenus les plus faibles (logement nécessité primaire).
- Si revenus égaux → INDIVISION_MAINTENUE (audience renvoyée pour départage).

### Résidence des enfants
- Si pas d'enfant → SANS_OBJET.
- Si `violencesAlleguees == true` → exclusive parent non-violent (par défaut le demandeur si ce dernier signale les violences).
- Sinon, on respecte `souhaitResidenceEnfants` si compatible :
  - Alternée acceptée si tous les enfants ≥ 6 ans.
  - Sinon, par défaut résidence exclusive chez le parent désigné par le souhait (sinon démarrage demandeur).

### Contribution aux charges
- `contributionCharges = max(revenus) / 2` arrondi à 2 décimales.

### Mesure conservatoire
- `mesureConservatoireRecommande = patrimoineCommunIsignificatif && demandeMesureConservatoire`.

### Score de cohésion
- Démarre à 100. Si la résidence recommandée diverge du souhait : -20.
- Si l'attribution du logement diverge de l'évidence (logement déjà propriété d'un époux) : -10.
- Si violences et résidence non protectrice : -30.
- Si revenus inconnus / égaux et indivision maintenue : -15.
- Borné [0, 100].

### Verdict
- ELEVEE ≥ 75, MOYENNE 40-74, FAIBLE < 40.

## Architecture

Pattern miroir DivorceAlteration (single-country FR DROIT_FAMILLE). Migration **136**. Table `mesures_provisoires_analyses`. UUID visibility `f1a04001-0000-0000-0000-ee00000fa121`, ALWAYS_ON FR DROIT_FAMILLE, priority 74, tool_id `F-FA-12-mesures-provisoires`.

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/mesures-provisoires`

### Request

```json
{
  "dateAudienceAOMP": "2026-06-15",
  "revenusEpouxDemandeurEur": 3500.00,
  "revenusEpouxDefendeurEur": 2000.00,
  "logementCommunDescription": "Maison Lyon 5e, 130m², location commune",
  "logementProprietaire": "EN_INDIVISION",
  "enfantsMineurs": [{"prenom": "Léa", "age": 8}, {"prenom": "Tom", "age": 12}],
  "souhaitResidenceEnfants": "ALTERNEE",
  "violencesAlleguees": false,
  "patrimoineCommunIsignificatif": true,
  "demandeMesureConservatoire": false
}
```

### Response

```json
{
  "caseFileId": "uuid",
  "dateAudienceAOMP": "...",
  "differentielRevenus": 1500.00,
  "pensionAlimentairePropose": 750.00,
  "attributionLogementRecommande": "DEFENDEUR",
  "residenceEnfantsRecommande": "ALTERNEE",
  "contributionCharges": 1750.00,
  "mesureConservatoireRecommande": false,
  "scoreCohesionMesures": 100,
  "verdictAcceptabilite": "ELEVEE",
  "baseJuridique": "Art. 254-256 Cciv + jurisprudence Cass. 1ère civ.",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

### Codes d'erreur

- 400 si workspace ≠ FRANCE (BE = pas de procédure équivalente directe — voir F-FA-11).
- 400 si dossier ≠ DROIT_FAMILLE.
- 400 si `dateAudienceAOMP`, `revenusEpouxDemandeurEur`, `revenusEpouxDefendeurEur`, `logementProprietaire`, `souhaitResidenceEnfants` manquants.
- 400 si revenus négatifs ou âges enfants invalides.
- 404 si dossier inexistant ou hors workspace.

## Tests

- ≥ 16 UT couvrant : nominal alternée, résidence exclusive (âges < 6 ans), violences (override attribution + résidence), revenus égaux (indivision), pas d'enfant (SANS_OBJET), patrimoine + demande conservatoire, contribution charges, scoring divergence, validations input.
- ≥ 10 IT (gate FR, gate DROIT_FAMILLE, isolation workspace, upsert, GET sans POST 404, missing fields 400, futureDateAudience accepté, négatif rejeté, nominal renvoie payload).

## Impact domaine

DROIT_FAMILLE FR. BE = traité par F-FA-11 (procédure distincte loi belge). Pas de version DROIT_DU_TRAVAIL ni DROIT_IMMIGRATION (concept inapplicable).

## Parité des domaines métier (niveau ≥ 5)

- Niveau outil : 5 (scoring de cohésion mesures).
- DROIT_FAMILLE FR : SF-FA-12-01 (le présent backend).
- DROIT_FAMILLE BE : F-FA-11 couvre les mesures provisoires belges (procédure distincte). Aucune SF jumelle requise.
- DROIT_DU_TRAVAIL : non applicable (concept matrimonial).
- DROIT_IMMIGRATION : non applicable.

## Hors scope

- Frontend (SF-FA-12-02 planifiée vague suivante avec scan cohérence template canonique F-155).
- Calcul détaillé pension alimentaire (= F-FA-02).
- Attribution définitive du logement (= F-FA-04 partage du patrimoine).
- Ordonnance de protection art. 515-9 Cciv (= F-FA-15 violence conjugale).
- Mise à jour calendrier procédural (= F-136 / F-137 si pertinent).

## Préoccupations transversales

- Auth / Principal : pattern OidcUser + Principal identique aux Divorce* (rien à changer).
- Workspace context : pattern WorkspaceMemberRepository identique.
- Plans / limites : aucun gate spécifique (outil basique inclus dans tous les plans).
- Navigation : aucune route ajoutée côté backend.
- Outil décisionnel : outil neuf sur situation métier propre (mesures provisoires AOMP), pas de chevauchement avec un autre outil existant. Invariant respecté.

## Analyse de cohérence transversale

- BE équivalent : F-FA-11 (déjà en place).
- DROIT_DU_TRAVAIL / DROIT_IMMIGRATION : non applicable.
- Pas de nouveau pattern UI ou service partagé (composants DTO/calculator/service identiques aux Divorce*).
- Frontend : SF-FA-12-02 ouverte au backlog, à scanner contre template canonique F-155 lors de la mise en chantier.

## Impact par domaine métier

- **DROIT_DU_TRAVAIL** : non applicable (concept matrimonial, hors compétence prud'hommes).
- **DROIT_IMMIGRATION** : non applicable.
- **DROIT_FAMILLE FR** : couvert par cette SF.
- **DROIT_FAMILLE BE** : non applicable (la procédure provisoire belge est intégrée dans F-FA-11 — divorce désunion irrémédiable).

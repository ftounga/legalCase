# Mini-spec — F-214 / SF-214-43 — Autorisation travail employeur L. 421-1 — backend

## Identifiant

`F-214 / SF-214-43`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Analyser les obligations de l'employeur souhaitant recruter un travailleur étranger hors UE (autorisation de travail préalable, vérification, recours en cas de refus DREETS/DDETS), côté employeur — complémentaire à F-IM-07 (côté étranger).

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/autorisation-travail-employeur-analysis`
- Body : `typeContrat` (enum `CDI` | `CDD` | `INTERIM`), `posteProposes` (string ≤ 200), `nationaliteCandidat` (string), `dureeContratMois` (int, optionnel pour CDI), `refusAutorisation` (boolean), `dateRefusAutorisation` (LocalDate, optionnel)
- Analyzer `AutorisationTravailEmployeurAnalyzer` :
  - Vérifie si le poste nécessite une autorisation préalable (L. 5221-1 Code travail) ou si exempt (citoyen UE/EEE/Suisse, accord bilatéral)
  - `obligationsDemande` : liste (formulaire CERFA 15187*03, contrat de travail, fiche métier, offre emploi publiée 3 semaines)
  - `delaiInstructionOFII` : 2 mois (R. 5221-20 Code travail)
  - `recoursPossible` : si refusAutorisation + dateRefusAutorisation → délai recours TA 2 mois
  - `taxeOFII` : indication que l'employeur paie une taxe OFII après obtention autorisation
  - `statut` ∈ {`AUTORISATION_REQUISE`, `AUTORISATION_NON_REQUISE`, `RECOURS_POSSIBLE`, `RECOURS_PRESCRIT`}
- Output persisté dans `autorisation_travail_employeur_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/autorisation-travail-employeur-analysis` → 200 ou 404

---

## Source juridique

- **L. 5221-1 à L. 5221-12 Code du travail** — autorisation de travail employeur.
- **R. 5221-1 à R. 5221-44 Code du travail** — procédure demande.
- **L. 421-1 CESEDA** — titre autorisant le séjour pour exercice d'une activité salariée.
- **R. 5221-20 Code du travail** — délai instruction 2 mois.
- **CE 2 octobre 2015, n° 384547** (à vérifier) — refus autorisation de travail, conditions.

**Distinction F-IM-07 vs F-IM-46** : F-IM-07 détermine si l'étranger peut travailler avec son titre (côté étranger). F-IM-46 analyse les démarches et obligations de l'employeur qui recrute (côté employeur). Deux situations juridiques distinctes avec des acteurs différents.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `nationaliteCandidat` | texte | `nationalite` | Réutiliser |
| `typeContrat` | enum | `typeTitreSejour` (proxy indirect) | Dériver |

**ALWAYS_ON** : outil ALWAYS_ON — tout dossier Immigration FR avec une dimension emploi/travail. Pertinent pour les avocats conseillant les employeurs. Pas de nouveau flag IA requis.

---

## Critères d'acceptation

- [x] POST nationalité UE → AUTORISATION_NON_REQUISE
- [x] POST CDI hors UE → AUTORISATION_REQUISE + obligationsDemande + delaiInstructionOFII
- [x] POST refusAutorisation=true → RECOURS_POSSIBLE + délai TA
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-46-autorisation-travail-employeur-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : ALWAYS_ON, DROIT_IMMIGRATION, FRANCE

## Plan de test minimal

- **UT** `AutorisationTravailEmployeurAnalyzerTest` : 6+ cas
- **IT** `AutorisationTravailEmployeurControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `autorisation_travail_employeur_analyses`
- **Migration Liquibase** + seed visibility rules (ALWAYS_ON)
- Pas d'extension `ImmigrationExtractedData` (`nationalite` existant suffisant)
- **Endpoint** `AutorisationTravailEmployeurController`

## Hors périmètre

- Composant Angular (SF-214-44)
- Taxe OFII montants (calculateur séparé P4 → F-220)

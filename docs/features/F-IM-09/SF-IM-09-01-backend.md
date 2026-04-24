# Mini-spec — F-IM-09 / SF-IM-09-01 AES — Métiers en tension — BACKEND

## Identifiant
`F-IM-09 / SF-IM-09-01`

## Feature parente
`F-IM-09` — AES 4 motifs distincts (🔴 critique)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-09-01-aes-metiers-tension-backend`

## Objectif

Premier des 4 outils dédiés AES (invariant "un outil = une situation"). Outil **AES métiers en tension** — loi du 26 janvier 2024 instituant une procédure d'admission exceptionnelle au séjour sans condition de visite médicale pour les étrangers justifiant 3 ans présence + 12 mois d'activité salariée dans un métier figurant sur la liste des métiers en tension (arrêté 1er avril 2021 et suivants).

**Single-country FR** — pas d'équivalent BE standard pour ce régime spécifique.

## Comportement

### Règles

- **Loi 26/01/2024 art. 26** : AES travail métier en tension, voie expérimentale 1er janvier 2024 au 31 décembre 2026.
- **Conditions cumulatives** :
  - Présence en France depuis au moins 3 ans
  - Justification d'au moins 12 mois d'activité salariée dans les 24 derniers mois
  - Métier figurant sur liste régionale des métiers en tension (arrêté consolidé 2021-2024)
  - Absence de menace pour l'ordre public
  - Contrat de travail ou promesse d'embauche valide
- **Délai d'instruction** : 6 mois (silence vaut rejet)
- **Titre délivré** : carte de séjour temporaire "salarié" ou "travailleur temporaire"

### Inputs

- `dateEntreeFrance` : LocalDate
- `moisActiviteSalarieeDernieres24Mois` : int 0-24
- `metierEstEnTension` : boolean
- `codeMetier` : string nullable (ex. "N1101" pour conducteur de véhicules — ROME)
- `menaceOrdrePublic` : boolean
- `contratOuPromesseValide` : boolean
- `dateDepotDemande` : LocalDate nullable

### Outputs

- `presence3Ans` : boolean (= dateEntreeFrance ≤ now - 3 ans)
- `activite12MoisOk` : boolean (= moisActiviteSalarieeDernieres24Mois ≥ 12)
- `conditionsReunies` : boolean (toutes conditions OK)
- `criteresNonRemplis` : array des conditions manquantes
- `dateEligibiliteAtteinte` : LocalDate (quand l'étranger atteindra les 3 ans si pas encore)
- `dateExpirationInstructionSiDemande` : LocalDate nullable (dateDepotDemande + 6 mois)
- `formule`
- `baseJuridique` : "Loi 26/01/2024 art. 26 + CESEDA L.435-4"
- `messages` : conseils (preuves à recueillir, risques de rejet, alternative si non éligible)

### Validation
- dateEntreeFrance dans futur → 400
- moisActiviteSalarieeDernieres24Mois < 0 ou > 24 → 400
- dateDepotDemande avant dateEntreeFrance → 400
- Workspace BE → 400 ("régime AES métier en tension propre à la France")
- Dossier non-immigration → 400
- Workspace étranger → 404

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/aes-metiers-tension`

## Architecture

Pattern DiscriminationCalculator / OqtfAvecDelai. Single-country FR. Migration 119. Tool_id `F-IM-09-aes-metiers-tension`. **Règle visibility CONTEXTUAL** FR sur trigger `type_procedure_detectee=AES_METIERS_TENSION` (code à ajouter futur F-IM-16 SF-02 ou simple ALWAYS_ON).

**Décision** : ALWAYS_ON FR pour simplicité (évite extension enum). UUID `f1a04001-0000-0000-0000-ee000000091a`, priority 60.

## Composants
- `AesMetiersTensionCalculator.java`
- `AesMetiersTensionAnalysis.java`
- `AesMetiersTensionRepository.java`
- `AesMetiersTensionRequest/Response/Result.java`
- `AesMetiersTensionService.java`
- `AesMetiersTensionController.java`
- Migration `119-create-aes-metiers-tension-analyses.xml`

## Tests
- Calcul presence3Ans (LocalDate.now() - 3 ans)
- Calcul activite12MoisOk
- conditionsReunies = AND de tous critères
- criteresNonRemplis listés si pas conditions réunies
- dateEligibiliteAtteinte si pas encore 3 ans
- Validation inputs
- Gate FR + DROIT_IMMIGRATION
- ≥12 UT + ≥8 IT

## Impact domaine
DROIT_IMMIGRATION FR. BE pas d'équivalent.

## Hors scope
- Frontend (à planifier ultérieurement SF-IM-09-01b)
- Liste métiers en tension comme référentiel (`LegalReferential type METIERS_TENSION`) → peut être une SF ultérieure
- Autres AES (famille L.435-1, humanitaire L.435-2, étudiant) → SF-IM-09-02/03/04

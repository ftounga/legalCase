# Mini-spec — F-IM-09 / SF-IM-09-03 AES — Voie humanitaire (L.435-2) — BACKEND

## Identifiant
`F-IM-09 / SF-IM-09-03`

## Feature parente
`F-IM-09` — AES 4 motifs distincts (🔴 critique)

## Statut `in-progress` · Date `2026-04-24` · Branche `feat/SF-IM-09-03-aes-humanitaire-backend`

## Objectif

Troisième des 4 outils dédiés AES (invariant "un outil = une situation"). Outil **AES voie humanitaire** — art. L.435-2 CESEDA : admission exceptionnelle au séjour pour motifs humanitaires après consultation de la commission du titre de séjour (L.432-14). Distinct de SF-IM-09-01 (métiers en tension L.435-4), SF-IM-09-02 (famille L.435-1), SF-IM-09-04 (étudiant).

**Single-country FR** — pas d'équivalent BE standard pour ce régime.

## Comportement

### Règles

- **Art. L.435-2 CESEDA** : admission exceptionnelle au séjour pour motifs humanitaires, après consultation de la commission du titre de séjour (L.432-14).
- **Motifs humanitaires reconnus (faisceau d'indices)** :
  - Risques personnels en cas de retour au pays (même sans statut d'asile)
  - Isolement total (pas de famille au pays)
  - Victime de violences (femmes, minorités)
  - Victime de traite / proxénétisme (connexe L.425-1)
  - Situation médicale précaire non couverte par L.425-9 (étranger malade)
  - Autre motif humanitaire (faisceau)
- **Adaptation preuves selon motif** : médicales pour situation médicale ; mains courantes / plaintes / associations (CIDFF, FNSF, Voix de femmes) pour violences ou traite.
- **Pas de menace pour l'ordre public**.
- **Délai d'instruction** : 6 mois à compter du dépôt (silence vaut rejet).
- **Commission titre de séjour L.432-14** : consultation recommandée pour les cas longs, complexes ou médicaux (pédagogique).

### Inputs

- `dateEntreeFrance` : LocalDate (obligatoire)
- `motifHumanitaireDominant` : enum `MotifHumanitaire` (obligatoire) :
  - `RISQUES_AU_RETOUR`
  - `ISOLEMENT_TOTAL`
  - `VICTIME_VIOLENCES`
  - `VICTIME_TRAITE`
  - `SITUATION_MEDICALE_PRECAIRE_HORS_L425_9`
  - `AUTRE_HUMANITAIRE`
- `preuvesMedicales` : boolean
- `preuvesViolencesOuTraite` : boolean
- `demandeAsileDeposeeEtRejetee` : boolean
- `commissionTitreSejourSaisie` : boolean
- `menaceOrdrePublic` : boolean
- `dateDepotDemande` : LocalDate nullable

### Outputs

- `motifEligible` : boolean (tous motifs sauf `AUTRE_HUMANITAIRE` seul sans preuves spécifiques)
- `preuvesAdaptees` : boolean (preuves cohérentes avec motif)
- `commissionRequise` : boolean (vrai pour médical / cas longs — pédagogique)
- `pasMenace` : boolean (= !menaceOrdrePublic)
- `scoreGlobal` : int 0-100
- `verdictProbabiliteAcceptation` : `ELEVEE` / `MOYENNE` / `FAIBLE`
- `criteresNonRemplis` : List<String>
- `dateExpirationInstruction` : LocalDate nullable (depot + 6 mois)
- `formule` : String
- `baseJuridique` : "Art. L.435-2 CESEDA + L.432-14 (commission titre séjour)"
- `messages` : List<String> — preuves à constituer selon motif, alternatives (L.425-1 traite, L.425-9 médical, asile), délai de la commission

### Validation
- `dateEntreeFrance` null → 400
- `dateEntreeFrance` dans futur → 400
- `motifHumanitaireDominant` null → 400
- `dateDepotDemande` avant `dateEntreeFrance` → 400
- Workspace BE → 400 ("régime AES voie humanitaire propre à la France")
- Dossier non-immigration → 400
- Workspace étranger → 404

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/aes-humanitaire`

Request body JSON :
```
{
  "dateEntreeFrance": "2022-01-15",
  "motifHumanitaireDominant": "VICTIME_VIOLENCES",
  "preuvesMedicales": false,
  "preuvesViolencesOuTraite": true,
  "demandeAsileDeposeeEtRejetee": false,
  "commissionTitreSejourSaisie": true,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "2026-04-01"
}
```

Response : voir Outputs ci-dessus, plus `caseFileId`, `country`.

## Architecture

Pattern `AesMetiersTensionCalculator` / `AesMetiersTensionService`. Single-country FR. Migration 121. Tool_id `F-IM-09-aes-humanitaire`. **Règle visibility ALWAYS_ON** FR : UUID `f1a04001-0000-0000-0000-ee000000093a`, priority 62.

## Composants
- `MotifHumanitaire.java` (enum)
- `AesHumanitaireCalculator.java`
- `AesHumanitaireResult.java`
- `AesHumanitaireAnalysis.java`
- `AesHumanitaireRepository.java`
- `AesHumanitaireRequest.java`
- `AesHumanitaireResponse.java`
- `AesHumanitaireService.java`
- `AesHumanitaireController.java`
- Migration `121-create-aes-humanitaire-analyses.xml`

## Tests
- Calcul scoreGlobal / verdict selon motif + preuves adaptées
- Motif médical sans preuves → preuvesAdaptees=false, verdict FAIBLE
- Motif violences avec preuves → preuvesAdaptees=true, verdict ELEVEE
- `commissionRequise` vrai pour médical / cas longs
- `menaceOrdrePublic` → blocage
- Validation inputs (null, futur, ordre dates)
- Gate FR + DROIT_IMMIGRATION
- Isolation workspace
- ≥12 UT + ≥8 IT

## Impact domaine métier
DROIT_IMMIGRATION **FR uniquement**. BE pas d'équivalent. Transversal sur tous les motifs humanitaires au sens L.435-2. Non applicable famille / droit du travail.

## Parité des domaines métier
Niveau 5 (scoring). Équivalent BE : **absent** (régime français propre). Équivalent DROIT_DU_TRAVAIL / FAMILLE : **non pertinent** (régime strictement immigration). Aucune feature jumelle à créer.

## Analyse de cohérence transversale
- Autres AES (F-IM-09-01/02/04) : déjà séparés — invariant "un outil = une situation" respecté.
- Autres scorings immigration (F-IM-05/06/07) : indépendants, pas de conflit.
- Pattern `AesMetiersTension*` : copié à l'identique pour cohérence.
- Préoccupations transversales : aucune (pas d'auth, pas de plan, pas de nav, pas de nouveau pattern UI).

## Hors scope
- Frontend (SF-IM-09-03-frontend à planifier)
- SF-IM-09-04 étudiant
- Référentiel associations CIDFF/FNSF comme liste paramétrable

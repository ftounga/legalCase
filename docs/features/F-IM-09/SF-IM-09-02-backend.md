# Mini-spec — F-IM-09 / SF-IM-09-02 AES — Famille (liens personnels et familiaux L.435-1) — BACKEND

## Identifiant
`F-IM-09 / SF-IM-09-02`

## Feature parente
`F-IM-09` — AES 4 motifs distincts (critique)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-09-02-aes-famille-backend`

## Objectif

Deuxième des 4 outils dédiés AES (invariant "un outil = une situation"). Outil **AES voie familiale** — art. **L.435-1 CESEDA** (régularisation sur le fondement des liens personnels et familiaux), correspondant à la circulaire Valls du 28 novembre 2012 et à ses développements jurisprudentiels. Appréciation par le préfet au cas par cas selon un faisceau d'indices.

**Distinct de** :
- `REGULARISATION_EXCEPTIONNELLE` (code enum legacy identique au fond, désormais remplacé par cet outil dédié)
- AES métiers en tension (SF-IM-09-01, mergée PR #504)
- AES humanitaire (SF-IM-09-03, L.435-2)
- AES étudiant (SF-IM-09-04)

**Single-country FR** — pas d'équivalent BE.

## Comportement

### Règles

- **Base légale** : art. L.435-1 CESEDA + circulaire Valls 28/11/2012
- **Faisceau d'indices** apprécié souverainement par le préfet :
  - Durée de présence en France (≥ 5 ans = signal fort, ≥ 10 ans = quasi-automatique)
  - Liens personnels et familiaux (conjoint français ou régulier, enfants scolarisés)
  - Insertion dans la société française (contrat, formation, logement)
  - Absence de trouble à l'ordre public
  - Fréquence des contrôles OFII et scolarité des enfants
- **Délai d'instruction** : 6 mois (silence vaut rejet implicite)
- **Titre délivré** : carte de séjour "vie privée et familiale" (1 an renouvelable)

### Inputs

- `dateEntreeFrance` : LocalDate
- `dureePresenceMois` : int (0+, calculé ou saisi)
- `conjointFrancaisOuRegulier` : boolean
- `enfantsScolarisesFrance` : int (0+)
- `dureeScolaritePlusAncienEnfantAnnees` : int (0+ ; 0 si pas d'enfant scolarisé)
- `preuvesInsertion` : boolean
- `menaceOrdrePublic` : boolean
- `dateDepotDemande` : LocalDate nullable

### Outputs

- `presence5AnsOk` : boolean
- `presence10AnsOk` : boolean (signal très fort)
- `liensFamiliauxOk` : boolean (conjoint OU ≥ 1 enfant scolarisé)
- `insertionOk` : boolean
- `pasMenace` : boolean (= !menaceOrdrePublic)
- `scoreGlobal` : int 0-100 pondéré
  - +30 si `presence10AnsOk`, sinon +20 si `presence5AnsOk`
  - +30 si `liensFamiliauxOk` + bonus +10 si `dureeScolaritePlusAncienEnfantAnnees ≥ 5`
  - +20 si `insertionOk`
  - +20 si `pasMenace`
- `verdictProbabiliteAcceptation` : `ELEVEE` (≥ 80) / `MOYENNE` (50-79) / `FAIBLE` (< 50)
- `criteresNonRemplis` : liste human-readable
- `dateExpirationInstructionSiDemande` : `dateDepotDemande + 6 mois` (nullable)
- `formule`
- `baseJuridique` : "Art. L.435-1 CESEDA + Circulaire Valls 28/11/2012"
- `messages` : conseils preuves, jurisprudence, alternative L.435-2

## Critères d'acceptation

1. Score calculé conforme à la grille
2. Verdict ELEVEE/MOYENNE/FAIBLE selon seuils
3. Bonus scolarité ≥ 5 ans appliqué uniquement si liensFamiliauxOk
4. Gate FR + DROIT_IMMIGRATION
5. Isolation workspace (404 si pas membre)
6. Upsert 1:1 par case_file_id
7. Migration 120 avec règle visibility ALWAYS_ON FR priority 61 (UUID f1a04001-0000-0000-0000-ee000000092a)
8. Validation inputs (dates non futures, entiers ≥ 0, depot ≥ entrée)

## Plan de test

**Calculator (UT)** : conditions réunies, cas quasi-automatique 10 ans, cas moyen 5 ans+famille+insertion, cas faible tous KO, scoring bornes, bonus scolarité, menace=false → pasMenace=true, validation inputs.

**Controller (IT)** : nominal FR, scores FAIBLE/MOYEN/ELEVE, BE→400, DT→400, autre workspace→404, futur→400, upsert, GET après POST, GET sans POST→404.

## Tables / endpoints / composants

- Table : `aes_famille_analyses` (migration 120)
- Endpoint : `POST|GET /api/v1/case-files/{caseFileId}/aes-famille`
- Règle visibility : ALWAYS_ON DROIT_IMMIGRATION FRANCE priority 61 UUID `f1a04001-0000-0000-0000-ee000000092a` tool_id `F-IM-09-aes-famille`

## Hors périmètre

- Frontend (SF séparée)
- AES humanitaire/étudiant/métier en tension (SF jumelles)
- Appréciation opposabilité des preuves IA (pas de LLM ici)

## Impact par domaine métier

Outil sensible au domaine DROIT_IMMIGRATION FR uniquement. Hors scope DT/Famille/BE.

## Parité des domaines métier

Scoring niveau 5. Équivalent famille = barème divorce (F-FA-07) déjà livré. Équivalent DT = scoring licenciement (F-DT-08) déjà livré. Parité OK pour l'ensemble du bloc scoring L.435-1 dans F-IM-09.

## Analyse de cohérence transversale

Pattern copié strictement de SF-IM-09-01 (AES métiers en tension) mergée #504. Aucun nouveau pattern UI ou service partagé introduit ; mêmes conventions (record result/request/response, JPA entity 1:1, service OidcUser+Principal, controller @AuthenticationPrincipal). Un outil = une situation respecté.

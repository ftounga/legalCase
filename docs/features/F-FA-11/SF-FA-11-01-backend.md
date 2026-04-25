# Mini-spec — F-FA-11 / SF-FA-11-01 Divorce désunion irrémédiable BE — BACKEND

## Objectif

Outil décisionnel BE pour **divorce pour désunion irrémédiable** (art. 229 §3 Code civil belge, Loi du 27/04/2007). Équivalent belge de F-FA-08 (altération FR) et F-FA-10 (accepté FR). Procédure objective fondée sur la **séparation de fait** : 6 mois si demande conjointe / consentue, 1 an si unilatérale.

## Règles (art. 229 CC belge + Loi 27/04/2007)

- Art. 229 §1 CC : « Le divorce est prononcé lorsque le juge constate la désunion irrémédiable entre les époux »
- Art. 229 §2 CC : preuve par toutes voies de droit, ou présomption légale après séparation de fait
- Art. 229 §3 CC : présomption automatique de désunion irrémédiable :
  - **Demande conjointe (consentue)** : séparation de fait ≥ **6 mois**
  - **Demande unilatérale** : séparation de fait ≥ **1 an** (12 mois)
- Pas de recherche de tort, pas de dommages-intérêts (différent de FR art. 266)
- Conséquences : pension alimentaire entre ex-époux possible (art. 301 CC), partage des biens

## Inputs (Contrat API figé)

- `dateSeparation` : LocalDate (date de cessation effective de la cohabitation)
- `separationConsentue` : boolean (true = demande conjointe → seuil 6 mois ; false = unilatérale → seuil 12 mois)
- `preuvesSeparation` : boolean (attestations résidences distinctes, témoins, courrier huissier)
- `preuvesDocumentaires` : boolean (extraits Registre national distinct, bail, factures séparées, contrats EDF/eau/gaz à des adresses différentes)
- `tentativesReconciliation` : boolean (signal contre désunion irrémédiable)
- `dateAssignation` : LocalDate nullable (date d'introduction de la demande — sinon today)

## Outputs (Contrat API figé)

- `caseFileId` : UUID
- `dateSeparation`, `separationConsentue`, `preuvesSeparation`, `preuvesDocumentaires`, `tentativesReconciliation`, `dateAssignation` : echo des inputs
- `dureeSeparationMois` : int (mois entre dateSeparation et dateAssignation ou today)
- `seuilSeparationMois` : int (6 si consentue, 12 si unilatérale)
- `delaiObjectifOk` : boolean (`dureeSeparationMois >= seuilSeparationMois`)
- `conditionsReunies` : boolean (`delaiObjectifOk && preuvesSeparation && (preuvesDocumentaires || !tentativesReconciliation)`)
- `scoreGlobal` : int 0-100 pondéré (délai 40, preuves 30, preuvesDoc 15, absenceReconciliation 15)
- `verdictProbabilite` : `ELEVEE` (≥75) / `MOYENNE` (≥50) / `FAIBLE`
- `baseJuridique` : "Art. 229 §3 Code civil belge + Loi 27/04/2007"
- `formule` : récap textuel
- `messages` : list<String> (recommandations procédurales, pension art. 301, etc.)
- `country` : "BELGIQUE"

## Logique métier

| Étape | Calcul |
|-------|--------|
| `seuilSeparationMois` | 6 si `separationConsentue=true`, 12 si `false` |
| `dureeSeparationMois` | `ChronoUnit.MONTHS.between(dateSeparation, dateAppreciation)` |
| `dateAppreciation` | `dateAssignation` si non null, sinon `today` |
| `delaiObjectifOk` | `dureeSeparationMois >= seuilSeparationMois` |
| `conditionsReunies` | `delaiObjectifOk && preuvesSeparation && (preuvesDocumentaires || !tentativesReconciliation)` |
| `scoreGlobal` | (delai 40) + (preuves 30) + (preuvesDoc 15) + (absenceReconciliation 15) |
| `verdictProbabilite` | ELEVEE ≥75, MOYENNE ≥50, FAIBLE sinon |

## Architecture

- Pattern miroir de `DivorceAlterationCalculator` (F-FA-08).
- Single-country **BELGIQUE** (gate sur `workspace.country = BELGIQUE`).
- DROIT_FAMILLE (gate `caseFile.legalDomain = DROIT_FAMILLE`).
- Migration **131** : table `divorce_desunion_irremediable_be_analyses` + index UNIQUE `case_file_id`.
- Visibility rule F-IA-04 : UUID `f1a04001-0000-0000-0000-ee00000fa111`, ALWAYS_ON, BELGIQUE, DROIT_FAMILLE, priority 73, tool_id `F-FA-11-desunion-irremediable-be`.

## Contrat API (figé pour SF-FA-11-02 frontend)

### Endpoint
- **POST** `/api/v1/case-files/{caseFileId}/desunion-irremediable-be` — calcul + persist
- **GET** `/api/v1/case-files/{caseFileId}/desunion-irremediable-be` — relecture

### Request body (POST)
```json
{
  "dateSeparation": "2024-10-01",
  "separationConsentue": true,
  "preuvesSeparation": true,
  "preuvesDocumentaires": true,
  "tentativesReconciliation": false,
  "dateAssignation": null
}
```

### Response body (200 OK)
```json
{
  "caseFileId": "uuid",
  "dateSeparation": "2024-10-01",
  "separationConsentue": true,
  "preuvesSeparation": true,
  "preuvesDocumentaires": true,
  "tentativesReconciliation": false,
  "dateAssignation": null,
  "dureeSeparationMois": 14,
  "seuilSeparationMois": 6,
  "delaiObjectifOk": true,
  "conditionsReunies": true,
  "scoreGlobal": 95,
  "verdictProbabilite": "ELEVEE",
  "baseJuridique": "Art. 229 §3 Code civil belge + Loi 27/04/2007",
  "formule": "Séparation 14 mois ≥ seuil 6 mois (consentue) → désunion irrémédiable établie",
  "messages": ["...", "..."],
  "country": "BELGIQUE"
}
```

### Codes d'erreur
- `400 Bad Request` : workspace ≠ BELGIQUE, legalDomain ≠ DROIT_FAMILLE, body null, dateSeparation null, dateSeparation dans le futur, dateAssignation < dateSeparation
- `404 Not Found` : caseFile inexistant ou hors workspace de l'utilisateur, GET sans POST préalable

## Plan de test minimal

### Tests unitaires (≥ 12 sur calculator)
1. Conditions réunies consentue ≥ 6 mois → ELEVEE 100
2. Conditions réunies unilatérale ≥ 12 mois → ELEVEE
3. Séparation < 6 mois (consentue) → délai KO
4. Séparation entre 6 et 12 mois unilatérale → délai KO
5. Séparation ≥ 12 mois unilatérale → délai OK
6. Date d'appréciation = dateAssignation si fournie
7. Tentatives réconciliation → preuvesDoc fallback obligatoire
8. Pas de preuves séparation → conditionsReunies KO
9. Verdict seuils ELEVEE/MOYENNE/FAIBLE
10. dateSeparation null → throws
11. dateSeparation futur → throws
12. dateAssignation < dateSeparation → throws
13. Messages contiennent "art. 229" et "Loi 27/04/2007"
14. baseJuridique contient "229" et "BELGIQUE" / "Code civil belge"

### Tests intégration (≥ 8 sur endpoint)
1. POST BE nominal consentue → 200 ELEVEE
2. POST BE nominal unilatérale 13 mois → 200
3. POST BE séparation < seuil → score MOYENNE/FAIBLE
4. POST workspace FR → 400 (gate country)
5. POST DROIT_DU_TRAVAIL → 400 (gate legal_domain)
6. POST autre workspace → 404 (isolation)
7. POST dateSeparation futur → 400
8. POST upsert (replace analyse existante)
9. GET après POST → 200 persisté
10. GET sans POST → 404

## Tables / endpoints / composants impactés

- **Table créée** : `divorce_desunion_irremediable_be_analyses` (migration 131)
- **Endpoints** : POST + GET `/api/v1/case-files/{id}/desunion-irremediable-be`
- **Visibility rule** : `decision_tool_visibility_rules` (1 INSERT)

## Préoccupations transversales
- **Auth/Principal** : @AuthenticationPrincipal OidcUser, pattern identique aux autres calculators
- **Workspace context** : isolation par `workspaceMemberRepository.findByUserAndPrimaryTrue`
- **Plans/limites** : aucun changement (outil décisionnel comme les autres)
- **Navigation/routing** : aucun changement backend

## Impact par domaine métier

- **DROIT_DU_TRAVAIL** : non applicable
- **DROIT_FAMILLE** :
  - **FR** : couvert par F-FA-08 (altération art. 237) + F-FA-10 (accepté art. 233) — outils distincts
  - **BE** : couvert par CETTE feature (F-FA-11 désunion irrémédiable art. 229 CC, équivalent belge des deux outils FR car la procédure unique BE recouvre les deux cas)
- **DROIT_DES_ETRANGERS** : non applicable

Cette SF est l'**équivalent belge** pur des features F-FA-08/F-FA-10 FR. Garantit la parité de couverture FR/BE en droit du divorce.

## Parité des domaines métier (outil scoring niveau 5)

- **Niveau** : 5 (scoring / analyse validité avec verdict probabiliste)
- **Domaines comparables** :
  - DROIT_FAMILLE FR : F-FA-08 (altération) + F-FA-10 (accepté) ✓ Terminés 2026-04-25
  - DROIT_FAMILLE BE : **CETTE SF** (F-FA-11) ← équivalent belge couvrant les 2 cas FR
  - DROIT_DU_TRAVAIL : F-DT-29 (rupture conventionnelle FR) / F-DT-31 (rupture commun accord BE)
  - DROIT_DES_ETRANGERS : F-IM-12 (regroupement familial) / F-IM-13 (mariages BE)
- **Parité** : OK — la SF F-FA-11 ferme le gap "Famille BE divorce contentieux" identifié dans le bloc 2026-04-24.

## Hors scope
- Frontend (SF-FA-11-02 livrée en parallèle, contrat API ci-dessus)
- Mesures provisoires (= F-FA-12)
- Pension alimentaire entre ex-époux (art. 301 CC) — calcul détaillé hors scope
- Partage de biens (régime matrimonial belge) — F-FA-04 multi-pays à venir

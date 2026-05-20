# SF-215-01 — Single permit BE — backend

## Identifiant
`F-215 / SF-215-01`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-01-single-permit-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour l'outil `F-IM-25-single-permit-be` : analyse d'éligibilité + calcul du délai de renouvellement du single permit (autorisation unique travail + séjour, loi 30/04/1999 + AR 02/09/2018), BELGIQUE UNIQUEMENT.

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/single-permit-be-analysis`
- Body : `dateDebutPermit` (LocalDate, requis), `dateFinPermit` (LocalDate, requis), `regionInstruction` (enum : WALLONIE / FLANDRE / BRUXELLES, requis), `typeActivite` (enum : SALARIÉ / STAGIAIRE / DETACHE / CHERCHEUR / ETUDIANT, requis), `motifDemande` (enum : NOUVEAU / RENOUVELLEMENT, requis)
- `SinglePermitBeCalculator` calcule :
  - `dateLimiteDemande` = dateFinPermit − 60 jours (délai réglementaire dépôt renouvellement, AR 02/09/2018 art. 23)
  - `joursAvantExpiration` = dateFinPermit − today
  - `statutRenouvellement` ∈ { DEPOSE_EN_TEMPS (>60j avant fin), DANS_DELAI (≤60j avant fin), URGENT (≤30j), EXPIRE (today > dateFinPermit) }
  - `regionCompetente` : description de l'organisme régional compétent selon `regionInstruction` (ACTIRIS BE / VDAB FL / FOREM WAL)
  - `etapesProchaines` : liste de chaînes décrivant les étapes procédurales selon `motifDemande`
- Output persisté dans `single_permit_be_analyses` (1:1 par case_file)
- GET `/api/v1/case-files/{caseFileId}/single-permit-be-analysis` → 200 ou 404 si jamais POST

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | Outil BE-only | 400 |
| caseFile.legalDomain ≠ DROIT_IMMIGRATION | Mauvais domaine | 400 |
| dateDebutPermit postérieure à dateFinPermit | Dates incohérentes | 400 |
| dateFinPermit dans le passé depuis > 2 ans | Permit trop ancien pour analyse | 400 |
| caseFile non accessible au workspace | Isolation | 404 |
| regionInstruction inconnu | Enum invalide | 400 |

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres outils décisionnels Immigration BE | Oui | `F-IM-08-annexe13-be` : pattern identique (calculator délais + entity 1:1 + endpoint POST/GET). Réutilisation directe. |
| BelgianBusinessDaysCalculator | Non applicable | Single permit : délais en jours calendaires (pas ouvrables). |
| Flags IA `single_permit_envisage` | Oui | Déjà seedé dans `ImmigrationExtractedData` + `IMMIGRATION_INSTRUCTION` par F-203. Migration visibility_rules : DELETE ALWAYS_ON existant si présent, INSERT CONTEXTUAL sur `single_permit_envisage=true`. |
| Autres domaines (Travail/Famille) | Non | Single permit = procédure immigration uniquement. Pas de pendant Travail/Famille. |
| Workspace context | Oui | Gate `country === 'BELGIQUE'` impératif — retour 400 si FR. |

### Décision
- Étendu aux cibles applicables dans cette SF.
- Non applicable aux autres domaines — le single permit est une procédure immigration BE-only sans équivalent Travail/Famille.

---

## Conformité F-IA-04

- [ ] **Non applicable** — SF backend pure. Le composant Angular est livré dans SF-215-02.

---

## Champs IA à extraire (pré-remplissage)

| Champ du formulaire | Type | Champ source `ImmigrationExtractedData` | Extension requise |
|---------------------|------|-----------------------------------------|-------------------|
| `dateDebutPermit` | date | `singlePermitDateDebut` | Nouveau — à ajouter au record + prompt `IMMIGRATION_INSTRUCTION` (mention « BELGIQUE UNIQUEMENT ») |
| `dateFinPermit` | date | `singlePermitDateFin` | Nouveau — à ajouter au record + prompt |
| `regionInstruction` | enum | `singlePermitRegion` | Nouveau — whitelist WALLONIE/FLANDRE/BRUXELLES |
| `typeActivite` | enum | `singlePermitTypeActivite` | Nouveau — whitelist 5 valeurs |
| `motifDemande` | enum | `singlePermitMotif` | Nouveau — NOUVEAU/RENOUVELLEMENT |

Extension obligatoire du record `ImmigrationExtractedData` (5 champs) + du prompt `IMMIGRATION_INSTRUCTION` avec gate « BELGIQUE UNIQUEMENT ». Flag `single_permit_envisage` déjà présent — seuls les 5 champs de pré-fill sont nouveaux.

---

## Critères d'acceptation

- [ ] POST nominal retourne 200 avec `dateLimiteDemande`, `joursAvantExpiration`, `statutRenouvellement`, `regionCompetente`, `etapesProchaines`
- [ ] POST workspace FR retourne 400
- [ ] POST domaine Travail retourne 400
- [ ] POST dateDebutPermit > dateFinPermit retourne 400
- [ ] GET sans POST préalable retourne 404
- [ ] POST upsert remplace l'analyse précédente (idempotent)
- [ ] Isolation workspace : avocat A ne voit pas l'analyse du dossier B
- [ ] `statutRenouvellement = URGENT` si `joursAvantExpiration ≤ 30`
- [ ] `regionCompetente` = "ACTIRIS" si regionInstruction = BRUXELLES
- [ ] UT Calculator : 6+ cas couvrant les 4 statuts renouvellement
- [ ] IT Controller : 6+ tests (nominal, country guard, domain guard, isolation, GET 404, upsert)
- [ ] `F-IM-25-single-permit-be` ajouté dans `KNOWN_FRONTEND_TOOL_IDS` du `DecisionToolVisibilityIntegrityIT`
- [ ] Migration Liquibase : table `single_permit_be_analyses` + INSERT `decision_tool_visibility_rules` CONTEXTUAL (`single_permit_envisage=true`, BELGIQUE, DROIT_IMMIGRATION)

---

## Hors périmètre
- Composant Angular (SF-215-02)
- Single permit cadre haut niveau / carte bleue UE (reporté F-221)
- Single permit variants régionaux détaillés (reporté F-221)
- Renouvellement avec changement d'employeur (P3, F-221)

---

## Tables / endpoints / composants impactés
- Nouvelle table `single_permit_be_analyses` (id UUID, case_file_id UUID UNIQUE, date_debut_permit DATE, date_fin_permit DATE, region_instruction VARCHAR(20), type_activite VARCHAR(20), motif_demande VARCHAR(20), country VARCHAR(20), result_data TEXT, created_at TIMESTAMP, updated_at TIMESTAMP)
- Migration Liquibase `N-create-single-permit-be-analyses.xml` (N = prochain numéro disponible)
- Endpoint `SinglePermitBeController` POST + GET sous `/api/v1/case-files/{caseFileId}/single-permit-be-analysis`
- Extension `ImmigrationExtractedData` (5 champs) + `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION`

---

## Plan de test

### Tests unitaires
- `SinglePermitBeCalculatorTest` : cas DEPOSE_EN_TEMPS, DANS_DELAI, URGENT, EXPIRE, date invalide (debut > fin), region BRUXELLES/WALLONIE/FLANDRE

### Tests d'intégration
- `SinglePermitBeControllerIT` : POST nominal → 200, POST country FR → 400, POST domain travail → 400, GET sans POST → 404, POST upsert, POST autre workspace → 404

### Isolation workspace
- Applicable — test dédié dans `SinglePermitBeControllerIT`

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Outil décisionnel métier** — nouveau tool_id `F-IM-25-single-permit-be`
- [ ] Aucune autre préoccupation transversale

### Smoke tests E2E concernés
- [ ] Aucun smoke test concerné — SF backend pure, tests IT suffisants

---

## Dépendances
- F-203 SF-203-01 : `ImmigrationExtractedData` avec flag `single_permit_envisage` — Done ✅
- Prochain numéro de migration Liquibase disponible (à synchroniser avec les SF parallèles)

---

## Notes et décisions
- La régionalisation est le point critique de cet outil : ACTIRIS (Bruxelles), VDAB (Flandre), FOREM (Wallonie) instruisent chacun la partie "autorisation de travail" ; l'OE instruite la partie séjour. Le `regionCompetente` doit donc décrire le double guichet.
- Délai 60 jours = délai réglementaire AR 02/09/2018 art. 23 — à annoter `// (à vérifier par avocat BE)` dans le Calculator.
- Source : Loi 30/04/1999 relative à l'emploi des travailleurs étrangers ; AR 02/09/2018 relatif à l'occupation des travailleurs étrangers.

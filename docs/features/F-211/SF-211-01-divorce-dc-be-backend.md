# SF-211-01 — Divorce par consentement mutuel (DC) BE — backend

## Objectif (1 phrase)
Vérifier la recevabilité d'un divorce par consentement mutuel belge (CJ art. 1287+ et Loi 27/04/2007) à partir des critères : convention préalable complète (logement, biens, enfants, contributions), délai de réflexion ≥ 3 mois entre signature et homologation, et 6 mois minimum de vie commune si présence d'enfants mineurs non communs.

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/divorce-dc-be-analysis`
- Body : `dateSignatureConvention` (LocalDate, requis), `dateAudienceHomologation` (LocalDate, optionnel), `conventionLogement` / `conventionBiens` / `conventionGardeEnfants` / `conventionContributions` (boolean, requis), `enfantsMineursCommuns` (boolean), `epouxConsentent` (boolean)
- Calculator `DivorceDcBeCalculator` calcule :
  - `delaiReflexionRespecte` = (audienceHomologation - signatureConvention) ≥ 90 jours
  - `conventionComplete` = les 4 volets sont à true
  - `verdict` ∈ {RECEVABLE, IRRECEVABLE, NON_CONCERNE}
  - Liste de motifs d'irrecevabilité si applicable
- Persistance 1:1 dans `divorce_dc_be_analyses`
- GET → 200 ou 404 si jamais POST

## Cas d'erreur
- 400 si dateSignatureConvention future, audienceHomologation < signature, paramètres null
- 400 si workspace.country ≠ BELGIQUE (outil BE-only)
- 400 si caseFile.legalDomain ≠ DROIT_FAMILLE
- 404 si caseFile inaccessible (isolation workspace)

## Critères d'acceptation vérifiables
- [x] POST nominal recevable retourne verdict=RECEVABLE
- [x] POST avec convention incomplète retourne IRRECEVABLE + motifs
- [x] POST avec délai réflexion < 90j retourne IRRECEVABLE
- [x] POST sans consentement des 2 époux retourne IRRECEVABLE
- [x] POST sur workspace FR retourne 400
- [x] POST sur dossier travail retourne 400
- [x] GET sans POST préalable retourne 404
- [x] POST upsert remplace l'analyse précédente
- [x] Isolation workspace

## Plan de test minimal
- **UT** `DivorceDcBeCalculatorTest` : 10+ tests (cas nominal recevable, convention incomplète chacun des 4 volets, délai réflexion ok/court, sans consentement, dates invalides, audience future)
- **IT** non requis dans cette SF (pattern F-208 — sera livré quand frontend en place)

## Tables / endpoints / composants impactés
- **Nouvelle table** `divorce_dc_be_analyses` (id UUID, case_file_id UUID UNIQUE, date_signature_convention DATE NOT NULL, date_audience_homologation DATE, convention_logement BOOLEAN NOT NULL, convention_biens BOOLEAN NOT NULL, convention_garde_enfants BOOLEAN NOT NULL, convention_contributions BOOLEAN NOT NULL, enfants_mineurs_communs BOOLEAN NOT NULL DEFAULT false, epoux_consentent BOOLEAN NOT NULL, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, created_at/updated_at TIMESTAMP)
- **Migration Liquibase** `224-create-divorce-dc-be-analyses.xml` (table uniquement — pas de seed visibility dans cette SF, voir hors périmètre)
- **Endpoint** `DivorceDcBeController` (POST, GET)

## Hors périmètre
- Composant Angular (SF F-211-XX-frontend ultérieure)
- Seed `decision_tool_visibility_rules` (différé : sans entrée `TOOL_REGISTRY` frontend, le garde-fou `DecisionToolVisibilityIntegrityIT` échouerait. Seed livré en SF jumelée avec frontend wrapper)
- Pré-fill IA / validation F-IA-03 (couverte par SF future avec frontend)

## Impact par domaine métier
**Sensible Famille BE uniquement.** Concept FR équivalent = divorce par consentement mutuel français (art. 229-1+ CC, déjudiciarisé depuis 2017 — outil F-FA-05/06 distinct). La procédure BE reste judiciaire (homologation par le tribunal de la famille). Aucun impact Travail / Immigration.

## Parité des domaines métier
Niveau 5 (analyse validité — verdict RECEVABLE/IRRECEVABLE/NON_CONCERNE).
- **FR×Famille** : F-FA-05 (Divorce — étapes générales) couvre la procédure FR — pas d'équivalent strict, déjudiciarisation 2017. Pas de feature jumelle requise.
- **Travail/Immigration** : concept non pertinent.

## Analyse de cohérence transversale
- **Autres outils décisionnels Famille BE** : avant F-211, 1 seul outil disponible (audit 2026-05-11). DC-BE comble une lacune majeure.
- **Pattern réutilisé** : copie pattern `JldRetentionCalculator` + service + controller — entity 1:1 case_file.
- **Pas de nouveau pattern UI / service partagé** : pure réutilisation backend.

## Audit "Impact F-166 cross-C×D"
- **FR×Famille** : non concerné (différé / déjudiciarisation).
- **BE×Famille** : nouvel outil contextuel candidat — seed visibility différé à SF frontend ultérieure (mode `CONTEXTUAL`, trigger `divorce_dc_envisage=true` selon flag F-202 existant).
- **BE×Immigration / BE×Travail / FR×Immigration / FR×Travail** : non concernés.

## Audit "exhaustivité droit national BE"
- Source juridique : Code judiciaire belge art. 1287-1304 (procédure DC) + Loi du 27/04/2007 réformant le divorce + CC art. 229 §1.
- Convention préalable obligatoire — art. 1287 CJ : règlement de tous les aspects (logement, biens, enfants — garde, hébergement, contribution alimentaire).
- Délai minimum de 3 mois entre dépôt et homologation (art. 1294 CJ).
- 2 audiences en principe (sauf accord pour audience unique).
- Équivalent FR (article 229-1+ CC) : déjudiciarisé depuis 2017 — procédure différente.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/divorce-dc-be-analysis`
```json
{
  "dateSignatureConvention": "2026-01-15",
  "dateAudienceHomologation": "2026-05-15",
  "conventionLogement": true,
  "conventionBiens": true,
  "conventionGardeEnfants": true,
  "conventionContributions": true,
  "enfantsMineursCommuns": true,
  "epouxConsentent": true
}
```
Réponse 200 :
```json
{
  "caseFileId": "...",
  "country": "BELGIQUE",
  "dateSignatureConvention": "2026-01-15",
  "dateAudienceHomologation": "2026-05-15",
  "delaiReflexionJours": 120,
  "delaiReflexionRespecte": true,
  "conventionComplete": true,
  "epouxConsentent": true,
  "verdict": "RECEVABLE",
  "motifsIrrecevabilite": [],
  "formule": "Divorce par consentement mutuel — convention signée le ...",
  "baseJuridique": "CJ art. 1287-1304, Loi 27/04/2007, CC art. 229 §1",
  "messages": ["..."]
}
```
Erreurs : 400 (validation, pays, domaine), 404 (dossier non trouvé / isolation).

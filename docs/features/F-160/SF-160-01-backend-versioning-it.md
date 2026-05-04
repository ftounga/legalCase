# SF-160-01 — Backend versioning checklists + questions (intégration test)

## Constat

Après lecture du code, l'infrastructure backend pour exposer les checklists procédurales (F-96 `ProcedureCheck`) et questions IA (F-13/F-14 `AiQuestion`) **par version d'analyse est déjà en place** :

- `ProcedureCheck` est stocké avec `case_analysis_id` (clé étrangère vers `case_analyses`)
- `AiQuestion` est stocké avec `case_analysis_id` (idem)
- `GET /api/v1/case-files/{caseFileId}/case-analysis/versions` → liste toutes les versions
- `GET /api/v1/case-files/{caseFileId}/case-analysis/versions/{n}` → version n
- `GET /api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks` → checks d'une analyse donnée
- `GET /api/v1/case-files/{caseFileId}/ai-questions?analysisId=...` → questions d'une analyse donnée

**Aucun changement de code applicatif nécessaire**. L'objectif de cette SF est de **figer la garantie** que ces endpoints continuent d'accepter un `analysisId` quelconque (pas uniquement la dernière) et que la chaîne entière reste consistante en ajoutant un test d'intégration qui crée 2 versions d'analyse et vérifie qu'on peut récupérer leurs checklists + questions distinctes.

## Comportement à garantir

1. Pour un `caseFile` avec ≥ 2 `case_analyses` (versions enrichies successives) :
   - `GET /versions` retourne les 2 versions triées par numéro descendant.
   - `GET /versions/1` et `/versions/2` retournent chacune la bonne `id` d'analyse.
   - `GET /analyses/{idV1}/procedure-checks` retourne les checks de la v1.
   - `GET /analyses/{idV2}/procedure-checks` retourne les checks de la v2 (différents de v1).
   - `GET /ai-questions?analysisId={idV1}` retourne les questions de la v1.
   - `GET /ai-questions?analysisId={idV2}` retourne les questions de la v2 (différentes de v1).

## Critères d'acceptation

- [x] Test d'intégration `SynthesisVersioningIT` créé : crée un workspace + caseFile + 2 versions d'analyse + 2 checks + 2 questions par version, et vérifie le déterminisme des fetches par version.
- [x] Test passe.
- [x] Aucune migration DB.
- [x] Aucune modif de service/contrôleur applicatif.

## Plan de test

- 1 nouveau IT : `SynthesisVersioningIT` (Spring Boot + MockMvc).

## Hors périmètre

- Modification du DTO `CaseAnalysisResponse` pour inclure les checklists/questions inline → **NON** : on garde l'architecture en endpoints séparés (procedure-checks et ai-questions sont déjà des resources REST indépendantes, pas besoin de duplication).
- Frontend paginator → SF-160-02.
- Déduplication sémantique des questions au moment de la regénération → optionnelle, traitée plus tard si signal terrain.

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune.
- **Nouveau pattern** : aucun.
- **Impact par domaine métier** : transversal, infrastructure d'historisation.

## Contrat API

Aucun nouvel endpoint. Les endpoints existants sont validés tels quels :
- `GET /api/v1/case-files/{caseFileId}/case-analysis/versions`
- `GET /api/v1/case-files/{caseFileId}/case-analysis/versions/{n}`
- `GET /api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks`
- `GET /api/v1/case-files/{caseFileId}/ai-questions?analysisId={id}`

# Mini-spec — F-193 / SF-193-01 Backend — Matérialisation des points procéduraux F-96 (statuts avocat) au run Synthèse enrichie

## Identifiant

`F-193 / SF-193-01`

## Feature parente

`F-193` — Matérialisation des points procéduraux F-96 vers outils décisionnels + dashboard + pieces/délais + PDF

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-193-01-backend-procedure-checks-alignment`

## Pattern de référence

**SF-192-01-backend.md** (F-192 mergée 2026-05-06, PR #861) — cette SF en est le **jumeau procédural** sur le bloc Checklist procédurale F-96. Lire SF-192-01 pour le pattern complet (gating Synthèse enrichie strict, fail-open, idempotence, isolation workspace).

---

## Objectif

Étendre le pattern F-192 au bloc Checklist procédurale F-96 : `ProcedureCheckService.updateStatus` reste un PUT pur, tous les effets se déclenchent au run Synthèse enrichie (matérialisation alignement, propagation pieces/délais, tile dashboard).

---

## Modèle d'activation — gating Synthèse enrichie strict

Strictement aligné F-192. `ProcedureCheckService.updateStatus(checkId, statut, ...)` reste un PUT pur, **strictement inchangé**. Tous les effets F-193 se déclenchent uniquement au prochain run de Synthèse enrichie via un nouveau hook `materializeForAnalysis(newAnalysis)` appelé depuis `EnrichedAnalysisService.run` à la fin (après `propagateRetainedAndDiscarded` ET après `RetainedPisteAlignmentService.materializeForAnalysis` F-192).

---

## Comportement attendu

### Cas nominal

1. L'avocat tag des checks via `PUT /api/v1/case-files/{id}/procedure-checks/{checkId}` (endpoint F-96 existant — **strictement inchangé, aucun side-effect ajouté**).
2. L'avocat clique « Synthèse enrichie ». `EnrichedAnalysisService.run` exécute son flow existant. À la **fin** du run, après commit de la nouvelle analyse, `ProcedureCheckAlignmentService.materializeForAnalysis(newAnalysis)` :
   - Calcule l'alignement de chaque check VERIFIED/NON_COMPLIANT/TO_CHECK propagé sur la nouvelle analyse contre les outils décisionnels via mapping `critereCode → toolId` (cf. ci-dessous), persiste sur la `CaseAnalysis` (champ `procedure_checks_alignment_json`)
   - Insère dans `pieces_manquantes` de la nouvelle analyse les libellés de checks NON_COMPLIANT non déjà présents (source = `PROCEDURE_CHECK_NON_COMPLIANT`)
   - Insère dans `case_deadlines` du dossier les délais inférables des checks TO_CHECK (mapping `critereCode → délai légal` via referentiel optionnel ; si pas de délai connu, pas de délai créé — fail-open)
3. Le frontend lit l'état via `GET /api/v1/case-files/{id}/procedure-checks-alignment` (lecture pure sur la dernière `CaseAnalysis` DONE).
4. `CaseFileDashboardService.assembleTiles` ajoute la tile `F-193-procedure-checks-summary` thème **DELAIS** :
   - `primaryValue` = N total checks (VERIFIED + NON_COMPLIANT + TO_CHECK confondus)
   - `secondaryValue` = "X non conformes · Y à vérifier" (si applicable)
   - `alertLevel` : `ALERT` si ≥ 1 NON_COMPLIANT, `WARNING` si 0 NON_COMPLIANT mais ≥ 1 TO_CHECK, `OK` si tous VERIFIED
5. Fail-open partout (cf. F-192).

### Mapping `critereCode → toolId`

Statique en code Java dans nouveau `ProcedureCheckToolMatcher`. Couverture V1 (transversale 3 domaines) :

**Travail FR/BE** :
- `LICENCIEMENT_NOTIFICATION`, `LICENCIEMENT_ENTRETIEN`, `LICENCIEMENT_DELAI`, `LICENCIEMENT_MOTIF` → `F-DT-08-validite-licenciement`
- `RUPTURE_CONV_FORMULAIRE`, `RUPTURE_CONV_DELAI` → `F-DT-10-rupture-conventionnelle`
- `HARCELEMENT_PREUVE` → `F-DT-12-harcelement-licenciement-nul` (FR) / équivalent BE
- `INDEMNITE_LEGALE_VERSEE`, `INDEMNITE_PREAVIS_VERSEE` → `F-DT-09-comparateur-indemnites`

**Immigration FR/BE** :
- `IM05_MOTIF`, `IM05_NATIONALITE_UE`, `IM05_FAMILLE_PROCHE_FR` → `F-IM-05-arbre-decisionnel-titre`
- `IM06_DELAI_RECOURS`, `IM06_TYPE_DECISION` → `F-IM-06-recours`
- `IM07_TITRE_OUVRE_TRAVAIL` → `F-IM-07-droit-au-travail`

**Famille FR/BE** :
- `DIVORCE_TYPE`, `DIVORCE_TENTATIVE_CONCILIATION` → `F-FA-07-checklist-divorce`
- `PARTAGE_TYPE`, `INDIVISION_DUREE` → `F-FA-05-partage-immobilier`
- `GARDE_DECISION_JAF` → `F-FA-06-calendrier-garde`

Tout `critereCode` non mappé → `toolIdCible = null` et `matchStatus = NO_TARGET_TOOL` (fail-open).

### Définition `matchStatus` (calculé à la matérialisation)

Pour chaque check propagé sur la nouvelle analyse :
- `VERIFIED` (statut F-96) + `expectedValue` matche le champ correspondant dans la sortie outil → `matchStatus = ALIGNED`
- `VERIFIED` mais `expectedValue` diverge → `matchStatus = DIVERGENT` (alerte interne — devrait remonter F-IA-03 mais signal positif aussi côté F-193)
- `NON_COMPLIANT` → `matchStatus = NON_COMPLIANT_FLAG` (signal d'alerte vers l'outil cible — différent du DIVERGENT F-192)
- `TO_CHECK` → `matchStatus = TO_VERIFY_FLAG` (signal d'incertitude vers l'outil cible)
- Outil cible non analysé sur le dossier → `matchStatus = NOT_ANALYZED`
- Pas de mapping → `matchStatus = NO_TARGET_TOOL`

### Cas d'erreur

Identiques à SF-192-01 (cf. mini-spec F-192).

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : V1 = mapping transversal Travail FR/BE + Immigration FR/BE + Famille FR/BE (plus large que F-192 V1 qui était Immigration only — c'est cohérent car les `critereCode` sont déjà transversaux dans F-96).
- [x] **Autres pays** : FR + BE déjà couverts via le mapping (les `critereCode` sont identiques sur les 2 pays pour la plupart, les outils cibles diffèrent).
- [x] **Autres flows transversaux** : étend `EnrichedAnalysisService.run` (hook après F-192). Réutilise infrastructure F-167 (DashboardTile), F-IA-04 (TOOL_REGISTRY), F-IA-03 (alertes coherence sur même `critereCode`).

### Pattern partagé avec F-192

- `materializeForAnalysis(newAnalysis)` — pattern miroir
- `getForLatestAnalysis(caseFileId)` — pattern miroir
- Tile dashboard F-167 — nouveau mapper miroir
- Propagation pieces_manquantes / case_deadlines — nouveau enum value
- Fail-open + idempotence — règles identiques

### Décision

- [x] Étendu transversalement aux 3 domaines × 2 pays V1 (mapping `critereCode → toolId` plus riche que F-192 car les critereCode sont déjà transversaux)
- [x] SF parallèles : SF-193-02 frontend + SF-193-03 PDF
- [x] Aucune cible reportée V2 (couverture intégrale V1)

---

## Critères d'acceptation

- [ ] **CA-01 PUT statut sans side-effect** : `PUT /procedure-checks/{id}` reste un PUT pur — aucune entrée créée dans `pieces_manquantes`/`case_deadlines`, aucune tile dashboard ne change, aucun alignement n'est calculé. Test régression IT obligatoire (cohérence F-96 stricte).
- [ ] **CA-02 matérialisation au run synthèse enrichie** : après `EnrichedAnalysisService.run` avec ≥ 1 check (toutes statuts confondus), la nouvelle `CaseAnalysis` porte un `procedure_checks_alignment_json` non vide
- [ ] **CA-03 endpoint lecture pure** : `GET /api/v1/case-files/{id}/procedure-checks-alignment` retourne 200 + JSON tableau lisant directement la dernière `CaseAnalysis` DONE
- [ ] **CA-04 matching ALIGNED** : check VERIFIED `critereCode = "IM05_MOTIF"` `expectedValue = "TRAVAIL"` + outil F-IM-05 retourne `motif = TRAVAIL` → `matchStatus = ALIGNED`
- [ ] **CA-05 matching NON_COMPLIANT_FLAG** : check NON_COMPLIANT `critereCode = "LICENCIEMENT_NOTIFICATION"` → `toolIdCible = "F-DT-08-validite-licenciement"`, `matchStatus = NON_COMPLIANT_FLAG`
- [ ] **CA-06 matching TO_VERIFY_FLAG** : check TO_CHECK → `matchStatus = TO_VERIFY_FLAG`
- [ ] **CA-07 matching NO_TARGET_TOOL** : `critereCode` inconnu → `toolIdCible = null`, `matchStatus = NO_TARGET_TOOL`
- [ ] **CA-08 tile dashboard** : tile `F-193-procedure-checks-summary` thème DELAIS apparaît quand ≥ 1 check matérialisé. alertLevel correctement dérivé.
- [ ] **CA-09 propagation pieces** : check NON_COMPLIANT « notification écrite manquante » → `pieces_manquantes` source `PROCEDURE_CHECK_NON_COMPLIANT` (idempotent)
- [ ] **CA-10 propagation délais** (optionnel V1) : check TO_CHECK avec délai légal connu (via referentiel statique en code) → `case_deadlines` source `PROCEDURE_CHECK_TO_CHECK`
- [ ] **CA-11 idempotence** : 2 runs successifs avec mêmes checks → pas de doublons
- [ ] **CA-12 erreur 404 isolation workspace** : autre workspace → 404 camouflage
- [ ] **CA-13 fail-open matching** : exception lecture *Analysis → `matchStatus = NOT_ANALYZED`, run réussit
- [ ] **CA-14 fail-open propagation** : exception propagation pieces/délais → run réussit + log warn
- [ ] **CA-15 cohérence F-96 strict** : `ProcedureCheckService.updateStatus` strictement inchangé (test régression `ProcedureCheckServiceIT`)

---

## Périmètre

### Hors scope (explicite)

- (a) Refonte de la UI bloc Checklist procédurale (V1 : afficher juste l'alignement sur la card outil + bloc sortie outil)
- (b) Mapping `critereCode` configurable en DB (V1 = code Java statique)
- (c) Notification email sur NON_COMPLIANT
- (d) Mapping délais légaux exhaustif (V1 = sous-ensemble courant ; étendre selon retour terrain)

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/procedure-checks-alignment` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | ALTER + UPDATE | Nouvelle colonne `procedure_checks_alignment_json TEXT NULL` |
| `procedure_checks` | SELECT | Lecture des checks propagés sur la nouvelle analyse (F-96 inchangée) |
| `pieces_manquantes` | INSERT (idempotent) | Nouvelle source enum `PROCEDURE_CHECK_NON_COMPLIANT` |
| `case_deadlines` | INSERT (idempotent) | Nouvelle source enum `PROCEDURE_CHECK_TO_CHECK` |

### Migration Liquibase

- `XXX-f193-procedure-checks-materialization.xml` :
  - Colonne `case_analyses.procedure_checks_alignment_json TEXT NULL`
  - Élargir contraintes `pieces_manquantes.source` + `case_deadlines.source` pour `PROCEDURE_CHECK_NON_COMPLIANT` + `PROCEDURE_CHECK_TO_CHECK`

### Composants Spring Boot

- `ProcedureCheckAlignmentService` (nouveau) — `materializeForAnalysis(CaseAnalysis newAnalysis)` + `getForLatestAnalysis(caseFileId)`
- `ProcedureCheckToolMatcher` (nouveau) — mapping statique `critereCode → toolId`
- `ProcedureCheckAlignmentController` (nouveau) — endpoint GET
- `ProcedureCheckAlignmentResponse` + `ProcedureCheckAlignment` (records DTO)
- `CaseFileDashboardService.assembleTiles` (étendu) — tile `F-193-procedure-checks-summary` thème DELAIS
- `ProcedureCheckService.updateStatus` — **strictement inchangé**
- `EnrichedAnalysisService.run` (étendu) — appel `materializeForAnalysis` à la fin, après `RetainedPisteAlignmentService.materializeForAnalysis` F-192

---

## Plan de test

### Tests unitaires (12-14 UT)

- `ProcedureCheckToolMatcherTest` — match exact `IM05_MOTIF` → F-IM-05 ; `LICENCIEMENT_NOTIFICATION` → F-DT-08 ; `DIVORCE_TYPE` → F-FA-07 ; etc. + sans match → null
- `ProcedureCheckAlignmentServiceTest` — `materializeForAnalysis` ALIGNED/DIVERGENT/NON_COMPLIANT_FLAG/TO_VERIFY_FLAG/NOT_ANALYZED/NO_TARGET_TOOL ; idempotence pieces/délais ; fail-open matching/propagation
- `CaseFileDashboardServiceTest` extension — pas de checks → pas de tile ; mix statuts → alertLevel correct ; primaryValue/secondaryValue corrects
- `ProcedureCheckServiceTest` — régression PUT pur (cohérence F-96 strict)

### Tests d'intégration (8-10 IT)

- `ProcedureCheckAlignmentControllerIT` — 200 / 200+`[]` legacy / 404 autre workspace / 401 unauthenticated
- `EnrichedAnalysisServiceIT` — run avec checks → matérialisation correcte ; 2 runs → pas de doublons ; exception matérialisation → run réussit
- `ProcedureCheckServiceIT` — PUT statut ne déclenche AUCUNE mutation pieces_manquantes/case_deadlines (régression)

### Isolation workspace

- [x] IT explicite : workspace A ne peut pas lire l'alignement de dossier workspace B (404 camouflage)

---

## Dépendances

### Subfeatures bloquantes

- F-96 ✅ Terminée (procedure_checks + endpoint statut)
- F-192 SF-192-01 ✅ Terminée (pattern de référence + colonnes enum partagées sur pieces_manquantes/case_deadlines)
- F-167 ✅ Terminée (DashboardTile)

### Pattern de référence

`docs/features/F-192/SF-192-01-backend.md` — lire pour le pattern complet (gating, fail-open, idempotence, isolation workspace, hook EnrichedAnalysisService).

---

## Impact par domaine métier

V1 = transversal 3 domaines × 2 pays via mapping `critereCode → toolId`. Les `critereCode` sont déjà transversaux dans F-96, donc le mapping couvre nativement Travail/Immigration/Famille FR+BE.

---

## Notes et décisions

- **Décision 2026-05-06** : V1 transversal contrairement à F-192 V1 (Immigration only) — justifié car les `critereCode` F-96 sont déjà multi-domaines.
- **Décision 2026-05-06** : pas de rollback automatique des pieces/délais quand un check repasse de NON_COMPLIANT → VERIFIED (cohérent F-192).
- **Décision 2026-05-06** : `matchStatus` plus riche que F-192 (NON_COMPLIANT_FLAG + TO_VERIFY_FLAG + ALIGNED + DIVERGENT + NOT_ANALYZED + NO_TARGET_TOOL = 6 valeurs vs 4 pour F-192) car les statuts F-96 sont eux-mêmes plus riches (3 statuts vs trichotomie classique TO_STUDY/RETAINED/DISCARDED).
- **Décision 2026-05-06** : tile dashboard thème DELAIS plutôt que DIAGNOSTIC (F-192) — les vérifications procédurales relèvent plus des délais que du diagnostic.
- **Décision 2026-05-06** : hook appelé APRÈS F-192 dans `EnrichedAnalysisService.run` — ordre cohérent avec l'ordre de livraison (F-192 d'abord, F-193 ensuite) ; pas de dépendance technique entre les 2 hooks (chacun fail-open).

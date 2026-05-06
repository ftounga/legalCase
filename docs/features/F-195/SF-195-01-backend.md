# Mini-spec — F-195 / SF-195-01 Backend — Risques markables + recompute riskScore + matérialisation au run enrichi

## Identifiant

`F-195 / SF-195-01`

## Statut

`draft` — 2026-05-06

## Branche Git

`feat/SF-195-01-backend-risques-markables`

## Pattern de référence

**SF-194-01-backend.md** (en cours) — pattern strictement aligné. F-195 réplique sur le bloc `risques`.

---

## Objectif

(1) Passer le bloc `risques` (aujourd'hui en lecture seule, persisté JSON `analysis_result`) en **markable** : trichotomie `À_CREUSER` / `VALIDÉ` / `ÉCARTÉ` (+ raison_ecarte). (2) Pattern F-192/F-193/F-194 + recompute `score_risque` excluant les ÉCARTÉ + propagation visibility outils (risque VALIDÉ « harcèlement avéré » → pré-flag F-DT-12 ; risque ÉCARTÉ « clause non-concurrence abusive » → masque F-DT-24 via F-IA-04).

---

## Modèle d'activation

Strictement aligné F-192/F-193/F-194. PUT statut risque = persistance pure. Effets matérialisés au run de Synthèse enrichie via `materializeForAnalysis(newAnalysis)` appelé en fin de `EnrichedAnalysisService.run` APRÈS hooks F-192, F-193, F-194.

---

## Architecture

**Option A retenue** (cohérent F-194) : nouvelle table `risque_status` (overlay sur JSON `risques`), match libellé normalisé.

```
risque_status
- id UUID PK
- case_file_id UUID NOT NULL FK
- workspace_id UUID NOT NULL FK (isolation stricte)
- risque_libelle_normalise VARCHAR(500) NOT NULL
- risque_libelle_original VARCHAR(500) NOT NULL
- statut VARCHAR(20) NOT NULL  -- A_CREUSER (default implicite) / VALIDE / ECARTE
- raison_ecarte TEXT NULL
- created_at, updated_at TIMESTAMP NOT NULL
- UNIQUE (case_file_id, risque_libelle_normalise)
```

---

## Comportement attendu

### Cas nominal

1. **PUT statut pur** : `PUT /api/v1/case-files/{id}/risques/status` body `{ risqueLibelleOriginal, statut, raisonEcarte? }` → upsert dans `risque_status`. **Aucun side-effect**.
2. **Matérialisation au run synthèse enrichie** :
   - `RisqueAlignmentService.materializeForAnalysis(newAnalysis)` :
     - Lit `risques` JSON de la nouvelle analyse
     - Pour chaque risque, joint statut depuis `risque_status` via libellé normalisé
     - Sérialise alignement dans nouvelle colonne `case_analyses.risques_alignment_json`
     - **Recompute `score_risque`** : exclut les risques ÉCARTÉ du calcul (le score initial vient de l'IA — F-195 produit un `score_risque_avocat` séparé, dans `case_analyses.score_risque_avocat_json` ou même JSON `score_risque` étendu)
   - **Mapping risque → toolId** via nouveau `RisqueToolMatcher` (statique, keyword-based) :
     - Travail FR : "harcèlement", "harcèlement moral", "harcèlement sexuel" → flag pour F-DT-12 harcelement-licenciement-nul
     - Travail FR : "discrimination" → flag pour F-DT-13 (si existe) ou F-DT-08 validité licenciement
     - Travail FR : "prescription échue", "délai forclos" → flag pour F-DT-03 prescription
     - Travail FR : "clause non-concurrence abusive" → flag pour F-DT-24 (peut être MASQUÉ si ÉCARTÉ)
     - Immigration : "OQTF imminente", "perte titre", "expulsion" → flag F-IM-08, F-IM-20
     - Famille : "violence intra-familiale" → flag F-FA-14 ordonnance protection ; "déplacement illicite enfant" → flag F-FA-19 ; "dilapidation patrimoine" → flag F-FA-15
3. **Prompt enrichi étendu** : `EnrichedAnalysisService.buildEnrichedPrompt` reçoit aussi les statuts risques et instruit l'IA :
   - `[Risques validés par votre avocat — à approfondir]`
   - `[Risques écartés — NE PAS re-proposer]` (raison incluse si présente)
4. **Tile dashboard** : `F-195-risques-summary` thème **DIAGNOSTIC** (cohérent F-192 — risques relèvent du diagnostic) :
   - `primaryValue` = N total risques
   - `secondaryValue` = "X validés · Y écartés · Z à creuser"
   - `alertLevel` = ALERT si ≥ 1 VALIDÉ avec libellé contenant "harcèlement" / "violence" / "expulsion" / "dilapidation" (tag prioritaire), WARNING sinon, OK si tous écartés
5. **Tile `riskScore` F-IA-02 existante** : étendre pour afficher 2 valeurs `Score IA brut : X · Score validé avocat : Y` quand `score_risque_avocat_json` non null.
6. **Visibility outils via F-IA-04** : risques ÉCARTÉ avec keyword reconnu → ajouter dans la condition de masquage du tool concerné (extension du moteur F-IA-04 — à investiguer dans le dev pour le bon hook).

### Cas d'erreur

Identiques F-194 (404 isolation, 400 statut invalide, fail-open matérialisation).

---

## Critères d'acceptation

- [ ] **CA-01 PUT statut pur** : aucune mutation `risques` JSON ni `score_risque`
- [ ] **CA-02 matérialisation** : nouveau `case_analyses.risques_alignment_json` après run enrichi
- [ ] **CA-03 endpoint lecture pure** : `GET /risques-alignment` 200
- [ ] **CA-04 score recomputé** : `score_risque_avocat` calculé en excluant ÉCARTÉ, persisté
- [ ] **CA-05 mapping ALIGNED** : risque "harcèlement moral subi" VALIDÉ → toolIdsCibles inclut F-DT-12
- [ ] **CA-06 mapping NO_TARGET_TOOL** : risque sans keyword reconnu → toolIdsCibles vide
- [ ] **CA-07 tile dashboard** : `F-195-risques-summary` thème DIAGNOSTIC, alertLevel correct
- [ ] **CA-08 prompt enrichi** : sections [Risques validés] + [Risques écartés] présentes
- [ ] **CA-09 isolation workspace** : autre workspace → 404
- [ ] **CA-10 fail-open matérialisation** : exception → run réussit + log warn
- [ ] **CA-11 cohérence F-IA-02 strict** : tile `riskScore` originale (F-IA-02) reste fonctionnelle ; F-195 ajoute juste `score_risque_avocat` à côté
- [ ] **CA-12 visibility F-IA-04** (option) : risque ÉCARTÉ "clause non-concurrence abusive" → F-DT-24 masqué dans le panel pour ce dossier
- [ ] **CA-13 régression** : `analysis_result.risques` JSON non modifié par F-195 (test régression)

---

## Hors scope V1

- (a) Matching fuzzy entre libellés (V1 = match exact normalisé)
- (b) Auto-tag VALIDÉ basé sur sémantique (V1 = curation manuelle uniquement)
- (c) Modification du calcul `score_risque` IA brut (V1 = IA inchangée, F-195 produit un score parallèle)
- (d) Notification email "risque critique validé"

---

## Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `risque_status` | CREATE | Nouvelle table |
| `case_analyses` | ALTER | Nouvelle colonne `risques_alignment_json TEXT NULL` + colonne `score_risque_avocat_json TEXT NULL` |

### Migration : `XXX-f195-risques-markables.xml`

### Composants Spring Boot

- `RisqueStatus` (entité), `RisqueStatusRepository`, `RisqueStatusService` (CRUD upsert + collectForEnrichment)
- `RisqueToolMatcher` (statique, keyword-based)
- `RisqueAlignmentService` (`materializeForAnalysis` + `getForLatestAnalysis`)
- `RisqueController` (PUT + GET)
- `CaseFileDashboardService.assembleTiles` extension — tile `F-195-risques-summary` thème DIAGNOSTIC
- `EnrichedAnalysisService` extension — `buildEnrichedPrompt` (sections risques) + `run` (hook matérialisation après F-192/193/194)
- Extension `RiskScoreService` (ou équivalent F-IA-02) pour calcul `score_risque_avocat` excluant ÉCARTÉ — à investiguer dans le dev pour le bon point d'extension
- Optionnel : extension `DecisionToolVisibilityService` (F-IA-04) pour masquer outils basé sur risques ÉCARTÉ — vérifier si pertinent V1 ou différer V2

---

## Plan de test

### UT (~10-12)

- `RisqueToolMatcherTest` — keyword match harcèlement/discrimination/violence/expulsion + sans match
- `RisqueAlignmentServiceTest` — matérialisation + recompute score + idempotence + fail-open
- `RisqueStatusServiceTest` — upsert idempotent + validation
- `CaseFileDashboardServiceTest` extension — tile alertLevel selon mix
- `EnrichedAnalysisServiceTest` — prompt enrichi sections correctes

### IT (~8)

- `RisqueControllerIT` — PUT/GET + 404 isolation + 401 + 400 statut invalide
- `EnrichedAnalysisServiceIT` — run avec risques statuts → matérialisation + score recomputé + 2 runs idempotents
- `RisqueStatusServiceIT` — PUT statut sans mutation `risques` JSON (régression)

---

## Dépendances

- F-192/F-193/F-194 SF-XX-01 backends mergés
- F-IA-02 (riskScore) ✅
- F-IA-04 (visibility) ✅
- F-167 ✅

---

## Notes et décisions

- **2026-05-06** : tile dashboard thème DIAGNOSTIC (cohérent F-192 — les risques relèvent du diagnostic, pas des délais ou documents)
- **2026-05-06** : `score_risque_avocat` calculé séparément, n'écrase pas le score IA brut — l'avocat voit les 2 (transparence)
- **2026-05-06** : V1 keyword-based mapping risque → toolId. Rapide à implémenter, suffisant pour les keywords clés. V2 si besoin = embedding match
- **2026-05-06** : visibility outils via F-IA-04 (extension moteur) — option V1 si simple à brancher, sinon V2. À trancher en dev

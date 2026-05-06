# Mini-spec — F-196 / SF-196-01 Backend — Matérialisation des questions complémentaires F-94 (réponses avocat) au run enrichi

## Identifiant

`F-196 / SF-196-01`

## Statut

`draft` — 2026-05-06

## Branche Git

`feat/SF-196-01-backend-questions-alignment`

## Pattern de référence

**SF-192-01 + SF-193-01** mergées. F-196 réplique sur le bloc questions complémentaires F-94 mais avec **gap réduit** car F-IA-03 fait déjà 60% du travail (alertes cohérence quand réponse contradicte champ outil).

---

## Objectif

Étendre le pattern F-192/F-193 sur les questions complémentaires F-94 (déjà markables avec réponses avocat). Manque : (1) tile dashboard, (2) propagation auto pieces_manquantes (réponses « oui »/« non » sur questions du type « avez-vous le document X ? » → pieces obtenue/manquante auto), (3) section PDF.

`AiQuestionAnswerService` (existant F-94) **strictement inchangé** — la PUT réponse reste pure.

---

## Comportement attendu

### Cas nominal

1. **PUT réponse pur** : F-94 endpoint inchangé.
2. **Matérialisation au run synthèse enrichie** :
   - `AiQuestionAlignmentService.materializeForAnalysis(newAnalysis)` :
     - Lit les réponses avocat de l'analyse (`ai_question_answers` joint à `ai_questions`)
     - Sérialise alignement dans `case_analyses.ai_questions_alignment_json` (mapping `questionId → { answerText, critereCode, expectedValue?, statutDeduction: 'PIECE_OBTENUE' | 'PIECE_MANQUANTE' | 'INFO_ONLY' }`)
     - **Auto-deduction pieces** : pour chaque question contenant « avez-vous » + libellé pièce reconnaissable :
       - Réponse `oui` → ajout libellé à `pieces_manquantes` source `QUESTION_REPONDUE_OUI` SI le libellé n'est pas déjà présent (cas rare — pieces déjà produites par IA généralement)
       - Réponse `non` → ajout libellé à `pieces_manquantes` source `QUESTION_REPONDUE_NON` SI absent
   - **Mapping question → piece** : statique en code (`AiQuestionPieceExtractor`) basé sur regex/keyword sur le `questionText` :
     - "Avez-vous reçu la lettre de licenciement" → libellé "Lettre de licenciement"
     - "Avez-vous le contrat de travail" → "Contrat de travail"
     - "Avez-vous des fiches de paie" → "Fiches de paie"
     - "Avez-vous l'acte de mariage" → "Acte de mariage"
     - etc. (~10-15 patterns courants par domaine)
3. **Tile dashboard** : `F-196-questions-summary` thème **DOCUMENTS** (cohérent F-194 pieces) :
   - `primaryValue` = N total questions
   - `secondaryValue` = "X répondues · Y en attente"
   - `alertLevel` = WARNING si ≥ 1 question en attente, OK sinon

### Cas d'erreur

Identiques F-192/F-193/F-194 (fail-open, isolation workspace).

---

## Critères d'acceptation

- [ ] **CA-01 PUT réponse pur** : `AiQuestionAnswerService` strictement inchangé (test régression)
- [ ] **CA-02 matérialisation** : nouveau `case_analyses.ai_questions_alignment_json` après run enrichi
- [ ] **CA-03 endpoint lecture pure** : `GET /api/v1/case-files/{id}/ai-questions-alignment` 200
- [ ] **CA-04 piece auto OUI** : question "Avez-vous reçu la lettre de licenciement ?" + réponse "oui" → "Lettre de licenciement" ajoutée pieces_manquantes source `QUESTION_REPONDUE_OUI` (si absente)
- [ ] **CA-05 piece auto NON** : réponse "non" → ajout source `QUESTION_REPONDUE_NON`
- [ ] **CA-06 idempotence pieces** : 2 runs successifs → pas de doublon
- [ ] **CA-07 tile dashboard** : tile `F-196-questions-summary` thème DOCUMENTS apparaît
- [ ] **CA-08 isolation workspace** : autre workspace → 404
- [ ] **CA-09 fail-open** : exception → run réussit + log warn
- [ ] **CA-10 pas d'extraction si pas de keyword** : question hors patterns connus → pas de piece créée (fail-open)

---

## Hors scope V1

- (a) Tagging des questions complémentaires (déjà F-94 — V1 lit juste les réponses)
- (b) Mapping fuzzy entre questionText et piece (V1 = regex statique)
- (c) Suppression auto pieces_manquantes quand réponse « oui »
- (d) Section PDF dédiée (cf. SF-196-03 mais low priority — peut être différée)

---

## Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | ALTER | Nouvelle colonne `ai_questions_alignment_json TEXT NULL` |
| `pieces_manquantes` | INSERT (idempotent via libellé normalisé) | Nouvelles sources enum `QUESTION_REPONDUE_OUI` / `QUESTION_REPONDUE_NON` |

### Migration : `XXX-f196-questions-alignment.xml`

### Composants Spring Boot

- `AiQuestionAlignmentService` (nouveau) — `materializeForAnalysis` + `getForLatestAnalysis`
- `AiQuestionPieceExtractor` (nouveau, statique) — regex `questionText → pieceLibelle`
- `AiQuestionAlignmentController` (nouveau) — endpoint GET
- `CaseFileDashboardService.assembleTiles` extension — tile `F-196-questions-summary` thème DOCUMENTS
- `EnrichedAnalysisService.run` — hook après F-192/F-193/F-194/F-195
- `AiQuestionAnswerService` — **strictement inchangé**

---

## Plan de test

### UT (~8)

- `AiQuestionPieceExtractorTest` — patterns "Avez-vous reçu X" → libellé X
- `AiQuestionAlignmentServiceTest` — matérialisation + auto-piece + idempotence + fail-open
- `CaseFileDashboardServiceTest` extension

### IT (~5)

- `AiQuestionAlignmentControllerIT` — 200 / `[]` / 404 / 401
- `EnrichedAnalysisServiceIT` — run avec réponses → auto-piece créée + idempotence
- `AiQuestionAnswerServiceIT` — PUT réponse ne crée AUCUNE pièce (régression cohérence F-94)

---

## Dépendances

- F-94 ✅
- F-IA-03 ✅
- F-192/F-193/F-194 backends ✅ (mergés ou en cours)
- F-167 ✅

---

## Notes 2026-05-06

- **Priorité basse** vs autres F-19X car F-IA-03 fait déjà l'essentiel (alertes cohérence). F-196 n'ajoute que tile + auto-piece + section PDF
- V1 keyword-based extractor — V2 si signal terrain
- Tile thème DOCUMENTS (cohérent — pieces sont impactées en sortie)

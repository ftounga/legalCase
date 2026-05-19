# SF-250-04 — Remédiation critereCode F-IA-03 — lot Travail BE + F-136

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-04-remediation-travail-be`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les 3 `critereCode` attendus par les 2 composants frontend du lot Travail BE + F-136 — déclenchant ainsi le cross-check F-IA-03 actuellement mort pour ces outils.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-DT-29-credit-temps-be` | `credit-temps-be-section` | `CREDIT_TEMPS_ANCIENNETE` | Ancienneté du travailleur identifiée dans les pièces — base d'éligibilité au crédit-temps. Binaire — VERIFIED = ancienneté documentée. **BELGIQUE UNIQUEMENT.** |
| `F-DT-29-credit-temps-be` | `credit-temps-be-section` | `CREDIT_TEMPS_AGE` | Âge du travailleur identifié dans les pièces — paramètre déterminant pour le régime crédit-temps senior. Binaire — VERIFIED = âge documenté. **BELGIQUE UNIQUEMENT.** |
| `F-136-travail-procedure` | `travail-procedure-section` | `TRAVAIL_PROCEDURE_TYPE` | Type de procédure de travail identifié dans les pièces (licenciement collectif, restructuration, fermeture d'entreprise). Binaire — VERIFIED = type documenté. |

Total : 2 outils, 3 codes, tous binaires (pas de `expected_value`).

---

## 3. Comportement nominal

- Lors d'une analyse de dossier droit du travail BELGIQUE, le LLM reçoit maintenant `CREDIT_TEMPS_ANCIENNETE` et `CREDIT_TEMPS_AGE` dans la liste autorisée des `critere_code` de `points_procedure[]`.
- Lors de la génération des questions complémentaires droit du travail BELGIQUE, le LLM reçoit ces 2 codes dans la liste autorisée des `critere_code` des `questions[]`.
- Pour l'outil F-136 (neutre France/Belgique), `TRAVAIL_PROCEDURE_TYPE` est émissible quel que soit le pays.
- Si le dossier contient des pièces relatives à un crédit-temps belge, le LLM émet un ou plusieurs `points_procedure` ou `questions` avec `CREDIT_TEMPS_ANCIENNETE` / `CREDIT_TEMPS_AGE`, `expected_value: null`, et `VERIFIED` / `NON_COMPLIANT` selon les pièces.

## 4. Cas d'erreur

- Les codes `CREDIT_TEMPS_*` ne doivent être émis que pour des dossiers BELGIQUE (annotation « BELGIQUE UNIQUEMENT » dans le prompt).
- Si les pièces ne permettent pas de trancher : le LLM n'émet aucun item avec ces codes (comportement fail-open préexistant).
- En l'absence de signe de crédit-temps dans les pièces, les codes restent absents — aucune régression possible.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (2 nouveaux tests) :
- `systemPrompt_containsCreditTempsCriteraCodes` — le prompt système travail contient `CREDIT_TEMPS_ANCIENNETE`, `CREDIT_TEMPS_AGE` annotés BELGIQUE UNIQUEMENT.
- `systemPrompt_containsTravailProcedureTypeCritereCode` — le prompt contient `TRAVAIL_PROCEDURE_TYPE`.

**AiQuestionServiceTest** (2 nouveaux tests) :
- `systemPrompt_containsCreditTempsCodesForQuestions` — le prompt questions travail contient `CREDIT_TEMPS_ANCIENNETE`, `CREDIT_TEMPS_AGE`.
- `systemPrompt_containsTravailProcedureTypeForQuestions` — le prompt contient `TRAVAIL_PROCEDURE_TYPE`.

### Non-régression
- Tests SF-250-02 et SF-250-03 restent verts (les codes DT36_*, RC_*, INAPT_*, AT_MP_* sont inchangés).

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `questions[].critere_code`)

### Backend (non modifiés)
- `ProcedureCheckService.java` — parseur fail-open, accepte déjà tout code

### Frontend (non modifiés)
- `credit-temps-be-section.component.ts` — attend déjà ces codes
- `travail-procedure-section.component.ts` — attend déjà `TRAVAIL_PROCEDURE_TYPE`

### Tests (ajoutés)
- `CaseAnalysisServiceTest.java` — 2 nouveaux tests
- `AiQuestionServiceTest.java` — 2 nouveaux tests

---

## 7. Hors-périmètre

- Codes du lot SF-250-05 (IM08_*, IM09_*) — SF-250-05.
- Codes du lot SF-250-06 (IM11_*, IM12_*, IM13_*, IM19_*, IM20_*, IM24_*) — SF-250-06.
- Codes du lot SF-250-07 (BE_9TER_*, IM17_*) — SF-250-07.
- Famille FR/BE (SF-250-08 à SF-250-10).
- Garde-fou de gouvernance (SF-250-11).

---

## 8. Référence audit

Tableau SF-250-01 section 3.2 — outils travail-BE ❌ cassés : F-DT-29 (codes CREDIT_TEMPS_*).
Tableau SF-250-01 section 3.1 — F-136 ❌ cassé : TRAVAIL_PROCEDURE_TYPE.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-04.

# SF-250-07 — Remédiation critereCode F-IA-03 — lot Immigration BE

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-07-remediation-immigration-be`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les 6 `critereCode` attendus par les 3 composants frontend du lot Immigration BE — déclenchant ainsi le cross-check F-IA-03 actuellement mort pour ces outils.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-IM-08-annexe13-be` | `annexe13-be-section` | `IM08_MOTIF_OQT_BE` | Motif de l'ordre de quitter le territoire (OQT) belge identifié dans les pièces (absence de titre, menace ordre public, refus de séjour). Binaire — VERIFIED = motif documenté. **BELGIQUE UNIQUEMENT.** |
| `F-IM-14-9ter-medical-be` | `belgian-9ter-section` | `BE_9TER_MALADIE_GRAVE` | Maladie grave identifiée dans les pièces — condition d'accès à la procédure 9ter (séjour pour raisons médicales). Binaire — VERIFIED = maladie documentée. **BELGIQUE UNIQUEMENT.** |
| `F-IM-14-9ter-medical-be` | `belgian-9ter-section` | `BE_9TER_SOINS_BE` | Disponibilité des soins médicaux en Belgique documentée dans les pièces. Binaire — VERIFIED = disponibilité documentée. **BELGIQUE UNIQUEMENT.** |
| `F-IM-14-9ter-medical-be` | `belgian-9ter-section` | `BE_9TER_SOINS_INACCESSIBLES` | Inaccessibilité des soins médicaux dans le pays d'origine documentée dans les pièces — critère central de la procédure 9ter. Binaire — VERIFIED = inaccessibilité documentée. **BELGIQUE UNIQUEMENT.** |
| `F-IM-14-9ter-medical-be` | `belgian-9ter-section` | `BE_9TER_MENACE_ORDRE_PUBLIC` | Absence de menace pour l'ordre public documentée dans les pièces — condition négative de la procédure 9ter. Binaire — VERIFIED = absence de menace documentée. **BELGIQUE UNIQUEMENT.** |
| `F-IM-17-regime-algerien` | `regime-algerien-section` | `IM17_VOIE_REGIME_ALGERIEN` | Voie de l'accord franco-algérien applicable identifiée dans les pièces (carte de résident algérien, certificat de résidence 1 an, etc.). Binaire — VERIFIED = voie documentée. **BELGIQUE UNIQUEMENT** (outil BE). |

Total : 3 outils, 6 codes, tous binaires (pas de `expected_value`).

---

## 3. Comportement nominal

- Lors d'une analyse de dossier droit de l'immigration BELGIQUE, le LLM reçoit maintenant ces 6 codes dans la liste autorisée des `critere_code` de `points_procedure[]`.
- Lors de la génération des questions complémentaires droit de l'immigration BELGIQUE, le LLM reçoit ces codes dans la liste autorisée.
- Tous ces codes sont annotés BELGIQUE UNIQUEMENT dans le prompt.

## 4. Cas d'erreur

- Ces codes ne doivent être émis que pour des dossiers BELGIQUE.
- Fail-open : si les pièces ne permettent pas de trancher, le LLM n'émet aucun item avec ces codes.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (2 nouveaux tests) :
- `systemPrompt_containsIm08OqtBeCritereCode` — le prompt contient `IM08_MOTIF_OQT_BE` annoté BELGIQUE UNIQUEMENT.
- `systemPrompt_containsBe9terAndIm17CriteraCodes` — le prompt contient `BE_9TER_MALADIE_GRAVE`, `BE_9TER_SOINS_BE`, `BE_9TER_SOINS_INACCESSIBLES`, `BE_9TER_MENACE_ORDRE_PUBLIC`, `IM17_VOIE_REGIME_ALGERIEN`.

**AiQuestionServiceTest** (2 nouveaux tests) :
- `systemPrompt_containsIm08OqtBeCodeForQuestions`
- `systemPrompt_containsBe9terAndIm17CodesForQuestions`

### Non-régression
- Tests SF-250-02 à SF-250-06 restent verts.

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `questions[].critere_code`)

### Frontend (non modifiés)
- 3 composants `*-section.component.ts` (annexe13-be, belgian-9ter, regime-algerien)

### Tests (ajoutés)
- `CaseAnalysisServiceTest.java` — 2 nouveaux tests
- `AiQuestionServiceTest.java` — 2 nouveaux tests

---

## 7. Hors-périmètre

- Famille FR/BE (SF-250-08 à SF-250-10).
- Garde-fou de gouvernance (SF-250-11).
- Outils en statut ⚠️ non encore affinés (F-IM-14-40ter, 9bis, 40bis, F-IM-21, F-IM-22, F-IM-23) — SF-250-10.

---

## 8. Référence audit

Tableau SF-250-01 section 3.4 — outils Immigration BE ❌ cassés : F-IM-08-annexe13-be (IM08_MOTIF_OQT_BE), F-IM-14-9ter (BE_9TER_*), F-IM-17 (IM17_VOIE_REGIME_ALGERIEN).
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-07.

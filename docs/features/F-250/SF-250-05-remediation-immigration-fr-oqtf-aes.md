# SF-250-05 — Remédiation critereCode F-IA-03 — lot Immigration FR — OQTF / AES

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-05-remediation-immigration-fr-oqtf-aes`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les 10 `critereCode` attendus par les ~7 composants frontend du lot Immigration FR OQTF/référés/AES — déclenchant ainsi le cross-check F-IA-03 actuellement mort pour ces outils.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-IM-08-oqtf-avec-delai-fr` | `oqtf-avec-delai-section` | `IM08_MOTIF_OQTF` | Motif de l'OQTF identifié dans les pièces (absence titre, menace ordre public, refus séjour). Binaire — VERIFIED = motif documenté. **FRANCE UNIQUEMENT.** |
| `F-IM-08-oqtf-avec-delai-fr` + `F-IM-08-oqtf-sans-delai-fr` | `oqtf-avec-delai-section`, `oqtf-sans-delai-section` | `IM08_RECOURS_FORME` | La forme du recours contre l'OQTF est identifiée dans les pièces (recours gracieux, recours contentieux TA). Binaire — VERIFIED = forme documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-08-referes-admin-fr` | `referes-admin-section` | `IM08RA_DECISION_CONTESTEE` | La décision administrative contestée est identifiée dans les pièces (OQTF, refus, arrêté). Binaire — VERIFIED = décision documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-09-aes-metiers-tension` | `aes-metiers-tension-section` | `IM09_DATE_ENTREE_FRANCE` | Date d'entrée en France identifiée dans les pièces — condition d'éligibilité à l'AES. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-09-aes-metiers-tension` | `aes-metiers-tension-section` | `IM09_MOIS_ACTIVITE` | Nombre de mois d'activité professionnelle identifié dans les pièces — condition AES métiers en tension. Binaire — VERIFIED = donnée documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-09-aes-etudiant` | `aes-etudiant-section` | `IM09_ETU_DATE_ENTREE_FRANCE` | Date d'entrée en France de l'étudiant identifiée dans les pièces. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-09-aes-etudiant` | `aes-etudiant-section` | `IM09_ETU_DUREE_PRESENCE` | Durée de présence en France de l'étudiant identifiée dans les pièces — condition d'éligibilité AES étudiant. Binaire — VERIFIED = donnée documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-09-aes-etudiant` | `aes-etudiant-section` | `IM09_ETU_DATE_DEPOT_DEMANDE` | Date de dépôt de la demande AES étudiant identifiée dans les pièces. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-09-aes-humanitaire` | `aes-humanitaire-section` | `IM09H_DATE_ENTREE_FRANCE` | Date d'entrée en France identifiée dans les pièces — condition AES humanitaire. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-09-aes-humanitaire` | `aes-humanitaire-section` | `IM09H_MOTIF_HUMANITAIRE` | Motif humanitaire identifié dans les pièces (circonstances humanitaires exceptionnelles, liens personnels/familiaux). Binaire — VERIFIED = motif documenté. **FRANCE UNIQUEMENT.** |

Total : ~7 outils, 10 codes, tous binaires (pas de `expected_value`).

---

## 3. Comportement nominal

- Lors d'une analyse de dossier droit de l'immigration FRANCE, le LLM reçoit maintenant ces 10 codes dans la liste autorisée des `critere_code` de `points_procedure[]`.
- Lors de la génération des questions complémentaires droit de l'immigration FRANCE, le LLM reçoit ces codes dans la liste autorisée des `critere_code` des `questions[]`.
- Tous ces codes sont annotés FRANCE UNIQUEMENT dans le prompt.

## 4. Cas d'erreur

- Ces codes ne doivent être émis que pour des dossiers FRANCE (annotation « FRANCE UNIQUEMENT »).
- Fail-open : si les pièces ne permettent pas de trancher, le LLM n'émet aucun item avec ces codes.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsIm08OqtfCriteraCodes` — le prompt contient `IM08_MOTIF_OQTF`, `IM08_RECOURS_FORME`, `IM08RA_DECISION_CONTESTEE` annotés FRANCE UNIQUEMENT.
- `systemPrompt_containsIm09AesMetiersTensionCriteraCodes` — le prompt contient `IM09_DATE_ENTREE_FRANCE`, `IM09_MOIS_ACTIVITE`.
- `systemPrompt_containsIm09EtudiantAndHumanitaireCriteraCodes` — le prompt contient les 5 codes étudiant + humanitaire.

**AiQuestionServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsIm08CodesForQuestions` — le prompt questions contient `IM08_MOTIF_OQTF`, `IM08_RECOURS_FORME`, `IM08RA_DECISION_CONTESTEE`.
- `systemPrompt_containsIm09MetiersCodesForQuestions` — le prompt questions contient `IM09_DATE_ENTREE_FRANCE`, `IM09_MOIS_ACTIVITE`.
- `systemPrompt_containsIm09EtudiantHumanitaireCodesForQuestions` — le prompt contient les 5 codes étudiant + humanitaire.

### Non-régression
- Tests SF-250-02, SF-250-03, SF-250-04 restent verts.
- Les codes IM05_MOTIF, IM06_RECOURS_TYPE, IM07_TITRE_TYPE, IM21_* existants sont inchangés.

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `questions[].critere_code`)

### Frontend (non modifiés)
- 7 composants `*-section.component.ts` (oqtf-avec-delai, oqtf-sans-delai, referes-admin, aes-metiers-tension, aes-etudiant, aes-humanitaire, aes-famille)

### Tests (ajoutés)
- `CaseAnalysisServiceTest.java` — 3 nouveaux tests
- `AiQuestionServiceTest.java` — 3 nouveaux tests

---

## 7. Hors-périmètre

- IM11_*, IM12_*, IM13_*, IM19_*, IM20_*, IM24_* — SF-250-06.
- BE_9TER_*, IM08_*_BE, IM17_* — SF-250-07.
- Famille FR/BE (SF-250-08 à SF-250-10).
- Garde-fou SF-250-11.

---

## 8. Référence audit

Tableau SF-250-01 section 3.3 — outils Immigration FR ❌ cassés : F-IM-08-OQTF×2, referes, AES×3.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-05.

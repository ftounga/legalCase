# SF-250-06 — Remédiation critereCode F-IA-03 — lot Immigration FR — titres / asile / éloignement

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-06-remediation-immigration-fr-titres-asile`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les 8 `critereCode` attendus par les 6 composants frontend du lot Immigration FR titres/asile/éloignement/victimes violences.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-IM-11-changement-statut` | `changement-statut-section` | `IM11_TITRE_ACTUEL` | Titre de séjour actuel identifié dans les pièces — condition préalable à l'analyse du changement de statut. Binaire — VERIFIED = titre documenté. **FRANCE UNIQUEMENT.** |
| `F-IM-12-asile-avance` | `asile-avance-section` | `IM12_DISPOSITIF_ASILE` | Dispositif de protection internationale visé identifié dans les pièces (statut réfugié, protection subsidiaire, apatride). Binaire — VERIFIED = dispositif documenté. **FRANCE UNIQUEMENT.** |
| `F-IM-13-naturalisation` | `naturalisation-section` | `IM13_VOIE_NATURALISATION` | Voie de naturalisation identifiée dans les pièces (décret, mariage avec Français, renonciation à nationalité étrangère). Binaire — VERIFIED = voie documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-19-mineurs` | `mineurs-immigration-section` | `IM19_DATE_NAISSANCE` | Date de naissance du mineur identifiée dans les pièces — condition déterminante pour les droits au séjour. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-19-mineurs` | `mineurs-immigration-section` | `IM19_DATE_ENTREE` | Date d'entrée en France du mineur identifiée dans les pièces. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-IM-20-mesures-eloignement` | `mesures-eloignement-section` | `IM20_DISPOSITIF_ELOIGNEMENT` | Dispositif d'éloignement identifié dans les pièces (OQTF, ITF, reconduite à la frontière, expulsion). Binaire — VERIFIED = dispositif documenté. **FRANCE UNIQUEMENT.** |
| `F-IM-20-mesures-eloignement` | `mesures-eloignement-section` | `IM20_MOTIF_MENACE` | Motif de menace à l'ordre public ou à la sécurité publique identifié dans les pièces — fondement de la mesure d'éloignement. Binaire — VERIFIED = motif documenté. **FRANCE UNIQUEMENT.** |
| `F-IM-24-victime-violences-l4256-fr` | `victime-violences-section` | `IM24_DATE_ORDONNANCE_PROTECTION` | Date de l'ordonnance de protection identifiée dans les pièces — ouvre droit au titre de séjour pour victime de violences. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |

Total : 6 outils, 8 codes, tous binaires (pas de `expected_value`).

---

## 3. Comportement nominal

- Lors d'une analyse de dossier droit de l'immigration FRANCE, le LLM reçoit maintenant ces 8 codes dans la liste autorisée des `critere_code` de `points_procedure[]`.
- Lors de la génération des questions complémentaires droit de l'immigration FRANCE, le LLM reçoit ces codes dans la liste autorisée.
- Tous ces codes sont annotés FRANCE UNIQUEMENT.

## 4. Cas d'erreur

- Ces codes ne doivent être émis que pour des dossiers FRANCE.
- Fail-open : si les pièces ne permettent pas de trancher, le LLM n'émet aucun item avec ces codes.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsIm11Im12Im13CriteraCodes` — le prompt contient `IM11_TITRE_ACTUEL`, `IM12_DISPOSITIF_ASILE`, `IM13_VOIE_NATURALISATION` annotés FRANCE UNIQUEMENT.
- `systemPrompt_containsIm19MineursCriteraCodes` — le prompt contient `IM19_DATE_NAISSANCE`, `IM19_DATE_ENTREE`.
- `systemPrompt_containsIm20EloignementAndIm24ViolencesCriteraCodes` — le prompt contient `IM20_DISPOSITIF_ELOIGNEMENT`, `IM20_MOTIF_MENACE`, `IM24_DATE_ORDONNANCE_PROTECTION`.

**AiQuestionServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsIm11Im12Im13CodesForQuestions`
- `systemPrompt_containsIm19MineursCodesForQuestions`
- `systemPrompt_containsIm20EloignementIm24ViolencesCodesForQuestions`

### Non-régression
- Tests SF-250-02 à SF-250-05 restent verts.

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `questions[].critere_code`)

### Frontend (non modifiés)
- 6 composants `*-section.component.ts` (changement-statut, asile-avance, naturalisation, mineurs-immigration, mesures-eloignement, victime-violences)

### Tests (ajoutés)
- `CaseAnalysisServiceTest.java` — 3 nouveaux tests
- `AiQuestionServiceTest.java` — 3 nouveaux tests

---

## 7. Hors-périmètre

- BE_9TER_*, IM08_*_BE, IM17_* — SF-250-07.
- Famille FR/BE (SF-250-08 à SF-250-10).
- Garde-fou SF-250-11.

---

## 8. Référence audit

Tableau SF-250-01 section 3.3 — outils Immigration FR ❌ cassés : F-IM-11, F-IM-12, F-IM-13, F-IM-19, F-IM-20, F-IM-24.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-06.

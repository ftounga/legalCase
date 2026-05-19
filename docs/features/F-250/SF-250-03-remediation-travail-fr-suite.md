# SF-250-03 — Remédiation critereCode F-IA-03 — lot Travail FR rupture/requalifications/inaptitude/AT-MP

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-03-remediation-travail-fr-suite`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les 16 `critereCode` attendus par les 7 composants frontend du lot Travail FR rupture/requalifications/inaptitude/AT-MP — déclenchant ainsi le cross-check F-IA-03 actuellement mort pour ces outils.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-DT-10-rupture-conv-validity` | `rupture-conv-section` | `RC_CONSENTEMENT` | Le consentement à la rupture conventionnelle est libre et éclairé (absence de vice du consentement identifiée dans les pièces). Binaire — VERIFIED = consentement documenté. |
| `F-DT-10-rupture-conv-validity` | `rupture-conv-section` | `RC_DELAI_RETRACTATION` | Le délai de rétractation de 15 jours calendaires a été respecté. Binaire — VERIFIED = délai écoulé sans rétractation. |
| `F-DT-10-rupture-conv-validity` | `rupture-conv-section` | `RC_HOMOLOGATION` | L'homologation de la convention par la DREETS/DRIESST est obtenue (ou en cours). Binaire — VERIFIED = homologation documentée. |
| `F-DT-10-rupture-conv-validity` | `rupture-conv-section` | `RC_ASSISTANCE` | Les parties disposaient de la possibilité d'être assistées lors de l'entretien. Binaire — VERIFIED = droit à l'assistance respecté. |
| `F-DT-10-rupture-conv-validity` | `rupture-conv-section` | `RC_INDEMNITE` | L'indemnité spécifique de rupture conventionnelle est au moins égale au plancher légal (1/4 de mois par année d'ancienneté). Binaire — VERIFIED = montant conforme. |
| `F-DT-10-rupture-conv-validity` | `rupture-conv-section` | `RC_ENTRETIENS` | Au moins un entretien préalable a été tenu entre les parties. Binaire — VERIFIED = entretien(s) tenu(s) documenté(s). |
| `F-DT-22-requalification-cdd-cdi` | `requalification-cdd-cdi-section` | `DT22_SALAIRE` | Salaire brut mensuel de référence identifié dans les pièces (base de calcul indemnité requalification). Binaire — VERIFIED = salaire identifié. |
| `F-DT-23-requalification-interim-cdi` | `requalification-interim-cdi-section` | `DT23_SALAIRE` | Salaire brut mensuel de référence identifié dans les pièces (base de calcul indemnité requalification intérim). Binaire — VERIFIED = salaire identifié. |
| `F-DT-24-non-concurrence` | `non-concurrence-section` | `DT24_SALAIRE` | Salaire brut mensuel de référence identifié dans les pièces (base de calcul contrepartie pécuniaire non-concurrence). Binaire — VERIFIED = salaire identifié. |
| `F-DT-31-transaction` | `transaction-section` | `DT31_SALAIRE_MENSUEL` | Salaire mensuel brut de référence identifié dans les pièces (base de calcul transaction). Binaire — VERIFIED = salaire identifié. |
| `F-DT-31-transaction` | `transaction-section` | `DT31_ANCIENNETE` | Ancienneté en mois/années identifiée dans les pièces (paramètre clé de la transaction). Binaire — VERIFIED = ancienneté identifiée. |
| `F-132-rupture-conv-indemnite` | `rupture-conv-indemnite-section` | `RCI_SALAIRE` | Salaire brut mensuel de référence identifié dans les pièces (base de calcul indemnité rupture conventionnelle). Binaire — VERIFIED = salaire identifié. |
| `F-132-rupture-conv-indemnite` | `rupture-conv-indemnite-section` | `RCI_ANCIENNETE` | Ancienneté en mois/années identifiée dans les pièces (paramètre clé de l'indemnité RC). Binaire — VERIFIED = ancienneté identifiée. |
| `F-DT-15-inaptitude` | `inaptitude-section` | `INAPT_ORIGINE` | Origine de l'inaptitude identifiée dans les pièces (professionnelle vs non-professionnelle — détermine le régime indemnitaire). Binaire — VERIFIED = origine documentée. |
| `F-DT-15-inaptitude` | `inaptitude-section` | `INAPT_RECLASSEMENT` | Obligation de reclassement et/ou recherche de reclassement documentée dans les pièces. Binaire — VERIFIED = reclassement documenté. |
| `F-DT-33-at-mp` | `at-mp-section` | `AT_MP_DATE_ACCIDENT` | Date de l'accident du travail ou de la déclaration de maladie professionnelle identifiée dans les pièces. Binaire — VERIFIED = date identifiée. |

Total : 7 outils, 16 codes, tous binaires (pas de `expected_value`). Tous FRANCE UNIQUEMENT.

**Note :** `SALAIRE_BRUT_MENSUEL` (code générique mentionné dans l'audit pour DT22/DT23/DT24/DT31) n'est pas ajouté — seuls les codes spécifiques outil (`DT22_SALAIRE`, `DT23_SALAIRE`, `DT24_SALAIRE`, `DT31_SALAIRE_MENSUEL`) sont dans le périmètre. Le code `DATE_ACCIDENT` (générique mentionné dans l'audit pour F-DT-33) est couvert par `AT_MP_DATE_ACCIDENT` — le code spécifique est préféré.

---

## 3. Comportement nominal

- Lors d'une analyse de dossier droit du travail FRANCE, le LLM reçoit maintenant ces 16 codes dans la liste autorisée des `critere_code` de `points_procedure[]`.
- Lors de la génération des questions complémentaires droit du travail FRANCE, le LLM reçoit maintenant ces 16 codes dans la liste autorisée des `critere_code` des `questions[]`.
- Si le dossier contient des pièces relatives à une rupture conventionnelle, une requalification CDD/intérim, une clause de non-concurrence, une transaction, une inaptitude ou un AT/MP, le LLM émet un ou plusieurs `points_procedure` ou `questions` avec le code correspondant, `expected_value: null`, et `VERIFIED` / `NON_COMPLIANT` selon les pièces.

## 4. Cas d'erreur

- Si les pièces ne permettent pas de trancher : le LLM n'émet aucun item avec ces codes (fail-open du parseur `ProcedureCheckService` — aucune modification nécessaire).
- Les codes ne sont **jamais** émis pour un dossier BELGIQUE (FRANCE UNIQUEMENT).
- En l'absence de signe de rupture conventionnelle, requalification, non-concurrence, transaction, inaptitude ou AT/MP dans les pièces, les codes restent absents de la réponse LLM.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsRcCriteraCodes` — le prompt système travail-FR contient les 6 codes RC_*.
- `systemPrompt_containsDt22Dt23Dt24Dt31RciCriteraCodes` — le prompt contient DT22_SALAIRE, DT23_SALAIRE, DT24_SALAIRE, DT31_SALAIRE_MENSUEL, DT31_ANCIENNETE, RCI_SALAIRE, RCI_ANCIENNETE.
- `systemPrompt_containsInapt_andAtMpCriteraCodes` — le prompt contient INAPT_ORIGINE, INAPT_RECLASSEMENT, AT_MP_DATE_ACCIDENT.

**AiQuestionServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsRcCodesForQuestions` — le prompt questions travail-FR contient les 6 codes RC_*.
- `systemPrompt_containsDt22Dt23Dt24Dt31RciCodesForQuestions` — contient les 7 codes salaire/ancienneté.
- `systemPrompt_containsInapt_andAtMpCodesForQuestions` — contient INAPT_ORIGINE, INAPT_RECLASSEMENT, AT_MP_DATE_ACCIDENT.

### Non-régression
- Tests existants SF-250-02 (DT36_*, HLN_*, DT13_*, PSE_*, PROTECTION_RP_*) restent verts.
- Test existant `systemPrompt_keepsExistingEnumeratedCriteriaCodesIntact` reste vert.

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` section `points_procedure`
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` section `questions[].critere_code`

### Backend (non modifiés)
- `ProcedureCheckService.java` — parseur fail-open
- `LegalDomainPromptBuilder.java` — non touché

### Frontend (non modifiés)
- 7 composants `*-section.component.ts` — attendent déjà ces codes

### Tests (ajoutés/étendus)
- `CaseAnalysisServiceTest.java` — 3 nouveaux tests
- `AiQuestionServiceTest.java` — 3 nouveaux tests

---

## 7. Hors-périmètre

- Codes du lot SF-250-04 (CREDIT_TEMPS_*, TRAVAIL_PROCEDURE_TYPE, F-136) — SF-250-04.
- Outils Belgique (F-DT-29) — SF-250-04.
- Outils Immigration, Famille — SF-250-05 à SF-250-10.
- Garde-fou de gouvernance (test d'intégrité automatique) — SF-250-11.
- Modification du parseur `ProcedureCheckService.java` — non requis.
- Code générique `SALAIRE_BRUT_MENSUEL` (non spécifique outil) — hors périmètre F-250.

---

## 8. Référence audit

Tableau SF-250-01 section 3.1 — outils travail-FR ❌ cassés : F-DT-10, F-DT-15, F-DT-22, F-DT-23, F-DT-24, F-DT-31, F-DT-33, F-132-RCI.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-03.

# SF-250-02 — Remédiation critereCode F-IA-03 — lot Travail FR validité/procédure

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-02-remediation-travail-fr`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les 8 `critereCode` attendus par les 5 composants frontend du lot Travail FR validité/procédure — déclenchant ainsi le cross-check F-IA-03 actuellement mort pour ces outils.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-DT-36-procedure-nullite-licenciement` | `procedure-nullite-licenciement-section` | `DT36_DATE_ENTRETIEN` | Date de convocation/tenue de l'entretien préalable retrouvée dans les pièces. Binaire — VERIFIED = date identifiée. |
| `F-DT-36-procedure-nullite-licenciement` | `procedure-nullite-licenciement-section` | `DT36_MOTIVATION` | La lettre de licenciement énonce un motif précis (légalement requis). Binaire — VERIFIED = motivation présente. |
| `F-DT-36-procedure-nullite-licenciement` | `procedure-nullite-licenciement-section` | `DT36_ENTRETIEN_TENU` | L'entretien préalable a effectivement eu lieu. Binaire — VERIFIED = entretien tenu. |
| `F-DT-11-harcelement-licenciement-nul` | `harcelement-licenciement-nul-section` | `HLN_MOTIF_NULLITE` | Motif de nullité du licenciement identifié (harcèlement, discrimination, protection). Binaire — VERIFIED = motif de nullité détecté dans les pièces. |
| `F-DT-13-licenciement-economique` | `licenciement-economique-section` | `DT13_MOTIF_ECONOMIQUE` | Motif économique justificatif du licenciement détecté dans les pièces. Binaire — VERIFIED = motif économique documenté (difficultés économiques, sauvegarde compétitivité, mutation technologique, cessation activité). |
| `F-DT-13-licenciement-economique` | `licenciement-economique-section` | `DT13_DATE_NOTIFICATION` | Date de notification du licenciement économique identifiée dans les pièces. Binaire — VERIFIED = date détectée. |
| `F-DT-14-pse-validite` | `pse-section` | `PSE_DATE_PROJET` | Date de présentation du projet de PSE (Plan de Sauvegarde de l'Emploi) identifiée dans les pièces. Binaire — VERIFIED = date détectée. |
| `F-DT-30-protection-rp` | `protection-rp-section` | `PROTECTION_RP_MOTIF` | Motif invoqué à l'appui du licenciement d'un représentant du personnel identifié dans les pièces. Binaire — VERIFIED = motif documenté. |

Total : 5 outils, 8 codes, tous binaires (pas de `expected_value`).

---

## 3. Comportement nominal

- Lors d'une analyse de dossier droit du travail FRANCE, le LLM reçoit maintenant ces 8 codes dans la liste autorisée des `critere_code` de `points_procedure[]`.
- Lors de la génération des questions complémentaires droit du travail FRANCE, le LLM reçoit maintenant ces 8 codes dans la liste autorisée des `critere_code` des `questions[]`.
- Si le dossier contient des pièces relatives à une procédure de licenciement (nullité, économique, PSE, représentant du personnel, harcèlement), le LLM émet un ou plusieurs `points_procedure` ou `questions` avec le code correspondant, `expected_value: null`, et `VERIFIED` / `NON_COMPLIANT` selon les pièces.

## 4. Cas d'erreur

- Si les pièces ne permettent pas de trancher : le LLM n'émet aucun item avec ces codes (comportement fail-open préexistant du parseur `ProcedureCheckService` — aucune modification nécessaire).
- Les codes ne sont **jamais** émis pour un dossier BELGIQUE (isolation domaine — les prompts travail et immigration sont contextualisés par `workspace.legalDomain` et `workspace.country`).
- En l'absence de signe de licenciement économique, PSE, représentant du personnel, harcèlement ou nullité dans les pièces, les codes restent absents de la réponse LLM — aucune régression possible.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsDt36CriteraCodes` — le prompt système travail-FR contient `DT36_DATE_ENTRETIEN`, `DT36_MOTIVATION`, `DT36_ENTRETIEN_TENU`.
- `systemPrompt_containsHlnAndDt13AndPseAndProtectionRpCriteraCodes` — le prompt contient `HLN_MOTIF_NULLITE`, `DT13_MOTIF_ECONOMIQUE`, `DT13_DATE_NOTIFICATION`, `PSE_DATE_PROJET`, `PROTECTION_RP_MOTIF`.
- `systemPrompt_travailFrNewCodesHaveNullExpectedValue` — la sémantique binaire est documentée dans le prompt (`expected_value` reste null pour ces codes).

**AiQuestionServiceTest** (2 nouveaux tests) :
- `systemPrompt_containsDt36CodesForQuestions` — le prompt système questions travail-FR contient `DT36_DATE_ENTRETIEN`, `DT36_MOTIVATION`, `DT36_ENTRETIEN_TENU`.
- `systemPrompt_containsHlnDt13PsePRotectionRpCodesForQuestions` — le prompt contient `HLN_MOTIF_NULLITE`, `DT13_MOTIF_ECONOMIQUE`, `DT13_DATE_NOTIFICATION`, `PSE_DATE_PROJET`, `PROTECTION_RP_MOTIF`.

### Non-régression
- Test existant `systemPrompt_keepsExistingEnumeratedCriteriaCodesIntact` reste vert (les codes FR_CONVOCATION, FR_ENTRETIEN, DT09_TYPE_RUPTURE, etc. sont inchangés).

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` lignes 52–61 (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` lignes 38–46 (section `questions[].critere_code`)

### Backend (non modifiés)
- `ProcedureCheckService.java` — parseur fail-open, accepte déjà tout code
- `LegalDomainPromptBuilder.java` — non touché (les codes sont dans les prompts génériques, pas dans les instructions domaine-spécifiques)

### Frontend (non modifiés)
- 5 composants `*-section.component.ts` — ils attendent déjà ces codes et les consomment

### Tests (ajoutés/étendus)
- `CaseAnalysisServiceTest.java` — 3 nouveaux tests de prompt
- `AiQuestionServiceTest.java` — 2 nouveaux tests de prompt

---

## 7. Hors-périmètre

- Codes du lot SF-250-03 (RC_*, DT22_*, DT23_*, DT24_*, DT31_*, INAPT_*, AT_MP_*) — dans une SF suivante.
- Codes du lot SF-250-04 (CREDIT_TEMPS_*, TRAVAIL_PROCEDURE_TYPE) — SF-250-04.
- Outils Belgique (F-DT-29), Immigration, Famille — SF-250-05 à SF-250-10.
- Garde-fou de gouvernance (test d'intégrité automatique) — SF-250-11.
- Modification du `LegalDomainPromptBuilder.java` — non requis pour ce lot.
- Modification du parseur `ProcedureCheckService.java` — non requis (fail-open préexistant).

---

## 8. Référence audit

Tableau SF-250-01 section 3.1 — outils travail-FR ❌ cassés : F-DT-36, F-DT-11, F-DT-13, F-DT-14, F-DT-30.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-02.

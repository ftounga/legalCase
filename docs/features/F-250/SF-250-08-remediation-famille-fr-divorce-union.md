# SF-250-08 — Remédiation critereCode F-IA-03 — lot Famille FR — divorce / union

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-08-remediation-famille-fr-divorce-union`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les 21 `critereCode` attendus par les outils frontend du lot Famille FR divorce / union — déclenchant ainsi le cross-check F-IA-03 actuellement mort pour ces outils.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-FA-08-divorce-alteration` | `divorce-alteration-section` | `DA_DUREE_MARIAGE` | Durée du mariage identifiée dans les pièces — condition légale de la procédure pour altération définitive du lien conjugal (2 ans de séparation requis). Binaire — VERIFIED = durée documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-09-divorce-faute` | `divorce-faute-section` | `FA09_DUREE_MARIAGE` | Durée du mariage identifiée dans les pièces. Binaire — VERIFIED = durée documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-09-divorce-faute` | `divorce-faute-section` | `FA09_DATE_DEPOT_ASSIGNATION` | Date de dépôt de l'assignation pour divorce pour faute identifiée dans les pièces. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-09-divorce-faute` | `divorce-faute-section` | `FA09_FAUTES_INVOQUEES` | Fautes invoquées à l'appui de la demande de divorce pour faute identifiées et qualifiées dans les pièces. Binaire — VERIFIED = fautes documentées. **FRANCE UNIQUEMENT.** |
| `F-FA-12-mesures-provisoires` | `mesures-provisoires-section` | `FA12_DATE_AUDIENCE` | Date de l'audience de mesures provisoires identifiée dans les pièces. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-12-mesures-provisoires` | `mesures-provisoires-section` | `FA12_VIOLENCES` | Violences alléguées dans le cadre des mesures provisoires identifiées et qualifiées dans les pièces. Binaire — VERIFIED = violences documentées. **FRANCE UNIQUEMENT.** |
| `F-FA-13-revisions-post-divorce` | `revisions-post-divorce-section` | `FA13_NB_ENFANTS` | Nombre d'enfants concernés par la révision des mesures post-divorce identifié dans les pièces. Binaire — VERIFIED = nombre documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-14-ordonnance-protection` | `ordonnance-protection-section` | `FA14_DATE_REQUETE` | Date de la requête en ordonnance de protection identifiée dans les pièces. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-14-ordonnance-protection` | `ordonnance-protection-section` | `FA14_VIOLENCES_ALLEGUEES` | Violences alléguées à l'appui de la demande d'ordonnance de protection identifiées dans les pièces. Binaire — VERIFIED = violences documentées. **FRANCE UNIQUEMENT.** |
| `F-FA-14-ordonnance-protection` | `ordonnance-protection-section` | `FA14_LOGEMENT_COMMUN` | Existence d'un logement commun identifiée dans les pièces — condition d'accès à certaines mesures de l'ordonnance de protection. Binaire — VERIFIED = logement commun documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-15-recompenses` | `recompenses-section` | `FA15_REGIME_MATRIMONIAL` | Régime matrimonial des époux identifié dans les pièces — détermine les règles de récompenses applicable (communauté légale ou conventionnelle). Binaire — VERIFIED = régime documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-16-communaute-universelle` | `communaute-universelle-section` | `COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE` | Contrat de mariage notarié adopant la communauté universelle identifié dans les pièces. Binaire — VERIFIED = contrat documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-16-communaute-universelle` | `communaute-universelle-section` | `COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS` | Existence d'enfants non communs identifiée dans les pièces — impacte les droits successoraux en régime de communauté universelle (clause d'attribution intégrale). Binaire — VERIFIED = situation documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-17-partage-judiciaire` | `partage-judiciaire-section` | `PARTAGE_JUDICIAIRE_PV` | Procès-verbal d'état liquidatif dressé par le notaire identifié dans les pièces — étape procédurale du partage judiciaire. Binaire — VERIFIED = PV documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-17-partage-judiciaire` | `partage-judiciaire-section` | `PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE` | Tentative amiable de partage préalable identifiée dans les pièces — condition d'accès au partage judiciaire. Binaire — VERIFIED = tentative documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-19-autorite-parentale` (+ 2 outils F-FA-19) | 3 composants F-FA-19 | `FA19_REGIME_EXERCICE_ACTUEL` | Régime d'exercice actuel de l'autorité parentale identifié dans les pièces (conjoint ou exclusif). Binaire — VERIFIED = régime documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-19-*` | 3 composants F-FA-19 | `FA19_DANGER_CARACTERISE` | Danger caractérisé pour l'enfant identifié dans les pièces — condition pour demande de modification de l'autorité parentale. Binaire — VERIFIED = danger documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-19-*` | composants F-FA-19 | `FA19_CONSENTEMENT_AUTRE_PARENT` | Consentement de l'autre parent identifié dans les pièces. Binaire — VERIFIED = consentement documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-19-*` | composants F-FA-19 | `FA19_INTERFERENCE_VIE_ENFANT` | Interférence dans la vie de l'enfant identifiée dans les pièces. Binaire — VERIFIED = interférence documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-19-*` | composants F-FA-19 | `FA19_AGE_ENFANTS` | Âge des enfants concernés identifié dans les pièces. Binaire — VERIFIED = âge documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-19-changement-residence` | `changement-residence-section` | `FA19_RAISON_CHANGEMENT` | Raison du changement de résidence identifiée dans les pièces. Binaire — VERIFIED = raison documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-19-changement-residence` | `changement-residence-section` | `FA19_INFORME_PREALABLEMENT` | Obligation d'information préalable de l'autre parent respectée selon les pièces. Binaire — VERIFIED = information documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-19-changement-residence` | `changement-residence-section` | `FA19_MODE_RESIDENCE_ACTUEL` | Mode de résidence actuel de l'enfant identifié dans les pièces. Binaire — VERIFIED = mode documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-19-desaccords-parentaux` | `desaccords-parentaux-section` | `FA19_DOMAINE_DESACCORD` | Domaine du désaccord parental identifié dans les pièces (scolarité, santé, religion, etc.). Binaire — VERIFIED = domaine documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-19-desaccords-parentaux` | `desaccords-parentaux-section` | `FA19_INTENSITE_DESACCORD` | Intensité du désaccord parental évaluée dans les pièces. Binaire — VERIFIED = intensité documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-19-desaccords-parentaux` | `desaccords-parentaux-section` | `FA19_TENTATIVES_MEDIATION` | Tentatives de médiation préalables identifiées dans les pièces. Binaire — VERIFIED = tentatives documentées. **FRANCE UNIQUEMENT.** |
| `F-FA-19-desaccords-parentaux` | `desaccords-parentaux-section` | `FA19_AGE_ENFANTS_CONCERNES` | Âge des enfants concernés par le désaccord identifié dans les pièces. Binaire — VERIFIED = âge documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-19-desaccords-parentaux` | `desaccords-parentaux-section` | `FA19_URGENCE` | Urgence de la situation identifiée dans les pièces (critère de recours au référé). Binaire — VERIFIED = urgence documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-20-pacs-dissolution` | `pacs-dissolution-section` | `FA20_MODE_DISSOLUTION` | Mode de dissolution du PACS identifié dans les pièces (conjointe, unilatérale, mariage, décès). Binaire — VERIFIED = mode documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-20-pacs-dissolution` | `pacs-dissolution-section` | `FA20_REGIME_BIENS` | Régime des biens du PACS identifié dans les pièces (indivision, séparation des patrimoines). Binaire — VERIFIED = régime documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-20-pacs-dissolution` | `pacs-dissolution-section` | `FA20_CREANCES_ALLEGUEES` | Créances alléguées entre les partenaires identifiées dans les pièces. Binaire — VERIFIED = créances documentées. **FRANCE UNIQUEMENT.** |
| `F-FA-21-separation-corps` | `separation-corps-section` | `FA21_DATE_JUGEMENT_SEPARATION` | Date du jugement de séparation de corps identifiée dans les pièces — détermine les délais de conversion en divorce. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-22-indivision` | `indivision-section` | `FA22_DATE_ORIGINE` | Date d'origine de l'indivision identifiée dans les pièces (date du décès, de la séparation, etc.). Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-22-indivision` | `indivision-section` | `FA22_OCCUPATION` | Occupation du bien indivis par l'un des indivisaires identifiée dans les pièces — détermine les droits à indemnité d'occupation. Binaire — VERIFIED = occupation documentée. **FRANCE UNIQUEMENT.** |

Total : ~14 outils, ~21 codes uniques (regroupés en familles FA19_*, DA_*, FA09_*, FA12_*, FA13_*, FA14_*, FA15_*, COMMUNAUTE_UNIVERSELLE_*, PARTAGE_JUDICIAIRE_*, FA20_*, FA21_*, FA22_*), tous binaires (pas de `expected_value`).

---

## 3. Comportement nominal

- Lors d'une analyse de dossier droit de la famille FRANCE, le LLM reçoit maintenant ces codes dans la liste autorisée des `critere_code` de `points_procedure[]`.
- Lors de la génération des questions complémentaires droit de la famille FRANCE, le LLM reçoit ces codes dans la liste autorisée.
- Tous ces codes sont annotés FRANCE UNIQUEMENT dans le prompt.

## 4. Cas d'erreur

- Ces codes ne doivent être émis que pour des dossiers FRANCE droit de la famille.
- Fail-open : si les pièces ne permettent pas de trancher, le LLM n'émet aucun item avec ces codes.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsDaFa09Fa12Fa13Fa14CriteraCodes` — le prompt contient `DA_DUREE_MARIAGE`, `FA09_*`, `FA12_*`, `FA13_NB_ENFANTS`, `FA14_*` annotés FRANCE UNIQUEMENT.
- `systemPrompt_containsFa15CommunauteUniversellePartageJudiciaireCriteraCodes` — le prompt contient `FA15_REGIME_MATRIMONIAL`, `COMMUNAUTE_UNIVERSELLE_*`, `PARTAGE_JUDICIAIRE_*`.
- `systemPrompt_containsFa19Fa20Fa21Fa22CriteraCodes` — le prompt contient `FA19_*` (8 codes), `FA20_*`, `FA21_DATE_JUGEMENT_SEPARATION`, `FA22_*`.

**AiQuestionServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsDaFa09Fa12Fa13Fa14CodesForQuestions`
- `systemPrompt_containsFa15CommunauteUniversellePartageJudiciairCodesForQuestions`
- `systemPrompt_containsFa19Fa20Fa21Fa22CodesForQuestions`

### Non-régression
- Tests SF-250-02 à SF-250-07 restent verts.

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `questions[].critere_code`)

### Frontend (non modifiés)
- ~14 composants `*-section.component.ts` (divorce-alteration, divorce-faute, mesures-provisoires, revisions-post-divorce, ordonnance-protection, recompenses, communaute-universelle, partage-judiciaire, 3x autorite-parentale/changement-residence/desaccords-parentaux, pacs-dissolution, separation-corps, indivision)

### Tests (ajoutés)
- `CaseAnalysisServiceTest.java` — 3 nouveaux tests
- `AiQuestionServiceTest.java` — 3 nouveaux tests

---

## 7. Hors-périmètre

- Famille FR successions/libéralités (SF-250-09).
- Famille BE (SF-250-10).
- Garde-fou de gouvernance (SF-250-11).
- Outils en statut ⚠️ (F-FA-10, F-FA-18-*, mediation, acceptation-renonciation) — SF-250-10.

---

## 8. Référence audit

Tableau SF-250-01 section 3.5 — outils Famille FR ❌ cassés : F-FA-08, F-FA-09, F-FA-12, F-FA-13, F-FA-14, F-FA-15, F-FA-16, F-FA-17, F-FA-19×3, F-FA-20, F-FA-21, F-FA-22.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-08.

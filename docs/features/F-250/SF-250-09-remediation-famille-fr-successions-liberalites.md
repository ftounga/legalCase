# SF-250-09 — Remédiation critereCode F-IA-03 — lot Famille FR — successions / libéralités

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-09-remediation-famille-fr-successions`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent les ~18 `critereCode` attendus par les outils frontend du lot Famille FR successions / libéralités — déclenchant ainsi le cross-check F-IA-03 actuellement mort pour ces outils.

---

## 2. Outils couverts et codes ajoutés

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-FA-24-testament-validite` | `testament-validite-section` | `TESTAMENT_FORME` | Forme du testament respectée selon les pièces (olographe : entièrement manuscrit, daté, signé ; authentique : acte notarié). Binaire — VERIFIED = forme documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-24-testament-validite` | `testament-validite-section` | `TESTAMENT_SAINE_ESPRIT` | Capacité du testateur au moment de la rédaction du testament documentée dans les pièces (saine d'esprit — art. 901 C.civ.). Binaire — VERIFIED = capacité documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-24-testament-validite` | `testament-validite-section` | `TESTAMENT_QUOTITE` | Respect de la quotité disponible identifié dans les pièces — le testament ne porte pas atteinte à la réserve héréditaire. Binaire — VERIFIED = respect documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-24-donation` | `donation-section` | `DONATION_FORME` | Forme de la donation respectée selon les pièces (acte notarié obligatoire pour donation d'immeuble ou de droits réels — art. 931 C.civ.). Binaire — VERIFIED = forme documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-24-donation` | `donation-section` | `DONATION_SAINE_ESPRIT` | Capacité du donateur au moment de la donation documentée dans les pièces. Binaire — VERIFIED = capacité documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-24-donation` | `donation-section` | `DONATION_QUOTITE` | Respect de la quotité disponible identifié dans les pièces — la donation ne porte pas atteinte à la réserve héréditaire. Binaire — VERIFIED = respect documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-24-partage-successoral` | `partage-successoral-section` | `PARTAGE_MODE` | Mode de partage retenu identifié dans les pièces : amiable ou judiciaire. Binaire — VERIFIED = mode documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-24-partage-successoral` | `partage-successoral-section` | `PARTAGE_CONSENTEMENTS` | Consentement de tous les co-indivisaires au partage amiable identifié dans les pièces. Binaire — VERIFIED = consentement documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-24-partage-successoral` | `partage-successoral-section` | `PARTAGE_PRESENCE_IMMEUBLES` | Présence de biens immobiliers dans la masse successorale identifiée dans les pièces — imposant le recours au notaire. Binaire — VERIFIED = présence documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-24-indivision-successorale` | `indivision-successorale-section` | `INDIVISION_DATE_OUVERTURE` | Date d'ouverture de l'indivision successorale identifiée dans les pièces (date du décès). Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-24-indivision-successorale` | `indivision-successorale-section` | `INDIVISION_TYPE` | Type d'indivision identifié dans les pièces : successorale, post-communautaire, conventionnelle. Binaire — VERIFIED = type documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-24-devolution-legale` | `devolution-legale-section` | `DEVOLUTION_LEGALE_CONJOINT` | Statut du conjoint survivant identifié dans les pièces — détermine ses droits légaux dans la dévolution (art. 731 et s. C.civ.). Binaire — VERIFIED = statut documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-24-devolution-legale` | `devolution-legale-section` | `DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS` | Existence de descendants communs ou non identifiée dans les pièces — détermine la quote-part du conjoint survivant en pleine propriété ou en usufruit. Binaire — VERIFIED = situation documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-25-majeurs-proteges` | `majeurs-proteges-section` | `FA25_DATE_CERTIFICAT` | Date du certificat médical circonstancié identifiée dans les pièces — condition d'ouverture d'une mesure de protection. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-25-majeurs-proteges` | `majeurs-proteges-section` | `FA25_ALT_MENTALES` | Altérations des facultés mentales ou corporelles identifiées dans les pièces — fondement de la mesure de protection. Binaire — VERIFIED = altérations documentées. **FRANCE UNIQUEMENT.** |
| `F-FA-25-majeurs-proteges` | `majeurs-proteges-section` | `FA25_CONSENTEMENT` | Consentement de la personne protégée recueilli selon les pièces — obligation procédurale de la mesure. Binaire — VERIFIED = consentement documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-25-majeurs-proteges` | `majeurs-proteges-section` | `FA25_DEMANDEUR_FAMILIAL` | Qualité familiale du demandeur de la mesure de protection identifiée dans les pièces (habilité à saisir le juge des tutelles). Binaire — VERIFIED = qualité documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-26-changement-etat-civil` | `changement-etat-civil-section` | `FA26_TYPE_CHANGEMENT` | Type de changement d'état civil demandé identifié dans les pièces : changement de prénom, de nom, de genre. Binaire — VERIFIED = type documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-26-changement-etat-civil` | `changement-etat-civil-section` | `FA26_MOTIF_INVOQUE` | Motif invoqué à l'appui de la demande de changement d'état civil identifié dans les pièces. Binaire — VERIFIED = motif documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-26-changement-etat-civil` | `changement-etat-civil-section` | `FA26_DATE_NAISSANCE` | Date de naissance du demandeur identifiée dans les pièces — condition de majorité pour certaines demandes. Binaire — VERIFIED = date documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-26-changement-etat-civil` | `changement-etat-civil-section` | `FA26_MAJEUR_DEMANDEUR` | Majorité du demandeur identifiée dans les pièces — condition pour les demandes sans représentant légal. Binaire — VERIFIED = majorité documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-26-changement-etat-civil` | `changement-etat-civil-section` | `FA26_CONSENTEMENT_PARENTAL` | Consentement parental identifié dans les pièces — requis pour les demandes de mineurs (art. 61-3 C.civ.). Binaire — VERIFIED = consentement documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-27-pma-gpa` | `pma-gpa-bioethique-section` | `PMA_GPA_DISPOSITIF` | Dispositif de bioéthique concerné identifié dans les pièces : PMA (procréation médicalement assistée — art. L.2141-1 CSP) ou GPA (gestation pour autrui — prohibition en France). Binaire — VERIFIED = dispositif documenté. **FRANCE UNIQUEMENT.** |
| `F-FA-05-partage-immobilier` | `partage-immobilier-section` | `FA05_VALEUR_VENALE` | Valeur vénale du bien immobilier identifiée dans les pièces — base de calcul du partage (estimation notariale, expertise, ou déclaration). Binaire — VERIFIED = valeur documentée. **FRANCE UNIQUEMENT.** |
| `F-FA-05-partage-immobilier` | `partage-immobilier-section` | `FA05_CAPITAL_RESTANT` | Capital restant dû du prêt immobilier identifié dans les pièces — déduction du passif dans le calcul du partage. Binaire — VERIFIED = capital documenté. **FRANCE UNIQUEMENT.** |

Total : 9 outils, ~18 codes uniques (TESTAMENT_*, DONATION_*, PARTAGE_*, INDIVISION_*, DEVOLUTION_LEGALE_*, FA25_*, FA26_*, PMA_GPA_DISPOSITIF, FA05_*), tous binaires (pas de `expected_value`).

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
- `systemPrompt_containsTestamentDonationCriteraCodes` — le prompt contient `TESTAMENT_*`, `DONATION_*` annotés FRANCE UNIQUEMENT.
- `systemPrompt_containsPartageIndivisionDevolutionCriteraCodes` — le prompt contient `PARTAGE_*`, `INDIVISION_*`, `DEVOLUTION_LEGALE_*`.
- `systemPrompt_containsFa25Fa26PmaGpaFa05CriteraCodes` — le prompt contient `FA25_*`, `FA26_*`, `PMA_GPA_DISPOSITIF`, `FA05_*`.

**AiQuestionServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsTestamentDonationCodesForQuestions`
- `systemPrompt_containsPartageIndivisionDevolutionCodesForQuestions`
- `systemPrompt_containsFa25Fa26PmaGpaFa05CodesForQuestions`

### Non-régression
- Tests SF-250-02 à SF-250-08 restent verts.

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `questions[].critere_code`)

### Frontend (non modifiés)
- 9 composants `*-section.component.ts` (testament-validite, donation, partage-successoral, indivision-successorale, devolution-legale, majeurs-proteges, changement-etat-civil, pma-gpa-bioethique, partage-immobilier)

### Tests (ajoutés)
- `CaseAnalysisServiceTest.java` — 3 nouveaux tests
- `AiQuestionServiceTest.java` — 3 nouveaux tests

---

## 7. Hors-périmètre

- Famille BE (SF-250-10).
- Garde-fou de gouvernance (SF-250-11).
- Outils en statut ⚠️ (F-FA-24-reserve-heriditaire, rapport-succession, F-FA-18-*, mediation, acceptation-renonciation) — SF-250-10.

---

## 8. Référence audit

Tableau SF-250-01 section 3.5 — outils Famille FR ❌ cassés : F-FA-05, F-FA-24-testament, F-FA-24-donation, F-FA-24-partage-successoral, F-FA-24-indivision-successorale, F-FA-24-devolution-legale, F-FA-25, F-FA-26, F-FA-27.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-09.

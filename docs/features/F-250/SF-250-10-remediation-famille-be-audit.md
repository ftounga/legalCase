# SF-250-10 — Remédiation critereCode F-IA-03 — Famille BE + audit outils ⚠️

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels
**Date** : 2026-05-19
**Branche** : `feat/SF-250-10-remediation-famille-be-audit`

---

## 1. Objectif

Étendre les prompts LLM (`CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` + `AiQuestionService.SYSTEM_PROMPT_TEMPLATE`) pour qu'ils émettent :
- (A) les 3 `critereCode` du lot Famille BE (F-FA-11, regime-mat-be, liquidation-partage-be) ;
- (B) les `critereCode` effectivement attendus par les 17 outils en statut ⚠️ « À approfondir » de l'audit SF-250-01, après vérification fine de chaque composant frontend.

---

## 2. Outils couverts et codes ajoutés

### 2A — Famille BE (BELGIQUE UNIQUEMENT)

| tool_id | Composant | critereCode ajoutés | Sémantique |
|---|---|---|---|
| `F-FA-11-desunion-irremediable-be` | `divorce-desunion-be-section` | `DESU_BE_DATE_SEPARATION` | Date de séparation de fait identifiée dans les pièces — condition légale 6 mois (accord) ou 1 an (non-accord) selon loi belge du 27/04/2007. Binaire — VERIFIED = date documentée. **BELGIQUE UNIQUEMENT.** |
| `F-FA-11-desunion-irremediable-be` | `divorce-desunion-be-section` | `DESU_BE_CONSENTEE` | La désuinion est consentie par les deux époux selon les pièces — détermine la durée minimale requise (6 mois vs 1 an). Binaire — VERIFIED = consentement documenté. **BELGIQUE UNIQUEMENT.** |
| `F-FA-11-desunion-irremediable-be` | `divorce-desunion-be-section` | `DESU_BE_DATE_ASSIGNATION` | Date d'assignation en divorce identifiée dans les pièces — déclenche le délai légal de séparation. Binaire — VERIFIED = date documentée. **BELGIQUE UNIQUEMENT.** |
| `regime-mat-be-communaute-legale` | `regime-communaute-legale-be-section` | `F217_DATE_MARIAGE` | Date du mariage identifiée dans les pièces — détermine le régime légal applicable et les droits des époux au regard de l'art. 1388 du Code civil belge (régime de la communauté légale depuis 2018). Binaire — VERIFIED = date documentée. **BELGIQUE UNIQUEMENT.** |
| `liquidation-partage-be` | `liquidation-partage-be-section` | `F217_DATE_NOTIFICATION_PROJET` | Date de notification du projet de liquidation-partage identifiée dans les pièces — point de départ du délai légal de discussion (art. 1218 Code judiciaire belge). Binaire — VERIFIED = date documentée. **BELGIQUE UNIQUEMENT.** |

### 2B — Outils en statut ⚠️ — résultat de l'audit fin

| tool_id | Composant | Résultat audit | critereCode ajoutés | Annotation |
|---|---|---|---|---|
| `F-IM-09-aes-famille` | `aes-famille-section` | **IA-only** — alertes comparent `aiData` directement (DATE_ENTREE_FRANCE, DUREE_PRESENCE via `aiData.dateEntreeFrance`, `aiData.dureePresenceMois`). Aucun filtre `critereCode` dans `procedureChecks`/`aiQuestions`. | Aucun code ajouté | `➖IA` — déjà couvert par comparaison directe |
| `F-IM-21-jld-retention-fr` | `jld-retention-section` | Filtre `critereCode` présent dans `procedureChecks` : `IM21_DATE_NOTIFICATION_PLACEMENT`, `IM21_PLACEMENT_CRA`, `IM21_MOTIF_PLACEMENT`. Codes absents des prompts. | `IM21_DATE_NOTIFICATION_PLACEMENT`, `IM21_PLACEMENT_CRA`, `IM21_MOTIF_PLACEMENT` | **FRANCE UNIQUEMENT** |
| `F-IM-22-dublin-recours-fr` | `dublin-recours-section` | Filtre `critereCode` présent dans `procedureChecks` : `IM22_DATE_NOTIFICATION_DUBLIN`. Code absent des prompts. | `IM22_DATE_NOTIFICATION_DUBLIN` | **FRANCE UNIQUEMENT** |
| `F-IM-23-crrv-refus-visa-fr` | `crrv-refus-visa-section` | Filtre `critereCode` présent dans `procedureChecks` : `IM23_DATE_NOTIFICATION_REFUS`. Code absent des prompts. | `IM23_DATE_NOTIFICATION_REFUS` | **FRANCE UNIQUEMENT** |
| `F-IM-14-40ter-familial-belge-be` | `belgian-40ter-section` | **IA-only** — alertes (`LIEN_FAMILIAL`, `REVENUS_MENSUELS`, `DATE_DEPOT`) comparent `aiData` directement via `aiData.lienFamilialBe`, `aiData.revenusNetsMensuels`, `aiData.dateDepotDemande`. Aucun filtre `critereCode` dans `procedureChecks`/`aiQuestions`. | Aucun code ajouté | `➖IA` — déjà couvert par comparaison directe |
| `F-IM-14-9bis-humanitaire-be` | `belgian-9bis-section` | `FIELD_CRITERE_CODES` déclaré : `B9BIS_DATE_ENTREE_BELGIQUE`, `B9BIS_DUREE_PRESENCE`, `B9BIS_CIRCONSTANCES_EXCEPTIONNELLES`, `B9BIS_LIENS_FAMILIAUX_BE`, `B9BIS_LIENS_PROFESSIONNELS`, `B9BIS_SCOLARITE_ENFANTS_BE`, `B9BIS_MENACE_ORDRE_PUBLIC`, `B9BIS_DATE_DEPOT_DEMANDE`. Codes absents des prompts. | `B9BIS_DATE_ENTREE_BELGIQUE`, `B9BIS_DUREE_PRESENCE`, `B9BIS_CIRCONSTANCES_EXCEPTIONNELLES`, `B9BIS_LIENS_FAMILIAUX_BE`, `B9BIS_LIENS_PROFESSIONNELS`, `B9BIS_SCOLARITE_ENFANTS_BE`, `B9BIS_MENACE_ORDRE_PUBLIC`, `B9BIS_DATE_DEPOT_DEMANDE` | **BELGIQUE UNIQUEMENT** |
| `F-IM-14-40bis-cohabitant-ue-be` | `belgian-40bis-section` | Filtre `critereCode` présent dans `piecesManquantes` : `IM14_LIEN_FAMILIAL`. Code absent des prompts. | `IM14_LIEN_FAMILIAL` | **BELGIQUE UNIQUEMENT** |
| `F-FA-10-divorce-accepte` | `divorce-accepte-section` | **IA-only** — alertes (`DUREE_MARIAGE`, `REVENUS_EPOUX1`, `REVENUS_EPOUX2`, `PATRIMOINE_COMMUN`, `DATE_ACCEPTATION_PV`) comparent `aiData` directement. Les codes `FA10_*` n'apparaissent que dans `findPieceManquante()`, pas dans des filtres `procedureChecks`/`aiQuestions`. | Aucun code ajouté | `➖IA` — déjà couvert par comparaison directe |
| `F-FA-18-reconnaissance-paternelle` | `reconnaissance-paternelle-section` | Filtre `critereCode` présent (method `buildBooleanAlert`) : `CONSENTEMENT_LIBRE`, `PATERNITE_VRAISEMBLABLE`. Codes absents des prompts. | `CONSENTEMENT_LIBRE`, `PATERNITE_VRAISEMBLABLE` | **FRANCE UNIQUEMENT** |
| `F-FA-18-contestation-paternite` | `contestation-paternite-section` | Filtre `critereCode` présent dans `piecesManquantes`/`procedureChecks` : `CONTESTATION_PATERNITE_MOTIFS_SERIEUX`, `CONTESTATION_PATERNITE_EXPERTISE_ADN`, `CONTESTATION_PATERNITE_POSSESSION_ETAT`. Codes absents des prompts. | `CONTESTATION_PATERNITE_MOTIFS_SERIEUX`, `CONTESTATION_PATERNITE_EXPERTISE_ADN`, `CONTESTATION_PATERNITE_POSSESSION_ETAT` | **FRANCE UNIQUEMENT** |
| `F-FA-18-recherche-paternite` | `recherche-paternite-section` | Filtre `critereCode` présent : `RECHERCHE_PATERNITE_POSSESSION_ETAT`, `RECHERCHE_PATERNITE_EXPERTISE_ADN`, `RECHERCHE_PATERNITE_REFUS_ADN`, `RECHERCHE_PATERNITE_MOTIFS_SERIEUX`. Codes absents des prompts. | `RECHERCHE_PATERNITE_POSSESSION_ETAT`, `RECHERCHE_PATERNITE_EXPERTISE_ADN`, `RECHERCHE_PATERNITE_REFUS_ADN`, `RECHERCHE_PATERNITE_MOTIFS_SERIEUX` | **FRANCE UNIQUEMENT** |
| `F-FA-18-possession-etat` | `possession-etat-section` | Filtre `critereCode` présent : `POSSESSION_ETAT_TRACTATUS`, `POSSESSION_ETAT_FAMA`, `POSSESSION_ETAT_CONTINUE`, `POSSESSION_ETAT_PAISIBLE`, `POSSESSION_ETAT_NON_EQUIVOQUE`. Codes absents des prompts. | `POSSESSION_ETAT_TRACTATUS`, `POSSESSION_ETAT_FAMA`, `POSSESSION_ETAT_CONTINUE`, `POSSESSION_ETAT_PAISIBLE`, `POSSESSION_ETAT_NON_EQUIVOQUE` | **FRANCE UNIQUEMENT** |
| `F-FA-18-adoption` | `adoption-section` | Filtre `critereCode` présent : `ADOPTION_FORME`, `ADOPTION_PUPILLE_ETAT`, `ADOPTION_ADOPTANT_MARIE`, `ADOPTION_AGE_ADOPTANT`, `ADOPTION_AGE_ADOPTE`. Codes absents des prompts. | `ADOPTION_FORME`, `ADOPTION_PUPILLE_ETAT`, `ADOPTION_ADOPTANT_MARIE`, `ADOPTION_AGE_ADOPTANT`, `ADOPTION_AGE_ADOPTE` | **FRANCE UNIQUEMENT** |
| `F-FA-24-reserve-heriditaire` | `reserve-heriditaire-section` | Filtre `critereCode` présent dans `procedureChecks`/`aiQuestions` : `RESERVE_CONJOINT_SURVIVANT`. Code absent des prompts. | `RESERVE_CONJOINT_SURVIVANT` | **FRANCE UNIQUEMENT** |
| `F-FA-24-rapport-succession` | `rapport-succession-section` | Filtre `critereCode` présent dans `piecesManquantes` : `RAPPORT_QUALITE_HERITIER`, `RAPPORT_DONATION_NOMINALE`, `RAPPORT_VALEUR_PARTAGE`, `RAPPORT_DATE_DONATION`. Codes absents des prompts. | `RAPPORT_QUALITE_HERITIER`, `RAPPORT_DONATION_NOMINALE`, `RAPPORT_VALEUR_PARTAGE`, `RAPPORT_DATE_DONATION` | **FRANCE UNIQUEMENT** |
| `mediation-familiale-pre-saisine` | `mediation-familiale-section` | **➖NO** — reçoit `procedureChecks`/`aiQuestions` mais n'applique aucun filtre `critereCode`. Aucun cross-check F-IA-03 actif. | Aucun code ajouté | `➖NO` — déjà couvert |
| `acceptation-renonciation-succession` | `acceptation-renonciation-section` | **➖NO** — reçoit `procedureChecks`/`aiQuestions` mais n'applique aucun filtre `critereCode`. Aucun cross-check F-IA-03 actif. | Aucun code ajouté | `➖NO` — déjà couvert |

**Total codes uniques ajoutés dans cette SF :**
- Famille BE : `DESU_BE_DATE_SEPARATION`, `DESU_BE_CONSENTEE`, `DESU_BE_DATE_ASSIGNATION`, `F217_DATE_MARIAGE`, `F217_DATE_NOTIFICATION_PROJET` (5 codes BE)
- Outils ⚠️ immigration FR : `IM21_DATE_NOTIFICATION_PLACEMENT`, `IM21_PLACEMENT_CRA`, `IM21_MOTIF_PLACEMENT`, `IM22_DATE_NOTIFICATION_DUBLIN`, `IM23_DATE_NOTIFICATION_REFUS` (5 codes FR)
- Outils ⚠️ immigration BE : `B9BIS_DATE_ENTREE_BELGIQUE`, `B9BIS_DUREE_PRESENCE`, `B9BIS_CIRCONSTANCES_EXCEPTIONNELLES`, `B9BIS_LIENS_FAMILIAUX_BE`, `B9BIS_LIENS_PROFESSIONNELS`, `B9BIS_SCOLARITE_ENFANTS_BE`, `B9BIS_MENACE_ORDRE_PUBLIC`, `B9BIS_DATE_DEPOT_DEMANDE`, `IM14_LIEN_FAMILIAL` (9 codes BE)
- Outils ⚠️ famille FR : `CONSENTEMENT_LIBRE`, `PATERNITE_VRAISEMBLABLE`, `CONTESTATION_PATERNITE_MOTIFS_SERIEUX`, `CONTESTATION_PATERNITE_EXPERTISE_ADN`, `CONTESTATION_PATERNITE_POSSESSION_ETAT`, `RECHERCHE_PATERNITE_POSSESSION_ETAT`, `RECHERCHE_PATERNITE_EXPERTISE_ADN`, `RECHERCHE_PATERNITE_REFUS_ADN`, `RECHERCHE_PATERNITE_MOTIFS_SERIEUX`, `POSSESSION_ETAT_TRACTATUS`, `POSSESSION_ETAT_FAMA`, `POSSESSION_ETAT_CONTINUE`, `POSSESSION_ETAT_PAISIBLE`, `POSSESSION_ETAT_NON_EQUIVOQUE`, `ADOPTION_FORME`, `ADOPTION_PUPILLE_ETAT`, `ADOPTION_ADOPTANT_MARIE`, `ADOPTION_AGE_ADOPTANT`, `ADOPTION_AGE_ADOPTE`, `RESERVE_CONJOINT_SURVIVANT`, `RAPPORT_QUALITE_HERITIER`, `RAPPORT_DONATION_NOMINALE`, `RAPPORT_VALEUR_PARTAGE`, `RAPPORT_DATE_DONATION` (24 codes FR)

**Total : 43 codes uniques ajoutés.**

---

## 3. Comportement nominal

- Lors d'une analyse de dossier Famille BELGIQUE, le LLM reçoit les 5 codes Famille BE dans la liste autorisée.
- Lors d'une analyse de dossier Immigration FRANCE (JLD, Dublin, CRRV), le LLM reçoit les codes IM21/IM22/IM23 correspondants.
- Lors d'une analyse de dossier Immigration BELGIQUE (9bis, 40bis), le LLM reçoit les codes B9BIS/IM14 correspondants.
- Lors d'une analyse de dossier Famille FRANCE (filiation, succession), le LLM reçoit les codes de filiation, possession d'état, adoption, réserve héréditaire, rapport à succession.

## 4. Cas d'erreur

- Les codes annotés BELGIQUE UNIQUEMENT ne doivent pas être émis pour des dossiers FRANCE.
- Les codes annotés FRANCE UNIQUEMENT ne doivent pas être émis pour des dossiers BELGIQUE.
- Fail-open : si les pièces ne permettent pas de trancher, le LLM n'émet aucun item avec ces codes.

---

## 5. Plan de test

### Tests unitaires backend (verts requis avant push)

**CaseAnalysisServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsFamilleBeCriteraCodes` — le prompt contient `DESU_BE_DATE_SEPARATION`, `DESU_BE_CONSENTEE`, `DESU_BE_DATE_ASSIGNATION`, `F217_DATE_MARIAGE`, `F217_DATE_NOTIFICATION_PROJET` annotés BELGIQUE UNIQUEMENT.
- `systemPrompt_containsJldDublinCrrvCriteraCodes` — le prompt contient `IM21_DATE_NOTIFICATION_PLACEMENT`, `IM22_DATE_NOTIFICATION_DUBLIN`, `IM23_DATE_NOTIFICATION_REFUS`.
- `systemPrompt_containsFiliationAdoptionReserveCriteraCodes` — le prompt contient les codes `PATERNITE_VRAISEMBLABLE`, `ADOPTION_FORME`, `RESERVE_CONJOINT_SURVIVANT`, `RAPPORT_QUALITE_HERITIER`, `POSSESSION_ETAT_TRACTATUS`.

**AiQuestionServiceTest** (3 nouveaux tests) :
- `systemPrompt_containsFamilleBeCriteraCodesForQuestions`
- `systemPrompt_containsJldDublinCrrvCriteraCodesForQuestions`
- `systemPrompt_containsFiliationAdoptionReserveCriteraCodesForQuestions`

### Non-régression
- Tests SF-250-02 à SF-250-09 restent verts.

---

## 6. Tables / endpoints / composants impactés

### Backend (modifiés)
- `CaseAnalysisService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `points_procedure`)
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (section `questions[].critere_code`)

### Frontend (non modifiés)
- Tous les composants `*-section.component.ts` concernés — lecture seule

### Tests (ajoutés)
- `CaseAnalysisServiceTest.java` — 3 nouveaux tests
- `AiQuestionServiceTest.java` — 3 nouveaux tests

---

## 7. Hors-périmètre

- Garde-fou de gouvernance (SF-250-11).
- Modification des composants frontend.
- `docs/PRODUCT_SPEC.md` (mis à jour après merge de la feature complète F-250).

---

## 8. Référence audit

Tableau SF-250-01 sections 3.4, 3.5, 3.6 — outils en statut ⚠️ et outils Famille BE ❌.
Plan de remédiation SF-250-01 section 6.2 — lot SF-250-10.

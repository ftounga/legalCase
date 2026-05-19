# SF-250-01 — Audit exhaustif des `critereCode` F-IA-03

**Feature parente** : F-250 — Complétion de la validation de cohérence (F-IA-03) des outils décisionnels  
**Date** : 2026-05-19  
**Auteur** : Audit outillé (Claude Code SF-250-01)

---

## 1. Méthodologie

### 1.1 Sources analysées

**Frontend :**
- `TOOL_REGISTRY` dans `decisional-tools-panel.component.ts` — 87 entrées recensées
- Chaque composant `*-section.component.ts` — extraction des `critereCode` attendus via :
  - Boucles `for (const chk of procedureChecksSignal())` + filtre `critereCode !== '<CODE>'`
  - Boucles `for (const q of aiQuestionsSignal())` + filtre `critereCode !== '<CODE>'`
  - Constantes `F96_CODE_*` / `RC_CODES` / `CRITERE_CODES` / tableaux de mapping

**Backend :**
- `CaseAnalysisService.java` — prompt `points_procedure` (lignes 52–61) : liste explicite des `critere_code` autorisés
- `AiQuestionService.java` — `SYSTEM_PROMPT_TEMPLATE` (lignes 38–46) : liste explicite des `critere_code` autorisés
- `ProcedureCheckService.java` — parsing (accepte tout code émis par le LLM)
- `AiQuestionService.java` — parsing (accepte tout code émis par le LLM)

### 1.2 Méthode de croisement

Un outil est classé **✅ OPÉRATIONNEL** si tous les `critereCode` qu'il attend sont listés explicitement dans les prompts backend (`CaseAnalysisService` OU `AiQuestionService`).  
**❌ CASSÉ** si au moins un code attendu n'est dans aucun prompt.  
**⚠️ PARTIEL** si certains codes sont couverts mais d'autres manquent.  
**➖ IA-ONLY** si le composant ne lit pas les `critereCode` des `procedureChecks`/`aiQuestions` (cohérence via comparaison directe `aiData` vs saisie avocat) — cross-check F-IA-03 F96/QUESTION_IA non applicable.  
**➖ NO-CHECK** si le composant reçoit `procedureChecks`/`aiQuestions` mais n'y lit aucun `critereCode` (aucun cross-check F-IA-03).

---

## 2. Codes émis par le backend (référence)

### 2.1 `CaseAnalysisService` — `points_procedure` (critère_code déclarés)

**Droit du travail :**
- `FR_CONVOCATION`, `FR_ENTRETIEN`, `FR_DELAI_NOTIFICATION`, `FR_MOTIVATION`, `FR_MOTIF_REEL`, `FR_PROCEDURE_DISCIPLINAIRE`, `FR_ORDRE_LICENCIEMENT`
- `BE_NOTIFICATION`, `BE_PREAVIS`, `BE_MOTIVATION`, `BE_AUDITION`, `BE_NON_DISCRIMINATION`, `BE_PROTECTION_SPECIALE`, `BE_INDEMNITE_MANIFESTE`
- `DT09_TYPE_RUPTURE` (énuméré, `expected_value` obligatoire)

**Droit de la famille :**
- `FR_CHOIX_AVOCATS`, `FR_REDACTION_CONVENTION`, `FR_ENVOI_LRAR`, `FR_DELAI_REFLEXION`, `FR_SIGNATURE_CONVENTION`, `FR_DEPOT_NOTAIRE`, `FR_ENREGISTREMENT`
- `BE_CHOIX_AVOCAT`, `BE_REDACTION_CONVENTION`, `BE_REQUETE_CONJOINTE`, `BE_COMPARUTION`, `BE_JUGEMENT`, `BE_TRANSCRIPTION`
- `FA06_MODE_GARDE` (énuméré)

**Immigration :**
- `IM21_REGULARITE_SEJOUR_FR`, `IM21_DELAI_DEPOT_FR`, `IM21_PIECE_IDENTITE_FR`, `IM21_JUSTIF_DOMICILE_FR`, `IM21_ETAT_CIVIL_FR`, `IM21_PHOTO_FR`, `IM21_TIMBRE_FISCAL_FR`, `IM21_PIECES_MARIAGE_FR`, `IM21_COMMUNAUTE_VIE_FR`, `IM21_RESSOURCES_FR`, `IM21_CONVENTION_ACCUEIL_FR`
- `IM21_REGULARITE_SEJOUR_BE`, `IM21_PIECE_IDENTITE_BE`, `IM21_PIECES_COHABITATION_BE`, `IM21_RESSOURCES_BE`, `IM21_LOGEMENT_BE`, `IM21_ASSURANCE_BE`, `IM21_EXTRAIT_CASIER_BE`
- `IM05_MOTIF` (énuméré), `IM06_RECOURS_TYPE` (énuméré), `IM07_TITRE_TYPE` (énuméré)

**Pièces manquantes (`pieces_manquantes`)** (mêmes codes que ci-dessus + `FR_ACTE_MARIAGE`, `FR_ACTE_NAISSANCE_EPOUX`, `FR_ACTE_NAISSANCE_ENFANTS`, `FR_LIVRET_FAMILLE`, `FR_JUSTIF_DOMICILE`, `FR_CONTRAT_MARIAGE`, `FR_ETAT_PATRIMOINE`, `FR_JUSTIF_REVENUS`, `FR_PIECE_IDENTITE`, `BE_*` équivalents)

### 2.2 `AiQuestionService` — `SYSTEM_PROMPT_TEMPLATE` (critere_code déclarés)

Identique au sous-ensemble `CaseAnalysisService` ci-dessus :  
`FR_CONVOCATION`, `FR_ENTRETIEN`, `FR_DELAI_NOTIFICATION`, `FR_MOTIVATION`, `FR_MOTIF_REEL`, `FR_PROCEDURE_DISCIPLINAIRE`, `FR_ORDRE_LICENCIEMENT`, `BE_NOTIFICATION`, `BE_PREAVIS`, `BE_MOTIVATION`, `BE_AUDITION`, `BE_NON_DISCRIMINATION`, `BE_PROTECTION_SPECIALE`, `BE_INDEMNITE_MANIFESTE`, `FA06_MODE_GARDE`, `IM05_MOTIF`, `IM06_RECOURS_TYPE`, `IM07_TITRE_TYPE`, `DT09_TYPE_RUPTURE`

**Absence notable** : les 18 codes `IM21_*`, `FR_CHOIX_AVOCATS` (étapes divorce), codes RC_*, et TOUS les codes spécifiques aux outils récents (DT13_, DT22_, DT24_, DT31_, DT36_, FA09_, FA11_, FA12_, FA13_, FA14_, FA15_, FA19_, FA20_, FA21_, FA22_, FA25_, FA26_, HLN_, IM08_, IM09*, IM11_, IM12_, IM13_, IM17_, IM19_, IM20_, IM24_, PSE_, AT_MP*, CREDIT_TEMPS*, TESTAMENT_*, PARTAGE_*, DEVOLUTION_*, DONATION_*, INDIVISION_*, PMA_GPA_*, COMMUNAUTE_UNIVERSELLE_*, BE_9TER_*, DESU_BE_*, DA_*, RCI_*, F217_*, FA11_DATE_SIGNATURE_CONVENTION, TRAVAIL_PROCEDURE_TYPE, PROTECTION_RP_MOTIF, INAPTITUDE*) ne sont **jamais** émis par le LLM.

---

## 3. Tableau exhaustif — outil × critereCode attendus × statut

**Légende statut cross-check :**
- ✅ = codes attendus émis par le prompt backend → cross-check opérationnel
- ❌ = codes attendus **non émis** par aucun prompt → cross-check mort (dead code)
- ⚠️ = partiellement couvert
- ➖IA = cohérence IA-only (compare `aiData` vs saisie, sans F96/QUESTION_IA code)
- ➖NO = composant reçoit procedureChecks/aiQuestions mais ne lit aucun critereCode

### 3.1 Domaine Travail (DROIT_DU_TRAVAIL) — France

| tool_id | Composant | critereCode attendu(s) | Statut cross-check | Nature |
|---|---|---|---|---|
| `F-DT-08-licenciement-validity` | `licenciement-section` | `FR_CONVOCATION`, `FR_ENTRETIEN`, `FR_DELAI_NOTIFICATION`, `FR_MOTIVATION`, `FR_MOTIF_REEL`, `FR_PROCEDURE_DISCIPLINAIRE`, `FR_ORDRE_LICENCIEMENT` (FR) ; `BE_NOTIFICATION`, `BE_PREAVIS`, `BE_MOTIVATION`, `BE_AUDITION`, `BE_NON_DISCRIMINATION`, `BE_PROTECTION_SPECIALE`, `BE_INDEMNITE_MANIFESTE` (BE) | ✅ | F96 + QUESTION_IA |
| `F-DT-09-comparateur-indemnites` | `indemnite-comparatif-section` | `DT09_TYPE_RUPTURE` | ✅ | F96 + QUESTION_IA |
| `F-DT-10-rupture-conv-validity` | `rupture-conv-section` | `RC_CONSENTEMENT`, `RC_DELAI_RETRACTATION`, `RC_HOMOLOGATION`, `RC_ASSISTANCE`, `RC_INDEMNITE`, `RC_ENTRETIENS` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-11-harcelement-licenciement-nul` | `harcelement-licenciement-nul-section` | `HLN_MOTIF_NULLITE` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-DT-13-licenciement-economique` | `licenciement-economique-section` | `DT13_MOTIF_ECONOMIQUE`, `DT13_DATE_NOTIFICATION` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-14-pse-validite` | `pse-section` | `PSE_DATE_PROJET` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-DT-15-inaptitude` | `inaptitude-section` | `INAPT_ORIGINE`, `INAPT_RECLASSEMENT` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-16-licenciement-nul-detection` | `licenciement-nul-detection-section` | (IA-only : salaire/date/protections via aiData) | ➖IA | IA-only |
| `F-DT-17-indemnite-precarite-cdd` | `indemnite-precarite-cdd-section` | (IA-only) | ➖IA | IA-only |
| `F-DT-18-fin-mission-interim` | `fin-mission-interim-section` | (IA-only) | ➖IA | IA-only |
| `F-DT-19-heures-sup` | `heures-sup-section` | (IA-only : TAUX_HORAIRE, HEURES_SUP, SALAIRE_DEDUIT via aiData, piecesManquantes via codes non-F96) | ➖IA | IA-only |
| `F-DT-20-rappel-salaire` | `rappel-salaire-section` | (IA-only : SALAIRE, CONVENTION via aiData) | ➖IA | IA-only |
| `F-DT-21-travail-dissimule` | `travail-dissimule-section` | (IA-only) | ➖IA | IA-only |
| `F-DT-22-requalification-cdd-cdi` | `requalification-cdd-cdi-section` | `DT22_SALAIRE`, `SALAIRE_BRUT_MENSUEL` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-23-requalification-interim-cdi` | `requalification-interim-cdi-section` | `DT23_SALAIRE`, `SALAIRE_BRUT_MENSUEL` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-24-non-concurrence` | `non-concurrence-section` | `DT24_SALAIRE`, `SALAIRE_BRUT_MENSUEL` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-25-indemnite-preavis` | `indemnite-preavis-section` | (IA-only : SALAIRE, DATE_RUPTURE, CONVENTION via aiData) | ➖IA | IA-only |
| `F-DT-26-conges-payes-indemnite` | `conges-payes-section` | (IA-only : SALAIRE_MENSUEL, SALAIRE_DEDUIT, DATE_RUPTURE via aiData) | ➖IA | IA-only |
| `F-DT-30-protection-rp` | `protection-rp-section` | `PROTECTION_RP_MOTIF` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-DT-31-transaction` | `transaction-section` | `DT31_SALAIRE_MENSUEL`, `SALAIRE_BRUT_MENSUEL`, `DT31_ANCIENNETE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-32-documents-fin-contrat` | `documents-fin-contrat-section` | (IA-only : SALAIRE, DATE_FIN_CONTRAT via aiData) | ➖IA | IA-only |
| `F-DT-33-at-mp` | `at-mp-section` | `AT_MP_DATE_ACCIDENT`, `DATE_ACCIDENT` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-34-refere-prudhomal` | `refere-prudhomal-section` | (IA-only : DATE_MISE_EN_DEMEURE, ANCIENNETE via aiData ; piecesManquantes via `MISE_EN_DEMEURE`, `DT34_DATE_MISE_EN_DEMEURE`, `DT34_ANCIENNETE`) | ➖IA | IA-only |
| `F-DT-35-contestation-are-fr` | `contestation-are-section` | (IA-only : DATE_NOTIFICATION via aiData) | ➖IA | IA-only |
| `F-DT-36-procedure-nullite-licenciement` | `procedure-nullite-licenciement-section` | `DT36_DATE_ENTRETIEN`, `DT36_MOTIVATION`, `DT36_ENTRETIEN_TENU` | ❌ | F96 + QUESTION_IA — codes **jamais** émis (cas déclencheur F-250) |
| `F-136-travail-procedure` | `travail-procedure-section` | `TRAVAIL_PROCEDURE_TYPE` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-132-rupture-conv-indemnite` | `rupture-conv-indemnite-section` | `RCI_SALAIRE`, `RCI_ANCIENNETE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-DT-03-prescription-litige` | `case-deadlines-section` | aucun critereCode | ➖NO | Aucun check |
| `F-DT-04-fiche-prudhomale` | `prudhome-fiche-section` | (reçoit procedureChecks/aiQuestions, logique custom checklist) | ➖NO | Checklist custom (pas critereCode F-IA-03) |
| `F-DT-06-requete-tribunal-travail` | `tribunal-travail-fiche-section` | (reçoit procedureChecks/aiQuestions, logique custom) | ➖NO | Checklist custom |
| `F-DT-07-anciennete-conges-prime` | `anciennete-section` | aucun | ➖NO | Calculateur pur |

### 3.2 Domaine Travail — Belgique

| tool_id | Composant | critereCode attendu(s) | Statut cross-check | Nature |
|---|---|---|---|---|
| `F-DT-27-motif-grave-be` | `motif-grave-be-section` | (IA-only : DATE_RUPTURE, SALAIRE via aiData) | ➖IA | IA-only |
| `F-DT-28-avantages-conventionnels-be` | `avantages-conventionnels-be-section` | (IA-only : SALAIRE via aiData) | ➖IA | IA-only |
| `F-DT-29-credit-temps-be` | `credit-temps-be-section` | `CREDIT_TEMPS_ANCIENNETE`, `CREDIT_TEMPS_AGE` | ❌ | F96 + QUESTION_IA — codes jamais émis |

### 3.3 Domaine Immigration (DROIT_DE_L_IMMIGRATION) — France

| tool_id | Composant | critereCode attendu(s) | Statut cross-check | Nature |
|---|---|---|---|---|
| `F-IM-05-arbre-decisionnel-titre` | `immigration-title-decision-section` | `IM05_MOTIF` | ✅ | F96 + QUESTION_IA |
| `F-IM-06-recours` | `immigration-recours-section` | `IM06_RECOURS_TYPE` | ✅ | F96 + QUESTION_IA |
| `F-IM-07-droit-au-travail` | `immigration-work-right-section` | `IM07_TITRE_TYPE` | ✅ | F96 + QUESTION_IA |
| `F-IM-08-oqtf-avec-delai-fr` | `oqtf-avec-delai-section` | `IM08_MOTIF_OQTF`, `IM08_RECOURS_FORME` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-IM-08-oqtf-sans-delai-fr` | `oqtf-sans-delai-section` | `IM08_RECOURS_FORME` | ❌ | QUESTION_IA — code jamais émis |
| `F-IM-08-referes-admin-fr` | `referes-admin-section` | `IM08RA_DECISION_CONTESTEE` | ❌ | F96 — code jamais émis |
| `F-IM-09-aes-metiers-tension` | `aes-metiers-tension-section` | `IM09_DATE_ENTREE_FRANCE`, `IM09_MOIS_ACTIVITE` | ❌ | F96 — codes jamais émis |
| `F-IM-09-aes-famille` | `aes-famille-section` | (reçoit procedureChecks/aiQuestions — à vérifier) | ⚠️ | À approfondir |
| `F-IM-09-aes-etudiant` | `aes-etudiant-section` | `IM09_ETU_DATE_ENTREE_FRANCE`, `IM09_ETU_DUREE_PRESENCE`, `IM09_ETU_DATE_DEPOT_DEMANDE` | ❌ | F96 — codes jamais émis |
| `F-IM-09-aes-humanitaire` | `aes-humanitaire-section` | `IM09H_DATE_ENTREE_FRANCE`, `IM09H_MOTIF_HUMANITAIRE` | ❌ | F96 — codes jamais émis |
| `F-IM-11-changement-statut` | `changement-statut-section` | `IM11_TITRE_ACTUEL` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-IM-12-asile-avance` | `asile-avance-section` | `IM12_DISPOSITIF_ASILE` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-IM-13-naturalisation` | `naturalisation-section` | `IM13_VOIE_NATURALISATION` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-IM-19-mineurs` | `mineurs-immigration-section` | `IM19_DATE_NAISSANCE`, `IM19_DATE_ENTREE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-IM-20-mesures-eloignement` | `mesures-eloignement-section` | `IM20_DISPOSITIF_ELOIGNEMENT`, `IM20_MOTIF_MENACE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-IM-21-jld-retention-fr` | `jld-retention-section` | (à vérifier — composant complet SF-208-05) | ⚠️ | À approfondir |
| `F-IM-22-dublin-recours-fr` | `dublin-recours-section` | (critereCode F96 présent) | ⚠️ | À approfondir |
| `F-IM-23-crrv-refus-visa-fr` | `crrv-refus-visa-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-IM-24-victime-violences-l4256-fr` | `victime-violences-section` | `IM24_DATE_ORDONNANCE_PROTECTION` | ❌ | code jamais émis |
| `F-IM-01-checklist-pieces` | `immigration-checklist-section` | aucun critereCode F-IA-03 | ➖NO | Checklist statique |

### 3.4 Domaine Immigration — Belgique

| tool_id | Composant | critereCode attendu(s) | Statut cross-check | Nature |
|---|---|---|---|---|
| `F-IM-08-annexe13-be` | `annexe13-be-section` | `IM08_MOTIF_OQT_BE`, `IM08_MOTIF_OQT` | ❌ | F96 — codes jamais émis |
| `F-IM-14-40ter-familial-belge-be` | `belgian-40ter-section` | (à vérifier détail) | ⚠️ | À approfondir |
| `F-IM-14-9bis-humanitaire-be` | `belgian-9bis-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-IM-14-9ter-medical-be` | `belgian-9ter-section` | `BE_9TER_MALADIE_GRAVE`, `BE_9TER_SOINS_BE`, `BE_9TER_SOINS_INACCESSIBLES`, `BE_9TER_MENACE_ORDRE_PUBLIC` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-IM-14-40bis-cohabitant-ue-be` | `belgian-40bis-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-IM-17-regime-algerien` | `regime-algerien-section` | `IM17_VOIE_REGIME_ALGERIEN` | ❌ | F96 + QUESTION_IA — code jamais émis |

### 3.5 Domaine Famille (DROIT_DE_LA_FAMILLE) — France

| tool_id | Composant | critereCode attendu(s) | Statut cross-check | Nature |
|---|---|---|---|---|
| `F-FA-05-partage-immobilier` | `partage-immobilier-section` | `FA05_VALEUR_VENALE`, `FA05_CAPITAL_RESTANT` | ❌ | codes jamais émis (hors `FA05_VALEUR_VENALE` listé dans `source_explanations` CaseAnalysisService mais pas dans `points_procedure`/`aiQuestions`) |
| `F-FA-06-calendrier-garde` | `calendrier-garde-section` | `FA06_MODE_GARDE` | ✅ | F96 + QUESTION_IA |
| `F-FA-07-checklist-divorce` | `divorce-checklist-section` | `FR_CHOIX_AVOCATS`, `FR_REDACTION_CONVENTION`, …, `FR_ENREGISTREMENT` (7 étapes FR) ; `BE_*` (6 étapes BE) ; `FR_ACTE_MARIAGE` … (9 pièces FR) ; `BE_*` (8 pièces BE) | ✅ | F96 + QUESTION_IA (codes présents dans les 2 prompts) |
| `F-FA-08-divorce-alteration` | `divorce-alteration-section` | `DA_DUREE_MARIAGE` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-FA-09-divorce-faute` | `divorce-faute-section` | `FA09_DUREE_MARIAGE`, `FA09_DATE_DEPOT_ASSIGNATION`, `FA09_FAUTES_INVOQUEES` | ❌ | F96 — codes jamais émis |
| `F-FA-10-divorce-accepte` | `divorce-accepte-section` | (à vérifier détail) | ⚠️ | À approfondir |
| `F-FA-12-mesures-provisoires` | `mesures-provisoires-section` | `FA12_DATE_AUDIENCE`, `FA12_VIOLENCES` | ❌ | F96 — codes jamais émis |
| `F-FA-13-revisions-post-divorce` | `revisions-post-divorce-section` | `FA13_NB_ENFANTS` | ❌ | F96 — code jamais émis |
| `F-FA-14-ordonnance-protection` | `ordonnance-protection-section` | `FA14_DATE_REQUETE`, `FA14_VIOLENCES_ALLEGUEES`, `FA14_LOGEMENT_COMMUN` | ❌ | F96 — codes jamais émis |
| `F-FA-15-recompenses` | `recompenses-section` | `FA15_REGIME_MATRIMONIAL` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `F-FA-16-communaute-universelle` | `communaute-universelle-section` | `COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE`, `COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-17-partage-judiciaire` | `partage-judiciaire-section` | `PARTAGE_JUDICIAIRE_PV`, `PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-18-reconnaissance-paternelle` | `reconnaissance-paternelle-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-18-contestation-paternite` | `contestation-paternite-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-18-recherche-paternite` | `recherche-paternite-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-18-possession-etat` | `possession-etat-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-18-adoption` | `adoption-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-19-autorite-parentale` | `autorite-parentale-section` | `FA19_REGIME_EXERCICE_ACTUEL`, `FA19_DANGER_CARACTERISE`, `FA19_CONSENTEMENT_AUTRE_PARENT`, `FA19_INTERFERENCE_VIE_ENFANT`, `FA19_AGE_ENFANTS` | ❌ | F96 — codes jamais émis |
| `F-FA-19-changement-residence` | `changement-residence-section` | `FA19_RAISON_CHANGEMENT`, `FA19_CONSENTEMENT_AUTRE_PARENT`, `FA19_INFORME_PREALABLEMENT`, `FA19_MODE_RESIDENCE_ACTUEL`, `FA19_AGE_ENFANTS` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-19-desaccords-parentaux` | `desaccords-parentaux-section` | `FA19_DOMAINE_DESACCORD`, `FA19_INTENSITE_DESACCORD`, `FA19_TENTATIVES_MEDIATION`, `FA19_AGE_ENFANTS_CONCERNES`, `FA19_URGENCE` | ❌ | F96 — codes jamais émis |
| `F-FA-20-pacs-dissolution` | `pacs-dissolution-section` | `FA20_MODE_DISSOLUTION`, `FA20_REGIME_BIENS`, `FA20_CREANCES_ALLEGUEES` | ❌ | F96 — codes jamais émis |
| `F-FA-21-separation-corps` | `separation-corps-section` | `FA21_DATE_JUGEMENT_SEPARATION` | ❌ | F96 — code jamais émis |
| `F-FA-22-indivision` | `indivision-section` | `FA22_DATE_ORIGINE`, `FA22_OCCUPATION` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-24-devolution-legale` | `devolution-legale-section` | `DEVOLUTION_LEGALE_CONJOINT`, `DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-24-testament-validite` | `testament-validite-section` | `TESTAMENT_FORME`, `TESTAMENT_SAINE_ESPRIT`, `TESTAMENT_QUOTITE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-24-donation` | `donation-section` | `DONATION_FORME`, `DONATION_SAINE_ESPRIT`, `DONATION_QUOTITE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-24-reserve-heriditaire` | `reserve-heriditaire-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-24-partage-successoral` | `partage-successoral-section` | `PARTAGE_MODE`, `PARTAGE_CONSENTEMENTS`, `PARTAGE_PRESENCE_IMMEUBLES` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-24-indivision-successorale` | `indivision-successorale-section` | `INDIVISION_DATE_OUVERTURE`, `INDIVISION_TYPE` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-24-rapport-succession` | `rapport-succession-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-25-majeurs-proteges` | `majeurs-proteges-section` | `FA25_DATE_CERTIFICAT`, `FA25_ALT_MENTALES`, `FA25_CONSENTEMENT`, `FA25_DEMANDEUR_FAMILIAL` | ❌ | F96 — codes jamais émis |
| `F-FA-26-changement-etat-civil` | `changement-etat-civil-section` | `FA26_TYPE_CHANGEMENT`, `FA26_MOTIF_INVOQUE`, `FA26_DATE_NAISSANCE`, `FA26_MAJEUR_DEMANDEUR`, `FA26_CONSENTEMENT_PARENTAL` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `F-FA-27-pma-gpa` | `pma-gpa-bioethique-section` | `PMA_GPA_DISPOSITIF` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `mediation-familiale-pre-saisine` | `mediation-familiale-section` | (à vérifier) | ⚠️ | À approfondir |
| `acceptation-renonciation-succession` | `acceptation-renonciation-section` | (à vérifier) | ⚠️ | À approfondir |
| `F-FA-01-prestation-compensatoire` | `prestation-compensatoire-section` | aucun critereCode | ➖NO | Présentation pure |
| `F-FA-02-pension-alimentaire` | `pension-alimentaire-section` | aucun critereCode | ➖NO | Présentation pure |
| `F-FA-04-liquidation-communaute` | `liquidation-communaute-section` | aucun critereCode | ➖NO | Présentation pure |
| `F-152-divorce-consentement-scoring` | `divorce-cm-scoring-section` | aucun critereCode | ➖NO | Scoring pur (présentation) |
| `F-153-fourchettes-jaf` | `fourchettes-jaf-section` | aucun critereCode | ➖NO | Présentation JAF |
| `F-132-rupture-amiable-info` | `rupture-amiable-info-section` | aucun critereCode | ➖NO | Informationnel |

### 3.6 Domaine Famille — Belgique

| tool_id | Composant | critereCode attendu(s) | Statut cross-check | Nature |
|---|---|---|---|---|
| `F-FA-11-desunion-irremediable-be` | `divorce-desunion-be-section` | `DESU_BE_DATE_SEPARATION`, `DESU_BE_CONSENTEE`/`DESU_BE_SEPARATION_CONSENTUE`, `DESU_BE_DATE_ASSIGNATION` | ❌ | F96 + QUESTION_IA — codes jamais émis |
| `divorce-dc-be` | `divorce-dc-be-section` | (IA-only : DATE_SIGNATURE_CONVENTION via aiData) | ➖IA | IA-only |
| `divorce-ddi-3voies-be` | `divorce-ddi-be-section` | aucun critereCode | ➖NO | Wrapper informationnel |
| `tribunal-famille-be-mesures-prov` | (wrapper simple) | aucun critereCode | ➖NO | Wrapper informationnel |
| `pacte-successoral-be-2018` | (wrapper simple) | aucun critereCode | ➖NO | Wrapper informationnel |
| `regime-mat-be-communaute-legale` | `regime-communaute-legale-be-section` | `F217_DATE_MARIAGE` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `liquidation-partage-be` | `liquidation-partage-be-section` | `F217_DATE_NOTIFICATION_PROJET` | ❌ | F96 + QUESTION_IA — code jamais émis |
| `autorite-parentale-be` | `autorite-parentale-be-section` | aucun critereCode | ➖NO | Formulaire pur |
| `contribution-alimentaire-enfants-be` | `contribution-alimentaire-enfants-be-section` | aucun critereCode | ➖NO | Calculateur pur |
| `contribution-conjoint-be` | `contribution-conjoint-be-section` | aucun critereCode | ➖NO | Calculateur pur |

---

## 4. Synthèse chiffrée

### 4.1 Comptage global

| Catégorie | Nb outils | % |
|---|---|---|
| Total outils dans TOOL_REGISTRY | 87 | 100 % |
| ✅ Cross-check F96/QUESTION_IA **opérationnel** | 9 | 10 % |
| ❌ Cross-check F96/QUESTION_IA **cassé** (codes attendus, jamais émis) | **44** | **51 %** |
| ⚠️ Statut à approfondir (composant présent, codes à vérifier finement) | 13 | 15 % |
| ➖IA IA-only (cohérence directe aiData, pas de F96/QUESTION_IA code) | 16 | 18 % |
| ➖NO Aucun cross-check F-IA-03 (presentationnel / calculateur) | 5 | 6 % |

**Bilan : 44 outils ont leur cross-check F-IA-03 (sources F96/QUESTION_IA) définitivement cassé — le LLM n'émet jamais les codes attendus.**

### 4.2 Les 9 outils opérationnels (✅)

1. `F-DT-08-licenciement-validity` — FR_CONVOCATION / FR_ENTRETIEN / ... / BE_*
2. `F-DT-09-comparateur-indemnites` — DT09_TYPE_RUPTURE
3. `F-IM-05-arbre-decisionnel-titre` — IM05_MOTIF
4. `F-IM-06-recours` — IM06_RECOURS_TYPE
5. `F-IM-07-droit-au-travail` — IM07_TITRE_TYPE
6. `F-FA-06-calendrier-garde` — FA06_MODE_GARDE
7. `F-FA-07-checklist-divorce` — FR_CHOIX_AVOCATS / FR_ACTE_MARIAGE / BE_*

> **Remarque :** F-IM-21 (JLD, Dublin, CRRV, Victime violences) utilise les codes `IM21_*` qui sont présents dans `points_procedure` de `CaseAnalysisService` mais absents du prompt `AiQuestionService`. Ces 4 outils sont **partiellement opérationnels** (F96 ✅, QUESTION_IA ❌ pour les codes IM21_*).

### 4.3 Répartition par domaine × pays des outils ❌ CASSÉS

| Domaine | Pays | Nb outils cassés |
|---|---|---|
| Travail | FR | 17 (F-DT-10, 11, 13, 14, 15, 22, 23, 24, 30, 31, 33, 36, F-136, F-132-RCI, + 3 outils dates-only) |
| Travail | BE | 3 (F-DT-29, F-DT-27*, F-DT-28*) — *IA-only non compté |
| Immigration | FR | 10 (F-IM-08-OQTF×2, referes, AES×3, IM-11, IM-12, IM-13, IM-19, IM-20, IM-24) |
| Immigration | BE | 3 (annexe13, 9ter, IM-17) |
| Famille | FR | 21 (FA-05, 08, 09, 12, 13, 14, 15, 16, 17, 19×3, 20, 21, 22, 24×4, 25, 26, 27) |
| Famille | BE | 3 (FA-11, regime-mat-be, liquidation-partage-be) |

**Note :** Les outils ➖IA (16) ne sont pas comptés comme "cassés" — leur cohérence IA-only fonctionne via comparaison directe `aiData` vs saisie ; ils n'utilisent simplement pas les `critereCode` F96/QUESTION_IA. Ils ne sont pas touchés par F-250 (pas de dette de codes).

### 4.4 Nature des codes manquants (catégories)

| Famille de codes | Nb codes uniques manquants | Outils impactés |
|---|---|---|
| DT36_* (F-DT-36) | 3 | 1 |
| FA19_* (3 outils F-FA-19) | 10 | 3 |
| FA26_* (F-FA-26) | 5 | 1 |
| FA09_* / FA12_* / FA13_* / FA14_* / FA15_* / FA20_* / FA21_* / FA22_* / FA25_* | 16 | 9 |
| TESTAMENT_* / DONATION_* / PARTAGE_* / INDIVISION_* / DEVOLUTION_* | 11 | 5 |
| HLN_* / DT13_* / DT22_* / DT23_* / DT24_* / DT31_* / RC_* / RCI_* | 14 | 7 |
| IM08_* / IM09_* / IM11_* / IM12_* / IM13_* / IM17_* / IM19_* / IM20_* / IM24_* | 12 | 11 |
| IM08_*_BE / BE_9TER_* / DESU_BE_* / F217_* | 8 | 5 |
| PSE_* / AT_MP_* / CREDIT_TEMPS_* / PROTECTION_RP_* / TRAVAIL_PROCEDURE_TYPE | 6 | 5 |
| COMMUNAUTE_UNIVERSELLE_* / PMA_GPA_* / FA08_DA_* | 4 | 3 |
| **TOTAL** | **~89** | **~44** |

---

## 5. Analyse de la cause racine

### 5.1 Architecture du problème

Le mécanisme F-IA-03 repose sur un **contrat implicite non enforced** :
- **Frontend** : code en dur les `critereCode` qu'il attend dans le composant (ex. `DT36_DATE_ENTRETIEN`)
- **Backend prompt** : le LLM doit émettre ces codes dans `points_procedure[].critere_code` ou `questions[].critere_code`
- **Absence de garde-fou** : aucun test d'intégrité ne vérifie que chaque code attendu frontend est listé dans un prompt backend

### 5.2 Trois classes d'outils

**Classe A — Opérationnels (9)** : les codes ont été ajoutés au prompt au moment de la livraison de l'outil (pratique appliquée pour F-DT-08, F-FA-07, F-IM-05/06/07, etc.).

**Classe B — IA-only (16)** : la cohérence est implémentée par comparaison directe `aiData` vs saisie. Pas de code attendu dans `procedureChecks`/`aiQuestions`. Non cassés — fonctionnels par construction différente.

**Classe C — Cassés (44)** : les `critereCode` ont été codés dans le composant frontend lors de la livraison de l'outil (SF de pré-fill/F-IA-03), **sans** extension simultanée du prompt backend pour émettre ces codes. Dette de couplage frontend ↔ backend.

### 5.3 Cas spécifique F-DT-36 (déclencheur signal utilisateur)

- Commit `8fe123af` (2026-05-17) : livraison frontend SF-246-01 — composant `procedure-nullite-licenciement-section` avec codes `DT36_DATE_ENTRETIEN`, `DT36_MOTIVATION`, `DT36_ENTRETIEN_TENU`
- Aucun commit backend correspondant — cross-check né débranché
- Confirmation par l'utilisateur le 2026-05-19 : "la validation IA ne marche plus sur l'outil de nullité de procédure"
- Statut : **dette native** (jamais fonctionné), pas une régression

---

## 6. Plan de remédiation — découpage SF-250-02..0N

### 6.1 Principe commun à toutes les SF de remédiation

Pour chaque lot : étendre le prompt LLM (`CaseAnalysisService` lignes 52–61 + `AiQuestionService` lignes 38–46) pour lister explicitement les nouveaux `critere_code` autorisés, avec la sémantique exacte attendue. Ajouter des fixtures de parsing (tests unitaires `ProcedureCheckServiceTest` / `AiQuestionServiceTest`).

### 6.2 Vagues proposées

| SF | Lot | Outils couverts | Codes à ajouter au prompt | Difficulté |
|---|---|---|---|---|
| **SF-250-02** | Travail FR — outils validité/procédure | F-DT-36, F-DT-11, F-DT-13, F-DT-14, F-DT-30 | `DT36_DATE_ENTRETIEN`, `DT36_MOTIVATION`, `DT36_ENTRETIEN_TENU`, `HLN_MOTIF_NULLITE`, `DT13_MOTIF_ECONOMIQUE`, `DT13_DATE_NOTIFICATION`, `PSE_DATE_PROJET`, `PROTECTION_RP_MOTIF` | Faible — codes binaires clairs |
| **SF-250-03** | Travail FR — rupture/indemnités/requalifications | F-DT-10 (RC_*), F-DT-22, F-DT-23, F-DT-24, F-DT-31, F-DT-15, F-DT-33 | `RC_CONSENTEMENT`, `RC_DELAI_RETRACTATION`, `RC_HOMOLOGATION`, `RC_ASSISTANCE`, `RC_INDEMNITE`, `RC_ENTRETIENS`, `DT22_SALAIRE`, `DT23_SALAIRE`, `DT24_SALAIRE`, `DT31_SALAIRE_MENSUEL`, `DT31_ANCIENNETE`, `RCI_SALAIRE`, `RCI_ANCIENNETE`, `INAPT_ORIGINE`, `INAPT_RECLASSEMENT`, `AT_MP_DATE_ACCIDENT` | Moyen — codes énumérés RC_* |
| **SF-250-04** | Travail FR/BE divers + F-136 | F-DT-29, F-136, F-132-RCI (déjà en SF-250-03) | `CREDIT_TEMPS_ANCIENNETE`, `CREDIT_TEMPS_AGE`, `TRAVAIL_PROCEDURE_TYPE` | Faible |
| **SF-250-05** | Immigration FR — OQTF/referes/AES | F-IM-08-OQTF×2, F-IM-08-referes, F-IM-09-AES×3 (metiers, etudiant, humanitaire) | `IM08_MOTIF_OQTF`, `IM08_RECOURS_FORME`, `IM08RA_DECISION_CONTESTEE`, `IM09_DATE_ENTREE_FRANCE`, `IM09_MOIS_ACTIVITE`, `IM09_ETU_DATE_ENTREE_FRANCE`, `IM09_ETU_DUREE_PRESENCE`, `IM09_ETU_DATE_DEPOT_DEMANDE`, `IM09H_DATE_ENTREE_FRANCE`, `IM09H_MOTIF_HUMANITAIRE` | Moyen |
| **SF-250-06** | Immigration FR — titres/asile/éloignement | F-IM-11, F-IM-12, F-IM-13, F-IM-19, F-IM-20, F-IM-24 | `IM11_TITRE_ACTUEL`, `IM12_DISPOSITIF_ASILE`, `IM13_VOIE_NATURALISATION`, `IM19_DATE_NAISSANCE`, `IM19_DATE_ENTREE`, `IM20_DISPOSITIF_ELOIGNEMENT`, `IM20_MOTIF_MENACE`, `IM24_DATE_ORDONNANCE_PROTECTION` | Moyen |
| **SF-250-07** | Immigration BE — codes OQT/9ter/algérien | F-IM-08-annexe13, F-IM-14-9ter, F-IM-17, + audit F-IM-14-40ter / 9bis / 40bis | `IM08_MOTIF_OQT_BE`, `BE_9TER_MALADIE_GRAVE`, `BE_9TER_SOINS_BE`, `BE_9TER_SOINS_INACCESSIBLES`, `BE_9TER_MENACE_ORDRE_PUBLIC`, `IM17_VOIE_REGIME_ALGERIEN` | Faible–moyen |
| **SF-250-08** | Famille FR — divorce/union (sauf successions) | F-FA-08, F-FA-09, F-FA-10, F-FA-12, F-FA-13, F-FA-14, F-FA-15, F-FA-16, F-FA-19×3, F-FA-20, F-FA-21, F-FA-22 | `DA_DUREE_MARIAGE`, `FA09_DUREE_MARIAGE`, `FA09_FAUTES_INVOQUEES`, `FA09_DATE_DEPOT_ASSIGNATION`, `FA12_DATE_AUDIENCE`, `FA12_VIOLENCES`, `FA13_NB_ENFANTS`, `FA14_DATE_REQUETE`, `FA14_VIOLENCES_ALLEGUEES`, `FA14_LOGEMENT_COMMUN`, `FA15_REGIME_MATRIMONIAL`, `COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE`, `COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS`, `FA19_*` (8 codes), `FA20_*` (3), `FA21_DATE_JUGEMENT_SEPARATION`, `FA22_DATE_ORIGINE`, `FA22_OCCUPATION`, `PARTAGE_JUDICIAIRE_PV`, `PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE` | Élevé — beaucoup de codes |
| **SF-250-09** | Famille FR — successions/libéralités | F-FA-24-testament, donation, partage-suc, indivision-suc, devolution, F-FA-25, F-FA-26, F-FA-27, F-FA-05 | `TESTAMENT_FORME`, `TESTAMENT_SAINE_ESPRIT`, `TESTAMENT_QUOTITE`, `DONATION_FORME`, `DONATION_SAINE_ESPRIT`, `DONATION_QUOTITE`, `PARTAGE_MODE`, `PARTAGE_CONSENTEMENTS`, `PARTAGE_PRESENCE_IMMEUBLES`, `INDIVISION_DATE_OUVERTURE`, `INDIVISION_TYPE`, `DEVOLUTION_LEGALE_CONJOINT`, `DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS`, `FA25_*` (4), `FA26_*` (5), `PMA_GPA_DISPOSITIF`, `FA05_VALEUR_VENALE`, `FA05_CAPITAL_RESTANT` | Élevé |
| **SF-250-10** | Famille BE + outils F-243/F-217 + audit outils ⚠️ | F-FA-11, regime-mat-be, liquidation-partage-be + outils en statut ⚠️ affinés | `DESU_BE_DATE_SEPARATION`, `DESU_BE_CONSENTEE`, `DESU_BE_DATE_ASSIGNATION`, `F217_DATE_MARIAGE`, `F217_DATE_NOTIFICATION_PROJET` | Faible–moyen |
| **SF-250-11** | Garde-fou de gouvernance (test d'intégrité) | Tous | Aucun code supplémentaire — test de non-régression : `CritereCodeIntegrityIT` vérifiant que chaque `critereCode` frontend déclaré est couvert par un prompt backend | Élevé (architecture) |

### 6.3 Résumé du découpage

- **10 SF de remédiation** (SF-250-02 à SF-250-11)
- **8 SF de vague prompt** (SF-250-02 à SF-250-09) : extension du prompt LLM avec nouveaux codes + tests unitaires
- **1 SF audit complémentaire** (SF-250-10) : finaliser les 13 outils ⚠️ + codes Famille BE
- **1 SF garde-fou** (SF-250-11) : test d'intégrité automatisé qui détecte toute future régression

### 6.4 Priorisation recommandée

| Priorité | SF | Justification |
|---|---|---|
| P1 — Immédiat | SF-250-02 | Déclencheur signal utilisateur (F-DT-36) + 4 autres outils validité travail très utilisés |
| P1 — Immédiat | SF-250-11 | Garde-fou — évite toute nouvelle dette du même type |
| P2 — Court terme | SF-250-03 + SF-250-08 | Outils rupture et divorce — fort volume cabinets |
| P3 — Moyen terme | SF-250-05 + SF-250-06 | Immigration FR |
| P3 — Moyen terme | SF-250-09 | Successions — domaine en croissance |
| P4 — Lot final | SF-250-04 + SF-250-07 + SF-250-10 | Compléments BE + outils moins fréquents |

---

## 7. Invariants à respecter dans toutes les SF de remédiation

1. **Un code = une sémantique stricte** : documenter dans le prompt ce que "oui" signifie pour chaque code (critère respecté vs étape faite vs type confirmé).
2. **Codes binaires** : `expected_value` reste null ; le statut `VERIFIED`/`NON_COMPLIANT` porte le signal.
3. **Codes énumérés** : `expected_value` obligatoire (ex. `DT09_TYPE_RUPTURE` = `RUPTURE_CONVENTIONNELLE`).
4. **Fail-open backend** : le parseur `ProcedureCheckService.parsePointsProcedure()` et `AiQuestionService.parseQuestions()` acceptent déjà tout code — pas de modification de ces parseurs nécessaire.
5. **Isolation domaine** : les codes d'un domaine (DROIT_DU_TRAVAIL) ne doivent pas interférer avec les autres — le prompt est contextualisé par `workspace.legalDomain`.
6. **Test de non-régression** : chaque SF inclut un test unitaire vérifiant que le LLM (mock) émet bien le code attendu avec la bonne sémantique.

---

## 8. Composants impactés (préoccupation transversale — outil décisionnel métier)

### Backend
- `CaseAnalysisService.java` (lignes 48–61) — `pieces_manquantes` + `points_procedure` critere_code list
- `AiQuestionService.java` (lignes 38–46) — `SYSTEM_PROMPT_TEMPLATE` critere_code list

### Frontend (lecture seule — aucune modification requise en phase audit)
- 44 composants `*-section.component.ts` concernés par les codes ❌
- `decisional-tools-panel.component.ts` — TOOL_REGISTRY (aucune modification)

### Tests
- `CaseAnalysisServiceTest.java`
- `AiQuestionServiceTest.java`
- `ProcedureCheckServiceTest.java`

---

*Document produit dans le cadre de SF-250-01 — Audit exhaustif F-250. Aucun code modifié dans cette SF.*

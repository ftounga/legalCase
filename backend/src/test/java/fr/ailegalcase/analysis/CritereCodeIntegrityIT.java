package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-250-11 — Garde-fou d'intégrité des {@code critereCode} F-IA-03 (F-250).
 *
 * <p>Détecte toute divergence entre les codes déclarés côté frontend (composants
 * {@code *-section}) et les codes couverts par les prompts LLM backend
 * ({@link CaseAnalysisService#SYSTEM_PROMPT_TEMPLATE} et
 * {@link AiQuestionService#SYSTEM_PROMPT_TEMPLATE}).</p>
 *
 * <p><b>Principe</b> : ce test ne démarre PAS le contexte Spring — il lit
 * directement les constantes {@code package-private} des deux services (même
 * package {@code fr.ailegalcase.analysis}). Résultat : exécution instantanée
 * sans base de données, sans mock OAuth2.</p>
 *
 * <p><b>Règle de maintenance</b> : quand un nouveau {@code critereCode} est
 * ajouté côté frontend, le développeur DOIT :
 * <ol>
 *   <li>Ajouter le code à {@link #KNOWN_FRONTEND_CRITERE_CODES} dans ce fichier.</li>
 *   <li>Ajouter le code aux deux prompts backend (SF correspondante).</li>
 * </ol>
 * Si l'étape 2 est oubliée, ce test échoue en CI avec un message listant les
 * codes orphelins. C'est précisément son rôle.</p>
 *
 * <p><b>Codes exclus</b> : certaines chaînes ressemblant à des {@code critereCode}
 * dans les sources frontend sont en réalité des field-names internes (ex :
 * {@code REVENUS_DEMANDEUR} qui est résolu en {@code FA09_REVENUS_DEMANDEUR} ou
 * {@code FA12_REVENUS_DEMANDEUR} selon le composant) ou des aliases legacy tolérés
 * (ex : {@code IM08_MOTIF_OQT} alias de {@code IM08_MOTIF_OQT_BE}). Ces codes
 * n'apparaissent pas dans la liste ci-dessous car ils ne correspondent pas à des
 * codes attendus du prompt LLM.</p>
 *
 * <p>Inspiré du pattern {@link BuilderPatternEnforcementIT} (F-234 SF-234-02)
 * et de {@link DecisionToolVisibilityIntegrityIT} (SF-164-01).</p>
 */
class CritereCodeIntegrityIT {

    /**
     * Source de vérité de gouvernance — liste exhaustive des {@code critereCode}
     * déclarés par les composants décisionnels frontend et attendus du prompt LLM.
     *
     * <p>Cette liste doit être maintenue à jour à chaque ajout de composant
     * ou de code côté frontend. Elle est organisée par domaine métier pour
     * faciliter les revues.</p>
     */
    static final List<String> KNOWN_FRONTEND_CRITERE_CODES = List.of(

            // ─── Droit du travail — Validité licenciement F-DT-08 (FR + BE) ────────────
            "FR_CONVOCATION",
            "FR_ENTRETIEN",
            "FR_DELAI_NOTIFICATION",
            "FR_MOTIVATION",
            "FR_MOTIF_REEL",
            "FR_PROCEDURE_DISCIPLINAIRE",
            "FR_ORDRE_LICENCIEMENT",
            "BE_NOTIFICATION",
            "BE_PREAVIS",
            "BE_MOTIVATION",
            "BE_AUDITION",
            "BE_NON_DISCRIMINATION",
            "BE_PROTECTION_SPECIALE",
            "BE_INDEMNITE_MANIFESTE",

            // ─── Droit du travail — Type de rupture F-DT-09 ───────────────────────────
            "DT09_TYPE_RUPTURE",

            // ─── Droit du travail — Nullité procédure licenciement F-DT-36 ────────────
            "DT36_DATE_ENTRETIEN",
            "DT36_MOTIVATION",
            "DT36_ENTRETIEN_TENU",

            // ─── Droit du travail — Abandon de poste / présomption démission F-DT-42 ──
            "DT42_DATE_MISE_EN_DEMEURE",
            "DT42_DELAI_ACCORDE",
            "DT42_MENTIONS_MED",
            "DT42_MOTIF_LEGITIME",

            // ─── Droit du travail — Congés payés acquis pendant arrêt maladie F-DT-75 ─
            "DT75_TYPE_ARRET",
            "DT75_DUREE_ARRET",
            "DT75_SALARIE_EN_POSTE",

            // ─── Droit du travail — Harcèlement / licenciement nul F-DT-11 ─────────────
            "HLN_MOTIF_NULLITE",

            // ─── Droit du travail — Licenciement économique F-DT-13 ──────────────────
            "DT13_MOTIF_ECONOMIQUE",
            "DT13_DATE_NOTIFICATION",

            // ─── Droit du travail — PSE F-DT-14 ──────────────────────────────────────
            "PSE_DATE_PROJET",

            // ─── Droit du travail — Protection représentant du personnel F-DT-30 ──────
            "PROTECTION_RP_MOTIF",

            // ─── Droit du travail — Rupture conventionnelle F-DT-10 ───────────────────
            "RC_CONSENTEMENT",
            "RC_DELAI_RETRACTATION",
            "RC_HOMOLOGATION",
            "RC_ASSISTANCE",
            "RC_INDEMNITE",
            "RC_ENTRETIENS",

            // ─── Droit du travail — Requalifications F-DT-22/23/24/31/132 ─────────────
            "DT22_SALAIRE",
            "DT23_SALAIRE",
            "DT24_SALAIRE",
            "DT31_SALAIRE_MENSUEL",
            "DT31_ANCIENNETE",
            "RCI_SALAIRE",
            "RCI_ANCIENNETE",

            // ─── Droit du travail — Inaptitude F-DT-15 ───────────────────────────────
            "INAPT_ORIGINE",
            "INAPT_RECLASSEMENT",

            // ─── Droit du travail — AT/MP F-DT-33 ────────────────────────────────────
            "AT_MP_DATE_ACCIDENT",

            // ─── Droit du travail — Crédit-temps BE F-DT-29 ───────────────────────────
            "CREDIT_TEMPS_ANCIENNETE",
            "CREDIT_TEMPS_AGE",

            // ─── Droit du travail — Procédure collective F-136 ────────────────────────
            "TRAVAIL_PROCEDURE_TYPE",

            // ─── Immigration — Titre de séjour F-IM-05 ────────────────────────────────
            "IM05_MOTIF",

            // ─── Immigration — Recours F-IM-06 ────────────────────────────────────────
            "IM06_RECOURS_TYPE",

            // ─── Immigration — Droit au travail F-IM-07 ───────────────────────────────
            "IM07_TITRE_TYPE",

            // ─── Immigration — OQTF FR F-IM-08 ───────────────────────────────────────
            "IM08_MOTIF_OQTF",
            "IM08_RECOURS_FORME",
            "IM08RA_DECISION_CONTESTEE",

            // ─── Immigration — OQT BE F-IM-08 ─────────────────────────────────────────
            "IM08_MOTIF_OQT_BE",

            // ─── Immigration — AES métiers en tension FR F-IM-09 ──────────────────────
            "IM09_DATE_ENTREE_FRANCE",
            "IM09_MOIS_ACTIVITE",

            // ─── Immigration — AES étudiant FR F-IM-09 ────────────────────────────────
            "IM09_ETU_DATE_ENTREE_FRANCE",
            "IM09_ETU_DUREE_PRESENCE",
            "IM09_ETU_DATE_DEPOT_DEMANDE",

            // ─── Immigration — AES humanitaire FR F-IM-09 ─────────────────────────────
            "IM09H_DATE_ENTREE_FRANCE",
            "IM09H_MOTIF_HUMANITAIRE",

            // ─── Immigration — Changement de statut F-IM-11 ───────────────────────────
            "IM11_TITRE_ACTUEL",

            // ─── Immigration — Asile F-IM-12 ──────────────────────────────────────────
            "IM12_DISPOSITIF_ASILE",

            // ─── Immigration — Naturalisation F-IM-13 ─────────────────────────────────
            "IM13_VOIE_NATURALISATION",

            // ─── Immigration — Régime algérien F-IM-17 ────────────────────────────────
            "IM17_VOIE_REGIME_ALGERIEN",

            // ─── Immigration — Mineurs FR F-IM-19 ─────────────────────────────────────
            "IM19_DATE_NAISSANCE",
            "IM19_DATE_ENTREE",

            // ─── Immigration — Mesures d'éloignement FR F-IM-20 ──────────────────────
            "IM20_DISPOSITIF_ELOIGNEMENT",
            "IM20_MOTIF_MENACE",

            // ─── Immigration — JLD rétention FR F-IM-21 ───────────────────────────────
            "IM21_DATE_NOTIFICATION_PLACEMENT",
            "IM21_PLACEMENT_CRA",
            "IM21_MOTIF_PLACEMENT",

            // ─── Immigration — Dublin recours FR F-IM-22 ──────────────────────────────
            "IM22_DATE_NOTIFICATION_DUBLIN",

            // ─── Immigration — CRRV refus de visa FR F-IM-23 ─────────────────────────
            "IM23_DATE_NOTIFICATION_REFUS",

            // ─── Immigration — 9ter médical BE F-IM-14 ───────────────────────────────
            "BE_9TER_MALADIE_GRAVE",
            "BE_9TER_SOINS_BE",
            "BE_9TER_SOINS_INACCESSIBLES",
            "BE_9TER_MENACE_ORDRE_PUBLIC",

            // ─── Immigration — 9bis humanitaire BE F-IM-14 ────────────────────────────
            "B9BIS_DATE_ENTREE_BELGIQUE",
            "B9BIS_DUREE_PRESENCE",
            "B9BIS_CIRCONSTANCES_EXCEPTIONNELLES",
            "B9BIS_LIENS_FAMILIAUX_BE",
            "B9BIS_LIENS_PROFESSIONNELS",
            "B9BIS_SCOLARITE_ENFANTS_BE",
            "B9BIS_MENACE_ORDRE_PUBLIC",
            "B9BIS_DATE_DEPOT_DEMANDE",

            // ─── Immigration — 40bis cohabitant UE BE F-IM-14 ────────────────────────
            "IM14_LIEN_FAMILIAL",

            // ─── Famille — Partage immobilier F-FA-05 ─────────────────────────────────
            "FA05_VALEUR_VENALE",
            "FA05_CAPITAL_RESTANT",

            // ─── Famille — Calendrier garde F-FA-06 ───────────────────────────────────
            "FA06_MODE_GARDE",

            // ─── Famille — Divorce altération F-FA-08 ────────────────────────────────
            "DA_DUREE_MARIAGE",

            // ─── Famille — Divorce pour faute F-FA-09 ────────────────────────────────
            "FA09_DUREE_MARIAGE",
            "FA09_FAUTES_INVOQUEES",
            "FA09_DATE_DEPOT_ASSIGNATION",

            // ─── Famille — Mesures provisoires F-FA-12 ────────────────────────────────
            "FA12_DATE_AUDIENCE",
            "FA12_VIOLENCES",

            // ─── Famille — Révisions post-divorce F-FA-13 ─────────────────────────────
            "FA13_NB_ENFANTS",

            // ─── Famille — Ordonnance de protection F-FA-14 ───────────────────────────
            "FA14_DATE_REQUETE",
            "FA14_VIOLENCES_ALLEGUEES",
            "FA14_LOGEMENT_COMMUN",

            // ─── Famille — Récompenses F-FA-15 ────────────────────────────────────────
            "FA15_REGIME_MATRIMONIAL",

            // ─── Famille — Communauté universelle F-FA-16 ─────────────────────────────
            "COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE",
            "COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS",

            // ─── Famille — Partage judiciaire F-FA-17 ─────────────────────────────────
            "PARTAGE_JUDICIAIRE_PV",
            "PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE",

            // ─── Famille — Autorité parentale / Résidence / Désaccords F-FA-19 ─────────
            "FA19_REGIME_EXERCICE_ACTUEL",
            "FA19_DANGER_CARACTERISE",
            "FA19_CONSENTEMENT_AUTRE_PARENT",
            "FA19_INTERFERENCE_VIE_ENFANT",
            "FA19_AGE_ENFANTS",
            "FA19_RAISON_CHANGEMENT",
            "FA19_INFORME_PREALABLEMENT",
            "FA19_MODE_RESIDENCE_ACTUEL",
            "FA19_DOMAINE_DESACCORD",
            "FA19_INTENSITE_DESACCORD",
            "FA19_TENTATIVES_MEDIATION",
            "FA19_AGE_ENFANTS_CONCERNES",
            "FA19_URGENCE",

            // ─── Famille — Dissolution PACS F-FA-20 ───────────────────────────────────
            "FA20_MODE_DISSOLUTION",
            "FA20_REGIME_BIENS",
            "FA20_CREANCES_ALLEGUEES",

            // ─── Famille — Séparation de corps F-FA-21 ────────────────────────────────
            "FA21_DATE_JUGEMENT_SEPARATION",

            // ─── Famille — Indivision F-FA-22 ─────────────────────────────────────────
            "FA22_DATE_ORIGINE",
            "FA22_OCCUPATION",

            // ─── Famille — Testament F-FA-24 ──────────────────────────────────────────
            "TESTAMENT_FORME",
            "TESTAMENT_SAINE_ESPRIT",
            "TESTAMENT_QUOTITE",

            // ─── Famille — Donation F-FA-24 ───────────────────────────────────────────
            "DONATION_FORME",
            "DONATION_SAINE_ESPRIT",
            "DONATION_QUOTITE",

            // ─── Famille — Partage successoral F-FA-24 ────────────────────────────────
            "PARTAGE_MODE",
            "PARTAGE_CONSENTEMENTS",
            "PARTAGE_PRESENCE_IMMEUBLES",

            // ─── Famille — Indivision successorale F-FA-24 ────────────────────────────
            "INDIVISION_DATE_OUVERTURE",
            "INDIVISION_TYPE",

            // ─── Famille — Dévolution légale F-FA-24 ──────────────────────────────────
            "DEVOLUTION_LEGALE_CONJOINT",
            "DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS",

            // ─── Famille — Majeurs protégés F-FA-25 ───────────────────────────────────
            "FA25_DATE_CERTIFICAT",
            "FA25_ALT_MENTALES",
            "FA25_CONSENTEMENT",
            "FA25_DEMANDEUR_FAMILIAL",

            // ─── Famille — Changement d'état civil F-FA-26 ────────────────────────────
            "FA26_TYPE_CHANGEMENT",
            "FA26_MOTIF_INVOQUE",
            "FA26_DATE_NAISSANCE",
            "FA26_MAJEUR_DEMANDEUR",
            "FA26_CONSENTEMENT_PARENTAL",

            // ─── Famille — PMA/GPA bioéthique F-FA-27 ─────────────────────────────────
            "PMA_GPA_DISPOSITIF",

            // ─── Famille — Désunion irrémédiable BE F-FA-11 ───────────────────────────
            "DESU_BE_DATE_SEPARATION",
            "DESU_BE_CONSENTEE",
            "DESU_BE_DATE_ASSIGNATION",

            // ─── Famille — Régime mat. communauté légale BE (F-217) ────────────────────
            "F217_DATE_MARIAGE",

            // ─── Famille — Liquidation-partage BE (F-217) ─────────────────────────────
            "F217_DATE_NOTIFICATION_PROJET",

            // ─── Famille — Filiation / Paternité F-FA-18 ─────────────────────────────
            "CONSENTEMENT_LIBRE",
            "PATERNITE_VRAISEMBLABLE",
            "CONTESTATION_PATERNITE_MOTIFS_SERIEUX",
            "CONTESTATION_PATERNITE_EXPERTISE_ADN",
            "CONTESTATION_PATERNITE_POSSESSION_ETAT",
            "RECHERCHE_PATERNITE_POSSESSION_ETAT",
            "RECHERCHE_PATERNITE_EXPERTISE_ADN",
            "RECHERCHE_PATERNITE_REFUS_ADN",
            "RECHERCHE_PATERNITE_MOTIFS_SERIEUX",
            "POSSESSION_ETAT_TRACTATUS",
            "POSSESSION_ETAT_FAMA",
            "POSSESSION_ETAT_CONTINUE",
            "POSSESSION_ETAT_PAISIBLE",
            "POSSESSION_ETAT_NON_EQUIVOQUE",
            "ADOPTION_FORME",
            "ADOPTION_PUPILLE_ETAT",
            "ADOPTION_ADOPTANT_MARIE",
            "ADOPTION_AGE_ADOPTANT",
            "ADOPTION_AGE_ADOPTE",

            // ─── Famille — Réserve héréditaire F-FA-24 ────────────────────────────────
            "RESERVE_CONJOINT_SURVIVANT",

            // ─── Famille — Rapport à succession F-FA-24 ───────────────────────────────
            "RAPPORT_QUALITE_HERITIER",
            "RAPPORT_DONATION_NOMINALE",
            "RAPPORT_VALEUR_PARTAGE",
            "RAPPORT_DATE_DONATION"
    );

    @Test
    void tous_les_critereCode_frontend_sont_couverts_par_au_moins_un_prompt_backend() {
        assertThat(KNOWN_FRONTEND_CRITERE_CODES)
                .withFailMessage("KNOWN_FRONTEND_CRITERE_CODES est vide — liste de gouvernance manquante.")
                .isNotEmpty();

        String caseAnalysisPrompt = CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE;
        String aiQuestionPrompt = AiQuestionService.SYSTEM_PROMPT_TEMPLATE;

        Set<String> orphans = KNOWN_FRONTEND_CRITERE_CODES.stream()
                .filter(code -> !caseAnalysisPrompt.contains(code) && !aiQuestionPrompt.contains(code))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        assertThat(orphans)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\nSF-250-11 — CritereCodeIntegrityIT ECHEC\n")
                      .append("═══════════════════════════════════════\n")
                      .append("Les critereCode suivants sont déclarés dans KNOWN_FRONTEND_CRITERE_CODES\n")
                      .append("mais n'apparaissent dans AUCUN des deux prompts backend :\n\n");
                    orphans.forEach(code -> sb.append("  ❌ ").append(code).append("\n"));
                    sb.append("\nAction requise (CLAUDE.md — SF-250-11 règle de maintenance) :\n")
                      .append("  (a) Ajouter le code manquant dans CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE\n")
                      .append("      ET dans AiQuestionService.SYSTEM_PROMPT_TEMPLATE\n")
                      .append("      en créant une SF de remédiation (modèle SF-250-02 à SF-250-10), OU\n")
                      .append("  (b) Si le code n'est plus utilisé côté frontend, le retirer de\n")
                      .append("      KNOWN_FRONTEND_CRITERE_CODES dans CritereCodeIntegrityIT.java.\n")
                      .append("\nSource de vérité : docs/features/F-250/SF-250-11-garde-fou-integrite-critere-codes.md\n")
                      .append("Prompts vérifiés :\n")
                      .append("  - CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE\n")
                      .append("  - AiQuestionService.SYSTEM_PROMPT_TEMPLATE\n");
                    return sb.toString();
                })
                .isEmpty();
    }
}

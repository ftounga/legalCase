package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegalDomainPromptBuilderTest {

    // U-01 : DROIT_DU_TRAVAIL + FRANCE → "droit du travail français"
    @Test
    void domainLabel_travailFrance_returnsFrench() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_DU_TRAVAIL", "FRANCE"))
                .isEqualTo("droit du travail français");
    }

    // U-02 : DROIT_IMMIGRATION + FRANCE → "droit de l'immigration française"
    @Test
    void domainLabel_immigrationFrance_returnsFeminineFrench() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_IMMIGRATION", "FRANCE"))
                .isEqualTo("droit de l'immigration française");
    }

    // U-03 : DROIT_FAMILLE + BELGIQUE → "droit de la famille belge"
    @Test
    void domainLabel_familleBelgique_returnsBelge() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_FAMILLE", "BELGIQUE"))
                .isEqualTo("droit de la famille belge");
    }

    // U-04 : DROIT_IMMIGRATION + BELGIQUE → "droit de l'immigration belge"
    @Test
    void domainLabel_immigrationBelgique_returnsBelge() {
        assertThat(LegalDomainPromptBuilder.domainLabel("DROIT_IMMIGRATION", "BELGIQUE"))
                .isEqualTo("droit de l'immigration belge");
    }

    // SF-128-01 : règle critique de classification en tête pour chaque domaine
    @Test
    void domainSpecificInstruction_travail_containsClassificationRule() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("RÈGLE CRITIQUE DE CLASSIFICATION");
        assertThat(instruction).contains("MÉCANISME FACTUEL");
        assertThat(instruction).contains("RUPTURE_CONVENTIONNELLE");
    }

    @Test
    void domainSpecificInstruction_immigration_containsClassificationRule() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        assertThat(instruction).contains("RÈGLE CRITIQUE DE CLASSIFICATION");
        assertThat(instruction).contains("MÉCANISME FACTUEL");
        assertThat(instruction).contains("RECOURS_CNDA");
    }

    @Test
    void domainSpecificInstruction_famille_containsClassificationRule() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("RÈGLE CRITIQUE DE CLASSIFICATION");
        assertThat(instruction).contains("MÉCANISME FACTUEL");
        assertThat(instruction).contains("regime_matrimonial");
    }

    // SF-155-04-00-BE-travail : 5 nouveaux champs documentés dans le prompt travail
    @Test
    void domainSpecificInstruction_travail_mentionsNewPrefillIaFields() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("motif_nullite_pressenti");
        assertThat(instruction).contains("origine_inaptitude_pressentie");
        assertThat(instruction).contains("avis_medecin_travail_date");
        assertThat(instruction).contains("reclassement_respecte_detected");
        assertThat(instruction).contains("heures_sup_mentionnees");
        // Les 7 valeurs autorisées de motif_nullite_pressenti doivent être listées
        assertThat(instruction).contains("HARCELEMENT_MORAL");
        assertThat(instruction).contains("DISCRIMINATION");
        // Les 3 valeurs d'origine inaptitude doivent être listées
        assertThat(instruction).contains("ACCIDENT_TRAVAIL");
        assertThat(instruction).contains("MALADIE_PROFESSIONNELLE");
    }

    // SF-155-04-00-BE-immig-FR : 5 nouveaux champs IA FR documentés dans le prompt immigration
    @Test
    void domainSpecificInstruction_immigration_mentionsNewPrefillIaFieldsFR() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        // Noms des 5 nouveaux champs (clés JSON attendues dans la réponse IA)
        assertThat(instruction).contains("date_notification_oqtf");
        assertThat(instruction).contains("motif_oqtf_code");
        assertThat(instruction).contains("recours_forme_detected");
        assertThat(instruction).contains("date_heure_notification_oqtf_sans_delai");
        assertThat(instruction).contains("placement_cra_detected");
        // Les 5 valeurs autorisées de motif_oqtf_code doivent être listées
        assertThat(instruction).contains("REFUS_TITRE");
        assertThat(instruction).contains("EXPIRATION_TITRE");
        assertThat(instruction).contains("SEJOUR_IRREGULIER");
        assertThat(instruction).contains("RETRAIT_TITRE");
        // Règle explicite "null pour dossier belge"
        assertThat(instruction).containsIgnoringCase("BELGIQUE");
        // Ancrage sur l'identifiant SF
        assertThat(instruction).contains("SF-155-04-00-BE-immig-FR");
    }

    // SF-155-04-00-BE-immig-BE : 4 champs Annexe 13 BE documentés + règle null pour dossiers FR
    @Test
    void domainSpecificInstruction_immigration_mentionsAnnexe13BeFields() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        // Les 4 noms de champs doivent figurer
        assertThat(instruction).contains("date_notification_annexe13");
        assertThat(instruction).contains("delai_depart_impose_jours");
        assertThat(instruction).contains("motif_oqt_code_be");
        assertThat(instruction).contains("transfert_imminent_detected");
        // Les 4 valeurs enum BE alignées sur Annexe13BeCalculator.MOTIFS_VALIDES
        assertThat(instruction).contains("SEJOUR_IRREGULIER_ART_7");
        assertThat(instruction).contains("REFUS_SEJOUR_APRES_DEMANDE");
        assertThat(instruction).contains("FIN_SEJOUR_REGULIER");
        // Règle "null pour dossiers FR" doit être explicite
        assertThat(instruction).contains("FR");
    }

    // SF-246-04 : champ IA date_ordonnance_protection_jaf documenté dans le prompt immigration
    // pour le pré-fill de F-IM-24 (victime de violences L.425-6).
    @Test
    void domainSpecificInstruction_immigration_mentionsDateOrdonnanceProtectionJaf() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        // Clé JSON attendue dans la réponse IA
        assertThat(instruction).contains("date_ordonnance_protection_jaf");
        // Définition juridique sans ambiguïté (juge aux affaires familiales, Cciv 515-9)
        assertThat(instruction).contains("juge aux affaires familiales");
        assertThat(instruction).contains("515-9");
        // Distinction explicite d'avec les deux dates concurrentes
        assertThat(instruction).contains("date_expiration_titre");
        assertThat(instruction).contains("date_depot_procedure");
        // FRANCE uniquement + ancrage SF
        assertThat(instruction).containsIgnoringCase("BELGE");
        assertThat(instruction).contains("SF-246-04");
    }

    // SF-172-01 : élargissement détection événements immigration FR aux faits imminents documentés.
    // C1 — la phrase de biais conservateur a été supprimée du prompt trigger_events.
    // C2 — la nouvelle règle d'inclusion forward-looking est présente, avec la notion de preuve documentaire.
    @Test
    void domainSpecificInstruction_immigration_triggerEvents_dropsConservativeBiasAndAddsImminentRule() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        // C1 — phrase de biais retirée
        assertThat(instruction)
                .doesNotContain("c'est le cas attendu pour la plupart des dossiers de renouvellement simple");
        // C2 — mots-clés de la nouvelle règle d'inclusion
        assertThat(instruction).contains("imminents documentés");
        assertThat(instruction).contains("preuve documentaire");
    }

    // SF-166-01 : prompt Travail enrichi des 8 flags décisionnels niveau 3
    @Test
    void domainSpecificInstruction_travail_contains8NiveauTroisFlags() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("rappel_salaire_detecte");      // F-DT-20
        assertThat(instruction).contains("travail_dissimule_detecte");   // F-DT-21
        assertThat(instruction).contains("clause_non_concurrence_detectee"); // F-DT-24
        assertThat(instruction).contains("statut_protege_detecte");      // F-DT-30
        assertThat(instruction).contains("transaction_envisagee");       // F-DT-31
        assertThat(instruction).contains("at_mp_detecte");               // F-DT-33
        assertThat(instruction).contains("urgence_procedurale");         // F-DT-34
        assertThat(instruction).contains("contestation_are_envisagee");  // F-DT-35
    }

    @Test
    void domainSpecificInstruction_travail_niveauTroisFlags_explicitlyExcludeBE() {
        // Tous les 8 flags doivent rester false pour un dossier travail BE
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("FRANCE UNIQUEMENT");
        assertThat(instruction).contains("BELGIQUE, TOUS ces 8 flags DOIVENT rester false");
    }

    // F-200 SF-200-01 : prompt Famille enrichi des 30 flags décisionnels niveau 3 FR
    @Test
    void domainSpecificInstruction_famille_contains30NiveauTroisFlagsFR() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // 4 cas de divorce
        assertThat(instruction).contains("divorce_consentement_mutuel_envisage");
        assertThat(instruction).contains("divorce_alteration_lien_envisage");
        assertThat(instruction).contains("divorce_faute_envisage");
        assertThat(instruction).contains("divorce_accepte_envisage");
        // Régimes / partage / révision
        assertThat(instruction).contains("revision_post_divorce_envisagee");
        assertThat(instruction).contains("ordonnance_protection_envisagee");
        assertThat(instruction).contains("recompenses_envisagees");
        assertThat(instruction).contains("regime_communaute_universelle_detecte");
        assertThat(instruction).contains("partage_judiciaire_envisage");
        // Adoption + filiation
        assertThat(instruction).contains("adoption_envisagee");
        assertThat(instruction).contains("reconnaissance_paternelle_envisagee");
        assertThat(instruction).contains("contestation_paternite_envisagee");
        assertThat(instruction).contains("recherche_paternite_envisagee");
        assertThat(instruction).contains("possession_etat_envisagee");
        // Autorité parentale conflictuelle
        assertThat(instruction).contains("changement_residence_envisage");
        assertThat(instruction).contains("desaccord_parental_detecte");
        // PACS / séparation / indivision / ordonnance requête
        assertThat(instruction).contains("pacs_dissolution_envisagee");
        assertThat(instruction).contains("separation_corps_envisagee");
        assertThat(instruction).contains("indivision_envisagee");
        assertThat(instruction).contains("ordonnance_requete_envisagee");
        // Successions
        assertThat(instruction).contains("succession_envisagee");
        assertThat(instruction).contains("testament_envisage");
        assertThat(instruction).contains("donation_envisagee");
        assertThat(instruction).contains("reserve_hereditaire_envisagee");
        assertThat(instruction).contains("partage_successoral_envisage");
        assertThat(instruction).contains("indivision_successorale_envisagee");
        assertThat(instruction).contains("rapport_succession_envisage");
        // Protection majeurs / état civil / PMA-GPA
        assertThat(instruction).contains("protection_majeur_envisagee");
        assertThat(instruction).contains("changement_etat_civil_envisage");
        assertThat(instruction).contains("pma_gpa_envisagee");
    }

    @Test
    void domainSpecificInstruction_famille_niveauTroisFlagsFR_explicitlyExcludeBE() {
        // Tous les 30 flags FR doivent rester false pour un dossier famille BE
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("FRANCE UNIQUEMENT");
        assertThat(instruction).contains("BELGIQUE, TOUS ces 30 flags FR DOIVENT rester false");
        // Mention F-202 pour Famille BE
        assertThat(instruction).contains("F-202");
    }

    // F-202 SF-202-01 : prompt Famille enrichi des 5 flags décisionnels niveau 3 BE
    @Test
    void domainSpecificInstruction_famille_contains5NiveauTroisFlagsBE() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("divorce_dc_envisage");
        assertThat(instruction).contains("divorce_ddi_envisage");
        assertThat(instruction).contains("cohabitation_legale_be_detectee");
        assertThat(instruction).contains("pacte_successoral_envisage");
        assertThat(instruction).contains("kafala_recueil_detecte");
        // Conteneur famille_extracted_data documenté
        assertThat(instruction).contains("famille_extracted_data");
    }

    @Test
    void domainSpecificInstruction_famille_niveauTroisFlagsBE_explicitlyExcludeFR() {
        // Tous les 5 flags BE doivent rester false pour un dossier famille FR
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("BELGIQUE UNIQUEMENT");
        assertThat(instruction).contains("FRANCE, TOUS ces 5 flags BE DOIVENT rester false");
    }

    // SF-246-06 : prompt Famille enrichi du sous-objet succession_detection
    // (16 champs successions / libéralités pour le pré-fill des 8 outils F-FA-24).
    @Test
    void domainSpecificInstruction_famille_containsSuccessionDetectionSubObject() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("succession_detection");
    }

    @Test
    void domainSpecificInstruction_famille_successionDetection_containsSixteenKeys() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // Les 16 clés du sous-objet succession_detection.
        assertThat(instruction).contains("\"date_deces\"");
        assertThat(instruction).contains("\"date_ouverture_succession\"");
        assertThat(instruction).contains("\"mode_partage_demande\"");
        assertThat(instruction).contains("\"nombre_coheritiers\"");
        assertThat(instruction).contains("\"montant_succession_eur\"");
        assertThat(instruction).contains("\"montant_liberalites_total_eur\"");
        assertThat(instruction).contains("\"nombre_enfants_succession\"");
        assertThat(instruction).contains("\"date_donation\"");
        assertThat(instruction).contains("\"montant_donations_recues_eur\"");
        assertThat(instruction).contains("\"valeur_donation_au_jour_partage_eur\"");
        assertThat(instruction).contains("\"actif_brut_succession_eur\"");
        assertThat(instruction).contains("\"passif_succession_eur\"");
        assertThat(instruction).contains("\"type_indivision_successorale\"");
        assertThat(instruction).contains("\"nb_descendants\"");
        assertThat(instruction).contains("\"nb_freres_soeurs\"");
        assertThat(instruction).contains("\"date_redaction_testament\"");
    }

    @Test
    void domainSpecificInstruction_famille_successionDetection_distinguishesDates() {
        // Le prompt nomme explicitement les 4 concepts de date pour éviter la confusion.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("QUATRE concepts distincts");
        assertThat(instruction).contains("Date du décès du de cujus");
        assertThat(instruction).contains("Date d'ouverture de la succession");
        assertThat(instruction).contains("Date de l'acte de donation");
        assertThat(instruction).contains("Date de rédaction du testament");
    }

    @Test
    void domainSpecificInstruction_famille_successionDetection_imposesNullOutsideFrance() {
        // Le sous-objet doit rester null hors FR et hors certitude.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("famille BELGIQUE, ce sous-objet DOIT rester null");
        assertThat(instruction).contains("null plutôt qu'une valeur approximative");
    }

    @Test
    void domainSpecificInstruction_famille_successionDetection_documentsEnumWhitelists() {
        // Énumérations strictes mode_partage_demande / type_indivision_successorale.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("\"AMIABLE\"");
        assertThat(instruction).contains("\"JUDICIAIRE\"");
        assertThat(instruction).contains("\"LEGALE\"");
        assertThat(instruction).contains("\"CONVENTIONNELLE\"");
    }

    // F-205 SF-205-01 : prompt Travail enrichi de 23 flags additionnels FR
    @Test
    void domainSpecificInstruction_travail_contains23F205Flags() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        // P1 — urgences procédurales (F-206)
        assertThat(instruction).contains("abandon_poste_detecte");
        assertThat(instruction).contains("arret_maladie_long_detecte");
        assertThat(instruction).contains("prise_acte_envisagee");
        assertThat(instruction).contains("resiliation_judiciaire_envisagee");
        // P2 — fréquence haute (F-212)
        assertThat(instruction).contains("forfait_jours_detecte");
        assertThat(instruction).contains("transfert_entreprise_detecte");
        assertThat(instruction).contains("faute_inexcusable_envisagee");
        assertThat(instruction).contains("cs_crp_envisage");
        assertThat(instruction).contains("csp_propose");
        assertThat(instruction).contains("mutation_refusee");
        assertThat(instruction).contains("modification_contrat_refusee");
        assertThat(instruction).contains("faute_grave_envisagee");
        assertThat(instruction).contains("faute_lourde_envisagee");
        assertThat(instruction).contains("cdd_requalification_envisagee");
        assertThat(instruction).contains("interim_requalification_envisagee");
        assertThat(instruction).contains("forfait_jours_validite_contestee");
        assertThat(instruction).contains("prescription_proche_detectee");
        assertThat(instruction).contains("rupture_amiable_negociee");
        assertThat(instruction).contains("entretien_preavis_obtenu");
        assertThat(instruction).contains("cse_consultation_demandee");
        assertThat(instruction).contains("irp_election_demandee");
        assertThat(instruction).contains("inspection_travail_saisie");
        assertThat(instruction).contains("mediation_judiciaire_envisagee");
    }

    @Test
    void domainSpecificInstruction_travail_F205Flags_explicitlyExcludeBE() {
        // Tous les 23 flags F-205 FR doivent rester false pour un dossier travail BE
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("F-205");
        assertThat(instruction).contains("23 flags FR DOIVENT rester false");
    }

    // SF-246-01 : prompt Travail enrichi du sous-objet procedure_licenciement_detection
    // (pré-fill F-DT-36, nullité de procédure de licenciement, FR uniquement).
    @Test
    void domainSpecificInstruction_travail_containsProcedureLicenciementDetectionKeys() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        // Le conteneur lui-même
        assertThat(instruction).contains("procedure_licenciement_detection");
        // Les 7 clés du sous-objet
        assertThat(instruction).contains("convocation_entretien_detectee");
        assertThat(instruction).contains("date_convocation_entretien");
        assertThat(instruction).contains("date_entretien_prealable");
        assertThat(instruction).contains("entretien_prealable_tenu");
        assertThat(instruction).contains("lettre_licenciement_ecrite");
        assertThat(instruction).contains("lettre_licenciement_motivee");
        assertThat(instruction).contains("motivation_lettre_suffisante");
    }

    @Test
    void domainSpecificInstruction_travail_procedureLicenciement_explicitlyExcludeBE() {
        // Le sous-objet procédural est FR uniquement — null / omis pour la BE.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("procedure_licenciement_detection");
        assertThat(instruction).contains("FRANCE UNIQUEMENT");
        // Ancrage juridique procédural FR
        assertThat(instruction).contains("L.1232-2");
        assertThat(instruction).contains("L.1235-2");
    }

    // SF-246-02 : prompt Travail enrichi du sous-objet clause_non_concurrence_detail
    // (pré-fill F-DT-24, validité de la clause de non-concurrence, FR uniquement).
    @Test
    void domainSpecificInstruction_travail_containsClauseNonConcurrenceDetailKeys() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        // Le conteneur lui-même
        assertThat(instruction).contains("clause_non_concurrence_detail");
        // Les 3 clés du sous-objet
        assertThat(instruction).contains("duree_mois");
        assertThat(instruction).contains("zone_geographique");
        assertThat(instruction).contains("contrepartie_montant_mensuel_eur");
    }

    @Test
    void domainSpecificInstruction_travail_clauseNonConcurrence_imposeConversionEnEurosMensuels() {
        // L'instruction impose la conversion %→€ via salaire_brut_mensuel.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("clause_non_concurrence_detail");
        assertThat(instruction).contains("RÈGLE DE CONVERSION OBLIGATOIRE");
        assertThat(instruction).contains("POURCENTAGE");
        assertThat(instruction).contains("salaire_brut_mensuel");
        assertThat(instruction).contains("EUROS BRUTS");
    }

    @Test
    void domainSpecificInstruction_travail_clauseNonConcurrence_explicitlyExcludeBE() {
        // Le sous-objet est FR uniquement — null / omis pour la BE (régime CCT 1bis).
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("clause_non_concurrence_detail");
        assertThat(instruction).contains("FRANCE UNIQUEMENT");
    }

    // SF-246-13 : sous-objet clause_non_concurrence_detail enrichi des clés
    // date_prise_effet et secteur_activite (pré-fill F-DT-24, FR uniquement).
    @Test
    void domainSpecificInstruction_travail_clauseNonConcurrence_containsDatePriseEffetAndSecteurKeys() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("date_prise_effet");
        assertThat(instruction).contains("secteur_activite");
        // Le sous-objet annonce désormais 5 clés.
        assertThat(instruction).contains("Les 5 clés attendues");
    }

    @Test
    void domainSpecificInstruction_travail_clauseNonConcurrence_secteurListsAllFiveEnumCodes() {
        // Le prompt impose le classement dans l'une des 5 valeurs exactes de l'enum.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("INFORMATIQUE");
        assertThat(instruction).contains("COMMERCE");
        assertThat(instruction).contains("INDUSTRIE");
        assertThat(instruction).contains("SERVICES");
        assertThat(instruction).contains("AUTRE");
    }

    @Test
    void domainSpecificInstruction_travail_clauseNonConcurrence_datePriseEffetImposeFormatIso() {
        // date_prise_effet doit être au format YYYY-MM-DD strict.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("date_prise_effet");
        assertThat(instruction).contains("YYYY-MM-DD");
    }

    // SF-246-05 : prompt Travail enrichi de la clé age_demandeur_annees
    // (pré-fill F-DT-29, crédit-temps fin de carrière, BELGIQUE uniquement).
    @Test
    void domainSpecificInstruction_travail_containsAgeDemandeurAnneesKey() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("age_demandeur_annees");
        // Définition juridique non ambiguë.
        assertThat(instruction).contains("ANNÉES RÉVOLUES");
        assertThat(instruction).contains("crédit-temps fin de carrière");
    }

    @Test
    void domainSpecificInstruction_travail_ageDemandeur_distingueDeLAnciennete() {
        // Invariant cadrage §5.1.1 : l'âge doit être explicitement distingué de
        // l'ancienneté et de toute durée du dossier.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("age_demandeur_annees");
        assertThat(instruction).contains("NE PAS confondre avec l'ancienneté");
    }

    @Test
    void domainSpecificInstruction_travail_ageDemandeur_explicitlyExcludeFR() {
        // Le champ est BE uniquement — null pour un dossier travail français.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_DU_TRAVAIL");
        assertThat(instruction).contains("age_demandeur_annees");
        assertThat(instruction).contains("BELGIQUE UNIQUEMENT");
    }

    // SF-246-07 : prompt Famille enrichi du sous-objet regime_matrimonial_detection
    // (4 champs régimes matrimoniaux / liquidation pour le pré-fill des 3 outils F-FA-15/16/17).

    @Test
    void domainSpecificInstruction_famille_containsRegimeMatrimonialDetectionSubObject() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("regime_matrimonial_detection");
    }

    @Test
    void domainSpecificInstruction_famille_regimeMatrimonialDetection_containsFourKeys() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // Les 4 clés du sous-objet regime_matrimonial_detection.
        assertThat(instruction).contains("\"regime_matrimonial\"");
        assertThat(instruction).contains("\"valeur_communaute_eur\"");
        assertThat(instruction).contains("\"valeur_biens_indivision_eur\"");
        assertThat(instruction).contains("\"nombre_coindivisaires\"");
    }

    @Test
    void domainSpecificInstruction_famille_regimeMatrimonialDetection_enumWhitelist() {
        // Énumération stricte des 4 régimes autorisés.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("\"COMMUNAUTE_LEGALE\"");
        assertThat(instruction).contains("\"COMMUNAUTE_UNIVERSELLE\"");
        assertThat(instruction).contains("\"SEPARATION_BIENS\"");
        assertThat(instruction).contains("\"PARTICIPATION_ACQUETS\"");
    }

    @Test
    void domainSpecificInstruction_famille_regimeMatrimonialDetection_imposesNullOutsideFrance() {
        // Le sous-objet doit rester null hors FR.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("famille BELGIQUE, ce sous-objet DOIT rester null");
    }

    @Test
    void domainSpecificInstruction_famille_regimeMatrimonialDetection_regleAntiFait() {
        // Le prompt impose de renseigner le régime JURIDIQUE documenté, jamais la prétention.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("RÈGLE ANTI-CONFUSION régime / fait");
    }

    // SF-246-08 : prompt Famille enrichi du sous-objet vie_commune_detection
    // ─────────────────────────────────────────────────────────────────────────────────
    @Test
    void domainSpecificInstruction_famille_containsVieCommuneDetectionSubObject() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("vie_commune_detection");
    }

    @Test
    void domainSpecificInstruction_famille_vieCommuneDetection_containsSevenKeys() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // Les 7 clés du sous-objet vie_commune_detection.
        assertThat(instruction).contains("\"date_separation\"");
        assertThat(instruction).contains("\"patrimoine_commun_eur\"");
        assertThat(instruction).contains("\"date_conclusion_pacs\"");
        assertThat(instruction).contains("\"date_requete_op\"");
        assertThat(instruction).contains("\"date_audience_aomp\"");
        assertThat(instruction).contains("\"nb_enfants_a_charge\"");
        assertThat(instruction).contains("\"revenus_annuels_epoux_eur\"");
    }

    @Test
    void domainSpecificInstruction_famille_vieCommuneDetection_imposesNullOutsideFrance() {
        // Le sous-objet doit rester null hors FR (Belgique gérée par ses propres champs).
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("FRANCE UNIQUEMENT");
    }

    @Test
    void domainSpecificInstruction_famille_vieCommuneDetection_regleAntiConfusionDates() {
        // La règle anti-confusion des 4 dates doit être présente.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("DISTINCTION DES DATES VIE COMMUNE");
    }

    @Test
    void domainSpecificInstruction_famille_vieCommuneDetection_borneNbEnfants() {
        // La borne 30 de nb_enfants_a_charge doit être explicite dans le prompt.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("entier entre 0 et 30");
    }

    // SF-246-09 : prompt Famille enrichi du sous-objet filiation_detection
    // ─────────────────────────────────────────────────────────────────────────────────
    @Test
    void domainSpecificInstruction_famille_containsFiliationDetectionSubObject() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("filiation_detection");
    }

    @Test
    void domainSpecificInstruction_famille_filiationDetection_containsSevenKeys() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // Les 7 clés du sous-objet filiation_detection.
        assertThat(instruction).contains("\"date_etablissement_filiation\"");
        assertThat(instruction).contains("\"date_connaissance_verite\"");
        assertThat(instruction).contains("\"date_majorite_enfant\"");
        assertThat(instruction).contains("\"date_naissance_enfant_recherche\"");
        assertThat(instruction).contains("\"date_naissance_enfant\"");
        assertThat(instruction).contains("\"age_adoptant\"");
        assertThat(instruction).contains("\"age_adopte\"");
    }

    @Test
    void domainSpecificInstruction_famille_filiationDetection_imposesNullOutsideFrance() {
        // Le sous-objet doit rester null hors FR.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // La mention FRANCE UNIQUEMENT est déjà vérifiée par d'autres tests ; on
        // vérifie ici la présence de la contrainte BE spécifique à filiation.
        assertThat(instruction).contains("filiation_detection");
        assertThat(instruction).contains("FRANCE UNIQUEMENT");
    }

    @Test
    void domainSpecificInstruction_famille_filiationDetection_regleAntiConfusionDates() {
        // La règle anti-confusion des 5 dates de filiation doit être présente.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("DISTINCTION DES DATES FILIATION");
    }

    @Test
    void domainSpecificInstruction_famille_filiationDetection_distingueNaissanceRechercheVsReconnaissance() {
        // La distinction entre les deux dates de naissance d'enfant doit être explicite.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("date_naissance_enfant_recherche");
        assertThat(instruction).contains("date_naissance_enfant");
        assertThat(instruction).contains("DISTINCT");
    }

    @Test
    void domainSpecificInstruction_famille_filiationDetection_borneAge() {
        // La borne [0, 120] pour les âges doit être explicite dans le prompt.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("entier entre 0 et 120");
    }

    // =========================================================================
    // SF-246-10 — autorite_parentale_detection
    // =========================================================================

    @Test
    void domainSpecificInstruction_famille_contientAutoriteParentaleDetectionSousObjet() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("autorite_parentale_detection");
    }

    @Test
    void domainSpecificInstruction_famille_autoriteParentaleDetection_contientTroisCles() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("ages_enfants");
        assertThat(instruction).contains("date_debut_calendrier");
        assertThat(instruction).contains("date_fin_calendrier");
    }

    @Test
    void domainSpecificInstruction_famille_autoriteParentaleDetection_imposesNullHorsFrance() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // Le prompt impose null hors France pour le sous-objet autorite_parentale_detection.
        assertThat(instruction).contains("autorite_parentale_detection");
        assertThat(instruction).contains("FRANCE UNIQUEMENT");
    }

    @Test
    void domainSpecificInstruction_famille_autoriteParentaleDetection_regleListeAges() {
        // L'instruction d'exclusion d'âge non fiable doit être présente.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("RÈGLE LISTE D'ÂGES");
    }

    // =========================================================================
    // SF-246-03 — divorce_faute_detection (codes de faute, F-FA-09)
    // =========================================================================

    @Test
    void domainSpecificInstruction_famille_contientDivorceFauteDetectionSousObjet() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("divorce_faute_detection");
    }

    @Test
    void domainSpecificInstruction_famille_divorceFauteDetection_contientFautesDetecteesCle() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("fautes_detectees");
    }

    @Test
    void domainSpecificInstruction_famille_divorceFauteDetection_contientHuitCodes() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // Les 8 codes de l'énumération fermée alignée sur FauteCode frontend.
        assertThat(instruction).contains("\"ADULTERE\"");
        assertThat(instruction).contains("\"VIOLENCES\"");
        assertThat(instruction).contains("\"ABANDON\"");
        assertThat(instruction).contains("\"OUTRAGES\"");
        assertThat(instruction).contains("\"DEVOIR_ASSISTANCE\"");
        assertThat(instruction).contains("\"DEVOIR_FIDELITE\"");
        assertThat(instruction).contains("\"DEVOIR_COMMUNAUTE_VIE\"");
        assertThat(instruction).contains("\"AUTRE\"");
    }

    @Test
    void domainSpecificInstruction_famille_divorceFauteDetection_imposesNullHorsFrance() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        // Le prompt doit exiger null pour les dossiers belges.
        assertThat(instruction).contains("divorce_faute_detection");
        assertThat(instruction).contains("BELGIQUE");
    }

    @Test
    void domainSpecificInstruction_famille_divorceFauteDetection_regleOrDocumentation() {
        // La règle d'or "pièce concrète" doit être présente.
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction).contains("RÈGLE D'OR");
    }
}

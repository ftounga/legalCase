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
}

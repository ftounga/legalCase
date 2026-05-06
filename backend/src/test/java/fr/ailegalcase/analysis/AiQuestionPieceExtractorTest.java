package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-196 SF-196-01 — UT du keyword extractor statique.
 *
 * <ul>
 *   <li>Patterns Travail FR/BE</li>
 *   <li>Patterns Famille FR/BE</li>
 *   <li>Patterns Immigration FR/BE</li>
 *   <li>Pas de match → {@code null}</li>
 *   <li>{@code parseYesNo} : "oui" / "Oui" / "OUI" / "non" / "Non" / autres → null</li>
 * </ul>
 */
class AiQuestionPieceExtractorTest {

    // ======================================================================
    //  extractPieceLibelle
    // ======================================================================

    @Test
    void extractPieceLibelle_lettreLicenciement_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous reçu la lettre de licenciement ?"))
                .isEqualTo("Lettre de licenciement");
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Disposez-vous d'une lettre de licenciement signée ?"))
                .isEqualTo("Lettre de licenciement");
    }

    @Test
    void extractPieceLibelle_contratTravail_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous le contrat de travail original ?"))
                .isEqualTo("Contrat de travail");
    }

    @Test
    void extractPieceLibelle_fichesPaie_variants() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous les fiches de paie des 12 derniers mois ?"))
                .isEqualTo("Fiches de paie");
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Disposez-vous des bulletins de salaire ?"))
                .isEqualTo("Fiches de paie");
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Pouvez-vous transmettre vos bulletins de paie ?"))
                .isEqualTo("Fiches de paie");
    }

    @Test
    void extractPieceLibelle_attestationPoleEmploi_withAccent() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous l'attestation Pôle emploi ?"))
                .isEqualTo("Attestation Pôle emploi");
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous l'attestation Pole emploi ?"))
                .isEqualTo("Attestation Pôle emploi");
    }

    @Test
    void extractPieceLibelle_soldeDeToutCompte_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous le solde de tout compte signé ?"))
                .isEqualTo("Solde de tout compte");
    }

    @Test
    void extractPieceLibelle_certificatTravail_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous le certificat de travail ?"))
                .isEqualTo("Certificat de travail");
    }

    @Test
    void extractPieceLibelle_acteMariage_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous l'acte de mariage ?"))
                .isEqualTo("Acte de mariage");
    }

    @Test
    void extractPieceLibelle_livretFamille_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous le livret de famille ?"))
                .isEqualTo("Livret de famille");
    }

    @Test
    void extractPieceLibelle_titreSejour_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous le titre de séjour en cours ?"))
                .isEqualTo("Titre de séjour");
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous votre carte de séjour ?"))
                .isEqualTo("Titre de séjour");
    }

    @Test
    void extractPieceLibelle_oqtf_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous reçu une OQTF ?"))
                .isEqualTo("OQTF (Obligation de Quitter le Territoire Français)");
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous reçu une obligation de quitter le territoire ?"))
                .isEqualTo("OQTF (Obligation de Quitter le Territoire Français)");
    }

    @Test
    void extractPieceLibelle_visa_returnsLabel() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous votre visa actuel ?"))
                .isEqualTo("Visa");
    }

    @Test
    void extractPieceLibelle_caseInsensitive() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "AVEZ-VOUS LE CONTRAT DE TRAVAIL ?"))
                .isEqualTo("Contrat de travail");
    }

    @Test
    void extractPieceLibelle_noMatch_returnsNull() {
        // Question informationnelle qui ne correspond à aucun pattern
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Quelle est la date de l'incident ?"))
                .isNull();
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Combien de personnes étaient présentes ?"))
                .isNull();
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(
                "Avez-vous des informations supplémentaires ?"))
                .isNull();
    }

    @Test
    void extractPieceLibelle_nullOrBlank_returnsNull() {
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle(null)).isNull();
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle("")).isNull();
        assertThat(AiQuestionPieceExtractor.extractPieceLibelle("   ")).isNull();
    }

    // ======================================================================
    //  parseYesNo
    // ======================================================================

    @Test
    void parseYesNo_oui_returnsTrue() {
        assertThat(AiQuestionPieceExtractor.parseYesNo("oui")).isTrue();
        assertThat(AiQuestionPieceExtractor.parseYesNo("Oui")).isTrue();
        assertThat(AiQuestionPieceExtractor.parseYesNo("OUI")).isTrue();
        assertThat(AiQuestionPieceExtractor.parseYesNo("  oui  ")).isTrue();
        assertThat(AiQuestionPieceExtractor.parseYesNo("oui je l'ai")).isTrue();
        assertThat(AiQuestionPieceExtractor.parseYesNo("Oui, signée le 12/03")).isTrue();
        assertThat(AiQuestionPieceExtractor.parseYesNo("yes")).isTrue();
    }

    @Test
    void parseYesNo_non_returnsFalse() {
        assertThat(AiQuestionPieceExtractor.parseYesNo("non")).isFalse();
        assertThat(AiQuestionPieceExtractor.parseYesNo("Non")).isFalse();
        assertThat(AiQuestionPieceExtractor.parseYesNo("NON")).isFalse();
        assertThat(AiQuestionPieceExtractor.parseYesNo("  non  ")).isFalse();
        assertThat(AiQuestionPieceExtractor.parseYesNo("Non, perdue")).isFalse();
        assertThat(AiQuestionPieceExtractor.parseYesNo("non, jamais reçue")).isFalse();
    }

    @Test
    void parseYesNo_otherTexts_returnsNull() {
        assertThat(AiQuestionPieceExtractor.parseYesNo(null)).isNull();
        assertThat(AiQuestionPieceExtractor.parseYesNo("")).isNull();
        assertThat(AiQuestionPieceExtractor.parseYesNo("   ")).isNull();
        assertThat(AiQuestionPieceExtractor.parseYesNo("peut-être")).isNull();
        assertThat(AiQuestionPieceExtractor.parseYesNo("je ne sais pas")).isNull();
        assertThat(AiQuestionPieceExtractor.parseYesNo("3 mois")).isNull();
        assertThat(AiQuestionPieceExtractor.parseYesNo("le 12/03/2024")).isNull();
    }
}

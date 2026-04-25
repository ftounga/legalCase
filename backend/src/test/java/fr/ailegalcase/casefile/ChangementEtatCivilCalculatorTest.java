package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-26-01 : tests unitaires de ChangementEtatCivilCalculator
 * (art. 60 / 61-3-1 / 61-5 Cciv).
 *
 * <p>Couvre la compétence procédurale, les contributions du score, les
 * verdicts, les documents canoniques manquants, les délais, la base
 * juridique et les erreurs de validation.
 */
class ChangementEtatCivilCalculatorTest {

    private static final LocalDate DATE_NAISSANCE = LocalDate.of(1985, 4, 15);

    @Test
    void compute_prenom_competenceMairie_delai2mois() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "INTERET_LEGITIME",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.competenceProcedure()).isEqualTo("MAIRIE");
        assertThat(r.delaiInstructionMoisPrevisionnel()).isEqualTo(2);
    }

    @Test
    void compute_nom_interetLegitimeAvec30Ans_competenceMairie_loi2022() {
        // NOM + intérêt légitime + JUSTIFICATIF_USAGE_30ANS = mairie
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM",
                "INTERET_LEGITIME",
                List.of("JUSTIFICATIF_USAGE_30ANS", "ACTES_CIVILS",
                        "CERTIFICAT_NAISSANCE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.competenceProcedure()).isEqualTo("MAIRIE");
        assertThat(r.delaiInstructionMoisPrevisionnel()).isEqualTo(2);
        assertThat(r.formule()).contains("30 ans");
    }

    @Test
    void compute_nom_motifClassique_competenceTribunalJudiciaire() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM",
                "RECTIFICATION_ERREUR",
                List.of("CERTIFICAT_NAISSANCE", "ACTES_CIVILS"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.competenceProcedure()).isEqualTo("TRIBUNAL_JUDICIAIRE");
        assertThat(r.delaiInstructionMoisPrevisionnel()).isEqualTo(6);
    }

    @Test
    void compute_sexe_competenceJuge_delai3mois() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "SEXE",
                "IDENTIFICATION_GENRE",
                List.of("TEMOIGNAGES", "ACTES_CIVILS"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.competenceProcedure()).isEqualTo("JUGE");
        assertThat(r.delaiInstructionMoisPrevisionnel()).isEqualTo(3);
    }

    @Test
    void compute_nomEtPrenom_partieNomRelevantTJ_competenceTJ() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM_ET_PRENOM",
                "MARIAGE",
                List.of("LIVRET_FAMILLE", "ACTES_CIVILS"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.competenceProcedure()).isEqualTo("TRIBUNAL_JUDICIAIRE");
    }

    @Test
    void compute_nomEtPrenom_partieNomEnMairie_competenceMairie() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM_ET_PRENOM",
                "INTERET_LEGITIME",
                List.of("JUSTIFICATIF_USAGE_30ANS", "ACTES_CIVILS",
                        "CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.competenceProcedure()).isEqualTo("MAIRIE");
        assertThat(r.delaiInstructionMoisPrevisionnel()).isEqualTo(2);
    }

    @Test
    void compute_score_plafonne_100() {
        // motif (+30) + ≥2 preuves (+25) + majeur (+15) + !dejaChange (+15)
        // + datesConcordants (+15) = 100
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "INTERET_LEGITIME",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE",
                        "ACTES_CIVILS"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(100);
        assertThat(r.verdictAcceptabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_score_plancher_0() {
        // motif AUTRE → pas de bonus motif (0)
        // 1 preuve seule → pas de bonus preuves (0)
        // mineur sans consentement → pas de bonus majorité
        // dejaChange=true → pas de bonus
        // datesConcordants=false → pas de bonus
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM",
                "AUTRE",
                List.of("AUTRE"),
                false, false, false, true,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(0);
        assertThat(r.verdictAcceptabilite()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_verdict_elevee_seuil70() {
        // motif (+30) + ≥2 preuves (+25) + majeur (+15) = 70 → ELEVEE
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("LIVRET_FAMILLE", "ACTES_CIVILS"),
                true, false, false, true,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(70);
        assertThat(r.verdictAcceptabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_verdict_moyenne_40to69() {
        // motif (+30) + majeur (+15) = 45 → MOYENNE
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("LIVRET_FAMILLE"),
                true, false, false, true,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(45);
        assertThat(r.verdictAcceptabilite()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_verdict_faible_inferieur40() {
        // motif AUTRE (+0) + majeur (+15) + !dejaChange (+15) = 30 → FAIBLE
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "AUTRE",
                List.of(),
                true, false, false, false,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(30);
        assertThat(r.verdictAcceptabilite()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_documentsManquants_PRENOM_listeActeNaissanceLivret() {
        // Aucune preuve → CERTIFICAT_NAISSANCE et LIVRET_FAMILLE manquants
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of(),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.documentsRequisManquants())
                .containsExactly("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE");
    }

    @Test
    void compute_documentsManquants_NOM_interetLegitime_demande30ans() {
        // NOM + INTERET_LEGITIME, sans JUSTIFICATIF_USAGE_30ANS dans preuves
        // → JUSTIFICATIF_USAGE_30ANS dans manquants
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM",
                "INTERET_LEGITIME",
                List.of("CERTIFICAT_NAISSANCE", "ACTES_CIVILS"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.documentsRequisManquants())
                .contains("JUSTIFICATIF_USAGE_30ANS");
    }

    @Test
    void compute_documentsManquants_complet_aucunManquant() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.documentsRequisManquants()).isEmpty();
    }

    @Test
    void compute_documentsManquants_SEXE_temoignages() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "SEXE",
                "IDENTIFICATION_GENRE",
                List.of("ACTES_CIVILS"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        // CERTIFICAT_NAISSANCE et TEMOIGNAGES manquants
        assertThat(r.documentsRequisManquants())
                .contains("CERTIFICAT_NAISSANCE", "TEMOIGNAGES");
    }

    @Test
    void compute_mineurSansConsentement_pasBonus15_messageAvert() {
        // mineur sans consentement parental → pas le bonus +15
        // motif (+30) + ≥2 preuves (+25) + !dejaChange (+15)
        // + datesConcordants (+15) = 85
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                false, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(85);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("mineur").contains("consentement"));
    }

    @Test
    void compute_mineurAvecConsentement_bonus15_message() {
        // mineur avec consentement → bonus +15 conservé
        // motif (+30) + ≥2 preuves (+25) + consentement (+15)
        // + !dejaChange (+15) + datesConcordants (+15) = 100
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                false, true, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(100);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Demandeur mineur"));
    }

    @Test
    void compute_dejaChangeAuparavant_scoreBaisse_message() {
        // motif (+30) + ≥2 preuves (+25) + majeur (+15) + datesConcordants (+15)
        // = 85 (sans le +15 dejaChange)
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, true,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(85);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Changement antérieur"));
    }

    @Test
    void compute_baseJuridique_contient60_61_3_1_61_5() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.baseJuridique())
                .contains("60")
                .contains("61-3-1")
                .contains("61-5");
    }

    @Test
    void compute_messages_loi2016_referencee_pourSexe() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "SEXE",
                "IDENTIFICATION_GENRE",
                List.of("TEMOIGNAGES", "CERTIFICAT_NAISSANCE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("61-5"));
    }

    @Test
    void compute_messages_loi2022_referencee_pourNomSimplifie() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM",
                "INTERET_LEGITIME",
                List.of("JUSTIFICATIF_USAGE_30ANS", "ACTES_CIVILS",
                        "CERTIFICAT_NAISSANCE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("61-3-1").contains("2022"));
    }

    @Test
    void compute_formule_contient_type_motif_competence_score_verdict() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "NOM",
                "INTERET_LEGITIME",
                List.of("JUSTIFICATIF_USAGE_30ANS", "ACTES_CIVILS",
                        "CERTIFICAT_NAISSANCE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.formule())
                .contains("NOM")
                .contains("intérêt légitime")
                .contains("mairie")
                .contains("Score")
                .contains("ELEVEE");
    }

    @Test
    void compute_preuves_dedupliquees() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "CERTIFICAT_NAISSANCE",
                        "LIVRET_FAMILLE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.preuvesProduites())
                .containsExactly("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE");
    }

    @Test
    void compute_typeInvalide_throws() {
        assertThatThrownBy(() -> ChangementEtatCivilCalculator.compute(
                "INVALIDE",
                "MARIAGE",
                List.of(),
                true, false, true, false,
                DATE_NAISSANCE, "75"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeChangement");
    }

    @Test
    void compute_typeNull_throws() {
        assertThatThrownBy(() -> ChangementEtatCivilCalculator.compute(
                null,
                "MARIAGE",
                List.of(),
                true, false, true, false,
                DATE_NAISSANCE, "75"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeChangement");
    }

    @Test
    void compute_motifInvalide_throws() {
        assertThatThrownBy(() -> ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "INVALIDE",
                List.of(),
                true, false, true, false,
                DATE_NAISSANCE, "75"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifInvoque");
    }

    @Test
    void compute_motifNull_throws() {
        assertThatThrownBy(() -> ChangementEtatCivilCalculator.compute(
                "PRENOM",
                null,
                List.of(),
                true, false, true, false,
                DATE_NAISSANCE, "75"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifInvoque");
    }

    @Test
    void compute_preuvesInvalides_throws() {
        assertThatThrownBy(() -> ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("INVALIDE"),
                true, false, true, false,
                DATE_NAISSANCE, "75"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preuvesProduites");
    }

    @Test
    void compute_preuvesNullOuVide_acceptees() {
        ChangementEtatCivilResult r1 = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                null,
                true, false, true, false,
                DATE_NAISSANCE, "75");
        assertThat(r1.preuvesProduites()).isEmpty();

        ChangementEtatCivilResult r2 = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of(),
                true, false, true, false,
                DATE_NAISSANCE, "75");
        assertThat(r2.preuvesProduites()).isEmpty();
    }

    @Test
    void compute_motifAutre_pasDeBonus30() {
        // motif AUTRE → pas le bonus +30
        // ≥2 preuves (+25) + majeur (+15) + !dejaChange (+15)
        // + datesConcordants (+15) = 70
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "AUTRE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.scoreAcceptabilite()).isEqualTo(70);
    }

    @Test
    void compute_messages_recoursTGI_mentionne_pourMairie() {
        ChangementEtatCivilResult r = ChangementEtatCivilCalculator.compute(
                "PRENOM",
                "MARIAGE",
                List.of("CERTIFICAT_NAISSANCE", "LIVRET_FAMILLE"),
                true, false, true, false,
                DATE_NAISSANCE, "75");

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("recours").contains("refus"));
    }
}

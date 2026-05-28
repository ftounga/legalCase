package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-23 : tests unitaires du
 * {@link DiscriminationBeHandicapAmenagementChecker}.
 *
 * <p>Couvre : qualification handicap (RECONNU_OFFICIEL /
 * MEDECIN_TRAVAIL_AVIS / INVALIDITE_MUTUELLE / DURABLE_FACTUEL avec
 * et sans avis SEPP / CONTESTE / INDETERMINE), réponse employeur
 * (ACCORDE / CONTRE_PROPOSITION / REFUSE_MOTIVE_DETAILLE /
 * REFUSE_NON_MOTIVE / NON_REPONDU_RAISONNABLE / EN_ATTENTE /
 * INDETERMINE), sanction subie (LICENCIEMENT / REGRESSION /
 * HARCELEMENT / AUCUNE), priorité représailles, charge
 * disproportionnée démontrée vs non démontrée, indemnité 6 mois
 * indicative, validation des inputs, base juridique stable.</p>
 */
class DiscriminationBeHandicapAmenagementCheckerTest {

    private static final LocalDate DATE_DEMANDE = LocalDate.of(2025, 3, 1);
    private static final LocalDate DATE_SANCTION = LocalDate.of(2025, 5, 15);

    private static DiscriminationBeHandicapAmenagementRequest reqAccorde() {
        return new DiscriminationBeHandicapAmenagementRequest(
                DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                DATE_DEMANDE,
                "Aménagement poste de travail ergonomique + télétravail 2 j/sem",
                new BigDecimal("3500.00"),
                true,
                120,
                new BigDecimal("8000000.00"),
                DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                true,
                true,
                false,
                false,
                false,
                DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                null,
                new BigDecimal("3200.00"),
                false);
    }

    // ── Cas nominal : aménagement accordé ──────────────────────────────────

    @Test
    void analyze_amenagementAccorde_returnsConforme() {
        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(reqAccorde());

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.CONFORME_AMENAGEMENT_ACCORDE);
        assertThat(r.handicapQualifie()).isTrue();
        assertThat(r.demandeFormalisee()).isTrue();
        assertThat(r.refusCaracterise()).isFalse();
        assertThat(r.representaillesPresumees()).isFalse();
        assertThat(r.indemniteForfaitaire6Mois())
                .isEqualByComparingTo(new BigDecimal("19200.00"));
        assertThat(r.raison()).isEqualTo("AMENAGEMENT_ACCORDE");
        assertThat(r.synthese()).contains("accordé l'aménagement");
        assertThat(r.baseJuridique()).contains("Loi du 10/05/2007");
    }

    @Test
    void analyze_contrePropositionAvecAlternatives_returnsConforme() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Télétravail intégral",
                        new BigDecimal("500.00"), true,
                        80, new BigDecimal("3000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.CONTRE_PROPOSITION,
                        true, true, false, false, true,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.CONFORME_AMENAGEMENT_ACCORDE);
        assertThat(r.raison()).isEqualTo("CONTRE_PROPOSITION_ACCEPTABLE");
    }

    // ── Représailles présumées (priorité absolue après A_ANALYSER) ─────────

    @Test
    void analyze_licenciementApresDemande_returnsRepresailles() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Réaffectation interne",
                        new BigDecimal("1000.00"), false,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, true, true, true, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.LICENCIEMENT,
                        DATE_SANCTION, new BigDecimal("4000.00"), true);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.REPRESAILLES_PRESUMEES_LICENCIEMENT);
        assertThat(r.representaillesPresumees()).isTrue();
        assertThat(r.indemniteForfaitaire6Mois())
                .isEqualByComparingTo(new BigDecimal("24000.00"));
        assertThat(r.synthese()).contains("licencié")
                .contains("présomption de représailles")
                .contains("art. 17");
    }

    @Test
    void analyze_regressionApresDemande_returnsRepresailles() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.MEDECIN_TRAVAIL_AVIS,
                        DATE_DEMANDE, "Horaires flexibles",
                        new BigDecimal("0.00"), false,
                        30, new BigDecimal("1500000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_NON_MOTIVE,
                        false, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.REGRESSION_CONDITIONS_TRAVAIL,
                        DATE_SANCTION, new BigDecimal("2800.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.REPRESAILLES_PRESUMEES_LICENCIEMENT);
        assertThat(r.synthese()).contains("régression substantielle");
    }

    @Test
    void analyze_harcelementApresDemande_returnsRepresailles() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.INVALIDITE_MUTUELLE,
                        DATE_DEMANDE, "Adaptation poste",
                        new BigDecimal("1500.00"), true,
                        40, new BigDecimal("1800000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, true, true, false, true,
                        DiscriminationBeHandicapAmenagementSanctionSubie.HARCELEMENT_REPRESAILLE,
                        DATE_SANCTION, new BigDecimal("3000.00"), true);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.REPRESAILLES_PRESUMEES_LICENCIEMENT);
        assertThat(r.synthese())
                .contains("comportement abusif")
                .contains("Loi 04/08/1996");
    }

    // ── A_ANALYSER : configuration ambigüe ─────────────────────────────────

    @Test
    void analyze_statutHandicapIndetermine_returnsAanalyser() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.INDETERMINE,
                        DATE_DEMANDE, "Poste ergonomique",
                        new BigDecimal("2000.00"), true,
                        50, new BigDecimal("3000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, false, true, true, true,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("CONFIGURATION_AMBIGUE");
        assertThat(r.synthese()).contains("statut handicap");
    }

    @Test
    void analyze_reponseEmployeurIndeterminee_returnsAanalyser() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        null, false,
                        50, null,
                        DiscriminationBeHandicapAmenagementReponseEmployeur.INDETERMINE,
                        false, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, null, false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.A_ANALYSER);
        assertThat(r.synthese()).contains("réponse de l'employeur");
    }

    @Test
    void analyze_reponseEnAttente_returnsAanalyser() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Télétravail",
                        new BigDecimal("0.00"), false,
                        100, new BigDecimal("5000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.EN_ATTENTE,
                        false, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.A_ANALYSER);
        assertThat(r.synthese()).contains("délai raisonnable");
    }

    // ── FRAGILE : qualification handicap incertaine ────────────────────────

    @Test
    void analyze_statutConteste_returnsFragileQualification() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.CONTESTE,
                        DATE_DEMANDE, "Adaptation poste",
                        new BigDecimal("1000.00"), true,
                        80, new BigDecimal("4000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, false, true, true, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE);
        assertThat(r.handicapQualifie()).isFalse();
        assertThat(r.raison()).isEqualTo("QUALIFICATION_HANDICAP_INCERTAINE");
        assertThat(r.synthese())
                .contains("conteste la qualification")
                .contains("Ring/Werge");
    }

    @Test
    void analyze_durableFactuelSansAvisSepp_returnsFragileQualification() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.DURABLE_FACTUEL,
                        DATE_DEMANDE, "Réaffectation",
                        new BigDecimal("0.00"), false,
                        20, new BigDecimal("800000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, false, true, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("2500.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE);
        assertThat(r.synthese()).contains("situation factuelle durable");
    }

    @Test
    void analyze_durableFactuelAvecAvisSepp_qualifieEtPoursuit() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.DURABLE_FACTUEL,
                        DATE_DEMANDE, "Adaptation poste",
                        new BigDecimal("500.00"), true,
                        60, new BigDecimal("3500000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3200.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        // DURABLE_FACTUEL + avisSeppFavorable → handicap qualifié
        assertThat(r.handicapQualifie()).isTrue();
        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.CONFORME_AMENAGEMENT_ACCORDE);
    }

    // ── DISCRIMINATION_PRESUMEE_NON_MOTIVATION ─────────────────────────────

    @Test
    void analyze_refusNonMotive_returnsDiscriminationPresumee() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Poste ergonomique",
                        new BigDecimal("1500.00"), false,
                        200, new BigDecimal("15000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_NON_MOTIVE,
                        false, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3500.00"), true);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.DISCRIMINATION_PRESUMEE_NON_MOTIVATION);
        assertThat(r.refusCaracterise()).isTrue();
        assertThat(r.indemniteForfaitaire6Mois())
                .isEqualByComparingTo(new BigDecimal("21000.00"));
        assertThat(r.synthese())
                .contains("présumer l'existence d'une discrimination")
                .contains("art. 28 § 3");
    }

    @Test
    void analyze_silenceProlongerEmployeur_returnsDiscriminationPresumee() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Télétravail",
                        new BigDecimal("0.00"), false,
                        50, new BigDecimal("2500000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.NON_REPONDU_RAISONNABLE,
                        false, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.DISCRIMINATION_PRESUMEE_NON_MOTIVATION);
        assertThat(r.synthese()).contains("absence de réponse");
    }

    @Test
    void analyze_refusMotiveDetailleSansMotivationFlag_returnsDiscriminationPresumee() {
        // REFUSE_MOTIVE_DETAILLE mais motivationDetailleeFournie = false →
        // bascule sur la présomption art. 28 § 3.
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Adaptation poste",
                        new BigDecimal("3000.00"), true,
                        100, new BigDecimal("6000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        false, // motivationDetailleeFournie = false
                        false, true, true, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3200.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.DISCRIMINATION_PRESUMEE_NON_MOTIVATION);
    }

    // ── CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE ─────────────────────────

    @Test
    void analyze_chargeDisproportionneeTpe_returnsConforme() {
        // Petite entreprise (5 ETP) + subsides demandés + devis externe →
        // charge disproportionnée démontrée.
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Remplacement intégral du poste de travail",
                        new BigDecimal("85000.00"), true,
                        5, new BigDecimal("500000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, false, true, true, true,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("2800.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE);
        assertThat(r.chargeDisproportionneeDemontree()).isTrue();
        assertThat(r.raison()).isEqualTo("CHARGE_DISPROPORTIONNEE_DEMONTREE");
        assertThat(r.synthese())
                .contains("charge disproportionnée")
                .contains("Cass. 18/12/2017");
    }

    @Test
    void analyze_coutEleveRatioCa_chargeDisproportionneeDemontree() {
        // Coût/CA = 10 % → ratio dépasse seuil 5 % + subsides demandés (2 critères).
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Investissement majeur",
                        new BigDecimal("100000.00"), true,
                        50, new BigDecimal("1000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, false, true, false, true,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE);
    }

    // ── DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE ──────────────────────────

    @Test
    void analyze_refusMotiveMaisSansSubsidesNiDevis_returnsDiscriminationInjustifiee() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Adaptation poste",
                        new BigDecimal("3000.00"), false, // pas de subside
                        200, new BigDecimal("20000000.00"), // grande entreprise
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, true,
                        true,  // charge invoquée
                        false, // pas de devis
                        false, // pas d'alternatives
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3500.00"), true);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE);
        assertThat(r.chargeDisproportionneeDemontree()).isFalse();
        assertThat(r.raison()).isEqualTo("CHARGE_DISPROPORTIONNEE_NON_DEMONTREE");
        assertThat(r.indemniteForfaitaire6Mois())
                .isEqualByComparingTo(new BigDecimal("21000.00"));
        assertThat(r.synthese())
                .contains("subsides AVIQ")
                .contains("devis externe")
                .contains("mesures alternatives");
    }

    @Test
    void analyze_chargeNonInvoquee_returnsDiscriminationInjustifiee() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.MEDECIN_TRAVAIL_AVIS,
                        DATE_DEMANDE, "Télétravail 3 j",
                        new BigDecimal("0.00"), false,
                        500, new BigDecimal("50000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_MOTIVE_DETAILLE,
                        true, true,
                        false, // charge NON invoquée
                        false, true,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3800.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE);
    }

    // ── Indemnité 6 mois indicative ────────────────────────────────────────

    @Test
    void analyze_salaireNull_indemniteNull() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "Adaptation",
                        new BigDecimal("1000.00"), true,
                        60, new BigDecimal("3000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, null, false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.indemniteForfaitaire6Mois()).isNull();
    }

    @Test
    void analyze_salaireZero_indemniteNull() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), false,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, BigDecimal.ZERO, false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.indemniteForfaitaire6Mois()).isNull();
    }

    @Test
    void analyze_salaire2500_indemnite15000() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), false,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_NON_MOTIVE,
                        false, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("2500.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.indemniteForfaitaire6Mois())
                .isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    // ── Priorité hiérarchie : représailles l'emporte sur statut fragile ────

    @Test
    void analyze_licenciementMemeSiStatutConteste_representaillesPrioritaire() {
        // Hiérarchie : A_ANALYSER d'abord (mais ici statut CONTESTE pas
        // INDETERMINE) → puis représailles avant fragile qualification.
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.CONTESTE,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), false,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.REFUSE_NON_MOTIVE,
                        false, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.LICENCIEMENT,
                        DATE_SANCTION, new BigDecimal("3000.00"), true);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.REPRESAILLES_PRESUMEES_LICENCIEMENT);
    }

    // ── Tous les statuts handicap reconnus officiellement ──────────────────

    @Test
    void analyze_statutMedecinTravail_handicapQualifie() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.MEDECIN_TRAVAIL_AVIS,
                        DATE_DEMANDE, "X",
                        new BigDecimal("1000.00"), true,
                        80, new BigDecimal("4000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.handicapQualifie()).isTrue();
        assertThat(r.verdict())
                .isEqualTo(DiscriminationBeHandicapAmenagementVerdict.CONFORME_AMENAGEMENT_ACCORDE);
    }

    @Test
    void analyze_statutInvaliditeMutuelle_handicapQualifie() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.INVALIDITE_MUTUELLE,
                        DATE_DEMANDE, "X",
                        new BigDecimal("1000.00"), true,
                        80, new BigDecimal("4000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, false, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(req);

        assertThat(r.handicapQualifie()).isTrue();
    }

    // ── Base juridique stable ──────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceStable() {
        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(reqAccorde());

        assertThat(r.baseJuridique())
                .contains("Loi du 10/05/2007")
                .contains("art. 14")
                .contains("CCT n° 95")
                .contains("Directive 2000/78/CE")
                .contains("Convention ONU")
                .contains("Cass. (BE) 09/01/2017")
                .contains("CJUE 11/04/2013");
    }

    @Test
    void analyze_avertissement_estPresent() {
        DiscriminationBeHandicapAmenagementResult r =
                DiscriminationBeHandicapAmenagementChecker.analyze(reqAccorde());

        assertThat(r.avertissement())
                .contains("avocat spécialisé en droit social BE")
                .contains("AVIQ")
                .contains("art. 17")
                .contains("UNIA");
    }

    // ── Validation des inputs ──────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête requise");
    }

    @Test
    void analyze_statutHandicapNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        null, DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutHandicap");
    }

    @Test
    void analyze_subsidesNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), null,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subsidesDemandes");
    }

    @Test
    void analyze_effectifNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        null, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_effectifNegatif_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        -5, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positif ou nul");
    }

    @Test
    void analyze_reponseEmployeurNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        null,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reponseEmployeur");
    }

    @Test
    void analyze_sanctionSubieNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        null, null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sanctionSubie");
    }

    @Test
    void analyze_coutNegatif_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("-100.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coutEstimeAmenagement");
    }

    @Test
    void analyze_salaireNegatif_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("-100.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireMensuelBrut");
    }

    @Test
    void analyze_caNegatif_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("-1.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chiffreAffairesAnnuel");
    }

    // ── Tous les flags booléens null → throw ──────────────────────────────

    @Test
    void analyze_motivationDetailleeNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        null, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivationDetailleeFournie");
    }

    @Test
    void analyze_avisSeppNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, null, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avisSeppFavorable");
    }

    @Test
    void analyze_chargeDisproportionneeInvoqueeNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, null, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chargeDisproportionneeInvoquee");
    }

    @Test
    void analyze_devisExterneFourniNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, null, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("devisExterneFourni");
    }

    @Test
    void analyze_mesuresAlternativesNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, null,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), false);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mesuresAlternativesProposees");
    }

    @Test
    void analyze_procedureUniaSaisieNull_throws() {
        DiscriminationBeHandicapAmenagementRequest req =
                new DiscriminationBeHandicapAmenagementRequest(
                        DiscriminationBeHandicapAmenagementStatutHandicap.RECONNU_OFFICIEL,
                        DATE_DEMANDE, "X",
                        new BigDecimal("0.00"), true,
                        50, new BigDecimal("2000000.00"),
                        DiscriminationBeHandicapAmenagementReponseEmployeur.ACCORDE,
                        true, true, false, false, false,
                        DiscriminationBeHandicapAmenagementSanctionSubie.AUCUNE,
                        null, new BigDecimal("3000.00"), null);

        assertThatThrownBy(() ->
                DiscriminationBeHandicapAmenagementChecker.analyze(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("procedureUniaSaisie");
    }
}

package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.ProtectionRpCalculator.CritereRegularite;
import fr.ailegalcase.casefile.ProtectionRpCalculator.MotifLicenciement;
import fr.ailegalcase.casefile.ProtectionRpCalculator.ProcedureSuivie;
import fr.ailegalcase.casefile.ProtectionRpCalculator.StatutProtege;
import fr.ailegalcase.casefile.ProtectionRpCalculator.VerdictLegalite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProtectionRpCalculatorTest {

    private static final LocalDate MANDAT_FUTUR = LocalDate.of(2026, 9, 30);
    private static final LocalDate RUPTURE = LocalDate.of(2026, 4, 15);
    private static final double SALAIRE = 3500.0;

    // ============ Verdict VALIDE ============

    @Test
    void autorisation_obtenue_motif_faute_grave_returns_VALIDE_score100() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE,
                MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE,
                MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.VALIDE);
        assertThat(r.scoreConformite()).isEqualTo(100);
        assertThat(r.salarieEncoreProtege()).isTrue();
        assertThat(r.criteresRemplis()).contains(
                CritereRegularite.STATUT_PROTEGE_VALIDE,
                CritereRegularite.PROCEDURE_AUTORISATION_DEMANDEE,
                CritereRegularite.AUTORISATION_OBTENUE,
                CritereRegularite.LICENCIEMENT_HORS_PROCEDURE_EN_COURS);
        assertThat(r.criteresManquants()).isEmpty();
    }

    // ============ Verdict NUL ============

    @Test
    void aucune_demande_pendant_protection_returns_NUL() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE,
                MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUCUNE_DEMANDE,
                MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.NUL);
        assertThat(r.scoreConformite()).isEqualTo(0);
        assertThat(r.criteresManquants()).contains(
                CritereRegularite.PROCEDURE_AUTORISATION_DEMANDEE,
                CritereRegularite.AUTORISATION_OBTENUE);
    }

    @Test
    void autorisation_refusee_licenciement_prononce_returns_NUL() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.DELEGUE_SYNDICAL,
                MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_REFUSEE,
                MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.NUL);
        assertThat(r.scoreConformite()).isEqualTo(0);
        assertThat(r.criteresManquants()).contains(CritereRegularite.AUTORISATION_OBTENUE);
    }

    // ============ Verdict CONTESTABLE ============

    @Test
    void en_cours_instruction_returns_CONTESTABLE() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_SUPPLEANT,
                MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.EN_COURS_INSTRUCTION,
                MotifLicenciement.INSUFFISANCE_PRO,
                SALAIRE, "FRANCE");
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.CONTESTABLE);
        assertThat(r.scoreConformite()).isEqualTo(30);
        assertThat(r.criteresManquants()).contains(
                CritereRegularite.LICENCIEMENT_HORS_PROCEDURE_EN_COURS,
                CritereRegularite.AUTORISATION_OBTENUE);
    }

    // ============ Hors période de protection ============

    @Test
    void salarie_hors_periode_protection_returns_VALIDE_message_hors_protection() {
        // Mandat expiré le 2025-09-30 + 6 mois = 2026-03-30
        // Rupture le 2026-04-15 → hors protection
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE,
                LocalDate.of(2025, 9, 30),
                LocalDate.of(2026, 4, 15),
                ProcedureSuivie.AUCUNE_DEMANDE,
                MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.salarieEncoreProtege()).isFalse();
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.VALIDE);
        assertThat(r.scoreConformite()).isEqualTo(100);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("hors période de protection"));
    }

    @Test
    void periode_6_mois_post_mandat_limite_haute_incluse() {
        // Mandat expiré 2025-10-15 + 6 mois = 2026-04-15
        // Rupture exactement 2026-04-15 → encore protégé (limite incluse)
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE,
                LocalDate.of(2025, 10, 15),
                LocalDate.of(2026, 4, 15),
                ProcedureSuivie.AUTORISATION_OBTENUE,
                MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.salarieEncoreProtege()).isTrue();
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.VALIDE);
    }

    // ============ Tous les statuts protégés ============

    @Test
    void statutCse_titulaire_meme_protection_que_suppleant() {
        ProtectionRpResult titulaire = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.AUTRE,
                SALAIRE, "FRANCE");
        ProtectionRpResult suppleant = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_SUPPLEANT, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.AUTRE,
                SALAIRE, "FRANCE");
        assertThat(titulaire.verdictLegalite()).isEqualTo(suppleant.verdictLegalite());
        assertThat(titulaire.salarieEncoreProtege()).isEqualTo(suppleant.salarieEncoreProtege());
    }

    @Test
    void delegue_syndical_protege() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.DELEGUE_SYNDICAL, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUCUNE_DEMANDE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.salarieEncoreProtege()).isTrue();
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.NUL);
    }

    @Test
    void conseiller_prudhommes_protege() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.CONSEILLER_PRUDHOMMES, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.statutProtege()).isEqualTo(StatutProtege.CONSEILLER_PRUDHOMMES);
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.VALIDE);
        assertThat(r.messages()).anyMatch(m -> m.contains("L.2411-22"));
    }

    @Test
    void medecin_travail_protege_specifique() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEDECIN_TRAVAIL, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUCUNE_DEMANDE, MotifLicenciement.AUTRE,
                SALAIRE, "FRANCE");
        assertThat(r.statutProtege()).isEqualTo(StatutProtege.MEDECIN_TRAVAIL);
        assertThat(r.verdictLegalite()).isEqualTo(VerdictLegalite.NUL);
        assertThat(r.messages()).anyMatch(m -> m.contains("L.2411-5"));
    }

    // ============ Score de conformité ============

    @Test
    void score_100_si_VALIDE() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.scoreConformite()).isEqualTo(100);
    }

    @Test
    void score_30_si_CONTESTABLE() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.EN_COURS_INSTRUCTION, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.scoreConformite()).isEqualTo(30);
    }

    @Test
    void score_0_si_NUL() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUCUNE_DEMANDE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.scoreConformite()).isEqualTo(0);
    }

    // ============ Indemnités ============

    @Test
    void indemniteForfaitaire_min_6_mois_salaire_si_NUL() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUCUNE_DEMANDE, MotifLicenciement.FAUTE_GRAVE,
                3000.0, "FRANCE");
        // 6 mois × 3000 = 18000
        assertThat(r.indemniteForfaitaireMinEur()).isEqualTo(18000.0);
    }

    @Test
    void salaire_eviction_calcul_si_NUL() {
        // Mandat expirera 2026-09-30 (+6 mois = 2027-03-30 fin protection)
        // Rupture 2026-04-15 → environ 11 mois d'éviction × 3000 = ~33000
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE,
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 4, 15),
                ProcedureSuivie.AUCUNE_DEMANDE, MotifLicenciement.FAUTE_GRAVE,
                3000.0, "FRANCE");
        assertThat(r.salaireEvictionPotentielEur()).isGreaterThanOrEqualTo(3000.0);
    }

    @Test
    void indemnite_zero_si_VALIDE() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.indemniteForfaitaireMinEur()).isEqualTo(0.0);
        assertThat(r.salaireEvictionPotentielEur()).isEqualTo(0.0);
    }

    // ============ Messages et listes ============

    @Test
    void criteresNonRemplis_explicite_si_AUCUNE_DEMANDE() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUCUNE_DEMANDE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.criteresManquants())
                .contains(CritereRegularite.PROCEDURE_AUTORISATION_DEMANDEE,
                        CritereRegularite.AUTORISATION_OBTENUE);
    }

    @Test
    void baseJuridique_contient_L2411() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.baseJuridique()).contains("L.2411");
        assertThat(r.baseJuridique()).contains("L.2422-1");
    }

    @Test
    void messages_alerte_si_NUL() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUCUNE_DEMANDE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("NUL"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("réintégration"));
    }

    @Test
    void delaiContestation_returns_60() {
        ProtectionRpResult r = ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE");
        assertThat(r.delaiContestationJours()).isEqualTo(60);
    }

    // ============ Validation ============

    @Test
    void validation_statutProtege_null_throws() {
        assertThatThrownBy(() -> ProtectionRpCalculator.compute(
                null, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Statut");
    }

    @Test
    void validation_dateExpirationMandat_null_throws() {
        assertThatThrownBy(() -> ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, null, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiration");
    }

    @Test
    void validation_datePresumeeRupture_null_throws() {
        assertThatThrownBy(() -> ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, null,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rupture");
    }

    @Test
    void validation_procedureSuivie_null_throws() {
        assertThatThrownBy(() -> ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                null, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Procédure");
    }

    @Test
    void validation_motifLicenciement_null_throws() {
        assertThatThrownBy(() -> ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, null,
                SALAIRE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motif");
    }

    @Test
    void validation_country_BELGIQUE_throws() {
        assertThatThrownBy(() -> ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                SALAIRE, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void validation_salaireNegatif_throws() {
        assertThatThrownBy(() -> ProtectionRpCalculator.compute(
                StatutProtege.MEMBRE_CSE_TITULAIRE, MANDAT_FUTUR, RUPTURE,
                ProcedureSuivie.AUTORISATION_OBTENUE, MotifLicenciement.FAUTE_GRAVE,
                -100.0, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

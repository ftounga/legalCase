package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MesuresEloignementCalculatorTest {

    private static final LocalDate ANALYSE = LocalDate.of(2026, 4, 25);

    // ---- EXPULSION_PREFECTORALE ------------------------------------------

    @Test
    void expulsionPrefectorale_commissionRespectee_ordrePublic_returnsVALIDE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("VALIDE");
        assertThat(r.dispositif()).isEqualTo("EXPULSION_PREFECTORALE");
        assertThat(r.delaiRecoursJours()).isEqualTo(30);
        assertThat(r.juridictionRecours()).isEqualTo("TA");
        assertThat(r.baseJuridique()).contains("L.631-1");
        assertThat(r.documentsRequis()).isNotEmpty();
    }

    @Test
    void expulsionPrefectorale_commissionNonRespectee_sansUrgence_returnsCONTESTABLE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                false, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("CONTESTABLE");
        assertThat(r.risqueAnnulation()).anySatisfy(m ->
                assertThat(m).contains("commission expulsion"));
    }

    @Test
    void expulsionPrefectorale_commissionNonRespectee_urgenceAbsolueJustifiee_returnsVALIDE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                false, true, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("VALIDE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Urgence absolue"));
    }

    @Test
    void expulsionPrefectorale_motifAutreSeul_returnsNUL() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", "AUTRE",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("NUL");
        assertThat(r.risqueAnnulation()).anySatisfy(m -> assertThat(m).contains("Motif AUTRE"));
    }

    // ---- EXPULSION_MINISTERIELLE -----------------------------------------

    @Test
    void expulsionMinisterielle_urgenceTerrorisme_returnsVALIDE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_MINISTERIELLE", "TERRORISME",
                true, true, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("VALIDE");
        assertThat(r.delaiRecoursJours()).isEqualTo(60);
        assertThat(r.juridictionRecours()).isEqualTo("CE");
        assertThat(r.baseJuridique()).contains("L.631-2");
    }

    @Test
    void expulsionMinisterielle_sansUrgence_returnsCONTESTABLE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_MINISTERIELLE", "ORDRE_PUBLIC",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("CONTESTABLE");
        assertThat(r.risqueAnnulation()).anySatisfy(m -> assertThat(m).contains("Urgence"));
    }

    // ---- EXPULSION_SECURITE_ETAT -----------------------------------------

    @Test
    void expulsionSecuriteEtat_motifSecurite_returnsVALIDE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_SECURITE_ETAT", "SECURITE_ETAT",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("VALIDE");
        assertThat(r.delaiRecoursJours()).isEqualTo(60);
        assertThat(r.juridictionRecours()).isEqualTo("CE");
        assertThat(r.baseJuridique()).contains("L.631-3");
    }

    @Test
    void expulsionSecuriteEtat_motifAutre_returnsCONTESTABLE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_SECURITE_ETAT", "AUTRE",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("CONTESTABLE");
    }

    // ---- IRTF ------------------------------------------------------------

    @Test
    void irtf_presenceLongue_comportementAggravant_returnsVALIDE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "IRTF", "ORDRE_PUBLIC",
                true, false, 6, 24, true, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("VALIDE");
        assertThat(r.delaiRecoursJours()).isEqualTo(15);
        assertThat(r.juridictionRecours()).isEqualTo("TA");
        assertThat(r.baseJuridique()).contains("L.612-6");
    }

    @Test
    void irtf_motifAutre_presenceCourte_pasComportement_returnsNUL() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "IRTF", "AUTRE",
                true, false, 1, 0, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("NUL");
    }

    @Test
    void irtf_presenceCourte_pasComportement_returnsCONTESTABLE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "IRTF", "ORDRE_PUBLIC",
                true, false, 4, 4, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("CONTESTABLE");
        assertThat(r.risqueAnnulation()).anySatisfy(m -> assertThat(m).contains("Fondement"));
    }

    // ---- IAT --------------------------------------------------------------

    @Test
    void iat_motifTerrorisme_returnsVALIDE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "IAT", "TERRORISME",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("VALIDE");
        assertThat(r.delaiRecoursJours()).isEqualTo(60);
        assertThat(r.juridictionRecours()).isEqualTo("CE");
        assertThat(r.baseJuridique()).contains("L.222-1");
    }

    @Test
    void iat_motifAutre_returnsCONTESTABLE() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "IAT", "AUTRE",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.verdictLegalite()).isEqualTo("CONTESTABLE");
    }

    // ---- Délais et juridictions par dispositif ---------------------------

    @Test
    void delaiRecours_paireeAuDispositif() {
        assertThat(MesuresEloignementCalculator.compute("IRTF", "ORDRE_PUBLIC",
                true, false, 0, 24, true, null, ANALYSE).delaiRecoursJours()).isEqualTo(15);
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                true, false, null, null, false, null, ANALYSE).delaiRecoursJours()).isEqualTo(30);
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_MINISTERIELLE", "TERRORISME",
                true, true, null, null, false, null, ANALYSE).delaiRecoursJours()).isEqualTo(60);
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_SECURITE_ETAT", "SECURITE_ETAT",
                true, false, null, null, false, null, ANALYSE).delaiRecoursJours()).isEqualTo(60);
        assertThat(MesuresEloignementCalculator.compute("IAT", "TERRORISME",
                true, false, null, null, false, null, ANALYSE).delaiRecoursJours()).isEqualTo(60);
    }

    @Test
    void juridictionRecours_paireeAuDispositif() {
        assertThat(MesuresEloignementCalculator.compute("IRTF", "ORDRE_PUBLIC",
                true, false, 0, 24, true, null, ANALYSE).juridictionRecours()).isEqualTo("TA");
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                true, false, null, null, false, null, ANALYSE).juridictionRecours()).isEqualTo("TA");
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_MINISTERIELLE", "TERRORISME",
                true, true, null, null, false, null, ANALYSE).juridictionRecours()).isEqualTo("CE");
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_SECURITE_ETAT", "SECURITE_ETAT",
                true, false, null, null, false, null, ANALYSE).juridictionRecours()).isEqualTo("CE");
        assertThat(MesuresEloignementCalculator.compute("IAT", "TERRORISME",
                true, false, null, null, false, null, ANALYSE).juridictionRecours()).isEqualTo("CE");
    }

    @Test
    void baseJuridique_referenceCorrecteParDispositif() {
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                true, false, null, null, false, null, ANALYSE).baseJuridique()).contains("L.631-1");
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_MINISTERIELLE", "TERRORISME",
                true, true, null, null, false, null, ANALYSE).baseJuridique()).contains("L.631-2");
        assertThat(MesuresEloignementCalculator.compute("EXPULSION_SECURITE_ETAT", "SECURITE_ETAT",
                true, false, null, null, false, null, ANALYSE).baseJuridique()).contains("L.631-3");
        assertThat(MesuresEloignementCalculator.compute("IRTF", "ORDRE_PUBLIC",
                true, false, 0, 24, true, null, ANALYSE).baseJuridique()).contains("L.612-6");
        assertThat(MesuresEloignementCalculator.compute("IAT", "TERRORISME",
                true, false, null, null, false, null, ANALYSE).baseJuridique()).contains("L.222-1");
    }

    // ---- Validation des inputs --------------------------------------------

    @Test
    void dispositifInvalide_throwsIllegalArgument() {
        assertThatThrownBy(() -> MesuresEloignementCalculator.compute(
                "AUTRE_DISPOSITIF", "ORDRE_PUBLIC",
                true, false, null, null, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dispositif non supporté");
    }

    @Test
    void dispositifNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> MesuresEloignementCalculator.compute(
                null, "ORDRE_PUBLIC",
                true, false, null, null, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositif est requis");
    }

    @Test
    void motifInvalide_throwsIllegalArgument() {
        assertThatThrownBy(() -> MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", "MOTIF_BIDON",
                true, false, null, null, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifMenace non supporté");
    }

    @Test
    void motifNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", null,
                true, false, null, null, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifMenace est requis");
    }

    @Test
    void dureeCirculariteNegative_throwsIllegalArgument() {
        assertThatThrownBy(() -> MesuresEloignementCalculator.compute(
                "IRTF", "ORDRE_PUBLIC",
                true, false, -1, 24, true, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dureePresenceNegative_throwsIllegalArgument() {
        assertThatThrownBy(() -> MesuresEloignementCalculator.compute(
                "IRTF", "ORDRE_PUBLIC",
                true, false, 6, -2, true, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recoursDelaiTropEloigne_throwsIllegalArgument() {
        LocalDate trop = ANALYSE.plusYears(2);
        assertThatThrownBy(() -> MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                true, false, null, null, false, trop, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trop éloignée");
    }

    @Test
    void recoursDelaiExpire_addsMessageEtRisque() {
        // recoursDelai bien antérieure (au-delà du délai 30 j pour expulsion préfectorale)
        LocalDate ancien = ANALYSE.minusDays(60);
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                true, false, null, null, false, ancien, ANALYSE);

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Délai de recours dépassé"));
        assertThat(r.risqueAnnulation()).anySatisfy(m -> assertThat(m).contains("forclos"));
    }

    @Test
    void formuleResume_contientDispositifEtVerdict() {
        MesuresEloignementResult r = MesuresEloignementCalculator.compute(
                "IAT", "TERRORISME",
                true, false, null, null, false, null, ANALYSE);

        assertThat(r.formule()).contains("VALIDE").contains("CE").contains("60");
    }
}

package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-01 : tests unitaires de {@link EtrangerMaladeAnalyzer}.
 * Couvre les 4 verdicts, le calcul du délai TA, la validation des entrées,
 * et les critères de non-remplissage.
 */
class EtrangerMaladeAnalyzerTest {

    private static final String PATHOLOGIE = "Tuberculose multi-résistante";
    private static final String PAYS = "Mali";
    private static final LocalDate DATE_AVIS = LocalDate.of(2026, 3, 15);

    // ── Verdicts nominaux ────────────────────────────────────────────────

    @Test
    void analyze_avisOFII_null_retourne_EN_ATTENTE_AVIS_OFII() {
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, null, null);

        assertThat(r.verdict()).isEqualTo(EtrangerMaladeAnalyzer.VERDICT_EN_ATTENTE_AVIS_OFII);
        assertThat(r.delaiRecoursTA()).isNull();
        assertThat(r.motifRecours()).isNull();
        assertThat(r.chipsCriteresNonRemplis()).doesNotContain("PATHOLOGIE_NON_RENSEIGNEE");
        assertThat(r.baseJuridique()).contains("L. 425-9");
    }

    @Test
    void analyze_avisOFII_EN_ATTENTE_retourne_EN_ATTENTE_AVIS_OFII() {
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "EN_ATTENTE", null);

        assertThat(r.verdict()).isEqualTo(EtrangerMaladeAnalyzer.VERDICT_EN_ATTENTE_AVIS_OFII);
        assertThat(r.chipsCriteresNonRemplis()).isEmpty();
    }

    @Test
    void analyze_avisOFII_FAVORABLE_traitementIndisponible_retourne_ELIGIBLE_PROBABLE() {
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "FAVORABLE", DATE_AVIS);

        assertThat(r.verdict()).isEqualTo(EtrangerMaladeAnalyzer.VERDICT_ELIGIBLE_PROBABLE);
        assertThat(r.chipsCriteresNonRemplis()).isEmpty();
        assertThat(r.delaiRecoursTA()).isNull();
    }

    @Test
    void analyze_avisOFII_FAVORABLE_traitementDisponible_retourne_ELIGIBLE_SOUS_RESERVE() {
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, true, "FAVORABLE", DATE_AVIS);

        assertThat(r.verdict()).isEqualTo(EtrangerMaladeAnalyzer.VERDICT_ELIGIBLE_SOUS_RESERVE);
        assertThat(r.chipsCriteresNonRemplis()).contains("TRAITEMENT_DISPONIBLE_PAYS_ORIGINE");
    }

    @Test
    void analyze_avisOFII_DEFAVORABLE_criteresRemplis_retourne_ELIGIBLE_PROBABLE_avec_delaiTA() {
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "DEFAVORABLE", DATE_AVIS);

        assertThat(r.verdict()).isEqualTo(EtrangerMaladeAnalyzer.VERDICT_ELIGIBLE_PROBABLE);
        assertThat(r.delaiRecoursTA()).isEqualTo(DATE_AVIS.plusMonths(2));
        assertThat(r.motifRecours()).contains(PATHOLOGIE);
        assertThat(r.motifRecours()).contains(PAYS);
        assertThat(r.motifRecours()).contains("L. 425-9");
        assertThat(r.chipsCriteresNonRemplis()).contains("AVIS_OFII_DEFAVORABLE");
    }

    @Test
    void analyze_avisOFII_DEFAVORABLE_traitementDisponible_retourne_NON_ELIGIBLE() {
        // Traitement disponible au pays d'origine + avis DEFAVORABLE → NON_ELIGIBLE
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, true, "DEFAVORABLE", DATE_AVIS);

        assertThat(r.verdict()).isEqualTo(EtrangerMaladeAnalyzer.VERDICT_NON_ELIGIBLE);
        assertThat(r.chipsCriteresNonRemplis())
                .contains("TRAITEMENT_DISPONIBLE_PAYS_ORIGINE")
                .contains("AVIS_OFII_DEFAVORABLE");
        assertThat(r.delaiRecoursTA()).isEqualTo(DATE_AVIS.plusMonths(2));
    }

    @Test
    void analyze_retourne_pathologie_et_paysOrigine_inchanges() {
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "FAVORABLE", null);

        assertThat(r.pathologiePrincipale()).isEqualTo(PATHOLOGIE);
        assertThat(r.paysOrigine()).isEqualTo(PAYS);
        assertThat(r.traitementDisponiblePaysOrigine()).isFalse();
        assertThat(r.avisOFII()).isEqualTo("FAVORABLE");
    }

    @Test
    void analyze_delai_TA_calcul_exact_2_mois() {
        LocalDate avis = LocalDate.of(2026, 1, 31);
        EtrangerMaladeResult r = EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "DEFAVORABLE", avis);

        assertThat(r.delaiRecoursTA()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    // ── Validation des entrées ───────────────────────────────────────────

    @Test
    void analyze_pathologieNulle_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> EtrangerMaladeAnalyzer.analyze(
                null, PAYS, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pathologiePrincipale");
    }

    @Test
    void analyze_pathologieVide_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> EtrangerMaladeAnalyzer.analyze(
                "   ", PAYS, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pathologiePrincipale");
    }

    @Test
    void analyze_pathologieTropLongue_lanceIllegalArgumentException() {
        String trop = "x".repeat(501);
        assertThatThrownBy(() -> EtrangerMaladeAnalyzer.analyze(
                trop, PAYS, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void analyze_paysOrigineNul_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, null, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paysOrigine");
    }

    @Test
    void analyze_avisOFII_DEFAVORABLE_sans_date_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "DEFAVORABLE", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAvisOFII");
    }

    @Test
    void analyze_avisOFII_inconnu_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "INCONNU", DATE_AVIS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avisOFII");
    }

    @Test
    void analyze_dateAvisOFII_future_lanceIllegalArgumentException() {
        LocalDate futur = LocalDate.now().plusDays(2);
        assertThatThrownBy(() -> EtrangerMaladeAnalyzer.analyze(
                PATHOLOGIE, PAYS, false, "DEFAVORABLE", futur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAvisOFII");
    }
}

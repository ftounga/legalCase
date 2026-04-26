package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.ReconnaissancePaterneleCalculator.SousType;
import fr.ailegalcase.casefile.ReconnaissancePaterneleCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReconnaissancePaterneleCalculatorTest {

    private static final LocalDate DATE_NAISSANCE = LocalDate.of(2024, 3, 15);
    private static final LocalDate DATE_RECO = LocalDate.of(2024, 2, 10);

    // ============ Verdict ELEVEE ============

    @Test
    void prenatale_tousCriteres_returnsELEVEE() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.scoreEligibilite()).isGreaterThanOrEqualTo(90);
        assertThat(r.delaiContestationAns()).isEqualTo(10);
    }

    @Test
    void postNatale_naissance_tousCriteres_returnsELEVEE() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_NAISSANCE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                true, true, true, true, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.effetFiliation()).isEqualTo(DATE_NAISSANCE);
    }

    @Test
    void postNatale_ulterieure_tousCriteres_returnsELEVEE_effetRetroactif() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                DATE_NAISSANCE, LocalDate.of(2025, 6, 1),
                true, true, true, true, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.effetFiliation()).isEqualTo(DATE_NAISSANCE);
    }

    // ============ Verdict MOYENNE ============

    @Test
    void paterniteNonVraisemblable_returnsMOYENNE() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                true, false, true, true, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.MOYENNE);
        assertThat(r.risquesContestation()).anyMatch(s ->
                s.toLowerCase().contains("vraisemblable") || s.toLowerCase().contains("possession"));
    }

    // ============ Verdict FAIBLE ============

    @Test
    void consentementVice_returnsFAIBLE() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                false, true, true, true, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.risquesContestation()).anyMatch(s ->
                s.toLowerCase().contains("consentement"));
        assertThat(r.messages()).anyMatch(m -> m.contains("Consentement NON libre"));
    }

    @Test
    void enfantDejaReconnu_returnsFAIBLE() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                true, true, false, true, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.risquesContestation()).anyMatch(s ->
                s.toLowerCase().contains("déjà reconnu"));
    }

    @Test
    void procedureNonRespectee_returnsFAIBLE() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_NAISSANCE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                true, true, true, false, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.risquesContestation()).anyMatch(s ->
                s.toLowerCase().contains("procédure"));
    }

    @Test
    void cumulDefaillances_returnsFAIBLE() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                false, false, false, false, false, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.risquesContestation()).hasSizeGreaterThanOrEqualTo(3);
    }

    // ============ Documents requis ============

    @Test
    void documentsRequis_prenatale_neContientPasActeNaissance() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                null, DATE_RECO,
                true, true, true, true, false, "FRANCE");
        assertThat(r.documentsRequis()).anyMatch(d -> d.toLowerCase().contains("identité"));
        assertThat(r.documentsRequis()).noneMatch(d ->
                d.toLowerCase().contains("acte de naissance"));
    }

    @Test
    void documentsRequis_ulterieure_contientActeNaissance() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                true, true, true, true, false, "FRANCE");
        assertThat(r.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("acte de naissance"));
    }

    @Test
    void documentsRequis_avecProcuration_contientProcuration() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                true, true, true, true, true, "FRANCE");
        assertThat(r.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("procuration"));
    }

    // ============ Effet filiation ============

    @Test
    void effetFiliation_postNatale_estDateNaissance() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                DATE_NAISSANCE, LocalDate.of(2025, 1, 1),
                true, true, true, true, false, "FRANCE");
        assertThat(r.effetFiliation()).isEqualTo(DATE_NAISSANCE);
    }

    @Test
    void effetFiliation_prenatale_sansDateNaissance_estNull() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                null, DATE_RECO,
                true, true, true, true, false, "FRANCE");
        assertThat(r.effetFiliation()).isNull();
    }

    // ============ Délai contestation ============

    @Test
    void delaiContestation_estToujours10Ans() {
        for (SousType st : SousType.values()) {
            LocalDate dn = (st == SousType.RECONNAISSANCE_PRENATALE) ? null : DATE_NAISSANCE;
            ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                    st, dn, DATE_RECO, true, true, true, true, false, "FRANCE");
            assertThat(r.delaiContestationAns()).isEqualTo(10);
        }
    }

    // ============ Base juridique ============

    @Test
    void baseJuridique_contient_316_332_372() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, "FRANCE");
        assertThat(r.baseJuridique()).contains("316");
        assertThat(r.baseJuridique()).contains("332");
        assertThat(r.baseJuridique()).contains("372");
    }

    // ============ Country ============

    @Test
    void country_FRANCE_normalized() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    // ============ Validations ============

    @Test
    void validation_sousType_null_throws() {
        assertThatThrownBy(() -> ReconnaissancePaterneleCalculator.compute(
                null, DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sous-type");
    }

    @Test
    void validation_consentement_null_throws() {
        assertThatThrownBy(() -> ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                null, true, true, true, false, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consentement");
    }

    @Test
    void validation_paterniteVraisemblable_null_throws() {
        assertThatThrownBy(() -> ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, null, true, true, false, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_postNatale_dateNaissanceNull_throws() {
        assertThatThrownBy(() -> ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_ULTERIEURE,
                null, DATE_RECO,
                true, true, true, true, false, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Date de naissance");
    }

    @Test
    void validation_country_null_throws() {
        assertThatThrownBy(() -> ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Messages / formule ============

    @Test
    void formule_contient_score_et_verdict() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, "FRANCE");
        assertThat(r.formule()).contains("score");
        assertThat(r.formule()).contains("ELEVEE");
    }

    @Test
    void messages_contiennent_libelle_sous_type() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_POST_NATALE_NAISSANCE,
                DATE_NAISSANCE, DATE_NAISSANCE,
                true, true, true, true, false, "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.contains("acte de naissance") || m.contains("art. 316"));
    }

    @Test
    void messages_mentionnent_droits_devoirs() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, false, "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("autorité parentale"));
    }

    @Test
    void presenceParProcuration_null_traiteCommeFalse() {
        ReconnaissancePaterneleResult r = ReconnaissancePaterneleCalculator.compute(
                SousType.RECONNAISSANCE_PRENATALE,
                DATE_NAISSANCE, DATE_RECO,
                true, true, true, true, null, "FRANCE");
        assertThat(r.presenceParProcuration()).isFalse();
        assertThat(r.documentsRequis()).noneMatch(d ->
                d.toLowerCase().contains("procuration"));
    }
}

package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.TransfertEntrepriseL12241Calculator.AnalyseTransfert;
import static fr.ailegalcase.casefile.TransfertEntrepriseL12241Calculator.CodePoint;
import static fr.ailegalcase.casefile.TransfertEntrepriseL12241Calculator.TypeTransfert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-05 — tests unitaires du calculateur d'applicabilité de l'art.
 * L. 1224-1 CT (transfert d'entreprise — FRANCE).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict APPLICABLE
 * (EEA + activité préservée), INAPPLICABLE (pas d'EEA), INCERTAIN (EEA sans
 * activité), alerte licenciements frauduleux, alerte défaut CSE, gate
 * country FRANCE.</p>
 */
class TransfertEntrepriseL12241CalculatorTest {

    /** Cession classique avec EEA + activité préservée + CSE consulté → APPLICABLE. */
    private static TransfertEntrepriseL12241Input cessionReguliere() {
        return new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                true,    // EEA identifiée
                true,    // activité préservée
                true,    // salariés transférés
                false,   // contrats non modifiés
                false,   // pas de licenciements pré-transfert
                0,
                true,    // CSE consulté
                null
        );
    }

    // ── AC1 : EEA + activité préservée → APPLICABLE ──────────────────────────

    @Test
    void eeaIdentifiee_activitePreservee_returnsApplicable() {
        var r = TransfertEntrepriseL12241Calculator.compute(cessionReguliere(), "FRANCE");
        assertThat(r.analyseL1224_1()).isEqualTo(AnalyseTransfert.L1224_APPLICABLE);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.scoreApplicabilite()).isGreaterThanOrEqualTo(80);
        assertThat(r.pointsAnalyse())
                .anyMatch(p -> p.code() == CodePoint.DT72_EEA_IDENTIFIEE)
                .anyMatch(p -> p.code() == CodePoint.DT72_ACTIVITE_PRESERVEE);
    }

    // ── AC2 : EEA non identifiée → INAPPLICABLE ──────────────────────────────

    @Test
    void eeaNonIdentifiee_returnsInapplicable() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                false,   // EEA non identifiée
                true,
                true,
                false,
                false,
                0,
                true,
                null
        );
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.analyseL1224_1()).isEqualTo(AnalyseTransfert.L1224_INAPPLICABLE);
        assertThat(r.scoreApplicabilite()).isLessThan(30);
    }

    @Test
    void eeaIdentifiee_sansActivite_returnsIncertain() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                true,    // EEA identifiée
                false,   // activité non préservée
                true,
                false,
                false,
                0,
                true,
                null
        );
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.analyseL1224_1()).isEqualTo(AnalyseTransfert.L1224_INCERTAIN);
    }

    // ── AC3 : Licenciements pré-transfert → alerte frauduleux ────────────────

    @Test
    void licenciementsPreTransfert_declencheAlerteFrauduleux() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                true, true, true, false,
                true,    // licenciements pré-transfert
                3,       // 3 salariés licenciés
                true,
                null
        );
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.alerteLicenciementsFrauduleux()).isTrue();
        assertThat(r.pointsAnalyse())
                .anyMatch(p -> p.code() == CodePoint.DT72_LICENCIEMENTS_PRE_TRANSFERT);
        assertThat(r.messages())
                .anyMatch(m -> m.contains("ALERTE") && m.contains("licenciement"));
    }

    @Test
    void licenciementsPreTransfert_avecNbZero_pasDAlerte() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                true, true, true, false,
                true,    // flag true mais nb=0 → pas d'alerte
                0,
                true,
                null
        );
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.alerteLicenciementsFrauduleux()).isFalse();
    }

    // ── AC4 : CSE non consulté → alerte défaut consultation ──────────────────

    @Test
    void cseNonConsulte_declencheAlerteDefautConsultation() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                true, true, true, false,
                false, 0,
                false,   // CSE non consulté
                null
        );
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.alerteDefautConsultationCse()).isTrue();
        assertThat(r.messages())
                .anyMatch(m -> m.contains("CSE"));
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1224-3"));
    }

    @Test
    void cseConsulte_pasDAlerteDefautConsultation() {
        var r = TransfertEntrepriseL12241Calculator.compute(cessionReguliere(), "FRANCE");
        assertThat(r.alerteDefautConsultationCse()).isFalse();
    }

    // ── AC5 : country != FRANCE → exception ──────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                TransfertEntrepriseL12241Calculator.compute(cessionReguliere(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                TransfertEntrepriseL12241Calculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void nbLicenciementsNegatif_throwsIllegalArgument() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                true, true, true, false,
                true, -1, true, null
        );
        assertThatThrownBy(() ->
                TransfertEntrepriseL12241Calculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    // ── Types de transfert ──────────────────────────────────────────────────

    @Test
    void typeFusion_returnsApplicable() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.FUSION,
                true, true, true, false, false, 0, true, null);
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.analyseL1224_1()).isEqualTo(AnalyseTransfert.L1224_APPLICABLE);
    }

    @Test
    void typeApportPartielActif_returnsApplicable() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.APPORT_PARTIEL_ACTIF,
                true, true, true, false, false, 0, true, null);
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.analyseL1224_1()).isEqualTo(AnalyseTransfert.L1224_APPLICABLE);
    }

    @Test
    void typeExternalisation_message_specifique() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.EXTERNALISATION,
                true, true, true, false, false, 0, true, null);
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Externalisation"));
    }

    @Test
    void typeRepriseActivite_returnsApplicable() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.REPRISE_ACTIVITE,
                true, true, true, false, false, 0, true, null);
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.analyseL1224_1()).isEqualTo(AnalyseTransfert.L1224_APPLICABLE);
    }

    @Test
    void typeAutre_messageAtypique() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.AUTRE,
                true, true, true, false, false, 0, true, null);
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("atypique"));
    }

    // ── Contrats modifiés par repreneur → alerte + score baissé ──────────────

    @Test
    void contratsModifiesParRepreneur_messageAlerte() {
        var input = new TransfertEntrepriseL12241Input(
                TypeTransfert.CESSION,
                true, true, true,
                true,    // contrats modifiés
                false, 0, true, null);
        var r = TransfertEntrepriseL12241Calculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("modification") || m.contains("Modification") || m.contains("clauses"));
    }

    // ── Score borné dans [0, 100] ────────────────────────────────────────────

    @Test
    void score_borneEntreZeroEtCent() {
        var r = TransfertEntrepriseL12241Calculator.compute(cessionReguliere(), "FRANCE");
        assertThat(r.scoreApplicabilite()).isBetween(0, 100);
    }

    // ── Bases juridiques présentes ──────────────────────────────────────────

    @Test
    void basesJuridiques_contiennent_L1224_1() {
        var r = TransfertEntrepriseL12241Calculator.compute(cessionReguliere(), "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1224-1"));
    }
}

package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.ContestationPaterniteCalculator.QualiteAagir;
import fr.ailegalcase.casefile.ContestationPaterniteCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContestationPaterniteCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 26);
    private static final LocalDate DATE_FILIATION = LocalDate.of(2018, 4, 15);
    private static final LocalDate DATE_CONNAISSANCE_RECENTE = LocalDate.of(2025, 1, 20); // ~16 mois ago
    private static final LocalDate DATE_CONNAISSANCE_ANCIENNE = LocalDate.of(2018, 1, 20); // > 5 ans ago
    private static final LocalDate DATE_MAJORITE_RECENTE = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_MAJORITE_ANCIENNE = LocalDate.of(2010, 6, 1); // > 10 ans ago

    // ============ Verdict ELEVEE ============

    @Test
    void pereDeclare_delaiNonPrescrit_motifsSerieux_adn_returnsELEVEE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.delaiPrescriptionAns()).isEqualTo(5);
        assertThat(r.delaiPrescriptionRestantMois()).isPositive();
        assertThat(r.expertiseAdnRecommandee()).isTrue();
    }

    @Test
    void enfantMajeur_delai10Ans_returnsELEVEE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.ENFANT_MAJEUR,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, DATE_MAJORITE_RECENTE,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.delaiPrescriptionAns()).isEqualTo(10);
    }

    @Test
    void mere_delaiNonPrescrit_returnsELEVEE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.MERE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.delaiPrescriptionAns()).isEqualTo(5);
    }

    @Test
    void pereBiologique_delaiNonPrescrit_returnsELEVEE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_BIOLOGIQUE_PRESUME,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
    }

    // ============ Verdict FAIBLE — prescription ============

    @Test
    void prescriptionAcquise_returnsFAIBLE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_ANCIENNE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.delaiPrescriptionRestantMois()).isLessThanOrEqualTo(0);
        assertThat(r.risquesRefus()).anyMatch(s ->
                s.toLowerCase().contains("prescription"));
        assertThat(r.messages()).anyMatch(m ->
                m.toUpperCase().contains("PRESCRIPTION ACQUISE"));
    }

    @Test
    void enfantMajeur_prescriptionAcquise_returnsFAIBLE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.ENFANT_MAJEUR,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, DATE_MAJORITE_ANCIENNE,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    // ============ Verdict FAIBLE — fin de non-recevoir possession d'état ============

    @Test
    void pereDeclare_possessionEtat5Ans_returnsFAIBLE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                true, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.risquesRefus()).anyMatch(s ->
                s.toLowerCase().contains("fin de non-recevoir")
                        || s.toLowerCase().contains("possession"));
        assertThat(r.messages()).anyMatch(m ->
                m.toUpperCase().contains("FIN DE NON-RECEVOIR"));
    }

    @Test
    void mere_possessionEtat5Ans_returnsFAIBLE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.MERE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                true, true, true, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    @Test
    void enfantMajeur_possessionEtat5Ans_resteRecevable() {
        // Pour l'enfant, la possession d'état 5 ans n'est PAS une fin de non-recevoir
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.ENFANT_MAJEUR,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, DATE_MAJORITE_RECENTE,
                true, true, true, TODAY, "FRANCE");
        // Le verdict reste au moins MOYENNE (pas FAIBLE par fin de non-recevoir)
        assertThat(r.verdictRecevabilite()).isIn(
                VerdictRecevabilite.ELEVEE, VerdictRecevabilite.MOYENNE);
        // Et la fin de non-recevoir n'apparaît pas dans les risques
        assertThat(r.risquesRefus()).noneMatch(s ->
                s.toLowerCase().contains("fin de non-recevoir art. 333 al. 2"));
    }

    // ============ Verdict FAIBLE — pas de motif sérieux ni d'ADN ============

    @Test
    void aucunMotifNiAdn_returnsFAIBLE() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, false, false, TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.risquesRefus()).anyMatch(s ->
                s.toLowerCase().contains("motifs sérieux"));
    }

    // ============ Expertise ADN ============

    @Test
    void expertiseAdnDemandee_recommandeeTrue() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, false, TODAY, "FRANCE");
        assertThat(r.expertiseAdnRecommandee()).isTrue();
    }

    @Test
    void prescrit_expertiseAdnNonRecommandee() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_ANCIENNE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.expertiseAdnRecommandee()).isFalse();
    }

    // ============ Délai prescription restant ============

    @Test
    void delaiRestantMois_positif_quandNonPrescrit() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        // Connaissance 2025-01-20 + 5 ans = 2030-01-20. TODAY = 2026-04-26 → reste ~45 mois
        assertThat(r.delaiPrescriptionRestantMois()).isBetween(40L, 50L);
    }

    @Test
    void delaiRestantMois_negatif_quandPrescrit() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_ANCIENNE, null,
                false, true, true, TODAY, "FRANCE");
        // Connaissance 2018-01-20 + 5 ans = 2023-01-20. TODAY = 2026-04-26 → ~-39 mois
        assertThat(r.delaiPrescriptionRestantMois()).isLessThan(0);
    }

    // ============ Documents ============

    @Test
    void documentsRequis_contiennentActeNaissance() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("acte de naissance"));
        assertThat(r.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("expertise") || d.toLowerCase().contains("adn")
                        || d.toLowerCase().contains("génétique"));
    }

    @Test
    void documentsRequis_specifiquesParQualite() {
        ContestationPaterniteResult rEnfant = ContestationPaterniteCalculator.compute(
                QualiteAagir.ENFANT_MAJEUR,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, DATE_MAJORITE_RECENTE,
                false, true, true, TODAY, "FRANCE");
        assertThat(rEnfant.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("majorité"));

        ContestationPaterniteResult rMere = ContestationPaterniteCalculator.compute(
                QualiteAagir.MERE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(rMere.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("paternité biologique"));
    }

    // ============ Base juridique ============

    @Test
    void baseJuridique_contient_332_333_311_321() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.baseJuridique()).contains("332");
        assertThat(r.baseJuridique()).contains("333"); // implicite via "332-335"
        assertThat(r.baseJuridique()).contains("311-1");
        assertThat(r.baseJuridique()).contains("321");
    }

    // ============ Country ============

    @Test
    void country_FRANCE_normalized() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    // ============ Validations ============

    @Test
    void validation_qualiteAagir_null_throws() {
        assertThatThrownBy(() -> ContestationPaterniteCalculator.compute(
                null, DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Qualité");
    }

    @Test
    void validation_dateEtablissementFiliation_null_throws() {
        assertThatThrownBy(() -> ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                null, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filiation");
    }

    @Test
    void validation_dateConnaissance_null_throws() {
        assertThatThrownBy(() -> ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, null, null,
                false, true, true, TODAY, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("connaissance");
    }

    @Test
    void validation_enfantMajeur_dateMajoriteNull_throws() {
        assertThatThrownBy(() -> ContestationPaterniteCalculator.compute(
                QualiteAagir.ENFANT_MAJEUR,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("majorité");
    }

    @Test
    void validation_country_null_throws() {
        assertThatThrownBy(() -> ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Booleans null traités comme false ============

    @Test
    void booleanNull_traitesCommeFalse() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                null, null, null, TODAY, "FRANCE");
        assertThat(r.possessionEtatConforme5Ans()).isFalse();
        assertThat(r.expertiseAdnDemandee()).isFalse();
        assertThat(r.motifsSerieux()).isFalse();
    }

    // ============ Formule + messages ============

    @Test
    void formule_contient_score_et_verdict() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.formule()).contains("score");
        assertThat(r.formule()).contains("ELEVEE");
    }

    @Test
    void messages_contiennent_libelle_qualite() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.ENFANT_MAJEUR,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, DATE_MAJORITE_RECENTE,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("enfant majeur"));
    }

    @Test
    void messages_mentionnent_tribunalCompetent() {
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, DATE_CONNAISSANCE_RECENTE, null,
                false, true, true, TODAY, "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("tribunal judiciaire"));
    }

    @Test
    void surcharge_sansToday_utilise_now() {
        // Smoke test : la surcharge sans paramètre `today` doit fonctionner
        ContestationPaterniteResult r = ContestationPaterniteCalculator.compute(
                QualiteAagir.PERE_DECLARE,
                DATE_FILIATION, LocalDate.now().minusMonths(6), null,
                false, true, true, "FRANCE");
        assertThat(r).isNotNull();
        assertThat(r.delaiPrescriptionAns()).isEqualTo(5);
    }
}

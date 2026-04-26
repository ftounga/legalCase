package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.RecherchePaterniteCalculator.QualiteDuDemandeur;
import fr.ailegalcase.casefile.RecherchePaterniteCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecherchePaterniteCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 26);

    // Enfant majeur jeune (19 ans) — naissance 2007-04-15, majorité 2025-04-15, forclusion 2035-04-15.
    // Choix : il faut que l'enfant ait consommé < 50% du délai de prescription
    // (10 ans à compter de la majorité) pour bénéficier du bonus +10 du calculator
    // sur le score (cf. branche `pctRestant > 0.5`). À 19 ans, pctRestant ≈ 0.91 → bonus appliqué.
    private static final LocalDate NAISSANCE_MAJEUR_JEUNE = LocalDate.of(2007, 4, 15);
    // Enfant majeur âgé (35 ans, prescription acquise) — naissance 1990-01-15, majorité 2008-01-15, forclusion 2018-01-15
    private static final LocalDate NAISSANCE_MAJEUR_AGE = LocalDate.of(1990, 1, 15);
    // Enfant mineur (8 ans) — naissance 2018-01-15
    private static final LocalDate NAISSANCE_MINEUR = LocalDate.of(2018, 1, 15);
    // Tout jeune mineur (5 ans) — naissance 2021-01-15
    private static final LocalDate NAISSANCE_JEUNE_MINEUR = LocalDate.of(2021, 1, 15);

    // ============ Verdict ELEVEE ============

    @Test
    void enfantMajeurJeune_motifsSerieux_adn_returnsELEVEE() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.delaiPrescriptionAns()).isEqualTo(10);
        assertThat(r.delaiPrescriptionRestantMois()).isPositive();
        assertThat(r.expertiseAdnRecommandee()).isTrue();
    }

    @Test
    void representantLegalMineur_adn_motifs_returnsELEVEE() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.REPRESENTANT_LEGAL_MINEUR,
                NAISSANCE_MINEUR,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.delaiPrescriptionAns()).isEqualTo(10);
        // Mineur : délai forclusion = majorité (8 ans plus tard) + 10 ans = ~216 mois
        assertThat(r.delaiPrescriptionRestantMois()).isGreaterThan(180);
    }

    @Test
    void mere_jeuneMineur_possessionEtat_returnsELEVEE() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.MERE,
                NAISSANCE_JEUNE_MINEUR,
                true, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("possession d'état"));
    }

    // ============ Présomption refus ADN ============

    @Test
    void pereDesigneRefuseAdn_avecAdnDemandee_presomptionRefus() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, true, true,
                TODAY, "FRANCE");
        assertThat(r.presomptionRefusADN()).isTrue();
        assertThat(r.messages()).anyMatch(m ->
                m.toUpperCase().contains("PRÉSOMPTION DE PATERNITÉ"));
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
    }

    @Test
    void pereDesigneRefuseAdn_sansAdnDemandee_pasDePresomption() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, false, true, true,
                TODAY, "FRANCE");
        assertThat(r.presomptionRefusADN()).isFalse();
        assertThat(r.risquesRefus()).anyMatch(s ->
                s.toLowerCase().contains("refus d'adn"));
    }

    // ============ Verdict FAIBLE — prescription ============

    @Test
    void enfantMajeur_prescriptionAcquise_returnsFAIBLE() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_AGE,
                true, true, true, true,
                TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.delaiPrescriptionRestantMois()).isLessThanOrEqualTo(0);
        assertThat(r.risquesRefus()).anyMatch(s ->
                s.toLowerCase().contains("prescription"));
        assertThat(r.messages()).anyMatch(m ->
                m.toUpperCase().contains("PRESCRIPTION ACQUISE"));
    }

    // ============ Verdict FAIBLE — aucun élément ============

    @Test
    void aucunElement_returnsFAIBLE() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, false, false, false,
                TODAY, "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.risquesRefus()).anyMatch(s ->
                s.toLowerCase().contains("faisceau d'indices"));
    }

    // ============ Expertise ADN recommandée ============

    @Test
    void delaiNonPrescrit_expertiseAdnToujoursRecommandee() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, false, false, true,
                TODAY, "FRANCE");
        assertThat(r.expertiseAdnRecommandee()).isTrue();
    }

    @Test
    void prescrit_expertiseAdnNonRecommandee() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_AGE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.expertiseAdnRecommandee()).isFalse();
    }

    // ============ Délai prescription mineur (suspension) ============

    @Test
    void enfantMineur_delaiSuspendu_largementPositif() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.REPRESENTANT_LEGAL_MINEUR,
                NAISSANCE_JEUNE_MINEUR,
                false, true, false, true,
                TODAY, "FRANCE");
        // Naissance 2021-01-15 + 18 ans = 2039-01-15. Forclusion = 2049-01-15. TODAY = 2026-04-26 → ~272 mois
        assertThat(r.delaiPrescriptionRestantMois()).isGreaterThan(250);
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("mineur") && m.toLowerCase().contains("suspendu"));
    }

    @Test
    void enfantMajeurJeune_delaiPositif() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        // Naissance 2007-04-15 + 18 ans = 2025-04-15. Forclusion = 2035-04-15. TODAY = 2026-04-26 → ~107 mois
        assertThat(r.delaiPrescriptionRestantMois()).isBetween(100L, 110L);
    }

    // ============ Documents ============

    @Test
    void documentsRequis_contiennentActeNaissance_etADN() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("acte de naissance"));
        assertThat(r.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("adn") || d.toLowerCase().contains("génétique"));
    }

    @Test
    void documentsRequis_specifiquesParQualite() {
        RecherchePaterniteResult rRep = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.REPRESENTANT_LEGAL_MINEUR,
                NAISSANCE_MINEUR,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(rRep.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("représentant légal"));

        RecherchePaterniteResult rMaj = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(rMaj.documentsRequis()).anyMatch(d ->
                d.toLowerCase().contains("majorité"));
    }

    // ============ Base juridique ============

    @Test
    void baseJuridique_contient_327_340_16_11_321() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.baseJuridique()).contains("327");
        assertThat(r.baseJuridique()).contains("340");
        assertThat(r.baseJuridique()).contains("16-11");
        assertThat(r.baseJuridique()).contains("321");
    }

    // ============ Country ============

    @Test
    void country_FRANCE_normalized() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    // ============ Validations ============

    @Test
    void validation_qualiteDuDemandeur_null_throws() {
        assertThatThrownBy(() -> RecherchePaterniteCalculator.compute(
                null, NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Qualité");
    }

    @Test
    void validation_dateNaissanceEnfant_null_throws() {
        assertThatThrownBy(() -> RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                null,
                false, true, false, true,
                TODAY, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("naissance");
    }

    @Test
    void validation_country_null_throws() {
        assertThatThrownBy(() -> RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ============ Booleans null traités comme false ============

    @Test
    void booleanNull_traitesCommeFalse() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                null, null, null, null,
                TODAY, "FRANCE");
        assertThat(r.presomptionPossessionEtat()).isFalse();
        assertThat(r.expertiseAdnDemandee()).isFalse();
        assertThat(r.pereDesigneRefuseADN()).isFalse();
        assertThat(r.motifsSerieux()).isFalse();
    }

    // ============ Formule + messages ============

    @Test
    void formule_contient_score_et_verdict() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.formule()).contains("score");
        assertThat(r.formule()).contains("ELEVEE");
    }

    @Test
    void messages_contiennent_libelle_qualite() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.REPRESENTANT_LEGAL_MINEUR,
                NAISSANCE_MINEUR,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("représentant légal"));
    }

    @Test
    void messages_mentionnent_tribunalCompetent() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("tribunal judiciaire"));
    }

    @Test
    void messages_mentionnent_caduciteArt340Ancien() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.messages()).anyMatch(m ->
                m.contains("340") || m.toLowerCase().contains("caduque"));
    }

    @Test
    void surcharge_sansToday_utilise_now() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                LocalDate.now().minusYears(25),
                false, true, false, true,
                "FRANCE");
        assertThat(r).isNotNull();
        assertThat(r.delaiPrescriptionAns()).isEqualTo(10);
    }

    // ============ Risques de refus ============

    @Test
    void risquesRefus_nonVidesQuandManquements() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.ENFANT_MAJEUR,
                NAISSANCE_MAJEUR_JEUNE,
                false, false, false, false,
                TODAY, "FRANCE");
        assertThat(r.risquesRefus()).isNotEmpty();
    }

    @Test
    void risquesRefus_avertissementMereEnfantMajeur() {
        RecherchePaterniteResult r = RecherchePaterniteCalculator.compute(
                QualiteDuDemandeur.MERE,
                NAISSANCE_MAJEUR_JEUNE,
                false, true, false, true,
                TODAY, "FRANCE");
        assertThat(r.risquesRefus()).anyMatch(s ->
                s.toLowerCase().contains("enfant majeur") && s.toLowerCase().contains("consenti"));
    }
}

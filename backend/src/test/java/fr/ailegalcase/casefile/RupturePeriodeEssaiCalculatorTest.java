package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.Anomalie;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.AuteurRupture;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CategorieSocioProfessionnelle;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CodeAnomalie;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.DiscriminationMotif;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContrat;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.Verdict;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-DT-38-01 : tests unitaires du moteur de qualification d'une rupture
 * pendant la période d'essai (FR).
 */
class RupturePeriodeEssaiCalculatorTest {

    /**
     * Rupture régulière de référence : cadre, CDI, embauché le 01/01/2025, rupture
     * employeur le 10/04/2025 (~99 jours d'ancienneté), durée essai 4 mois, délai
     * de prévenance 1 mois (30 jours) respecté, motif lié aux compétences, pas
     * de discrimination ni protection violée.
     */
    private RupturePeriodeEssaiInput.Builder ruptureReguliere() {
        return new RupturePeriodeEssaiInput.Builder()
                .categorieSocioProfessionnelle(CategorieSocioProfessionnelle.CADRE)
                .typeContrat(TypeContrat.CDI)
                .dateDebutContrat(LocalDate.of(2025, 1, 1))
                .dateRupture(LocalDate.of(2025, 4, 10))
                .dureePeriodeEssaiContractuelleMois(4)
                .renouvellementInvoque(false)
                .auteurRupture(AuteurRupture.EMPLOYEUR)
                .delaiPrevenanceJoursAppliques(30)
                .motifLieAuxCompetencesProfessionnelles(true)
                .motifEconomiqueOuOrganisationnel(false)
                .lettreRuptureMotivee(true)
                .motifsAveresParPieces(true)
                .conventionCollectiveApplicable(false)
                .salaireMensuelBrut(4500.0);
    }

    private static List<CodeAnomalie> codes(RupturePeriodeEssaiResult r) {
        return r.anomaliesDetectees().stream().map(Anomalie::code).toList();
    }

    // ============================================================================
    // 1 — REGULIERE
    // ============================================================================

    @Test
    void compute_ruptureReguliereCadreEmployeur_aucuneAnomalie() {
        var r = RupturePeriodeEssaiCalculator.compute(ruptureReguliere().build(), "FRANCE");
        assertThat(r.anomaliesDetectees()).isEmpty();
        assertThat(r.scoreIrregularite()).isZero();
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
        assertThat(r.remedeReintegration()).isFalse();
        assertThat(r.indemniteEstimee()).isNull();
        assertThat(r.dureeLegaleMaximaleMois()).isEqualTo(4);
        assertThat(r.delaiPrevenanceLegalJours()).isEqualTo(30);
        assertThat(r.delaiPrevenanceRespecte()).isTrue();
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void compute_ruptureReguliereSalarieMoins8Jours_aucuneAnomalie() {
        // Salarié rompt dans les 7 jours, prévenance 24h (1 jour) — régulier
        var in = ruptureReguliere()
                .auteurRupture(AuteurRupture.SALARIE)
                .dateRupture(LocalDate.of(2025, 1, 5))
                .delaiPrevenanceJoursAppliques(1)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
        assertThat(r.delaiPrevenanceLegalJours()).isEqualTo(1);
    }

    // ============================================================================
    // 2 — ILLEGALE_REQUALIF_LICENCIEMENT (durée essai > légale OU renouvellement irrégulier)
    // ============================================================================

    @Test
    void compute_dureeEssaiCadreDepasse_sansLettreMotivee_ILLEGALE() {
        var in = ruptureReguliere()
                .dureePeriodeEssaiContractuelleMois(5) // cadre = 4 max
                .lettreRuptureMotivee(false)
                .motifsAveresParPieces(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DUREE_ESSAI_DEPASSEE);
        assertThat(r.verdict()).isEqualTo(Verdict.ILLEGALE_REQUALIF_LICENCIEMENT);
    }

    @Test
    void compute_dureeEssaiCadreDepasse_avecLettreMotiveeEtMotifsAveres_RISQUE_ABUSIVE() {
        // Atténuation Marjolaine 19/05 : lettre motivée + motifs avérés → RISQUE_ABUSIVE
        var in = ruptureReguliere()
                .dureePeriodeEssaiContractuelleMois(5)
                .lettreRuptureMotivee(true)
                .motifsAveresParPieces(true)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DUREE_ESSAI_DEPASSEE);
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
    }

    @Test
    void compute_renouvellementSansAccordBranche_ILLEGALE() {
        var in = ruptureReguliere()
                .renouvellementInvoque(true)
                .accordBrancheRenouvellement(false)
                .accordEcritSalarieRenouvellement(true)
                .lettreRuptureMotivee(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.RENOUVELLEMENT_IRREGULIER);
        assertThat(r.verdict()).isEqualTo(Verdict.ILLEGALE_REQUALIF_LICENCIEMENT);
    }

    @Test
    void compute_renouvellementSansAccordSalarie_ILLEGALE() {
        var in = ruptureReguliere()
                .renouvellementInvoque(true)
                .accordBrancheRenouvellement(true)
                .accordEcritSalarieRenouvellement(false)
                .lettreRuptureMotivee(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.RENOUVELLEMENT_IRREGULIER);
        assertThat(r.verdict()).isEqualTo(Verdict.ILLEGALE_REQUALIF_LICENCIEMENT);
    }

    @Test
    void compute_renouvellementRegulier_doubleEssai_REGULIERE() {
        // Cadre, essai contractuel 4 mois, renouvellement régulier → 8 mois effectifs
        // Rupture à 5 mois → toujours dans l'essai effectif
        var in = ruptureReguliere()
                .renouvellementInvoque(true)
                .accordBrancheRenouvellement(true)
                .accordEcritSalarieRenouvellement(true)
                .dateRupture(LocalDate.of(2025, 6, 1)) // 5 mois après début
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
    }

    // ============================================================================
    // 3 — NULLE (discrimination / grossesse / AT-MP / liberté fondamentale)
    // ============================================================================

    @Test
    void compute_discriminationInvoquee_NULLE_avecReintegration() {
        var in = ruptureReguliere()
                .discriminationInvoquee(DiscriminationMotif.SEXE)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DISCRIMINATION_AVEREE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_grossesseAuMomentRupture_NULLE_avecReintegration() {
        var in = ruptureReguliere().grossesseAuMomentRupture(true).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.GROSSESSE_PROTECTION_VIOLEE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_atMpEnCours_NULLE_avecReintegration() {
        var in = ruptureReguliere().arretAccidentTravailEnCours(true).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.AT_MP_PROTECTION_VIOLEE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_atteinteLiberteFondamentale_NULLE_avecReintegration() {
        var in = ruptureReguliere()
                .atteinteLiberteFondamentale("Licenciement consécutif à expression d'une opinion politique")
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.ATTEINTE_LIBERTE_FONDAMENTALE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_nulliteprimeSurIllegalite_grossessePlusDureeDepassee_NULLE() {
        // Cumul : grossesse + durée essai dépassée → NULLE prime (priorité 1)
        var in = ruptureReguliere()
                .grossesseAuMomentRupture(true)
                .dureePeriodeEssaiContractuelleMois(5)
                .lettreRuptureMotivee(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r))
                .contains(CodeAnomalie.GROSSESSE_PROTECTION_VIOLEE)
                .contains(CodeAnomalie.DUREE_ESSAI_DEPASSEE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    // ============================================================================
    // 4 — RISQUE_ABUSIVE
    // ============================================================================

    @Test
    void compute_motifNonProfessionnel_RISQUE_ABUSIVE() {
        var in = ruptureReguliere()
                .motifLieAuxCompetencesProfessionnelles(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.MOTIF_NON_PROFESSIONNEL);
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
    }

    @Test
    void compute_motifEconomique_RISQUE_ABUSIVE() {
        var in = ruptureReguliere().motifEconomiqueOuOrganisationnel(true).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.MOTIF_ETRANGER_A_ESSAI);
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
    }

    @Test
    void compute_delaiPrevenanceInsuffisant_RISQUE_ABUSIVE() {
        // Cadre 99 jours → prévenance légale 30, l'avocat indique seulement 5 jours
        var in = ruptureReguliere().delaiPrevenanceJoursAppliques(5).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DELAI_PREVENANCE_INSUFFISANT);
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
        assertThat(r.delaiPrevenanceRespecte()).isFalse();
    }

    @Test
    void compute_conventionCollectiveNonRespectee_RISQUE_ABUSIVE() {
        var in = ruptureReguliere()
                .conventionCollectiveApplicable(true)
                .conventionCollectivePlusFavorableRespectee(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.CONVENTION_COLLECTIVE_NON_RESPECTEE);
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
    }

    @Test
    void compute_ruptureHorsPeriodeEssai_RISQUE_ABUSIVE() {
        // Cadre, essai 4 mois, rupture 5 mois après début → hors période effective
        var in = ruptureReguliere()
                .dateDebutContrat(LocalDate.of(2025, 1, 1))
                .dateRupture(LocalDate.of(2025, 6, 15)) // 5,5 mois
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI);
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
    }

    // ============================================================================
    // 5 — Cas spéciaux
    // ============================================================================

    @Test
    void compute_periodeEssaiAbsente_REGULIERE_avecMessageHorsScope() {
        var in = ruptureReguliere().dureePeriodeEssaiContractuelleMois(0).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.anomaliesDetectees()).isEmpty();
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
        assertThat(r.messages())
                .anyMatch(m -> m.toLowerCase().contains("outil non applicable"));
    }

    @Test
    void compute_indemniteAbus_fourchette1a6Mois_calculeeCorrectement() {
        var in = ruptureReguliere()
                .motifLieAuxCompetencesProfessionnelles(false)
                .salaireMensuelBrut(4500.0)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
        assertThat(r.indemniteEstimee()).isNotNull();
        assertThat(r.indemniteEstimee().montantMinEuros()).isEqualTo(4500.0);
        assertThat(r.indemniteEstimee().montantMaxEuros()).isEqualTo(27000.0);
    }

    @Test
    void compute_indemniteAbus_sansSalaire_fourchetteNullAvecMessage() {
        var in = ruptureReguliere()
                .motifLieAuxCompetencesProfessionnelles(false)
                .salaireMensuelBrut(null)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.indemniteEstimee()).isNotNull();
        assertThat(r.indemniteEstimee().montantMinEuros()).isNull();
        assertThat(r.indemniteEstimee().baseCalcul()).contains("non renseigné");
    }

    @Test
    void compute_dureeLegaleSelonCategorie_correcte() {
        var ouvrier = ruptureReguliere()
                .categorieSocioProfessionnelle(CategorieSocioProfessionnelle.OUVRIER_EMPLOYE)
                .dureePeriodeEssaiContractuelleMois(2)
                .dateRupture(LocalDate.of(2025, 2, 28))
                .build();
        var r1 = RupturePeriodeEssaiCalculator.compute(ouvrier, "FRANCE");
        assertThat(r1.dureeLegaleMaximaleMois()).isEqualTo(2);

        var am = ruptureReguliere()
                .categorieSocioProfessionnelle(CategorieSocioProfessionnelle.AGENT_MAITRISE_TECHNICIEN)
                .dureePeriodeEssaiContractuelleMois(3)
                .build();
        var r2 = RupturePeriodeEssaiCalculator.compute(am, "FRANCE");
        assertThat(r2.dureeLegaleMaximaleMois()).isEqualTo(3);

        var cadre = ruptureReguliere().build();
        var r3 = RupturePeriodeEssaiCalculator.compute(cadre, "FRANCE");
        assertThat(r3.dureeLegaleMaximaleMois()).isEqualTo(4);
    }

    @Test
    void compute_basesJuridiques_toujoursIncluentL1221_19EtL1221_25() {
        var r = RupturePeriodeEssaiCalculator.compute(ruptureReguliere().build(), "FRANCE");
        assertThat(r.basesJuridiques())
                .contains("Art. L.1221-19 C. trav.")
                .contains("Art. L.1221-25 C. trav.");
    }

    @Test
    void compute_paysBelgique_levegeException() {
        var in = ruptureReguliere().build();
        assertThatThrownBy(() -> RupturePeriodeEssaiCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void compute_inputNull_levegeException() {
        assertThatThrownBy(() -> RupturePeriodeEssaiCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_categorieMissing_levegeException() {
        var in = new RupturePeriodeEssaiInput.Builder()
                .typeContrat(TypeContrat.CDI)
                .dateDebutContrat(LocalDate.of(2025, 1, 1))
                .dateRupture(LocalDate.of(2025, 2, 1))
                .dureePeriodeEssaiContractuelleMois(2)
                .auteurRupture(AuteurRupture.EMPLOYEUR)
                .build();
        assertThatThrownBy(() -> RupturePeriodeEssaiCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Catégorie");
    }

    @Test
    void compute_motifInvoqueTropLong_levegeException() {
        String tropLong = "x".repeat(1001);
        var in = ruptureReguliere().motifInvoque(tropLong).build();
        assertThatThrownBy(() -> RupturePeriodeEssaiCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifInvoque");
    }

    @Test
    void compute_messageNullite_inclutMentionReintegration() {
        var in = ruptureReguliere().grossesseAuMomentRupture(true).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.toUpperCase().contains("RÉINTÉGRATION"));
    }

    @Test
    void compute_messageIllegale_inclutMentionBaremeMacron() {
        var in = ruptureReguliere()
                .dureePeriodeEssaiContractuelleMois(5)
                .lettreRuptureMotivee(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("Barème Macron") || m.contains("L.1235-3"));
    }

    // ============================================================================
    // 6 — Échelle prévenance employeur L.1221-25
    // ============================================================================

    @Test
    void delaiPrevenanceLegalJours_employeur_echelleComplete() {
        assertThat(RupturePeriodeEssaiCalculator.delaiPrevenanceLegalJours(AuteurRupture.EMPLOYEUR, 5))
                .isEqualTo(1);  // < 8 jours = 24h
        assertThat(RupturePeriodeEssaiCalculator.delaiPrevenanceLegalJours(AuteurRupture.EMPLOYEUR, 20))
                .isEqualTo(2);  // < 1 mois = 48h
        assertThat(RupturePeriodeEssaiCalculator.delaiPrevenanceLegalJours(AuteurRupture.EMPLOYEUR, 60))
                .isEqualTo(14); // < 3 mois = 2 sem.
        assertThat(RupturePeriodeEssaiCalculator.delaiPrevenanceLegalJours(AuteurRupture.EMPLOYEUR, 120))
                .isEqualTo(30); // ≥ 3 mois = 1 mois
    }

    @Test
    void delaiPrevenanceLegalJours_salarie_echelleComplete() {
        assertThat(RupturePeriodeEssaiCalculator.delaiPrevenanceLegalJours(AuteurRupture.SALARIE, 5))
                .isEqualTo(1);  // < 8 jours = 24h
        assertThat(RupturePeriodeEssaiCalculator.delaiPrevenanceLegalJours(AuteurRupture.SALARIE, 30))
                .isEqualTo(2);  // ≥ 8 jours = 48h
    }

    @Test
    void dureeLegaleMaximaleMois_CDD_court_2semaines() {
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleMois(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.CDD, 3)).isEqualTo(1);
    }

    @Test
    void dureeLegaleMaximaleMois_CDD_long_1mois() {
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleMois(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.CDD, 12)).isEqualTo(1);
    }
}

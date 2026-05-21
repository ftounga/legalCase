package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.Anomalie;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.AuteurRupture;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CategorieSocioProfessionnelle;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CodeAnomalie;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.DiscriminationMotif;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContrat;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContratPrecedent;
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
    void compute_delaiPrevenanceInsuffisant_anomaliedetectee_verdictReguliere_indemniteCompensatricePrevenanceCalculee() {
        // SF-252b-01 (audit 2026-05-20) — Cass. soc., 23/01/2013, n° 11-23.428 :
        // l'inobservation du délai de prévenance L.1221-25 n'ouvre droit qu'à une
        // indemnité compensatrice de préavis non exécuté — elle ne caractérise pas
        // un abus en soi et ne requalifie pas la rupture.
        // Cadre 99 jours → prévenance légale 30 jours, l'avocat indique 5 jours
        // → 25 jours manquants × salaire/30 = indemnité spécifique.
        var in = ruptureReguliere().delaiPrevenanceJoursAppliques(5).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DELAI_PREVENANCE_INSUFFISANT);
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
        assertThat(r.delaiPrevenanceRespecte()).isFalse();
        // Indemnité prévenance = 4500 € × 25 jours / 30 = 3750 €
        assertThat(r.indemnitePrevenanceEuros()).isNotNull();
        assertThat(r.indemnitePrevenanceEuros()).isEqualTo(3750.0);
        // Pas d'indemnité abus (verdict REGULIERE)
        assertThat(r.indemniteEstimee()).isNull();
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

    // ============================================================================
    // SF-252-01 — 5 protections nullité additionnelles + enum discrimination 23 motifs
    // ============================================================================

    @Test
    void compute_salarieProtegeSansAutorisationInspectionTravail_NULLE() {
        // L.2411-1 — rupture d'un élu CSE / DS / etc. sans autorisation IT = NULLE
        var in = ruptureReguliere()
                .salarieProtege(true)
                .autorisationInspectionTravailObtenue(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.SALARIE_PROTEGE_SANS_AUTORISATION);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_salarieProtegeAvecAutorisationInspectionTravail_pasAnomalie() {
        // Autorisation obtenue → pas d'anomalie protection
        var in = ruptureReguliere()
                .salarieProtege(true)
                .autorisationInspectionTravailObtenue(true)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).doesNotContain(CodeAnomalie.SALARIE_PROTEGE_SANS_AUTORISATION);
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
    }

    @Test
    void compute_lanceurAlerte_NULLE() {
        // L.1132-3-3 — lanceur d'alerte = NULLE de plein droit
        var in = ruptureReguliere().lanceurAlerte(true).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.LANCEUR_ALERTE_PROTECTION_VIOLEE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_temoinOuVictimeHarcelement_NULLE() {
        // L.1132-3-1 / L.1152-2 / L.1153-2 = NULLE
        var in = ruptureReguliere().temoinOuVictimeHarcelement(true).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.TEMOIN_HARCELEMENT_PROTECTION_VIOLEE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_droitDeRetraitExerce_NULLE() {
        // L.4131-3 — rupture sanctionnant l'exercice du droit de retrait = NULLE
        var in = ruptureReguliere().droitDeRetraitExerce(true).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DROIT_RETRAIT_PROTECTION_VIOLEE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_grossesseNotifieePostRupture_dansDelai15j_NULLE() {
        // L.1225-5 — grossesse notifiée dans les 15 jours suivant la rupture = NULLE rétroactive
        // Rupture 10/04, notification 20/04 = 10 jours → dans le délai
        var in = ruptureReguliere()
                .grossesseDeclareePostRupture(true)
                .dateNotificationGrossesse(LocalDate.of(2025, 4, 20))
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.GROSSESSE_NOTIFIEE_POST_RUPTURE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
        assertThat(r.remedeReintegration()).isTrue();
    }

    @Test
    void compute_grossesseNotifieePostRupture_horsDelai15j_pasAnomalie() {
        // Rupture 10/04, notification 30/04 = 20 jours → hors délai → pas de nullité
        var in = ruptureReguliere()
                .grossesseDeclareePostRupture(true)
                .dateNotificationGrossesse(LocalDate.of(2025, 4, 30))
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).doesNotContain(CodeAnomalie.GROSSESSE_NOTIFIEE_POST_RUPTURE);
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
    }

    @Test
    void compute_discriminationOrientationSexuelle_NULLE() {
        // SF-252-01 — enum L.1132-1 étendu : nouveau motif ORIENTATION_SEXUELLE
        var in = ruptureReguliere()
                .discriminationInvoquee(DiscriminationMotif.ORIENTATION_SEXUELLE)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DISCRIMINATION_AVEREE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
    }

    @Test
    void compute_discriminationAge_NULLE() {
        // SF-252-01 — enum L.1132-1 étendu : nouveau motif AGE
        var in = ruptureReguliere()
                .discriminationInvoquee(DiscriminationMotif.AGE)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DISCRIMINATION_AVEREE);
        assertThat(r.verdict()).isEqualTo(Verdict.NULLE);
    }

    // ============================================================================
    // SF-252b-01 — Gaps critiques #1 (CDD L.1242-10), #2 (INTERIM L.1251-14),
    //              #4 (prévenance Cass. soc. 23/01/2013)
    // ============================================================================

    @Test
    void dureeLegaleMaximaleJours_CDD_court_3mois_essai_max_12jours() {
        // CDD de 3 mois : 1 jour/semaine × 12 semaines (3 × 4) = 12 jours
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleJours(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.CDD, 3)).isEqualTo(12);
    }

    @Test
    void dureeLegaleMaximaleJours_CDD_court_5mois_essai_max_14jours_plafond() {
        // CDD de 5 mois : 5 × 4 = 20 sem, mais plafonné à 14 jours (2 semaines absolu)
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleJours(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.CDD, 5)).isEqualTo(14);
    }

    @Test
    void dureeLegaleMaximaleJours_CDD_long_8mois_essai_max_30jours() {
        // CDD > 6 mois : 1 mois maxi = 30 jours
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleJours(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.CDD, 8)).isEqualTo(30);
    }

    @Test
    void dureeLegaleMaximaleJours_INTERIM_mission_sous_1mois_essai_2jours() {
        // Intérim L.1251-14 : mission ≤ 1 mois → 2 jours
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleJours(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.INTERIM, 1)).isEqualTo(2);
    }

    @Test
    void dureeLegaleMaximaleJours_INTERIM_mission_entre_1_et_2mois_essai_3jours() {
        // Intérim L.1251-14 : 1 < mission ≤ 2 mois → 3 jours
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleJours(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.INTERIM, 2)).isEqualTo(3);
    }

    @Test
    void dureeLegaleMaximaleJours_INTERIM_mission_plus_2mois_essai_5jours() {
        // Intérim L.1251-14 : mission > 2 mois → 5 jours
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleJours(
                CategorieSocioProfessionnelle.OUVRIER_EMPLOYE, TypeContrat.INTERIM, 6)).isEqualTo(5);
    }

    @Test
    void dureeLegaleMaximaleJours_CDI_cadre_120jours() {
        // CDI cadre : 4 mois × 30 = 120 jours
        assertThat(RupturePeriodeEssaiCalculator.dureeLegaleMaximaleJours(
                CategorieSocioProfessionnelle.CADRE, TypeContrat.CDI, null)).isEqualTo(120);
    }

    @Test
    void compute_CDD_court_essai_contractuel_30jours_DEPASSE_legal_12jours() {
        // CDD 3 mois (12 jours max légal) avec contractuel de 30 jours → ILLEGALE
        var in = ruptureReguliere()
                .typeContrat(TypeContrat.CDD)
                .dureeCddMois(3)
                .dureePeriodeEssaiContractuelleJours(30)  // 30 > 12 légal
                .dureePeriodeEssaiContractuelleMois(1)
                .lettreRuptureMotivee(false)
                .motifsAveresParPieces(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DUREE_ESSAI_DEPASSEE);
        assertThat(r.verdict()).isEqualTo(Verdict.ILLEGALE_REQUALIF_LICENCIEMENT);
        assertThat(r.dureeLegaleMaximaleJours()).isEqualTo(12);
    }

    @Test
    void compute_INTERIM_mission_1mois_essai_contractuel_5jours_DEPASSE_legal_2jours() {
        // Intérim mission 1 mois (2 jours max légal) avec contractuel 5 jours → ILLEGALE
        var in = ruptureReguliere()
                .typeContrat(TypeContrat.INTERIM)
                .dureeCddMois(1)
                .dureePeriodeEssaiContractuelleJours(5)  // 5 > 2 légal
                .dureePeriodeEssaiContractuelleMois(1)
                .lettreRuptureMotivee(false)
                .motifsAveresParPieces(false)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.DUREE_ESSAI_DEPASSEE);
        assertThat(r.verdict()).isEqualTo(Verdict.ILLEGALE_REQUALIF_LICENCIEMENT);
        assertThat(r.dureeLegaleMaximaleJours()).isEqualTo(2);
    }

    @Test
    void compute_indemnitePrevenance_cumulable_avec_indemnitéAbus() {
        // Cas combiné : motif non professionnel (RISQUE_ABUSIVE → indemnité abus 1-6 mois)
        // ET délai prévenance non respecté → indemnité préavis cumulable
        var in = ruptureReguliere()
                .motifLieAuxCompetencesProfessionnelles(false)  // déclenche RISQUE_ABUSIVE
                .delaiPrevenanceJoursAppliques(5)  // requis 30, manque 25 jours
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.RISQUE_ABUSIVE);
        // Indemnité abus présente (fourchette 1-6 mois × 4500)
        assertThat(r.indemniteEstimee()).isNotNull();
        assertThat(r.indemniteEstimee().montantMinEuros()).isEqualTo(4500.0);
        assertThat(r.indemniteEstimee().montantMaxEuros()).isEqualTo(27000.0);
        // Indemnité prévenance présente en parallèle (25 jours × 4500/30 = 3750)
        assertThat(r.indemnitePrevenanceEuros()).isEqualTo(3750.0);
    }

    @Test
    void compute_prevenanceRespectee_pas_indemnitePrevenance() {
        // Délai respecté → pas d'indemnité prévenance
        var in = ruptureReguliere().delaiPrevenanceJoursAppliques(30).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.delaiPrevenanceRespecte()).isTrue();
        assertThat(r.indemnitePrevenanceEuros()).isNull();
    }

    @Test
    void compute_indemnitePrevenance_salaire_null_returns_null() {
        // Pas de salaire → indemnité non calculable
        var in = ruptureReguliere()
                .salaireMensuelBrut(null)
                .delaiPrevenanceJoursAppliques(5)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.indemnitePrevenanceEuros()).isNull();
    }

    // ============================================================================
    // SF-252c-01 — Gaps moyens #10 #11 #12 #13 #14 (audit 2026-05-20)
    // ============================================================================

    @Test
    void compute_apprentissage_horsScope_verdictReguliereNeutre() {
        // Gap #13 — Apprentissage L.6222-18 = régime spécial, hors scope F-DT-38
        var in = ruptureReguliere().typeContrat(TypeContrat.APPRENTISSAGE).build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
        assertThat(r.anomaliesDetectees()).isEmpty();
        assertThat(r.basesJuridiques()).contains("Art. L.6222-18 C. trav.");
        assertThat(r.messages()).anyMatch(m -> m.contains("Contrat d'apprentissage"));
        assertThat(r.messages()).anyMatch(m -> m.contains("45 premiers jours"));
        assertThat(r.indemniteEstimee()).isNull();
        assertThat(r.indemnitePrevenanceEuros()).isNull();
    }

    @Test
    void compute_suspensionContrat_prolongeFinEssai_pasAnomalie() {
        // Gap #10 — Cass. soc. 31/01/2018 : arrêt maladie suspend essai
        // Cadre, début 01/01, essai 4 mois = fin 01/05. Avec 20 jours d'arrêt
        // maladie, fin reportée au 21/05. Rupture le 15/05 → toujours dans l'essai.
        var in = ruptureReguliere()
                .dateRupture(LocalDate.of(2025, 5, 15))
                .joursSuspensionContrat(20)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).doesNotContain(CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI);
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
    }

    @Test
    void compute_suspensionContrat_pas_assez_detecteRuptureHorsEssai() {
        // Sans suspension, rupture le 15/05 (essai jusqu'au 01/05) → HORS ESSAI
        var in = ruptureReguliere()
                .dateRupture(LocalDate.of(2025, 5, 15))
                .joursSuspensionContrat(5)  // 5 jours < 14 nécessaires
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI);
    }

    @Test
    void compute_repriseAncienneteCddPrecedent_dureeEssaiReduite_RUPTURE_HORS_PERIODE() {
        // Gap #11 — L.1243-11 : CDD précédent de 3 mois → durée d'essai CDI réduite
        // Cadre, début 01/01, contractuel 4 mois, mais 3 mois CDD précédent
        // → essai effectif 1 mois (jusqu'au 01/02). Rupture le 01/03 → HORS ESSAI.
        var in = ruptureReguliere()
                .dateRupture(LocalDate.of(2025, 3, 1))
                .ancienneteContratPrecedentMois(3)
                .typeContratPrecedent(TypeContratPrecedent.CDD)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI);
    }

    @Test
    void compute_repriseAncienneteStageMoins2Mois_pasDeReduction() {
        // Cass. soc. 09/10/2013 : stage < 2 mois → pas de déduction
        var in = ruptureReguliere()
                .ancienneteContratPrecedentMois(1)
                .typeContratPrecedent(TypeContratPrecedent.STAGE)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        // Avec essai 4 mois et ancienneté 1 mois stage (non déductible),
        // rupture le 10/04 reste dans l'essai → REGULIERE
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
    }

    @Test
    void compute_repriseAncienneteStagePlus2Mois_reductionAppliquee() {
        // Cass. soc. 09/10/2013 : stage ≥ 2 mois → déduction
        // Cadre, début 01/01, contractuel 4 mois, mais 3 mois stage précédent
        // → essai effectif 1 mois. Rupture le 01/03 → HORS ESSAI.
        var in = ruptureReguliere()
                .dateRupture(LocalDate.of(2025, 3, 1))
                .ancienneteContratPrecedentMois(3)
                .typeContratPrecedent(TypeContratPrecedent.STAGE)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(codes(r)).contains(CodeAnomalie.RUPTURE_HORS_PERIODE_ESSAI);
    }

    @Test
    void compute_typeContratPrecedentAutre_pasDeReduction() {
        // Type AUTRE → pas de déduction (cas conservateur)
        var in = ruptureReguliere()
                .ancienneteContratPrecedentMois(5)
                .typeContratPrecedent(TypeContratPrecedent.AUTRE)
                .build();
        var r = RupturePeriodeEssaiCalculator.compute(in, "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.REGULIERE);
    }
}

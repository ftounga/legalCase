package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-18 : tests unitaires du {@link Semaine4JoursBeChecker}.
 *
 * <p>Couvre : conditions cumulatives (statut demande, temps plein
 * art. 5 § 1, demande écrite art. 5 § 2, journée ≤ 9h30 art. 5 § 3,
 * avenant + règlement art. 5 § 4 + art. 6, durée 6 mois art. 5 § 4,
 * refus motivé art. 5 § 5, protection licenciement art. 5 § 6),
 * hiérarchie des verdicts, validation des inputs et base juridique
 * stable.</p>
 */
class Semaine4JoursBeCheckerTest {

    private static final LocalDate DATE_DEMANDE = LocalDate.of(2024, 6, 1);
    private static final LocalDate DATE_LIC = LocalDate.of(2024, 7, 15);
    private static final BigDecimal DUREE_38H = new BigDecimal("38.00");
    private static final BigDecimal JOURNEE_9H30 = new BigDecimal("9.50");
    private static final BigDecimal JOURNEE_10H = new BigDecimal("10.00");
    private static final BigDecimal DUREE_6_MOIS = new BigDecimal("6");

    private static Semaine4JoursBeRequest reqConforme() {
        return new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, // temps plein
                true, // demande écrite
                DATE_DEMANDE,
                DUREE_38H,
                JOURNEE_9H30,
                false, // pas besoin de CCT 10h
                true, // avenant signé
                true, // règlement modifié
                DUREE_6_MOIS,
                false, // pas de renouvellement
                false, // refusMotive : N/A car accord
                null, // dateLicenciement N/A
                false); // motifObjectif : N/A
    }

    // ── Cas conforme nominal ────────────────────────────────────────────────

    @Test
    void analyze_accordCompletConforme_returnsConforme() {
        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(reqConforme());

        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.CONFORME_REGIME_4_JOURS_VALIDE);
        assertThat(r.eligibiliteRespectee()).isTrue();
        assertThat(r.demandeRespectee()).isTrue();
        assertThat(r.journeeRespectee()).isTrue();
        assertThat(r.formalisationRespectee()).isTrue();
        assertThat(r.dureeRespectee()).isTrue();
        assertThat(r.raison()).isEqualTo("CONFORME_REGIME_4_JOURS_VALIDE");
        assertThat(r.journeeMaximaleAutoriseeHeures())
                .isEqualByComparingTo("9.50");
        assertThat(r.synthese()).contains("valablement mise en place");
    }

    @Test
    void analyze_avenantRenouveleDuree12mois_conforme() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true,
                new BigDecimal("12"), true, // renouvelé
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.CONFORME_REGIME_4_JOURS_VALIDE);
    }

    @Test
    void analyze_cctAutorise10h_journee10h_conforme() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                new BigDecimal("40.00"), JOURNEE_10H, true, // CCT 10h
                true, true,
                DUREE_6_MOIS, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.CONFORME_REGIME_4_JOURS_VALIDE);
        assertThat(r.journeeMaximaleAutoriseeHeures())
                .isEqualByComparingTo("10.00");
    }

    // ── Hiérarchie 1 : statut indéterminé → A_ANALYSER ──────────────────────

    @Test
    void analyze_statutIndetermine_returnsAanalyser() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.INDETERMINE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(Semaine4JoursBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("STATUT_DEMANDE_INDETERMINE");
        assertThat(r.synthese()).contains("indéterminé");
    }

    // ── Hiérarchie 2 : licenciement après demande ───────────────────────────

    @Test
    void analyze_licenciementPostDemandeSansMotifObjectif_returnsRepresailles() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.LICENCIE_APRES_DEMANDE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, DATE_LIC, false); // motif objectif non établi

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.LICENCIEMENT_REPRESAILLES_PRESUME);
        assertThat(r.raison()).isEqualTo("LICENCIEMENT_REPRESAILLES_PRESUME");
        assertThat(r.synthese()).contains("art. 5 § 6", "6 mois");
    }

    @Test
    void analyze_licenciementPostDemandeAvecMotifObjectif_returnsAanalyser() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.LICENCIE_APRES_DEMANDE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, DATE_LIC, true); // motif objectif établi

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(Semaine4JoursBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("LICENCIEMENT_MOTIF_OBJECTIF_A_VERIFIER");
    }

    // ── Hiérarchie 3 : refus employeur non motivé ───────────────────────────

    @Test
    void analyze_refusSansMotivation_returnsRefusNonMotive() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.REFUSE_SANS_MOTIVATION_ECRITE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, false, BigDecimal.ZERO, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.REFUS_EMPLOYEUR_NON_MOTIVE);
        assertThat(r.raison()).isEqualTo("REFUS_EMPLOYEUR_NON_MOTIVE");
        assertThat(r.synthese()).contains("abus de droit");
    }

    @Test
    void analyze_refuseMotivePretendu_maisMotivationAbsente_returnsRefusNonMotive() {
        // statut REFUSE_MOTIVE_PAR_ECRIT mais refusMotiveParEcrit=false → fragile
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.REFUSE_MOTIVE_PAR_ECRIT,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, false, BigDecimal.ZERO, false,
                false, null, false); // refusMotiveParEcrit = false → contradiction

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.REFUS_EMPLOYEUR_NON_MOTIVE);
    }

    @Test
    void analyze_refusMotiveRegulier_returnsAanalyserOpposable() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.REFUSE_MOTIVE_PAR_ECRIT,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, false, BigDecimal.ZERO, false,
                true, null, false); // refusMotiveParEcrit = true

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(Semaine4JoursBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("REFUS_EMPLOYEUR_MOTIVE_OPPOSABLE");
        assertThat(r.synthese()).contains("opposable");
    }

    // ── Hiérarchie 4 : non éligibilité temps partiel ────────────────────────

    @Test
    void analyze_tempsPartiel_returnsNonEligible() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                false, // temps partiel
                true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_ELIGIBLE_TEMPS_PARTIEL);
        assertThat(r.eligibiliteRespectee()).isFalse();
        assertThat(r.raison()).isEqualTo("TRAVAILLEUR_TEMPS_PARTIEL");
        assertThat(r.synthese()).contains("temps partiel", "art. 5 § 1");
    }

    // ── Hiérarchie 5 : demande écrite manquante ─────────────────────────────

    @Test
    void analyze_pasDemandeEcrite_returnsDemandeManquante() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true,
                false, // pas de demande écrite
                DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_DEMANDE_ECRITE_MANQUANTE);
        assertThat(r.demandeRespectee()).isFalse();
        assertThat(r.raison()).isEqualTo("DEMANDE_ECRITE_MANQUANTE");
        assertThat(r.synthese()).contains("art. 5 § 2");
    }

    // ── Hiérarchie 6 : journée > plafond ────────────────────────────────────

    @Test
    void analyze_journee10hSansCct_returnsJourneeDepasse() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_10H,
                false, // pas de CCT 10h
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_JOURNEE_DEPASSE_9H30);
        assertThat(r.journeeRespectee()).isFalse();
        assertThat(r.raison()).isEqualTo("JOURNEE_DEPASSE_PLAFOND");
    }

    @Test
    void analyze_seuilJournee9h30_estConforme() {
        // Exactement 9h30
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.CONFORME_REGIME_4_JOURS_VALIDE);
    }

    @Test
    void analyze_seuilJournee9h31_depasse() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, new BigDecimal("9.51"), false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_JOURNEE_DEPASSE_9H30);
    }

    // ── Hiérarchie 7 : avenant ou règlement manquant ────────────────────────

    @Test
    void analyze_avenantManquant_returnsFormalisationManquante() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, // avenant manquant
                true, DUREE_6_MOIS, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT);
        assertThat(r.formalisationRespectee()).isFalse();
        assertThat(r.raison()).isEqualTo("AVENANT_OU_REGLEMENT_MANQUANT");
    }

    @Test
    void analyze_reglementManquant_returnsFormalisationManquante() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true,
                false, // règlement manquant
                DUREE_6_MOIS, false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT);
    }

    // ── Hiérarchie 8 : durée > 6 mois sans renouvellement ───────────────────

    @Test
    void analyze_duree12moisSansRenouvellement_returnsDureeDepasse() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true,
                new BigDecimal("12"), false, // pas renouvelé
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_DUREE_DEPASSE_6_MOIS);
        assertThat(r.dureeRespectee()).isFalse();
        assertThat(r.raison()).isEqualTo("DUREE_DEPASSE_6_MOIS");
    }

    @Test
    void analyze_duree7moisAvecRenouvellement_conforme() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true,
                new BigDecimal("7"), true,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.CONFORME_REGIME_4_JOURS_VALIDE);
    }

    // ── Hiérarchie 9 : en attente réponse employeur ─────────────────────────

    @Test
    void analyze_enAttenteReponse_returnsAanalyser() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.EN_ATTENTE_REPONSE_EMPLOYEUR,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, false, BigDecimal.ZERO, false,
                false, null, false);

        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(Semaine4JoursBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("EN_ATTENTE_REPONSE_EMPLOYEUR");
        assertThat(r.synthese()).contains("1 mois");
    }

    // ── Priorisation entre verdicts ─────────────────────────────────────────

    @Test
    void analyze_licenciementPrimeAutresAnomalies() {
        // licenciement + temps partiel + avenant manquant → licenciement gagne
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.LICENCIE_APRES_DEMANDE,
                false, // temps partiel
                false, // pas demande écrite
                DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, false, BigDecimal.ZERO, false,
                false, DATE_LIC, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.LICENCIEMENT_REPRESAILLES_PRESUME);
    }

    @Test
    void analyze_refusPrimeTempsPartiel() {
        // refus non motivé + temps partiel → refus gagne
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.REFUSE_SANS_MOTIVATION_ECRITE,
                false, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, false, BigDecimal.ZERO, false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.REFUS_EMPLOYEUR_NON_MOTIVE);
    }

    @Test
    void analyze_tempsPartielPrimeDemandeManquante() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                false, false, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_ELIGIBLE_TEMPS_PARTIEL);
    }

    @Test
    void analyze_demandePrimeJourneeDepasse() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, false, DATE_DEMANDE,
                DUREE_38H, JOURNEE_10H, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_DEMANDE_ECRITE_MANQUANTE);
    }

    @Test
    void analyze_journeeDepassePrimeAvenantManquant() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_10H, false,
                false, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_JOURNEE_DEPASSE_9H30);
    }

    @Test
    void analyze_avenantManquantPrimeDureeExcessive() {
        Semaine4JoursBeRequest req = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                false, true, new BigDecimal("12"), false,
                false, null, false);

        assertThat(Semaine4JoursBeChecker.analyze(req).verdict())
                .isEqualTo(Semaine4JoursBeVerdict.NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(reqConforme());

        assertThat(r.statutDemande())
                .isEqualTo(Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE);
        assertThat(r.travailleurTempsPlein()).isTrue();
        assertThat(r.demandeEcriteTravailleur()).isTrue();
        assertThat(r.dateDemande()).isEqualTo(DATE_DEMANDE);
        assertThat(r.dureeHebdomadaireHeures())
                .isEqualByComparingTo("38.00");
        assertThat(r.journeeMaximaleHeures())
                .isEqualByComparingTo("9.50");
        assertThat(r.cctAutorise10h()).isFalse();
        assertThat(r.avenantEcritSigne()).isTrue();
        assertThat(r.reglementTravailModifie()).isTrue();
        assertThat(r.dureeAvenantMois()).isEqualByComparingTo("6");
        assertThat(r.avenantRenouvele()).isFalse();
        assertThat(r.refusMotiveParEcrit()).isFalse();
        assertThat(r.dateLicenciement()).isNull();
        assertThat(r.motifLicenciementObjectifEtabli()).isFalse();
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi03102022EtArticles() {
        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(reqConforme());

        assertThat(r.baseJuridique())
                .contains("Loi du 03/10/2022")
                .contains("Deal pour l'emploi")
                .contains("art. 5")
                .contains("art. 6")
                .contains("16/03/1971")
                .contains("03/07/1978")
                .contains("08/04/1965");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        Semaine4JoursBeResult r = Semaine4JoursBeChecker.analyze(reqConforme());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("Deal pour l'emploi")
                .contains("CCT");
    }

    // ── Validation conditionnelle ───────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_statutNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                null, true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutDemande");
    }

    @Test
    void analyze_tempsPleinNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                null, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("travailleurTempsPlein");
    }

    @Test
    void analyze_demandeEcriteNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, null, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demandeEcriteTravailleur");
    }

    @Test
    void analyze_dateDemandeNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, null,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDemande");
    }

    @Test
    void analyze_dureeHebdoZero_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                BigDecimal.ZERO, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeHebdomadaireHeures");
    }

    @Test
    void analyze_journeeZero_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, BigDecimal.ZERO, false,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("journeeMaximaleHeures");
    }

    @Test
    void analyze_cct10hNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, null,
                true, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cctAutorise10h");
    }

    @Test
    void analyze_avenantSigneNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                null, true, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avenantEcritSigne");
    }

    @Test
    void analyze_reglementModifieNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, null, DUREE_6_MOIS, false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reglementTravailModifie");
    }

    @Test
    void analyze_dureeAvenantNegative_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, new BigDecimal("-1"), false,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeAvenantMois");
    }

    @Test
    void analyze_avenantRenouveleNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, null,
                false, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avenantRenouvele");
    }

    @Test
    void analyze_refusMotiveNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                null, null, false);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refusMotiveParEcrit");
    }

    @Test
    void analyze_motifLicenciementNull_throws() {
        Semaine4JoursBeRequest bad = new Semaine4JoursBeRequest(
                Semaine4JoursBeStatutDemande.ACCORDE_AVENANT_SIGNE,
                true, true, DATE_DEMANDE,
                DUREE_38H, JOURNEE_9H30, false,
                true, true, DUREE_6_MOIS, false,
                false, null, null);

        assertThatThrownBy(() -> Semaine4JoursBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifLicenciementObjectifEtabli");
    }
}

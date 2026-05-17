package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.LiquidationPartageBeCalculator.CodeEtape;
import fr.ailegalcase.casefile.LiquidationPartageBeCalculator.DelaiProcedure;
import fr.ailegalcase.casefile.LiquidationPartageBeCalculator.DelaiStatutBe;
import fr.ailegalcase.casefile.LiquidationPartageBeCalculator.EtapeProcedure;
import fr.ailegalcase.casefile.LiquidationPartageBeCalculator.EtapeStatutBe;
import fr.ailegalcase.casefile.LiquidationPartageBeCalculator.LiquidationPartageBeVerdict;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-02 : tests unitaires du moteur de suivi de la procédure de
 * liquidation-partage belge (notaire commis).
 */
class LiquidationPartageBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 17);

    /** Étape 1 seulement franchie : notaire désigné. */
    private LiquidationPartageBeInput.Builder notaireDesigne() {
        return new LiquidationPartageBeInput.Builder()
                .notaireDesigne(true)
                .dateDesignationNotaire(LocalDate.of(2026, 1, 10));
    }

    /**
     * Procédure menée jusqu'à l'établissement et la notification du projet.
     * La désignation du notaire et l'ouverture des opérations sont dérivées
     * en amont de la notification pour rester chronologiquement cohérentes
     * quelle que soit la date notifiée (la notification ne peut précéder ni la
     * désignation ni l'ouverture — cf. validateCoherenceDates).
     */
    private LiquidationPartageBeInput.Builder projetNotifie(LocalDate dateNotification) {
        return new LiquidationPartageBeInput.Builder()
                .notaireDesigne(true)
                .dateDesignationNotaire(dateNotification.minusMonths(2))
                .operationsOuvertes(true)
                .dateOuvertureOperations(dateNotification.minusWeeks(1))
                .inventaireEtabli(true)
                .projetLiquidationEtabli(true)
                .dateNotificationProjet(dateNotification);
    }

    private static EtapeProcedure etape(LiquidationPartageBeResult r, CodeEtape code) {
        return r.etapes().stream().filter(e -> e.code() == code).findFirst().orElseThrow();
    }

    private static DelaiProcedure delaiContredits(LiquidationPartageBeResult r) {
        return r.delais().get(0);
    }

    @Test
    void compute_notaireNonDesigne_procedureNonEngagee() {
        var in = new LiquidationPartageBeInput.Builder().notaireDesigne(false).build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(LiquidationPartageBeVerdict.PROCEDURE_NON_ENGAGEE);
        assertThat(etape(r, CodeEtape.DESIGNATION_NOTAIRE).statut()).isEqualTo(EtapeStatutBe.EN_COURS);
        assertThat(r.etapes()).hasSize(8);
        assertThat(delaiContredits(r).statut()).isEqualTo(DelaiStatutBe.NON_DEMARRE);
    }

    @Test
    void compute_projetNotifieDelaiLarge_enCoursDelaiOk() {
        // projet notifié le 12/05/2026 → échéance 12/06/2026, > 10 jours restants
        var in = projetNotifie(LocalDate.of(2026, 5, 12)).build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(LiquidationPartageBeVerdict.EN_COURS);
        var delai = delaiContredits(r);
        assertThat(delai.statut()).isEqualTo(DelaiStatutBe.OK);
        assertThat(delai.dateEcheance()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(delai.joursRestants()).isEqualTo(26);
        assertThat(etape(r, CodeEtape.NOTIFICATION_PROJET).statut()).isEqualTo(EtapeStatutBe.FAITE);
        assertThat(etape(r, CodeEtape.CONTREDITS).statut()).isEqualTo(EtapeStatutBe.EN_COURS);
    }

    @Test
    void compute_projetNotifieIlYa25Jours_delaiCritique() {
        // notifié le 22/04/2026 → échéance 22/05/2026, soit 5 jours restants au 17/05
        var in = projetNotifie(LocalDate.of(2026, 4, 22)).build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(LiquidationPartageBeVerdict.DELAI_CONTREDITS_CRITIQUE);
        var delai = delaiContredits(r);
        assertThat(delai.statut()).isEqualTo(DelaiStatutBe.CRITIQUE);
        assertThat(delai.dateEcheance()).isEqualTo(LocalDate.of(2026, 5, 22));
        assertThat(delai.joursRestants()).isEqualTo(5);
    }

    @Test
    void compute_projetNotifieIlYa40Jours_sansContredits_delaiDepasse() {
        // notifié le 07/04/2026 → échéance 07/05/2026, dépassée au 17/05
        var in = projetNotifie(LocalDate.of(2026, 4, 7)).contreditsDeposes(false).build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(LiquidationPartageBeVerdict.DELAI_CONTREDITS_CRITIQUE);
        var delai = delaiContredits(r);
        assertThat(delai.statut()).isEqualTo(DelaiStatutBe.DEPASSE);
        assertThat(delai.joursRestants()).isNegative();
    }

    @Test
    void compute_contreditsDeposesApresDelai_pasDeVerdictCritique() {
        // échéance matériellement dépassée mais contredits actés : le délai n'est
        // plus un sujet → statut OK (mini-spec : DEPASSE n'existe que sans contredits)
        var in = projetNotifie(LocalDate.of(2026, 4, 7)).contreditsDeposes(true).build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(delaiContredits(r).statut()).isEqualTo(DelaiStatutBe.OK);
        // contredits déposés → le verdict n'est plus DELAI_CONTREDITS_CRITIQUE
        assertThat(r.verdict()).isEqualTo(LiquidationPartageBeVerdict.EN_COURS);
        assertThat(etape(r, CodeEtape.CONTREDITS).statut()).isEqualTo(EtapeStatutBe.FAITE);
    }

    @Test
    void compute_procesVerbalDiresEtabli_enAttenteHomologation() {
        var in = projetNotifie(LocalDate.of(2026, 1, 15)) // délai largement écoulé
                .contreditsDeposes(true)
                .procesVerbalDiresEtabli(true)
                .build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(LiquidationPartageBeVerdict.EN_ATTENTE_HOMOLOGATION);
        assertThat(etape(r, CodeEtape.PROCES_VERBAL_DIRES).statut()).isEqualTo(EtapeStatutBe.FAITE);
        assertThat(etape(r, CodeEtape.HOMOLOGATION_TF).statut()).isEqualTo(EtapeStatutBe.EN_COURS);
    }

    @Test
    void compute_homologationPrononcee_clotureeToutesEtapesFaites() {
        var in = projetNotifie(LocalDate.of(2026, 1, 15))
                .contreditsDeposes(true)
                .procesVerbalDiresEtabli(true)
                .homologationDemandee(true)
                .dateHomologation(LocalDate.of(2026, 4, 30))
                .build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(LiquidationPartageBeVerdict.CLOTUREE);
        assertThat(r.etapes()).allMatch(e -> e.statut() == EtapeStatutBe.FAITE);
    }

    @Test
    void compute_etapeAvanceeAvecEtapeAnterieureManquante_messageAnomalie() {
        // procès-verbal de dires établi mais ni opérations ni inventaire renseignés
        var in = notaireDesigne()
                .procesVerbalDiresEtabli(true)
                .build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.messages()).anyMatch(m -> m.contains("Anomalie de séquence"));
    }

    @Test
    void compute_delaiUnMois_dateADate_gereLongueursDeMois() {
        // notification le 31/01 → échéance 28/02 (février court) ; today = 28/02 → 0 j
        var in = projetNotifie(LocalDate.of(2026, 1, 31)).build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", LocalDate.of(2026, 2, 28));
        assertThat(delaiContredits(r).dateEcheance()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(delaiContredits(r).joursRestants()).isZero();
        assertThat(delaiContredits(r).statut()).isEqualTo(DelaiStatutBe.CRITIQUE);
    }

    @Test
    void compute_projetEtabliNonNotifie_messageInvitationANotifier() {
        var in = notaireDesigne()
                .operationsOuvertes(true)
                .dateOuvertureOperations(LocalDate.of(2026, 2, 5))
                .inventaireEtabli(true)
                .projetLiquidationEtabli(true)
                .build();
        var r = LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(delaiContredits(r).statut()).isEqualTo(DelaiStatutBe.NON_DEMARRE);
        assertThat(r.messages()).anyMatch(m -> m.contains("n'a pas encore été notifié"));
    }

    // --- Validation ---

    @Test
    void compute_paysNonBelgique_rejete() {
        assertThatThrownBy(() ->
                LiquidationPartageBeCalculator.compute(notaireDesigne().build(), "FRANCE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_rejete() {
        assertThatThrownBy(() ->
                LiquidationPartageBeCalculator.compute(null, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateDesignationManquanteAlorsQueDesigne_rejete() {
        var in = new LiquidationPartageBeInput.Builder().notaireDesigne(true).build();
        assertThatThrownBy(() ->
                LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_notificationAvantDesignation_rejete() {
        var in = notaireDesigne()
                .operationsOuvertes(true)
                .dateOuvertureOperations(LocalDate.of(2026, 1, 20))
                .projetLiquidationEtabli(true)
                .dateNotificationProjet(LocalDate.of(2025, 12, 1)) // avant désignation
                .build();
        assertThatThrownBy(() ->
                LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_commentaireTropLong_rejete() {
        var in = notaireDesigne().commentaire("x".repeat(1001)).build();
        assertThatThrownBy(() ->
                LiquidationPartageBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

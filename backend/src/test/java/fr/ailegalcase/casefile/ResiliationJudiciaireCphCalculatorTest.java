package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static fr.ailegalcase.casefile.ResiliationJudiciaireCphCalculator.CodeManquement;
import static fr.ailegalcase.casefile.ResiliationJudiciaireCphCalculator.DateEffetProbable;
import static fr.ailegalcase.casefile.ResiliationJudiciaireCphCalculator.ManquementRetenu;
import static fr.ailegalcase.casefile.ResiliationJudiciaireCphCalculator.Verdict;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-206-07 — tests unitaires du calculateur de demande de résiliation
 * judiciaire du contrat de travail aux torts de l'employeur (FR — Cass. soc.
 * 16/03/1989 ; Cass. soc. 20/01/1998 ; art. L.1411-1 CT ; art. 1224 /
 * 1227-1228 C.civ.).
 *
 * <p>Couvre les 7 critères d'acceptation de la mini-spec : chaque manquement
 * isolé, cumul, modulateur persistance au jour de la demande, bascule
 * {@link DateEffetProbable} selon le licenciement en cours d'instance, aucun
 * manquement, bornes 0-100 du score, message de rappel « voie moins risquée »,
 * validation des bornes (montant impayé &lt; 0, pays).</p>
 */
class ResiliationJudiciaireCphCalculatorTest {

    /** Input par défaut "rien coché" — aucun manquement, verdict défavorable. */
    private static ResiliationJudiciaireCphInput aucunManquement() {
        return new ResiliationJudiciaireCphInput.Builder()
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
    }

    @Test
    void aucunManquement_returnsDefavorable_dateEffetDecision() {
        var r = ResiliationJudiciaireCphCalculator.compute(aucunManquement(), "FRANCE");
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_DEFAVORABLE);
        assertThat(r.scoreSolidite()).isZero();
        assertThat(r.manquementsRetenus()).isEmpty();
        assertThat(r.dateEffetProbable()).isEqualTo(DateEffetProbable.DATE_DECISION);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void defautPaiementSignificatifEtPersistant_pondereMajeur() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("3000"))
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus()))
                .containsExactly(CodeManquement.DT40_DEFAUT_PAIEMENT_SALAIRE);
        assertThat(r.manquementsRetenus().get(0).poids())
                .isEqualTo(ResiliationJudiciaireCphCalculator.POIDS_MANQUEMENT_MAJEUR);
    }

    @Test
    void defautPaiementRegularise_pondereSignificatifEtScoreDivise() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("500"))
                .manquementsPersistantsAuJourDemande(false)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus()))
                .containsExactly(CodeManquement.DT40_DEFAUT_PAIEMENT_SALAIRE);
        // poids significatif = 15, divisé par 2 (manquement non persistant) → 7
        assertThat(r.scoreSolidite()).isEqualTo(7);
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_DEFAVORABLE);
    }

    @Test
    void harcelement_isole_poidsMajeur() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .harcelement(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus())).containsExactly(CodeManquement.DT40_HARCELEMENT);
        // score 30 → INCERTAINE (>=25)
        assertThat(r.scoreSolidite()).isEqualTo(ResiliationJudiciaireCphCalculator.POIDS_MANQUEMENT_MAJEUR);
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_INCERTAINE);
        // Pas de bascule "nullité" en résiliation judiciaire : effet = DATE_DECISION.
        assertThat(r.dateEffetProbable()).isEqualTo(DateEffetProbable.DATE_DECISION);
    }

    @Test
    void discrimination_isole_poidsMajeur() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .discrimination(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus())).containsExactly(CodeManquement.DT40_DISCRIMINATION);
        assertThat(r.dateEffetProbable()).isEqualTo(DateEffetProbable.DATE_DECISION);
    }

    @Test
    void manquementSecurite_isole_poidsMajeur() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .manquementSecurite(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus())).containsExactly(CodeManquement.DT40_MANQUEMENT_SECURITE);
        assertThat(r.scoreSolidite()).isEqualTo(ResiliationJudiciaireCphCalculator.POIDS_MANQUEMENT_MAJEUR);
    }

    @Test
    void modificationContrat_isole_poidsSignificatif() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .modificationUnilateraleContrat(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus()))
                .containsExactly(CodeManquement.DT40_MODIFICATION_UNILATERALE_CONTRAT);
        assertThat(r.scoreSolidite())
                .isEqualTo(ResiliationJudiciaireCphCalculator.POIDS_MANQUEMENT_SIGNIFICATIF);
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_DEFAVORABLE);
    }

    @Test
    void declassement_isole_poidsSignificatif() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .declassement(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus())).containsExactly(CodeManquement.DT40_DECLASSEMENT);
        assertThat(r.scoreSolidite())
                .isEqualTo(ResiliationJudiciaireCphCalculator.POIDS_MANQUEMENT_SIGNIFICATIF);
    }

    @Test
    void heuresSupNonPayees_isole_poidsSignificatif() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .heuresSupNonPayees(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus())).containsExactly(CodeManquement.DT40_HEURES_SUP_NON_PAYEES);
        assertThat(r.scoreSolidite())
                .isEqualTo(ResiliationJudiciaireCphCalculator.POIDS_MANQUEMENT_SIGNIFICATIF);
    }

    @Test
    void nonRespectDureesRepos_isole_poidsMarginal() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .nonRespectDureesRepos(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(codes(r.manquementsRetenus())).containsExactly(CodeManquement.DT40_NON_RESPECT_DUREES_REPOS);
        assertThat(r.scoreSolidite())
                .isEqualTo(ResiliationJudiciaireCphCalculator.POIDS_MANQUEMENT_MARGINAL);
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_DEFAVORABLE);
    }

    @Test
    void cumulHarcelementEtSecurite_donneFavorable() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .harcelement(true)
                .manquementSecurite(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        // 30 + 30 = 60 → FAVORABLE
        assertThat(r.scoreSolidite()).isEqualTo(60);
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_FAVORABLE);
        assertThat(r.dateEffetProbable()).isEqualTo(DateEffetProbable.DATE_DECISION);
    }

    @Test
    void licenciementEnCoursInstance_basculeDateEffetLicenciement() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .harcelement(true)
                .manquementSecurite(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(false)
                .licenciementIntervenuEnCoursInstance(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        // Bascule date effet (Cass. soc. 21/12/2006).
        assertThat(r.dateEffetProbable()).isEqualTo(DateEffetProbable.DATE_LICENCIEMENT);
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_FAVORABLE);
        // Le message d'information sur le report d'effet est présent.
        assertThat(r.messages())
                .anyMatch(m -> m.contains("date du licenciement")
                        && m.contains("21/12/2006"));
    }

    @Test
    void aucunLicenciementEnCoursInstance_dateEffetDecision() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .harcelement(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .licenciementIntervenuEnCoursInstance(false)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(r.dateEffetProbable()).isEqualTo(DateEffetProbable.DATE_DECISION);
    }

    @Test
    void cumulMassif_scorePlafonneA100() {
        // 4 manquements majeurs (4×30) + 3 significatifs (3×15) + 1 marginal (10)
        // = 175 → plafond 100
        var in = new ResiliationJudiciaireCphInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("5000"))
                .harcelement(true)
                .manquementSecurite(true)
                .modificationUnilateraleContrat(true)
                .declassement(true)
                .discrimination(true)
                .heuresSupNonPayees(true)
                .nonRespectDureesRepos(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(r.scoreSolidite()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(Verdict.RESILIATION_FAVORABLE);
        assertThat(r.manquementsRetenus()).hasSize(8);
    }

    @Test
    void manquementsNonPersistants_diviseScoreParDeux() {
        var inPersistant = new ResiliationJudiciaireCphInput.Builder()
                .modificationUnilateraleContrat(true)
                .heuresSupNonPayees(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var inNonPersistant = new ResiliationJudiciaireCphInput.Builder()
                .modificationUnilateraleContrat(true)
                .heuresSupNonPayees(true)
                .manquementsPersistantsAuJourDemande(false)
                .salarieToujoursEnPoste(true)
                .build();
        var rPersist = ResiliationJudiciaireCphCalculator.compute(inPersistant, "FRANCE");
        var rNonPersist = ResiliationJudiciaireCphCalculator.compute(inNonPersistant, "FRANCE");
        assertThat(rPersist.scoreSolidite()).isEqualTo(30);
        assertThat(rNonPersist.scoreSolidite()).isEqualTo(15);
    }

    @Test
    void messages_contiennent_rappel_voie_moins_risquee() {
        // Critère d'acceptation 5 — rappel structurant présent dans tous les cas.
        var in = new ResiliationJudiciaireCphInput.Builder()
                .harcelement(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("moins risquée") && m.contains("NE ROMPT PAS LE CONTRAT"));
    }

    @Test
    void messages_aucunManquement_contiennent_rappel_voie_moins_risquee() {
        // Le rappel doit être présent y compris en l'absence de manquement.
        var r = ResiliationJudiciaireCphCalculator.compute(aucunManquement(), "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("moins risquée"));
    }

    @Test
    void basesJuridiques_contiennentArretsFondateurs() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .harcelement(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("16/03/1989"))
                .anyMatch(b -> b.contains("20/01/1998"))
                .anyMatch(b -> b.contains("L.1411-1 CT"))
                .anyMatch(b -> b.contains("30/03/2010"));
    }

    @Test
    void salarieNonEnPoste_sansLicenciementEnCours_messageReorientation() {
        // Vigilance pédagogique : contrat déjà rompu pour un autre motif.
        var in = new ResiliationJudiciaireCphInput.Builder()
                .harcelement(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(false)
                .licenciementIntervenuEnCoursInstance(false)
                .build();
        var r = ResiliationJudiciaireCphCalculator.compute(in, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.contains("maintien")
                        && m.contains("réorienter"));
    }

    @Test
    void paysBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ResiliationJudiciaireCphCalculator.compute(aucunManquement(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void paysVide_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ResiliationJudiciaireCphCalculator.compute(aucunManquement(), ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ResiliationJudiciaireCphCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Input");
    }

    @Test
    void montantImpayesNegatif_throwsIllegalArgument() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("-100"))
                .build();
        assertThatThrownBy(() ->
                ResiliationJudiciaireCphCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantImpayesEur");
    }

    @Test
    void commentaireTropLong_throwsIllegalArgument() {
        var in = new ResiliationJudiciaireCphInput.Builder()
                .manquementsCommentaire("x".repeat(2001))
                .build();
        assertThatThrownBy(() ->
                ResiliationJudiciaireCphCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scoreSolidite_resteDansBornes_0_100() {
        var inMax = new ResiliationJudiciaireCphInput.Builder()
                .defautPaiementSalaire(true)
                .montantImpayesEur(new BigDecimal("99999"))
                .harcelement(true)
                .manquementSecurite(true)
                .discrimination(true)
                .modificationUnilateraleContrat(true)
                .manquementsPersistantsAuJourDemande(true)
                .salarieToujoursEnPoste(true)
                .build();
        var rMax = ResiliationJudiciaireCphCalculator.compute(inMax, "FRANCE");
        assertThat(rMax.scoreSolidite()).isBetween(0, 100).isEqualTo(100);

        var rMin = ResiliationJudiciaireCphCalculator.compute(aucunManquement(), "FRANCE");
        assertThat(rMin.scoreSolidite()).isBetween(0, 100).isZero();
    }

    private static List<CodeManquement> codes(List<ManquementRetenu> manquements) {
        return manquements.stream().map(ManquementRetenu::code).toList();
    }
}

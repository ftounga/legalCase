package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.ContestationFiliationBeCalculator.ContestationFiliationBeVerdict;
import fr.ailegalcase.casefile.ContestationFiliationBeCalculator.DelaiStatutBe;
import fr.ailegalcase.casefile.ContestationFiliationBeCalculator.MotifIrrecevabiliteFiliationBeCode;
import fr.ailegalcase.casefile.ContestationFiliationBeCalculator.NatureActionFiliationBe;
import fr.ailegalcase.casefile.ContestationFiliationBeCalculator.QualiteDemandeurFiliationBe;
import fr.ailegalcase.casefile.ContestationFiliationBeCalculator.VoieProceduraleFiliationBe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-18 : tests unitaires du moteur décisionnel BE de la contestation de
 * filiation.
 */
class ContestationFiliationBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 21);

    /**
     * Base : père présumé contestant la paternité, enfant né il y a 3 ans,
     * connaissance du fait il y a 30 jours, pas de possession d'état, ADN
     * disponible.
     */
    private ContestationFiliationBeInput.Builder basePerePresume() {
        return new ContestationFiliationBeInput.Builder()
                .natureActionFiliation(NatureActionFiliationBe.PATERNITE_PRESUMEE_MARI)
                .qualiteDemandeur(QualiteDemandeurFiliationBe.PERE_PRESUME_OU_RECONNAISSANT)
                .dateNaissanceEnfant(LocalDate.of(2023, 1, 15))
                .dateConnaissanceFaitContestation(TODAY.minusDays(30))
                .possessionEtatConforme(false)
                .dureePossessionEtatAnnees(0)
                .expertiseAdnDisponible(true)
                .demandeExpertiseAdnEnvisagee(false);
    }

    // --- Cas nominal ---

    @Test
    void compute_perePresumeConnaissanceRecente_actionRecevable() {
        var in = basePerePresume().build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE);
        assertThat(r.delaiStatut()).isEqualTo(DelaiStatutBe.OK);
        assertThat(r.voieProcedurale()).isEqualTo(VoieProceduraleFiliationBe.REQUETE_TRIBUNAL_FAMILLE);
        assertThat(r.motifsIrrecevabilite()).isEmpty();
        // connaissance 30 j avant TODAY + 1 an = TODAY + 335 j
        assertThat(r.dateLimiteAction()).isEqualTo(TODAY.minusDays(30).plusYears(1));
        assertThat(r.joursRestants()).isEqualTo(335);
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("CC art. 318"));
    }

    // --- Délai ---

    @Test
    void compute_connaissancePlus1AnMoins15j_actionRecevableDelaiCritique() {
        // Connaissance 1 an et 1 jour avant aujourd'hui → date limite il y a 1 jour ? non, 1 an + 1 j → critique
        // Plus simple : connaissance = TODAY - 350j → date limite = TODAY + 15j → critique
        var in = basePerePresume()
                .dateConnaissanceFaitContestation(TODAY.minusDays(350))
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE_DELAI_CRITIQUE);
        assertThat(r.delaiStatut()).isEqualTo(DelaiStatutBe.CRITIQUE);
        assertThat(r.joursRestants()).isLessThanOrEqualTo(30);
        assertThat(r.joursRestants()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void compute_connaissanceIlYa2Ans_irrecevableDelaiDepasse() {
        var in = basePerePresume()
                .dateConnaissanceFaitContestation(TODAY.minusYears(2))
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.IRRECEVABLE_DELAI_DEPASSE);
        assertThat(r.delaiStatut()).isEqualTo(DelaiStatutBe.DEPASSE);
        assertThat(r.joursRestants()).isNegative();
        assertThat(r.voieProcedurale()).isEqualTo(VoieProceduraleFiliationBe.AUCUNE_ACTION_RECEVABLE);
        assertThat(r.motifsIrrecevabilite()).hasSize(1);
        assertThat(r.motifsIrrecevabilite().get(0).code())
                .isEqualTo(MotifIrrecevabiliteFiliationBeCode.DELAI_FORCLUSION);
    }

    // --- Possession d'état ---

    @Test
    void compute_possessionEtatConforme6Ans_irrecevablePossessionEtat() {
        var in = basePerePresume()
                .possessionEtatConforme(true)
                .dureePossessionEtatAnnees(6)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.IRRECEVABLE_POSSESSION_ETAT);
        assertThat(r.dateLimiteAction()).isNull();
        assertThat(r.joursRestants()).isNull();
        assertThat(r.delaiStatut()).isNull();
        assertThat(r.voieProcedurale()).isEqualTo(VoieProceduraleFiliationBe.AUCUNE_ACTION_RECEVABLE);
        assertThat(r.motifsIrrecevabilite()).hasSize(1);
        assertThat(r.motifsIrrecevabilite().get(0).code())
                .isEqualTo(MotifIrrecevabiliteFiliationBeCode.POSSESSION_ETAT_CONFORME_5_ANS);
    }

    @Test
    void compute_possessionEtatConformeExactement5Ans_irrecevablePossessionEtat() {
        // Seuil exact : 5 ans → bloquant.
        var in = basePerePresume()
                .possessionEtatConforme(true)
                .dureePossessionEtatAnnees(5)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.IRRECEVABLE_POSSESSION_ETAT);
    }

    @Test
    void compute_possessionEtatConforme3Ans_pasDeBlocage() {
        var in = basePerePresume()
                .possessionEtatConforme(true)
                .dureePossessionEtatAnnees(3)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        // 3 ans < 5 ans → pas de blocage par possession d'état, action recevable.
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE);
        assertThat(r.motifsIrrecevabilite()).isEmpty();
    }

    @Test
    void compute_possessionEtatNonConforme_pasDeBlocage() {
        var in = basePerePresume()
                .possessionEtatConforme(false)
                .dureePossessionEtatAnnees(10) // durée ignorée car non conforme
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE);
    }

    // --- Possession d'état prime sur délai/qualité ---

    @Test
    void compute_possessionEtatBloquanteEtDelaiDepasse_possessionPrime() {
        // Possession d'état 6 ans + délai dépassé 2 ans → possession prime.
        var in = basePerePresume()
                .possessionEtatConforme(true)
                .dureePossessionEtatAnnees(6)
                .dateConnaissanceFaitContestation(TODAY.minusYears(2))
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.IRRECEVABLE_POSSESSION_ETAT);
    }

    // --- Qualité à agir ---

    @Test
    void compute_paternitePresumee_qualiteEnfantMajeur_recevable() {
        var in = basePerePresume()
                .qualiteDemandeur(QualiteDemandeurFiliationBe.ENFANT_MAJEUR)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE);
    }

    @Test
    void compute_paternitePresumee_qualiteTiers_recevable() {
        var in = basePerePresume()
                .qualiteDemandeur(QualiteDemandeurFiliationBe.TIERS_PRETENDANT_PERE_BIOLOGIQUE)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE);
    }

    @Test
    void compute_paternitePresumee_qualiteMinisterePublic_recevable() {
        var in = basePerePresume()
                .qualiteDemandeur(QualiteDemandeurFiliationBe.MINISTERE_PUBLIC)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE);
    }

    @Test
    void compute_reconnaissanceVolontaire_qualiteMere_recevable() {
        var in = basePerePresume()
                .natureActionFiliation(NatureActionFiliationBe.PATERNITE_RECONNUE_VOLONTAIRE)
                .qualiteDemandeur(QualiteDemandeurFiliationBe.MERE)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.verdict()).isEqualTo(ContestationFiliationBeVerdict.ACTION_RECEVABLE);
    }

    // --- Maternité hors scope V1 ---

    @Test
    void compute_maternite_rejete() {
        var in = basePerePresume()
                .natureActionFiliation(NatureActionFiliationBe.MATERNITE)
                .build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maternité");
    }

    // --- Validation ---

    @Test
    void compute_paysNonBelgique_rejete() {
        var in = basePerePresume().build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "FRANCE", TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void compute_inputNull_rejete() {
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(null, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_natureActionAbsente_rejete() {
        var in = basePerePresume().natureActionFiliation(null).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_qualiteDemandeurAbsente_rejete() {
        var in = basePerePresume().qualiteDemandeur(null).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateNaissanceAbsente_rejete() {
        var in = basePerePresume().dateNaissanceEnfant(null).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateNaissanceFuture_rejete() {
        var in = basePerePresume().dateNaissanceEnfant(TODAY.plusDays(1)).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateConnaissanceAbsente_rejete() {
        var in = basePerePresume().dateConnaissanceFaitContestation(null).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateConnaissanceFuture_rejete() {
        var in = basePerePresume().dateConnaissanceFaitContestation(TODAY.plusDays(1)).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateConnaissanceAvantNaissance_rejete() {
        var in = basePerePresume()
                .dateNaissanceEnfant(LocalDate.of(2023, 1, 15))
                .dateConnaissanceFaitContestation(LocalDate.of(2022, 1, 1))
                .build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("naissance");
    }

    @Test
    void compute_dureePossessionNegative_rejete() {
        var in = basePerePresume().dureePossessionEtatAnnees(-1).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dureePossessionAu101_rejete() {
        var in = basePerePresume().dureePossessionEtatAnnees(101).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_commentaireTropLong_rejete() {
        var in = basePerePresume().commentaire("x".repeat(1001)).build();
        assertThatThrownBy(() -> ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Calcul du délai date à date (années bissextiles) ---

    @Test
    void compute_delaiDateADate_geresAnneesBissextiles() {
        // 29 février → +1 an → 28 février (non bissextile).
        var in = basePerePresume()
                .dateNaissanceEnfant(LocalDate.of(2020, 1, 1))
                .dateConnaissanceFaitContestation(LocalDate.of(2024, 2, 29))
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE",
                LocalDate.of(2024, 3, 1));
        assertThat(r.dateLimiteAction()).isEqualTo(LocalDate.of(2025, 2, 28));
    }

    @Test
    void compute_delaiDateADate_mois31Vs30() {
        // 31 janvier → +1 an → 31 janvier de l'année suivante (date à date stricte).
        var in = basePerePresume()
                .dateNaissanceEnfant(LocalDate.of(2020, 1, 1))
                .dateConnaissanceFaitContestation(LocalDate.of(2025, 1, 31))
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE",
                LocalDate.of(2025, 2, 1));
        assertThat(r.dateLimiteAction()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    // --- Bases juridiques et actions ---

    @Test
    void compute_basesJuridiquesContiennentArticles318() {
        var in = basePerePresume().build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("CC art. 318"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("572bis"));
    }

    @Test
    void compute_actionsContiennentSaisineTribunalFamille() {
        var in = basePerePresume().build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.actionsConcretes()).anyMatch(a -> a.toLowerCase().contains("tribunal de la famille"));
    }

    @Test
    void compute_adnNonDisponibleEtDemandeEnvisagee_messageDemanderExpertise() {
        var in = basePerePresume()
                .expertiseAdnDisponible(false)
                .demandeExpertiseAdnEnvisagee(true)
                .build();
        var r = ContestationFiliationBeCalculator.compute(in, "BELGIQUE", TODAY);
        assertThat(r.actionsConcretes()).anyMatch(a -> a.toLowerCase().contains("expertise adn"));
    }
}

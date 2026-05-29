package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-43 — tests unitaires de {@link AutorisationTravailEmployeurAnalyzer}.
 * Outil FRANCE uniquement (autorisation de travail employeur, L. 5221-1).
 */
class AutorisationTravailEmployeurAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 29);

    private AutorisationTravailEmployeurAnalyzer analyzer() {
        return new AutorisationTravailEmployeurAnalyzer(TODAY);
    }

    private AutorisationTravailEmployeurRequest req(
            AutorisationTravailEmployeurTypeContrat type, String poste, String nationalite,
            Integer duree, boolean refus, LocalDate dateRefus) {
        return new AutorisationTravailEmployeurRequest(type, poste, nationalite, duree, refus, dateRefus);
    }

    @Test
    void nationaliteUe_donneAutorisationNonRequise_sansObligations() {
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDI,
                "Développeur", "Italienne", null, false, null));

        assertThat(r.statut()).isEqualTo(AutorisationTravailEmployeurStatut.AUTORISATION_NON_REQUISE);
        assertThat(r.autorisationRequise()).isFalse();
        assertThat(r.obligationsDemande()).isEmpty();
        assertThat(r.delaiInstructionOFIIMois()).isNull();
        assertThat(r.taxeOFII()).isNull();
    }

    @Test
    void nationaliteSuisse_donneAutorisationNonRequise() {
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDD,
                "Cuisinier", "Suisse", 12, false, null));

        assertThat(r.statut()).isEqualTo(AutorisationTravailEmployeurStatut.AUTORISATION_NON_REQUISE);
    }

    @Test
    void cdiHorsUe_donneAutorisationRequise_avecObligationsEtDelaiInstruction() {
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDI,
                "Ingénieur", "Marocaine", null, false, null));

        assertThat(r.statut()).isEqualTo(AutorisationTravailEmployeurStatut.AUTORISATION_REQUISE);
        assertThat(r.autorisationRequise()).isTrue();
        assertThat(r.obligationsDemande()).hasSize(4);
        assertThat(r.obligationsDemande())
                .anyMatch(o -> o.contains("CERFA 15187*03"))
                .anyMatch(o -> o.toLowerCase().contains("offre"));
        assertThat(r.delaiInstructionOFIIMois()).isEqualTo(2);
    }

    @Test
    void delaiInstructionOFII_estDeDeuxMois() {
        assertThat(AutorisationTravailEmployeurAnalyzer.DELAI_INSTRUCTION_OFII_MOIS).isEqualTo(2);
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.INTERIM,
                "Magasinier", "Sénégalaise", 6, false, null));
        assertThat(r.delaiInstructionOFIIMois()).isEqualTo(2);
    }

    @Test
    void taxeOFII_estPresentQuandAutorisationRequise() {
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDI,
                "Analyste", "Indienne", null, false, null));

        assertThat(r.taxeOFII()).isNotNull().contains("taxe OFII");
    }

    @Test
    void refusDansDelai_donneRecoursPossible_avecDelaiTaDeuxMois() {
        LocalDate dateRefus = TODAY.minusDays(10);
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDI,
                "Technicien", "Tunisienne", null, true, dateRefus));

        assertThat(r.statut()).isEqualTo(AutorisationTravailEmployeurStatut.RECOURS_POSSIBLE);
        assertThat(r.recoursPossible()).isTrue();
        assertThat(r.delaiRecoursTa()).isEqualTo(dateRefus.plusMonths(2));
    }

    @Test
    void refusDateLimite_jourExactDeEcheance_resteRecoursPossible() {
        LocalDate dateRefus = TODAY.minusMonths(2); // échéance == today
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDI,
                "Soudeur", "Algérienne", null, true, dateRefus));

        assertThat(r.statut()).isEqualTo(AutorisationTravailEmployeurStatut.RECOURS_POSSIBLE);
    }

    @Test
    void refusDelaiDepasse_donneRecoursPrescrit() {
        LocalDate dateRefus = TODAY.minusMonths(3);
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDD,
                "Serveur", "Camerounaise", 9, true, dateRefus));

        assertThat(r.statut()).isEqualTo(AutorisationTravailEmployeurStatut.RECOURS_PRESCRIT);
        assertThat(r.recoursPossible()).isFalse();
        assertThat(r.delaiRecoursTa()).isEqualTo(dateRefus.plusMonths(2));
    }

    @Test
    void refusSansDate_donneRecoursPossible_parPrudence() {
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDI,
                "Comptable", "Brésilienne", null, true, null));

        assertThat(r.statut()).isEqualTo(AutorisationTravailEmployeurStatut.RECOURS_POSSIBLE);
        assertThat(r.delaiRecoursTa()).isNull();
    }

    @Test
    void posteTropLong_leveIllegalArgument() {
        String poste = "a".repeat(201);
        assertThatThrownBy(() -> analyzer().analyze(req(
                AutorisationTravailEmployeurTypeContrat.CDI, poste, "Marocaine", null, false, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void typeContratNull_leveIllegalArgument() {
        assertThatThrownBy(() -> analyzer().analyze(req(
                null, "Poste", "Marocaine", null, false, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nationaliteVide_leveIllegalArgument() {
        assertThatThrownBy(() -> analyzer().analyze(req(
                AutorisationTravailEmployeurTypeContrat.CDI, "Poste", "  ", null, false, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void baseJuridique_referenceCodeTravailEtCeseda() {
        var r = analyzer().analyze(req(AutorisationTravailEmployeurTypeContrat.CDI,
                "Poste", "Marocaine", null, false, null));
        assertThat(r.baseJuridique()).contains("L. 5221-1").contains("R. 5221-20");
    }
}

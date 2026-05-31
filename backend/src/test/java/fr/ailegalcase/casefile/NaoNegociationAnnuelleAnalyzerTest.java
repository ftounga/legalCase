package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-29 : tests unitaires de {@link NaoNegociationAnnuelleAnalyzer}
 * (F-DT-66, outil FRANCE uniquement).
 *
 * <p>Logique déterministe (art. L.2242-1 à L.2242-8, L.2242-11, L.2242-15,
 * L.2242-17 CT) :
 * <ul>
 *   <li>pas de DS → NON_APPLICABLE ;</li>
 *   <li>DS + 2 blocs négociés + périodicité 12 + échéance non dépassée →
 *       CONFORME + risque FAIBLE ;</li>
 *   <li>DS + bloc non engagé → NON_CONFORME + risque ELEVE ;</li>
 *   <li>échéance : A_JOUR &gt; 60 j, ECHEANCE_PROCHE 0–60 j, DEPASSEE &lt; 0 ;</li>
 *   <li>périodicité 24 mois sans accord → item non conforme ; avec accord →
 *       conforme ; &gt; 48 mois → IllegalArgument ;</li>
 *   <li>négociation non aboutie sans PV de désaccord → item PV non conforme.</li>
 * </ul>
 */
class NaoNegociationAnnuelleAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 1);

    @Test
    void pasDeDelegueSyndical_nonApplicable() {
        NaoNegociationAnnuelleResult r = NaoNegociationAnnuelleAnalyzer.analyze(
                80, false, false, false, false,
                null, 12, false, false, TODAY);

        assertThat(r.statut()).isEqualTo(NaoNegociationAnnuelleStatut.NON_APPLICABLE);
        assertThat(r.applicable()).isFalse();
        assertThat(r.risqueEntrave()).isEqualTo(NaoRisqueEntrave.FAIBLE);
        assertThat(r.checklist()).isEmpty();
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("L.2242-1"));
    }

    @Test
    void dsPresent_deuxBlocsNegocies_periodicite12_echeanceNonDepassee_conforme() {
        // Dernière négociation il y a 2 mois → prochaine échéance dans 10 mois → A_JOUR.
        NaoNegociationAnnuelleResult r = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                TODAY.minusMonths(2), 12, false, true, TODAY);

        assertThat(r.statut()).isEqualTo(NaoNegociationAnnuelleStatut.CONFORME);
        assertThat(r.risqueEntrave()).isEqualTo(NaoRisqueEntrave.FAIBLE);
        assertThat(r.itemsObligatoiresManquants()).isZero();
        assertThat(r.statutEcheance()).isEqualTo(NaoStatutEcheance.A_JOUR);
        assertThat(r.dateProchaineEcheance()).isEqualTo(TODAY.minusMonths(2).plusMonths(12));
        assertThat(r.baseJuridique()).contains("L.2242-1").contains("L.2242-15").contains("L.2242-17");
    }

    @Test
    void dsPresent_blocsNonNegocies_nonConforme_risqueEleve() {
        NaoNegociationAnnuelleResult r = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, false, false, false,
                TODAY.minusMonths(1), 12, false, false, TODAY);

        assertThat(r.statut()).isEqualTo(NaoNegociationAnnuelleStatut.NON_CONFORME);
        assertThat(r.risqueEntrave()).isEqualTo(NaoRisqueEntrave.ELEVE);
        // 2 blocs + PV (non aboutie) manquants.
        assertThat(r.itemsObligatoiresManquants()).isGreaterThanOrEqualTo(2);
        assertThat(r.consequences()).anySatisfy(c -> assertThat(c).contains("L.2243-2"));
    }

    @Test
    void echeance_aJour_proche_depassee_selonDate() {
        // -2 mois, périodicité 12 → échéance dans 10 mois → A_JOUR.
        assertThat(NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                TODAY.minusMonths(2), 12, false, true, TODAY)
                .statutEcheance()).isEqualTo(NaoStatutEcheance.A_JOUR);

        // -11 mois, périodicité 12 → échéance dans ~1 mois → ECHEANCE_PROCHE.
        assertThat(NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                TODAY.minusMonths(11), 12, false, true, TODAY)
                .statutEcheance()).isEqualTo(NaoStatutEcheance.ECHEANCE_PROCHE);

        // -13 mois, périodicité 12 → échéance il y a 1 mois → DEPASSEE → NON_CONFORME.
        NaoNegociationAnnuelleResult depassee = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                TODAY.minusMonths(13), 12, false, true, TODAY);
        assertThat(depassee.statutEcheance()).isEqualTo(NaoStatutEcheance.DEPASSEE);
        assertThat(depassee.statut()).isEqualTo(NaoNegociationAnnuelleStatut.NON_CONFORME);
        assertThat(depassee.risqueEntrave()).isEqualTo(NaoRisqueEntrave.MODERE);
    }

    @Test
    void periodicite24_sansAccord_itemNonConforme_avecAccord_conforme() {
        NaoNegociationAnnuelleResult sansAccord = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                TODAY.minusMonths(2), 24, false, true, TODAY);
        assertThat(sansAccord.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("Périodicité");
                    assertThat(i.obligatoire()).isTrue();
                    assertThat(i.conforme()).isFalse();
                });
        assertThat(sansAccord.statut()).isEqualTo(NaoNegociationAnnuelleStatut.NON_CONFORME);

        NaoNegociationAnnuelleResult avecAccord = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, true,
                TODAY.minusMonths(2), 24, false, true, TODAY);
        assertThat(avecAccord.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("Périodicité");
                    assertThat(i.conforme()).isTrue();
                });
        assertThat(avecAccord.statut()).isEqualTo(NaoNegociationAnnuelleStatut.CONFORME);
    }

    @Test
    void negociationNonAboutie_sansPvDesaccord_itemPvNonConforme() {
        NaoNegociationAnnuelleResult r = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                TODAY.minusMonths(2), 12, false, false, TODAY);

        assertThat(r.checklist())
                .anySatisfy(i -> {
                    assertThat(i.item()).contains("PV de désaccord");
                    assertThat(i.obligatoire()).isTrue();
                    assertThat(i.conforme()).isFalse();
                });
        assertThat(r.statut()).isEqualTo(NaoNegociationAnnuelleStatut.NON_CONFORME);

        // PV établi → item conforme → CONFORME.
        NaoNegociationAnnuelleResult avecPv = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                TODAY.minusMonths(2), 12, true, false, TODAY);
        assertThat(avecPv.statut()).isEqualTo(NaoNegociationAnnuelleStatut.CONFORME);
    }

    @Test
    void sansDateDerniereNegociation_echeanceNull_conformeSiBlocsOk() {
        NaoNegociationAnnuelleResult r = NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false,
                null, 12, false, true, TODAY);
        assertThat(r.dateProchaineEcheance()).isNull();
        assertThat(r.joursAvantEcheance()).isNull();
        assertThat(r.statutEcheance()).isNull();
        assertThat(r.statut()).isEqualTo(NaoNegociationAnnuelleStatut.CONFORME);
    }

    @Test
    void validations_leventIllegalArgument() {
        // effectif null
        assertThatThrownBy(() -> NaoNegociationAnnuelleAnalyzer.analyze(
                null, true, true, true, false, null, 12, false, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        // effectif 0
        assertThatThrownBy(() -> NaoNegociationAnnuelleAnalyzer.analyze(
                0, true, true, true, false, null, 12, false, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        // delegueSyndicalPresent null
        assertThatThrownBy(() -> NaoNegociationAnnuelleAnalyzer.analyze(
                120, null, true, true, false, null, 12, false, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        // blocRemunerationNegocie null
        assertThatThrownBy(() -> NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, null, true, false, null, 12, false, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        // blocEgaliteQvtNegocie null
        assertThatThrownBy(() -> NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, null, false, null, 12, false, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        // periodiciteMois 0
        assertThatThrownBy(() -> NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, false, null, 0, false, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
        // periodiciteMois 60 (> 48)
        assertThatThrownBy(() -> NaoNegociationAnnuelleAnalyzer.analyze(
                120, true, true, true, true, null, 60, false, true, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.RegimeCommunauteLegaleBeInput.BienItem;
import fr.ailegalcase.casefile.RegimeCommunauteLegaleBeInput.DetteItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-01 : tests unitaires du moteur de qualification du régime de communauté
 * légale belge.
 */
class RegimeCommunauteLegaleBeCalculatorTest {

    private static final LocalDate MARIAGE = LocalDate.of(2015, 6, 20);

    /** Bien acquêt simple (aucun critère de propre coché). */
    private BienItem acquet(String libelle, BienCategorieBe cat) {
        return new BienItem(libelle, cat, false, false, false, false, false, null);
    }

    private RegimeCommunauteLegaleBeInput input(List<BienItem> biens, List<DetteItem> dettes) {
        return new RegimeCommunauteLegaleBeInput(MARIAGE, false, biens, dettes);
    }

    private static RegimeCommunauteLegaleBeResult compute(RegimeCommunauteLegaleBeInput in) {
        return RegimeCommunauteLegaleBeCalculator.compute(in, "BELGIQUE");
    }

    // --- Cas nominal : tout acquêt ---

    @Test
    void compute_coupleSansContrat_biensAcquets_communauteApplicable() {
        var r = compute(input(
                List.of(acquet("Compte épargne", BienCategorieBe.FINANCIER),
                        acquet("Voiture", BienCategorieBe.MOBILIER)),
                List.of()));
        assertThat(r.verdict())
                .isEqualTo(RegimeCommunauteLegaleBeVerdict.COMMUNAUTE_LEGALE_APPLICABLE);
        assertThat(r.biensQualifies()).extracting(b -> b.qualification())
                .containsExactly(QualificationBienBe.COMMUN, QualificationBienBe.COMMUN);
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.syntheseComposition().nbBiensCommuns()).isEqualTo(2);
    }

    // --- Qualification d'un bien ---

    @Test
    void compute_bienAnterieurAuMariage_propre_gestionExclusive() {
        var b = new BienItem("Maison de famille", BienCategorieBe.IMMOBILIER,
                true, false, true, false, false, null);
        var r = compute(input(List.of(b), List.of()));
        assertThat(r.biensQualifies()).singleElement()
                .satisfies(q -> {
                    assertThat(q.qualification()).isEqualTo(QualificationBienBe.PROPRE);
                    assertThat(q.modeGestion()).isEqualTo(ModeGestionBe.GESTION_EXCLUSIVE);
                });
    }

    @Test
    void compute_bienRecuParDonationOuSuccession_propre() {
        var b = new BienItem("Terrain hérité", BienCategorieBe.IMMOBILIER,
                false, true, true, false, false, null);
        var r = compute(input(List.of(b), List.of()));
        assertThat(r.biensQualifies()).singleElement()
                .extracting(q -> q.qualification()).isEqualTo(QualificationBienBe.PROPRE);
    }

    @Test
    void compute_outilDeTravailPersonnel_propre() {
        var b = new BienItem("Cabinet d'avocat — matériel", BienCategorieBe.PROFESSIONNEL,
                false, false, true, false, true, null);
        var r = compute(input(List.of(b), List.of()));
        assertThat(r.biensQualifies()).singleElement()
                .satisfies(q -> {
                    assertThat(q.qualification()).isEqualTo(QualificationBienBe.PROPRE);
                    assertThat(q.modeGestion()).isEqualTo(ModeGestionBe.GESTION_EXCLUSIVE);
                });
    }

    @Test
    void compute_bienImmobilierCommun_cogestion() {
        var r = compute(input(
                List.of(acquet("Appartement Schaerbeek", BienCategorieBe.IMMOBILIER)),
                List.of()));
        assertThat(r.biensQualifies()).singleElement()
                .satisfies(q -> {
                    assertThat(q.qualification()).isEqualTo(QualificationBienBe.COMMUN);
                    assertThat(q.modeGestion()).isEqualTo(ModeGestionBe.COGESTION);
                });
    }

    @Test
    void compute_bienCommunAffecteProfession_gestionExclusive() {
        var b = new BienItem("Fonds de commerce", BienCategorieBe.PROFESSIONNEL,
                false, false, false, true, false, null);
        var r = compute(input(List.of(b), List.of()));
        assertThat(r.biensQualifies()).singleElement()
                .satisfies(q -> {
                    assertThat(q.qualification()).isEqualTo(QualificationBienBe.COMMUN);
                    assertThat(q.modeGestion()).isEqualTo(ModeGestionBe.GESTION_EXCLUSIVE);
                });
    }

    @Test
    void compute_bienPropreFinanceParFondsCommuns_mixteRecompense() {
        // bien propre par origine (antérieur) mais financeParFondsPropres = false
        var b = new BienItem("Studio antérieur rénové", BienCategorieBe.IMMOBILIER,
                true, false, false, false, false, null);
        var r = compute(input(List.of(b), List.of()));
        assertThat(r.biensQualifies()).singleElement()
                .satisfies(q -> {
                    assertThat(q.qualification()).isEqualTo(QualificationBienBe.MIXTE_RECOMPENSE);
                    assertThat(q.modeGestion()).isEqualTo(ModeGestionBe.GESTION_EXCLUSIVE);
                });
        assertThat(r.syntheseComposition().nbBiensMixtesAvecRecompense()).isEqualTo(1);
    }

    @Test
    void compute_criteresContradictoires_indetermine_qualificationIncomplete() {
        // antérieur au mariage ET reçu par donation/succession : origines exclusives
        var b = new BienItem("Bien à l'origine ambiguë", BienCategorieBe.AUTRE,
                true, true, true, false, false, null);
        var r = compute(input(List.of(b), List.of()));
        assertThat(r.biensQualifies()).singleElement()
                .extracting(q -> q.qualification()).isEqualTo(QualificationBienBe.INDETERMINE);
        assertThat(r.verdict())
                .isEqualTo(RegimeCommunauteLegaleBeVerdict.QUALIFICATION_INCOMPLETE);
    }

    // --- Verdict ---

    @Test
    void compute_contratDeMariageSigne_regimeConventionnelDetecte() {
        var in = new RegimeCommunauteLegaleBeInput(MARIAGE, true,
                List.of(acquet("Compte joint", BienCategorieBe.FINANCIER)), List.of());
        var r = RegimeCommunauteLegaleBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict())
                .isEqualTo(RegimeCommunauteLegaleBeVerdict.REGIME_CONVENTIONNEL_DETECTE);
        // la qualification reste fournie même avec contrat
        assertThat(r.biensQualifies()).hasSize(1);
    }

    @Test
    void compute_contratSigne_primeSurBienIndetermine() {
        // un contrat signé bascule le verdict en REGIME_CONVENTIONNEL_DETECTE
        // même si un bien est INDETERMINE
        var b = new BienItem("Bien ambigu", BienCategorieBe.AUTRE,
                true, true, false, false, false, null);
        var in = new RegimeCommunauteLegaleBeInput(MARIAGE, true, List.of(b), List.of());
        var r = RegimeCommunauteLegaleBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict())
                .isEqualTo(RegimeCommunauteLegaleBeVerdict.REGIME_CONVENTIONNEL_DETECTE);
    }

    // --- Qualification d'une dette ---

    @Test
    void compute_detteInteretDuMenage_detteCommune() {
        var d = new DetteItem("Emprunt hypothécaire", false, true, false, null);
        var r = compute(input(List.of(acquet("Maison", BienCategorieBe.IMMOBILIER)), List.of(d)));
        assertThat(r.dettesQualifiees()).singleElement()
                .extracting(q -> q.qualification()).isEqualTo(QualificationDetteBe.DETTE_COMMUNE);
    }

    @Test
    void compute_detteAnterieureAuMariage_dettePropre() {
        var d = new DetteItem("Crédit conso pré-mariage", true, false, true, null);
        var r = compute(input(List.of(acquet("Voiture", BienCategorieBe.MOBILIER)), List.of(d)));
        assertThat(r.dettesQualifiees()).singleElement()
                .extracting(q -> q.qualification()).isEqualTo(QualificationDetteBe.DETTE_PROPRE);
    }

    @Test
    void compute_detteUnSeulEpouxHorsMenage_dettePropreAvecRecours() {
        var d = new DetteItem("Dette de jeu", false, false, true, null);
        var r = compute(input(List.of(acquet("Voiture", BienCategorieBe.MOBILIER)), List.of(d)));
        assertThat(r.dettesQualifiees()).singleElement()
                .extracting(q -> q.qualification())
                .isEqualTo(QualificationDetteBe.DETTE_PROPRE_AVEC_RECOURS);
    }

    @Test
    void compute_detteSansCritereDiscriminant_detteCommune() {
        var d = new DetteItem("Dette indéterminée", false, false, false, null);
        var r = compute(input(List.of(acquet("Voiture", BienCategorieBe.MOBILIER)), List.of(d)));
        assertThat(r.dettesQualifiees()).singleElement()
                .extracting(q -> q.qualification()).isEqualTo(QualificationDetteBe.DETTE_COMMUNE);
    }

    // --- Synthèse ---

    @Test
    void compute_syntheseComposition_comptesExacts() {
        var biens = List.of(
                acquet("Voiture", BienCategorieBe.MOBILIER),                      // COMMUN
                acquet("Appartement", BienCategorieBe.IMMOBILIER),                // COMMUN (cogestion)
                new BienItem("Héritage", BienCategorieBe.AUTRE,
                        false, true, true, false, false, null),                  // PROPRE
                new BienItem("Studio propre rénové", BienCategorieBe.IMMOBILIER,
                        true, false, false, false, false, null));                // MIXTE_RECOMPENSE
        var dettes = List.of(
                new DetteItem("Hypothèque", false, true, false, null),           // DETTE_COMMUNE
                new DetteItem("Crédit pré-mariage", true, false, true, null),    // DETTE_PROPRE
                new DetteItem("Dette de jeu", false, false, true, null));        // DETTE_PROPRE_AVEC_RECOURS
        var r = compute(input(biens, dettes));
        var s = r.syntheseComposition();
        assertThat(s.nbBiensCommuns()).isEqualTo(2);
        assertThat(s.nbBiensPropres()).isEqualTo(1);
        assertThat(s.nbBiensMixtesAvecRecompense()).isEqualTo(1);
        assertThat(s.nbDettesCommunes()).isEqualTo(1);
        assertThat(s.nbDettesPropres()).isEqualTo(2);
        assertThat(r.basesJuridiques()).isNotEmpty();
    }

    // --- Validation / pays ---

    @Test
    void compute_paysNonBelgique_rejete() {
        assertThatThrownBy(() -> RegimeCommunauteLegaleBeCalculator.compute(
                input(List.of(acquet("Voiture", BienCategorieBe.MOBILIER)), List.of()), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_rejete() {
        assertThatThrownBy(() -> RegimeCommunauteLegaleBeCalculator.compute(null, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_listeBiensVide_rejete() {
        assertThatThrownBy(() -> compute(input(List.of(), List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateMariageFuture_rejete() {
        var in = new RegimeCommunauteLegaleBeInput(LocalDate.now().plusDays(1), false,
                List.of(acquet("Voiture", BienCategorieBe.MOBILIER)), List.of());
        assertThatThrownBy(() -> RegimeCommunauteLegaleBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateMariageNull_rejete() {
        var in = new RegimeCommunauteLegaleBeInput(null, false,
                List.of(acquet("Voiture", BienCategorieBe.MOBILIER)), List.of());
        assertThatThrownBy(() -> RegimeCommunauteLegaleBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_libelleBienTropLong_rejete() {
        var b = new BienItem("x".repeat(201), BienCategorieBe.MOBILIER,
                false, false, false, false, false, null);
        assertThatThrownBy(() -> compute(input(List.of(b), List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_commentaireBienTropLong_rejete() {
        var b = new BienItem("Voiture", BienCategorieBe.MOBILIER,
                false, false, false, false, false, "x".repeat(1001));
        assertThatThrownBy(() -> compute(input(List.of(b), List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

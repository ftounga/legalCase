package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-19 : tests unitaires du {@link DroitDeconnexionBeChecker}.
 *
 * <p>Couvre : seuil 20 travailleurs (art. 16 § 1 Loi 03/10/2022),
 * statut accord (CCT déposée / règlement modifié / négociation /
 * aucune initiative / indéterminé), contenu cumulatif (modalités
 * pratiques § 2 1°, sensibilisation § 2 2°, organisation § 2 3°),
 * consultation organe concertation, hiérarchie des verdicts,
 * validation des inputs et base juridique stable.</p>
 */
class DroitDeconnexionBeCheckerTest {

    private static final LocalDate DATE_VIGUEUR = LocalDate.of(2024, 3, 1);

    private static DroitDeconnexionBeRequest reqConforme() {
        return new DroitDeconnexionBeRequest(
                50, // effectif ≥ 20
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, // modalités pratiques
                true, // sensibilisation
                true, // organisation
                true, // consultation
                false); // pas de manquement signalé
    }

    // ── Cas conforme nominal ────────────────────────────────────────────────

    @Test
    void analyze_cctComplete_returnsConforme() {
        DroitDeconnexionBeResult r =
                DroitDeconnexionBeChecker.analyze(reqConforme());

        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.CONFORME_ACCORD_COMPLET);
        assertThat(r.seuilAtteint()).isTrue();
        assertThat(r.instrumentFormalise()).isTrue();
        assertThat(r.contenuComplet()).isTrue();
        assertThat(r.modalitesPratiquesRespectees()).isTrue();
        assertThat(r.sensibilisationRespectee()).isTrue();
        assertThat(r.modalitesOrganisationRespectees()).isTrue();
        assertThat(r.consultationRespectee()).isTrue();
        assertThat(r.raison()).isEqualTo("CONFORME_ACCORD_COMPLET");
        assertThat(r.synthese()).contains("CCT d'entreprise");
    }

    @Test
    void analyze_reglementTravailComplet_returnsConforme() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                30,
                DroitDeconnexionBeStatutAccord.REGLEMENT_TRAVAIL_MODIFIE,
                DATE_VIGUEUR,
                true, true, true, true, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.CONFORME_ACCORD_COMPLET);
        assertThat(r.synthese()).contains("règlement de travail");
    }

    @Test
    void analyze_conformeMaisConsultationAbsente_warningDansSynthese() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, true,
                false, // consultation absente
                false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.CONFORME_ACCORD_COMPLET);
        assertThat(r.consultationRespectee()).isFalse();
        assertThat(r.synthese()).contains("fragilité procédurale");
    }

    // ── Hiérarchie 1 : statut indéterminé → A_ANALYSER ──────────────────────

    @Test
    void analyze_statutIndetermine_returnsAanalyser() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.INDETERMINE,
                null,
                true, true, true, true, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict()).isEqualTo(DroitDeconnexionBeVerdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("STATUT_ACCORD_INDETERMINE");
        assertThat(r.synthese()).contains("indéterminé");
    }

    // ── Hiérarchie 2 : seuil non atteint ────────────────────────────────────

    @Test
    void analyze_effectif15_returnsHorsChamp() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                15,
                DroitDeconnexionBeStatutAccord.AUCUNE_INITIATIVE,
                null,
                false, false, false, false, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT);
        assertThat(r.seuilAtteint()).isFalse();
        assertThat(r.raison()).isEqualTo("EFFECTIF_INFERIEUR_SEUIL");
        assertThat(r.synthese()).contains("15", "20");
    }

    @Test
    void analyze_seuilExact20_estDansLeChamp() {
        // 20 = seuil inclusif → obligation applicable
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                20,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, true, true, false);

        assertThat(DroitDeconnexionBeChecker.analyze(req).verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.CONFORME_ACCORD_COMPLET);
    }

    @Test
    void analyze_seuilJuste19_horsChamp() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                19,
                DroitDeconnexionBeStatutAccord.AUCUNE_INITIATIVE,
                null,
                false, false, false, false, false);

        assertThat(DroitDeconnexionBeChecker.analyze(req).verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT);
    }

    // ── Hiérarchie 3 : aucune initiative ────────────────────────────────────

    @Test
    void analyze_aucuneInitiative_returnsManquementGrave() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.AUCUNE_INITIATIVE,
                null,
                false, false, false, false, true);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.MANQUEMENT_GRAVE_AUCUNE_INITIATIVE);
        assertThat(r.raison()).isEqualTo("AUCUNE_INITIATIVE");
        assertThat(r.synthese()).contains("art. 137", "niveau 2");
    }

    // ── Hiérarchie 4 : négociation en cours sans instrument ─────────────────

    @Test
    void analyze_negociationSansInstrument_returnsInstrumentManquant() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.NEGOCIATION_EN_COURS,
                null,
                true, true, true, true, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.NON_CONFORME_INSTRUMENT_MANQUANT);
        assertThat(r.instrumentFormalise()).isFalse();
        assertThat(r.raison()).isEqualTo("INSTRUMENT_NON_FORMALISE");
        assertThat(r.synthese()).contains("obligation de résultat");
    }

    // ── Hiérarchie 5 : contenu incomplet ────────────────────────────────────

    @Test
    void analyze_modalitesPratiquesManquantes_returnsContenuIncomplet() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                false, true, true, true, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.NON_CONFORME_CONTENU_INCOMPLET);
        assertThat(r.contenuComplet()).isFalse();
        assertThat(r.modalitesPratiquesRespectees()).isFalse();
        assertThat(r.raison()).isEqualTo("CONTENU_INCOMPLET");
        assertThat(r.synthese()).contains("modalités pratiques");
    }

    @Test
    void analyze_sensibilisationManquante_returnsContenuIncomplet() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.REGLEMENT_TRAVAIL_MODIFIE,
                DATE_VIGUEUR,
                true, false, true, true, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.NON_CONFORME_CONTENU_INCOMPLET);
        assertThat(r.synthese()).contains("sensibilisation");
    }

    @Test
    void analyze_organisationManquante_returnsContenuIncomplet() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, false, true, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.NON_CONFORME_CONTENU_INCOMPLET);
        assertThat(r.synthese()).contains("organisation");
    }

    @Test
    void analyze_troisThemesManquants_returnsContenuIncomplet() {
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                false, false, false, true, false);

        DroitDeconnexionBeResult r = DroitDeconnexionBeChecker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.NON_CONFORME_CONTENU_INCOMPLET);
        assertThat(r.synthese())
                .contains("modalités pratiques", "sensibilisation",
                        "organisation");
    }

    // ── Priorisation entre verdicts ─────────────────────────────────────────

    @Test
    void analyze_seuilNonAtteintPrimeAucuneInitiative() {
        // effectif 5 + AUCUNE_INITIATIVE → hors champ gagne
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                5,
                DroitDeconnexionBeStatutAccord.AUCUNE_INITIATIVE,
                null,
                false, false, false, false, false);

        assertThat(DroitDeconnexionBeChecker.analyze(req).verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT);
    }

    @Test
    void analyze_statutIndeterminePrimeSeuilNonAtteint() {
        // INDETERMINE prime même hors seuil — on ne préjuge pas
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                5,
                DroitDeconnexionBeStatutAccord.INDETERMINE,
                null,
                false, false, false, false, false);

        assertThat(DroitDeconnexionBeChecker.analyze(req).verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.A_ANALYSER);
    }

    @Test
    void analyze_aucuneInitiativePrimeNegociation() {
        // aucune initiative + contenu vide → aucune initiative gagne
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.AUCUNE_INITIATIVE,
                null,
                true, true, true, true, false);

        assertThat(DroitDeconnexionBeChecker.analyze(req).verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.MANQUEMENT_GRAVE_AUCUNE_INITIATIVE);
    }

    @Test
    void analyze_negociationPrimeContenuIncomplet() {
        // négociation + contenu incomplet → instrument manquant gagne
        DroitDeconnexionBeRequest req = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.NEGOCIATION_EN_COURS,
                null,
                false, false, false, false, false);

        assertThat(DroitDeconnexionBeChecker.analyze(req).verdict())
                .isEqualTo(DroitDeconnexionBeVerdict.NON_CONFORME_INSTRUMENT_MANQUANT);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        DroitDeconnexionBeResult r =
                DroitDeconnexionBeChecker.analyze(reqConforme());

        assertThat(r.effectifEntreprise()).isEqualTo(50);
        assertThat(r.statutAccord())
                .isEqualTo(DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE);
        assertThat(r.dateEntreeVigueurInstrument()).isEqualTo(DATE_VIGUEUR);
        assertThat(r.modalitesPratiquesDeconnexionDefinies()).isTrue();
        assertThat(r.sensibilisationFormationPrevue()).isTrue();
        assertThat(r.modalitesOrganisationTravailDefinies()).isTrue();
        assertThat(r.consultationOrganeConcertationEffectuee()).isTrue();
        assertThat(r.manquementSignaleCbeOuSpf()).isFalse();
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi03102022EtArticles() {
        DroitDeconnexionBeResult r =
                DroitDeconnexionBeChecker.analyze(reqConforme());

        assertThat(r.baseJuridique())
                .contains("Loi du 03/10/2022")
                .contains("Deal pour l'emploi")
                .contains("art. 16")
                .contains("19/02/2023")
                .contains("CCT n° 149")
                .contains("26/03/2018")
                .contains("06/06/2010")
                .contains("art. 137")
                .contains("16/03/1971");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        DroitDeconnexionBeResult r =
                DroitDeconnexionBeChecker.analyze(reqConforme());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("01/04/2023")
                .contains("CCT");
    }

    // ── Validation conditionnelle ───────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_effectifNull_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                null,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, true, true, false);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_effectifNegatif_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                -1,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, true, true, false);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_statutAccordNull_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                50,
                null,
                DATE_VIGUEUR,
                true, true, true, true, false);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutAccord");
    }

    @Test
    void analyze_modalitesPratiquesNull_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                null, true, true, true, false);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modalitesPratiquesDeconnexionDefinies");
    }

    @Test
    void analyze_sensibilisationNull_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, null, true, true, false);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sensibilisationFormationPrevue");
    }

    @Test
    void analyze_organisationNull_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, null, true, false);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("modalitesOrganisationTravailDefinies");
    }

    @Test
    void analyze_consultationNull_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, true, null, false);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consultationOrganeConcertationEffectuee");
    }

    @Test
    void analyze_manquementNull_throws() {
        DroitDeconnexionBeRequest bad = new DroitDeconnexionBeRequest(
                50,
                DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE,
                DATE_VIGUEUR,
                true, true, true, true, null);

        assertThatThrownBy(() -> DroitDeconnexionBeChecker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquementSignaleCbeOuSpf");
    }
}

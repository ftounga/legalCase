package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-11 : tests unitaires de {@link AesPresenceProuveeCalculator}.
 * Couvre fusion des périodes chevauchantes/contiguës, calcul des mois/années,
 * seuils des 4 voies AES (5/10/3/3 ans), détection des gaps et recommandations,
 * ainsi que les cas d'erreur (liste vide, fin avant début, début futur).
 */
class AesPresenceProuveeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    private AesPresenceProuveeRequest.PeriodePresentee p(String debut, String fin, AesPieceType t) {
        return new AesPresenceProuveeRequest.PeriodePresentee(
                LocalDate.parse(debut), LocalDate.parse(fin), t);
    }

    // ── 1. Nominal : 6 ans continus → famille OK, humanitaire KO ───────────

    @Test
    void sixAnsContinus_eligibleFamilleEtudiantMetiers_pasHumanitaire() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2019-01-01", "2024-12-31", AesPieceType.QUITTANCE_LOYER)), TODAY);

        assertThat(result.anneesTotalesProuvees()).isEqualTo(6);
        assertThat(result.eligibiliteParVoie())
                .containsEntry(AesPresenceProuveeCalculator.VOIE_FAMILLE, true)
                .containsEntry(AesPresenceProuveeCalculator.VOIE_ETUDIANT, true)
                .containsEntry(AesPresenceProuveeCalculator.VOIE_METIERS_TENSION, true)
                .containsEntry(AesPresenceProuveeCalculator.VOIE_HUMANITAIRE, false);
        assertThat(result.gapsPeriodes()).isEmpty();
    }

    // ── 2. Seuil humanitaire (≥ 10 ans) ────────────────────────────────────

    @Test
    void dixAnsContinus_eligibleTouteVoie_humanitaireInclus() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2014-01-01", "2024-12-31", AesPieceType.AVIS_IMPOSITION)), TODAY);

        assertThat(result.anneesTotalesProuvees()).isGreaterThanOrEqualTo(10);
        assertThat(result.eligibiliteParVoie())
                .containsEntry(AesPresenceProuveeCalculator.VOIE_HUMANITAIRE, true);
    }

    // ── 3. Humanitaire inéligible à 9 ans ──────────────────────────────────

    @Test
    void neufAns_humanitaireIneligible_familleEligible() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2016-01-01", "2024-12-31", AesPieceType.BULLETIN_SALAIRE)), TODAY);

        assertThat(result.anneesTotalesProuvees()).isEqualTo(9);
        assertThat(result.eligibiliteParVoie())
                .containsEntry(AesPresenceProuveeCalculator.VOIE_HUMANITAIRE, false)
                .containsEntry(AesPresenceProuveeCalculator.VOIE_FAMILLE, true);
    }

    // ── 4. Fusion de périodes chevauchantes ────────────────────────────────

    @Test
    void periodesChevauchantes_fusionnees_uneSeulePeriode() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2020-01-01", "2022-06-30", AesPieceType.QUITTANCE_LOYER),
                p("2022-01-01", "2024-12-31", AesPieceType.FACTURE_EDF_GAZ)), TODAY);

        assertThat(result.periodesFusionnees()).hasSize(1);
        assertThat(result.periodesFusionnees().get(0).debut()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(result.periodesFusionnees().get(0).fin()).isEqualTo(LocalDate.parse("2024-12-31"));
        assertThat(result.gapsPeriodes()).isEmpty();
        // 2020-01-01 → 2024-12-31 = 5 ans, sans double-comptage du chevauchement.
        assertThat(result.anneesTotalesProuvees()).isEqualTo(5);
    }

    // ── 5. Périodes contiguës (gap ≤ 1 jour) fusionnées ────────────────────

    @Test
    void periodesContigues_fusionnees() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2020-01-01", "2020-12-31", AesPieceType.RIB_BANQUE),
                p("2021-01-01", "2021-12-31", AesPieceType.RIB_BANQUE)), TODAY);

        assertThat(result.periodesFusionnees()).hasSize(1);
        assertThat(result.gapsPeriodes()).isEmpty();
        assertThat(result.anneesTotalesProuvees()).isEqualTo(2);
    }

    // ── 6. Gap détecté entre deux périodes disjointes ──────────────────────

    @Test
    void periodesDisjointes_gapDetecte_etRecommandation() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2018-01-01", "2019-12-31", AesPieceType.QUITTANCE_LOYER),
                p("2022-01-01", "2024-12-31", AesPieceType.AVIS_IMPOSITION)), TODAY);

        assertThat(result.periodesFusionnees()).hasSize(2);
        assertThat(result.gapsPeriodes()).hasSize(1);
        var gap = result.gapsPeriodes().get(0);
        assertThat(gap.debut()).isEqualTo(LocalDate.parse("2020-01-01"));
        assertThat(gap.fin()).isEqualTo(LocalDate.parse("2021-12-31"));
        assertThat(gap.dureeMois()).isGreaterThan(20);
        assertThat(result.recommandationsPieces())
                .anyMatch(r -> r.contains("sans preuve"));
    }

    // ── 7. Présence courte (< 3 ans) : aucune voie atteinte ────────────────

    @Test
    void deuxAns_aucuneVoieAtteinte() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2023-01-01", "2024-12-31", AesPieceType.SCOLARITE_ENFANT)), TODAY);

        assertThat(result.anneesTotalesProuvees()).isEqualTo(2);
        assertThat(result.eligibiliteParVoie().values()).containsOnly(false);
        assertThat(result.recommandationsPieces())
                .anyMatch(r -> r.contains("inférieure à 3 ans"));
    }

    // ── 8. Seuil étudiant / métiers tension exact (3 ans) ──────────────────

    @Test
    void troisAnsExacts_etudiantEtMetiersTensionEligibles_pasFamille() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2022-01-01", "2024-12-31", AesPieceType.ATTESTATION_EMPLOYEUR)), TODAY);

        assertThat(result.anneesTotalesProuvees()).isEqualTo(3);
        assertThat(result.eligibiliteParVoie())
                .containsEntry(AesPresenceProuveeCalculator.VOIE_ETUDIANT, true)
                .containsEntry(AesPresenceProuveeCalculator.VOIE_METIERS_TENSION, true)
                .containsEntry(AesPresenceProuveeCalculator.VOIE_FAMILLE, false);
    }

    // ── 9. Ordre non trié en entrée → trié et fusionné ─────────────────────

    @Test
    void periodesDesordonnees_triees() {
        var result = AesPresenceProuveeCalculator.analyze(List.of(
                p("2022-01-01", "2024-12-31", AesPieceType.RIB_BANQUE),
                p("2018-01-01", "2019-12-31", AesPieceType.QUITTANCE_LOYER)), TODAY);

        assertThat(result.periodesNormalisees().get(0).debut())
                .isEqualTo(LocalDate.parse("2018-01-01"));
        assertThat(result.periodesFusionnees()).hasSize(2);
    }

    // ── Cas d'erreur ───────────────────────────────────────────────────────

    @Test
    void listeVide_leveException() {
        assertThatThrownBy(() -> AesPresenceProuveeCalculator.analyze(List.of(), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void finAvantDebut_leveException() {
        assertThatThrownBy(() -> AesPresenceProuveeCalculator.analyze(List.of(
                p("2020-12-31", "2020-01-01", AesPieceType.RIB_BANQUE)), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void debutFutur_leveException() {
        assertThatThrownBy(() -> AesPresenceProuveeCalculator.analyze(List.of(
                p("2030-01-01", "2030-12-31", AesPieceType.RIB_BANQUE)), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

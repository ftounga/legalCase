package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-214-15 : tests unitaires de {@link RecepisseAttestationAnalyzer}.
 * Couvre le récépissé (droit au travail), l'attestation de prolongation
 * (risque employeur), le type inconnu (recommandations d'identification),
 * le calcul de la durée de validité et la validation des entrées.
 */
class RecepisseAttestationAnalyzerTest {

    @Test
    void analyze_recepisse_ouvre_droit_travail_sans_risque_employeur() {
        RecepisseAttestationResult r = RecepisseAttestationAnalyzer.analyze(
                RecepisseAttestationAnalyzer.TYPE_RECEPISSE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), true);

        assertThat(r.droitSejour()).isTrue();
        assertThat(r.droitTravail()).isTrue();
        assertThat(r.risqueEmployeur()).isFalse();
        assertThat(r.baseJuridique()).contains("R. 311-4");
        assertThat(r.recommandations()).isNotEmpty();
    }

    @Test
    void analyze_attestationProlongation_ferme_travail_et_signale_risque_employeur() {
        RecepisseAttestationResult r = RecepisseAttestationAnalyzer.analyze(
                RecepisseAttestationAnalyzer.TYPE_ATTESTATION_PROLONGATION,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1), null);

        assertThat(r.droitSejour()).isTrue();
        assertThat(r.droitTravail()).isFalse();
        assertThat(r.risqueEmployeur()).isTrue();
        assertThat(r.baseJuridique()).contains("R. 311-6");
        assertThat(r.recommandations())
                .anyMatch(s -> s.contains("L. 8253-1"));
    }

    @Test
    void analyze_inconnu_ferme_travail_et_donne_recommandations_identification() {
        RecepisseAttestationResult r = RecepisseAttestationAnalyzer.analyze(
                RecepisseAttestationAnalyzer.TYPE_INCONNU, null, null, null);

        assertThat(r.droitSejour()).isTrue();
        assertThat(r.droitTravail()).isFalse();
        assertThat(r.risqueEmployeur()).isFalse();
        assertThat(r.dureeValiditeJours()).isNull();
        assertThat(r.recommandations())
                .anyMatch(s -> s.toLowerCase().contains("indéterminé")
                        || s.toLowerCase().contains("identifier"));
    }

    @Test
    void analyze_calcule_dureeValiditeJours() {
        RecepisseAttestationResult r = RecepisseAttestationAnalyzer.analyze(
                RecepisseAttestationAnalyzer.TYPE_RECEPISSE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null);

        // 30 jours entre le 1er et le 31 janvier.
        assertThat(r.dureeValiditeJours()).isEqualTo(30L);
    }

    @Test
    void analyze_recepisse_sansMentionTravail_ajoute_recommandation_specifique() {
        RecepisseAttestationResult r = RecepisseAttestationAnalyzer.analyze(
                RecepisseAttestationAnalyzer.TYPE_RECEPISSE,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1), false);

        assertThat(r.mentionAutorisationTravail()).isFalse();
        assertThat(r.recommandations())
                .anyMatch(s -> s.contains("autorise à travailler"));
    }

    @Test
    void analyze_typeNull_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RecepisseAttestationAnalyzer.analyze(
                null, LocalDate.now(), LocalDate.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeDocument");
    }

    @Test
    void analyze_typeInconnuHorsWhitelist_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RecepisseAttestationAnalyzer.analyze(
                "PASSEPORT", LocalDate.now(), LocalDate.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeDocument");
    }

    @Test
    void analyze_dateExpirationAvantDelivrance_lanceIllegalArgumentException() {
        assertThatThrownBy(() -> RecepisseAttestationAnalyzer.analyze(
                RecepisseAttestationAnalyzer.TYPE_RECEPISSE,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateExpiration");
    }

    @Test
    void analyze_datesNull_resultatSansDuree() {
        RecepisseAttestationResult r = RecepisseAttestationAnalyzer.analyze(
                RecepisseAttestationAnalyzer.TYPE_ATTESTATION_PROLONGATION, null, null, null);

        assertThat(r.dureeValiditeJours()).isNull();
        assertThat(r.droitTravail()).isFalse();
        assertThat(r.risqueEmployeur()).isTrue();
    }
}

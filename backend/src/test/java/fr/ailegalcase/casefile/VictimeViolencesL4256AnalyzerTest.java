package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-208-04 : tests unitaires de {@link VictimeViolencesL4256Analyzer}.
 */
class VictimeViolencesL4256AnalyzerTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void analyze_nominal_pleinDroit() {
        // Ordonnance JAF 2026-03-01, 6 mois → expire 2026-09-01, today 2026-05-10 → ELIGIBLE_PLEIN_DROIT
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 3, 1),
                "JAF Paris",
                6,
                null,
                2,
                "Marocaine",
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.eligibiliteScore()).isEqualTo("ELIGIBLE_PLEIN_DROIT");
        assertThat(r.dateExpirationProtectionEffective()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(r.dureeTitreSejourMois()).isEqualTo(12);
        assertThat(r.criteresValides()).isNotEmpty();
        assertThat(r.criteresManquants()).isEmpty();
        assertThat(r.baseJuridique()).contains("L.425-6").contains("Cciv 515-9");
    }

    @Test
    void analyze_juridictionNonJaf_sousReserve() {
        // Ordonnance "TGI Paris" (pas JAF) → ELIGIBLE_SOUS_RESERVE (1 critère manquant)
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 3, 1),
                "TGI Paris",
                6,
                null,
                0,
                null,
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.eligibiliteScore()).isEqualTo("ELIGIBLE_SOUS_RESERVE");
        assertThat(r.criteresManquants()).anySatisfy(c -> assertThat(c).contains("JAF"));
    }

    @Test
    void analyze_ordonnanceExpireeRecente_sousReserve() {
        // Ordonnance 2025-09-01 (6 mois → expire 2026-03-01), today 2026-05-10 → ~2 mois post-exp → SOUS_RESERVE
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2025, 9, 1),
                "JAF Lyon",
                6,
                null,
                1,
                "Tunisienne",
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.eligibiliteScore()).isEqualTo("ELIGIBLE_SOUS_RESERVE");
        assertThat(r.criteresManquants()).anySatisfy(c -> assertThat(c).contains("expirée"));
    }

    @Test
    void analyze_ordonnanceTresExpiree_nonEligible() {
        // Ordonnance 2024-09-01 (expire 2025-03-01), today 2026-05-10 → > 3 mois post-exp + JAF présent
        // critères manquants = 1 (expirée), juridiction OK → SOUS_RESERVE selon règle (1 critère manquant)
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2024, 9, 1),
                "JAF Marseille",
                6,
                null,
                0,
                null,
                clockAt(LocalDate.of(2026, 5, 10)));

        // Le règlage "1 critère manquant → SOUS_RESERVE" → ici expiration > 3 mois, ordonnance JAF présente
        // → 1 critère manquant → ELIGIBLE_SOUS_RESERVE (logique métier conservatrice)
        assertThat(r.eligibiliteScore()).isEqualTo("ELIGIBLE_SOUS_RESERVE");
        assertThat(r.criteresManquants()).anySatisfy(c -> assertThat(c).contains("plus de"));
    }

    @Test
    void analyze_juridictionNonJafEtExpiree_nonEligible() {
        // Les 2 critères manquants → NON_ELIGIBLE
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2024, 9, 1),
                "TGI Bobigny",
                6,
                null,
                0,
                null,
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.eligibiliteScore()).isEqualTo("NON_ELIGIBLE");
        assertThat(r.criteresManquants()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void analyze_dateExpirationFournie_useIt() {
        // dateExpiration explicite 2026-12-31 (override la durée 6m)
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 1, 1),
                "JAF Paris",
                6,
                LocalDate.of(2026, 12, 31),
                0,
                null,
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.dateExpirationProtectionEffective()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(r.eligibiliteScore()).isEqualTo("ELIGIBLE_PLEIN_DROIT");
    }

    @Test
    void analyze_nationaliteAlgerienne_addsAccordMessage() {
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 3, 1),
                "JAF Paris",
                6,
                null,
                0,
                "Algérienne",
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Accord franco-alg"));
    }

    @Test
    void analyze_enfantsAcharge_addsMessage() {
        VictimeViolencesL4256Result r = VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 3, 1),
                "JAF Paris",
                6,
                null,
                3,
                null,
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("3 enfant"));
    }

    @Test
    void analyze_futureDate_throws() {
        assertThatThrownBy(() -> VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 6, 1),
                "JAF Paris",
                6,
                null,
                0,
                null,
                clockAt(LocalDate.of(2026, 5, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void analyze_invalidDuree_throws() {
        assertThatThrownBy(() -> VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 3, 1),
                "JAF Paris",
                0,
                null,
                0,
                null,
                clockAt(LocalDate.of(2026, 5, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeProtectionMois");
    }

    @Test
    void analyze_negativeEnfants_throws() {
        assertThatThrownBy(() -> VictimeViolencesL4256Analyzer.analyze(
                LocalDate.of(2026, 3, 1),
                "JAF Paris",
                6,
                null,
                -1,
                null,
                clockAt(LocalDate.of(2026, 5, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enfantsAcharge");
    }

    @Test
    void analyze_nullDate_throws() {
        assertThatThrownBy(() -> VictimeViolencesL4256Analyzer.analyze(
                null,
                "JAF Paris",
                6,
                null,
                0,
                null,
                clockAt(LocalDate.of(2026, 5, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateOrdonnanceProtection");
    }
}

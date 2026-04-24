package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Belgian40terCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_toutesConditions_ok_revenusLargementAuDessus_scoreMaxAvecBonus() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT",
                true,
                new BigDecimal("2500"), // largement > 1740 * 1.2 = 2088
                new BigDecimal("1740"),
                true,
                true,
                false,
                LocalDate.of(2026, 4, 10),
                clockAt(today));

        assertThat(r.lienValide()).isTrue();
        assertThat(r.regroupantBelgeOk()).isTrue();
        assertThat(r.revenusSuffisantsOk()).isTrue();
        assertThat(r.assuranceOk()).isTrue();
        assertThat(r.logementOk()).isTrue();
        assertThat(r.pasMenace()).isTrue();
        assertThat(r.differentielRevenus()).isEqualByComparingTo("760.00");
        // 6 × 18 = 108 + bonus 10 → plafonné 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateExpirationInstructionSiDemande())
                .isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(r.baseJuridique()).contains("40ter").contains("RIS");
        assertThat(r.formule()).contains("ELEVEE").contains("100/100");
    }

    @Test
    void compute_toutesConditions_ok_revenusJusteAuSeuil_pasDeBonus() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT",
                true,
                new BigDecimal("1740"), // exactement au seuil
                new BigDecimal("1740"),
                true,
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.revenusSuffisantsOk()).isTrue();
        assertThat(r.differentielRevenus()).isEqualByComparingTo("0.00");
        // 6 × 18 = 108 → plafonné 100, sans bonus
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_revenusLegerementEnDessous_ko_differentielNegatif() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT",
                true,
                new BigDecimal("1700"),
                new BigDecimal("1740"),
                true,
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.revenusSuffisantsOk()).isFalse();
        assertThat(r.differentielRevenus()).isEqualByComparingTo("-40.00");
        // 5 × 18 = 90, sans bonus, pas plafonné
        assertThat(r.scoreGlobal()).isEqualTo(90);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("120 % RIS"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("40.00 €/mois"));
    }

    @Test
    void compute_tousLesTypesDeLien_sontValides() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        for (String lien : new String[]{"CONJOINT", "PARTENAIRE_LEGAL_ENREGISTRE",
                "DESCENDANT_MINEUR", "DESCENDANT_MAJEUR_CHARGE",
                "ASCENDANT_CHARGE_HANDICAP"}) {
            Belgian40terResult r = Belgian40terCalculator.compute(
                    lien, true, new BigDecimal("1800"), new BigDecimal("1740"),
                    true, true, false, null, clockAt(today));
            assertThat(r.lienValide()).as("lien " + lien).isTrue();
        }
    }

    @Test
    void compute_lienInvalide_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian40terCalculator.compute(
                "CONCUBIN_SIMPLE", true,
                new BigDecimal("1800"), new BigDecimal("1740"),
                true, true, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lienFamilial invalide");
    }

    @Test
    void compute_regroupantNonBelge_scoreInferieur() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT",
                false, // non Belge
                new BigDecimal("1800"),
                new BigDecimal("1740"),
                true,
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.regroupantBelgeOk()).isFalse();
        // 5 × 18 = 90
        assertThat(r.scoreGlobal()).isEqualTo(90);
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("non Belge"));
    }

    @Test
    void compute_seuilDefaut_siNull() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT",
                true,
                new BigDecimal("1739"),
                null, // utilise le défaut 1740
                true,
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.seuil120PctRisEur()).isEqualByComparingTo("1740");
        assertThat(r.revenusSuffisantsOk()).isFalse();
    }

    @Test
    void compute_bonusDifferentielSolide_applique_siPlusDe20PctAuDessus() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        // 5 conditions OK sur 6 + différentiel >20% → 5×18 + 10 = 100
        // (assurance KO pour laisser place au bonus visible)
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT",
                true,
                new BigDecimal("1201"),
                new BigDecimal("1000"),
                false, // assurance KO
                true,
                false,
                null,
                clockAt(today));

        // 5 × 18 = 90 + 10 bonus = 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
    }

    @Test
    void compute_bonusDifferentielSolide_pasApplique_siExactement20Pct() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        // seuil 1000, exactement +20% = 1200 → pas de bonus (strictement >)
        // 5 conditions OK sur 6 (assurance KO) → 5×18 = 90, sans bonus
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT",
                true,
                new BigDecimal("1200"),
                new BigDecimal("1000"),
                false, // assurance KO
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.revenusSuffisantsOk()).isTrue();
        assertThat(r.differentielRevenus()).isEqualByComparingTo("200.00");
        // 5 × 18 = 90, pas de bonus (= 20% exact, pas strictement >)
        assertThat(r.scoreGlobal()).isEqualTo(90);
    }

    @Test
    void compute_verdict_seuilsExacts() {
        LocalDate today = LocalDate.of(2026, 4, 24);

        // 4 × 18 = 72 = MOYENNE (lien OK + regroupant OK + assurance OK + logement OK,
        // revenus KO et menace OK KO)
        Belgian40terResult r72 = Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("1500"),
                new BigDecimal("1740"),
                true, true, true, null, clockAt(today));
        assertThat(r72.scoreGlobal()).isEqualTo(72);
        assertThat(r72.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");

        // 5 × 18 = 90 = ELEVEE (menace KO, le reste OK — différentiel 60 < 20% × 1740)
        Belgian40terResult r90 = Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("1800"),
                new BigDecimal("1740"),
                true, true, true, null, clockAt(today));
        assertThat(r90.scoreGlobal()).isEqualTo(90);
        assertThat(r90.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");

        // 2 × 18 = 36 = FAIBLE (lien + regroupant seuls)
        Belgian40terResult r36 = Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("100"),
                new BigDecimal("1740"),
                false, false, true, null, clockAt(today));
        assertThat(r36.scoreGlobal()).isEqualTo(36);
        assertThat(r36.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_toutKo_scoreZero() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian40terCalculator.compute(
                "INVALID", false, new BigDecimal("100"),
                new BigDecimal("1740"),
                false, false, true, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class);

        // Avec un lien valide mais tout le reste KO
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT", false, new BigDecimal("100"),
                new BigDecimal("1740"),
                false, false, true, null, clockAt(today));
        // 1 × 18 (lien) = 18
        assertThat(r.scoreGlobal()).isEqualTo(18);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void compute_messages_incluentMentionsCleWacle() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("2500"),
                new BigDecimal("1740"),
                true, true, false, null, clockAt(today));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Carte F"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CCE"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("30 jours"));
    }

    @Test
    void compute_dateExpiration_depotPlus6Mois() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("1800"),
                new BigDecimal("1740"),
                true, true, false,
                LocalDate.of(2026, 1, 15), clockAt(today));
        assertThat(r.dateExpirationInstructionSiDemande())
                .isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void compute_dateExpiration_nullSiPasDeDepot() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian40terResult r = Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("1800"),
                new BigDecimal("1740"),
                true, true, false, null, clockAt(today));
        assertThat(r.dateExpirationInstructionSiDemande()).isNull();
    }

    @Test
    void compute_revenusNegatifs_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("-100"),
                new BigDecimal("1740"),
                true, true, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusMensuelsNetsEur");
    }

    @Test
    void compute_revenusNull_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian40terCalculator.compute(
                "CONJOINT", true, null,
                new BigDecimal("1740"),
                true, true, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusMensuelsNetsEur");
    }

    @Test
    void compute_seuilNegatif_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("1800"),
                new BigDecimal("0"),
                true, true, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seuil120PctRisEur");
    }

    @Test
    void compute_dateDepotDansLeFutur_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian40terCalculator.compute(
                "CONJOINT", true, new BigDecimal("1800"),
                new BigDecimal("1740"),
                true, true, false,
                LocalDate.of(2026, 5, 1), clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }

    @Test
    void compute_lienNull_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian40terCalculator.compute(
                null, true, new BigDecimal("1800"),
                new BigDecimal("1740"),
                true, true, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lienFamilial");
    }
}

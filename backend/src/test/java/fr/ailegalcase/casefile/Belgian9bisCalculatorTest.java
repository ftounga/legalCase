package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Belgian9bisCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_nominalElevee_scoreMax() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1),
                72, // 6 ans
                true,
                true,   // famille BE
                true,   // pro
                true,   // scolarité
                false,  // pas menace
                LocalDate.of(2026, 4, 10),
                clockAt(today));

        assertThat(r.presence3AnsOk()).isTrue();
        assertThat(r.liensConstitutifsOk()).isTrue();
        assertThat(r.pasMenace()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100); // 25 + 40 + 35
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateExpirationInstructionPrevisionnelle())
                .isEqualTo(LocalDate.of(2027, 10, 10)); // +18 mois
        assertThat(r.baseJuridique())
                .contains("9bis").contains("Loi 15/12/1980").contains("17/05/2007");
        assertThat(r.formule()).contains("ELEVEE").contains("100/100");
    }

    @Test
    void compute_elevee_sansScolariteNiPro_uniquementFamille() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1),
                72,
                true,
                true,   // famille
                false,
                false,
                false,
                null,
                clockAt(today));
        // 25 + 40 + 35 = 100
        assertThat(r.liensConstitutifsOk()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_liensConstitutifs_sontOrLogique() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        // seulement pro
        Belgian9bisResult rPro = Belgian9bisCalculator.compute(
                LocalDate.of(2022, 1, 1), 48,
                true, false, true, false, false, null, clockAt(today));
        assertThat(rPro.liensConstitutifsOk()).isTrue();

        // seulement scolarité
        Belgian9bisResult rSco = Belgian9bisCalculator.compute(
                LocalDate.of(2022, 1, 1), 48,
                true, false, false, true, false, null, clockAt(today));
        assertThat(rSco.liensConstitutifsOk()).isTrue();

        // aucun
        Belgian9bisResult rNone = Belgian9bisCalculator.compute(
                LocalDate.of(2022, 1, 1), 48,
                true, false, false, false, false, null, clockAt(today));
        assertThat(rNone.liensConstitutifsOk()).isFalse();
    }

    @Test
    void compute_moyenne_presenceFaibleMaisLiens() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2025, 1, 1),
                15, // < 3 ans
                true,
                true,   // famille
                false,
                false,
                false,  // pas menace
                null,
                clockAt(today));
        // 0 (présence) + 40 (liens) + 35 (pas menace) = 75
        assertThat(r.presence3AnsOk()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(75);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE"); // seuil 75 inclusif
    }

    @Test
    void compute_moyenne_score40Exact() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2025, 1, 1),
                15,
                false,
                true, // 40
                false, false,
                true, // menace → 0
                null,
                clockAt(today));
        assertThat(r.scoreGlobal()).isEqualTo(40);
        assertThat(r.verdictProbabilite()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_faible_rienDeSolide() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2025, 6, 1),
                10,
                false,
                false, false, false,
                true,   // menace
                null,
                clockAt(today));
        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictProbabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).hasSize(4); // présence, liens, menace, circonstances
    }

    @Test
    void compute_verdictSeuils_ELEVEE_MOYENNE_FAIBLE() {
        LocalDate today = LocalDate.of(2026, 4, 24);

        // 75 ELEVEE : liens 40 + menace 35 = 75
        Belgian9bisResult r75 = Belgian9bisCalculator.compute(
                LocalDate.of(2025, 1, 1), 15,
                true, true, false, false, false, null, clockAt(today));
        assertThat(r75.scoreGlobal()).isEqualTo(75);
        assertThat(r75.verdictProbabilite()).isEqualTo("ELEVEE");

        // 35 FAIBLE : seulement pas menace
        Belgian9bisResult r35 = Belgian9bisCalculator.compute(
                LocalDate.of(2025, 1, 1), 15,
                true, false, false, false, false, null, clockAt(today));
        assertThat(r35.scoreGlobal()).isEqualTo(35);
        assertThat(r35.verdictProbabilite()).isEqualTo("FAIBLE");

        // 60 MOYENNE : présence 25 + menace 35 = 60
        Belgian9bisResult r60 = Belgian9bisCalculator.compute(
                LocalDate.of(2022, 1, 1), 40,
                false, false, false, false, false, null, clockAt(today));
        assertThat(r60.scoreGlobal()).isEqualTo(60);
        assertThat(r60.verdictProbabilite()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_criteresNonRemplis_humanReadable() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2025, 1, 1), 15,
                false,
                false, false, false,
                true,
                null,
                clockAt(today));
        assertThat(r.criteresNonRemplis())
                .contains("Présence en Belgique inférieure à 3 ans (signal faible)")
                .contains("Aucun lien constitutif (ni famille BE, ni travail, ni scolarité d'enfants)")
                .contains("Menace pour l'ordre public présumée")
                .contains("Circonstances exceptionnelles non documentées (condition de recevabilité 9bis)");
    }

    @Test
    void compute_dateExpiration_null_siPasDepot() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1), 72,
                true, true, false, false, false, null, clockAt(today));
        assertThat(r.dateExpirationInstructionPrevisionnelle()).isNull();
    }

    @Test
    void compute_dateExpiration_depotPlus18Mois() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1), 72,
                true, true, false, false, false,
                LocalDate.of(2026, 4, 1), clockAt(today));
        assertThat(r.dateExpirationInstructionPrevisionnelle())
                .isEqualTo(LocalDate.of(2027, 10, 1));
    }

    @Test
    void compute_messages_mentionnent9ter_ordrePublic_circonstances() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1), 72,
                false, true, false, false, false, null, clockAt(today));
        // Alternative 9ter toujours mentionnée
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("9ter"));
        // Circonstances exceptionnelles absentes → avertissement
        assertThat(r.messages())
                .anySatisfy(m -> assertThat(m).contains("circonstances exceptionnelles"));
        // OE mentionné
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Office des étrangers"));
    }

    @Test
    void compute_messages_elevee_mentionneTitre() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult r = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1), 72,
                true, true, true, true, false, null, clockAt(today));
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.messages())
                .anySatisfy(m -> assertThat(m).contains("CIRE"));
    }

    @Test
    void compute_futureDateEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian9bisCalculator.compute(
                LocalDate.of(2026, 5, 1), 12,
                true, true, false, false, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullDateEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian9bisCalculator.compute(
                null, 12, true, true, false, false, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateEntreeBelgique");
    }

    @Test
    void compute_dureePresenceNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1), -5,
                true, true, false, false, false, null, clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureePresenceMois");
    }

    @Test
    void compute_dateDepotAvantEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> Belgian9bisCalculator.compute(
                LocalDate.of(2022, 4, 1), 48,
                true, true, false, false, false,
                LocalDate.of(2021, 1, 1), clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }

    @Test
    void compute_pasMenace_inverseMenaceOrdrePublic() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        Belgian9bisResult rSafe = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1), 72,
                true, true, false, false, false, null, clockAt(today));
        assertThat(rSafe.pasMenace()).isTrue();
        Belgian9bisResult rBad = Belgian9bisCalculator.compute(
                LocalDate.of(2020, 1, 1), 72,
                true, true, false, false, true, null, clockAt(today));
        assertThat(rBad.pasMenace()).isFalse();
        assertThat(rBad.criteresNonRemplis())
                .contains("Menace pour l'ordre public présumée");
    }
}

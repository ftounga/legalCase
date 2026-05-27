package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SinglePermitBeCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    // ---- statuts renouvellement ----

    @Test
    void compute_depose_en_temps_quand_plus_de_60j_avant_expiration() {
        // today 2026-01-01, dateFinPermit 2026-12-31 → 364j → DEPOSE_EN_TEMPS
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                SinglePermitBeRegionEnum.WALLONIE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.RENOUVELLEMENT,
                clockAt(LocalDate.of(2026, 1, 1)));

        assertThat(r.statutRenouvellement()).isEqualTo(SinglePermitBeStatutRenouvellement.DEPOSE_EN_TEMPS);
        assertThat(r.joursAvantExpiration()).isEqualTo(364L);
        // 2026-12-31 minus 60 jours = 2026-11-01
        assertThat(r.dateLimiteDemande()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(r.baseJuridique()).contains("30/04/1999").contains("02/09/2018").contains("art. 23");
    }

    @Test
    void compute_dans_delai_quand_entre_30_et_60j() {
        // today 2026-03-01, dateFinPermit 2026-04-15 → 45j → DANS_DELAI
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2025, 4, 15),
                LocalDate.of(2026, 4, 15),
                SinglePermitBeRegionEnum.WALLONIE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.RENOUVELLEMENT,
                clockAt(LocalDate.of(2026, 3, 1)));

        assertThat(r.statutRenouvellement()).isEqualTo(SinglePermitBeStatutRenouvellement.DANS_DELAI);
        assertThat(r.joursAvantExpiration()).isEqualTo(45L);
    }

    @Test
    void compute_urgent_quand_inferieur_ou_egal_30j() {
        // today 2026-04-01, dateFinPermit 2026-04-15 → 14j → URGENT
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2025, 4, 15),
                LocalDate.of(2026, 4, 15),
                SinglePermitBeRegionEnum.FLANDRE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.RENOUVELLEMENT,
                clockAt(LocalDate.of(2026, 4, 1)));

        assertThat(r.statutRenouvellement()).isEqualTo(SinglePermitBeStatutRenouvellement.URGENT);
        assertThat(r.joursAvantExpiration()).isEqualTo(14L);
    }

    @Test
    void compute_expire_quand_today_apres_dateFinPermit() {
        // today 2026-05-01, dateFinPermit 2026-04-15 → EXPIRE
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2025, 4, 15),
                LocalDate.of(2026, 4, 15),
                SinglePermitBeRegionEnum.BRUXELLES,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.RENOUVELLEMENT,
                clockAt(LocalDate.of(2026, 5, 1)));

        assertThat(r.statutRenouvellement()).isEqualTo(SinglePermitBeStatutRenouvellement.EXPIRE);
        assertThat(r.joursAvantExpiration()).isEqualTo(-16L);
    }

    // ---- région compétente ----

    @Test
    void compute_region_bruxelles_returns_ACTIRIS() {
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                SinglePermitBeRegionEnum.BRUXELLES,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.NOUVEAU,
                clockAt(LocalDate.of(2026, 1, 1)));

        assertThat(r.regionCompetente()).contains("ACTIRIS").contains("Bruxelles")
                .contains("Office des Étrangers");
    }

    @Test
    void compute_region_wallonie_returns_FOREM() {
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                SinglePermitBeRegionEnum.WALLONIE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.NOUVEAU,
                clockAt(LocalDate.of(2026, 1, 1)));

        assertThat(r.regionCompetente()).contains("FOREM").contains("Wallonie");
    }

    @Test
    void compute_region_flandre_returns_VDAB() {
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                SinglePermitBeRegionEnum.FLANDRE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.NOUVEAU,
                clockAt(LocalDate.of(2026, 1, 1)));

        assertThat(r.regionCompetente()).contains("VDAB").contains("Flandre");
    }

    // ---- étapes prochaines : NOUVEAU vs RENOUVELLEMENT ----

    @Test
    void compute_motif_nouveau_returns_etapes_5_visa_D() {
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                SinglePermitBeRegionEnum.WALLONIE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.NOUVEAU,
                clockAt(LocalDate.of(2026, 1, 1)));

        assertThat(r.etapesProchaines()).hasSize(5);
        assertThat(String.join("|", r.etapesProchaines())).contains("visa D").contains("Conseil régional");
    }

    @Test
    void compute_motif_renouvellement_returns_etapes_5_60j() {
        SinglePermitBeResult r = SinglePermitBeCalculator.compute(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 12, 31),
                SinglePermitBeRegionEnum.FLANDRE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.RENOUVELLEMENT,
                clockAt(LocalDate.of(2026, 1, 1)));

        assertThat(r.etapesProchaines()).hasSize(5);
        assertThat(String.join("|", r.etapesProchaines())).contains("60 jours").contains("AR 02/09/2018");
    }

    // ---- validation ----

    @Test
    void compute_throws_si_debut_apres_fin() {
        assertThatThrownBy(() -> SinglePermitBeCalculator.compute(
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2026, 1, 1),
                SinglePermitBeRegionEnum.WALLONIE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.NOUVEAU,
                clockAt(LocalDate.of(2026, 1, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incohérentes");
    }

    @Test
    void compute_throws_si_permit_trop_ancien() {
        // dateFinPermit 2023-01-01 vs today 2026-06-01 → expiré > 2 ans → reject
        assertThatThrownBy(() -> SinglePermitBeCalculator.compute(
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2023, 1, 1),
                SinglePermitBeRegionEnum.WALLONIE,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.RENOUVELLEMENT,
                clockAt(LocalDate.of(2026, 6, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trop ancien");
    }

    @Test
    void compute_throws_si_region_null() {
        assertThatThrownBy(() -> SinglePermitBeCalculator.compute(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                null,
                SinglePermitBeTypeActiviteEnum.SALARIE,
                SinglePermitBeMotifEnum.NOUVEAU,
                clockAt(LocalDate.of(2026, 1, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regionInstruction");
    }
}

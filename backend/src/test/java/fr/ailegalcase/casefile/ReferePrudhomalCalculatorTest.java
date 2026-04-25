package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferePrudhomalCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 25);
    private static final LocalDate MED_RECENTE = LocalDate.of(2026, 4, 1); // 24 jours avant TODAY
    private static final BigDecimal MONTANT_5K = new BigDecimal("5000.00");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_provisionFavorable_score80_provisionProbable() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                MONTANT_5K,
                true,
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                true,
                false,
                MED_RECENTE,
                18,
                clockAt(TODAY));

        // 35 (absence contestation) + 25 (≥2 preuves) + 20 (dommage immédiat) + 10 (urgence carac) = 90
        assertThat(r.scoreSuccess()).isEqualTo(90);
        assertThat(r.verdictRecommandation()).isEqualTo("PROVISION_PROBABLE");
        assertThat(r.delaiAudienceJoursPrevisionnel()).isEqualTo(15);
        assertThat(r.delaiOrdonnanceJoursPrevisionnel()).isEqualTo(8);
        assertThat(r.montantProvisionRecommandeEur()).isEqualByComparingTo(MONTANT_5K);
    }

    @Test
    void compute_provisionScoreInsuffisant_autreVoie() {
        // 35 + 0 (1 preuve seule) + 0 + 0 + 0 = 35 → < 40 → INSUFFISAMMENT_FONDE
        // Pour avoir AUTRE_VOIE_RECOMMANDEE il faut score in [40,69]
        // 35 (abs) + 25 (preuves) = 60 sans dommage → AUTRE_VOIE_RECOMMANDEE
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null,
                true,
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                false,
                false,
                null,
                null,
                clockAt(TODAY));

        assertThat(r.scoreSuccess()).isEqualTo(60);
        assertThat(r.verdictRecommandation()).isEqualTo("AUTRE_VOIE_RECOMMANDEE");
    }

    @Test
    void compute_expertiseMedicale_returnsExpertiseRecommandee() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "EXPERTISE_MEDICALE", "AUTRE",
                null,
                false,
                List.of(),
                false,
                false,
                null,
                null,
                clockAt(TODAY));

        assertThat(r.verdictRecommandation()).isEqualTo("EXPERTISE_RECOMMANDEE");
    }

    @Test
    void compute_expertiseTechnique_returnsExpertiseRecommandee() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "EXPERTISE_TECHNIQUE", "HEURES_SUPPLEMENTAIRES",
                null,
                false,
                List.of(),
                false,
                false,
                null,
                null,
                clockAt(TODAY));

        assertThat(r.verdictRecommandation()).isEqualTo("EXPERTISE_RECOMMANDEE");
    }

    @Test
    void compute_aucuneCondition_score0_insuffisamentFonde() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null,
                false,
                List.of(),
                false,
                false,
                null,
                null,
                clockAt(TODAY));

        assertThat(r.scoreSuccess()).isZero();
        assertThat(r.verdictRecommandation()).isEqualTo("INSUFFISAMMENT_FONDE");
    }

    @Test
    void compute_scoreCappedAt100() {
        // 35 + 25 + 20 + 10 + 10 = 100 (cap)
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                MONTANT_5K,
                true,
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR", "MISE_EN_DEMEURE",
                        "CONSTAT_HUISSIER"),
                true,
                true,
                MED_RECENTE,
                24,
                clockAt(TODAY));

        assertThat(r.scoreSuccess()).isEqualTo(100);
        assertThat(r.verdictRecommandation()).isEqualTo("PROVISION_PROBABLE");
    }

    @Test
    void compute_unePreuveSeule_pasDeBonus() {
        // 35 (abs) + 0 (1 preuve seule, seuil 2) + 0 + 0 + 0 = 35 → < 40 → INSUFFISAMMENT_FONDE
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null,
                true,
                List.of("BULLETIN_PAIE"),
                false,
                false,
                null,
                null,
                clockAt(TODAY));

        assertThat(r.scoreSuccess()).isEqualTo(35);
        assertThat(r.verdictRecommandation()).isEqualTo("INSUFFISAMMENT_FONDE");
    }

    @Test
    void compute_cinqPreuves_bonusCappedAt25() {
        // 0 + 25 (≥ 2 preuves, fixe) + 0 + 0 + 0 = 25 → INSUFFISAMMENT_FONDE
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null,
                false,
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR", "MISE_EN_DEMEURE",
                        "CONSTAT_HUISSIER", "CONTRAT"),
                false,
                false,
                null,
                null,
                clockAt(TODAY));

        assertThat(r.scoreSuccess()).isEqualTo(25);
    }

    @Test
    void compute_tresorerieDouteuseSeule_addsBonus10() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "MESURES_CONSERVATOIRES", "SALAIRES_NON_VERSES",
                null,
                false,
                List.of(),
                false,
                true,
                null,
                null,
                clockAt(TODAY));

        assertThat(r.scoreSuccess()).isEqualTo(10);
    }

    @Test
    void compute_baseJuridiqueMentionsR1454R1455() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null, true, List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                false, false, null, null,
                clockAt(TODAY));

        assertThat(r.baseJuridique())
                .contains("R.1454-1")
                .contains("R.1455-1")
                .contains("Code travail");
    }

    @Test
    void compute_formuleContainsTypeAndScoreAndVerdict() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                MONTANT_5K, true, List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                true, false, MED_RECENTE, 18,
                clockAt(TODAY));

        assertThat(r.formule())
                .contains("PROVISION_SALAIRES")
                .contains("score 90")
                .contains("PROVISION_PROBABLE");
    }

    @Test
    void compute_montantRecommandeEqualsDemande_whenCreanceNonContestee() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                MONTANT_5K,
                true,
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                false, false, null, null,
                clockAt(TODAY));

        assertThat(r.montantProvisionRecommandeEur()).isEqualByComparingTo("5000.00");
    }

    @Test
    void compute_montantRecommandeZero_whenCreanceContestee() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                MONTANT_5K,
                false, // créance contestée
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                false, false, null, null,
                clockAt(TODAY));

        assertThat(r.montantProvisionRecommandeEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_montantRecommandeZero_whenTypeNotProvision() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "EXPERTISE_TECHNIQUE", "HEURES_SUPPLEMENTAIRES",
                MONTANT_5K,
                true,
                List.of(),
                false, false, null, null,
                clockAt(TODAY));

        assertThat(r.montantProvisionRecommandeEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_messagesContainR1455_15jours() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null, true, List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                false, false, null, null,
                clockAt(TODAY));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("15 jours"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("R.1455-1"));
    }

    @Test
    void compute_urgenceCalculeeAuto_quandMiseEnDemeureAncienne() {
        // Mise en demeure il y a 30 jours, sans dommageImmediat → urgence carac calculée → +10
        // 35 (abs) + 25 (preuves) + 0 + 0 + 10 (urgence calculée) = 70 → PROVISION_PROBABLE
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                MONTANT_5K,
                true,
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                false,
                false,
                LocalDate.of(2026, 3, 26), // 30 jours avant TODAY
                12,
                clockAt(TODAY));

        assertThat(r.scoreSuccess()).isEqualTo(70);
        assertThat(r.verdictRecommandation()).isEqualTo("PROVISION_PROBABLE");
    }

    @Test
    void compute_typeRefereNull_throws() {
        assertThatThrownBy(() -> ReferePrudhomalCalculator.compute(
                null, "SALAIRES_NON_VERSES",
                null, true, List.of(), false, false, null, null,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRefere");
    }

    @Test
    void compute_typeRefereInconnu_throws() {
        assertThatThrownBy(() -> ReferePrudhomalCalculator.compute(
                "INCONNU", "SALAIRES_NON_VERSES",
                null, true, List.of(), false, false, null, null,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRefere");
    }

    @Test
    void compute_natureCreanceInconnue_throws() {
        assertThatThrownBy(() -> ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "INCONNUE",
                null, true, List.of(), false, false, null, null,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("natureCreance");
    }

    @Test
    void compute_preuveInconnue_throws() {
        assertThatThrownBy(() -> ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null, true, List.of("PREUVE_BIDON"),
                false, false, null, null,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preuveUrgence");
    }

    @Test
    void compute_dateMiseEnDemeureFuture_throws() {
        assertThatThrownBy(() -> ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null, true, List.of(),
                false, false,
                LocalDate.of(2026, 5, 1), // après TODAY
                null,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_montantNegatif_throws() {
        assertThatThrownBy(() -> ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                new BigDecimal("-100"),
                true, List.of(), false, false, null, null,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    @Test
    void compute_ancienneteNegative_throws() {
        assertThatThrownBy(() -> ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                null, true, List.of(), false, false,
                null, -1,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anciennete");
    }

    @Test
    void compute_allTypesAccepted() {
        for (String type : List.of("PROVISION_SALAIRES", "EXPERTISE_MEDICALE",
                "EXPERTISE_TECHNIQUE", "MESURES_CONSERVATOIRES",
                "REINTEGRATION_URGENCE", "AUTRE")) {
            ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                    type, "SALAIRES_NON_VERSES",
                    null, false, List.of(),
                    false, false, null, null,
                    clockAt(TODAY));
            assertThat(r.typeRefere()).isEqualTo(type);
        }
    }

    @Test
    void compute_allNaturesAccepted() {
        for (String nature : List.of("SALAIRES_NON_VERSES", "INDEMNITE_RUPTURE",
                "HEURES_SUPPLEMENTAIRES", "PRIMES", "CONGES_PAYES", "AUTRE")) {
            ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                    "PROVISION_SALAIRES", nature,
                    null, false, List.of(),
                    false, false, null, null,
                    clockAt(TODAY));
            assertThat(r.natureCreance()).isEqualTo(nature);
        }
    }

    @Test
    void compute_messagesNotEmpty() {
        ReferePrudhomalResult r = ReferePrudhomalCalculator.compute(
                "PROVISION_SALAIRES", "SALAIRES_NON_VERSES",
                MONTANT_5K, true,
                List.of("BULLETIN_PAIE", "RELANCE_EMPLOYEUR"),
                true, false, MED_RECENTE, 18,
                clockAt(TODAY));
        assertThat(r.messages()).isNotEmpty().hasSizeGreaterThan(5);
    }
}

package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-FA-25-01 : tests unitaires de MajeursProtegesCalculator
 * (art. 433-441 + 494-1 et s. Cciv).
 *
 * <p>Couvre les contributions du score, les seuils de verdict, l'arbre
 * de décision pour le régime optimal recommandé, le délai prévisionnel,
 * l'audition obligatoire art. 432, l'expertise psy, la base juridique
 * et les erreurs de validation.
 */
class MajeursProtegesCalculatorTest {

    private static final LocalDate DATE_CERT = LocalDate.of(2026, 4, 15);

    @Test
    void compute_nominal_habilitation_familiale_score90_elevee() {
        // certificat (+30) + mentale (+25) + consentement & habilitation (+20)
        // + ENFANT_MAJEUR (+15) + patrimoine (+10) = 100 plafonné
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false);

        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("ELEVEE");
        assertThat(r.regimeOptimalRecommande()).isEqualTo("HABILITATION_FAMILIALE");
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(4);
        assertThat(r.auditionPersonneObligatoire()).isTrue();
        assertThat(r.baseJuridique()).contains("433-441").contains("494-1");
    }

    @Test
    void compute_sauvegarde_urgence_patrimoniale() {
        // urgence + altération mentale → SAUVEGARDE_JUSTICE recommandée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "SAUVEGARDE_JUSTICE",
                true, false,
                true, DATE_CERT,
                false,
                "CONJOINT",
                List.of("GESTION_PATRIMOINE"),
                true, true, false);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("SAUVEGARDE_JUSTICE");
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(4);
    }

    @Test
    void compute_curatelle_renforcee_sansConsentement() {
        // gestion patrimoine + patrimoine significatif + sans consentement
        // → CURATELLE_RENFORCEE recommandée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_RENFORCEE");
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(8);
    }

    @Test
    void compute_tutelle_isolementSocial() {
        // altération mentale + isolement + sans consentement → TUTELLE
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "FRERE_SOEUR",
                List.of(),
                false, false, true);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("TUTELLE");
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(8);
    }

    @Test
    void compute_fallback_regimeDemande_curatelleSimple() {
        // aucune branche de l'arbre ne matche → fallback
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                false, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of(),
                false, false, false);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_SIMPLE");
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(8);
    }

    @Test
    void compute_score_plafonne_100() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, true,
                true, DATE_CERT,
                true,
                "PARENT",
                List.of("GESTION_PATRIMOINE"),
                false, true, false);

        assertThat(r.scoreEligibilite()).isLessThanOrEqualTo(100);
        // 30 + 25 + 20 + 15 + 10 = 100
        assertThat(r.scoreEligibilite()).isEqualTo(100);
    }

    @Test
    void compute_score_plancher_0() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                false, false,
                false, null,
                false,
                "TIERS_PROCHE",
                List.of(),
                false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(0);
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_verdict_elevee_seuil75() {
        // certificat (+30) + mentale (+25) + consentement habilitation (+20) = 75
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "TIERS_PROCHE",
                List.of(),
                false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(75);
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_verdict_moyenne_50to74() {
        // certificat (+30) + mentale (+25) = 55 → MOYENNE
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of(),
                false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(55);
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_verdict_faible_inferieur50() {
        // mentale (+25) + famille proche (+15) = 40 → FAIBLE
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                false, null,
                false,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(40);
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_consentementSansHabilitation_pasDeBonus20() {
        // consentement présent mais régime ≠ habilitation → bonus +20 ne s'applique pas
        // certificat (+30) + mentale (+25) + ENFANT_MAJEUR (+15) = 70
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(70);
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_demandeurNonProche_pasDeBonus15() {
        // demandeur TIERS_PROCHE → pas de bonus famille proche
        // certificat (+30) + mentale (+25) = 55
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of(),
                false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(55);
    }

    @Test
    void compute_audition_toujoursObligatoire_art432() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.auditionPersonneObligatoire()).isTrue();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("432"));
    }

    @Test
    void compute_expertisePsy_recommandee_alterationMixte() {
        // mentale ET physique → altération non claire → expertise recommandée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, true,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.expertisePsyComplementaireRecommandee()).isTrue();
    }

    @Test
    void compute_expertisePsy_recommandee_acteIrreversible_sante() {
        // acte santé → irréversible → expertise recommandée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false);

        assertThat(r.expertisePsyComplementaireRecommandee()).isTrue();
    }

    @Test
    void compute_expertisePsy_recommandee_pasDeCertificat() {
        // pas de certificat → besoin objectif → expertise recommandée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                false, null,
                false,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.expertisePsyComplementaireRecommandee()).isTrue();
    }

    @Test
    void compute_expertisePsy_nonRecommandee_certificatEtAlterationClaire() {
        // certificat OK, altération mentale claire (pas physique), pas d'acte
        // irréversible → expertise non recommandée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false);

        assertThat(r.expertisePsyComplementaireRecommandee()).isFalse();
    }

    @Test
    void compute_baseJuridique_contient433_et_494_1() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "SAUVEGARDE_JUSTICE",
                true, false,
                true, DATE_CERT,
                false,
                "CONJOINT",
                List.of(),
                true, false, false);

        assertThat(r.baseJuridique()).contains("433-441").contains("494-1");
    }

    @Test
    void compute_messages_certificatManquant_avertit() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                false, null,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Certificat médical").contains("431"));
    }

    @Test
    void compute_messages_habilitationRecommandee_contientReference() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("HABILITATION FAMILIALE"));
    }

    @Test
    void compute_formule_contient_score_verdict_regime() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.formule()).contains("Score").contains("ELEVEE")
                .contains("HABILITATION_FAMILIALE").contains("4 mois");
    }

    @Test
    void compute_actes_dedupliques() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false);

        assertThat(r.actesEnvisages())
                .containsExactly("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT");
    }

    @Test
    void compute_regimeInvalide_throws() {
        assertThatThrownBy(() -> MajeursProtegesCalculator.compute(
                "INVALIDE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regimeProtectionDemande");
    }

    @Test
    void compute_regimeNull_throws() {
        assertThatThrownBy(() -> MajeursProtegesCalculator.compute(
                null,
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regimeProtectionDemande");
    }

    @Test
    void compute_demandeurInvalide_throws() {
        assertThatThrownBy(() -> MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "AMI",
                List.of(),
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("demandeurFamilial");
    }

    @Test
    void compute_actesInvalides_throws() {
        assertThatThrownBy(() -> MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of("INVALIDE"),
                false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actesEnvisages");
    }

    @Test
    void compute_actesNullOuVide_acceptes() {
        MajeursProtegesResult r1 = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                null,
                false, false, false);
        assertThat(r1.actesEnvisages()).isEmpty();

        MajeursProtegesResult r2 = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);
        assertThat(r2.actesEnvisages()).isEmpty();
    }

    @Test
    void compute_urgenceSansAlteration_pasSauvegarde() {
        // urgence sans altération → pas de sauvegarde, fallback sur regime demande
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                false, false,
                true, DATE_CERT,
                false,
                "CONJOINT",
                List.of(),
                true, false, false);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("MANDAT_PROTECTION_FUTURE");
    }

    @Test
    void compute_habilitation_consentementOblige() {
        // sans consentement, l'habilitation NE peut PAS être recommandée
        // (fallback sur régime demandé MANDAT_PROTECTION_FUTURE)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of(),
                false, false, false);

        assertThat(r.regimeOptimalRecommande())
                .isNotEqualTo("HABILITATION_FAMILIALE");
    }

    @Test
    void compute_ministerePublic_pasFamilleProche() {
        // ministère public → pas de bonus famille proche
        // certificat (+30) + mentale (+25) = 55
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "MINISTERE_PUBLIC",
                List.of(),
                false, false, false);

        assertThat(r.scoreEligibilite()).isEqualTo(55);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Ministère Public"));
    }
}

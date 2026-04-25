package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-FA-25-05 : tests unitaires pour le régime MANDAT_PROTECTION_FUTURE
 * (art. 477-494 Cciv) — clôture F-FA-25 6/6 régimes.
 *
 * <p>Couvre l'analyse d'éligibilité (eligible + criteresNonRemplis), la
 * priorité de l'arbre de décision (mandat prioritaire sur tutelle/curatelle
 * sauf urgence + sous seing privé), les critères pivots
 * mandatPrealableSigne et formeMandatProtection (NOTARIE / SOUS_SEING_PRIVE),
 * les messages spécifiques mandat (art. 477, art. 489, art. 492, art. 493),
 * et la backward compatibility de l'API (13-args, 12-args, 11-args restent
 * valides).
 */
class MajeursProtegesCalculatorMandatProtectionTest {

    private static final LocalDate DATE_CERT = LocalDate.of(2026, 4, 15);

    // -------------------------------------------------------------------------
    // MANDAT — cas nominal ELEVEE
    // -------------------------------------------------------------------------

    @Test
    void mandat_recommandee_ELEVEE_notarie() {
        // tous critères mandat remplis + forme NOTARIE → éligible ELEVEE
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false,
                false /* incap */,
                false /* altGrave */,
                true /* mandatPrealableSigne */,
                "NOTARIE");

        assertThat(r.regimeOptimalRecommande()).isEqualTo("MANDAT_PROTECTION_FUTURE");
        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("ELEVEE");
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(4);
    }

    @Test
    void mandat_recommandee_ELEVEE_sousSeingPrive_actesNonGraves() {
        // sous seing privé sans acte grave → éligible
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "CONJOINT",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "SOUS_SEING_PRIVE");

        assertThat(r.regimeOptimalRecommande()).isEqualTo("MANDAT_PROTECTION_FUTURE");
        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("ELEVEE");
    }

    // -------------------------------------------------------------------------
    // MANDAT — cas dégradés (eligible = false)
    // -------------------------------------------------------------------------

    @Test
    void mandat_eligible_false_mandatPrealableAbsent() {
        // sans mandat préalable → non éligible (autre régime recommandé)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false,
                false,
                false,
                false /* mandatPrealableSigne = false */,
                "NOTARIE");

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Mandat préalable").contains("477"));
        assertThat(r.regimeOptimalRecommande()).isNotEqualTo("MANDAT_PROTECTION_FUTURE");
    }

    @Test
    void mandat_eligible_false_certManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                false /* sans cert */, null,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Certificat médical").contains("431"));
    }

    @Test
    void mandat_eligible_false_alterationAbsente() {
        // sans altération → non éligible
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                false, false /* aucune altération */,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Altération").contains("425"));
    }

    @Test
    void mandat_MOYENNE_sousSeingPrive_actesGraves() {
        // sous seing privé pour acte grave (GESTION_PATRIMOINE) → MOYENNE
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false,
                false,
                false,
                true,
                "SOUS_SEING_PRIVE");

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Forme notariée").contains("493"));
        assertThat(r.verdictAcceptabiliteJaf()).isEqualTo("MOYENNE");
    }

    // -------------------------------------------------------------------------
    // Priorité MANDAT vs TUTELLE / autre régime
    // -------------------------------------------------------------------------

    @Test
    void mandat_priorite_surTutelle() {
        // même contexte, seule différence = mandatPrealableSigne
        // sans mandat → tutelle (altération + cert + altGrave + incap + 2 cat)
        MajeursProtegesResult sansMandat = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true,
                false /* sans mandat */,
                null);
        // avec mandat → MANDAT prioritaire (subsidiarité art. 428)
        MajeursProtegesResult avecMandat = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true,
                true /* mandatPrealableSigne */,
                "NOTARIE");

        assertThat(sansMandat.regimeOptimalRecommande()).isEqualTo("TUTELLE");
        assertThat(avecMandat.regimeOptimalRecommande()).isEqualTo("MANDAT_PROTECTION_FUTURE");
    }

    @Test
    void mandat_criteresNonRemplis_listeExplicite() {
        // sans mandat préalable + sans cert → 2 critères manquants
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                false, null,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                false,
                "NOTARIE");

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Mandat préalable"));
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Certificat médical"));
    }

    // -------------------------------------------------------------------------
    // Messages
    // -------------------------------------------------------------------------

    @Test
    void mandat_msg_contient_477() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        // message évoque art. 477-494 (entrée principale du régime)
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("MANDAT").contains("477"));
    }

    @Test
    void mandat_msg_contient_489_si_notarie() {
        // mandat notarié → message évoque art. 489
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("NOTARI").contains("489"));
    }

    @Test
    void mandat_propage_dansResult() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.mandatPrealableSigne()).isTrue();
        assertThat(r.formeMandatProtection()).isEqualTo("NOTARIE");
    }

    @Test
    void mandat_null_traiteCommeFalse() {
        // surcharge 13-args (sans mandat) → mandatPrealableSigne = false
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true,
                true);

        assertThat(r.mandatPrealableSigne()).isFalse();
        assertThat(r.formeMandatProtection()).isNull();
        // sans mandat, le régime recommandé reste TUTELLE
        assertThat(r.regimeOptimalRecommande()).isEqualTo("TUTELLE");
    }

    @Test
    void forme_null_traiteCommeNonRenseigne() {
        // mandatPrealableSigne = true mais formeMandatProtection = null
        // → critère "Forme du mandat requise" manquant
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true /* mandat préalable */,
                null /* forme non renseignée */);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Forme du mandat"));
        assertThat(r.formeMandatProtection()).isNull();
    }

    @Test
    void arbreDecision_mandat_existant_remplaceToutAutreRegime() {
        // mandat préalable + altération + cert → MANDAT prioritaire (sauf
        // urgence + sous seing privé)
        // contexte qui aurait normalement basculé en HABILITATION
        // (consentement + famille proche + altération mentale)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "HABILITATION_FAMILIALE",
                true, false,
                true, DATE_CERT,
                true /* consentement */,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_LOGEMENT"),
                false, false, false,
                false,
                false,
                true /* mandat préalable */,
                "NOTARIE");

        assertThat(r.regimeOptimalRecommande()).isEqualTo("MANDAT_PROTECTION_FUTURE");
    }

    @Test
    void mandat_eligible_dérivéDeListe() {
        // eligible = (criteresNonRemplis.isEmpty())
        MajeursProtegesResult ok = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");
        assertThat(ok.eligible()).isTrue();
        assertThat(ok.criteresNonRemplis()).isEmpty();

        MajeursProtegesResult ko = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false,
                false,
                true,
                "SOUS_SEING_PRIVE" /* inadapté pour GESTION_PATRIMOINE */);
        assertThat(ko.eligible()).isFalse();
        assertThat(ko.criteresNonRemplis()).isNotEmpty();
    }

    @Test
    void mandat_dureeProcedure_courte() {
        // mandat → DELAI_COURT_MOIS = 4 (pas d'audience JAF)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.delaiProcedureMoisPrevisionnel())
                .isEqualTo(MajeursProtegesCalculator.DELAI_COURT_MOIS);
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(4);
    }

    @Test
    void backwardCompat_surcharge13Args_traitéCommeFalse() {
        // signature SF-FA-25-04 (13-args) → mandatPrealableSigne = false
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE", "DECISIONS_LOGEMENT"),
                false, false, false,
                true /* incap */,
                true /* altGrave */);

        assertThat(r.mandatPrealableSigne()).isFalse();
        assertThat(r.formeMandatProtection()).isNull();
        assertThat(r.regimeOptimalRecommande()).isEqualTo("TUTELLE");
    }

    @Test
    void mandat_priorite_sauf_urgence_sousSeingPrive() {
        // urgence + sous seing privé → SAUVEGARDE prioritaire (mesure
        // provisoire conservatoire en attendant l'activation effective du
        // mandat sous seing privé limité à la gestion patrimoniale)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                true /* urgence */, false, false,
                false,
                false,
                true,
                "SOUS_SEING_PRIVE");

        assertThat(r.regimeOptimalRecommande()).isEqualTo("SAUVEGARDE_JUSTICE");
    }

    @Test
    void mandat_priorite_urgence_avec_notarie_reste_mandat() {
        // urgence + NOTARIE → mandat reste prioritaire (force exécutoire)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                true /* urgence */, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.regimeOptimalRecommande()).isEqualTo("MANDAT_PROTECTION_FUTURE");
    }

    @Test
    void formeMandat_invalide_400() {
        try {
            MajeursProtegesCalculator.compute(
                    "MANDAT_PROTECTION_FUTURE",
                    true, false,
                    true, DATE_CERT,
                    false,
                    "ENFANT_MAJEUR",
                    List.of("DECISIONS_SANTE"),
                    false, false, false,
                    false,
                    false,
                    true,
                    "INVALIDE");
            assertThat(false).as("doit lever IllegalArgumentException").isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("formeMandatProtection invalide");
        }
    }

    @Test
    void mandat_msg_pivot_signe_present() {
        // si mandatPrealableSigne = true → message global évoque la
        // subsidiarité art. 428
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Mandat préalable").contains("477"));
    }

    @Test
    void mandat_formule_eligibleOui() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "NOTARIE");

        assertThat(r.formule()).contains("Éligibilité : OUI");
    }

    @Test
    void mandat_formule_eligibleNon_si_critereManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false,
                false,
                true,
                "SOUS_SEING_PRIVE");

        assertThat(r.formule()).contains("Éligibilité : NON");
    }

    @Test
    void mandat_msg_492_si_sousSeingPrive() {
        // mandat sous seing privé → message évoque art. 492
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "MANDAT_PROTECTION_FUTURE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false,
                false,
                true,
                "SOUS_SEING_PRIVE");

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("SOUS SEING PRIV").contains("492"));
    }

    @Test
    void mandat_personne_sous_curatelle_renforcee_sansMandat_pasMandatRecommande() {
        // sans mandat préalable, contexte curatelle renforcée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true /* incap */,
                false,
                false /* sans mandat */,
                null);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_RENFORCEE");
    }
}

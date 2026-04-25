package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-FA-25-03 : tests unitaires pour les régimes CURATELLE_SIMPLE
 * (art. 440 al. 1 Cciv) et CURATELLE_RENFORCEE (art. 472 Cciv).
 *
 * <p>Couvre l'analyse d'éligibilité (eligible + criteresNonRemplis), la
 * priorité de l'arbre de décision (sauvegarde/habilitation conservés en
 * priorité), le critère pivot incapaciteGestionQuotidienne, les messages
 * spécifiques curatelle, et la backward compatibility de l'API.
 */
class MajeursProtegesCalculatorCuratelleTest {

    private static final LocalDate DATE_CERT = LocalDate.of(2026, 4, 15);

    // -------------------------------------------------------------------------
    // CURATELLE_SIMPLE — art. 440 al. 1 Cciv
    // -------------------------------------------------------------------------

    @Test
    void curatelleSimple_recommandee_eligibleELEVEE() {
        // altération + cert + acte patrimoine + sans incap quotidienne
        // + sans urgence + sans isolement + sans consentement → CURATELLE_SIMPLE
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false /* incapaciteGestionQuotidienne */);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_SIMPLE");
        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(8);
    }

    @Test
    void curatelleSimple_eligibleFalse_certManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                false, null,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Certificat médical").contains("431"));
    }

    @Test
    void curatelleSimple_eligibleFalse_alterationAbsente() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                false, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Altération").contains("425"));
    }

    @Test
    void curatelleSimple_eligibleFalse_aucunActePatrimonialImportant() {
        // DECISIONS_SANTE n'est pas un acte patrimonial important
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("DECISIONS_SANTE"),
                false, false, false,
                false);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("acte patrimonial").contains("440 al. 1"));
    }

    @Test
    void curatelleSimple_eligibleFalse_incapacitePresente_basculerRenforcee() {
        // si la personne ne peut pas gérer son quotidien → simple inadaptée
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true /* incapacite quotidienne */);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("renforcée").contains("472"));
    }

    @Test
    void curatelleSimple_logementSeulCompteCommeActePatrimonial() {
        // DECISIONS_LOGEMENT seul suffit pour curatelle simple
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of("DECISIONS_LOGEMENT"),
                false, false, false,
                false);

        assertThat(r.eligible()).isTrue();
        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_SIMPLE");
    }

    @Test
    void curatelleSimple_messageContient440() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("CURATELLE SIMPLE").contains("440 al. 1"));
    }

    // -------------------------------------------------------------------------
    // CURATELLE_RENFORCEE — art. 472 Cciv
    // -------------------------------------------------------------------------

    @Test
    void curatelleRenforcee_recommandee_eligibleELEVEE() {
        // critère pivot incapaciteGestionQuotidienne + altération + cert
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, true, false,
                true /* incap quotidienne = critère pivot */);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_RENFORCEE");
        assertThat(r.eligible()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.delaiProcedureMoisPrevisionnel()).isEqualTo(8);
    }

    @Test
    void curatelleRenforcee_eligibleFalse_incapAbsente() {
        // sans incapacité quotidienne → renforcée non éligible (mais simple si autres critères ok)
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false /* PAS d'incapacite */);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Incapacité").contains("472"));
    }

    @Test
    void curatelleRenforcee_prioritairesSurSimple_quandIncapPresente() {
        // même contexte, seule différence = incapaciteGestionQuotidienne
        // sans incap → curatelle simple recommandée si conditions OK
        MajeursProtegesResult sansIncap = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);
        // avec incap → curatelle renforcée recommandée
        MajeursProtegesResult avecIncap = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true);

        assertThat(sansIncap.regimeOptimalRecommande()).isEqualTo("CURATELLE_SIMPLE");
        assertThat(avecIncap.regimeOptimalRecommande()).isEqualTo("CURATELLE_RENFORCEE");
    }

    @Test
    void curatelleRenforcee_messageContient472() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("CURATELLE RENFORCÉE").contains("472"));
    }

    // -------------------------------------------------------------------------
    // Eligibilité — comportement transversal
    // -------------------------------------------------------------------------

    @Test
    void eligible_deriveDeListeCriteresNonRemplis() {
        // verdict ELEVEE possible mais eligible peut être false si régime
        // demandé a critère manquant
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false /* incap manquant → critère pas rempli */);

        assertThat(r.eligible()).isFalse();
        assertThat(r.criteresNonRemplis()).isNotEmpty();
        // mais la liste ne contient PAS l'altération (qui est OK)
        assertThat(r.criteresNonRemplis()).noneSatisfy(c ->
                assertThat(c).contains("Altération"));
    }

    @Test
    void incapaciteGestionQuotidienne_propageeDansResult() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true);

        assertThat(r.incapaciteGestionQuotidienne()).isTrue();
    }

    @Test
    void incapaciteGestionQuotidienneFalse_paselligibleRenforcee_messageExplique() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);

        // message global liste explicitement les critères manquants
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("NON REMPLIS").contains("CURATELLE_RENFORCEE"));
    }

    // -------------------------------------------------------------------------
    // Priorité arbre de décision — non régression
    // -------------------------------------------------------------------------

    @Test
    void prioriteArbre_sauvegardeUrgenceTopPriority() {
        // urgence + altération → SAUVEGARDE même si conditions curatelle OK
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                true /* urgence */, false, false,
                true);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("SAUVEGARDE_JUSTICE");
    }

    @Test
    void prioriteArbre_habilitationFamillePrioritaireSurCuratelle() {
        // famille proche + consentement + alteration mentale + non urgent
        // → habilitation, même si actes patrimoniaux et incap quotidienne
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                true /* consentement */,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                true);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("HABILITATION_FAMILIALE");
    }

    @Test
    void prioriteArbre_tutelleIsolementSocial() {
        // altération + isolement + sans consentement → TUTELLE (avant curatelle)
        // si pas d'acte patrimonial ni incap quotidienne
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "TUTELLE",
                true, false,
                true, DATE_CERT,
                false,
                "FRERE_SOEUR",
                List.of(),
                false, false, true /* isolement */,
                false);

        assertThat(r.regimeOptimalRecommande()).isEqualTo("TUTELLE");
    }

    // -------------------------------------------------------------------------
    // Backward compatibility — surcharge sans incap quotidienne
    // -------------------------------------------------------------------------

    @Test
    void backwardCompat_surchargeSansIncap_traitéeCommeFalse() {
        // signature 11-args (sans incap) → traitée comme false → simple si conditions OK
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "TIERS_PROCHE",
                List.of("GESTION_PATRIMOINE"),
                false, false, false);

        assertThat(r.incapaciteGestionQuotidienne()).isFalse();
        assertThat(r.regimeOptimalRecommande()).isEqualTo("CURATELLE_SIMPLE");
        assertThat(r.eligible()).isTrue();
    }

    @Test
    void formule_contient_eligibleOuiSiAucunCritereManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_SIMPLE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false);

        assertThat(r.formule()).contains("Éligibilité : OUI");
    }

    @Test
    void formule_contient_eligibleNonSiCritereManquant() {
        MajeursProtegesResult r = MajeursProtegesCalculator.compute(
                "CURATELLE_RENFORCEE",
                true, false,
                true, DATE_CERT,
                false,
                "ENFANT_MAJEUR",
                List.of("GESTION_PATRIMOINE"),
                false, false, false,
                false /* incap manque */);

        assertThat(r.formule()).contains("Éligibilité : NON");
    }
}

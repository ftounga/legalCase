package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-06 : tests unitaires du {@link TransactionBeTravailValidator}.
 *
 * <p>Vérifie la matrice de verdict (4 états × ordre d'évaluation), le
 * calcul du ratio (HALF_UP 1 décimale), l'avertissement de lésion
 * (&lt; 50 %), la construction de la checklist renonciations (stratégie
 * V1 conservatrice : tous valides ou tous invalides selon le flag
 * {@code renonciationOrdrePublicDetectee}) et la validation des inputs.</p>
 */
class TransactionBeTravailValidatorTest {

    private static TransactionBeTravailRequest req(
            BigDecimal montant,
            BigDecimal indemnite,
            boolean concessions,
            List<String> renonciations,
            boolean ordrePublic,
            boolean mentionContestation) {
        return new TransactionBeTravailRequest(
                montant, indemnite, concessions,
                renonciations == null ? List.of() : renonciations,
                ordrePublic, mentionContestation);
    }

    // ── Matrice de verdict ──────────────────────────────────────────────────

    @Test
    void validate_absenceConcessions_returnsINVALIDE_ABSENCE_CONCESSIONS() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("12000.00"),
                        false,
                        List.of("Renonciation à l'action en indemnité de rupture"),
                        false, true));

        assertThat(r.verdict()).isEqualTo(TransactionBeTravailVerdict.INVALIDE);
        assertThat(r.raisonInvalidite())
                .isEqualTo(TransactionBeTravailRaisonInvalidite.ABSENCE_CONCESSIONS);
    }

    @Test
    void validate_renonciationOrdrePublic_returnsINVALIDE_PARTIELLE_ordrePublic() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("12000.00"),
                        true,
                        List.of("Renonciation à tous droits issus de la CCT impérative"),
                        true, true));

        assertThat(r.verdict()).isEqualTo(TransactionBeTravailVerdict.INVALIDE_PARTIELLE);
        assertThat(r.raisonInvalidite())
                .isEqualTo(TransactionBeTravailRaisonInvalidite.RENONCIATION_ORDRE_PUBLIC);
        // Stratégie conservatrice V1 : tous les items invalides si ordre public détecté.
        assertThat(r.checklistRenonciations())
                .allMatch(item -> !item.valide());
    }

    @Test
    void validate_mentionContestationAbsente_returnsACOMPLETER_LITIGE_NON_VISE() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("12000.00"),
                        true,
                        List.of("Renonciation à l'action en indemnité de rupture"),
                        false, false));

        assertThat(r.verdict()).isEqualTo(TransactionBeTravailVerdict.A_COMPLETER);
        assertThat(r.raisonInvalidite())
                .isEqualTo(TransactionBeTravailRaisonInvalidite.LITIGE_NON_VISE);
    }

    @Test
    void validate_renonciationsListeesVide_returnsACOMPLETER_LITIGE_NON_VISE() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("12000.00"),
                        true,
                        List.of(), // checklist vide
                        false, true));

        assertThat(r.verdict()).isEqualTo(TransactionBeTravailVerdict.A_COMPLETER);
        assertThat(r.raisonInvalidite())
                .isEqualTo(TransactionBeTravailRaisonInvalidite.LITIGE_NON_VISE);
        assertThat(r.checklistRenonciations()).isEmpty();
    }

    @Test
    void validate_toutesConditionsOK_returnsVALIDE_raisonNull() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("12000.00"),
                        true,
                        List.of("Renonciation à l'action en indemnité de rupture",
                                "Renonciation à l'action CCT 109"),
                        false, true));

        assertThat(r.verdict()).isEqualTo(TransactionBeTravailVerdict.VALIDE);
        assertThat(r.raisonInvalidite()).isNull();
        assertThat(r.checklistRenonciations()).hasSize(2);
        assertThat(r.checklistRenonciations()).allMatch(TransactionBeTravailChecklistItem::valide);
    }

    // ── Ordre d'évaluation : concessions absorbe le reste ──────────────────

    @Test
    void validate_absenceConcessionsPriorityOverAutres_returnsINVALIDE() {
        // concessions absentes + ordre public détecté + contestation absente
        // → seul ABSENCE_CONCESSIONS doit ressortir (priorité art. 2044 Cciv).
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("12000.00"),
                        false, List.of(), true, false));

        assertThat(r.verdict()).isEqualTo(TransactionBeTravailVerdict.INVALIDE);
        assertThat(r.raisonInvalidite())
                .isEqualTo(TransactionBeTravailRaisonInvalidite.ABSENCE_CONCESSIONS);
    }

    // ── Ratio et avertissement de lésion ───────────────────────────────────

    @Test
    void validate_ratio45percent_avertissementPresent() {
        // 4500 / 10000 × 100 = 45.0 % → ratio < 50 % → avertissement.
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("4500.00"), new BigDecimal("10000.00"),
                        true,
                        List.of("Renonciation à l'action en indemnité de rupture"),
                        false, true));

        assertThat(r.ratioPourcentage()).isEqualByComparingTo("45.0");
        assertThat(r.avertissement())
                .isNotNull()
                .contains("lésion")
                .contains("50 %")
                .contains("45,0");
    }

    @Test
    void validate_ratio80percent_avertissementNull() {
        // 8000 / 10000 × 100 = 80.0 % → ratio >= 50 % → pas d'avertissement.
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("8000.00"), new BigDecimal("10000.00"),
                        true,
                        List.of("Renonciation à l'action en indemnité de rupture"),
                        false, true));

        assertThat(r.ratioPourcentage()).isEqualByComparingTo("80.0");
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void validate_ratioExactement50percent_avertissementNull_borneInclusive() {
        // 5000 / 10000 × 100 = 50.0 % → exactement seuil → pas d'avertissement.
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("5000.00"), new BigDecimal("10000.00"),
                        true,
                        List.of("Renonciation à l'action en indemnité de rupture"),
                        false, true));

        assertThat(r.ratioPourcentage()).isEqualByComparingTo("50.0");
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void validate_ratioNull_siIndemniteAbsente() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), null,
                        true,
                        List.of("Renonciation à l'action en indemnité de rupture"),
                        false, true));

        assertThat(r.ratioPourcentage()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void validate_ratioNull_siIndemniteZero() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), BigDecimal.ZERO,
                        true,
                        List.of("Renonciation à l'action en indemnité de rupture"),
                        false, true));

        assertThat(r.ratioPourcentage()).isNull();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void validate_ratio_arrondiHALF_UP_1decimale() {
        // 3333 / 10000 × 100 = 33.33 → HALF_UP 1 déc = 33.3
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("3333.00"), new BigDecimal("10000.00"),
                        true,
                        List.of("Renonciation"),
                        false, true));

        assertThat(r.ratioPourcentage()).isEqualByComparingTo("33.3");
    }

    // ── Checklist renonciations — stratégie V1 ─────────────────────────────

    @Test
    void validate_checklist_tousValides_siPasOrdrePublic() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("10000.00"),
                        true,
                        List.of("A", "B", "C"),
                        false, true));

        assertThat(r.checklistRenonciations())
                .hasSize(3)
                .extracting(TransactionBeTravailChecklistItem::valide)
                .containsOnly(true);
        assertThat(r.checklistRenonciations())
                .extracting(TransactionBeTravailChecklistItem::item)
                .containsExactly("A", "B", "C");
    }

    @Test
    void validate_checklist_tousInvalides_siOrdrePublicDetecte() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("10000.00"),
                        true,
                        List.of("A", "B"),
                        true, true));

        assertThat(r.checklistRenonciations())
                .hasSize(2)
                .extracting(TransactionBeTravailChecklistItem::valide)
                .containsOnly(false);
    }

    // ── Base juridique ─────────────────────────────────────────────────────

    @Test
    void validate_baseJuridique_referenceArt2044Cciv_etLoi03_07_1978() {
        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), null,
                        true,
                        List.of("Renonciation"),
                        false, true));

        assertThat(r.baseJuridique())
                .contains("Art. 2044")
                .contains("Code civil belge")
                .contains("03/07/1978")
                .contains("art. 6");
    }

    // ── Snapshot inputs/outputs ────────────────────────────────────────────

    @Test
    void validate_snapshot_inclutTousLesInputs() {
        BigDecimal montant = new BigDecimal("12500.00");
        BigDecimal indemnite = new BigDecimal("15000.00");
        List<String> renonciations = List.of("R1", "R2");

        TransactionBeTravailResult r = TransactionBeTravailValidator.validate(
                req(montant, indemnite, true, renonciations, false, true));

        assertThat(r.montantTransactionBrut()).isEqualByComparingTo(montant);
        assertThat(r.indemniteLegaleEtimee()).isEqualByComparingTo(indemnite);
        assertThat(r.concessionsEmployeurDescrites()).isTrue();
        assertThat(r.renonciationsListees()).containsExactly("R1", "R2");
        assertThat(r.renonciationOrdrePublicDetectee()).isFalse();
        assertThat(r.mentionContestation()).isTrue();
    }

    // ── Validation des inputs ──────────────────────────────────────────────

    @Test
    void validate_requestNull_throws() {
        assertThatThrownBy(() -> TransactionBeTravailValidator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void validate_montantNull_throws() {
        assertThatThrownBy(() -> TransactionBeTravailValidator.validate(
                req(null, new BigDecimal("10000.00"),
                        true, List.of("R"), false, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantTransactionBrut");
    }

    @Test
    void validate_montantZero_throws() {
        assertThatThrownBy(() -> TransactionBeTravailValidator.validate(
                req(BigDecimal.ZERO, new BigDecimal("10000.00"),
                        true, List.of("R"), false, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantTransactionBrut");
    }

    @Test
    void validate_montantNegatif_throws() {
        assertThatThrownBy(() -> TransactionBeTravailValidator.validate(
                req(new BigDecimal("-1"), new BigDecimal("10000.00"),
                        true, List.of("R"), false, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantTransactionBrut");
    }

    @Test
    void validate_renonciationsListeesNull_throws() {
        // Cas pathologique : la mini-spec exige une liste (vide possible mais non null).
        TransactionBeTravailRequest bad = new TransactionBeTravailRequest(
                new BigDecimal("10000.00"), null, true, null, false, true);
        assertThatThrownBy(() -> TransactionBeTravailValidator.validate(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("renonciationsListees");
    }

    @Test
    void validate_indemniteNegative_throws() {
        assertThatThrownBy(() -> TransactionBeTravailValidator.validate(
                req(new BigDecimal("10000.00"), new BigDecimal("-1"),
                        true, List.of("R"), false, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("indemniteLegaleEtimee");
    }
}

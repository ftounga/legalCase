package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-08 : tests unitaires du
 * {@link LicenciementBeProtectionDelegueeValidator}.
 *
 * <p>Vérifie : fenêtre de protection par statut (4 ans élus / 2 ans candidats
 * + CCT 5), verdict binaire, calcul de l'indemnité forfaitaire 2 ans
 * (4 ans aggravantes), délai de demande de réintégration 30 j depuis
 * dateLicenciement, validation des incohérences (refus sans demande).</p>
 */
class LicenciementBeProtectionDelegueeValidatorTest {

    private static final LocalDate DATE_ELECTION = LocalDate.of(2024, 5, 15);
    private static final LocalDate DATE_LIC_DANS_MANDAT = LocalDate.of(2026, 4, 10);
    private static final BigDecimal REM = new BigDecimal("40000.00");

    private static final Clock FIXED_TODAY = Clock.fixed(
            LocalDate.of(2026, 4, 20).atStartOfDay(ZoneId.of("UTC")).toInstant(),
            ZoneId.of("UTC"));

    private static LicenciementBeProtectionDelegueeRequest req(
            LicenciementBeStatutProtegeEnum statut,
            int anciennete,
            BigDecimal remuneration,
            LocalDate dateLicenciement,
            LocalDate dateElection,
            boolean demandeReinteg,
            boolean refusEmployeur,
            boolean aggravantes) {
        return new LicenciementBeProtectionDelegueeRequest(
                statut, anciennete, remuneration, dateLicenciement,
                dateElection, demandeReinteg, refusEmployeur, aggravantes);
    }

    // ── Verdict : licenciement dans la fenêtre de protection ───────────────

    @Test
    void analyze_delegueElu_licenciementDansMandat_returnsLicenciementInterdit() {
        // DELEGUE_SYNDICAL_ELU élu 15/05/2024, licencié 10/04/2026 → 4 ans
        // de protection (jusqu'au 15/05/2028) — dans la fenêtre.
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionDelegueeVerdict
                        .LICENCIEMENT_INTERDIT_SANS_PROCEDURE);
        assertThat(r.licenciementDansProtection()).isTrue();
        assertThat(r.dateFinProtection()).isEqualTo(LocalDate.of(2028, 5, 15));
    }

    // ── Indemnité 2 ans = 2 × rémunération ─────────────────────────────────

    @Test
    void analyze_dansProtection_indemnite2ans() {
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY);

        assertThat(r.anneesForfait()).isEqualTo(2);
        assertThat(r.indemniteForfaitaire())
                .isEqualByComparingTo(new BigDecimal("80000.00"));
    }

    // ── Circonstances aggravantes → 4 ans ──────────────────────────────────

    @Test
    void analyze_dansProtection_circonstancesAggravantes_indemnite4ans() {
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false, false, true),
                        FIXED_TODAY);

        assertThat(r.anneesForfait()).isEqualTo(4);
        assertThat(r.indemniteForfaitaire())
                .isEqualByComparingTo(new BigDecimal("160000.00"));
    }

    // ── Délai 30 j de demande de réintégration expiré ──────────────────────

    @Test
    void analyze_delai30jExpire_delaiDepasseTrue_avertissementPose() {
        // Licenciement le 10/02/2026, today = 20/04/2026 → 30 j passés
        Clock today = Clock.fixed(
                LocalDate.of(2026, 4, 20).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC"));
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM,
                                LocalDate.of(2026, 2, 10), DATE_ELECTION,
                                false, false, false),
                        today);

        assertThat(r.dateLimiteDemandeReintegration())
                .isEqualTo(LocalDate.of(2026, 3, 12));
        assertThat(r.joursRestantsReintegration()).isLessThan(0);
        assertThat(r.delaiReintegrationDepasse()).isTrue();
        assertThat(r.avertissement())
                .isNotNull()
                .contains("30 jours")
                .contains("tribunal du travail");
    }

    // ── Candidat non élu hors fenêtre 2 ans → HORS_PERIODE_PROTECTION ──────

    @Test
    void analyze_candidatNonElu_horsFenetreDe2ans_returnsHorsProtection() {
        // CANDIDAT_NON_ELU élu 15/05/2024, fenêtre = 2 ans (jusqu'au
        // 15/05/2026). Licencié le 01/08/2026 → hors fenêtre.
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.CANDIDAT_NON_ELU,
                                3, REM,
                                LocalDate.of(2026, 8, 1), DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionDelegueeVerdict
                        .HORS_PERIODE_PROTECTION);
        assertThat(r.licenciementDansProtection()).isFalse();
        assertThat(r.dateFinProtection()).isEqualTo(LocalDate.of(2026, 5, 15));
        // Pas d'indemnité forfaitaire hors fenêtre
        assertThat(r.indemniteForfaitaire()).isNull();
        assertThat(r.anneesForfait()).isNull();
    }

    // ── Cas frontière : dateLicenciement = dateFinProtection (borne incluse) ──

    @Test
    void analyze_licenciementExactementSurDateFinProtection_inclus_dansFenetre() {
        // Licencié exactement le 15/05/2028 (= dateElection + 4 ans)
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM,
                                LocalDate.of(2028, 5, 15), DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY);

        assertThat(r.licenciementDansProtection()).isTrue();
        assertThat(r.verdict())
                .isEqualTo(LicenciementBeProtectionDelegueeVerdict
                        .LICENCIEMENT_INTERDIT_SANS_PROCEDURE);
    }

    // ── DELEGUE_CCT5 : protection 2 ans ────────────────────────────────────

    @Test
    void analyze_delegueCCT5_fenetre2ans() {
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_CCT5,
                                3, REM,
                                LocalDate.of(2025, 6, 1), DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY);

        assertThat(r.dateFinProtection()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(r.licenciementDansProtection()).isTrue();
    }

    // ── CONSEILLER_CPPT : protection 4 ans ─────────────────────────────────

    @Test
    void analyze_conseillerCPPT_fenetre4ans() {
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.CONSEILLER_CPPT,
                                5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY);

        assertThat(r.dateFinProtection()).isEqualTo(LocalDate.of(2028, 5, 15));
        assertThat(r.licenciementDansProtection()).isTrue();
    }

    // ── Délai 30 j non expiré ──────────────────────────────────────────────

    @Test
    void analyze_delai30jNonExpire_pasAvertissement() {
        // Licencié 15/04/2026, today 20/04/2026 → reste 25 j
        Clock today = Clock.fixed(
                LocalDate.of(2026, 4, 20).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC"));
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM,
                                LocalDate.of(2026, 4, 15), DATE_ELECTION,
                                false, false, false),
                        today);

        assertThat(r.delaiReintegrationDepasse()).isFalse();
        assertThat(r.joursRestantsReintegration()).isEqualTo(25);
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_delai30jExactementZero_borneInclusive_depasse() {
        // dateLimite = dateLic + 30j ; si today == dateLimite, joursRestants=0
        // → delaiReintegrationDepasse = true (la borne 0 est traitée comme
        // expirée pour éviter toute action le jour J sans marge).
        Clock today = Clock.fixed(
                LocalDate.of(2026, 5, 15).atStartOfDay(ZoneId.of("UTC")).toInstant(),
                ZoneId.of("UTC"));
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM,
                                LocalDate.of(2026, 4, 15), DATE_ELECTION,
                                false, false, false),
                        today);

        assertThat(r.joursRestantsReintegration()).isEqualTo(0);
        assertThat(r.delaiReintegrationDepasse()).isTrue();
    }

    // ── Base juridique ─────────────────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceLoi19_03_1991_et_CCT5() {
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY);

        assertThat(r.baseJuridique())
                .contains("19/03/1991")
                .contains("CCT n° 5")
                .contains("24/05/1971");
    }

    // ── Snapshot inputs/outputs ────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.CONSEILLER_CPPT,
                                7, new BigDecimal("55000.00"),
                                DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                true, true, false),
                        FIXED_TODAY);

        assertThat(r.statutProtege())
                .isEqualTo(LicenciementBeStatutProtegeEnum.CONSEILLER_CPPT);
        assertThat(r.ancienneteAnnees()).isEqualTo(7);
        assertThat(r.remunerationAnnuelleBrute())
                .isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(r.dateLicenciement()).isEqualTo(DATE_LIC_DANS_MANDAT);
        assertThat(r.dateElectionOuMandat()).isEqualTo(DATE_ELECTION);
        assertThat(r.demandeReintegrationDansTrente()).isTrue();
        assertThat(r.employeurRefuseReintegration()).isTrue();
        assertThat(r.circonstancesAggravantes()).isFalse();
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_statutProtegeNull_throws() {
        LicenciementBeProtectionDelegueeRequest bad =
                new LicenciementBeProtectionDelegueeRequest(
                        null, 5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                        false, false, false);
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutProtege");
    }

    @Test
    void analyze_ancienneteNull_throws() {
        LicenciementBeProtectionDelegueeRequest bad =
                new LicenciementBeProtectionDelegueeRequest(
                        LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                        null, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                        false, false, false);
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteAnnees");
    }

    @Test
    void analyze_ancienneteNegative_throws() {
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                -1, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ancienneteAnnees");
    }

    @Test
    void analyze_remunerationNull_throws() {
        LicenciementBeProtectionDelegueeRequest bad =
                new LicenciementBeProtectionDelegueeRequest(
                        LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                        5, null, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                        false, false, false);
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationAnnuelleBrute");
    }

    @Test
    void analyze_remunerationZero_throws() {
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, BigDecimal.ZERO, DATE_LIC_DANS_MANDAT,
                                DATE_ELECTION, false, false, false),
                        FIXED_TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positive");
    }

    @Test
    void analyze_remunerationNegative_throws() {
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, new BigDecimal("-1000"),
                                DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false, false, false),
                        FIXED_TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positive");
    }

    @Test
    void analyze_dateLicenciementNull_throws() {
        LicenciementBeProtectionDelegueeRequest bad =
                new LicenciementBeProtectionDelegueeRequest(
                        LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                        5, REM, null, DATE_ELECTION, false, false, false);
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateLicenciement");
    }

    @Test
    void analyze_dateElectionNull_throws() {
        LicenciementBeProtectionDelegueeRequest bad =
                new LicenciementBeProtectionDelegueeRequest(
                        LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                        5, REM, DATE_LIC_DANS_MANDAT, null,
                        false, false, false);
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateElectionOuMandat");
    }

    @Test
    void analyze_refusEmployeurSansDemande_throws() {
        // Cohérence procédure : refus de réintégration ⇒ demande préalable
        assertThatThrownBy(() ->
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                false /* demandeReinteg */,
                                true /* refusEmployeur */,
                                false),
                        FIXED_TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employeurRefuseReintegration")
                .hasMessageContaining("demandeReintegrationDansTrente");
    }

    @Test
    void analyze_refusEmployeurAvecDemande_passe() {
        LicenciementBeProtectionDelegueeResult r =
                LicenciementBeProtectionDelegueeValidator.analyze(
                        req(LicenciementBeStatutProtegeEnum.DELEGUE_SYNDICAL_ELU,
                                5, REM, DATE_LIC_DANS_MANDAT, DATE_ELECTION,
                                true /* demandeReinteg */,
                                true /* refusEmployeur */,
                                false),
                        FIXED_TODAY);
        assertThat(r.employeurRefuseReintegration()).isTrue();
        assertThat(r.demandeReintegrationDansTrente()).isTrue();
    }
}

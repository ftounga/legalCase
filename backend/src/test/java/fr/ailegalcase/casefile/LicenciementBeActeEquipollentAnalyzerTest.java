package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-09 : tests unitaires du {@link LicenciementBeActeEquipollentAnalyzer}.
 *
 * <p>Vérifie la matrice de décision ampleur × élément essentiel, le calcul
 * du délai depuis modification (today − dateModification), la rétrogradation
 * du verdict en RISQUE_ACCEPTATION_TACITE si silence > 30 j, le calcul de
 * l'ICP indicatif et la validation des inputs.</p>
 */
class LicenciementBeActeEquipollentAnalyzerTest {

    private static final LocalDate DATE_MOD_RECENTE = LocalDate.of(2026, 4, 10);
    private static final BigDecimal REM_HEBDO = new BigDecimal("500.00");

    /** today = 20/04/2026 → DATE_MOD_RECENTE est à 10 j (< 30 j → pas de risque). */
    private static final Clock FIXED_TODAY = Clock.fixed(
            LocalDate.of(2026, 4, 20).atStartOfDay(ZoneId.of("UTC")).toInstant(),
            ZoneId.of("UTC"));

    private static LicenciementBeActeEquipollentRequest req(
            TypeModificationUnilateraleEnum type,
            AmpleurModificationEnum ampleur,
            boolean elementEssentiel,
            LocalDate dateModification,
            boolean salarieAProteste,
            Integer delaiDepuisModif,
            BigDecimal remHebdo,
            Integer dureePreavisSem) {
        return new LicenciementBeActeEquipollentRequest(
                type, ampleur, dateModification, elementEssentiel,
                salarieAProteste, delaiDepuisModif, remHebdo, dureePreavisSem);
    }

    // ── Matrice de décision ────────────────────────────────────────────────

    @Test
    void analyze_substantielleElementEssentiel_returnsActeEquipollentProbable() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null, null, null),
                        FIXED_TODAY);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeActeEquipollentVerdict.ACTE_EQUIPOLLENT_PROBABLE);
        assertThat(r.risqueAcceptationTacite()).isFalse();
        assertThat(r.avertissement()).isNull();
    }

    @Test
    void analyze_substantielleNonEssentiel_returnsAAnalyser() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.HORAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                false, DATE_MOD_RECENTE, true, null, null, null),
                        FIXED_TODAY);

        assertThat(r.verdict()).isEqualTo(LicenciementBeActeEquipollentVerdict.A_ANALYSER);
    }

    @Test
    void analyze_mineure_returnsPasActeEquipollent() {
        // Mineure → Ius Variandi quel que soit l'élément essentiel
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.HORAIRE,
                                AmpleurModificationEnum.MINEURE,
                                true, DATE_MOD_RECENTE, true, null, null, null),
                        FIXED_TODAY);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeActeEquipollentVerdict.PAS_ACTE_EQUIPOLLENT);
        assertThat(r.icpIndicatif()).isNull();
    }

    @Test
    void analyze_indeterminees_returnsAAnalyser() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.FONCTION,
                                AmpleurModificationEnum.INDETERMINEES,
                                false, DATE_MOD_RECENTE, true, null, null, null),
                        FIXED_TODAY);

        assertThat(r.verdict()).isEqualTo(LicenciementBeActeEquipollentVerdict.A_ANALYSER);
    }

    // ── Risque acceptation tacite ──────────────────────────────────────────

    @Test
    void analyze_silenceSuperieurA30j_retrogradeVerdictEnRisqueAcceptationTacite() {
        // 35 j sans protestation + substantielle + essentiel
        // → verdict initial ACTE_EQUIPOLLENT_PROBABLE
        // → rétrogradé RISQUE_ACCEPTATION_TACITE
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, false, 35, null, null),
                        FIXED_TODAY);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeActeEquipollentVerdict.RISQUE_ACCEPTATION_TACITE);
        assertThat(r.risqueAcceptationTacite()).isTrue();
        assertThat(r.avertissement())
                .isNotNull()
                .contains("30 jours")
                .contains("acceptation tacite");
    }

    @Test
    void analyze_silenceA30jExactement_pasDeRisque_borneStrictement() {
        // 30 j sans protestation → NON déclenché (strict > 30)
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, false, 30, null, null),
                        FIXED_TODAY);

        assertThat(r.risqueAcceptationTacite()).isFalse();
        assertThat(r.verdict())
                .isEqualTo(LicenciementBeActeEquipollentVerdict.ACTE_EQUIPOLLENT_PROBABLE);
    }

    @Test
    void analyze_silence35j_maisSalarieAProteste_pasDeRisque() {
        // Salarié a protesté → pas de risque même au-delà de 30 j
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, 35, null, null),
                        FIXED_TODAY);

        assertThat(r.risqueAcceptationTacite()).isFalse();
        assertThat(r.verdict())
                .isEqualTo(LicenciementBeActeEquipollentVerdict.ACTE_EQUIPOLLENT_PROBABLE);
    }

    @Test
    void analyze_risqueAcceptationTaciteAvecVerdictMineure_neRetrogradePas() {
        // Risque ne change pas le verdict si mineure (PAS_ACTE_EQUIPOLLENT
        // reste). Mais le flag risqueAcceptationTacite est posé.
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.HORAIRE,
                                AmpleurModificationEnum.MINEURE,
                                true, DATE_MOD_RECENTE, false, 35, null, null),
                        FIXED_TODAY);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeActeEquipollentVerdict.PAS_ACTE_EQUIPOLLENT);
        assertThat(r.risqueAcceptationTacite()).isTrue();
        assertThat(r.avertissement()).isNotNull();
    }

    // ── Calcul délai depuis modification ──────────────────────────────────

    @Test
    void analyze_delaiNonFourni_calculeDepuisDateModification() {
        // dateMod = 10/04/2026, today = 20/04/2026 → 10 j
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null, null, null),
                        FIXED_TODAY);

        assertThat(r.delaiDepuisModificationJours()).isEqualTo(10);
    }

    @Test
    void analyze_delaiFourni_utiliseLaValeurExplicite() {
        // Override la valeur calculée
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, 99, null, null),
                        FIXED_TODAY);

        assertThat(r.delaiDepuisModificationJours()).isEqualTo(99);
    }

    // ── ICP indicatif ──────────────────────────────────────────────────────

    @Test
    void analyze_acteProbable_avecRemEtDuree_calculeIcpIndicatif() {
        // 500 €/sem × 27 sem = 13 500 €
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null,
                                REM_HEBDO, 27),
                        FIXED_TODAY);

        assertThat(r.icpIndicatif())
                .isEqualByComparingTo(new BigDecimal("13500.00"));
    }

    @Test
    void analyze_acteProbableSansRem_pasIcpIndicatif() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null,
                                null, 27),
                        FIXED_TODAY);

        assertThat(r.icpIndicatif()).isNull();
    }

    @Test
    void analyze_acteProbableSansDureePreavis_pasIcpIndicatif() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null,
                                REM_HEBDO, null),
                        FIXED_TODAY);

        assertThat(r.icpIndicatif()).isNull();
    }

    @Test
    void analyze_verdictAAnalyser_pasIcpIndicatif() {
        // ICP n'est calculé QUE si ACTE_EQUIPOLLENT_PROBABLE
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.HORAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                false, DATE_MOD_RECENTE, true, null,
                                REM_HEBDO, 27),
                        FIXED_TODAY);

        assertThat(r.verdict()).isEqualTo(LicenciementBeActeEquipollentVerdict.A_ANALYSER);
        assertThat(r.icpIndicatif()).isNull();
    }

    @Test
    void analyze_verdictRisqueAcceptationTacite_pasIcpIndicatif() {
        // Verdict rétrogradé → plus d'ICP indicatif
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, false, 35,
                                REM_HEBDO, 27),
                        FIXED_TODAY);

        assertThat(r.verdict())
                .isEqualTo(LicenciementBeActeEquipollentVerdict.RISQUE_ACCEPTATION_TACITE);
        assertThat(r.icpIndicatif()).isNull();
    }

    // ── Délai recommandé + base juridique ─────────────────────────────────

    @Test
    void analyze_delaiRecommandeProtestation_toujours30jours() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.MINEURE,
                                false, DATE_MOD_RECENTE, true, null, null, null),
                        FIXED_TODAY);

        assertThat(r.delaiRecommandeProtestationJours()).isEqualTo(30);
    }

    @Test
    void analyze_baseJuridique_referenceLoi03_07_1978_etCassBE() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null, null, null),
                        FIXED_TODAY);

        assertThat(r.baseJuridique())
                .contains("03/07/1978")
                .contains("art. 20")
                .contains("Cass. BE");
        assertThat(r.fondementJuridique())
                .contains("03/07/1978")
                .contains("élément essentiel");
    }

    // ── Snapshot inputs/outputs ───────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.LIEU_TRAVAIL,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, 5,
                                new BigDecimal("750.50"), 18),
                        FIXED_TODAY);

        assertThat(r.typeModification())
                .isEqualTo(TypeModificationUnilateraleEnum.LIEU_TRAVAIL);
        assertThat(r.ampleurModification())
                .isEqualTo(AmpleurModificationEnum.SUBSTANTIELLE);
        assertThat(r.elementEssentielDuContrat()).isTrue();
        assertThat(r.dateModification()).isEqualTo(DATE_MOD_RECENTE);
        assertThat(r.salarieAProteste()).isTrue();
        assertThat(r.delaiDepuisModificationJours()).isEqualTo(5);
        assertThat(r.remunerationHebdomadaireBrute())
                .isEqualByComparingTo(new BigDecimal("750.50"));
        assertThat(r.dureePreavisCalculeeSemaines()).isEqualTo(18);
    }

    // ── Validation conditionnelle ─────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                LicenciementBeActeEquipollentAnalyzer.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_typeModificationNull_throws() {
        LicenciementBeActeEquipollentRequest bad =
                new LicenciementBeActeEquipollentRequest(
                        null, AmpleurModificationEnum.SUBSTANTIELLE,
                        DATE_MOD_RECENTE, true, false, null, null, null);
        assertThatThrownBy(() ->
                LicenciementBeActeEquipollentAnalyzer.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeModification");
    }

    @Test
    void analyze_ampleurModificationNull_throws() {
        LicenciementBeActeEquipollentRequest bad =
                new LicenciementBeActeEquipollentRequest(
                        TypeModificationUnilateraleEnum.SALAIRE, null,
                        DATE_MOD_RECENTE, true, false, null, null, null);
        assertThatThrownBy(() ->
                LicenciementBeActeEquipollentAnalyzer.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ampleurModification");
    }

    @Test
    void analyze_dateModificationNull_throws() {
        LicenciementBeActeEquipollentRequest bad =
                new LicenciementBeActeEquipollentRequest(
                        TypeModificationUnilateraleEnum.SALAIRE,
                        AmpleurModificationEnum.SUBSTANTIELLE,
                        null, true, false, null, null, null);
        assertThatThrownBy(() ->
                LicenciementBeActeEquipollentAnalyzer.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateModification");
    }

    @Test
    void analyze_delaiNegatif_throws() {
        assertThatThrownBy(() ->
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, -1, null, null),
                        FIXED_TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delaiDepuisModificationJours");
    }

    @Test
    void analyze_remunerationNegative_throws() {
        assertThatThrownBy(() ->
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null,
                                new BigDecimal("-100"), 27),
                        FIXED_TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remunerationHebdomadaireBrute");
    }

    @Test
    void analyze_dureePreavisNegative_throws() {
        assertThatThrownBy(() ->
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null,
                                REM_HEBDO, -5),
                        FIXED_TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureePreavisCalculeeSemaines");
    }

    @Test
    void analyze_remunerationZero_acteProbable_pasIcpIndicatif() {
        // Zéro est accepté (pas négatif) mais ne déclenche pas le calcul
        LicenciementBeActeEquipollentResult r =
                LicenciementBeActeEquipollentAnalyzer.analyze(
                        req(TypeModificationUnilateraleEnum.SALAIRE,
                                AmpleurModificationEnum.SUBSTANTIELLE,
                                true, DATE_MOD_RECENTE, true, null,
                                BigDecimal.ZERO, 27),
                        FIXED_TODAY);

        assertThat(r.icpIndicatif()).isNull();
    }
}

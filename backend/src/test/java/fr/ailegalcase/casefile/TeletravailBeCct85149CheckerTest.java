package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-16 : tests unitaires du {@link TeletravailBeCct85149Checker}.
 *
 * <p>Couvre : conditions cumulatives (volontariat, convention écrite
 * art. 6 CCT n° 85, équipement fourni art. 9, droits maintenus art. 4,
 * déconnexion Loi 03/10/2022 si effectif ≥ 20), hiérarchie des
 * verdicts, validation des inputs et base juridique stable.</p>
 */
class TeletravailBeCct85149CheckerTest {

    private static final LocalDate DATE_DEBUT = LocalDate.of(2024, 6, 1);
    private static final BigDecimal INDEMNITE_30 = new BigDecimal("30.00");
    private static final BigDecimal PLAFOND_154 = new BigDecimal("154.74");

    private static TeletravailBeCct85149Request reqStructurelConforme() {
        return new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true,
                true,
                true,
                INDEMNITE_30,
                PLAFOND_154,
                true,
                true,
                10);
    }

    private static TeletravailBeCct85149Request reqOccasionnelConforme() {
        return new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.OCCASIONNEL_CCT_149,
                true,
                false,
                false,
                BigDecimal.ZERO,
                PLAFOND_154,
                true,
                true,
                10);
    }

    // ── Cas conforme nominaux ──────────────────────────────────────────────────

    @Test
    void analyze_structurelConforme_returnsConformeCct85() {
        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(reqStructurelConforme());

        assertThat(r.verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_85_STRUCTUREL);
        assertThat(r.conventionRespectee()).isTrue();
        assertThat(r.equipementRespecte()).isTrue();
        assertThat(r.droitsRespectes()).isTrue();
        assertThat(r.deconnexionRespectee()).isTrue();
        assertThat(r.indemniteExcedentaire())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.raison()).isEqualTo("CONFORME_STRUCTUREL");
        assertThat(r.synthese()).contains("pleinement conforme");
    }

    @Test
    void analyze_occasionnelConforme_returnsConformeCct149() {
        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(reqOccasionnelConforme());

        assertThat(r.verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_149_OCCASIONNEL);
        assertThat(r.raison()).isEqualTo("CONFORME_OCCASIONNEL");
        assertThat(r.synthese()).contains("CCT n° 149", "occasionnel");
    }

    @Test
    void analyze_structurelEffectifMoins20_pasDeDeconnexionExigee() {
        // Effectif 5 → la déconnexion n'est pas exigible, même si non définie
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154,
                true, false, 5);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_85_STRUCTUREL);
    }

    @Test
    void analyze_structurelIndemniteEgalePlafond_conforme() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                PLAFOND_154, PLAFOND_154,
                true, true, 10);

        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_85_STRUCTUREL);
        assertThat(r.indemniteExcedentaire())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void analyze_structurelIndemniteExcessive_conformeAvecAlerte() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                new BigDecimal("200.00"), PLAFOND_154,
                true, true, 10);

        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(req);
        assertThat(r.verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_85_STRUCTUREL);
        assertThat(r.indemniteExcedentaire())
                .isEqualByComparingTo(new BigDecimal("45.26"));
        assertThat(r.synthese())
                .contains("ATTENTION", "excédent imposable");
    }

    // ── Hiérarchie 1 : type indéterminé → A_ANALYSER ────────────────────────────

    @Test
    void analyze_typeIndetermine_returnsAanalyser() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.INDETERMINE,
                true, true, true,
                INDEMNITE_30, PLAFOND_154,
                true, true, 10);

        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("TYPE_TELETRAVAIL_INDETERMINE");
        assertThat(r.synthese()).contains("qualification");
    }

    // ── Hiérarchie 2 : convention écrite manquante (structurel) ─────────────────

    @Test
    void analyze_structurelSansConventionEcrite_returnsConventionManquante() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, false, true,
                INDEMNITE_30, PLAFOND_154,
                true, true, 10);

        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(req);

        assertThat(r.verdict()).isEqualTo(
                TeletravailBeCct85149Verdict.NON_CONFORME_CONVENTION_ECRITE_MANQUANTE);
        assertThat(r.conventionRespectee()).isFalse();
        assertThat(r.raison()).isEqualTo("CONVENTION_ECRITE_MANQUANTE");
        assertThat(r.synthese()).contains("art. 6 CCT n° 85");
    }

    @Test
    void analyze_occasionnelSansConventionEcrite_nePassePasParConventionManquante() {
        // L'occasionnel CCT 149 n'exige PAS la convention individuelle écrite
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.OCCASIONNEL_CCT_149,
                true, false, false,
                INDEMNITE_30, PLAFOND_154,
                true, true, 10);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_149_OCCASIONNEL);
    }

    // ── Hiérarchie 3 : équipement non fourni (structurel) ───────────────────────

    @Test
    void analyze_structurelEquipementNonFourni_returnsEquipementNonFourni() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, false,
                INDEMNITE_30, PLAFOND_154,
                true, true, 10);

        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(req);

        assertThat(r.verdict()).isEqualTo(
                TeletravailBeCct85149Verdict.NON_CONFORME_EQUIPEMENT_NON_FOURNI);
        assertThat(r.equipementRespecte()).isFalse();
        assertThat(r.raison()).isEqualTo("EQUIPEMENT_NON_FOURNI");
        assertThat(r.synthese()).contains("art. 9 CCT n° 85");
    }

    @Test
    void analyze_occasionnelEquipementNonFourni_nePassePasParEquipement() {
        // L'occasionnel CCT 149 n'exige PAS l'équipement fourni structurel
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.OCCASIONNEL_CCT_149,
                true, false, false,
                INDEMNITE_30, PLAFOND_154,
                true, true, 10);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_149_OCCASIONNEL);
    }

    // ── Hiérarchie 4 : droits réduits (structurel + occasionnel) ────────────────

    @Test
    void analyze_structurelDroitsReduits_returnsDroitsReduits() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154,
                false, true, 10);

        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.NON_CONFORME_DROITS_REDUITS);
        assertThat(r.droitsRespectes()).isFalse();
        assertThat(r.raison()).isEqualTo("DROITS_SOCIAUX_REDUITS");
        assertThat(r.synthese()).contains("art. 4 CCT n° 85");
    }

    @Test
    void analyze_occasionnelDroitsReduits_returnsDroitsReduits() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.OCCASIONNEL_CCT_149,
                true, false, false,
                INDEMNITE_30, PLAFOND_154,
                false, true, 10);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.NON_CONFORME_DROITS_REDUITS);
    }

    // ── Hiérarchie 5 : déconnexion non définie (≥ 20 travailleurs) ──────────────

    @Test
    void analyze_structurelDeconnexionNonDefinie_returnsFragileDeconnexion() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154,
                true, false, 30);

        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.FRAGILE_DECONNEXION_NON_DEFINIE);
        assertThat(r.deconnexionRespectee()).isFalse();
        assertThat(r.raison()).isEqualTo("DECONNEXION_NON_DEFINIE");
        assertThat(r.synthese()).contains("Loi du 03/10/2022");
    }

    @Test
    void analyze_seuilDeconnexion20_estExigible() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154,
                true, false, 20);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.FRAGILE_DECONNEXION_NON_DEFINIE);
    }

    @Test
    void analyze_seuilDeconnexion19_nonExigible() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154,
                true, false, 19);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.CONFORME_CCT_85_STRUCTUREL);
    }

    // ── Priorisation entre verdicts ─────────────────────────────────────────────

    @Test
    void analyze_conventionEtEquipementManquants_conventionPrime() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, false, false,
                INDEMNITE_30, PLAFOND_154,
                true, true, 10);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.NON_CONFORME_CONVENTION_ECRITE_MANQUANTE);
    }

    @Test
    void analyze_equipementEtDroitsManquants_equipementPrime() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, false,
                INDEMNITE_30, PLAFOND_154,
                false, true, 10);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.NON_CONFORME_EQUIPEMENT_NON_FOURNI);
    }

    @Test
    void analyze_droitsEtDeconnexionManquants_droitsPrime() {
        TeletravailBeCct85149Request req = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154,
                false, false, 30);

        assertThat(TeletravailBeCct85149Checker.analyze(req).verdict())
                .isEqualTo(TeletravailBeCct85149Verdict.NON_CONFORME_DROITS_REDUITS);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(reqStructurelConforme());

        assertThat(r.dateDebutTeletravail()).isEqualTo(DATE_DEBUT);
        assertThat(r.typeTeletravail())
                .isEqualTo(TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85);
        assertThat(r.volontariatReciproque()).isTrue();
        assertThat(r.conventionEcriteIndividuelle()).isTrue();
        assertThat(r.equipementFourniOuIndemnise()).isTrue();
        assertThat(r.indemniteForfaitaireMensuelleEuros())
                .isEqualByComparingTo("30.00");
        assertThat(r.plafondIndemniteMensuelleEuros())
                .isEqualByComparingTo("154.74");
        assertThat(r.droitsSociauxMaintenus()).isTrue();
        assertThat(r.deconnexionDefinie()).isTrue();
        assertThat(r.effectifEntreprise()).isEqualTo(10);
    }

    // ── Base juridique + avertissement ──────────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCct85EtCct149EtLoi() {
        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(reqStructurelConforme());

        assertThat(r.baseJuridique())
                .contains("CCT n° 85")
                .contains("09/11/2005")
                .contains("CCT n° 149")
                .contains("26/01/2021")
                .contains("art. 4")
                .contains("art. 6")
                .contains("art. 9")
                .contains("03/10/2022")
                .contains("déconnexion");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        TeletravailBeCct85149Result r =
                TeletravailBeCct85149Checker.analyze(reqStructurelConforme());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("avocat")
                .contains("ONSS")
                .contains("plafond");
    }

    // ── Validation conditionnelle ───────────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateDebutNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                null,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebutTeletravail");
    }

    @Test
    void analyze_typeNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT, null,
                true, true, true,
                INDEMNITE_30, PLAFOND_154, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeTeletravail");
    }

    @Test
    void analyze_volontariatNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                null, true, true,
                INDEMNITE_30, PLAFOND_154, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volontariatReciproque");
    }

    @Test
    void analyze_conventionEcriteNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, null, true,
                INDEMNITE_30, PLAFOND_154, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conventionEcriteIndividuelle");
    }

    @Test
    void analyze_equipementNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, null,
                INDEMNITE_30, PLAFOND_154, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equipementFourniOuIndemnise");
    }

    @Test
    void analyze_indemniteNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                null, PLAFOND_154, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("indemniteForfaitaireMensuelleEuros");
    }

    @Test
    void analyze_indemniteNegatif_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                new BigDecimal("-1.00"), PLAFOND_154, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("indemniteForfaitaireMensuelleEuros");
    }

    @Test
    void analyze_plafondZero_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, BigDecimal.ZERO, true, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plafondIndemniteMensuelleEuros");
    }

    @Test
    void analyze_droitsNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154, null, true, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("droitsSociauxMaintenus");
    }

    @Test
    void analyze_deconnexionNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154, true, null, 10);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deconnexionDefinie");
    }

    @Test
    void analyze_effectifNull_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154, true, true, null);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_effectifNegatif_throws() {
        TeletravailBeCct85149Request bad = new TeletravailBeCct85149Request(
                DATE_DEBUT,
                TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85,
                true, true, true,
                INDEMNITE_30, PLAFOND_154, true, true, -1);

        assertThatThrownBy(() -> TeletravailBeCct85149Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }
}

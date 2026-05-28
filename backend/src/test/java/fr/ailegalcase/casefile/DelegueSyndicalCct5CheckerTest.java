package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-10 : tests unitaires du {@link DelegueSyndicalCct5Checker}.
 *
 * <p>Couvre : champ d'application (seuil + présence OS), régularité de
 * la désignation (4 statuts), missions exerçables art. 3 + missions
 * supplétives art. 24 (4 combinaisons CE/CPPT), hiérarchie des verdicts
 * (champ &gt; OS &gt; contestation &gt; notification), calcul de la date
 * indicative de fin de mandat (4 ans), validation des inputs et base
 * juridique stable (CCT n° 5 + CCT sectorielles).</p>
 */
class DelegueSyndicalCct5CheckerTest {

    private static final LocalDate DATE_DES = LocalDate.of(2026, 4, 1);

    private static DelegueSyndicalCct5Request reqReconnu() {
        return new DelegueSyndicalCct5Request(
                DATE_DES,
                80, 50,
                true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);
    }

    // ── Statut reconnu ──────────────────────────────────────────────────────

    @Test
    void analyze_statutReconnu_returnsStatutReconnu() {
        DelegueSyndicalCct5Result r =
                DelegueSyndicalCct5Checker.analyze(reqReconnu());

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.STATUT_RECONNU);
        assertThat(r.eligibleStatutDs()).isTrue();
        assertThat(r.entrepriseDansChamp()).isTrue();
        assertThat(r.designationReguliere()).isTrue();
        assertThat(r.dateFinMandatIndicative())
                .isEqualTo(LocalDate.of(2030, 4, 1));
        assertThat(r.raison()).isEqualTo("STATUT_RECONNU");
        assertThat(r.synthese())
                .contains("Statut de délégué syndical CCT n° 5 pleinement"
                        + " reconnu")
                .contains("2030-04-01");
        assertThat(r.missionsExercables()).isNotEmpty();
    }

    // ── Missions art. 3 toujours présentes ─────────────────────────────────

    @Test
    void analyze_statutReconnu_missionsArt3_toujoursPresentes() {
        DelegueSyndicalCct5Result r =
                DelegueSyndicalCct5Checker.analyze(reqReconnu());

        assertThat(r.missionsExercables())
                .anyMatch(m -> m.contains("négociation des CCT d'entreprise"))
                .anyMatch(m -> m.contains("traitement des réclamations"))
                .anyMatch(m -> m.contains("information du personnel"))
                .anyMatch(m -> m.contains("conflit collectif"));
    }

    // ── Missions supplétives art. 24 — 4 combinaisons CE/CPPT ──────────────

    @Test
    void analyze_ceEtCpptExistants_pasDeMissionSuppletive() {
        DelegueSyndicalCct5Result r =
                DelegueSyndicalCct5Checker.analyze(reqReconnu());

        assertThat(r.missionsExercables())
                .noneMatch(m -> m.contains("art. 24"));
    }

    @Test
    void analyze_ceAbsent_addsMissionSuppletiveCe() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                false, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.missionsExercables())
                .anyMatch(m -> m.contains("Conseil d'entreprise")
                        && m.contains("art. 24"))
                .noneMatch(m -> m.contains("Comité PPT"));
    }

    @Test
    void analyze_cpptAbsent_addsMissionSuppletiveCppt() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, false);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.missionsExercables())
                .anyMatch(m -> m.contains("Comité PPT")
                        && m.contains("art. 24"))
                .noneMatch(m -> m.contains("Conseil d'entreprise")
                        && m.contains("art. 24"));
    }

    @Test
    void analyze_ceEtCpptAbsents_addsLesDeuxMissionsSuppletives() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                false, false);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.missionsExercables())
                .anyMatch(m -> m.contains("Conseil d'entreprise"))
                .anyMatch(m -> m.contains("Comité PPT"));
    }

    // ── Notification employeur manquante → STATUT_FRAGILE ───────────────────

    @Test
    void analyze_notificationManquante_returnsStatutFragile() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.NOTIFICATION_EMPLOYEUR_MANQUANTE,
                true, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.STATUT_FRAGILE_NOTIFICATION_MANQUANTE);
        assertThat(r.eligibleStatutDs()).isFalse();
        assertThat(r.entrepriseDansChamp()).isTrue();
        assertThat(r.designationReguliere()).isFalse();
        assertThat(r.dateFinMandatIndicative())
                .isEqualTo(LocalDate.of(2030, 4, 1));
        assertThat(r.raison()).isEqualTo("NOTIFICATION_EMPLOYEUR_MANQUANTE");
        assertThat(r.synthese())
                .contains("notification écrite à l'employeur")
                .contains("régulariser");
        // Missions art. 3 quand même listées (utiles à connaître pour
        // régularisation).
        assertThat(r.missionsExercables()).isNotEmpty();
    }

    // ── Entreprise hors champ ───────────────────────────────────────────────

    @Test
    void analyze_effectifSousSeuil_returnsHorsChamp() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 30, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.INELIGIBLE_ENTREPRISE_HORS_CHAMP);
        assertThat(r.eligibleStatutDs()).isFalse();
        assertThat(r.entrepriseDansChamp()).isFalse();
        assertThat(r.dateFinMandatIndicative()).isNull();
        assertThat(r.missionsExercables()).isEmpty();
        assertThat(r.raison()).isEqualTo("ENTREPRISE_HORS_CHAMP_CCT5");
        assertThat(r.synthese())
                .contains("30")
                .contains("50");
    }

    @Test
    void analyze_pasOsRepresentative_returnsHorsChamp() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, false,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.INELIGIBLE_ENTREPRISE_HORS_CHAMP);
        assertThat(r.entrepriseDansChamp()).isFalse();
        assertThat(r.synthese())
                .contains("absence d'organisation syndicale représentative");
    }

    @Test
    void analyze_seuilExactementAtteint_estDansChamp() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 50, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.STATUT_RECONNU);
    }

    // ── Pas désigné par OS représentative ───────────────────────────────────

    @Test
    void analyze_pasDesignePerOS_returnsIneligible() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.PAS_DESIGNE_PAR_OS,
                true, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.INELIGIBLE_PAS_DESIGNE_PAR_OS);
        assertThat(r.eligibleStatutDs()).isFalse();
        assertThat(r.designationReguliere()).isFalse();
        assertThat(r.entrepriseDansChamp()).isTrue();
        assertThat(r.dateFinMandatIndicative()).isNull();
        assertThat(r.missionsExercables()).isEmpty();
        assertThat(r.raison()).isEqualTo("PAS_DESIGNE_PAR_OS_REPRESENTATIVE");
        assertThat(r.synthese())
                .contains("CSC / FGTB / CGSLB")
                .contains("art. 2");
    }

    // ── Désignation contestée → A_ANALYSER ──────────────────────────────────

    @Test
    void analyze_designationContestee_returnsAanalyser() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNATION_CONTESTEE,
                true, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.A_ANALYSER);
        assertThat(r.raison()).isEqualTo("DESIGNATION_CONTESTEE");
        assertThat(r.synthese())
                .contains("contestation pendante")
                .contains("avocat BE spécialisé droit syndical");
    }

    // ── Hiérarchie : champ prime sur statut désignation ─────────────────────

    @Test
    void analyze_horsChampEtPasDesignePerOS_horsChampWins() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 10, 50, false,
                DelegueSyndicalCct5StatutDesignation.PAS_DESIGNE_PAR_OS,
                false, false);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.INELIGIBLE_ENTREPRISE_HORS_CHAMP);
    }

    @Test
    void analyze_dansChampMaisPasDesignePerOs_pasDesigneWins() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.PAS_DESIGNE_PAR_OS,
                false, false);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict())
                .isEqualTo(DelegueSyndicalCct5Verdict.INELIGIBLE_PAS_DESIGNE_PAR_OS);
    }

    @Test
    void analyze_contestationPrimePresent_avant_notification() {
        DelegueSyndicalCct5Request req = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNATION_CONTESTEE,
                true, true);

        DelegueSyndicalCct5Result r = DelegueSyndicalCct5Checker.analyze(req);

        assertThat(r.verdict()).isEqualTo(DelegueSyndicalCct5Verdict.A_ANALYSER);
    }

    // ── Snapshot inputs ─────────────────────────────────────────────────────

    @Test
    void analyze_snapshot_inclutTousLesInputs() {
        DelegueSyndicalCct5Result r =
                DelegueSyndicalCct5Checker.analyze(reqReconnu());

        assertThat(r.dateDesignation()).isEqualTo(DATE_DES);
        assertThat(r.effectifEntreprise()).isEqualTo(80);
        assertThat(r.seuilSectorielRequis()).isEqualTo(50);
        assertThat(r.presenceOsRepresentative()).isTrue();
        assertThat(r.statutDesignation())
                .isEqualTo(DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE);
        assertThat(r.ceExistant()).isTrue();
        assertThat(r.cpptExistant()).isTrue();
    }

    // ── Base juridique + avertissement ──────────────────────────────────────

    @Test
    void analyze_baseJuridique_referenceCct5_etArticles_2_3_24() {
        DelegueSyndicalCct5Result r =
                DelegueSyndicalCct5Checker.analyze(reqReconnu());

        assertThat(r.baseJuridique())
                .contains("CCT n° 5 du 24/05/1971")
                .contains("art. 2")
                .contains("art. 3")
                .contains("art. 24")
                .contains("AR du 26/01/1972")
                .contains("CCT sectorielles");
    }

    @Test
    void analyze_avertissementSystematiquePresent() {
        DelegueSyndicalCct5Result r =
                DelegueSyndicalCct5Checker.analyze(reqReconnu());

        assertThat(r.avertissement())
                .isNotNull()
                .contains("CCT sectorielle")
                .contains("avocat")
                .contains("Loi 19/03/1991");
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    @Test
    void analyze_requestNull_throws() {
        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requête");
    }

    @Test
    void analyze_dateDesignationNull_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                null, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDesignation");
    }

    @Test
    void analyze_effectifNegatif_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, -5, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_effectifNull_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, null, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectifEntreprise");
    }

    @Test
    void analyze_seuilNull_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, 80, null, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seuilSectorielRequis");
    }

    @Test
    void analyze_seuilZero_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 0, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seuilSectorielRequis");
    }

    @Test
    void analyze_presenceOsNull_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, null,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("presenceOsRepresentative");
    }

    @Test
    void analyze_statutDesignationNull_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true, null, true, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statutDesignation");
    }

    @Test
    void analyze_ceExistantNull_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                null, true);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ceExistant");
    }

    @Test
    void analyze_cpptExistantNull_throws() {
        DelegueSyndicalCct5Request bad = new DelegueSyndicalCct5Request(
                DATE_DES, 80, 50, true,
                DelegueSyndicalCct5StatutDesignation.DESIGNE_PAR_OS_REPRESENTATIVE,
                true, null);

        assertThatThrownBy(() ->
                DelegueSyndicalCct5Checker.analyze(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpptExistant");
    }
}

package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-218-09 : tests unitaires de {@link ActionGroupeDiscriminationAnalyzer}.
 * Couvre les verdicts (RECEVABLE, PREMATURE, IRRECEVABLE_QUALITE,
 * INFO_MANQUANTE), le calcul du délai de carence de 6 mois
 * (dateRecevabiliteSaisine = mise en demeure + 6 mois, L. 1134-9 CT), la
 * pluralité, la checklist et la validation des entrées.
 */
class ActionGroupeDiscriminationAnalyzerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 30);

    // ── RECEVABLE : qualité + pluralité + carence écoulée ───────────────

    @Test
    void analyze_syndicat_carenceEcoulee_pluralite_recevable() {
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.SEXE,
                5,
                ActionGroupeDiscriminationObjet.LES_DEUX,
                TODAY.minusDays(200),
                TODAY);

        assertThat(r.verdict()).isEqualTo(ActionGroupeDiscriminationVerdict.RECEVABLE);
        assertThat(r.qualiteAAgir()).isTrue();
        assertThat(r.pluraliteEtablie()).isTrue();
        assertThat(r.delaiCarenceRespecte()).isTrue();
        assertThat(r.dateRecevabiliteSaisine()).isEqualTo(TODAY.minusDays(200).plusMonths(6));
        assertThat(r.baseJuridique()).contains("L. 1134-7");
    }

    @Test
    void analyze_associationAgreee_recevable() {
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.ASSOCIATION_AGREEE_5ANS,
                ActionGroupeDiscriminationMotif.ORIGINE,
                3,
                ActionGroupeDiscriminationObjet.CESSATION_MANQUEMENT,
                TODAY.minusMonths(8),
                TODAY);

        assertThat(r.verdict()).isEqualTo(ActionGroupeDiscriminationVerdict.RECEVABLE);
        assertThat(r.qualiteAAgir()).isTrue();
    }

    // ── PREMATURE : délai de carence non écoulé ─────────────────────────

    @Test
    void analyze_carenceNonEcoulee_premature_avecDateRecevabilite() {
        LocalDate miseEnDemeure = TODAY.minusDays(30);
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.AGE,
                4,
                ActionGroupeDiscriminationObjet.LES_DEUX,
                miseEnDemeure,
                TODAY);

        assertThat(r.verdict()).isEqualTo(ActionGroupeDiscriminationVerdict.PREMATURE);
        assertThat(r.delaiCarenceRespecte()).isFalse();
        assertThat(r.dateRecevabiliteSaisine()).isEqualTo(miseEnDemeure.plusMonths(6));
    }

    @Test
    void analyze_dateRecevabiliteSaisine_exactementAujourdhui_recevable() {
        // Mise en demeure il y a exactement 6 mois → carence respectée (>= today).
        LocalDate miseEnDemeure = TODAY.minusMonths(6);
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.HANDICAP,
                2,
                ActionGroupeDiscriminationObjet.LES_DEUX,
                miseEnDemeure,
                TODAY);

        assertThat(r.dateRecevabiliteSaisine()).isEqualTo(TODAY);
        assertThat(r.delaiCarenceRespecte()).isTrue();
        assertThat(r.verdict()).isEqualTo(ActionGroupeDiscriminationVerdict.RECEVABLE);
    }

    // ── IRRECEVABLE_QUALITE : organisation non habilitée ────────────────

    @Test
    void analyze_organisationAutre_irrecevableQualite() {
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.AUTRE,
                ActionGroupeDiscriminationMotif.RELIGION,
                10,
                ActionGroupeDiscriminationObjet.LES_DEUX,
                TODAY.minusDays(300),
                TODAY);

        assertThat(r.verdict()).isEqualTo(ActionGroupeDiscriminationVerdict.IRRECEVABLE_QUALITE);
        assertThat(r.qualiteAAgir()).isFalse();
        assertThat(r.checklist())
                .anyMatch(i -> i.bloquant() && i.baseJuridique().contains("L. 1134-7"));
    }

    // ── INFO_MANQUANTE : mise en demeure absente ────────────────────────

    @Test
    void analyze_sansMiseEnDemeure_infoManquante_itemBloquant() {
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.ACTIVITE_SYNDICALE,
                6,
                ActionGroupeDiscriminationObjet.LES_DEUX,
                null,
                TODAY);

        assertThat(r.verdict()).isEqualTo(ActionGroupeDiscriminationVerdict.INFO_MANQUANTE);
        assertThat(r.dateRecevabiliteSaisine()).isNull();
        assertThat(r.delaiCarenceRespecte()).isFalse();
        assertThat(r.checklist())
                .anyMatch(i -> i.bloquant() && i.baseJuridique().contains("L. 1134-9"));
    }

    // ── Pluralité : une seule personne ──────────────────────────────────

    @Test
    void analyze_pluraliteNonEtablie_uneSeulePersonne() {
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.GROSSESSE,
                1,
                ActionGroupeDiscriminationObjet.LES_DEUX,
                TODAY.minusDays(300),
                TODAY);

        assertThat(r.pluraliteEtablie()).isFalse();
        // Qualité + carence OK mais pluralité non établie → pas RECEVABLE.
        assertThat(r.verdict()).isNotEqualTo(ActionGroupeDiscriminationVerdict.RECEVABLE);
        assertThat(r.checklist())
                .anyMatch(i -> i.bloquant() && i.libelle().contains("pluralité"));
    }

    // ── Checklist : items structurels présents ──────────────────────────

    @Test
    void analyze_checklist_contientConditionsCles() {
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.ASSOCIATION_AGREEE_5ANS,
                ActionGroupeDiscriminationMotif.ORIENTATION_SEXUELLE,
                7,
                ActionGroupeDiscriminationObjet.LES_DEUX,
                TODAY.minusDays(250),
                TODAY);

        assertThat(r.checklist()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(r.checklist())
                .anyMatch(i -> i.baseJuridique().contains("L. 1134-10"));
        assertThat(r.objetAction()).isEqualTo(ActionGroupeDiscriminationObjet.LES_DEUX);
    }

    // ── Défaut objetAction null → LES_DEUX ──────────────────────────────

    @Test
    void analyze_objetActionNull_defautLesDeux() {
        ActionGroupeDiscriminationResult r = ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.AUTRE,
                3,
                null,
                TODAY.minusDays(300),
                TODAY);

        assertThat(r.objetAction()).isEqualTo(ActionGroupeDiscriminationObjet.LES_DEUX);
    }

    // ── Validation ──────────────────────────────────────────────────────

    @Test
    void analyze_typeOrganisationNull_throws() {
        assertThatThrownBy(() -> ActionGroupeDiscriminationAnalyzer.analyze(
                null, ActionGroupeDiscriminationMotif.SEXE, 3,
                ActionGroupeDiscriminationObjet.LES_DEUX, TODAY.minusDays(200), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_nombrePersonnesZero_throws() {
        assertThatThrownBy(() -> ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.SEXE, 0,
                ActionGroupeDiscriminationObjet.LES_DEUX, TODAY.minusDays(200), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void analyze_miseEnDemeureFuture_throws() {
        assertThatThrownBy(() -> ActionGroupeDiscriminationAnalyzer.analyze(
                ActionGroupeDiscriminationTypeOrganisation.SYNDICAT_REPRESENTATIF,
                ActionGroupeDiscriminationMotif.SEXE, 3,
                ActionGroupeDiscriminationObjet.LES_DEUX, TODAY.plusDays(1), TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

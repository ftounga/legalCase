package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-283 / SF-283-03 — couverture des 6 catalogues (domaine × pays) + fallback.
 * Vérifie taille, ordre, type de tête/queue et libellé par défaut.
 */
class CasePhaseSuggestionCatalogTest {

    private static List<CasePhaseType> types(List<CasePhaseSuggestion> s) {
        return s.stream().map(CasePhaseSuggestion::type).toList();
    }

    @Test
    void travailFrance_orderedPhases_withCloture() {
        List<CasePhaseSuggestion> s = CasePhaseSuggestionCatalog
                .forDomainAndCountry("DROIT_DU_TRAVAIL", "FRANCE");
        // SF-283-05 — « Clôture de l'instruction » insérée entre Mise en état et le fond
        // (procédure écrite devant le CPH, ordonnance de clôture).
        assertThat(types(s)).containsExactly(
                CasePhaseType.SAISINE, CasePhaseType.CONCILIATION, CasePhaseType.MISE_EN_ETAT,
                CasePhaseType.CLOTURE, CasePhaseType.FOND, CasePhaseType.JUGEMENT,
                CasePhaseType.APPEL, CasePhaseType.CASSATION, CasePhaseType.EXECUTION);
        assertThat(s.get(0).defaultLabel()).isEqualTo("Saisine du Conseil de prud'hommes");
    }

    @Test
    void travailBelgique_introductionFirst() {
        List<CasePhaseSuggestion> s = CasePhaseSuggestionCatalog
                .forDomainAndCountry("DROIT_DU_TRAVAIL", "BELGIQUE");
        assertThat(types(s)).containsExactly(
                CasePhaseType.INTRODUCTION, CasePhaseType.MISE_EN_ETAT, CasePhaseType.FOND,
                CasePhaseType.JUGEMENT, CasePhaseType.APPEL, CasePhaseType.CASSATION,
                CasePhaseType.EXECUTION);
        assertThat(s.get(0).defaultLabel()).isEqualTo("Introduction (citation / requête)");
    }

    @Test
    void immigrationFrance_administrativePath() {
        List<CasePhaseSuggestion> s = CasePhaseSuggestionCatalog
                .forDomainAndCountry("DROIT_IMMIGRATION", "FRANCE");
        assertThat(types(s)).containsExactly(
                CasePhaseType.RECOURS_PREALABLE, CasePhaseType.TRIBUNAL_ADMINISTRATIF,
                CasePhaseType.CNDA, CasePhaseType.COUR_ADMINISTRATIVE_APPEL,
                CasePhaseType.CONSEIL_ETAT, CasePhaseType.EXECUTION);
        assertThat(s.get(0).defaultLabel()).isEqualTo("Recours gracieux / hiérarchique");
    }

    @Test
    void immigrationBelgique_cceAndConseilEtat() {
        List<CasePhaseSuggestion> s = CasePhaseSuggestionCatalog
                .forDomainAndCountry("DROIT_IMMIGRATION", "BELGIQUE");
        assertThat(types(s)).containsExactly(
                CasePhaseType.RECOURS_PREALABLE, CasePhaseType.CCE,
                CasePhaseType.CONSEIL_ETAT, CasePhaseType.EXECUTION);
        assertThat(s.get(1).defaultLabel()).isEqualTo("Conseil du Contentieux des Étrangers");
    }

    @Test
    void familleFrance_jafPath() {
        List<CasePhaseSuggestion> s = CasePhaseSuggestionCatalog
                .forDomainAndCountry("DROIT_FAMILLE", "FRANCE");
        assertThat(types(s)).containsExactly(
                CasePhaseType.SAISINE, CasePhaseType.CONCILIATION, CasePhaseType.MISE_EN_ETAT,
                CasePhaseType.FOND, CasePhaseType.JUGEMENT, CasePhaseType.APPEL,
                CasePhaseType.CASSATION, CasePhaseType.EXECUTION);
        assertThat(s.get(0).defaultLabel()).isEqualTo("Requête / assignation");
    }

    @Test
    void familleBelgique_chambreReglementAmiable() {
        List<CasePhaseSuggestion> s = CasePhaseSuggestionCatalog
                .forDomainAndCountry("DROIT_FAMILLE", "BELGIQUE");
        assertThat(types(s)).containsExactly(
                CasePhaseType.INTRODUCTION, CasePhaseType.CONCILIATION, CasePhaseType.MISE_EN_ETAT,
                CasePhaseType.FOND, CasePhaseType.JUGEMENT, CasePhaseType.APPEL,
                CasePhaseType.CASSATION, CasePhaseType.EXECUTION);
        assertThat(s.get(1).defaultLabel()).isEqualTo("Chambre de règlement amiable");
    }

    @Test
    void unknownCombination_fallsBackToCivilFrTravail() {
        // Domaine inconnu
        assertThat(CasePhaseSuggestionCatalog.forDomainAndCountry("DROIT_PENAL", "FRANCE"))
                .isEqualTo(CasePhaseSuggestionCatalog.FALLBACK);
        // Pays inconnu
        assertThat(CasePhaseSuggestionCatalog.forDomainAndCountry("DROIT_DU_TRAVAIL", "SUISSE"))
                .isEqualTo(CasePhaseSuggestionCatalog.FALLBACK);
        // Nulls
        assertThat(CasePhaseSuggestionCatalog.forDomainAndCountry(null, null))
                .isEqualTo(CasePhaseSuggestionCatalog.FALLBACK);
        // Le fallback est bien la liste civile FR travail (9 phases depuis SF-283-05 : + Clôture)
        assertThat(CasePhaseSuggestionCatalog.FALLBACK).hasSize(9);
        assertThat(CasePhaseSuggestionCatalog.FALLBACK.get(0).type()).isEqualTo(CasePhaseType.SAISINE);
    }

    @Test
    void caseInsensitive_resolvesCatalog() {
        assertThat(CasePhaseSuggestionCatalog.forDomainAndCountry("droit_immigration", "belgique"))
                .extracting(CasePhaseSuggestion::type)
                .containsExactly(CasePhaseType.RECOURS_PREALABLE, CasePhaseType.CCE,
                        CasePhaseType.CONSEIL_ETAT, CasePhaseType.EXECUTION);
    }

    @Test
    void allEnumValuesWithinVarchar30() {
        for (CasePhaseType t : CasePhaseType.values()) {
            assertThat(t.name().length())
                    .as("enum %s ≤ 30 chars (varchar(30))", t.name())
                    .isLessThanOrEqualTo(30);
        }
    }
}

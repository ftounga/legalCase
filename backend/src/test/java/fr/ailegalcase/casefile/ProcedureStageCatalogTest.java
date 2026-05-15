package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F-243 / SF-243-01 — Tests unitaires du référentiel {@link ProcedureStageCatalog}.
 *
 * <p>Vérifie que les 6 combinaisons domaine × pays sont encodées, internement cohérentes,
 * et que la validation de combinaison se comporte comme spécifié dans la mini-spec.
 */
class ProcedureStageCatalogTest {

    private static final List<String> DOMAINS = List.of(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL,
            ProcedureStageCatalog.DROIT_IMMIGRATION,
            ProcedureStageCatalog.DROIT_FAMILLE);
    private static final List<String> COUNTRIES = List.of(
            ProcedureStageCatalog.FRANCE,
            ProcedureStageCatalog.BELGIQUE);

    // U-01 : les 6 combinaisons sont encodées et non vides.
    @Test
    void sixCombinations_areAllPresentAndNonEmpty() {
        assertThat(ProcedureStageCatalog.allCombinationKeys()).hasSize(6);
        for (String domain : DOMAINS) {
            for (String country : COUNTRIES) {
                ProcedureStageCatalog.CatalogEntry entry =
                        ProcedureStageCatalog.forDomainAndCountry(domain, country);
                assertThat(entry.jurisdictions()).as("juridictions %s/%s", domain, country).isNotEmpty();
                assertThat(entry.stages()).as("stades %s/%s", domain, country).isNotEmpty();
                assertThat(entry.positions()).as("positions %s/%s", domain, country).isNotEmpty();
            }
        }
    }

    // U-02 : intégrité interne — aucun stade orphelin, aucune position orpheline.
    @Test
    void selfCheck_reportsNoIntegrityError() {
        assertThat(ProcedureStageCatalog.selfCheck()).isEmpty();
    }

    // U-03 : isKnownDomain / isKnownCountry.
    @Test
    void knownDomainAndCountry_recognised() {
        assertThat(ProcedureStageCatalog.isKnownDomain("DROIT_DU_TRAVAIL")).isTrue();
        assertThat(ProcedureStageCatalog.isKnownDomain("DROIT_PENAL")).isFalse();
        assertThat(ProcedureStageCatalog.isKnownDomain(null)).isFalse();
        assertThat(ProcedureStageCatalog.isKnownCountry("BELGIQUE")).isTrue();
        assertThat(ProcedureStageCatalog.isKnownCountry("SUISSE")).isFalse();
        assertThat(ProcedureStageCatalog.isKnownCountry(null)).isFalse();
    }

    // U-04 : combinaison inconnue → exception.
    @Test
    void forDomainAndCountry_unknownCombination_throws() {
        assertThatThrownBy(() -> ProcedureStageCatalog.forDomainAndCountry("DROIT_PENAL", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // U-05 : combinaison valide DROIT_DU_TRAVAIL / FRANCE → pas d'erreur.
    @Test
    void validate_validCombination_returnsEmpty() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", "CPH", "FOND", "DEMANDEUR")).isEmpty();
    }

    // U-06 : combinaison entièrement nulle → valide (les 3 champs optionnels).
    @Test
    void validate_allNull_returnsEmpty() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", null, null, null)).isEmpty();
    }

    // U-07 : juridiction seule renseignée → valide.
    @Test
    void validate_jurisdictionOnly_returnsEmpty() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", "CPH", null, null)).isEmpty();
    }

    // U-08 : juridiction inconnue pour le domaine → erreur 422.
    @Test
    void validate_unknownJurisdiction_returnsError() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", "JAF", null, null))
                .isPresent()
                .get().asString().contains("Juridiction inconnue");
    }

    // U-09 : stade non rattaché à la juridiction fournie → erreur 422.
    @Test
    void validate_stageNotUnderJurisdiction_returnsError() {
        // APPEL appartient à CA_SOC, pas à CPH.
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", "CPH", "APPEL", null))
                .isPresent()
                .get().asString().contains("n'appartient pas à la juridiction");
    }

    // U-10 : position non valide pour le stade → erreur 422.
    @Test
    void validate_positionNotValidForStage_returnsError() {
        // APPELANT est valide pour APPEL, pas pour FOND.
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", "CPH", "FOND", "APPELANT"))
                .isPresent()
                .get().asString().contains("n'est pas valide pour le stade");
    }

    // U-11 : stade renseigné sans juridiction → erreur 422.
    @Test
    void validate_stageWithoutJurisdiction_returnsError() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", null, "FOND", null))
                .isPresent()
                .get().asString().contains("sans juridiction");
    }

    // U-12 : position renseignée sans stade → erreur 422.
    @Test
    void validate_positionWithoutStage_returnsError() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", "CPH", null, "DEMANDEUR"))
                .isPresent()
                .get().asString().contains("sans stade");
    }

    // U-13 : stade inconnu pour le domaine → erreur 422.
    @Test
    void validate_unknownStage_returnsError() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_DU_TRAVAIL", "FRANCE", "CPH", "DIVORCE_FOND", null))
                .isPresent()
                .get().asString().contains("Stade inconnu");
    }

    // U-14 : labels résolus pour une combinaison valide.
    @Test
    void labels_resolvedForKnownCodes() {
        assertThat(ProcedureStageCatalog.jurisdictionLabel("DROIT_DU_TRAVAIL", "FRANCE", "CPH"))
                .isEqualTo("Conseil de prud'hommes");
        assertThat(ProcedureStageCatalog.stageLabel("DROIT_DU_TRAVAIL", "FRANCE", "FOND"))
                .isEqualTo("Bureau de jugement (fond)");
        assertThat(ProcedureStageCatalog.positionLabel("DROIT_DU_TRAVAIL", "FRANCE", "DEMANDEUR"))
                .isEqualTo("Demandeur (salarié)");
    }

    // U-15 : labels null pour code null ou inconnu.
    @Test
    void labels_nullForNullOrUnknownCode() {
        assertThat(ProcedureStageCatalog.jurisdictionLabel("DROIT_DU_TRAVAIL", "FRANCE", null)).isNull();
        assertThat(ProcedureStageCatalog.stageLabel("DROIT_DU_TRAVAIL", "FRANCE", "INEXISTANT")).isNull();
    }

    // U-16 : BE immigration — référentiel BE-only, codes distincts de la FR.
    @Test
    void belgiumImmigration_hasBelgiumSpecificCodes() {
        ProcedureStageCatalog.CatalogEntry be =
                ProcedureStageCatalog.forDomainAndCountry("DROIT_IMMIGRATION", "BELGIQUE");
        Set<String> jurisdictionCodes = be.jurisdictions().stream()
                .map(ProcedureStageCatalog.Jurisdiction::code).collect(java.util.stream.Collectors.toSet());
        assertThat(jurisdictionCodes).containsExactlyInAnyOrder("CCE", "CE_BE", "OE");
        // REQUERANT valide pour le recours en plein contentieux BE.
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_IMMIGRATION", "BELGIQUE", "CCE", "RECOURS_PLEIN_CONTENTIEUX", "REQUERANT")).isEmpty();
    }

    // U-17 : DROIT_FAMILLE / FRANCE — ordonnance de protection + requérant valide.
    @Test
    void familyFrance_ordonnanceProtection_requerantValid() {
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_FAMILLE", "FRANCE", "JAF", "ORDONNANCE_PROTECTION", "REQUERANT")).isEmpty();
        // DEMANDEUR n'est pas valide pour ORDONNANCE_PROTECTION.
        assertThat(ProcedureStageCatalog.validate(
                "DROIT_FAMILLE", "FRANCE", "JAF", "ORDONNANCE_PROTECTION", "DEMANDEUR")).isPresent();
    }
}

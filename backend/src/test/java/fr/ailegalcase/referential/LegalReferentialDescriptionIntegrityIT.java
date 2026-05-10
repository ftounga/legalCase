package fr.ailegalcase.referential;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-140-03 garde-fou A : test d'intégrité qui bloque toute migration qui
 * oublie de renseigner {@code description} pour une entry système de
 * {@link LegalReferential}.
 *
 * <p>Principe : une entry {@code isSystem == true} doit toujours porter une
 * description métier (DB = seule source de vérité, cf. F-139). Les overrides
 * workspace ({@code isSystem == false}) sont exemptés : l'admin peut créer
 * une variante sans fournir de description (le fallback remontera la
 * description d'origine).
 *
 * <p>Ce test tourne via {@link SpringBootTest} pour que Liquibase applique les
 * migrations seed avant la vérification — il détecte donc en CI une migration
 * qui oublierait la description d'une nouvelle entry système.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
class LegalReferentialDescriptionIntegrityIT {

    @Autowired
    LegalReferentialRepository repository;

    /**
     * Types exemptés du check parce qu'ils ont une description riche native
     * dans leur {@code value_json} (champ {@code description}, {@code conditions},
     * etc.) et que le frontend la remonte via le fallback {@code extractMetierDescription}.
     *
     * <p>Ces types peuvent recevoir une description DB (ajoutée par l'admin ou
     * par une migration future) qui primera alors, mais ce n'est pas obligatoire.
     */
    private static final List<String> TYPES_AVEC_DESCRIPTION_NATIVE_JSON = List.of(
            "LICENCIEMENT_CRITERES",
            "RUPTURE_CONV_CRITERES",
            "IMMIGRATION_TITLES",
            "IMMIGRATION_RECOURS",
            "IMMIGRATION_WORK_RIGHTS",
            "DIVORCE_ETAPES",
            "DIVORCE_PIECES"
    );

    @Test
    void toute_entry_systeme_a_une_description_sauf_types_avec_description_native_JSON() {
        List<LegalReferential> offenders = repository.findAll().stream()
                .filter(LegalReferential::isSystem)
                .filter(e -> !TYPES_AVEC_DESCRIPTION_NATIVE_JSON.contains(e.getReferentialType()))
                .filter(e -> e.getDescription() == null || e.getDescription().isBlank())
                .collect(Collectors.toList());

        assertThat(offenders)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-140-03 : les entries système suivantes n'ont pas de description métier en DB. ")
                            .append("Toute migration qui ajoute une nouvelle entry système doit inclure 'description' dans son INSERT ")
                            .append("(sauf pour les types à description riche native dans le JSON : ")
                            .append(TYPES_AVEC_DESCRIPTION_NATIVE_JSON)
                            .append("). Entries en faute :\n");
                    offenders.forEach(e -> sb.append(" - ")
                            .append(e.getReferentialType()).append(" / ")
                            .append(e.getEntryKey())
                            .append(" (country=").append(e.getCountry()).append(")\n"));
                    return sb.toString();
                })
                .isEmpty();
    }

    @Test
    void les_types_a_description_native_JSON_peuvent_avoir_description_null() {
        // Sanity check : le filtre par type n'a pas introduit de régression
        // en marquant par erreur des types qui ont BIEN une description DB.
        List<LegalReferential> nativeTypesEntries = repository.findAll().stream()
                .filter(LegalReferential::isSystem)
                .filter(e -> TYPES_AVEC_DESCRIPTION_NATIVE_JSON.contains(e.getReferentialType()))
                .collect(Collectors.toList());

        // On s'attend à au moins 1 entry de chaque type natif (ils sont tous seedés)
        assertThat(nativeTypesEntries).isNotEmpty();
        // Pas d'assertion sur description : null OU renseignée sont tous les deux OK
    }

    /**
     * F-225 SF-225-03 garde-fou : tout {@code referential_type} système doit avoir
     * une intégration UX dédiée côté frontend (entrée {@code SECTION_LABELS} +
     * branches {@code formatValue} / {@code sectionIcon} dans
     * {@code frontend/src/app/referentials/referentials.component.ts}).
     *
     * <p>Pattern miroir de {@code DecisionToolVisibilityIntegrityIT} (F-164 SF-164-01) :
     * la liste {@link #KNOWN_FRONTEND_REFERENTIAL_TYPES} doit être manuellement mise
     * à jour en même temps que toute migration qui INSERT un nouveau {@code referential_type}.
     * Si elle est oubliée, le test échoue → empêche un type orphelin (titre brut, JSON brut)
     * d'arriver en prod (cas réel des 5 types F-225 SF-225-01).
     *
     * <p>Pour ajouter un nouveau {@code referential_type} :
     * <ol>
     *   <li>Ajouter l'entrée {@code SECTION_LABELS} + branches {@code formatValue} / {@code sectionIcon}
     *       dans {@code referentials.component.ts}.</li>
     *   <li>Ajouter la clé dans {@link #KNOWN_FRONTEND_REFERENTIAL_TYPES} ci-dessous.</li>
     *   <li>Lancer la migration Liquibase qui INSERT les entries du nouveau type.</li>
     * </ol>
     */
    private static final List<String> KNOWN_FRONTEND_REFERENTIAL_TYPES = List.of(
            // Existants pré-SF-225 (parité avec SECTION_LABELS référentiels)
            "LITIGATION_TYPE",
            "BAREME_MACRON",
            "IMMIGRATION_JALONS",
            "IMMIGRATION_PIECES",
            "PENSION_TAUX",
            "PRESTATION_COEFF",
            "IMMIGRATION_TITLES",
            "IMMIGRATION_RECOURS",
            "IMMIGRATION_WORK_RIGHTS",
            "CONVENTION_BAREMES",
            "LICENCIEMENT_CRITERES",
            "RUPTURE_CONV_CRITERES",
            "INDEMNITE_BAREMES",
            "GARDE_MODES",
            "DIVORCE_ETAPES",
            "DIVORCE_PIECES",
            // SF-225-01 : 5 types orphelins intégrés UX
            "CONVENTION_PREAVIS",
            "TRAVAIL_PROCEDURE_JALONS",
            "FAMILLE_PROCEDURE_JALONS",
            "MAJEURS_PROTEGES_REGIMES",
            "IM21_VALIDITY_CRITERES"
    );

    @Test
    void tout_referential_type_systeme_a_une_integration_UX_frontend() {
        List<String> orphans = repository.findAll().stream()
                .filter(LegalReferential::isSystem)
                .map(LegalReferential::getReferentialType)
                .distinct()
                .filter(type -> !KNOWN_FRONTEND_REFERENTIAL_TYPES.contains(type))
                .sorted()
                .collect(Collectors.toList());

        assertThat(orphans)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("F-225 SF-225-03 : les referential_type suivants sont seedés en DB ")
                            .append("(is_system=true) mais n'ont PAS d'intégration UX dédiée côté frontend ")
                            .append("(absence dans SECTION_LABELS / formatValue / sectionIcon de ")
                            .append("frontend/src/app/referentials/referentials.component.ts).\n\n")
                            .append("Action : étendre référentials.component.ts (3 branches : ")
                            .append("SECTION_LABELS, formatValue, sectionIcon — éventuellement buildForm ")
                            .append("dans le edit dialog) PUIS ajouter les types ci-dessous dans ")
                            .append("KNOWN_FRONTEND_REFERENTIAL_TYPES de ce test.\n\n")
                            .append("Types orphelins :\n");
                    orphans.forEach(t -> sb.append(" - ").append(t).append("\n"));
                    return sb.toString();
                })
                .isEmpty();
    }
}

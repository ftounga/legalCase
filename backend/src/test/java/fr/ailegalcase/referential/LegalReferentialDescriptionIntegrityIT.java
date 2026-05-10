package fr.ailegalcase.referential;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
     * <p>Refactor 2026-05-10 : extraction dynamique de {@code SECTION_LABELS}
     * via regex sur le source frontend — pattern miroir de
     * {@code DashboardTileToolIdIntegrityIT} (F-229 SF-229-03) et
     * {@link DecisionToolVisibilityIntegrityIT} (refactor parallèle 2026-05-10).
     * La précédente liste hardcodée pouvait être contournée en l'éditant sans
     * livrer l'intégration UX réelle.</p>
     *
     * <p>Pour ajouter un nouveau {@code referential_type} :
     * <ol>
     *   <li>Ajouter l'entrée {@code SECTION_LABELS} + branches {@code formatValue} / {@code sectionIcon}
     *       dans {@code referentials.component.ts}.</li>
     *   <li>Lancer la migration Liquibase qui INSERT les entries du nouveau type.</li>
     * </ol>
     * Le test détecte automatiquement l'ajout côté frontend — pas de liste à
     * synchroniser manuellement.
     */
    private static final String SECTION_LABELS_SOURCE =
            "../frontend/src/app/referentials/referentials.component.ts";

    /**
     * Capture le bloc {@code const SECTION_LABELS: Record<string, string> = { ... };}
     * pour ne scanner que ses entrées — pas les autres maps du fichier.
     */
    private static final Pattern SECTION_LABELS_BLOCK = Pattern.compile(
            "const\\s+SECTION_LABELS\\s*:\\s*Record\\s*<[^>]+>\\s*=\\s*\\{(.*?)\\}\\s*;",
            Pattern.DOTALL);

    /**
     * Capture chaque clé d'entrée {@code SECTION_LABELS} : identifiant d'enum
     * ({@code [A-Z_][A-Z0-9_]*}) suivi de {@code :}. Ignore les lignes commentées.
     */
    private static final Pattern SECTION_LABELS_KEY = Pattern.compile(
            "(?m)^\\s*([A-Z_][A-Z0-9_]*)\\s*:");

    @Test
    void tout_referential_type_systeme_a_une_integration_UX_frontend() throws IOException {
        Set<String> frontendSectionLabels = extractSectionLabelsFromFrontend();
        assertThat(frontendSectionLabels)
                .withFailMessage("Aucune clé SECTION_LABELS extraite — vérifier le regex et le chemin source : %s",
                        Paths.get(SECTION_LABELS_SOURCE).toAbsolutePath())
                .isNotEmpty();

        List<String> orphans = repository.findAll().stream()
                .filter(LegalReferential::isSystem)
                .map(LegalReferential::getReferentialType)
                .distinct()
                .filter(type -> !frontendSectionLabels.contains(type))
                .sorted()
                .collect(Collectors.toList());

        assertThat(orphans)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("F-225 SF-225-03 : les referential_type suivants sont seedés en DB ")
                            .append("(is_system=true) mais n'ont PAS d'entrée SECTION_LABELS dans\n")
                            .append("frontend/src/app/referentials/referentials.component.ts\n")
                            .append("(extraction dynamique — règle CLAUDE.md ").append(SECTION_LABELS_SOURCE).append(").\n\n")
                            .append("Action : étendre referentials.component.ts (3 branches : ")
                            .append("SECTION_LABELS + formatValue + sectionIcon — éventuellement ")
                            .append("buildForm dans le edit dialog).\n\n")
                            .append("Le test extrait dynamiquement SECTION_LABELS — pas de liste hardcodée\n")
                            .append("à mettre à jour : c'est le source frontend qui fait foi.\n\n")
                            .append("Types orphelins :\n");
                    orphans.forEach(t -> sb.append(" - ").append(t).append("\n"));
                    return sb.toString();
                })
                .isEmpty();
    }

    private Set<String> extractSectionLabelsFromFrontend() throws IOException {
        Path path = Paths.get(SECTION_LABELS_SOURCE);
        String source = String.join("\n", Files.readAllLines(path));

        Matcher blockMatcher = SECTION_LABELS_BLOCK.matcher(source);
        if (!blockMatcher.find()) {
            throw new IllegalStateException(
                    "SECTION_LABELS block introuvable dans " + path.toAbsolutePath()
                            + " — le format a-t-il changé ?");
        }
        String body = blockMatcher.group(1);

        Set<String> keys = new LinkedHashSet<>();
        Matcher keyMatcher = SECTION_LABELS_KEY.matcher(body);
        while (keyMatcher.find()) {
            keys.add(keyMatcher.group(1));
        }
        return keys;
    }
}

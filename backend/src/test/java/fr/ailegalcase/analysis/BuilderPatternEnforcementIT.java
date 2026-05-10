package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-234 SF-234-02 — Garde-fou empêchant la réintroduction d'un constructeur rétrocompat
 * sur les records {@code *ExtractedData} de {@link CaseAnalysisResponse}.
 *
 * <p>Motif : à chaque ajout de champ IA, l'ancien réflexe consistait à ajouter un constructeur
 * rétrocompat préservant la signature historique. Risque humain d'oubli de propagation
 * {@code false}. Avec F-234, la construction passe désormais exclusivement par le Builder
 * pattern. Ce test échoue si quelqu'un rajoute un constructeur explicite sur l'un des records.
 *
 * <p>Stratégie : lecture textuelle du source Java + regex qui matche
 * {@code public TravailExtractedData(}, {@code public ImmigrationExtractedData(} et
 * {@code public FamilleExtractedData(}. Compte le nombre d'occurrences. Comme le record canonique
 * n'écrit pas son constructeur full explicitement (il est généré par le compilateur), tout match
 * de la regex correspond à un constructeur rétrocompat — donc le compteur attendu est 0.
 *
 * <p>Ce test n'utilise pas la réflexion : la réflexion ne distingue pas le constructeur canonique
 * généré d'un constructeur explicite, alors que la lecture du source si.
 */
class BuilderPatternEnforcementIT {

    /** Liste des records à protéger. À étendre si de nouveaux records {@code *ExtractedData} sont ajoutés. */
    private static final List<String> PROTECTED_RECORDS = List.of(
            "TravailExtractedData",
            "ImmigrationExtractedData",
            "FamilleExtractedData"
    );

    private static final Path SOURCE_PATH = Path.of(
            "src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java"
    );

    @Test
    void noExplicitConstructorOnExtractedDataRecords() throws IOException {
        String source = Files.readString(SOURCE_PATH);

        for (String recordName : PROTECTED_RECORDS) {
            // Pattern : "public " + nomRecord + "("
            // Note : le constructeur compact d'un record s'écrit "public TravailExtractedData {"
            // (sans parenthèses), donc il n'est pas matché par cette regex. Seul un constructeur
            // explicite avec arguments est matché.
            Pattern pattern = Pattern.compile(
                    "\\bpublic\\s+" + Pattern.quote(recordName) + "\\s*\\("
            );
            Matcher matcher = pattern.matcher(source);

            int occurrences = 0;
            while (matcher.find()) {
                occurrences++;
            }

            assertThat(occurrences)
                    .as("Constructeurs explicites trouvés sur le record %s. " +
                            "Pas de constructeur rétrocompat — utiliser Builder pattern (F-234).", recordName)
                    .isZero();
        }
    }
}

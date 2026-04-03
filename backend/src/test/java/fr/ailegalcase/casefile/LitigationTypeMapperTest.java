package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LitigationTypeMapperTest {

    @ParameterizedTest
    @CsvSource({
        "LICENCIEMENT_SANS_CAUSE_REELLE, 1, Art. L1471-1",
        "LICENCIEMENT_ECONOMIQUE,        1, Art. L1471-1",
        "PRISE_ACTE_RUPTURE,             1, Art. L1471-1",
        "HARCELEMENT_MORAL,              5, Art. L1152-1",
        "DISCRIMINATION,                 5, Art. L1132-1",
        "HEURES_SUPPLEMENTAIRES,         3, Art. L3245-1",
        "RAPPEL_SALAIRE,                 3, Art. L3245-1",
    })
    void knownType_returnsCorrectPeriodAndArticle(String type, int expectedYears, String expectedArticle) {
        Optional<LitigationTypeMapper.LitigationPeriod> result = LitigationTypeMapper.resolve(type);
        assertThat(result).isPresent();
        assertThat(result.get().years()).isEqualTo(expectedYears);
        assertThat(result.get().article()).isEqualTo(expectedArticle);
    }

    @Test
    void unknownType_returnsEmpty() {
        assertThat(LitigationTypeMapper.resolve("RUPTURE_CONVENTIONNELLE")).isEmpty();
    }

    @Test
    void nullType_returnsEmpty() {
        assertThat(LitigationTypeMapper.resolve(null)).isEmpty();
    }

    @Test
    void lowercaseType_isNormalized() {
        assertThat(LitigationTypeMapper.resolve("licenciement_sans_cause_reelle")).isPresent();
    }
}

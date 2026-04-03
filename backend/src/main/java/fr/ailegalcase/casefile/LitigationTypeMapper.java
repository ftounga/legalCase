package fr.ailegalcase.casefile;

import java.util.Optional;

/**
 * Maps a litigation type string (from AI JSON field "type_litige_detecte") to its
 * statutory limitation period, legal article reference, and human-readable label.
 */
public final class LitigationTypeMapper {

    public record LitigationPeriod(int years, String article, String label) {}

    private LitigationTypeMapper() {}

    public static Optional<LitigationPeriod> resolve(String typeDetecte) {
        if (typeDetecte == null) return Optional.empty();
        return switch (typeDetecte.trim().toUpperCase()) {
            case "LICENCIEMENT_SANS_CAUSE_REELLE" -> Optional.of(
                    new LitigationPeriod(1, "Art. L1471-1", "Licenciement sans cause réelle et sérieuse"));
            case "LICENCIEMENT_ECONOMIQUE" -> Optional.of(
                    new LitigationPeriod(1, "Art. L1471-1", "Licenciement économique"));
            case "PRISE_ACTE_RUPTURE" -> Optional.of(
                    new LitigationPeriod(1, "Art. L1471-1", "Prise d'acte de rupture"));
            case "HARCELEMENT_MORAL" -> Optional.of(
                    new LitigationPeriod(5, "Art. L1152-1", "Harcèlement moral"));
            case "DISCRIMINATION" -> Optional.of(
                    new LitigationPeriod(5, "Art. L1132-1", "Discrimination"));
            case "HEURES_SUPPLEMENTAIRES" -> Optional.of(
                    new LitigationPeriod(3, "Art. L3245-1", "Heures supplémentaires"));
            case "RAPPEL_SALAIRE" -> Optional.of(
                    new LitigationPeriod(3, "Art. L3245-1", "Rappel de salaire"));
            default -> Optional.empty();
        };
    }
}

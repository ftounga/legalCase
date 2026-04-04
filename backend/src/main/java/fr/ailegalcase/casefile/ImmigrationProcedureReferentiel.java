package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique des jalons procéduraux pour DROIT_IMMIGRATION.
 * Chaque procédure connue est associée à une liste de jalons (label + offset en jours depuis date_depot).
 */
public final class ImmigrationProcedureReferentiel {

    public record ProcedureJalon(String label, int offsetDays) {}

    private static final Map<String, List<ProcedureJalon>> REFERENTIEL = Map.of(
            "RENOUVELLEMENT_TITRE_SEJOUR", List.of(
                    new ProcedureJalon("Délai d'instruction préfecture — renouvellement titre de séjour", 120),
                    new ProcedureJalon("Silence vaut rejet — renouvellement titre de séjour", 60)
            ),
            "DEMANDE_ASILE_OFPRA", List.of(
                    new ProcedureJalon("Convocation entretien OFPRA", 21),
                    new ProcedureJalon("Délai décision OFPRA", 180)
            ),
            "RECOURS_CNDA", List.of(
                    new ProcedureJalon("Audience CNDA", 150),
                    new ProcedureJalon("Décision CNDA", 270)
            )
    );

    private ImmigrationProcedureReferentiel() {}

    public static List<ProcedureJalon> resolve(String typeProcedure) {
        if (typeProcedure == null) return List.of();
        return REFERENTIEL.getOrDefault(typeProcedure, List.of());
    }
}

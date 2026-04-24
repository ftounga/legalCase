package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique des jalons procéduraux pour DROIT_IMMIGRATION.
 * Chaque procédure connue est associée à une liste de jalons (label + offset en jours depuis date_depot).
 * Ce référentiel est un fallback — la source de vérité est la table {@code legal_referentials}
 * de type {@code IMMIGRATION_JALONS} (seedée par migrations 049 + 108).
 */
public final class ImmigrationProcedureReferentiel {

    public record ProcedureJalon(String label, int offsetDays) {}

    private static final Map<String, List<ProcedureJalon>> REFERENTIEL = Map.ofEntries(
            // ---- Migration 049 ----
            Map.entry("RENOUVELLEMENT_TITRE_SEJOUR", List.of(
                    new ProcedureJalon("Délai d'instruction préfecture — renouvellement titre de séjour", 120),
                    new ProcedureJalon("Silence vaut rejet — renouvellement titre de séjour", 60)
            )),
            Map.entry("DEMANDE_ASILE_OFPRA", List.of(
                    new ProcedureJalon("Convocation entretien OFPRA", 21),
                    new ProcedureJalon("Délai décision OFPRA", 180)
            )),
            Map.entry("RECOURS_CNDA", List.of(
                    new ProcedureJalon("Audience CNDA", 150),
                    new ProcedureJalon("Décision CNDA", 270)
            )),
            Map.entry("REGULARISATION_EXCEPTIONNELLE", List.of(
                    new ProcedureJalon("Instruction préfecture — régularisation exceptionnelle", 180),
                    new ProcedureJalon("Silence vaut rejet — régularisation exceptionnelle", 120)
            )),
            // ---- Migration 108 (SF-IM-16-01) ----
            Map.entry("REGROUPEMENT_FAMILIAL", List.of(
                    new ProcedureJalon("Instruction OFII — regroupement familial", 180),
                    new ProcedureJalon("Silence vaut rejet — regroupement familial", 180)
            )),
            Map.entry("NATURALISATION_DECRET", List.of(
                    new ProcedureJalon("Instruction sous-préfecture — naturalisation par décret", 540),
                    new ProcedureJalon("Silence vaut rejet — naturalisation", 540)
            )),
            Map.entry("CHANGEMENT_STATUT", List.of(
                    new ProcedureJalon("Instruction préfecture — changement de statut", 120),
                    new ProcedureJalon("Silence vaut rejet — changement de statut", 120)
            )),
            Map.entry("AES_SALARIE", List.of(
                    new ProcedureJalon("Instruction préfecture — admission exceptionnelle au séjour (salarié)", 180),
                    new ProcedureJalon("Silence vaut rejet — AES salarié", 180)
            )),
            Map.entry("OQTF_AVEC_DELAI", List.of(
                    new ProcedureJalon("Expiration du délai de départ volontaire (30 j) — OQTF", 30),
                    new ProcedureJalon("Audience tribunal administratif — recours OQTF", 90),
                    new ProcedureJalon("Décision tribunal administratif — recours OQTF", 180)
            )),
            Map.entry("OQTF_SANS_DELAI", List.of(
                    new ProcedureJalon("Audience tribunal administratif (JLD) — recours OQTF sans DDV", 4),
                    new ProcedureJalon("Décision tribunal administratif (JLD) — recours OQTF sans DDV", 7)
            ))
    );

    private ImmigrationProcedureReferentiel() {}

    public static List<ProcedureJalon> resolve(String typeProcedure) {
        if (typeProcedure == null) return List.of();
        return REFERENTIEL.getOrDefault(typeProcedure, List.of());
    }
}

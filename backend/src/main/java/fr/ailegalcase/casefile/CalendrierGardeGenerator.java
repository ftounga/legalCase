package fr.ailegalcase.casefile;

/**
 * Génère un calendrier de garde structuré à partir du mode choisi et des noms des parents.
 */
public final class CalendrierGardeGenerator {

    private CalendrierGardeGenerator() {}

    public static CalendrierGardeResult generate(String gardeCode, String parentANom, String parentBNom) {
        return generate(GardeModeReferentiel.getByCode(gardeCode), gardeCode, parentANom, parentBNom);
    }

    public static CalendrierGardeResult generate(GardeMode mode, String gardeCode, String parentANom, String parentBNom) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode de garde inconnu : " + gardeCode);
        }

        int joursA, joursB;
        String commentaire;

        switch (mode.repartitionType()) {
            case "ALTERNEE_1_SUR_2" -> {
                joursA = 182;
                joursB = 183;
                commentaire = "Répartition égalitaire : ~50/50. " + parentANom + " et " + parentBNom
                        + " hébergent l'enfant en alternance une semaine sur deux.";
            }
            case "DVH_CLASSIQUE" -> {
                joursA = 249;
                joursB = 116;
                commentaire = parentANom + " (résidence principale) héberge l'enfant ~68% du temps. "
                        + parentBNom + " (DVH) héberge ~32% du temps (WE + mercredi + moitié vacances).";
            }
            case "DVH_ELARGI" -> {
                joursA = 220;
                joursB = 145;
                commentaire = parentANom + " (résidence principale) héberge l'enfant ~60% du temps. "
                        + parentBNom + " (DVH élargi) héberge ~40% du temps.";
            }
            default -> {
                joursA = 182;
                joursB = 183;
                commentaire = "Répartition à déterminer.";
            }
        }

        return new CalendrierGardeResult(
                mode.code(), mode.label(), mode.country(),
                parentANom, parentBNom,
                mode.repartitionType(),
                mode.periodesParentA(), mode.periodesParentB(),
                mode.vacancesRegle(),
                joursA, joursB,
                mode.baseJuridique(), commentaire
        );
    }
}

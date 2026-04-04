package fr.ailegalcase.analysis;

public final class LegalDomainPromptBuilder {

    private static final String FAMILLE_INSTRUCTION = """

            Pour ce dossier de droit de la famille, inclure également dans le JSON le champ suivant :
            "pension_alimentaire_data" : objet avec les champs :
              "revenus_net_mensuel_debiteur" : revenu net mensuel du parent débiteur en euros, null si non détectable.
              "revenus_net_mensuel_creancier" : revenu net mensuel du parent créancier en euros, null si non détectable.
              "nb_enfants" : nombre d'enfants concernés par la pension, null si non détectable.
              "mode_garde" : mode de garde, l'une de ces valeurs exactes : "EXCLUSIVE", "ALTERNEE", null si non détectable.
              "pays_applicable" : pays du barème applicable, l'une de ces valeurs exactes : "FRANCE", "BELGIQUE", null si non détectable.
            """;

    private static final String IMMIGRATION_INSTRUCTION = """

            Pour ce dossier de droit de l'immigration, inclure également dans le JSON les champs suivants :
            "date_expiration_titre" : date d'expiration du titre de séjour au format YYYY-MM-DD, null si non détectable.
            "type_titre_sejour" : type du titre de séjour (ex: "CARTE_RESIDENT", "TITRE_SEJOUR_TEMPORAIRE"), null si non détectable.
            "type_procedure_detectee" : type de procédure administrative en cours, l'une de ces valeurs exactes : "RENOUVELLEMENT_TITRE_SEJOUR", "DEMANDE_ASILE_OFPRA", "RECOURS_CNDA", null si non détectable.
            "date_depot_procedure" : date de dépôt de la demande ou du recours au format YYYY-MM-DD, null si non détectable.
            """;

    private LegalDomainPromptBuilder() {}

    /**
     * Construit la description du domaine juridique selon le domaine et le pays.
     * Ex : "droit du travail français", "droit de l'immigration belge"
     */
    public static String domainLabel(String legalDomain, String country) {
        String domainPart = switch (legalDomain) {
            case "DROIT_IMMIGRATION" -> "droit de l'immigration";
            case "DROIT_FAMILLE"     -> "droit de la famille";
            default                  -> "droit du travail"; // DROIT_DU_TRAVAIL
        };

        boolean isFeminine = "DROIT_IMMIGRATION".equals(legalDomain);
        String countryAdjective = switch (country) {
            case "BELGIQUE" -> isFeminine ? "belge" : "belge";
            default         -> isFeminine ? "française" : "français"; // FRANCE
        };

        return domainPart + " " + countryAdjective;
    }

    public static String domainSpecificInstruction(String legalDomain) {
        if ("DROIT_IMMIGRATION".equals(legalDomain)) return IMMIGRATION_INSTRUCTION;
        if ("DROIT_FAMILLE".equals(legalDomain))     return FAMILLE_INSTRUCTION;
        return "";
    }
}

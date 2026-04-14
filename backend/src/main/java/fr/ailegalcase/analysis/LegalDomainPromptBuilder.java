package fr.ailegalcase.analysis;

public final class LegalDomainPromptBuilder {

    private static final String FAMILLE_INSTRUCTION = """

            Pour ce dossier de droit de la famille, inclure également dans le JSON les champs suivants :
            "pension_alimentaire_data" : objet avec les champs :
              "revenus_net_mensuel_debiteur" : revenu net mensuel du parent débiteur en euros, null si non détectable.
              "revenus_net_mensuel_creancier" : revenu net mensuel du parent créancier en euros, null si non détectable.
              "nb_enfants" : nombre d'enfants concernés par la pension, null si non détectable.
              "mode_garde" : mode de garde binaire (pour le calcul de pension), l'une de ces valeurs exactes : "EXCLUSIVE", "ALTERNEE", null si non détectable.
              "mode_garde_detaille" : mode de garde précis pour le calendrier F-FA-06, l'une de ces valeurs exactes (null si non déterminable avec certitude) : "ALTERNEE_FR" (résidence alternée 1 semaine sur 2 en France), "DVH_CLASSIQUE_FR" (droit de visite et hébergement classique 1 week-end sur 2 en France), "DVH_ELARGI_FR" (DVH élargi avec mercredi après-midi en France), "ALTERNEE_BE" (hébergement égalitaire en Belgique), "SECONDAIRE_BE" (hébergement secondaire classique en Belgique), "SECONDAIRE_ELARGI_BE" (hébergement secondaire élargi en Belgique). Si l'information ne permet pas de choisir parmi ces 6 valeurs, laisser null — le champ "mode_garde" binaire suffit pour la pension.
              "pays_applicable" : pays du barème applicable, l'une de ces valeurs exactes : "FRANCE", "BELGIQUE", null si non détectable.
            "prestation_compensatoire_data" : objet avec les champs :
              "revenus_net_mensuel_epoux_a" : revenu net mensuel de l'époux A en euros, null si non détectable.
              "revenus_net_mensuel_epoux_b" : revenu net mensuel de l'époux B en euros, null si non détectable.
              "duree_mariage_annees" : durée du mariage en années (entier), null si non détectable.
              "nb_enfants_charge" : nombre d'enfants à charge communs, null si non détectable.
              "pays_applicable" : pays applicable, l'une de ces valeurs exactes : "FRANCE", "BELGIQUE", null si non détectable.
            "liquidation_communaute_data" : objet avec les champs :
              "regime_matrimonial" : régime matrimonial détecté, l'une de ces valeurs exactes : "COMMUNAUTE_LEGALE", "SEPARATION_BIENS", "PARTICIPATION_ACQUETS", null si non détectable.
              "actif_commun" : tableau d'objets {libelle: string, valeur_estimee: number|null} listant les biens communs détectés dans les documents.
              "biens_propres_epoux_a" : tableau d'objets {libelle: string, valeur_estimee: number|null} listant les biens propres de l'époux A.
              "biens_propres_epoux_b" : tableau d'objets {libelle: string, valeur_estimee: number|null} listant les biens propres de l'époux B.
              "passif_commun" : tableau d'objets {libelle: string, montant: number|null} listant les dettes communes détectées.
            """;

    private static final String TRAVAIL_INSTRUCTION = """

            Pour ce dossier de droit du travail, inclure également dans le JSON les champs suivants :
            "travail_extracted_data" : objet avec les champs :
              "convention_collective" : identifiant de la convention collective détectée (ex: "METALLURGIE", "SYNTEC", "BTP", "HCR", "COMMERCE", "CP200", "CP124", "CP302"), null si non détectable.
              "date_entree" : date d'entrée dans l'entreprise au format YYYY-MM-DD, null si non détectable.
              "salaire_brut_mensuel" : salaire brut mensuel en euros (nombre), null si non détectable.
              "type_contrat" : type de contrat détecté ("CDI", "CDD", "INTERIM"), null si non détectable.
              "poste" : intitulé du poste occupé, null si non détectable.
              "motif_licenciement" : motif du licenciement si détecté (texte libre), null si non détectable.
              "date_licenciement" : date du licenciement ou de la rupture au format YYYY-MM-DD, null si non détectable.
              "conges_contractuels" : nombre de jours de congés prévus au contrat (entier), null si non détectable.
              "prime_anciennete_contractuelle" : pourcentage de prime d'ancienneté au contrat (nombre), null si non détectable.
            "licenciement_validity_detection" : objet qui, à partir des documents, évalue chaque critère de validité du licenciement. Chaque valeur est un objet {reponse, justification} où reponse est l'une de "OUI", "NON", "INCONNU" (utiliser "INCONNU" si les documents ne permettent pas de trancher) et justification est une courte phrase (≤ 500 caractères) citant le document ou l'extrait qui soutient la réponse (chaîne vide acceptée). Champs attendus :
              "FR_CONVOCATION" : lettre de convocation à entretien préalable remise/envoyée avec délai minimal de 5 jours ouvrables.
              "FR_ENTRETIEN" : entretien préalable effectivement tenu, avec information sur le droit d'être assisté.
              "FR_DELAI_NOTIFICATION" : notification du licenciement dans le délai légal après l'entretien (2 jours ouvrables minimum, 7 pour licenciement économique ou cadres).
              "FR_MOTIVATION" : lettre de licenciement écrite énonçant des motifs précis (pas de motif vague).
              "FR_MOTIF_REEL" : motif réel et sérieux (objectif, exact, suffisamment grave) identifiable dans les documents.
              "FR_PROCEDURE_DISCIPLINAIRE" : délai de prescription de 2 mois entre connaissance des faits et sanction respecté, pas de double sanction.
              "FR_ORDRE_LICENCIEMENT" : pour un licenciement économique, ordre des licenciements fondé sur ancienneté, situation familiale, qualités professionnelles.
              "BE_NOTIFICATION" : notification par lettre recommandée ou exploit d'huissier.
              "BE_PREAVIS" : préavis respecté selon la loi 2013 (ou indemnité compensatoire si rupture immédiate).
              "BE_MOTIVATION" : motivation conforme CCT 109 (comportement, aptitude, ou nécessité de l'entreprise).
              "BE_AUDITION" : audition préalable tenue (recommandée mais non obligatoire — "OUI" même si audition non nécessaire).
              "BE_NON_DISCRIMINATION" : absence de motif discriminatoire (genre, âge, origine, convictions, état de santé, etc.).
              "BE_PROTECTION_SPECIALE" : aucune protection spéciale violée (délégué syndical, femme enceinte, congé parental, etc.).
              "BE_INDEMNITE_MANIFESTE" : licenciement non manifestement déraisonnable au sens CCT 109 (risque d'indemnité 3 à 17 semaines).
            Pour un dossier français, remplir prioritairement les clés FR_* ; pour un dossier belge, les clés BE_*. Les clés de l'autre pays peuvent être omises ou à "INCONNU". Si aucune évaluation n'est possible, laisser l'objet vide ({}) ou omettre entièrement.
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
        if ("DROIT_DU_TRAVAIL".equals(legalDomain)) return TRAVAIL_INSTRUCTION;
        if ("DROIT_IMMIGRATION".equals(legalDomain)) return IMMIGRATION_INSTRUCTION;
        if ("DROIT_FAMILLE".equals(legalDomain))     return FAMILLE_INSTRUCTION;
        return "";
    }
}

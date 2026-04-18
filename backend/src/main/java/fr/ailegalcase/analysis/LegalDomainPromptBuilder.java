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
              "nom_salarie" : nom de famille du salarié (texte), null si non détectable.
              "prenom_salarie" : prénom du salarié (texte), null si non détectable.
              "adresse_salarie" : adresse postale du salarié (rue + code postal + ville concaténés), null si partielle non utilisable.
              "nom_employeur" : raison sociale de l'employeur (ex: "FinConsult SPRL", "Acme SAS"). Prioriser la raison sociale sur le nom commercial. Null si non détectable.
              "adresse_employeur" : adresse du siège social de l'employeur (rue + code postal + ville concaténés). Ne pas prendre l'adresse d'une succursale. Null si non détectable.
              "siret_employeur" : numéro SIREN (9 chiffres) ou SIRET (14 chiffres) pour un employeur français uniquement. Retirer espaces et points. Null pour un dossier belge ou si non détectable.
              "bce_employeur" : numéro BCE (10 chiffres) pour un employeur belge uniquement. Retirer le préfixe "BE" et les points. Null pour un dossier français ou si non détectable.
              "representant_employeur" : nom du représentant légal ou RH signataire (ex. administrateur délégué, DRH). Null si non détectable. NE JAMAIS INVENTER un SIRET/BCE/adresse — null en cas de doute.
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
            "compensation_data" : objet décrivant la rupture du contrat pour alimenter le comparateur d'indemnités F-DT-09. Obligatoire dès qu'une rupture du contrat est identifiée dans les documents (même si ancienneté/salaire inconnus). Mettre à null UNIQUEMENT si aucune rupture n'est évoquée. Champs :
              "type_rupture" : type de rupture, l'une de ces valeurs exactes (choisir celle qui reflète la réalité des pièces — lettre de licenciement > convention de rupture > décision judiciaire > allégation) :
                France : "LICENCIEMENT" (cause réelle et sérieuse, sans faute grave), "LICENCIEMENT_ECONOMIQUE", "RUPTURE_CONVENTIONNELLE", "DEMISSION", "PRISE_ACTE", "RESILIATION_JUDICIAIRE".
                Belgique : "LICENCIEMENT_ORDINAIRE" (tout licenciement côté employeur quelle que soit la motivation — économique, disciplinaire, etc. ; "manifestement déraisonnable" est une qualification CCT 109 art. 8, pas une nature de rupture, donc utilise "LICENCIEMENT_ORDINAIRE" dans ce cas-là aussi), "RUPTURE_AMIABLE", "DEMISSION".
                Ce champ est obligatoire si compensation_data est émis. Ne jamais renvoyer null ici ; si le type est incertain, choisir la valeur la plus probable au vu des pièces, jamais omettre.
              "anciennete_annees" : ancienneté (entier), null si non détectable.
              "anciennete_mois" : ancienneté mois complémentaire (entier), null si non détectable.
              "salaire_reference_mensuel" : salaire brut mensuel moyen de référence (décimal), null si non détectable.
              En cas de ruptures multiples, retenir la plus récente (celle qui fonde la saisine actuelle).
              Exemple positif : une lettre de licenciement pour faute simple → {"type_rupture": "LICENCIEMENT", ...}. Exemple négatif : dossier harcèlement sans rupture évoquée → "compensation_data": null.
            "rupture_conv_validity_detection" : objet à ÉMETTRE UNIQUEMENT si compensation_data.type_rupture vaut "RUPTURE_CONVENTIONNELLE". Évalue les 6 critères de validité de la rupture conventionnelle (art. L1237-11 s.). Chaque valeur est un objet {reponse, justification} où reponse vaut "OUI" / "NON" / "INCONNU" et justification (≤ 500 caractères) cite brièvement le document ou l'indice qui soutient la réponse. Champs attendus :
              "RC_CONSENTEMENT" : consentement libre et éclairé. "OUI" si aucun élément n'évoque pression, dol, menace, erreur. "NON" si vice du consentement évoqué dans les pièces. "INCONNU" sinon.
              "RC_DELAI_RETRACTATION" : délai de 15 jours calendaires respecté entre signature de la convention et demande d'homologation. "OUI" si ≥ 15 jours, "NON" si < 15 jours, "INCONNU" si dates manquantes.
              "RC_HOMOLOGATION" : homologation par la DREETS (ex-DIRECCTE) obtenue ou réputée acquise. "OUI" si pièce d'homologation présente, "NON" si refus documenté, "INCONNU" à défaut.
              "RC_ASSISTANCE" : assistance possible documentée (avocat, conseiller du salarié, représentant du personnel). "OUI" si documentée, "NON" si l'employeur l'a refusée, "INCONNU" sinon.
              "RC_INDEMNITE" : indemnité spécifique supérieure ou égale à l'indemnité légale de licenciement. "OUI" si les deux montants sont connus et indemnité spécifique ≥ légale. "NON" si strictement inférieure. "INCONNU" si un montant manque.
              "RC_ENTRETIENS" : au moins un entretien préalable tenu et documenté (compte-rendu ou correspondance). "OUI" si trace écrite, "NON" si aucune, "INCONNU" sinon.
            """;

    private static final String IMMIGRATION_INSTRUCTION = """

            Pour ce dossier de droit de l'immigration, inclure également dans le JSON les champs suivants :
            "date_expiration_titre" : date d'expiration du titre de séjour au format YYYY-MM-DD, null si non détectable.
            "type_titre_sejour" : type du titre de séjour en texte libre (ex: "Carte de résident", "Titre de séjour temporaire"), null si non détectable.
            "type_titre_sejour_code" : code normalisé du titre de séjour détecté, l'un des 16 codes exacts (null si non déterminable avec certitude) : "VLS_TS_ETUDIANT", "VLS_TS_SALARIE", "CST_SALARIE", "CARTE_PLURIANNUELLE", "CARTE_RESIDENT", "APS", "CST_VPF", "RECEPISSE_ASILE" (France), "CARTE_A_TRAVAIL", "CARTE_A_ETUDES", "CARTE_A_FAMILLE", "CARTE_B", "CARTE_C", "PERMIS_UNIQUE", "ANNEXE_15", "ATTESTATION_IMMATRICULATION" (Belgique). Si le pays ou le type ne correspond pas à cette liste, laisser null — le champ "type_titre_sejour" libre suffit pour l'affichage.
            "nationalite_ue" : booléen — true si le ressortissant est citoyen de l'Union européenne, EEE (Norvège/Islande/Liechtenstein) ou Suisse ; false s'il est ressortissant d'un pays tiers ; null si non déterminable.
            "type_procedure_detectee" : type de procédure administrative en cours, l'une de ces valeurs exactes : "RENOUVELLEMENT_TITRE_SEJOUR", "DEMANDE_ASILE_OFPRA", "RECOURS_CNDA", null si non détectable.
            "date_depot_procedure" : date de dépôt de la demande ou du recours au format YYYY-MM-DD, null si non détectable.
            "type_recours_code" : code normalisé du recours à former ou déjà formé, l'un des 6 codes exacts (null si non déterminable avec certitude) : "RECOURS_GRACIEUX_PREFET", "RECOURS_CONTENTIEUX_TA", "RECOURS_CNDA" (France), "RECOURS_CGRA", "RECOURS_CCE", "RECOURS_CE_BELGIQUE" (Belgique). Ce champ complète "type_procedure_detectee" avec la granularité nécessaire à F-IM-06 (générateur de recours). Si la procédure est un renouvellement ou une demande d'asile OFPRA (pas un recours), laisser null.
            "date_notification_decision_contestee" : date de la décision administrative que l'on attaque par recours (refus de titre, rejet OFPRA, etc.), format YYYY-MM-DD, null si non détectable. À ne pas confondre avec "date_depot_procedure" (date de dépôt effectif du recours) ni "date_expiration_titre".
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

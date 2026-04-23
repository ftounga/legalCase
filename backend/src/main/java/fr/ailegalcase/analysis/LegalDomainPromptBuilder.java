package fr.ailegalcase.analysis;

public final class LegalDomainPromptBuilder {

    private static final String FAMILLE_INSTRUCTION = """

            ========== RÈGLE CRITIQUE DE CLASSIFICATION — À APPLIQUER EN PREMIER ==========
            Identifier le MÉCANISME FACTUEL (pièce signée / décision notifiée),
            JAMAIS la qualification demandée dans les arguments d'une partie.

            Exemples critiques :
            - Contrat de mariage signé (séparation de biens, participation aux acquêts) +
              arguments de la partie pour requalifier en communauté légale de fait →
              regime_matrimonial reste SEPARATION_BIENS (ou PARTICIPATION_ACQUETS) tant
              que le juge n'a pas requalifié ; la convention existe factuellement.
            - Mode de garde actuel acté dans une ordonnance provisoire vs demande de
              modification → mode_garde_detaille reflète l'ORDONNANCE EN COURS, pas la
              demande en cours d'instruction.
            - Convention de divorce signée mais non homologuée → elle sert d'indice
              sur le régime choisi mais la situation procédurale peut rester incertaine.
            ================================================================================

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
            "divorce_consentement_validity_detection" (F-152) : objet avec une clé par critère de validité du divorce par consentement mutuel (art. 229 Code civil). Inclure UNIQUEMENT les clés suivantes (7 codes exacts) quand l'indice textuel est présent ; omettre complètement les clés dont l'information ne peut être établie. Chaque valeur : {"reponse": "OUI" | "NON" | "INCONNU", "justification": phrase courte citant l'indice textuel du dossier, max 500 car.}. Codes :
              "DC_MAJORITE" : les deux époux sont majeurs. "OUI" si leurs dates de naissance montrent la majorité à la date de signature, "NON" si un époux est mineur, "INCONNU" si les âges ne sont pas détectables.
              "DC_CONSENTEMENT_LIBRE" : absence de vice du consentement (pression, dol, violences conjugales récentes non purgées). "OUI" par défaut si aucun indice de vice ; "NON" si violences conjugales en cours, pression documentée, trouble majeur du consentement ; "INCONNU" si la question n'est pas abordée.
              "DC_CONVENTION_EQUITABLE" : convention qui ne lèse manifestement aucun époux (liquidation équilibrée, pension et prestation proportionnées). "OUI" si aucun déséquilibre notable ; "NON" si un époux renonce à sa part sans contrepartie apparente, ou si l'écart de revenus crée une iniquité flagrante non compensée ; "INCONNU" si la convention n'est pas détaillée.
              "DC_ENFANT_MINEUR_ENTENDU" : si au moins un enfant mineur ≥ âge de discernement (environ 10-12 ans), il a été informé de son droit à être entendu (art. 388-1 Cciv). "OUI" si une mention explicite figure dans les pièces ; "NON" si le formulaire obligatoire manque alors que l'enfant a l'âge requis ; "INCONNU" s'il n'y a pas d'enfant mineur OU si l'information est absente.
              "DC_DELAI_REFLEXION_15J" : délai de 15 jours calendaires entre réception de la convention par l'époux et sa signature (art. 229-4 Cciv). "OUI" si les deux dates figurent et l'intervalle ≥ 15 jours ; "NON" si < 15 jours ; "INCONNU" si au moins une date manque.
              "DC_NOTAIRE_DEPOT" : convention déposée chez un notaire pour acquisition de la date certaine (art. 229-1 Cciv). "OUI" si un acte de dépôt notarial est documenté ; "NON" si explicitement non fait ou si le mode de divorce est autre (contentieux) ; "INCONNU" si la procédure n'en est pas encore là.
              "DC_INDEPENDANCE_AVOCATS" : chaque époux a son propre avocat (pas d'avocat unique). "OUI" si deux avocats distincts sont identifiés ; "NON" si un seul avocat signe pour les deux (cas de nullité de droit) ; "INCONNU" si les avocats ne sont pas identifiables dans les pièces.
            """;

    private static final String TRAVAIL_INSTRUCTION = """

            ========== RÈGLE CRITIQUE DE CLASSIFICATION — À APPLIQUER EN PREMIER ==========
            Avant toute autre analyse, identifie le MÉCANISME FACTUEL de la rupture
            (ce qui s'est réellement passé, attesté par une pièce signée/notifiée),
            JAMAIS la qualification juridique demandée dans les arguments d'une partie.

            Procédure OBLIGATOIRE de classification (dans cet ordre strict) :
            1. Existe-t-il dans les documents une CONVENTION DE RUPTURE signée par les deux
               parties ? → OUI = "RUPTURE_CONVENTIONNELLE" (ne cherche pas plus loin, même
               si le dossier argumente la nullité pour obtenir une requalification en
               licenciement sans cause réelle). Les vices de la RC (pas d'homologation,
               délai rétractation non respecté, consentement vicié, etc.) alimenteront
               "rupture_conv_validity_detection" avec "NON" justifiés — jamais type_rupture.
            2. Existe-t-il une LETTRE DE LICENCIEMENT notifiée par l'employeur ? →
               "LICENCIEMENT" (ou "LICENCIEMENT_ECONOMIQUE" si motif économique explicite).
            3. Le salarié a-t-il pris acte de la rupture ? → "PRISE_ACTE".
            4. Y a-t-il une demande de résiliation judiciaire en cours ? → "RESILIATION_JUDICIAIRE".
            5. Lettre de démission du salarié ? → "DEMISSION".
            6. Aucun de ces faits mais une rupture est évoquée ? → Choisir la valeur la
               plus probable au vu des pièces ; ne jamais omettre.

            PIÈGE FRÉQUENT À ÉVITER : un dossier où le salarié conteste une RC en demandant
            sa REQUALIFICATION en licenciement sans cause réelle comporte souvent de
            nombreuses mentions de "licenciement" dans les conclusions/arguments. Ces
            mentions sont des DEMANDES JURIDIQUES, pas des faits. type_rupture reste
            "RUPTURE_CONVENTIONNELLE" tant que la RC n'a pas été annulée par un juge.
            ================================================================================

            Pour ce dossier de droit du travail, inclure également dans le JSON les champs suivants :
            "travail_extracted_data" : objet avec les champs :
              "convention_collective" : code identifiant la convention collective détectée. FORMAT ATTENDU : numéro IDCC officiel sur 4 chiffres préfixé par "IDCC_" (ex: "IDCC_3043" pour Propreté, "IDCC_3248" pour Métallurgie, "IDCC_1486" pour Syntec, "IDCC_2216" pour Commerce de détail). Pour la Belgique, utiliser le format "CP{numéro}" (ex: "CP200"). Si la convention est identifiable mais que son numéro IDCC exact n'est pas certain, retourner un code descriptif en MAJUSCULES (ex: "NETTOYAGE"). Retourner null si aucune convention n'est détectable.
              "date_entree" : date d'entrée dans l'entreprise au format YYYY-MM-DD, null si non détectable.
              "salaire_brut_mensuel" : salaire brut mensuel en euros (nombre). Si les documents mentionnent UNIQUEMENT un salaire net (pas de brut explicite), applique la conversion approximative brut ≈ net × 1,30 (moyenne FR non-cadre) et positionne "salaire_est_deduit": true. Si brut explicite détecté → omets "salaire_est_deduit" ou le positionnes à false. Null uniquement si AUCUN salaire (ni brut ni net) n'est détectable.
              "salaire_est_deduit" : booléen optionnel (true/false/null). True quand salaire_brut_mensuel a été déduit d'un net via la conversion × 1,30. False ou null sinon.
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
              "type_rupture" : type de rupture, l'une de ces valeurs exactes. RÈGLE IMPÉRATIVE : le type reflète le MÉCANISME FACTUEL de la rupture (ce qui s'est passé, attesté par les pièces), JAMAIS la qualification demandée en justice.
                Exemples critiques :
                - Convention de rupture signée par les deux parties + arguments de nullité pour obtenir une requalification en licenciement sans cause réelle → "RUPTURE_CONVENTIONNELLE" (la RC existe factuellement, la requalification est une conséquence juridique visée, pas un fait). Les vices identifiés (homologation absente, délai rétractation non respecté, consentement vicié, etc.) alimenteront "rupture_conv_validity_detection" avec réponses "NON" justifiées.
                - Prise d'acte ou résiliation judiciaire demandée par le salarié alors que le contrat est encore en cours → "PRISE_ACTE" ou "RESILIATION_JUDICIAIRE" selon la voie choisie, pas "LICENCIEMENT".
                - Lettre de licenciement notifiée, peu importe les arguments de contestation → "LICENCIEMENT" (ou "LICENCIEMENT_ECONOMIQUE").
                Priorité factuelle (du plus fort au plus faible) : pièce signée/notifiée engageant juridiquement > décision administrative (DREETS, CSE) > décision judiciaire > allégation.
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

            ========== RÈGLE CRITIQUE DE CLASSIFICATION — À APPLIQUER EN PREMIER ==========
            Identifier le MÉCANISME FACTUEL (pièce signée / décision notifiée),
            JAMAIS la qualification demandée dans les arguments d'une partie.

            Exemples critiques :
            - Refus OFPRA notifié + arguments pour recours CNDA non encore déposé →
              type_procedure_detectee = DEMANDE_ASILE_OFPRA (état actuel), type_recours_code
              reste null tant que le recours n'est pas formellement introduit (date de dépôt).
              Une fois le recours déposé : type_procedure_detectee = RECOURS_CNDA.
            - Titre de séjour temporaire en cours de validité + demande de renouvellement
              déposée → type_titre_sejour_code reflète le TITRE ACTUEL (ex. VLS_TS_SALARIE),
              pas le titre demandé. type_procedure_detectee = RENOUVELLEMENT_TITRE_SEJOUR.
            - OQTF reçue + arguments pour recours contentieux TA non encore introduit →
              type_procedure_detectee reste null (ou DEMANDE_ASILE_OFPRA selon contexte) ;
              type_recours_code = RECOURS_CONTENTIEUX_TA seulement si le recours est déposé
              ou immédiatement sur le point de l'être (pièce rédigée au dossier).
            ================================================================================

            Pour ce dossier de droit de l'immigration, inclure également dans le JSON les champs suivants :
            "date_expiration_titre" : date d'expiration du titre de séjour au format YYYY-MM-DD, null si non détectable.
            "type_titre_sejour" : type du titre de séjour en texte libre (ex: "Carte de résident", "Titre de séjour temporaire"), null si non détectable.
            "type_titre_sejour_code" : code normalisé du titre de séjour détecté, l'un des 16 codes exacts (null si non déterminable avec certitude) : "VLS_TS_ETUDIANT", "VLS_TS_SALARIE", "CST_SALARIE", "CARTE_PLURIANNUELLE", "CARTE_RESIDENT", "APS", "CST_VPF", "RECEPISSE_ASILE" (France), "CARTE_A_TRAVAIL", "CARTE_A_ETUDES", "CARTE_A_FAMILLE", "CARTE_B", "CARTE_C", "PERMIS_UNIQUE", "ANNEXE_15", "ATTESTATION_IMMATRICULATION" (Belgique). Si le pays ou le type ne correspond pas à cette liste, laisser null — le champ "type_titre_sejour" libre suffit pour l'affichage.
            "nationalite_ue" : booléen — true si le ressortissant est citoyen de l'Union européenne, EEE (Norvège/Islande/Liechtenstein) ou Suisse ; false s'il est ressortissant d'un pays tiers ; null si non déterminable.
            "type_procedure_detectee" : type de procédure administrative en cours, l'une de ces valeurs exactes : "RENOUVELLEMENT_TITRE_SEJOUR", "DEMANDE_ASILE_OFPRA", "RECOURS_CNDA", null si non détectable.
            "date_depot_procedure" : date de dépôt de la demande ou du recours au format YYYY-MM-DD, null si non détectable.
            "type_recours_code" : code normalisé du recours à former ou déjà formé, l'un des 6 codes exacts (null si non déterminable avec certitude) : "RECOURS_GRACIEUX_PREFET", "RECOURS_CONTENTIEUX_TA", "RECOURS_CNDA" (France), "RECOURS_CGRA", "RECOURS_CCE", "RECOURS_CE_BELGIQUE" (Belgique). Ce champ complète "type_procedure_detectee" avec la granularité nécessaire à F-IM-06 (générateur de recours). Si la procédure est un renouvellement ou une demande d'asile OFPRA (pas un recours), laisser null.
            "date_notification_decision_contestee" : date de la décision administrative que l'on attaque par recours (refus de titre, rejet OFPRA, etc.), format YYYY-MM-DD, null si non détectable. À ne pas confondre avec "date_depot_procedure" (date de dépôt effectif du recours) ni "date_expiration_titre".
            "trigger_events" : tableau (éventuellement vide) des événements factuels identifiés dans les pièces qui ouvrent un nouveau droit de séjour (F-150). Chaque élément : {"event_code" : l'un des 10 codes exacts (null si l'événement détecté ne correspond à aucun de ces codes) : "MARIAGE_RESSORTISSANT_FR" (mariage avec Français), "PACS_RESSORTISSANT_FR" (PACS avec Français, communauté de vie > 1 an), "NAISSANCE_ENFANT_FR" (naissance enfant français), "DOCTORAT_OBTENU" (doctorat obtenu en France), "CDI_OBTENU_SALARIE" (CDI signé), "ENTREE_LEGALE_10ANS" (10 ans de titre régulier), "VIOLENCES_CONJUGALES_CONSTATEES" (conjoint victime), "DEMANDE_ASILE_ACCORDEE_OFPRA" (réfugié OFPRA/CNDA), "ENFANT_NE_FR_13ANS_PRESENCE" (né FR + résidence depuis 13 ans), "REGROUPEMENT_FAMILIAL_AUTORISE" ; "event_date" : date factuelle de l'événement YYYY-MM-DD (celle du mariage / doctorat / CDI, pas d'une date de raisonnement), null si inconnue ; "source_document" : nom du document support (ex. "acte_mariage.pdf"), null si non identifiable ; "justification" : phrase courte expliquant l'indice factuel (ex. "Mariage célébré le 15/03/2025 mentionné dans l'acte n°127 de la Mairie du 5ème")}. N'inventer AUCUN événement non directement mentionné dans les pièces. Tableau vide [] si aucun événement déclencheur détecté — c'est le cas attendu pour la plupart des dossiers de renouvellement simple.
            "strategy_scenarios" : tableau (2 à 3 éléments OU vide) de scenarii stratégiques alternatifs quand plusieurs voies juridiques sont ouvertes (F-151). Chaque élément : {"scenario_label" : titre court (ex. "Changement de statut immédiat", "Attendre expiration du titre étudiant") ; "scenario_description" : explication factuelle 1-2 phrases ; "base_legale" : texte CESEDA applicable (ex. "Art. L.423-1 CESEDA") ; "target_title_code" : code titre cible ou null ; "target_title_label" : libellé du titre cible ou alternative en texte libre ; "delay_days_estimate" : fourchette de délai de traitement en jours (ex. "90-180"), null si inconnue ; "risk_level" : "FAIBLE" / "MOYEN" / "ELEVE" (risque d'échec) ; "risk_justification" : phrase courte expliquant le niveau de risque ; "required_additional_pieces" : liste de pièces à ajouter au dossier ; "advantages" : liste de points forts de ce scénario ; "drawbacks" : liste d'inconvénients}. Ne produire un tableau que si la situation offre GENUINEMENT plusieurs voies — sinon tableau vide. 2 ou 3 scenarii maximum, contrastés, pas 2 variantes quasi-identiques.
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

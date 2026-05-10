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
            "famille_extracted_data" : objet contenant les flags de détection Famille FR/BE pour le panneau décisionnel F-IA-04. Tous les flags sont booléens avec valeur par défaut false ; ne mettre true QUE si un indice factuel concret est documenté dans les pièces.
            F-202 — 5 flags décisionnels niveau 3 (BE), BELGIQUE UNIQUEMENT. Chaque flag est un booléen avec valeur par défaut false ; ne mettre true QUE si un indice factuel concret est documenté dans les pièces. Pour un dossier famille FRANCE, TOUS ces 5 flags BE DOIVENT rester false (les régimes FR équivalents — divorce par consentement mutuel art. 229 Cciv, divorce contentieux 3 voies, PACS, donations FR — sont gérés séparément ou couverts par F-200).
              "divorce_dc_envisage" : booléen — true uniquement si le dossier évoque une procédure de divorce par consentement mutuel BE au sens du CJ art. 1287+ et de la Loi 27/04/2007 (mots-clés "consentement mutuel", "DC", "accord total entre époux", "convention préalable", "deux comparutions devant juge famille"). False par défaut. Pertinent pour les futurs outils `dc-be-conditions-recevabilite`, `dc-be-convention-prealable`, `dc-be-procedure-comparutions`.
              "divorce_ddi_envisage" : booléen — true uniquement si le dossier évoque une procédure de divorce pour désunion irrémédiable BE au sens du CC art. 229 § 1 ou § 3 et du CJ art. 1255 § 1 ou § 2 (mots-clés "désunion irrémédiable", "DDI", "séparation de N mois", "séparation 1 an", "faits constitutifs de mésentente persistante", "preuves de mésentente"). Couvre les 3 voies du DDI : séparation prouvée 6 mois consensuelle, séparation prouvée 1 an unilatérale, faits constitutifs sans délai. False par défaut. Pertinent pour `F-FA-11-desunion-irremediable-be` (existant) et futurs outils `ddi-be-separation-1an-unilaterale`, `ddi-be-faits-preuves`.
              "cohabitation_legale_be_detectee" : booléen — true uniquement si le dossier évoque la cohabitation légale BE au sens de la Loi 23/11/1998 et du CC art. 1475+ / 1476 (mots-clés "cohabitation légale", "déclaration commune devant l'officier d'état civil", "dissolution cohabitation légale", "déclaration unilatérale fin cohabitation"). Distinct du PACS français. False par défaut. Pertinent pour les futurs outils `cohabitation-legale-be-formation`, `cohabitation-legale-be-effets`, `cohabitation-legale-be-dissolution`.
              "pacte_successoral_envisage" : booléen — true uniquement si le dossier évoque un pacte successoral BE au sens du CC art. 1100/1+ (Loi 31/07/2017 réforme succession, en vigueur 01/09/2018) — pacte global ou ponctuel, renonciation héréditaire anticipée, accord patrimonial pluripartite. **Spécificité BE post-2018, aucun équivalent FR** (Cciv art. 1130 al. 2 interdit la renonciation à une succession non ouverte). False par défaut. Pertinent pour le futur outil `succession-be-pacte-successoral`.
              "kafala_recueil_detecte" : booléen — true uniquement si le dossier évoque la kafala (recueil légal en droit musulman) avec une question de reconnaissance ou d'effets en BE au sens du CDIP belge et du CC art. 343 al. 2 nouveau (qui exclut l'adoption-kafala mais admet le recueil légal via le DIP). **Spécificité BE — la France refuse l'adoption d'enfant kafala (Cciv art. 370-3), la Belgique reconnaît le recueil légal de manière plus large**. False par défaut. Pertinent pour le futur outil `kafala-be-recueil-legal`.
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
              "motif_nullite_pressenti" : code motif de nullité du licenciement pressenti au vu des pièces (dossiers FR de harcèlement / discrimination / rupture de contrat protégée). Utiliser EXCLUSIVEMENT l'une de ces 7 valeurs en majuscules (null si aucun indice clair ou dossier BE) : "DISCRIMINATION" (art. L.1132-1 CT, critère prohibé documenté), "HARCELEMENT_MORAL" (art. L.1152-3 CT, dénonciation ou témoignages), "HARCELEMENT_SEXUEL" (art. L.1153-3 CT), "RETORSION" (licenciement suite à dénonciation), "SYNDICAL" (art. L.2411-1 CT, représentant du personnel), "MATERNITE_PATERNITE" (art. L.1225-71 CT, grossesse / congé mat ou pat), "ACCIDENT_MP" (L.1226-9 CT, accident du travail ou maladie professionnelle). Ne jamais inventer : si le motif dominant n'est pas factuellement identifiable dans les pièces, laisser null.
              "origine_inaptitude_pressentie" : code origine de l'inaptitude pressentie au vu d'un avis du médecin du travail présent dans les pièces (dossiers FR d'inaptitude). Utiliser EXCLUSIVEMENT l'une de ces 3 valeurs (null si aucun avis ou dossier BE) : "ACCIDENT_TRAVAIL" (origine AT qualifiée), "MALADIE_PROFESSIONNELLE" (origine MP qualifiée), "MALADIE_ORDINAIRE" (maladie non professionnelle).
              "avis_medecin_travail_date" : date de l'avis d'inaptitude au format YYYY-MM-DD, null si aucun avis présent dans les pièces.
              "reclassement_respecte_detected" : objet {"reponse": "OUI"|"NON"|"INCONNU", "justification": phrase courte citant l'indice ≤ 500 car}. "OUI" si une recherche de reclassement est documentée (courriers aux postes disponibles, propositions refusées par le salarié). "NON" si l'employeur a déclaré explicitement l'impossibilité de reclassement sans documenter la recherche, ou si aucune recherche n'est mentionnée alors que le dossier est en inaptitude. "INCONNU" à défaut d'information.
              "heures_sup_mentionnees" : objet {"total_declarees_25pct": <entier ≥ 0>, "total_declarees_50pct": <entier ≥ 0>, "hors_contingent": <entier ≥ 0>} qui agrège les heures supplémentaires mentionnées dans les pièces (bulletins de paie, décomptes, attestations). Tous les champs nullables — chaque catégorie est indépendante. Null (ou objet vide) si aucune heure sup n'est évoquée. Dossiers BE : laisser null (la règlementation BE fonctionne par sursalaire forfaitaire, pas 25/50 %).
              SF-166-01 — 8 flags décisionnels niveau 3 (F-DT-20/21/24/30/31/33/34/35), FRANCE UNIQUEMENT. Chaque flag est un booléen avec valeur par défaut false ; ne mettre true QUE si un indice factuel concret est documenté dans les pièces. Pour un dossier travail BELGIQUE, TOUS ces 8 flags DOIVENT rester false (les régimes BE équivalents — travail au noir, clause non-concurrence BE, délégué syndical, transaction BE, Fedris, référé président tribunal travail BE, contestation ONEM — sont distincts et seront traités séparément).
              "rappel_salaire_detecte" : booléen — true uniquement si les pièces contiennent des indices factuels d'arriérés salariaux (bulletin de paie litigieux, mise en demeure pour salaire impayé, mention "rappel de salaire" / "heures non rémunérées" / "primes contractuelles non versées" / "13e mois dû"). False par défaut, y compris si aucun indice clair. Ne jamais inventer. Pertinent pour F-DT-20.
              "travail_dissimule_detecte" : booléen — true uniquement si les pièces évoquent une dissimulation au sens art. L.8221-3+ CT (heures non déclarées, absence de DPAE, fausse qualification de poste, sous-déclaration salariale documentée, témoignages de collègues, courrier d'inspection du travail). False par défaut. Pertinent pour F-DT-21.
              "clause_non_concurrence_detectee" : booléen — true UNIQUEMENT si une clause de non-concurrence est textuellement présente dans le contrat de travail produit aux pièces (avec ou sans contrepartie financière chiffrée). False par défaut, y compris si la clause est seulement évoquée sans pièce contractuelle. Pertinent pour F-DT-24.
              "statut_protege_detecte" : booléen — true uniquement si les pièces mentionnent factuellement un statut de représentant du personnel pour le salarié (CSE titulaire ou suppléant, délégué syndical, RSS, conseiller prud'homme, conseiller du salarié, défenseur syndical, médecin du travail, ancien CHSCT — art. L.2411-1 CT). False par défaut. Pertinent pour F-DT-30.
              "transaction_envisagee" : booléen — true uniquement si une transaction post-rupture est envisagée, négociée ou signée selon les pièces (protocole transactionnel, courrier proposant une transaction, mention "concessions réciproques", "indemnité transactionnelle"). False par défaut. Pertinent pour F-DT-31.
              "at_mp_detecte" : booléen — true uniquement si le dossier contient des pièces relatives à un accident du travail ou à une maladie professionnelle (déclaration CPAM, certificat médical initial AT/MP, taux IPP fixé, contestation de reconnaissance, congé maladie professionnelle, décision CRRMP). False par défaut. Pertinent pour F-DT-33.
              "urgence_procedurale" : booléen — true uniquement si les pièces évoquent factuellement une urgence procédurale au sens du référé prud'homal (art. R.1454-1 CT) : non-paiement de salaires en cours, mise à pied conservatoire, contestation immédiate d'un licenciement nul (réintégration sans tarder), provisions à obtenir rapidement, mesures conservatoires demandées. False par défaut. Pertinent pour F-DT-34.
              "contestation_are_envisagee" : booléen — true uniquement si les pièces mentionnent un litige avec France Travail / Pôle Emploi (refus ARE notifié, recours hiérarchique formé, contestation de carence, démission requalifiée par France Travail, courrier France Travail). False par défaut. Pertinent pour F-DT-35.
              F-204 — 5 flags décisionnels niveau 3 (F-DT-11/12/15/19/27 BE), BELGIQUE UNIQUEMENT. Chaque flag est un booléen avec valeur par défaut false ; ne mettre true QUE si un indice factuel concret est documenté dans les pièces. Pour un dossier travail FRANCE, TOUS ces 5 flags DOIVENT rester false (les régimes FR équivalents sont gérés par les flags FR ci-dessus, par F-DT-08 validity, ou par F-205 ultérieur).
              "harcelement_be_detecte" : booléen — true uniquement si les pièces évoquent des faits de harcèlement moral ou sexuel au travail au sens de la Loi du 4 août 1996 relative au bien-être des travailleurs (plainte formelle déposée auprès du conseiller en prévention aspects psychosociaux, témoignages de collègues, demande d'intervention psychosociale, certificat médical lien stress travail, courrier inspection sociale CBE). False par défaut. Pertinent pour F-DT-11 BE.
              "discrimination_be_detectee" : booléen — true uniquement si les pièces évoquent une discrimination au sens de la Loi du 10 mai 2007 (genre, origine, conviction religieuse, orientation sexuelle, handicap, âge, état de santé) avec indice factuel documenté (refus d'embauche, refus de promotion, licenciement sur motif protégé, courrier UNIA / Institut pour l'égalité des femmes et des hommes, témoignage). False par défaut. Pertinent pour F-DT-12 BE.
              "inaptitude_medicale_be_detectee" : booléen — true uniquement si les pièces évoquent une inaptitude médicale définitive ou un trajet de réintégration au sens de l'art. 34 Loi 03/07/1978 ou de l'AR 28/05/2003 (avis du médecin du travail, décision de force majeure médicale, plan de réintégration, refus de poste adapté). False par défaut. Pertinent pour F-DT-15 BE.
              "heures_sup_mentionnees_be" : booléen — true uniquement si les pièces évoquent factuellement des heures supplémentaires au sens de l'art. 29 Loi 16/03/1971 (sursalaire 50 % semaine ou 100 % dimanche/jours fériés, repos compensatoire dû, contestation décompte). False par défaut, y compris si le dossier mentionne uniquement la durée légale. Pertinent pour F-DT-19 BE.
              "motif_grave_be_envisage" : booléen — true uniquement si les pièces évoquent un licenciement pour motif grave au sens de l'art. 35 Loi 03/07/1978 (notification dans les 3 jours ouvrables après connaissance du fait, lettre de notification du motif dans les 3 jours suivants, contestation par le travailleur). False par défaut. Pertinent pour F-DT-27 BE.
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
            "type_titre_sejour_code" : code normalisé du titre de séjour détecté, l'un des 21 codes exacts (null si non déterminable avec certitude) : "VLS_TS_ETUDIANT", "VLS_TS_SALARIE", "CST_SALARIE", "CARTE_PLURIANNUELLE", "CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE", "CARTE_PLURIANNUELLE_SALARIE", "CARTE_PLURIANNUELLE_PASSEPORT_TALENT", "CARTE_PLURIANNUELLE_VPF", "CARTE_RESIDENT", "APS", "CST_VPF", "CST_VPF_CONJOINT_FR", "RECEPISSE_ASILE" (France), "CARTE_A_TRAVAIL", "CARTE_A_ETUDES", "CARTE_A_FAMILLE", "CARTE_B", "CARTE_C", "PERMIS_UNIQUE", "ANNEXE_15", "ATTESTATION_IMMATRICULATION" (Belgique).
            RÈGLES DE PRÉCISION SF-IM-07-04 (France) — NE PAS utiliser les codes génériques "CARTE_PLURIANNUELLE" et "CST_VPF" si la mention précise du motif est lisible dans les pièces. Préférer TOUJOURS le sous-type :
            • Carte pluriannuelle + mention "Étudiant" ou "Étudiant-Recherche" → "CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE" (régime 964 h/an)
            • Carte pluriannuelle + mention "Salarié" (hors Passeport Talent) → "CARTE_PLURIANNUELLE_SALARIE"
            • Carte pluriannuelle + mention "Passeport Talent" / "Chercheur" L.421-14 / "Cadre supérieur" L.421-9 / "Créateur d'entreprise" → "CARTE_PLURIANNUELLE_PASSEPORT_TALENT"
            SF-IM-10-01 — Quand "type_titre_sejour_code" = "CARTE_PLURIANNUELLE_PASSEPORT_TALENT", ajouter le champ "talent_sub_category_code" pour préciser la sous-catégorie CESEDA (null si non déterminable) : "TALENT_CHERCHEUR" (L.421-14, convention d'accueil recherche), "TALENT_SALARIE_QUALIFIE" (L.421-9, salaire ≥ 2× SMIC + master), "TALENT_ENTREPRENEUR" (L.421-10, création entreprise + invest ≥ 30 000 €), "TALENT_INNOVANT" (L.421-11, projet innovant reconnu DGE/French Tech), "TALENT_INVESTISSEUR" (L.421-12, invest direct ≥ 300 000 €), "TALENT_PROFESSION_ARTISTIQUE" (L.421-13, artiste/créateur culturel), "TALENT_RENOMMEE_INTERNATIONALE" (L.421-13 al. 3, prix/palmarès/grands concours/JO), "TALENT_SALARIE_EN_MISSION" (L.421-15, mission ICT intra-groupe), "TALENT_FAMILLE" (L.421-22, conjoint/enfant d'un bénéficiaire Passeport Talent). Si ambigu, laisser null — le type générique "PASSEPORT_TALENT" fera fallback.
            • Carte pluriannuelle + mention "Vie privée et familiale" / "VPF" → "CARTE_PLURIANNUELLE_VPF"
            • Carte VPF 1 an délivrée au conjoint d'un ressortissant FRANÇAIS (art. L.423-1, indices : acte de mariage avec conjoint français présent au dossier) → "CST_VPF_CONJOINT_FR" (pas "CST_VPF" générique)
            Les codes génériques "CARTE_PLURIANNUELLE" et "CST_VPF" ne sont utilisés que si le motif exact n'est pas déterminable depuis les pièces.
            Si le pays ou le type ne correspond pas à cette liste, laisser null — le champ "type_titre_sejour" libre suffit pour l'affichage.
            "nationalite_ue" : booléen — true si le ressortissant est citoyen de l'Union européenne, EEE (Norvège/Islande/Liechtenstein) ou Suisse ; false s'il est ressortissant d'un pays tiers ; null si non déterminable.
            "type_procedure_detectee" : type de procédure administrative en cours, l'une de ces valeurs exactes : "RENOUVELLEMENT_TITRE_SEJOUR", "DEMANDE_ASILE_OFPRA", "RECOURS_CNDA", "REGROUPEMENT_FAMILIAL" (R.434-35), "NATURALISATION_DECRET" (art. 21-15 CCiv), "CHANGEMENT_STATUT" (L.412-1, ex. étudiant → salarié), "AES_SALARIE" (circulaire Valls 28/11/2012, 5 ans présence + 24 mois salariat), "REGULARISATION_EXCEPTIONNELLE" (L.435-1, liens personnels et familiaux, voie humanitaire), "OQTF_AVEC_DELAI" (L.614-5, OQTF avec délai de départ volontaire 30 j), "OQTF_SANS_DELAI" (L.731-1, OQTF sans DDV, procédure JLD 48 h), null si non détectable.
            "date_depot_procedure" : date de dépôt de la demande ou du recours au format YYYY-MM-DD, null si non détectable.
            "type_recours_code" : code normalisé du recours à former ou déjà formé, l'un des 6 codes exacts (null si non déterminable avec certitude) : "RECOURS_GRACIEUX_PREFET", "RECOURS_CONTENTIEUX_TA", "RECOURS_CNDA" (France), "RECOURS_CGRA", "RECOURS_CCE", "RECOURS_CE_BELGIQUE" (Belgique). Ce champ complète "type_procedure_detectee" avec la granularité nécessaire à F-IM-06 (générateur de recours). Si la procédure est un renouvellement ou une demande d'asile OFPRA (pas un recours), laisser null.
            "date_notification_decision_contestee" : date de la décision administrative que l'on attaque par recours (refus de titre, rejet OFPRA, etc.), format YYYY-MM-DD, null si non détectable. À ne pas confondre avec "date_depot_procedure" (date de dépôt effectif du recours) ni "date_expiration_titre".
            SF-155-04-00-BE-immig-FR — 5 champs IA FRANCE UNIQUEMENT pour pré-fill outils décisionnels OQTF (F-IM-08-02 OQTF avec délai, F-IM-08-04 OQTF sans délai urgence 48h). Tous ces champs DOIVENT rester null pour un dossier immigration BELGIQUE (les équivalents Annexe 13 sont traités par les 4 champs BE ci-dessous).
            "date_notification_oqtf" : date de notification de l'OQTF avec délai (arrêté préfectoral L.614-5) au format YYYY-MM-DD. Null si dossier non concerné par une OQTF avec délai ou si la date n'est pas lisible dans les pièces. Dossier belge → null.
            "motif_oqtf_code" : motif de l'OQTF avec délai détecté dans l'arrêté préfectoral, l'une de ces valeurs exactes (null si indéterminable avec certitude ou dossier belge) : "REFUS_TITRE" (refus de délivrance/renouvellement d'un titre de séjour), "EXPIRATION_TITRE" (titre de séjour expiré sans renouvellement), "SEJOUR_IRREGULIER" (entrée ou séjour irrégulier sans titre initial), "RETRAIT_TITRE" (retrait/abrogation d'un titre antérieurement délivré), "AUTRE" (motif présent mais non classable dans les 4 ci-dessus). Ne pas confondre avec les motifs d'OQTF sans délai (risque de fuite, trouble à l'ordre public, etc.) — ces derniers ne sont PAS demandés dans ce champ.
            "recours_forme_detected" : objet {"reponse": "OUI" | "NON" | "INCONNU", "justification": phrase courte (≤ 500 car.) citant l'indice textuel} indiquant si un recours contentieux contre l'OQTF a déjà été introduit au moment de l'analyse. "OUI" si une requête TA, un mémoire ou un accusé de réception greffe est présent ; "NON" si les pièces indiquent explicitement qu'aucun recours n'a encore été déposé ; "INCONNU" si les pièces ne permettent pas de trancher. Pertinent pour F-IM-08-02 et F-IM-08-04. Null pour dossier belge ou hors OQTF.
            "date_heure_notification_oqtf_sans_delai" : horodatage précis (format ISO "YYYY-MM-DDTHH:mm" ou "YYYY-MM-DDTHH:mm:ss") de la notification de l'OQTF sans délai (arrêté préfectoral L.731-1). L'heure est capitale car le délai de recours est de 48 heures devant le JLD. Null si dossier non concerné par une OQTF sans délai, si seule la date (sans heure) est détectable, ou pour un dossier belge.
            "placement_cra_detected" : booléen — true si l'arrêté mentionne un placement en centre de rétention administrative (CRA) concomitant à l'OQTF sans délai, false si le dossier précise une assignation à résidence ou absence de placement, null si non déterminable ou dossier belge. Important pour le calcul de l'urgence F-IM-08-04.
            SF-155-04-00-BE-immig-BE — 4 champs BE Annexe 13 (F-IM-08) — DOSSIERS BELGIQUE UNIQUEMENT. Pour un dossier FRANCE, laisser ces 4 champs à null (les champs OQTF FR sont gérés séparément dans le même objet).
            "date_notification_annexe13" : date de notification de l'Annexe 13 / OQT belge au format YYYY-MM-DD, null si aucune annexe 13 n'est présente ou non détectable. L'annexe 13 est la décision administrative de l'Office des étrangers (OE) ordonnant de quitter le territoire (art. 7, 74/14 Loi du 15/12/1980). Dossiers FR : null.
            "delai_depart_impose_jours" : délai de départ volontaire imposé par l'OE (entier ≥ 0). Valeurs typiques : 0 (OQT sans délai, procédure d'urgence art. 74/14 §3), 7 (délai réduit), 30 (délai standard art. 74/14 §1). Null si pas d'OQT ou délai non identifiable. Dossiers FR : null.
            "motif_oqt_code_be" : code motif factuel de l'OQT belge, l'une EXACTEMENT de ces 4 valeurs (null sinon) : "SEJOUR_IRREGULIER_ART_7" (séjour irrégulier art. 7 al.1 1° — absence de titre ou titre expiré), "REFUS_SEJOUR_APRES_DEMANDE" (refus de séjour notifié après une demande — annexe 13 suite à décision négative sur demande introduite), "FIN_SEJOUR_REGULIER" (fin de séjour régulier — expiration d'un titre non renouvelé, annexe 15 terminée, rejet asile sans autre titre), "AUTRE" (motif non couvert par les 3 premiers — à utiliser avec parcimonie, uniquement si clairement un motif distinct). Ne jamais inventer ; si le motif exact n'est pas identifiable dans les pièces, utiliser null. Dossiers FR : null.
            "transfert_imminent_detected" : booléen — true SEULEMENT s'il y a des indices factuels clairs d'un transfert imminent vers un centre fermé ou la frontière (placement en centre fermé notifié, escorte annoncée par l'OE, vol de retour programmé mentionné). false si une mention explicite indique l'absence de placement. null par défaut si non déterminable — c'est le cas attendu pour la plupart des dossiers. Dossiers FR : null.
            F-201 — 9 flags décisionnels niveau 3, FRANCE UNIQUEMENT. Chaque flag est un booléen avec valeur par défaut false ; ne mettre true QUE si un indice factuel concret est documenté dans les pièces. Pour un dossier immigration BELGIQUE, TOUS ces 9 flags DOIVENT rester false (les régimes BE équivalents sont gérés par les 5 flags BE ci-dessous). Permettent à F-IA-04 de basculer 10 outils Immigration FR ALWAYS_ON → CONTEXTUAL.
            "aes_metiers_tension_eligible_detecte" : booléen — true si le dossier évoque une admission exceptionnelle au séjour (AES) au titre des métiers en tension (art. L. 435-4 CESEDA, métier figurant à la liste arrêtée par le ministre du Travail) avec indice factuel (fiche de paie sur métier en tension, attestation employeur, mention explicite dans une pièce préfecture / avocat). False par défaut.
            "aes_familial_eligible_detecte" : booléen — true si le dossier évoque une AES au titre des liens personnels et familiaux (art. L. 435-1 CESEDA — durée de séjour, intensité des liens, scolarisation des enfants en France, conjoint Français, etc.). False par défaut.
            "aes_humanitaire_eligible_detecte" : booléen — true si le dossier évoque une AES pour motif humanitaire (art. L. 435-1 CESEDA — situation médicale grave non couverte par L.425-9, victime d'une infraction grave, etc.). False par défaut.
            "aes_etudiant_eligible_detecte" : booléen — true si le dossier évoque une AES au titre étudiant (art. L. 435-3 CESEDA — étudiant en cours d'études, parcours universitaire continu, etc.). False par défaut.
            "changement_statut_envisage_detecte" : booléen — true si le dossier évoque un changement de statut envisagé (étudiant → salarié L.412-1, conjoint → indépendant, asile rejeté → AES, etc.) avec indice factuel (CDI signé, demande déposée, etc.). False par défaut.
            "procedure_asile_detectee" : booléen — true uniquement si le dossier comporte une procédure d'asile en cours ou récente (récépissé asile, dépôt OFPRA, recours CNDA, attestation demande asile, document Dublin). False par défaut.
            "naturalisation_envisagee_detectee" : booléen — true uniquement si le dossier évoque une démarche de naturalisation en cours ou imminente (dossier déposé en préfecture, demande par décret art. 21-15 CCiv, demande par déclaration art. 21-2 / 21-13). False par défaut.
            "client_mineur_detecte" : booléen — true uniquement si le client de l'avocat (ressortissant étranger concerné par le dossier) est mineur au moment de la procédure (date de naissance attestée, mention "MNA", "mineur isolé", "ASE", autorité parentale identifiée). False si majeur. False par défaut.
            "mesure_eloignement_detectee" : booléen — true uniquement si le dossier comporte une mesure d'éloignement notifiée (OQTF L.614-5 ou L.731-1, ITF, expulsion L.631-1+, IRTF). Cumul avec les flags OQTF FR détaillés ci-dessus possible. False par défaut.
            F-203 — 5 flags décisionnels niveau 3, BELGIQUE UNIQUEMENT. Chaque flag est un booléen avec valeur par défaut false ; ne mettre true QUE si un indice factuel concret est documenté dans les pièces. Pour un dossier immigration FRANCE, TOUS ces 5 flags DOIVENT rester false. Permettent à F-IA-04 de basculer 5 outils Immigration BE ALWAYS_ON → CONTEXTUAL.
            "procedure_9bis_envisagee" : booléen — true uniquement si le dossier évoque une procédure de régularisation par circonstances exceptionnelles humanitaires au sens de l'art. 9bis Loi du 15/12/1980 (demande adressée au bourgmestre, motivation humanitaire / liens durables / scolarisation enfants, attente décision Office des étrangers). False par défaut.
            "procedure_9ter_medicale_detectee" : booléen — true uniquement si le dossier évoque une procédure médicale au sens de l'art. 9ter Loi du 15/12/1980 (certificat médical type, maladie grave, absence de traitement disponible au pays d'origine, avis du médecin de l'OE). False par défaut.
            "regroupement_40bis_detecte" : booléen — true uniquement si le dossier évoque un regroupement familial de citoyen UE / EEE / Suisse au sens de l'art. 40bis Loi du 15/12/1980 (conjoint, partenaire enregistré, descendant, ascendant à charge d'un citoyen UE résidant en Belgique). False par défaut.
            "regroupement_40ter_detecte" : booléen — true uniquement si le dossier évoque un regroupement familial de ressortissant tiers ou Belge au sens de l'art. 40ter Loi du 15/12/1980 (conjoint d'un Belge, descendant d'un Belge, ascendant à charge d'un Belge ou d'un ressortissant tiers titulaire d'un titre de long séjour). False par défaut.
            "oqt_annexe13_detectee" : booléen — true uniquement si une annexe 13 / OQT belge a été notifiée au client (art. 7, 74/14 Loi du 15/12/1980 — décision administrative de l'Office des étrangers ordonnant de quitter le territoire). Recoupe en partie "date_notification_annexe13" mais signale la simple présence factuelle même si la date n'est pas extractible. False par défaut.
            "trigger_events" : tableau (éventuellement vide) des événements factuels identifiés dans les pièces qui ouvrent un nouveau droit de séjour (F-150). Chaque élément : {"event_code" : l'un des 10 codes exacts (null si l'événement détecté ne correspond à aucun de ces codes) : "MARIAGE_RESSORTISSANT_FR" (mariage avec Français), "PACS_RESSORTISSANT_FR" (PACS avec Français, communauté de vie > 1 an), "NAISSANCE_ENFANT_FR" (naissance enfant français), "DOCTORAT_OBTENU" (doctorat obtenu en France), "CDI_OBTENU_SALARIE" (CDI signé), "ENTREE_LEGALE_10ANS" (10 ans de titre régulier), "VIOLENCES_CONJUGALES_CONSTATEES" (conjoint victime), "DEMANDE_ASILE_ACCORDEE_OFPRA" (réfugié OFPRA/CNDA), "ENFANT_NE_FR_13ANS_PRESENCE" (né FR + résidence depuis 13 ans), "REGROUPEMENT_FAMILIAL_AUTORISE" ; "event_date" : date factuelle de l'événement YYYY-MM-DD (celle du mariage / doctorat / CDI, pas d'une date de raisonnement), null si inconnue ; "source_document" : nom du document support (ex. "acte_mariage.pdf"), null si non identifiable ; "justification" : phrase courte expliquant l'indice factuel (ex. "Mariage célébré le 15/03/2025 mentionné dans l'acte n°127 de la Mairie du 5ème")}. N'inventer AUCUN événement non directement mentionné dans les pièces. Inclure aussi les événements imminents documentés : soutenance de thèse programmée avec convention d'accueil ; mariage publié des bans ; CDI signé même non commencé ; naissance déclarée et reconnaissance de paternité française. La preuve documentaire (date + acte/attestation officielle) suffit, l'événement n'a pas besoin d'être révolu. Tableau vide [] si aucun événement déclencheur détecté.
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

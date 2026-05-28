package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.StatutoryDeadlineService;
import fr.ailegalcase.chat.ChatMessage;
import fr.ailegalcase.chat.ChatMessageRepository;
import fr.ailegalcase.shared.PaymentRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile({"local", "prod"})
public class EnrichedAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EnrichedAnalysisService.class);

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois la synthèse globale d'un dossier juridique ainsi que les réponses de l'avocat à des questions complémentaires.
            Produis une synthèse enrichie et mise à jour en intégrant ces nouvelles informations.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu (inclure tous les champs) : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "points_juridiques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "risques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "questions_ouvertes": [...], "pieces_manquantes": [...], "points_procedure": [...], "pistes_strategiques": [...], "score_risque": {"niveau": "FAIBLE"|"MOYEN"|"ELEVE", "valeur": <0-100>}, "checks_a_requalifier": [{"description": "...", "nouveau_statut": "NON_COMPLIANT"|"TO_CHECK", "raison": "..."}], "type_litige_detecte": "LICENCIEMENT_SANS_CAUSE_REELLE"|"LICENCIEMENT_ECONOMIQUE"|"PRISE_ACTE_RUPTURE"|"HARCELEMENT_MORAL"|"DISCRIMINATION"|"HEURES_SUPPLEMENTAIRES"|"RAPPEL_SALAIRE"|null, "date_reference_prescription": "YYYY-MM-DD"|null, "compensation_data": {"type_rupture": "LICENCIEMENT"|"LICENCIEMENT_ECONOMIQUE"|"RUPTURE_CONVENTIONNELLE"|"DEMISSION"|"PRISE_ACTE"|"RESILIATION_JUDICIAIRE"|"LICENCIEMENT_ORDINAIRE"|"RUPTURE_AMIABLE", "anciennete_annees": <entier>|null, "anciennete_mois": <entier>|null, "salaire_reference_mensuel": <décimal>|null}|null}
            Pour les champs "faits", "points_juridiques" et "risques", chaque élément est un objet avec "texte" (le contenu), "source" (nom exact du fichier tel qu'il apparaît dans la synthèse précédente) et "extrait" (phrase exacte tirée du document). Si la source n'est pas identifiable, utilise "source": null et "extrait": null.
            F-146 : ajoute AUSSI à chaque item un champ "sourceRef" précisant la pièce juridique exacte : {"documentName": "<nom fichier>", "pieceType": "<type>", "pieceLabel": "<label de la pièce>", "pageStart": <début>, "pageEnd": <fin>}. Utilise la section "=== PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===" fournie dans le prompt utilisateur. "sourceRef": null si non identifiable. Ne jamais inventer un label absent de cette section.
            Le champ "pieces_manquantes" liste les pièces habituellement attendues dans ce type de dossier qui sont absentes des documents fournis. Chaque élément est un objet {"texte": "<description de la pièce>", "critere_code": "<code ou null>"}. "critere_code" est rempli UNIQUEMENT si l'absence de cette pièce correspond à un code surveillé :
            - Critères F-DT-08 Validité licenciement : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE.
            - Critères F-DT-10 Validité rupture conventionnelle (France, art. L1237-11 s.) : RC_CONSENTEMENT, RC_DELAI_RETRACTATION, RC_HOMOLOGATION, RC_ASSISTANCE, RC_INDEMNITE, RC_ENTRETIENS.
            - Pièces F-FA-07 Checklist divorce : FR_ACTE_MARIAGE, FR_ACTE_NAISSANCE_EPOUX, FR_ACTE_NAISSANCE_ENFANTS, FR_LIVRET_FAMILLE, FR_JUSTIF_DOMICILE, FR_CONTRAT_MARIAGE, FR_ETAT_PATRIMOINE, FR_JUSTIF_REVENUS, FR_PIECE_IDENTITE, BE_ACTE_MARIAGE, BE_ACTE_NAISSANCE_EPOUX, BE_ACTE_NAISSANCE_ENFANTS, BE_COMPOSITION_MENAGE, BE_CONTRAT_MARIAGE, BE_CONVENTION_PREALABLE, BE_JUSTIF_REVENUS, BE_PIECE_IDENTITE.
            - Pièces F-FA-05 Partage immobilier : FA05_VALEUR_VENALE, FA05_CAPITAL_RESTANT.
            Sinon null. Rétrocompat : format string legacy accepté. Si le dossier semble complet, utilise "pieces_manquantes": [].
            Le champ "points_procedure" liste les étapes procédurales légalement requises dans ce type de dossier. Chaque élément est un objet {"texte": "<description de l'étape>", "critere_code": "<code ou null>", "expected_value": "<valeur ou null>"}. "critere_code" est rempli uniquement si le point porte sur l'un des critères surveillés :
            - Critères F-DT-08 Validité licenciement (droit du travail, binaires) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE. "expected_value" doit rester null.
            - Critères F-DT-10 Validité rupture conventionnelle (droit du travail France, binaires) : RC_CONSENTEMENT, RC_DELAI_RETRACTATION, RC_HOMOLOGATION, RC_ASSISTANCE, RC_INDEMNITE, RC_ENTRETIENS. VERIFIED = critère respecté, NON_COMPLIANT = critère non respecté. "expected_value" reste null.
            - Étapes F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_CHOIX_AVOCATS, FR_REDACTION_CONVENTION, FR_ENVOI_LRAR, FR_DELAI_REFLEXION, FR_SIGNATURE_CONVENTION, FR_DEPOT_NOTAIRE, FR_ENREGISTREMENT, BE_CHOIX_AVOCAT, BE_REDACTION_CONVENTION, BE_REQUETE_CONJOINTE, BE_COMPARUTION, BE_JUGEMENT, BE_TRANSCRIPTION. VERIFIED = étape accomplie, NON_COMPLIANT = étape non faite. "expected_value" reste null.
            - Critères F-IM-21 Validité dossier immigration (droit de l'immigration, binaires) : IM21_REGULARITE_SEJOUR_FR, IM21_DELAI_DEPOT_FR, IM21_PIECE_IDENTITE_FR, IM21_JUSTIF_DOMICILE_FR, IM21_ETAT_CIVIL_FR, IM21_PHOTO_FR, IM21_TIMBRE_FISCAL_FR, IM21_PIECES_MARIAGE_FR, IM21_COMMUNAUTE_VIE_FR, IM21_RESSOURCES_FR, IM21_CONVENTION_ACCUEIL_FR, IM21_REGULARITE_SEJOUR_BE, IM21_PIECE_IDENTITE_BE, IM21_PIECES_COHABITATION_BE, IM21_RESSOURCES_BE, IM21_LOGEMENT_BE, IM21_ASSURANCE_BE, IM21_EXTRAIT_CASIER_BE. Pour ces critères, "expected_value" doit rester null (statut VERIFIED/NON_COMPLIANT porte le signal).
            - Critère F-FA-06 Calendrier garde (droit de la famille, énuméré) : FA06_MODE_GARDE. "expected_value" obligatoire parmi : ALTERNEE_FR, DVH_CLASSIQUE_FR, DVH_ELARGI_FR (France), ALTERNEE_BE, SECONDAIRE_BE, SECONDAIRE_ELARGI_BE (Belgique).
            - Critère F-FA-05 Partage immobilier (droit de la famille, numérique) : FA05_VALEUR_VENALE, FA05_CAPITAL_RESTANT. "expected_value" obligatoire = valeur en euros en chaîne (ex. "350000" ou "120000.50").
            - Critère F-IM-05 Titre de séjour (droit de l'immigration, énuméré) : IM05_MOTIF. "expected_value" obligatoire parmi : TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE.
            - Critère F-IM-06 Recours (droit de l'immigration, énuméré) : IM06_RECOURS_TYPE. "expected_value" obligatoire parmi : RECOURS_GRACIEUX_PREFET, RECOURS_CONTENTIEUX_TA, RECOURS_CNDA (France), RECOURS_CGRA, RECOURS_CCE, RECOURS_CE_BELGIQUE (Belgique).
            - Critère F-IM-07 Droit au travail (droit de l'immigration, énuméré) : IM07_TITRE_TYPE. "expected_value" obligatoire parmi les 16 codes de titre (identiques à F-IM-05).
            - Critère F-DT-09 Type de rupture (énuméré) : DT09_TYPE_RUPTURE. Pour ce critère, renseigne obligatoirement "expected_value" avec la valeur affirmée par le point, parmi : LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE (France), LICENCIEMENT_ORDINAIRE, RUPTURE_AMIABLE (Belgique).
            Pour tout point sans lien avec ces critères, "critere_code" et "expected_value" restent null. Rétrocompat : format string legacy accepté. Si la procédure semble conforme, utilise "points_procedure": [].
            SF-96-06 — Durcissement : quand "critere_code" est null, "points_procedure" ne doit contenir QUE des vérifications binaires factuelles d'étapes légalement requises sur le dossier en cours (les 3 statuts ✅Vérifié / ❌Non conforme / ⚠️À vérifier doivent tous avoir du sens sur l'item). SONT INTERDITS dans "points_procedure" et doivent être redirigés ailleurs : (a) options stratégiques ("En cas de demande…", "Si l'avocat envisage…", "Possibilité de demander…", "Alternative…") → mettre dans "pistes_strategiques" (cf. ci-dessous) ; (b) opportunités futures à plus de 6 mois ("Après N ans de mariage…", "À partir de…", "Une fois N années révolues…") → "pistes_strategiques" si c'est une option à étudier, "risques" si elles imposent un délai à respecter ; (c) recommandations d'action ("Demande à déposer auprès de…", "Joindre la convention…", "Prendre attache avec…") → "pistes_strategiques" si c'est une décision stratégique, "questions_ouvertes" si ça suppose une réponse de l'avocat. Règle de répartition : on VÉRIFIE dans points_procedure, on PROPOSE dans pistes_strategiques, on ALERTE dans risques, on QUESTIONNE dans questions_ouvertes.
            F-176 — Le champ "pistes_strategiques" liste les options stratégiques, opportunités futures et recommandations d'action que l'avocat peut envisager pour ce dossier (c'est-à-dire ce que la règle SF-96-06 ci-dessus exclut de "points_procedure"). Chaque élément est un objet {"texte": "<description de la piste>", "base_juridique": "<articles, lois, jurisprudence référencés>" ou null, "horizon_temporel": "<court terme | moyen terme | long terme + délai approximatif>" ou null, "conditions": ["<condition 1>", "<condition 2>"] (array de strings, [] si aucune), "source": "<source factuelle dans le dossier>" ou null}. Si aucune piste, utilise "pistes_strategiques": []. RÈGLE PROPAGATION : si le prompt utilisateur contient une section [Pistes stratégiques retenues à approfondir], développe ces pistes (les remettre + les enrichir avec base_juridique précise et conditions actualisées). Si le prompt contient [Pistes stratégiques écartées — NE PAS re-proposer], ne remets PAS ces pistes dans "pistes_strategiques" — l'avocat les a déjà étudiées et écartées.
            Le champ "score_risque" est obligatoire : évalue le niveau de risque global du dossier. "niveau" est l'un de "FAIBLE", "MOYEN" ou "ELEVE". "valeur" est un entier entre 0 et 100 reflétant l'intensité du risque (0 = aucun risque, 100 = risque maximum).
            Le champ "checks_a_requalifier" liste les points procéduraux marqués "vérifiés" dans le prompt que tu estimes devoir requalifier à la lumière des nouvelles informations. Pour chaque point : "description" doit correspondre exactement au libellé fourni dans le prompt, "nouveau_statut" est "NON_COMPLIANT" si le point est manifestement non respecté ou "TO_CHECK" si des doutes subsistent, "raison" explique brièvement pourquoi ce point doit être revu. Si aucun point vérifié ne doit être requalifié, utilise "checks_a_requalifier": [].
            ========== RÈGLE CRITIQUE DE PRÉSERVATION BASELINE (mode enrichi) ==========
            Les CHAMPS DE CLASSIFICATION FACTUELS extraits par la synthèse précédente
            sont la BASELINE à PRÉSERVER. Tu ne dois les CHANGER QUE si un nouveau
            document signé/notifié dans le dossier contredit la classification précédente.
            Les réponses Q&A de l'avocat, le chat et les checks procéduraux
            N'AUTORISENT PAS à retourner ces classifications — ils ajoutent du signal
            sur la VALIDITÉ ou les vices, pas sur le mécanisme factuel.

            Champs concernés (tous domaines) :
            - Droit du travail : type_rupture
            - Droit immigration : type_titre_sejour_code, type_procedure_detectee,
              type_recours_code, nationalite_ue
            - Droit famille : mode_garde_detaille, regime_matrimonial, pays_applicable

            Exemple : synthèse précédente type_rupture="RUPTURE_CONVENTIONNELLE" sur
            la base d'une convention signée → conservée même si les Q&A mentionnent
            "licenciement" (le dossier vise la requalification = pas un nouveau fait
            mécanique). Idem pour un regime_matrimonial="SEPARATION_BIENS" basé sur
            un contrat signé : pas retourné parce que l'avocat argumente une
            requalification en communauté.
            ================================================================================

            Le champ "compensation_data" contient les données nécessaires au pré-remplissage de F-DT-09. Il est OBLIGATOIRE dès qu'une rupture du contrat est identifiée, même si ancienneté ou salaire sont inconnus.
            Renseigne "type_rupture" parmi — France : "LICENCIEMENT" (cause réelle et sérieuse), "LICENCIEMENT_ECONOMIQUE", "RUPTURE_CONVENTIONNELLE", "DEMISSION", "PRISE_ACTE", "RESILIATION_JUDICIAIRE" ; Belgique : "LICENCIEMENT_ORDINAIRE" (tout licenciement côté employeur quelle que soit la motivation — économique, disciplinaire, etc.), "RUPTURE_AMIABLE", "DEMISSION". Ne jamais renvoyer "type_rupture": null si compensation_data est émis : choisir la valeur la plus probable. "anciennete_annees" et "anciennete_mois" sont l'ancienneté totale (entiers). "salaire_reference_mensuel" est le salaire brut mensuel moyen de référence (décimal). Utilise null uniquement pour les champs numériques non déterminables. En cas de ruptures multiples, retiens la plus récente. Si le dossier ne relève pas du droit du travail ou si aucune rupture n'est identifiée, utilise "compensation_data": null.
            Le champ "type_litige_detecte" identifie le type principal de litige du dossier. Utilise l'une des valeurs suivantes (null si non déterminable) : %s.
            Le champ "date_reference_prescription" est la date à partir de laquelle commence le délai de prescription (ex : date de rupture du contrat, date des faits). Format ISO 8601 (YYYY-MM-DD). Utilise null si non déterminable.

            Le champ "travail_extracted_data" doit être préservé et actualisé depuis la synthèse initiale. Pour un dossier de droit du travail, ré-émets l'objet complet avec les champs : "convention_collective", "date_entree", "salaire_brut_mensuel", "salaire_est_deduit" (SF-130-01 : true si salaire_brut_mensuel déduit d'un net via × 1,30), "type_contrat", "poste", "motif_licenciement", "date_licenciement", "conges_contractuels", "prime_anciennete_contractuelle", "nom_salarie", "prenom_salarie", "adresse_salarie", "nom_employeur", "adresse_employeur", "siret_employeur" (FR uniquement, chiffres), "bce_employeur" (BE uniquement, chiffres), "representant_employeur", "motif_nullite_pressenti" (SF-155-04 : DISCRIMINATION | HARCELEMENT_MORAL | HARCELEMENT_SEXUEL | RETORSION | SYNDICAL | MATERNITE_PATERNITE | ACCIDENT_MP, null par défaut), "origine_inaptitude_pressentie" (SF-155-04 : ACCIDENT_TRAVAIL | MALADIE_PROFESSIONNELLE | MALADIE_ORDINAIRE, null par défaut), "avis_medecin_travail_date" (SF-155-04 : YYYY-MM-DD, null par défaut), "reclassement_respecte_detected" (SF-155-04 : {"reponse","justification"} ou null), "heures_sup_mentionnees" (SF-155-04 : {"total_declarees_25pct","total_declarees_50pct","hors_contingent"} ou null). Utilise null pour les champs non extractibles. NE JAMAIS INVENTER un SIRET/BCE/adresse ni un motif de nullité / origine d'inaptitude non factuellement établi. Pour un dossier hors droit du travail, utilise "travail_extracted_data": null.

            Le champ "immigration_extracted_data" doit également être préservé et actualisé depuis la synthèse initiale. Pour un dossier de droit de l'immigration, ré-émets l'objet complet avec les champs factuels historiques : "date_expiration_titre", "type_titre_sejour", "type_titre_sejour_code", "nationalite_ue", "type_procedure_detectee", "date_depot_procedure", "type_recours_code", "date_notification_decision_contestee", et SF-155-04-00-BE-immig-BE — 4 champs Annexe 13 BE DOSSIERS BELGIQUE UNIQUEMENT : "date_notification_annexe13" (YYYY-MM-DD), "delai_depart_impose_jours" (entier ≥ 0, typiquement 0/7/30), "motif_oqt_code_be" (SEJOUR_IRREGULIER_ART_7 | REFUS_SEJOUR_APRES_DEMANDE | FIN_SEJOUR_REGULIER | AUTRE, null sinon), "transfert_imminent_detected" (boolean, null par défaut). Dossiers FR : ces 4 champs BE restent null (leurs pendants OQTF français sont gérés dans le même objet). Utilise null pour les champs non extractibles. NE JAMAIS INVENTER de date de notification ni de motif non factuellement établi. Pour un dossier hors droit de l'immigration, utilise "immigration_extracted_data": null.

            Le champ "source_explanations" liste UNE explication par donnée factuelle clé, pour alimenter le popover d'incohérence (F-IA-03). Chaque explication SÉPARE STRICTEMENT 3 zones affichables :
            - sentence : règle juridique pure (≤ 220 car), SANS mention du nom du document/question/F96/pièce. Ex CORRECT : "La convention BTP prévoit une prime de 12%% après 15 ans."
            - label : nom CANONIQUE court de la source (nom de fichier exact pour DOCUMENT ; question complète pour QUESTION_AI ; description courte pour CHECKLIST_F96 ; intitulé court pour MISSING_PIECE).
            - secondaryText : citation/détail verbatim (≤ 200 car). RÉUTILISE les "extrait" que tu as produits dans faits/points/risques. Ex : "Clause 6.2 — '12%% après 15 ans'" | "Réponse de l'avocat : '15 ans'" | "Marqué non conforme — 'Aucune LRAR'".
            Format : [{"sourceKey": "<snake_case|UPPER_F96_CODE>", "sourceType": "DOCUMENT"|"QUESTION_AI"|"CHECKLIST_F96"|"MISSING_PIECE"|"ANALYSIS_DETECTION", "label": "…", "sentence": "…", "secondaryText": "…", "anchorDocName": "<nom exact doc ou null>"}]. sourceKeys génériques : convention_collective, date_entree, salaire_brut_mensuel, conges_contractuels, prime_anciennete_contractuelle, type_rupture, date_licenciement, duree_mariage, revenus_conjoints, nationalite_ue, type_titre_sejour, type_recours, date_notification_decision_contestee. Codes F96 : FR_CONVOCATION, FR_MOTIVATION, BE_AUDITION, RC_CONSENTEMENT, RC_DELAI_RETRACTATION, DT09_TYPE_RUPTURE, FA05_VALEUR_VENALE, FA06_MODE_GARDE, IM05_MOTIF, IM06_RECOURS_TYPE, IM07_TITRE_TYPE, IM21_REGULARITE_SEJOUR_FR, IM21_DELAI_DEPOT_FR, IM21_PIECE_IDENTITE_FR, IM21_JUSTIF_DOMICILE_FR, IM21_ETAT_CIVIL_FR, IM21_PHOTO_FR, IM21_TIMBRE_FISCAL_FR, IM21_PIECES_MARIAGE_FR, IM21_COMMUNAUTE_VIE_FR, IM21_RESSOURCES_FR, IM21_CONVENTION_ACCUEIL_FR, IM21_REGULARITE_SEJOUR_BE, IM21_PIECE_IDENTITE_BE, IM21_PIECES_COHABITATION_BE, IM21_RESSOURCES_BE, IM21_LOGEMENT_BE, IM21_ASSURANCE_BE, IM21_EXTRAIT_CASIER_BE. Omet les sourcekeys sans donnée. Si aucune source unique identifiable → sourceType="ANALYSIS_DETECTION", label="Synthèse du dossier". Si rien de pertinent, "source_explanations": [].
            IMPORTANT : si plusieurs sources corroborent la même donnée, produis PLUSIEURS entries avec le MÊME sourceKey, chacune avec un sourceType et label différents.

            Contraintes de longueur : produis jusqu'à %d entrées timeline, %d faits, %d points_juridiques, %d risques, %d questions_ouvertes, %d pièces manquantes, %d points procédure, %d pistes stratégiques. Pas de minimum — produis exactement ce que la richesse du dossier justifie, sans rembourrer pour atteindre les limites.
            """;

    static String buildSystemPrompt(String legalDomain, String country, AnalysisLimitsProperties.LevelLimits limits,
                                     List<String> litigationTypeKeys) {
        String litigationTypes = litigationTypeKeys.isEmpty()
                ? "aucun type configuré"
                : litigationTypeKeys.stream().map(k -> "\"" + k + "\"").collect(java.util.stream.Collectors.joining(", "));
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country),
                litigationTypes,
                limits.getTimeline(), limits.getFaits(),
                limits.getPointsJuridiques(), limits.getRisques(), limits.getQuestionsOuvertes(),
                limits.getPiecesManquantes(), limits.getPointsProcedure(), limits.getPistesStrategiques())
                + LegalDomainPromptBuilder.domainSpecificInstruction(legalDomain);
    }

    record PreparedEnrichedAnalysis(UUID analysisId, String prompt, String systemPrompt, UUID caseFileId,
                                     AnalysisLimitsProperties.LevelLimits limits, UUID previousAnalysisId,
                                     UUID workspaceId, UUID userId) {}

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService;
    private final AnalysisQaSnapshotService analysisQaSnapshotService;
    private final AnalysisLimitsProperties analysisLimitsProperties;
    private final ChatMessageRepository chatMessageRepository;
    private final ProcedureCheckService procedureCheckService;
    private final StrategicOptionService strategicOptionService;
    private final RetainedPisteAlignmentService retainedPisteAlignmentService;
    private final ProcedureCheckAlignmentService procedureCheckAlignmentService;
    private final PieceManquanteAlignmentService pieceManquanteAlignmentService;
    private final PieceManquanteStatusService pieceManquanteStatusService;
    private final RisqueAlignmentService risqueAlignmentService;
    private final RisqueStatusService risqueStatusService;
    private final AiQuestionAlignmentService aiQuestionAlignmentService;
    private final TypeLitigeOverrideService typeLitigeOverrideService;
    private final StatutoryDeadlineService statutoryDeadlineService;
    private final fr.ailegalcase.referential.LegalReferentialService legalReferentialService;
    private final SourceExplanationGenerator sourceExplanationGenerator;
    private final SourceExplanationService sourceExplanationService;
    private final JurisprudenceVerificationService jurisprudenceVerificationService;
    private final fr.ailegalcase.document.DocumentRepository documentRepository;
    private final fr.ailegalcase.document.DocumentExtractionRepository documentExtractionRepository;
    private final PiecesPromptContext piecesPromptContext;

    /** SF-35-03-bis : budget pour les extraits bruts injectés dans l'enrichie —
     *  identique à CaseAnalysisService pour cohérence. Permet à l'IA de voir
     *  directement les pièces signées (ex. Convention de rupture) au lieu de
     *  s'appuyer sur la synthèse précédente qui peut avoir été biaisée. */
    static final int RAW_DOC_PREFIX_CHARS = 2_000;

    @Lazy @Autowired
    private EnrichedAnalysisService self;

    public EnrichedAnalysisService(CaseAnalysisRepository caseAnalysisRepository,
                                   CaseFileRepository caseFileRepository,
                                   AiQuestionRepository aiQuestionRepository,
                                   AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                   AnalysisJobRepository analysisJobRepository,
                                   AnthropicService anthropicService,
                                   UsageEventService usageEventService,
                                   ApplicationEventPublisher eventPublisher,
                                   AnalysisDocumentSnapshotService analysisDocumentSnapshotService,
                                   AnalysisQaSnapshotService analysisQaSnapshotService,
                                   AnalysisLimitsProperties analysisLimitsProperties,
                                   ChatMessageRepository chatMessageRepository,
                                   ProcedureCheckService procedureCheckService,
                                   StrategicOptionService strategicOptionService,
                                   RetainedPisteAlignmentService retainedPisteAlignmentService,
                                   ProcedureCheckAlignmentService procedureCheckAlignmentService,
                                   PieceManquanteAlignmentService pieceManquanteAlignmentService,
                                   PieceManquanteStatusService pieceManquanteStatusService,
                                   RisqueAlignmentService risqueAlignmentService,
                                   RisqueStatusService risqueStatusService,
                                   AiQuestionAlignmentService aiQuestionAlignmentService,
                                   TypeLitigeOverrideService typeLitigeOverrideService,
                                   StatutoryDeadlineService statutoryDeadlineService,
                                   fr.ailegalcase.referential.LegalReferentialService legalReferentialService,
                                   SourceExplanationGenerator sourceExplanationGenerator,
                                   SourceExplanationService sourceExplanationService,
                                   JurisprudenceVerificationService jurisprudenceVerificationService,
                                   fr.ailegalcase.document.DocumentRepository documentRepository,
                                   fr.ailegalcase.document.DocumentExtractionRepository documentExtractionRepository,
                                   PiecesPromptContext piecesPromptContext) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
        this.analysisDocumentSnapshotService = analysisDocumentSnapshotService;
        this.analysisQaSnapshotService = analysisQaSnapshotService;
        this.analysisLimitsProperties = analysisLimitsProperties;
        this.chatMessageRepository = chatMessageRepository;
        this.procedureCheckService = procedureCheckService;
        this.strategicOptionService = strategicOptionService;
        this.retainedPisteAlignmentService = retainedPisteAlignmentService;
        this.procedureCheckAlignmentService = procedureCheckAlignmentService;
        this.pieceManquanteAlignmentService = pieceManquanteAlignmentService;
        this.pieceManquanteStatusService = pieceManquanteStatusService;
        this.risqueAlignmentService = risqueAlignmentService;
        this.risqueStatusService = risqueStatusService;
        this.aiQuestionAlignmentService = aiQuestionAlignmentService;
        this.typeLitigeOverrideService = typeLitigeOverrideService;
        this.statutoryDeadlineService = statutoryDeadlineService;
        this.legalReferentialService = legalReferentialService;
        this.sourceExplanationGenerator = sourceExplanationGenerator;
        this.sourceExplanationService = sourceExplanationService;
        this.jurisprudenceVerificationService = jurisprudenceVerificationService;
        this.documentRepository = documentRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.piecesPromptContext = piecesPromptContext;
    }

    @RabbitListener(queues = RabbitMQConfig.RE_ANALYSIS_QUEUE, concurrency = "3")
    public void consumeReAnalysis(ReAnalysisMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedEnrichedAnalysis prepared = self.prepareEnrichedAnalysis(message);
        if (prepared == null) return;

        // F-257 — résolution du contexte AiCallContext user-level. Si manquant → FAILED.
        if (prepared.workspaceId() == null || prepared.userId() == null || prepared.caseFileId() == null) {
            log.warn("EnrichedAnalysis caseFile {} missing user/workspace context " +
                            "(userId={}, workspaceId={}, caseFileId={}) — analysis skipped",
                    caseFileId, prepared.userId(), prepared.workspaceId(), prepared.caseFileId());
            self.finalizeEnrichedAnalysis(prepared.analysisId(), prepared.caseFileId(), null,
                    new IllegalStateException("Missing AiCallContext"), prepared.limits(),
                    prepared.previousAnalysisId());
            return;
        }
        AiCallContext ctx = AiCallContext.userLevel(prepared.workspaceId(), prepared.userId(),
                prepared.caseFileId(), JobType.ENRICHED_ANALYSIS);

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Enriched analysis START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            // F-190 SF-190-02 — streaming SSE miroir de F-185 SF-185-01.
            // À chaque section JSON top-level close détectée par PartialJsonSectionExtractor,
            // persistance immédiate dans partial_state (REQUIRES_NEW) + event SSE
            // ENRICHED_ANALYSIS PARTIAL pour que la page synthèse affiche les sections
            // au fil de l'eau pendant la re-analyse enrichie.
            //
            // Fallback gracieux : si le streaming échoue (HTTP, parsing), on retombe
            // sur l'appel synchrone — aucune régression possible.
            //
            // Bump tokens : SF d'origine 16384 → 64000 (cf. F-161 SF-161-02).
            java.util.concurrent.atomic.AtomicInteger chunkCount = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger sectionCount = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger persistCount = new java.util.concurrent.atomic.AtomicInteger();
            try {
                PartialJsonSectionExtractor extractor = new PartialJsonSectionExtractor();
                result = anthropicService.analyzeWithSystemCacheStreaming(
                        ctx, prepared.systemPrompt(), prepared.prompt(), 64000,
                        delta -> {
                            chunkCount.incrementAndGet();
                            try {
                                List<Map.Entry<String, String>> newSections = extractor.append(delta);
                                if (!newSections.isEmpty()) {
                                    sectionCount.addAndGet(newSections.size());
                                    self.persistPartialAndNotify(prepared.analysisId(), caseFileId,
                                            extractor.snapshot());
                                    persistCount.incrementAndGet();
                                }
                            } catch (Exception ex) {
                                log.warn("Partial state update failed for caseFile {} (enriched analysis continues): {}",
                                        caseFileId, ex.getMessage());
                            }
                        });
            } catch (Exception streamingFailure) {
                log.warn("Streaming Anthropic failed for enriched caseFile {} ({}), falling back to synchronous mode",
                        caseFileId, streamingFailure.getMessage());
            }
            log.info("Enriched analysis STREAMING SUMMARY caseFile={} chunks={} sections={} persists={}",
                    caseFileId, chunkCount.get(), sectionCount.get(), persistCount.get());
            if (result == null) {
                result = anthropicService.analyzeWithSystemCache(
                        ctx, prepared.systemPrompt(), prepared.prompt(), 64000);
            }
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Enriched analysis DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (PaymentRequiredException pre) {
            // F-257 — quota dépassé → FAILED localement, ne pas propager dans le listener RabbitMQ.
            log.warn("Token budget exceeded during enriched analysis caseFile {} — analysis FAILED (workspace={})",
                    caseFileId, prepared.workspaceId());
            failure = pre;
        } catch (Exception e) {
            log.error("Enriched analysis FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeEnrichedAnalysis(prepared.analysisId(), prepared.caseFileId(), result, failure, prepared.limits(),
                prepared.previousAnalysisId());
    }

    /**
     * F-190 SF-190-02 — miroir de {@link CaseAnalysisService#persistPartialAndNotify}.
     * Persiste {@code partial_state} dans une transaction REQUIRES_NEW pour visibilité
     * immédiate côté endpoint partial, puis publie un événement SSE
     * {@code ENRICHED_ANALYSIS PARTIAL} après commit.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void persistPartialAndNotify(UUID analysisId, UUID caseFileId, Map<String, String> sections) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            if (!first) json.append(",");
            json.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
            first = false;
        }
        json.append("}");

        CaseAnalysis analysis = caseAnalysisRepository.findById(analysisId).orElse(null);
        if (analysis == null) return;
        analysis.setPartialState(json.toString());
        if (analysis.getAnalysisStatus() == AnalysisStatus.PROCESSING) {
            analysis.setAnalysisStatus(AnalysisStatus.PARTIAL);
        }
        caseAnalysisRepository.save(analysis);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(
                        caseFileId, AnalysisStatus.PARTIAL, JobType.ENRICHED_ANALYSIS));
            }
        });
    }

    @Transactional
    public PreparedEnrichedAnalysis prepareEnrichedAnalysis(ReAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    j.setStatus(AnalysisStatus.PROCESSING);
                    return j;
                });

        CaseAnalysis previousAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);

        if (previousAnalysis == null) {
            log.warn("No DONE case analysis found for caseFile {} — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("No previous case analysis found");
            analysisJobRepository.save(job);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Case file not found");
            analysisJobRepository.save(job);
            return null;
        }

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis enrichedAnalysis = new CaseAnalysis();
        enrichedAnalysis.setCaseFile(caseFile);
        enrichedAnalysis.setVersion(nextVersion);
        enrichedAnalysis.setAnalysisType(AnalysisType.ENRICHED);
        enrichedAnalysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        enrichedAnalysis = caseAnalysisRepository.save(enrichedAnalysis);

        analysisDocumentSnapshotService.snapshot(enrichedAnalysis.getId(), caseFile);
        analysisQaSnapshotService.snapshot(enrichedAnalysis.getId(), caseFileId);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String legalDomain = ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL";
        String country     = ws != null ? ws.getCountry()     : "FRANCE";
        AnalysisLimitsProperties.LevelLimits limits = analysisLimitsProperties.forDomain(legalDomain).getDossier();
        List<String> litigationTypeKeys = "DROIT_DU_TRAVAIL".equals(legalDomain)
                ? legalReferentialService.getLitigationTypeKeys(country)
                : List.of();
        String systemPrompt = buildSystemPrompt(legalDomain, country, limits, litigationTypeKeys);
        String chatSummary = buildChatSummary(caseFileId);
        List<String> nonCompliantChecks;
        try {
            nonCompliantChecks = procedureCheckService.listNonCompliant(caseFile);
        } catch (Exception e) {
            log.warn("listNonCompliant failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            nonCompliantChecks = List.of();
        }
        List<String> toCheckChecks;
        try {
            toCheckChecks = procedureCheckService.listToCheck(caseFile);
        } catch (Exception e) {
            log.warn("listToCheck failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            toCheckChecks = List.of();
        }
        List<String> verifiedChecks;
        try {
            verifiedChecks = procedureCheckService.listVerified(caseFile);
        } catch (Exception e) {
            log.warn("listVerified failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            verifiedChecks = List.of();
        }
        StrategicOptionService.EnrichmentSnapshot strategicSnapshot;
        try {
            strategicSnapshot = strategicOptionService.collectForEnrichment(previousAnalysis.getId());
            if (strategicSnapshot == null) {
                strategicSnapshot = StrategicOptionService.EnrichmentSnapshot.empty();
            }
        } catch (Exception e) {
            log.warn("collectForEnrichment failed for previousAnalysis {} — enriched analysis will proceed without it",
                    previousAnalysis.getId(), e);
            strategicSnapshot = StrategicOptionService.EnrichmentSnapshot.empty();
        }
        // F-194 SF-194-01 — snapshot statuts pièces avocat pour 3 sections prompt enrichi
        // ([Pièces déjà obtenues] / [Pièces non applicables] / [Pièces à demander]).
        PieceManquanteStatusService.EnrichmentSnapshot piecesSnapshot;
        try {
            piecesSnapshot = pieceManquanteStatusService.collectForEnrichment(caseFileId);
            if (piecesSnapshot == null) {
                piecesSnapshot = PieceManquanteStatusService.EnrichmentSnapshot.empty();
            }
        } catch (Exception e) {
            log.warn("F-194: pieces collectForEnrichment failed for caseFile {} — enriched analysis will proceed without it",
                    caseFileId, e);
            piecesSnapshot = PieceManquanteStatusService.EnrichmentSnapshot.empty();
        }
        // F-195 SF-195-01 — snapshot statuts risques avocat pour 2 sections prompt enrichi
        // ([Risques validés par votre avocat — à approfondir] / [Risques écartés — NE PAS re-proposer]).
        RisqueStatusService.EnrichmentSnapshot risquesSnapshot;
        try {
            risquesSnapshot = risqueStatusService.collectForEnrichment(caseFileId);
            if (risquesSnapshot == null) {
                risquesSnapshot = RisqueStatusService.EnrichmentSnapshot.empty();
            }
        } catch (Exception e) {
            log.warn("F-195: risques collectForEnrichment failed for caseFile {} — enriched analysis will proceed without it",
                    caseFileId, e);
            risquesSnapshot = RisqueStatusService.EnrichmentSnapshot.empty();
        }
        // F-197 SF-197-01 — snapshot override avocat (type_litige Travail FR ou
        // type_procedure Immigration) lu sur l'analyse précédente. Injecté comme
        // section [Type litige fixé par l'avocat] dans le prompt pour cadrer l'IA
        // sur le type imposé (pas de tentative de re-détection).
        TypeLitigeOverrideService.OverrideSnapshot overrideSnapshot = null;
        try {
            String pT = previousAnalysis.getTypeLitigeAvocatOverride();
            String pP = previousAnalysis.getTypeProcedureAvocatOverride();
            String pR = previousAnalysis.getTypeOverrideRaison();
            if (pT != null || pP != null) {
                overrideSnapshot = new TypeLitigeOverrideService.OverrideSnapshot(pT, pP, pR);
            }
        } catch (Exception e) {
            log.warn("F-197: override snapshot read failed for previousAnalysis {} — enriched analysis will proceed without it",
                    previousAnalysis.getId(), e);
        }

        String basePrompt = buildEnrichedPrompt(caseFileId, previousAnalysis.getAnalysisResult(), chatSummary,
                nonCompliantChecks, toCheckChecks, verifiedChecks,
                strategicSnapshot.retainedTexts(), strategicSnapshot.discardedTexts(),
                piecesSnapshot.obtenues(), piecesSnapshot.nonApplicables(), piecesSnapshot.aDemander(),
                risquesSnapshot.valides(), risquesSnapshot.ecartes(), risquesSnapshot.aCreuser(),
                overrideSnapshot);
        // F-146 SF-146-01 : préfixe le prompt avec la liste des pièces pour que
        // la re-synthèse enrichie produise aussi des sourceRef précis.
        String piecesContext = piecesPromptContext.buildContextForCaseFile(caseFileId);
        String prompt = (piecesContext == null || piecesContext.isEmpty())
                ? basePrompt
                : piecesContext + "\n" + basePrompt;
        // F-257 — pré-résolution workspaceId + userId pour la construction de
        // AiCallContext dans consumeReAnalysis (un seul ctx pour streaming + fallback sync).
        UUID workspaceId = ws != null ? ws.getId()
                : caseFileRepository.findWorkspaceIdById(caseFileId).orElse(null);
        UUID userId = caseFileRepository.findCreatedByUserIdById(caseFileId).orElse(null);

        return new PreparedEnrichedAnalysis(enrichedAnalysis.getId(), prompt, systemPrompt, caseFileId, limits,
                previousAnalysis.getId(), workspaceId, userId);
    }

    @Transactional
    public void finalizeEnrichedAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure,
                                          AnalysisLimitsProperties.LevelLimits limits, UUID previousAnalysisId) {
        CaseAnalysis enrichedAnalysis = caseAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            String truncated = AnalysisJsonTruncator.truncateCaseAnalysis(result.content(), limits);
            enrichedAnalysis.setAnalysisResult(truncated);
            enrichedAnalysis.setModelUsed(result.modelUsed());
            enrichedAnalysis.setPromptTokens(result.promptTokens());
            enrichedAnalysis.setCompletionTokens(result.completionTokens());
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
            CaseAnalysisResponse.populateCounts(enrichedAnalysis, truncated);
            CaseAnalysisResponse.populateRiskScore(enrichedAnalysis, truncated);
        }
        // F-190 SF-190-02 — purger l'état partiel : remplacé par analysisResult complet
        // (DONE) ou rendu obsolète par l'échec (FAILED). Miroir CaseAnalysisService.
        enrichedAnalysis.setPartialState(null);
        caseAnalysisRepository.save(enrichedAnalysis);

        if (failure == null) {
            procedureCheckService.createChecksWithVerifiedPropagation(enrichedAnalysis,
                    enrichedAnalysis.getAnalysisResult(), previousAnalysisId);
            // F-176 SF-176-01 : extraire les nouvelles pistes IA + cloner les RETAINED + DISCARDED de l'analyse précédente
            try {
                strategicOptionService.persistFromAnalysis(enrichedAnalysis, enrichedAnalysis.getAnalysisResult());
                strategicOptionService.propagateRetainedAndDiscarded(previousAnalysisId, enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: strategic options persistence/propagation failed for enriched analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            // F-192 SF-192-01 : matérialise l'alignement RETAINED → outils, propage pieces/délais.
            // Doit être APRÈS propagateRetainedAndDiscarded pour lire les pistes RETAINED clonées.
            try {
                retainedPisteAlignmentService.materializeForAnalysis(enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: retained pistes materialization failed for enriched analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            // F-193 SF-193-01 : matérialise l'alignement procedure_checks F-96 → outils,
            // propage pieces NON_COMPLIANT + délais TO_CHECK. Doit être APRÈS F-192 pour
            // garder un ordre cohérent (chacun fail-open, pas de dépendance technique).
            try {
                procedureCheckAlignmentService.materializeForAnalysis(enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: procedure checks materialization failed for enriched analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            // F-194 SF-194-01 : matérialise l'alignement pièces (statut avocat overlay sur
            // pieces_manquantes IA), propage les délais auto PIECE_A_DEMANDER. Doit être
            // APRÈS F-192 + F-193 (qui peuvent injecter des entrées pieces_manquantes que
            // F-194 doit voir pour calculer son alignement).
            try {
                pieceManquanteAlignmentService.materializeForAnalysis(enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: pieces manquantes materialization failed for enriched analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            // F-195 SF-195-01 : matérialise l'alignement risques (statut avocat overlay sur
            // le tableau risques IA) + recompute score_risque_avocat parallèle excluant les
            // ÉCARTÉ. Doit être APRÈS F-192/F-193/F-194 — l'ordre est cohérent (chacun
            // fail-open, pas de dépendance technique). Cohérence F-IA-02 STRICTE : le
            // score_risque IA brut N'est PAS modifié.
            try {
                risqueAlignmentService.materializeForAnalysis(enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: risques materialization failed for enriched analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            // F-196 SF-196-01 : matérialise l'alignement questions complémentaires F-94
            // (réponses avocat → pieces auto via mapping keyword statique). Doit être
            // APRÈS F-192/F-193/F-194/F-195 — l'ordre est cohérent (chacun fail-open,
            // pas de dépendance technique). Cohérence F-94 STRICTE : les tables
            // ai_questions / ai_question_answers ne sont PAS modifiées.
            try {
                aiQuestionAlignmentService.materializeForAnalysis(enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: ai questions materialization failed for enriched analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            // F-197 SF-197-01 : clone l'override avocat (type_litige_avocat_override /
            // type_procedure_avocat_override / type_override_raison) depuis l'analyse
            // précédente vers la nouvelle. Évite à l'avocat de re-saisir à chaque run.
            // Fail-open : si la lecture/écriture échoue, le run continue sans override.
            try {
                typeLitigeOverrideService.cloneOverrideFromPrevious(previousAnalysisId, enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: type litige override clone failed for enriched analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            statutoryDeadlineService.createStatutoryDeadlines(enrichedAnalysis,
                    enrichedAnalysis.getAnalysisResult());
            // SF-IA-03-18 : la synthèse enrichie régénère aussi les explications de source via Haiku (fail-open).
            try {
                CaseAnalysis finalAnalysis = enrichedAnalysis;
                caseFileRepository.findById(caseFileId).ifPresent(cf -> {
                    List<SourceExplanationData> explanations = sourceExplanationGenerator.generate(cf, finalAnalysis);
                    sourceExplanationService.persist(finalAnalysis, explanations);
                });
            } catch (Exception e) {
                log.warn("Fail-open: enriched source explanation generation failed for analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
            // F-179 SF-179-01 : vérification des références jurisprudentielles citées
            // dans les documents — post-traitement fail-open, symétrique de
            // CaseAnalysisService.finalizeCaseAnalysis.
            try {
                jurisprudenceVerificationService.verifyForAnalysis(enrichedAnalysis);
            } catch (Exception e) {
                log.warn("Fail-open: enriched jurisprudence verification failed for analysis {}: {}",
                        enrichedAnalysis.getId(), e.getMessage());
            }
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setProcessedItems(1);
        job.setStatus(enrichedAnalysis.getAnalysisStatus());
        if (enrichedAnalysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Enriched analysis failed");
            logJobFailure(caseFileId, JobType.ENRICHED_ANALYSIS, "Enriched analysis failed");
        }
        analysisJobRepository.save(job);

        AnalysisStatus finalStatus = enrichedAnalysis.getAnalysisStatus();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, finalStatus, JobType.ENRICHED_ANALYSIS));
            }
        });

        // F-257 — record automatique dans AnthropicService.analyzeWithSystemCacheStreaming
        // / analyzeWithSystemCache, plus de record manuel ici.
    }

    /**
     * SF-INFRA-09 : remplace l'envoi Sentry par un log SLF4J ERROR.
     * Le mot ERROR généré par SLF4J en début de ligne déclenche le metric
     * filter CloudWatch et alimente l'alarme legalcase-production-backend-error-rate.
     */
    private void logJobFailure(UUID caseFileId, JobType jobType, String errorMessage) {
        try {
            log.error("IA job FAILED: jobType={} caseFileId={} errorMessage={}",
                    jobType, caseFileId, errorMessage);
        } catch (Exception ex) {
            log.warn("Failed to log job failure", ex);
        }
    }

    String buildChatSummary(UUID caseFileId) {
        List<ChatMessage> messages = chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId);
        if (messages.isEmpty()) return null;

        String chatText = messages.stream()
                .map(m -> "Q: %s\nR: %s".formatted(m.getQuestion(), m.getAnswer() != null ? m.getAnswer() : "(sans réponse)"))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                Tu es un assistant juridique. Résume en points clés analytiques les échanges suivants entre un avocat et l'IA sur un dossier juridique.
                Extrais uniquement les informations factuelles, les clarifications importantes et les observations de l'avocat.
                Ignore les reformulations et questions triviales. Sois concis (maximum 10 points).
                """;

        try {
            // F-257 — helper Haiku 512 tokens system-level (résumé chat pré-re-analyse).
            // Skip gate user mais record obligatoire (JobType.SYSTEM_CHAT_SUMMARY) avec
            // userId=null pour traçabilité globale du coût Anthropic.
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_CHAT_SUMMARY, caseFileId);
            AnthropicResult result = anthropicService.analyzeFast(ctx, systemPrompt, chatText, 512);
            String summary = result.content();
            return (summary != null && !summary.isBlank()) ? summary.trim() : null;
        } catch (Exception e) {
            log.warn("Chat summary failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            return null;
        }
    }

    /** Overload F-96 (sans pistes stratégiques) — utilisé par les tests existants. */
    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult, String chatSummary,
                                List<String> nonCompliantChecks, List<String> toCheckChecks,
                                List<String> verifiedChecks) {
        return buildEnrichedPrompt(caseFileId, previousAnalysisResult, chatSummary,
                nonCompliantChecks, toCheckChecks, verifiedChecks, List.of(), List.of(),
                List.of(), List.of(), List.of());
    }

    /** Overload F-176 (sans pieces statuts F-194) — utilisé par les tests existants. */
    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult, String chatSummary,
                                List<String> nonCompliantChecks, List<String> toCheckChecks,
                                List<String> verifiedChecks,
                                List<String> retainedStrategicOptions, List<String> discardedStrategicOptions) {
        return buildEnrichedPrompt(caseFileId, previousAnalysisResult, chatSummary,
                nonCompliantChecks, toCheckChecks, verifiedChecks,
                retainedStrategicOptions, discardedStrategicOptions,
                List.of(), List.of(), List.of());
    }

    /** Overload F-194 (sans risques statuts F-195) — utilisé par les tests existants. */
    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult, String chatSummary,
                                List<String> nonCompliantChecks, List<String> toCheckChecks,
                                List<String> verifiedChecks,
                                List<String> retainedStrategicOptions, List<String> discardedStrategicOptions,
                                List<String> piecesObtenues, List<String> piecesNonApplicables,
                                List<String> piecesADemander) {
        return buildEnrichedPrompt(caseFileId, previousAnalysisResult, chatSummary,
                nonCompliantChecks, toCheckChecks, verifiedChecks,
                retainedStrategicOptions, discardedStrategicOptions,
                piecesObtenues, piecesNonApplicables, piecesADemander,
                List.of(), List.of(), List.of());
    }

    /** Overload F-195 (sans override avocat F-197) — utilisé par les tests existants. */
    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult, String chatSummary,
                                List<String> nonCompliantChecks, List<String> toCheckChecks,
                                List<String> verifiedChecks,
                                List<String> retainedStrategicOptions, List<String> discardedStrategicOptions,
                                List<String> piecesObtenues, List<String> piecesNonApplicables,
                                List<String> piecesADemander,
                                List<String> risquesValides,
                                List<RisqueStatusService.EcarteEntry> risquesEcartes,
                                List<String> risquesACreuser) {
        return buildEnrichedPrompt(caseFileId, previousAnalysisResult, chatSummary,
                nonCompliantChecks, toCheckChecks, verifiedChecks,
                retainedStrategicOptions, discardedStrategicOptions,
                piecesObtenues, piecesNonApplicables, piecesADemander,
                risquesValides, risquesEcartes, risquesACreuser,
                null);
    }

    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult, String chatSummary,
                                List<String> nonCompliantChecks, List<String> toCheckChecks,
                                List<String> verifiedChecks,
                                List<String> retainedStrategicOptions, List<String> discardedStrategicOptions,
                                List<String> piecesObtenues, List<String> piecesNonApplicables,
                                List<String> piecesADemander,
                                List<String> risquesValides,
                                List<RisqueStatusService.EcarteEntry> risquesEcartes,
                                List<String> risquesACreuser,
                                TypeLitigeOverrideService.OverrideSnapshot overrideSnapshot) {
        List<AiQuestion> questions = aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId);

        List<AiQuestion> answeredQuestions = questions.stream()
                .filter(q -> "ANSWERED".equals(q.getStatus()))
                .toList();

        String qaSection = IntStream.range(0, answeredQuestions.size())
                .mapToObj(i -> {
                    AiQuestion q = answeredQuestions.get(i);
                    String answerText = aiQuestionAnswerRepository
                            .findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())
                            .map(AiQuestionAnswer::getAnswerText)
                            .orElse("(sans réponse)");
                    return "Q%d : %s\nR%d : %s".formatted(i + 1, q.getQuestionText(), i + 1, answerText);
                })
                .collect(Collectors.joining("\n"));

        StringBuilder prompt = new StringBuilder();
        prompt.append("[Synthèse précédente]\n").append(previousAnalysisResult).append("\n\n");

        // SF — injecter les 2000 premiers car de chaque doc brut pour que Claude puisse
        // vérifier la classification baseline contre les pièces signées réelles, au lieu
        // de se fier uniquement à la synthèse précédente (risque de dérive sur l'enrichie).
        String rawDocs = buildRawDocumentsSection(caseFileId);
        if (!rawDocs.isEmpty()) {
            prompt.append("[Pièces du dossier — extraits bruts pour vérification baseline]\n")
                  .append(rawDocs).append("\n\n");
        }

        prompt.append("[Questions et réponses de l'avocat]\n")
              .append(qaSection.isEmpty() ? "(aucune réponse)" : qaSection);

        if (chatSummary != null && !chatSummary.isBlank()) {
            prompt.append("\n\n[Échanges libres avec l'assistant — points clés]\n").append(chatSummary);
        }

        if (nonCompliantChecks != null && !nonCompliantChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux non conformes]\n");
            nonCompliantChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        if (toCheckChecks != null && !toCheckChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux à vérifier — non encore qualifiés par l'avocat]\n");
            toCheckChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        if (verifiedChecks != null && !verifiedChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux vérifiés — à reconsidérer si nécessaire]\n");
            verifiedChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        // F-176 SF-176-01 : injection des pistes stratégiques de l'analyse précédente
        if (retainedStrategicOptions != null && !retainedStrategicOptions.isEmpty()) {
            prompt.append("\n\n[Pistes stratégiques retenues à approfondir]\n");
            retainedStrategicOptions.forEach(p -> prompt.append("- ").append(p).append("\n"));
        }

        if (discardedStrategicOptions != null && !discardedStrategicOptions.isEmpty()) {
            prompt.append("\n\n[Pistes stratégiques écartées — NE PAS re-proposer]\n");
            discardedStrategicOptions.forEach(p -> prompt.append("- ").append(p).append("\n"));
        }

        // F-194 SF-194-01 — 3 sections statuts pièces avocat
        if (piecesObtenues != null && !piecesObtenues.isEmpty()) {
            prompt.append("\n\n[Pièces déjà obtenues — ne pas réclamer]\n");
            piecesObtenues.forEach(p -> prompt.append("- ").append(p).append("\n"));
        }

        if (piecesNonApplicables != null && !piecesNonApplicables.isEmpty()) {
            prompt.append("\n\n[Pièces non applicables au dossier — ne pas mentionner]\n");
            piecesNonApplicables.forEach(p -> prompt.append("- ").append(p).append("\n"));
        }

        if (piecesADemander != null && !piecesADemander.isEmpty()) {
            prompt.append("\n\n[Pièces à demander au client — pousser explicitement dans la nouvelle synthèse]\n");
            piecesADemander.forEach(p -> prompt.append("- ").append(p).append("\n"));
        }

        // F-195 SF-195-01 — sections risques curés par l'avocat
        if (risquesValides != null && !risquesValides.isEmpty()) {
            prompt.append("\n\n[Risques validés par votre avocat — à approfondir]\n");
            risquesValides.forEach(r -> prompt.append("- ").append(r).append("\n"));
        }

        if (risquesEcartes != null && !risquesEcartes.isEmpty()) {
            prompt.append("\n\n[Risques écartés — NE PAS re-proposer]\n");
            for (RisqueStatusService.EcarteEntry e : risquesEcartes) {
                prompt.append("- ").append(e.libelle());
                if (e.raison() != null && !e.raison().isBlank()) {
                    prompt.append(" (raison : ").append(e.raison()).append(")");
                }
                prompt.append("\n");
            }
        }

        if (risquesACreuser != null && !risquesACreuser.isEmpty()) {
            prompt.append("\n\n[Risques en cours d'instruction par votre avocat]\n");
            risquesACreuser.forEach(r -> prompt.append("- ").append(r).append("\n"));
        }

        // F-197 SF-197-01 — section override avocat sur le type_litige (Travail) ou
        // type_procedure (Immigration). Instruit l'IA de cadrer son analyse sur le
        // type imposé par l'avocat (pas de tentative de re-détection sur ce champ).
        if (overrideSnapshot != null
                && (overrideSnapshot.typeLitige() != null || overrideSnapshot.typeProcedure() != null)) {
            prompt.append("\n\n[Type litige fixé par l'avocat]\n");
            if (overrideSnapshot.typeLitige() != null) {
                prompt.append("- type_litige_detecte = ").append(overrideSnapshot.typeLitige()).append("\n");
            }
            if (overrideSnapshot.typeProcedure() != null) {
                prompt.append("- type_procedure_detectee = ").append(overrideSnapshot.typeProcedure()).append("\n");
            }
            if (overrideSnapshot.raison() != null && !overrideSnapshot.raison().isBlank()) {
                prompt.append("Raison de l'avocat : ").append(overrideSnapshot.raison()).append("\n");
            }
            prompt.append("CONSIGNE : cadre ton analyse sur ce type imposé. Ne tente pas de le re-détecter ; reflète-le tel quel dans la sortie JSON.\n");
        }

        return prompt.toString();
    }

    /** Construit la section extraits bruts documents pour le prompt enrichi. */
    private String buildRawDocumentsSection(UUID caseFileId) {
        var docs = documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId);
        if (docs.isEmpty()) return "";
        var docIds = docs.stream().map(fr.ailegalcase.document.Document::getId).toList();
        var extractionsByDocId = documentExtractionRepository.findByDocumentIdIn(docIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getDocument().getId(), e -> e, (a, b) -> a, java.util.HashMap::new));
        StringBuilder sb = new StringBuilder();
        for (var doc : docs) {
            var ex = extractionsByDocId.get(doc.getId());
            if (ex == null
                    || ex.getExtractionStatus() != fr.ailegalcase.document.ExtractionStatus.DONE
                    || ex.getExtractedText() == null || ex.getExtractedText().isBlank()) {
                continue;
            }
            String text = ex.getExtractedText();
            String slice = text.length() <= RAW_DOC_PREFIX_CHARS
                    ? text : text.substring(0, RAW_DOC_PREFIX_CHARS) + " [...]";
            sb.append("=== ").append(doc.getOriginalFilename()).append(" ===\n")
              .append(slice.replace("\n", " ").trim()).append("\n\n");
        }
        return sb.toString();
    }
}

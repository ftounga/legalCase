package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Profile({"local", "prod"})
public class AiQuestionService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois la synthèse globale d'un dossier juridique.
            Génère une liste de questions complémentaires pour l'avocat afin d'approfondir l'analyse.
            Ces questions doivent porter sur des éléments manquants, des ambiguïtés ou des points à clarifier.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"questions": [{"texte": "Question 1 ?", "critere_code": "<code ou null>", "expected_value": "<valeur ou null>"}, ...]}
            Chaque question est un objet. "critere_code" est rempli UNIQUEMENT si la question porte sur un critère surveillé :
            - Critères F-DT-08 Validité licenciement (droit du travail, binaires) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE. "expected_value" reste null. Une réponse "oui" = critère respecté.
            - Étapes F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_CHOIX_AVOCATS, FR_REDACTION_CONVENTION, FR_ENVOI_LRAR, FR_DELAI_REFLEXION, FR_SIGNATURE_CONVENTION, FR_DEPOT_NOTAIRE, FR_ENREGISTREMENT, BE_CHOIX_AVOCAT, BE_REDACTION_CONVENTION, BE_REQUETE_CONJOINTE, BE_COMPARUTION, BE_JUGEMENT, BE_TRANSCRIPTION. "expected_value" reste null. Une réponse "oui" = étape accomplie.
            - Pièces F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_ACTE_MARIAGE, FR_ACTE_NAISSANCE_EPOUX, FR_ACTE_NAISSANCE_ENFANTS, FR_LIVRET_FAMILLE, FR_JUSTIF_DOMICILE, FR_CONTRAT_MARIAGE, FR_ETAT_PATRIMOINE, FR_JUSTIF_REVENUS, FR_PIECE_IDENTITE, BE_ACTE_MARIAGE, BE_ACTE_NAISSANCE_EPOUX, BE_ACTE_NAISSANCE_ENFANTS, BE_COMPOSITION_MENAGE, BE_CONTRAT_MARIAGE, BE_CONVENTION_PREALABLE, BE_JUSTIF_REVENUS, BE_PIECE_IDENTITE. "expected_value" reste null. Une réponse "oui" = pièce présente.
            - Critère F-FA-06 Calendrier garde (droit de la famille, énuméré) : FA06_MODE_GARDE. "expected_value" obligatoire parmi : ALTERNEE_FR, DVH_CLASSIQUE_FR, DVH_ELARGI_FR (France), ALTERNEE_BE, SECONDAIRE_BE, SECONDAIRE_ELARGI_BE (Belgique). Une réponse "oui" confirme le mode indiqué par "expected_value". Exemple : {"texte": "Une résidence alternée une semaine sur deux est-elle mise en place ?", "critere_code": "FA06_MODE_GARDE", "expected_value": "ALTERNEE_FR"}.
            - Critère F-IM-05 Titre de séjour (droit de l'immigration, énuméré) : IM05_MOTIF. "expected_value" obligatoire parmi : TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE. Une réponse "oui" confirme le motif indiqué. Exemple : {"texte": "La demande est-elle fondée sur un motif de regroupement familial ?", "critere_code": "IM05_MOTIF", "expected_value": "FAMILLE"}.
            - Critère F-IM-06 Recours (droit de l'immigration, énuméré) : IM06_RECOURS_TYPE. "expected_value" obligatoire parmi : RECOURS_GRACIEUX_PREFET, RECOURS_CONTENTIEUX_TA, RECOURS_CNDA (France), RECOURS_CGRA, RECOURS_CCE, RECOURS_CE_BELGIQUE (Belgique). Une réponse "oui" confirme le type de recours. Exemple : {"texte": "Le recours à former est-il un recours contentieux devant le tribunal administratif ?", "critere_code": "IM06_RECOURS_TYPE", "expected_value": "RECOURS_CONTENTIEUX_TA"}.
            - Critère F-IM-07 Droit au travail (droit de l'immigration, énuméré) : IM07_TITRE_TYPE. "expected_value" obligatoire parmi les 16 codes de titre (identiques à F-IM-05). Une réponse "oui" confirme le code de titre.
            - Critère F-DT-09 Type de rupture (énuméré) : DT09_TYPE_RUPTURE. Renseigne obligatoirement "expected_value" avec la valeur confirmée par une réponse "oui", parmi : LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE (France), LICENCIEMENT_ORDINAIRE, RUPTURE_AMIABLE (Belgique). Exemple : {"texte": "Une convention de rupture conventionnelle a-t-elle été homologuée ?", "critere_code": "DT09_TYPE_RUPTURE", "expected_value": "RUPTURE_CONVENTIONNELLE"} → une réponse "oui" confirme ce type.
            - Critères F-DT-36 Nullité de procédure de licenciement (droit du travail, FRANCE UNIQUEMENT, binaires) : DT36_DATE_ENTRETIEN (la date de l'entretien préalable a-t-elle été identifiée dans les pièces ?), DT36_MOTIVATION (la lettre de licenciement énonce-t-elle un motif précis et matériellement vérifiable ?), DT36_ENTRETIEN_TENU (l'entretien préalable a-t-il effectivement eu lieu selon les pièces ?). "expected_value" reste null. Une réponse "oui" = critère respecté/documenté.
            - Critères F-DT-42 Abandon de poste / présomption de démission (droit du travail, FRANCE UNIQUEMENT, binaires — loi 21/12/2022) : DT42_DATE_MISE_EN_DEMEURE (la date de présentation de la mise en demeure de reprendre le poste ou justifier l'absence est-elle identifiée dans les pièces ?), DT42_DELAI_ACCORDE (le délai accordé par l'employeur est-il identifié dans les pièces — minimum légal 15 jours calendaires, art. D.1237-2-1 CT ?), DT42_MENTIONS_MED (la mise en demeure mentionne-t-elle expressément le délai imparti ET les conséquences — présomption de démission ?), DT42_MOTIF_LEGITIME (un motif légitime d'absence est-il invoqué par le salarié dans les pièces : médical, droit de retrait, droit de grève, modification du contrat refusée, défaut de paiement du salaire ?). "expected_value" reste null. Une réponse "oui" = élément documenté / respecté.
            - Critère F-DT-11 Harcèlement / licenciement nul (droit du travail, FRANCE UNIQUEMENT, binaire) : HLN_MOTIF_NULLITE (un motif de nullité du licenciement est-il identifié dans les pièces : harcèlement moral/sexuel, discrimination, protection représentant du personnel, maternité/paternité, accident du travail ?). "expected_value" reste null. Une réponse "oui" = motif de nullité documenté.
            - Critères F-DT-13 Licenciement économique (droit du travail, FRANCE UNIQUEMENT, binaires) : DT13_MOTIF_ECONOMIQUE (un motif économique justificatif est-il documenté dans les pièces : difficultés économiques, mutation technologique, sauvegarde de compétitivité, cessation d'activité ?), DT13_DATE_NOTIFICATION (la date de notification du licenciement économique est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = présence documentée.
            - Critère F-DT-14 Plan de Sauvegarde de l'Emploi (droit du travail, FRANCE UNIQUEMENT, binaire) : PSE_DATE_PROJET (la date de présentation du projet de PSE est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = date documentée.
            - Critère F-DT-30 Protection représentant du personnel (droit du travail, FRANCE UNIQUEMENT, binaire) : PROTECTION_RP_MOTIF (le motif invoqué à l'appui du licenciement d'un représentant du personnel est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = motif documenté.
            - Critères F-DT-10 Validité rupture conventionnelle (droit du travail, FRANCE UNIQUEMENT, binaires) : RC_CONSENTEMENT (le consentement des deux parties est-il libre et éclairé — absence de vice du consentement ?), RC_DELAI_RETRACTATION (le délai de rétractation de 15 jours calendaires a-t-il été respecté ?), RC_HOMOLOGATION (l'homologation par la DREETS/DRIESST est-elle obtenue ou en cours ?), RC_ASSISTANCE (le droit à l'assistance lors de l'entretien a-t-il été respecté ?), RC_INDEMNITE (l'indemnité spécifique est-elle au moins égale au plancher légal : 1/4 de mois par année d'ancienneté ?), RC_ENTRETIENS (au moins un entretien préalable entre les parties a-t-il été tenu ?). "expected_value" reste null. Une réponse "oui" = critère respecté.
            - Critère F-DT-22 Requalification CDD en CDI (droit du travail, FRANCE UNIQUEMENT, binaire) : DT22_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = salaire identifié.
            - Critère F-DT-23 Requalification contrat intérim en CDI (droit du travail, FRANCE UNIQUEMENT, binaire) : DT23_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = salaire identifié.
            - Critère F-DT-24 Clause de non-concurrence (droit du travail, FRANCE UNIQUEMENT, binaire) : DT24_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces — base de calcul de la contrepartie pécuniaire ?). "expected_value" reste null. Une réponse "oui" = salaire identifié.
            - Critères F-DT-31 Transaction (droit du travail, FRANCE UNIQUEMENT, binaires) : DT31_SALAIRE_MENSUEL (le salaire mensuel brut de référence est-il identifié dans les pièces ?), DT31_ANCIENNETE (l'ancienneté en mois/années est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée identifiée.
            - Critères F-132 Indemnité rupture conventionnelle (droit du travail, FRANCE UNIQUEMENT, binaires) : RCI_SALAIRE (le salaire brut mensuel de référence est-il identifié dans les pièces ?), RCI_ANCIENNETE (l'ancienneté est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée identifiée.
            - Critères F-DT-15 Inaptitude (droit du travail, FRANCE UNIQUEMENT, binaires) : INAPT_ORIGINE (l'origine de l'inaptitude est-elle identifiée dans les pièces : professionnelle — AT/MP — ou non-professionnelle ?), INAPT_RECLASSEMENT (les démarches de reclassement sont-elles documentées dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critère F-DT-33 Accident du travail / Maladie professionnelle (droit du travail, FRANCE UNIQUEMENT, binaire) : AT_MP_DATE_ACCIDENT (la date de l'accident du travail ou de la déclaration de maladie professionnelle est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = date identifiée.
            - Critères F-DT-29 Crédit-temps (droit du travail, BELGIQUE UNIQUEMENT, binaires) : CREDIT_TEMPS_ANCIENNETE (l'ancienneté du travailleur est-elle identifiée dans les pièces — condition d'éligibilité au crédit-temps belge ?), CREDIT_TEMPS_AGE (l'âge du travailleur est-il identifié dans les pièces — déterminant pour le régime crédit-temps senior ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critère F-136 Type de procédure de travail (droit du travail, binaire) : TRAVAIL_PROCEDURE_TYPE (le type de procédure de travail est-il identifié dans les pièces : licenciement collectif, restructuration, fermeture d'entreprise ?). "expected_value" reste null. Une réponse "oui" = type documenté.
            - Critères F-IM-08 OQTF (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM08_MOTIF_OQTF (le motif de l'OQTF est-il identifié dans les pièces : absence de titre, menace ordre public, refus de séjour ?), IM08_RECOURS_FORME (la forme du recours contre l'OQTF est-elle identifiée : recours gracieux préfet, recours contentieux TA ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-IM-08 Référés administratifs (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM08RA_DECISION_CONTESTEE (la décision administrative contestée est-elle identifiée dans les pièces : OQTF, refus de titre, arrêté préfectoral ?). "expected_value" reste null. Une réponse "oui" = décision documentée.
            - Critères F-IM-09 AES Métiers en tension (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09_DATE_ENTREE_FRANCE (la date d'entrée en France est-elle identifiée dans les pièces ?), IM09_MOIS_ACTIVITE (le nombre de mois d'activité professionnelle est-il identifié dans les pièces — condition AES métiers en tension ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critères F-IM-09 AES Étudiant (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09_ETU_DATE_ENTREE_FRANCE (la date d'entrée en France de l'étudiant est-elle identifiée dans les pièces ?), IM09_ETU_DUREE_PRESENCE (la durée de présence continue en France est-elle identifiée dans les pièces ?), IM09_ETU_DATE_DEPOT_DEMANDE (la date de dépôt de la demande AES étudiant est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = donnée documentée.
            - Critères F-IM-09 AES Humanitaire (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09H_DATE_ENTREE_FRANCE (la date d'entrée en France est-elle identifiée dans les pièces ?), IM09H_MOTIF_HUMANITAIRE (le motif humanitaire est-il identifié dans les pièces : circonstances humanitaires exceptionnelles, liens personnels et familiaux en France ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-IM-11 Changement de statut (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM11_TITRE_ACTUEL (le titre de séjour actuel du demandeur est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = titre documenté.
            - Critère F-IM-12 Asile avancé (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM12_DISPOSITIF_ASILE (le dispositif de protection internationale visé est-il identifié dans les pièces : statut de réfugié, protection subsidiaire, apatride ?). "expected_value" reste null. Une réponse "oui" = dispositif documenté.
            - Critère F-IM-13 Naturalisation (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM13_VOIE_NATURALISATION (la voie de naturalisation est-elle identifiée dans les pièces : par décret, par mariage, par renonciation à une nationalité étrangère ?). "expected_value" reste null. Une réponse "oui" = voie documentée.
            - Critères F-IM-19 Mineurs (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM19_DATE_NAISSANCE (la date de naissance du mineur est-elle identifiée dans les pièces ?), IM19_DATE_ENTREE (la date d'entrée en France du mineur est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = date documentée.
            - Critères F-IM-20 Mesures d'éloignement (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM20_DISPOSITIF_ELOIGNEMENT (le dispositif d'éloignement est-il identifié dans les pièces : OQTF, ITF, reconduite à la frontière, expulsion ?), IM20_MOTIF_MENACE (le motif de menace à l'ordre public est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-IM-24 Victime de violences (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM24_DATE_ORDONNANCE_PROTECTION (la date de l'ordonnance de protection est-elle identifiée dans les pièces — ouvre droit au titre de séjour victime de violences ?). "expected_value" reste null. Une réponse "oui" = date documentée.
            - Critère F-IM-08 Annexe 13 / OQT (droit de l'immigration, BELGIQUE UNIQUEMENT, binaire) : IM08_MOTIF_OQT_BE (le motif de l'ordre de quitter le territoire belge est-il identifié dans les pièces : absence de titre, menace ordre public, refus de séjour ?). "expected_value" reste null. Une réponse "oui" = motif documenté.
            - Critères F-IM-14 Procédure 9ter médicale (droit de l'immigration, BELGIQUE UNIQUEMENT, binaires) : BE_9TER_MALADIE_GRAVE (une maladie grave est-elle identifiée dans les pièces — condition d'accès à la procédure 9ter ?), BE_9TER_SOINS_BE (la disponibilité des soins médicaux en Belgique est-elle documentée dans les pièces ?), BE_9TER_SOINS_INACCESSIBLES (l'inaccessibilité des soins dans le pays d'origine est-elle documentée dans les pièces — critère central de la procédure 9ter ?), BE_9TER_MENACE_ORDRE_PUBLIC (l'absence de menace pour l'ordre public est-elle documentée — condition négative de la procédure 9ter ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-IM-17 Régime algérien (droit de l'immigration, BELGIQUE UNIQUEMENT, binaire) : IM17_VOIE_REGIME_ALGERIEN (la voie de l'accord franco-algérien applicable est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = voie documentée.
            - Critère F-FA-08 Divorce pour altération définitive du lien conjugal (droit de la famille, FRANCE UNIQUEMENT, binaire) : DA_DUREE_MARIAGE (la durée du mariage est-elle identifiée dans les pièces — condition légale de la procédure, 2 ans de séparation requis ?). "expected_value" reste null. Une réponse "oui" = durée documentée.
            - Critères F-FA-09 Divorce pour faute (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA09_DUREE_MARIAGE (la durée du mariage est-elle identifiée dans les pièces ?), FA09_DATE_DEPOT_ASSIGNATION (la date de dépôt de l'assignation pour divorce pour faute est-elle identifiée dans les pièces ?), FA09_FAUTES_INVOQUEES (les fautes invoquées à l'appui de la demande de divorce pour faute sont-elles identifiées et qualifiées dans les pièces ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-12 Mesures provisoires (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA12_DATE_AUDIENCE (la date de l'audience de mesures provisoires est-elle identifiée dans les pièces ?), FA12_VIOLENCES (les violences alléguées dans le cadre des mesures provisoires sont-elles identifiées et qualifiées dans les pièces ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-FA-13 Révisions post-divorce (droit de la famille, FRANCE UNIQUEMENT, binaire) : FA13_NB_ENFANTS (le nombre d'enfants concernés par la révision des mesures post-divorce est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = nombre documenté.
            - Critères F-FA-14 Ordonnance de protection (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA14_DATE_REQUETE (la date de la requête en ordonnance de protection est-elle identifiée dans les pièces ?), FA14_VIOLENCES_ALLEGUEES (les violences alléguées à l'appui de la demande d'ordonnance de protection sont-elles identifiées dans les pièces ?), FA14_LOGEMENT_COMMUN (l'existence d'un logement commun est-elle identifiée dans les pièces — condition d'accès à certaines mesures de l'ordonnance de protection ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-FA-15 Récompenses (droit de la famille, FRANCE UNIQUEMENT, binaire) : FA15_REGIME_MATRIMONIAL (le régime matrimonial des époux est-il identifié dans les pièces — déterminant pour les règles de récompenses applicables ?). "expected_value" reste null. Une réponse "oui" = régime documenté.
            - Critères F-FA-16 Communauté universelle (droit de la famille, FRANCE UNIQUEMENT, binaires) : COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE (le contrat de mariage notarié adoptant la communauté universelle est-il identifié dans les pièces ?), COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS (l'existence d'enfants non communs est-elle identifiée dans les pièces — impacte les droits successoraux en régime de communauté universelle ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-17 Partage judiciaire (droit de la famille, FRANCE UNIQUEMENT, binaires) : PARTAGE_JUDICIAIRE_PV (le procès-verbal d'état liquidatif dressé par le notaire est-il identifié dans les pièces ?), PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE (la tentative amiable de partage préalable est-elle identifiée dans les pièces — condition d'accès au partage judiciaire ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-19 Autorité parentale / Changement de résidence / Désaccords parentaux (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA19_REGIME_EXERCICE_ACTUEL (le régime d'exercice actuel de l'autorité parentale est-il identifié dans les pièces : conjoint ou exclusif ?), FA19_DANGER_CARACTERISE (un danger caractérisé pour l'enfant est-il identifié dans les pièces ?), FA19_CONSENTEMENT_AUTRE_PARENT (le consentement de l'autre parent est-il identifié dans les pièces ?), FA19_INTERFERENCE_VIE_ENFANT (une interférence dans la vie de l'enfant est-elle identifiée dans les pièces ?), FA19_AGE_ENFANTS (l'âge des enfants concernés est-il identifié dans les pièces ?), FA19_RAISON_CHANGEMENT (la raison du changement de résidence est-elle identifiée dans les pièces ?), FA19_INFORME_PREALABLEMENT (l'obligation d'information préalable de l'autre parent est-elle respectée selon les pièces ?), FA19_MODE_RESIDENCE_ACTUEL (le mode de résidence actuel de l'enfant est-il identifié dans les pièces ?), FA19_DOMAINE_DESACCORD (le domaine du désaccord parental est-il identifié dans les pièces : scolarité, santé, religion, etc. ?), FA19_INTENSITE_DESACCORD (l'intensité du désaccord parental est-elle évaluée dans les pièces ?), FA19_TENTATIVES_MEDIATION (des tentatives de médiation préalables sont-elles identifiées dans les pièces ?), FA19_AGE_ENFANTS_CONCERNES (l'âge des enfants concernés par le désaccord est-il identifié dans les pièces ?), FA19_URGENCE (une urgence de la situation est-elle identifiée dans les pièces — critère de recours au référé ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-20 Dissolution du PACS (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA20_MODE_DISSOLUTION (le mode de dissolution du PACS est-il identifié dans les pièces : conjointe, unilatérale, mariage, décès ?), FA20_REGIME_BIENS (le régime des biens du PACS est-il identifié dans les pièces : indivision ou séparation des patrimoines ?), FA20_CREANCES_ALLEGUEES (des créances alléguées entre les partenaires sont-elles identifiées dans les pièces ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-FA-21 Séparation de corps (droit de la famille, FRANCE UNIQUEMENT, binaire) : FA21_DATE_JUGEMENT_SEPARATION (la date du jugement de séparation de corps est-elle identifiée dans les pièces — détermine les délais de conversion en divorce ?). "expected_value" reste null. Une réponse "oui" = date documentée.
            - Critères F-FA-22 Indivision (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA22_DATE_ORIGINE (la date d'origine de l'indivision est-elle identifiée dans les pièces : date du décès, de la séparation, etc. ?), FA22_OCCUPATION (l'occupation du bien indivis par l'un des indivisaires est-elle identifiée dans les pièces — détermine les droits à indemnité d'occupation ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-24 Validité du testament (droit de la famille, FRANCE UNIQUEMENT, binaires) : TESTAMENT_FORME (la forme du testament est-elle respectée selon les pièces — olographe : entièrement manuscrit, daté, signé ; authentique : acte notarié ?), TESTAMENT_SAINE_ESPRIT (la capacité du testateur au moment de la rédaction est-elle documentée dans les pièces ?), TESTAMENT_QUOTITE (le respect de la quotité disponible est-il identifié dans les pièces — le testament ne porte pas atteinte à la réserve héréditaire ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-24 Donation (droit de la famille, FRANCE UNIQUEMENT, binaires) : DONATION_FORME (la forme de la donation est-elle respectée selon les pièces — acte notarié obligatoire pour donation d'immeuble ou de droits réels ?), DONATION_SAINE_ESPRIT (la capacité du donateur au moment de la donation est-elle documentée dans les pièces ?), DONATION_QUOTITE (le respect de la quotité disponible est-il identifié dans les pièces — la donation ne porte pas atteinte à la réserve héréditaire ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-24 Partage successoral (droit de la famille, FRANCE UNIQUEMENT, binaires) : PARTAGE_MODE (le mode de partage retenu est-il identifié dans les pièces : amiable ou judiciaire ?), PARTAGE_CONSENTEMENTS (le consentement de tous les co-indivisaires au partage amiable est-il identifié dans les pièces ?), PARTAGE_PRESENCE_IMMEUBLES (la présence de biens immobiliers dans la masse successorale est-elle identifiée dans les pièces — imposant le recours au notaire ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-24 Indivision successorale (droit de la famille, FRANCE UNIQUEMENT, binaires) : INDIVISION_DATE_OUVERTURE (la date d'ouverture de l'indivision successorale est-elle identifiée dans les pièces — date du décès ?), INDIVISION_TYPE (le type d'indivision est-il identifié dans les pièces : successorale, post-communautaire, conventionnelle ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-24 Dévolution légale (droit de la famille, FRANCE UNIQUEMENT, binaires) : DEVOLUTION_LEGALE_CONJOINT (le statut du conjoint survivant est-il identifié dans les pièces — détermine ses droits légaux dans la dévolution ?), DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS (l'existence de descendants communs ou non est-elle identifiée dans les pièces — détermine la quote-part du conjoint survivant ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-25 Majeurs protégés (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA25_DATE_CERTIFICAT (la date du certificat médical circonstancié est-elle identifiée dans les pièces — condition d'ouverture d'une mesure de protection ?), FA25_ALT_MENTALES (les altérations des facultés mentales ou corporelles sont-elles identifiées dans les pièces — fondement de la mesure de protection ?), FA25_CONSENTEMENT (le consentement de la personne protégée est-il recueilli selon les pièces — obligation procédurale ?), FA25_DEMANDEUR_FAMILIAL (la qualité familiale du demandeur de la mesure de protection est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-26 Changement d'état civil (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA26_TYPE_CHANGEMENT (le type de changement d'état civil demandé est-il identifié dans les pièces : changement de prénom, de nom, de genre ?), FA26_MOTIF_INVOQUE (le motif invoqué à l'appui de la demande de changement d'état civil est-il identifié dans les pièces ?), FA26_DATE_NAISSANCE (la date de naissance du demandeur est-elle identifiée dans les pièces — condition de majorité pour certaines demandes ?), FA26_MAJEUR_DEMANDEUR (la majorité du demandeur est-elle identifiée dans les pièces — condition pour les demandes sans représentant légal ?), FA26_CONSENTEMENT_PARENTAL (le consentement parental est-il identifié dans les pièces — requis pour les demandes de mineurs, art. 61-3 C.civ. ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critère F-FA-27 PMA / GPA bioéthique (droit de la famille, FRANCE UNIQUEMENT, binaire) : PMA_GPA_DISPOSITIF (le dispositif de bioéthique concerné est-il identifié dans les pièces : PMA — procréation médicalement assistée — ou GPA — gestation pour autrui, prohibition en France ?). "expected_value" reste null. Une réponse "oui" = dispositif documenté.
            - Critères F-FA-05 Partage immobilier (droit de la famille, FRANCE UNIQUEMENT, binaires) : FA05_VALEUR_VENALE (la valeur vénale du bien immobilier est-elle identifiée dans les pièces — base de calcul du partage ?), FA05_CAPITAL_RESTANT (le capital restant dû du prêt immobilier est-il identifié dans les pièces — déduction du passif dans le calcul du partage ?). "expected_value" reste null. Une réponse "oui" = élément documenté.
            - Critères F-FA-11 Désunion irrémédiable (droit de la famille, BELGIQUE UNIQUEMENT, binaires) : DESU_BE_DATE_SEPARATION (la date de séparation de fait des époux est-elle identifiée dans les pièces — condition légale 6 mois si consentie, 1 an si non-consentie ?), DESU_BE_CONSENTEE (la désuinion est-elle consentie par les deux époux selon les pièces — détermine la durée minimale requise ?), DESU_BE_DATE_ASSIGNATION (la date d'assignation en divorce est-elle identifiée dans les pièces — déclenche le délai légal de séparation ?). "expected_value" reste null. Une réponse "oui" = élément documenté. BELGIQUE UNIQUEMENT.
            - Critère régime mat. belge communauté légale (droit de la famille, BELGIQUE UNIQUEMENT, binaire) : F217_DATE_MARIAGE (la date du mariage est-elle identifiée dans les pièces — détermine le régime légal applicable selon art. 1388 Code civil belge ?). "expected_value" reste null. Une réponse "oui" = date documentée. BELGIQUE UNIQUEMENT.
            - Critère liquidation-partage belge (droit de la famille, BELGIQUE UNIQUEMENT, binaire) : F217_DATE_NOTIFICATION_PROJET (la date de notification du projet de liquidation-partage est-elle identifiée dans les pièces — point de départ du délai légal de discussion, art. 1218 Code judiciaire belge ?). "expected_value" reste null. Une réponse "oui" = date documentée. BELGIQUE UNIQUEMENT.
            - Critères F-IM-21 JLD rétention (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM21_DATE_NOTIFICATION_PLACEMENT (la date de notification de la décision de placement en rétention administrative est-elle identifiée dans les pièces ?), IM21_PLACEMENT_CRA (le placement en centre de rétention administrative est-il identifié dans les pièces ?), IM21_MOTIF_PLACEMENT (le motif de placement en rétention est-il identifié dans les pièces ?). "expected_value" reste null. Une réponse "oui" = élément documenté. FRANCE UNIQUEMENT.
            - Critère F-IM-22 Dublin recours (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM22_DATE_NOTIFICATION_DUBLIN (la date de notification de la décision de transfert Dublin est-elle identifiée dans les pièces — point de départ du délai de recours 7 jours avec effet suspensif ?). "expected_value" reste null. Une réponse "oui" = date documentée. FRANCE UNIQUEMENT.
            - Critère F-IM-23 CRRV recours refus de visa (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM23_DATE_NOTIFICATION_REFUS (la date de notification du refus de visa est-elle identifiée dans les pièces — point de départ du délai de recours 2 mois devant la CRRV ?). "expected_value" reste null. Une réponse "oui" = date documentée. FRANCE UNIQUEMENT.
            - Critères F-IM-14 Procédure 9bis humanitaire (droit de l'immigration, BELGIQUE UNIQUEMENT, binaires) : B9BIS_DATE_ENTREE_BELGIQUE (la date d'entrée en Belgique est-elle identifiée dans les pièces — condition d'ancienneté pour la procédure 9bis ?), B9BIS_DUREE_PRESENCE (la durée de présence continue en Belgique est-elle identifiée dans les pièces — condition fondamentale de la procédure 9bis ?), B9BIS_CIRCONSTANCES_EXCEPTIONNELLES (des circonstances exceptionnelles justifiant l'impossibilité de rentrer au pays d'origine sont-elles identifiées dans les pièces ?), B9BIS_LIENS_FAMILIAUX_BE (des liens familiaux en Belgique sont-ils identifiés dans les pièces — élément de rattachement à l'intégration ?), B9BIS_LIENS_PROFESSIONNELS (des liens professionnels en Belgique sont-ils identifiés dans les pièces — élément d'intégration économique ?), B9BIS_SCOLARITE_ENFANTS_BE (la scolarisation des enfants en Belgique est-elle identifiée dans les pièces — élément d'intégration familiale ?), B9BIS_MENACE_ORDRE_PUBLIC (l'absence de menace pour l'ordre public est-elle documentée dans les pièces — condition négative de la procédure 9bis ?), B9BIS_DATE_DEPOT_DEMANDE (la date de dépôt de la demande 9bis est-elle identifiée dans les pièces ?). "expected_value" reste null. Une réponse "oui" = élément documenté. BELGIQUE UNIQUEMENT.
            - Critère F-IM-14 40bis cohabitant UE (droit de l'immigration, BELGIQUE UNIQUEMENT, binaire) : IM14_LIEN_FAMILIAL (le lien familial avec le citoyen UE est-il identifié dans les pièces — condition d'accès à la procédure 40bis : conjoint, enfant, ascendant, etc. ?). "expected_value" reste null. Une réponse "oui" = lien documenté. BELGIQUE UNIQUEMENT.
            - Critères F-FA-18 Reconnaissance de paternité (droit de la famille, FRANCE UNIQUEMENT, binaires) : CONSENTEMENT_LIBRE (le consentement de la personne reconnaissant l'enfant est-il libre et éclairé selon les pièces — absence de vice du consentement, art. 316 C.civ. ?), PATERNITE_VRAISEMBLABLE (la paternité est-elle vraisemblable selon les éléments des pièces — condition de recevabilité de la reconnaissance ?). "expected_value" reste null. Une réponse "oui" = élément documenté. FRANCE UNIQUEMENT.
            - Critères F-FA-18 Contestation de paternité (droit de la famille, FRANCE UNIQUEMENT, binaires) : CONTESTATION_PATERNITE_MOTIFS_SERIEUX (des motifs sérieux de contestation de la paternité sont-ils identifiés dans les pièces — condition de recevabilité, art. 332 C.civ. ?), CONTESTATION_PATERNITE_EXPERTISE_ADN (une expertise ADN est-elle demandée ou ordonnée selon les pièces — moyen probatoire principal ?), CONTESTATION_PATERNITE_POSSESSION_ETAT (la possession d'état conforme au titre est-elle identifiée dans les pièces — si présente, fait obstacle à la contestation pendant 5 ans, art. 333 C.civ. ?). "expected_value" reste null. Une réponse "oui" = élément documenté. FRANCE UNIQUEMENT.
            - Critères F-FA-18 Recherche de paternité (droit de la famille, FRANCE UNIQUEMENT, binaires) : RECHERCHE_PATERNITE_POSSESSION_ETAT (une possession d'état du père envers l'enfant est-elle identifiée dans les pièces — mode de preuve privilégié, art. 311-1 C.civ. ?), RECHERCHE_PATERNITE_EXPERTISE_ADN (une expertise ADN est-elle demandée ou ordonnée selon les pièces ?), RECHERCHE_PATERNITE_REFUS_ADN (un refus de se soumettre à l'expertise ADN est-il identifié dans les pièces — crée une présomption défavorable ?), RECHERCHE_PATERNITE_MOTIFS_SERIEUX (des motifs sérieux de croire à la paternité alléguée sont-ils identifiés dans les pièces — condition de recevabilité ?). "expected_value" reste null. Une réponse "oui" = élément documenté. FRANCE UNIQUEMENT.
            - Critères F-FA-18 Possession d'état (droit de la famille, FRANCE UNIQUEMENT, binaires) : POSSESSION_ETAT_TRACTATUS (le tractatus est-il identifié dans les pièces — traitement public comme enfant par le père, art. 311-1 C.civ. ?), POSSESSION_ETAT_FAMA (la fama est-elle identifiée dans les pièces — reconnaissance publique de la relation père-enfant ?), POSSESSION_ETAT_CONTINUE (la possession d'état est-elle continue selon les pièces — condition de durée dans le temps ?), POSSESSION_ETAT_PAISIBLE (la possession d'état est-elle paisible selon les pièces — absence de contestation sérieuse ?), POSSESSION_ETAT_NON_EQUIVOQUE (la possession d'état est-elle non équivoque selon les pièces — absence d'ambiguïté sur l'identité de la filiation ?). "expected_value" reste null. Une réponse "oui" = élément documenté. FRANCE UNIQUEMENT.
            - Critères F-FA-18 Adoption (droit de la famille, FRANCE UNIQUEMENT, binaires) : ADOPTION_FORME (la forme de l'adoption — plénière ou simple — est-elle identifiée dans les pièces ?), ADOPTION_PUPILLE_ETAT (le statut de pupille de l'État ou d'enfant déclaré abandonné est-il identifié dans les pièces — condition d'accès à l'adoption plénière, art. 347 C.civ. ?), ADOPTION_ADOPTANT_MARIE (le statut marital de l'adoptant est-il identifié dans les pièces — l'adoption plénière nécessite un couple marié ou un adoptant seul ?), ADOPTION_AGE_ADOPTANT (l'âge de l'adoptant est-il identifié dans les pièces — condition légale minimale 28 ans pour l'adoption plénière, art. 343 C.civ. ?), ADOPTION_AGE_ADOPTE (l'âge de l'adopté est-il identifié dans les pièces — l'adoption plénière requiert un adopté de moins de 15 ans sauf exceptions, art. 345 C.civ. ?). "expected_value" reste null. Une réponse "oui" = élément documenté. FRANCE UNIQUEMENT.
            - Critère F-FA-24 Réserve héréditaire (droit de la famille, FRANCE UNIQUEMENT, binaire) : RESERVE_CONJOINT_SURVIVANT (le statut du conjoint survivant est-il identifié dans les pièces — détermine s'il bénéficie de la réserve héréditaire et dans quelle mesure, art. 914-1 C.civ. ?). "expected_value" reste null. Une réponse "oui" = statut documenté. FRANCE UNIQUEMENT.
            - Critères F-FA-24 Rapport à succession (droit de la famille, FRANCE UNIQUEMENT, binaires) : RAPPORT_QUALITE_HERITIER (la qualité d'héritier tenu au rapport est-elle identifiée dans les pièces — seuls les héritiers en ligne directe et ceux ayant accepté sont tenus au rapport, art. 843 C.civ. ?), RAPPORT_DONATION_NOMINALE (une donation nominale sujette à rapport est-elle identifiée dans les pièces — donation faite à un héritier sans dispense de rapport ?), RAPPORT_VALEUR_PARTAGE (la valeur du bien au moment du partage est-elle identifiée dans les pièces — base de calcul du rapport, art. 860 C.civ. ?), RAPPORT_DATE_DONATION (la date de la donation sujette à rapport est-elle identifiée dans les pièces — détermine si la donation est prescrite ?). "expected_value" reste null. Une réponse "oui" = élément documenté. FRANCE UNIQUEMENT.
            CONVENTION IMPÉRATIVE : toute question avec "critere_code" doit être formulée pour qu'une réponse "oui" porte un signal positif (critère respecté ou type confirmé).
            Rétrocompat acceptée : format string legacy. Génère entre 3 et 8 questions.
            """;

    static String buildSystemPrompt(String legalDomain, String country) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(LegalDomainPromptBuilder.domainLabel(legalDomain, country));
    }

    record PreparedQuestionGeneration(String prompt, String systemPrompt, UUID caseFileId, UUID caseAnalysisId) {}

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;

    @Lazy @Autowired
    private AiQuestionService self;

    public AiQuestionService(CaseAnalysisRepository caseAnalysisRepository,
                             CaseFileRepository caseFileRepository,
                             AiQuestionRepository aiQuestionRepository,
                             AnalysisJobRepository analysisJobRepository,
                             AnthropicService anthropicService,
                             UsageEventService usageEventService,
                             ApplicationEventPublisher eventPublisher) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_QUESTION_GENERATION_QUEUE, concurrency = "3")
    public void consumeQuestionGeneration(AiQuestionGenerationMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedQuestionGeneration prepared = self.prepareQuestionGeneration(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Question generation START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            // 8192 tokens = max output Sonnet 4.6. Largement suffisant pour 5-8 questions
            // avec justifications sur n'importe quel dossier. Anthropic facture sur
            // l'usage réel (~1000-2000 tokens typiquement), pas le max → zéro surcoût.
            // Aligne avec CaseAnalysisService et EnrichedAnalysisService. Historique :
            // 1024 → 4096 (2026-04-19 PR #386) → 8192 (cette PR, demande user pour
            // éliminer définitivement le risque de troncature silencieuse).
            // F-142-04 : prompt caching ephemeral (5 min TTL) — gain de latence sur
            // les appels successifs avec le même system prompt dans la même session.
            result = anthropicService.analyzeWithSystemCache(prepared.systemPrompt(), prepared.prompt(), 8192);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Question generation DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Question generation FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeQuestionGeneration(prepared.caseFileId(), prepared.caseAnalysisId(), result, failure);
    }

    @Transactional
    public PreparedQuestionGeneration prepareQuestionGeneration(AiQuestionGenerationMessage message) {
        UUID caseFileId = message.caseFileId();

        CaseAnalysis caseAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);

        if (caseAnalysis == null) {
            log.warn("No DONE case analysis found for caseFile {} — question generation skipped", caseFileId);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — question generation skipped", caseFileId);
            return null;
        }

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.QUESTION_GENERATION);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String systemPrompt = buildSystemPrompt(
                ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL",
                ws != null ? ws.getCountry() : "FRANCE");
        return new PreparedQuestionGeneration(caseAnalysis.getAnalysisResult(), systemPrompt, caseFileId, caseAnalysis.getId());
    }

    @Transactional
    public void finalizeQuestionGeneration(UUID caseFileId, UUID caseAnalysisId, AnthropicResult result, Exception failure) {
        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.QUESTION_GENERATION);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });

        if (failure != null) {
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Question generation failed");
            analysisJobRepository.save(job);
            // F-185 SF-185-02 — émet aussi l'event SSE FAILED pour que le front masque le spinner.
            publishStatusEventAfterCommit(caseFileId, AnalysisStatus.FAILED);
            return;
        }

        try {
            List<ParsedQuestion> questions = parseQuestions(result.content());
            CaseFile caseFile = caseFileRepository.findById(caseFileId).orElseThrow();
            CaseAnalysis caseAnalysis = caseAnalysisRepository.findById(caseAnalysisId).orElseThrow();

            for (int i = 0; i < questions.size(); i++) {
                ParsedQuestion pq = questions.get(i);
                AiQuestion question = new AiQuestion();
                question.setCaseFile(caseFile);
                question.setCaseAnalysis(caseAnalysis);
                question.setQuestionText(pq.text());
                question.setCritereCode(pq.critereCode());
                question.setExpectedValue(pq.expectedValue());
                question.setOrderIndex(i);
                aiQuestionRepository.save(question);
            }

            job.setProcessedItems(1);
            job.setStatus(AnalysisStatus.DONE);
            log.info("Question generation finalized for caseFile {} — {} questions", caseFileId, questions.size());
        } catch (Exception e) {
            log.error("Question generation finalization FAILED for caseFile {}", caseFileId, e);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Question generation failed");
        }

        analysisJobRepository.save(job);

        if (job.getStatus() == AnalysisStatus.DONE) {
            final AnthropicResult finalResult = result;
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.QUESTION_GENERATION,
                        finalResult.promptTokens(), finalResult.completionTokens()));
        }

        // F-185 SF-185-02 — émission SSE QUESTION_GENERATION_DONE / _FAILED après commit
        // pour que le frontend bascule du spinner "Génération des questions complémentaires…"
        // vers la bannière "N question(s) en attente" sans attendre le polling jobs (5-15 s économisés).
        publishStatusEventAfterCommit(caseFileId, job.getStatus());
    }

    /**
     * F-185 SF-185-02 — helper pour publier l'événement SSE QUESTION_GENERATION
     * après commit Spring. Pas de noop si une transaction n'est pas active : le code
     * s'exécute hors transaction (test unitaire qui ne wrap pas explicitement) → on
     * publie directement.
     */
    private void publishStatusEventAfterCommit(UUID caseFileId, AnalysisStatus status) {
        AnalysisStatusEvent event = new AnalysisStatusEvent(caseFileId, status, JobType.QUESTION_GENERATION);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(event);
                }
            });
        } else {
            eventPublisher.publishEvent(event);
        }
    }

    record ParsedQuestion(String text, String critereCode, String expectedValue) {}

    static List<ParsedQuestion> parseQuestions(String json) {
        try {
            JsonNode root = MAPPER.readTree(CaseAnalysisResponse.stripMarkdownCodeBlock(json));
            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray()) return List.of();
            List<ParsedQuestion> result = new ArrayList<>();
            for (JsonNode item : questionsNode) {
                if (item.isTextual()) {
                    String txt = item.asText();
                    if (txt != null && !txt.isBlank()) result.add(new ParsedQuestion(txt, null, null));
                } else if (item.isObject()) {
                    JsonNode texteNode = item.get("texte");
                    if (texteNode == null || !texteNode.isTextual()) continue;
                    String texte = texteNode.asText();
                    if (texte.isBlank()) continue;
                    String code = null;
                    JsonNode codeNode = item.get("critere_code");
                    if (codeNode != null && codeNode.isTextual()) {
                        String raw = codeNode.asText().trim();
                        if (!raw.isEmpty()) code = raw.toUpperCase();
                    }
                    String expectedValue = null;
                    JsonNode evNode = item.get("expected_value");
                    if (evNode != null && evNode.isTextual()) {
                        String raw = evNode.asText().trim();
                        if (!raw.isEmpty()) expectedValue = raw.toUpperCase();
                    }
                    result.add(new ParsedQuestion(texte, code, expectedValue));
                }
            }
            return List.copyOf(result);
        } catch (Exception e) {
            return List.of();
        }
    }
}

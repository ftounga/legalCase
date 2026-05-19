package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseDeadlineService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.ExtractionStatus;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile({"local", "prod"})
public class CaseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisService.class);

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois les analyses de plusieurs documents d'un dossier juridique.
            Produis une synthèse globale du dossier en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "points_juridiques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "risques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "questions_ouvertes": [...], "pieces_manquantes": [...], "points_procedure": [...], "pistes_strategiques": [...], "score_risque": {"niveau": "FAIBLE"|"MOYEN"|"ELEVE", "valeur": <0-100>}, "delais_detectes": [{"label": "...", "date_detectee": "YYYY-MM-DD", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>"}]}
            Pour les champs "faits", "points_juridiques" et "risques", chaque élément est un objet avec "texte" (le contenu), "source" (nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus) et "extrait" (phrase exacte tirée du document). Si la source n'est pas identifiable, utilise "source": null et "extrait": null.
            F-146 : ajoute AUSSI à chaque item un champ "sourceRef" précisant la pièce juridique exacte, au format {"documentName": "<nom fichier>", "pieceType": "<type pièce parmi la liste du contexte>", "pieceLabel": "<label de la pièce tel qu'indiqué dans la section PIÈCES IDENTIFIÉES>", "pageStart": <page début>, "pageEnd": <page fin>}. Utilise les informations de la section "=== PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===" fournie dans le prompt utilisateur. Si la pièce n'est pas identifiable ou si le dossier n'a pas de pièces détectées (dossier pré-F-145), utilise "sourceRef": null. Ne jamais inventer un label de pièce qui n'apparaît pas dans la section PIÈCES IDENTIFIÉES.
            La timeline doit lister les événements clés du dossier par ordre chronologique. Si aucune date n'est identifiable, utilise "timeline": [].
            Le champ "pieces_manquantes" liste les pièces habituellement attendues dans ce type de dossier qui sont absentes des documents fournis. Chaque élément est un objet {"texte": "<description de la pièce>", "critere_code": "<code ou null>"}. "critere_code" est rempli UNIQUEMENT si l'absence de cette pièce correspond à un des codes surveillés :
            - Critères F-DT-08 Validité licenciement (droit du travail) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE.
            - Pièces F-FA-07 Checklist divorce (droit de la famille) : FR_ACTE_MARIAGE, FR_ACTE_NAISSANCE_EPOUX, FR_ACTE_NAISSANCE_ENFANTS, FR_LIVRET_FAMILLE, FR_JUSTIF_DOMICILE, FR_CONTRAT_MARIAGE, FR_ETAT_PATRIMOINE, FR_JUSTIF_REVENUS, FR_PIECE_IDENTITE, BE_ACTE_MARIAGE, BE_ACTE_NAISSANCE_EPOUX, BE_ACTE_NAISSANCE_ENFANTS, BE_COMPOSITION_MENAGE, BE_CONTRAT_MARIAGE, BE_CONVENTION_PREALABLE, BE_JUSTIF_REVENUS, BE_PIECE_IDENTITE.
            Sinon null. Exemple : {"texte": "Copie intégrale de l'acte de mariage", "critere_code": "FR_ACTE_MARIAGE"}. Rétrocompat : format string legacy accepté. Si le dossier semble complet, utilise "pieces_manquantes": [].
            Le champ "points_procedure" liste les étapes procédurales légalement requises dans ce type de dossier. Chaque élément est un objet {"texte": "<description de l'étape>", "critere_code": "<code ou null>", "expected_value": "<valeur ou null>"}. "critere_code" est rempli uniquement si le point porte sur l'un des critères surveillés :
            - Critères F-DT-08 Validité licenciement (droit du travail, binaires) : FR_CONVOCATION, FR_ENTRETIEN, FR_DELAI_NOTIFICATION, FR_MOTIVATION, FR_MOTIF_REEL, FR_PROCEDURE_DISCIPLINAIRE, FR_ORDRE_LICENCIEMENT, BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE. Pour ces critères, "expected_value" doit rester null (statut VERIFIED/NON_COMPLIANT porte le signal).
            - Étapes F-FA-07 Checklist divorce (droit de la famille, binaires) : FR_CHOIX_AVOCATS, FR_REDACTION_CONVENTION, FR_ENVOI_LRAR, FR_DELAI_REFLEXION, FR_SIGNATURE_CONVENTION, FR_DEPOT_NOTAIRE, FR_ENREGISTREMENT, BE_CHOIX_AVOCAT, BE_REDACTION_CONVENTION, BE_REQUETE_CONJOINTE, BE_COMPARUTION, BE_JUGEMENT, BE_TRANSCRIPTION. Pour ces étapes, "expected_value" doit rester null. Sémantique : VERIFIED = étape accomplie, NON_COMPLIANT = étape non faite.
            - Critères F-IM-21 Validité dossier immigration (droit de l'immigration, binaires) : IM21_REGULARITE_SEJOUR_FR, IM21_DELAI_DEPOT_FR, IM21_PIECE_IDENTITE_FR, IM21_JUSTIF_DOMICILE_FR, IM21_ETAT_CIVIL_FR, IM21_PHOTO_FR, IM21_TIMBRE_FISCAL_FR, IM21_PIECES_MARIAGE_FR, IM21_COMMUNAUTE_VIE_FR, IM21_RESSOURCES_FR, IM21_CONVENTION_ACCUEIL_FR, IM21_REGULARITE_SEJOUR_BE, IM21_PIECE_IDENTITE_BE, IM21_PIECES_COHABITATION_BE, IM21_RESSOURCES_BE, IM21_LOGEMENT_BE, IM21_ASSURANCE_BE, IM21_EXTRAIT_CASIER_BE. Pour ces critères, "expected_value" doit rester null (statut VERIFIED/NON_COMPLIANT porte le signal).
            - Critère F-FA-06 Calendrier garde (droit de la famille, énuméré) : FA06_MODE_GARDE. Renseigne obligatoirement "expected_value" avec la valeur affirmée par le point, parmi : ALTERNEE_FR, DVH_CLASSIQUE_FR, DVH_ELARGI_FR (France), ALTERNEE_BE, SECONDAIRE_BE, SECONDAIRE_ELARGI_BE (Belgique). Exemple : {"texte": "Résidence alternée une semaine sur deux actée dans la convention", "critere_code": "FA06_MODE_GARDE", "expected_value": "ALTERNEE_FR"}.
            - Critère F-IM-05 Titre de séjour (droit de l'immigration, énuméré) : IM05_MOTIF. Renseigne obligatoirement "expected_value" avec le motif de la demande, parmi : TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE. Exemple : {"texte": "La demande est fondée sur un regroupement familial avec conjoint français", "critere_code": "IM05_MOTIF", "expected_value": "FAMILLE"}.
            - Critère F-IM-06 Recours (droit de l'immigration, énuméré) : IM06_RECOURS_TYPE. Renseigne obligatoirement "expected_value" avec le type de recours à former, parmi : RECOURS_GRACIEUX_PREFET, RECOURS_CONTENTIEUX_TA, RECOURS_CNDA (France), RECOURS_CGRA, RECOURS_CCE, RECOURS_CE_BELGIQUE (Belgique). Exemple : {"texte": "Le refus OFPRA doit être contesté devant la CNDA dans un délai de 30 jours", "critere_code": "IM06_RECOURS_TYPE", "expected_value": "RECOURS_CNDA"}.
            - Critère F-IM-07 Droit au travail (droit de l'immigration, énuméré) : IM07_TITRE_TYPE. Renseigne obligatoirement "expected_value" avec le code du titre de séjour parmi les 16 codes (identiques à F-IM-05) : VLS_TS_ETUDIANT, VLS_TS_SALARIE, CST_SALARIE, CARTE_PLURIANNUELLE, CARTE_RESIDENT, APS, CST_VPF, RECEPISSE_ASILE (France), CARTE_A_TRAVAIL, CARTE_A_ETUDES, CARTE_A_FAMILLE, CARTE_B, CARTE_C, PERMIS_UNIQUE, ANNEXE_15, ATTESTATION_IMMATRICULATION (Belgique). Ne renseigner ce critère que si le point évoque spécifiquement le droit au travail attaché à un titre.
            - Critère F-DT-09 Type de rupture (énuméré) : DT09_TYPE_RUPTURE. Pour ce critère, renseigne obligatoirement "expected_value" avec la valeur affirmée par le point, parmi : LICENCIEMENT, LICENCIEMENT_ECONOMIQUE, RUPTURE_CONVENTIONNELLE (France), LICENCIEMENT_ORDINAIRE, RUPTURE_AMIABLE (Belgique). Exemple : {"texte": "Convention de rupture conventionnelle homologuée présente au dossier", "critere_code": "DT09_TYPE_RUPTURE", "expected_value": "RUPTURE_CONVENTIONNELLE"}.
            - Critères F-DT-36 Nullité de procédure de licenciement (droit du travail, FRANCE UNIQUEMENT, binaires) : DT36_DATE_ENTRETIEN (date de l'entretien préalable identifiée dans les pièces), DT36_MOTIVATION (lettre de licenciement énonçant un motif précis et matériellement vérifiable), DT36_ENTRETIEN_TENU (entretien préalable effectivement tenu selon les pièces). Pour ces critères, "expected_value" doit rester null (statut VERIFIED/NON_COMPLIANT porte le signal). VERIFIED = critère respecté/documenté, NON_COMPLIANT = absent ou non respecté.
            - Critère F-DT-11 Harcèlement / licenciement nul (droit du travail, FRANCE UNIQUEMENT, binaire) : HLN_MOTIF_NULLITE (motif de nullité du licenciement identifié dans les pièces : harcèlement moral/sexuel, discrimination, protection représentant du personnel, maternité/paternité, accident du travail). "expected_value" reste null. VERIFIED = motif de nullité documenté dans les pièces.
            - Critères F-DT-13 Licenciement économique (droit du travail, FRANCE UNIQUEMENT, binaires) : DT13_MOTIF_ECONOMIQUE (motif économique justificatif documenté dans les pièces : difficultés économiques, mutation technologique, sauvegarde de compétitivité, cessation d'activité), DT13_DATE_NOTIFICATION (date de notification du licenciement économique identifiée dans les pièces). Pour ces critères, "expected_value" reste null. VERIFIED = présence documentée.
            - Critère F-DT-14 Plan de Sauvegarde de l'Emploi (droit du travail, FRANCE UNIQUEMENT, binaire) : PSE_DATE_PROJET (date de présentation du projet de PSE identifiée dans les pièces). "expected_value" reste null. VERIFIED = date documentée.
            - Critère F-DT-30 Protection représentant du personnel (droit du travail, FRANCE UNIQUEMENT, binaire) : PROTECTION_RP_MOTIF (motif invoqué à l'appui du licenciement d'un représentant du personnel identifié dans les pièces). "expected_value" reste null. VERIFIED = motif documenté.
            - Critères F-DT-10 Validité rupture conventionnelle (droit du travail, FRANCE UNIQUEMENT, binaires) : RC_CONSENTEMENT (consentement libre et éclairé des deux parties documenté — absence de vice du consentement), RC_DELAI_RETRACTATION (délai de rétractation de 15 jours calendaires respecté), RC_HOMOLOGATION (homologation par la DREETS/DRIESST obtenue ou en cours), RC_ASSISTANCE (droit à l'assistance lors de l'entretien respecté), RC_INDEMNITE (indemnité spécifique de rupture conventionnelle au moins égale au plancher légal : 1/4 de mois par année d'ancienneté), RC_ENTRETIENS (au moins un entretien préalable tenu entre les parties). Pour ces critères, "expected_value" reste null. VERIFIED = critère respecté/documenté.
            - Critère F-DT-22 Requalification CDD en CDI (droit du travail, FRANCE UNIQUEMENT, binaire) : DT22_SALAIRE (salaire brut mensuel de référence identifié dans les pièces — base de calcul de l'indemnité de requalification). "expected_value" reste null. VERIFIED = salaire identifié.
            - Critère F-DT-23 Requalification contrat intérim en CDI (droit du travail, FRANCE UNIQUEMENT, binaire) : DT23_SALAIRE (salaire brut mensuel de référence identifié dans les pièces — base de calcul de l'indemnité de requalification intérim). "expected_value" reste null. VERIFIED = salaire identifié.
            - Critère F-DT-24 Clause de non-concurrence (droit du travail, FRANCE UNIQUEMENT, binaire) : DT24_SALAIRE (salaire brut mensuel de référence identifié dans les pièces — base de calcul de la contrepartie pécuniaire de la clause de non-concurrence). "expected_value" reste null. VERIFIED = salaire identifié.
            - Critères F-DT-31 Transaction (droit du travail, FRANCE UNIQUEMENT, binaires) : DT31_SALAIRE_MENSUEL (salaire mensuel brut de référence identifié dans les pièces — paramètre de calcul de la transaction), DT31_ANCIENNETE (ancienneté en mois/années identifiée dans les pièces — paramètre clé de la transaction). Pour ces critères, "expected_value" reste null. VERIFIED = donnée identifiée.
            - Critères F-132 Indemnité rupture conventionnelle (droit du travail, FRANCE UNIQUEMENT, binaires) : RCI_SALAIRE (salaire brut mensuel de référence identifié dans les pièces), RCI_ANCIENNETE (ancienneté identifiée dans les pièces). Pour ces critères, "expected_value" reste null. VERIFIED = donnée identifiée.
            - Critères F-DT-15 Inaptitude (droit du travail, FRANCE UNIQUEMENT, binaires) : INAPT_ORIGINE (origine de l'inaptitude identifiée dans les pièces : professionnelle — AT/MP — ou non-professionnelle — détermine le régime indemnitaire et les obligations de reclassement), INAPT_RECLASSEMENT (obligation de reclassement et/ou démarches de reclassement documentées dans les pièces). Pour ces critères, "expected_value" reste null. VERIFIED = donnée documentée.
            - Critère F-DT-33 Accident du travail / Maladie professionnelle (droit du travail, FRANCE UNIQUEMENT, binaire) : AT_MP_DATE_ACCIDENT (date de l'accident du travail ou de la déclaration de maladie professionnelle identifiée dans les pièces). "expected_value" reste null. VERIFIED = date identifiée.
            - Critères F-DT-29 Crédit-temps (droit du travail, BELGIQUE UNIQUEMENT, binaires) : CREDIT_TEMPS_ANCIENNETE (ancienneté du travailleur identifiée dans les pièces — condition d'éligibilité au crédit-temps belge), CREDIT_TEMPS_AGE (âge du travailleur identifié dans les pièces — déterminant pour le régime crédit-temps senior). Pour ces critères, "expected_value" doit rester null (statut VERIFIED/NON_COMPLIANT porte le signal). VERIFIED = donnée documentée dans les pièces.
            - Critère F-136 Type de procédure de travail (droit du travail, binaire) : TRAVAIL_PROCEDURE_TYPE (type de procédure de travail identifié dans les pièces : licenciement collectif, restructuration, fermeture d'entreprise). "expected_value" doit rester null. VERIFIED = type documenté.
            - Critères F-IM-08 OQTF (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM08_MOTIF_OQTF (motif de l'obligation de quitter le territoire français identifié dans les pièces : absence de titre de séjour, menace à l'ordre public, refus de séjour), IM08_RECOURS_FORME (forme du recours contre l'OQTF identifiée dans les pièces : recours gracieux devant le préfet, recours contentieux devant le tribunal administratif). Pour ces critères, "expected_value" doit rester null. VERIFIED = élément documenté dans les pièces.
            - Critère F-IM-08 Référés administratifs (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM08RA_DECISION_CONTESTEE (la décision administrative contestée est identifiée dans les pièces : OQTF, refus de titre, arrêté préfectoral). "expected_value" doit rester null. VERIFIED = décision documentée.
            - Critères F-IM-09 AES Métiers en tension (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09_DATE_ENTREE_FRANCE (date d'entrée en France identifiée dans les pièces — condition d'éligibilité à l'admission exceptionnelle au séjour), IM09_MOIS_ACTIVITE (nombre de mois d'activité professionnelle identifié dans les pièces — condition AES métiers en tension : 8 mois sur 24 mois). Pour ces critères, "expected_value" doit rester null. VERIFIED = donnée documentée.
            - Critères F-IM-09 AES Étudiant (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09_ETU_DATE_ENTREE_FRANCE (date d'entrée en France de l'étudiant identifiée dans les pièces), IM09_ETU_DUREE_PRESENCE (durée de présence continue en France identifiée dans les pièces — condition d'éligibilité AES étudiant), IM09_ETU_DATE_DEPOT_DEMANDE (date de dépôt de la demande AES étudiant identifiée dans les pièces). Pour ces critères, "expected_value" doit rester null. VERIFIED = donnée documentée.
            - Critères F-IM-09 AES Humanitaire (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM09H_DATE_ENTREE_FRANCE (date d'entrée en France identifiée dans les pièces — condition AES humanitaire), IM09H_MOTIF_HUMANITAIRE (motif humanitaire identifié dans les pièces : circonstances humanitaires exceptionnelles, liens personnels et familiaux en France). Pour ces critères, "expected_value" doit rester null. VERIFIED = élément documenté.
            - Critère F-IM-11 Changement de statut (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM11_TITRE_ACTUEL (titre de séjour actuel du demandeur identifié dans les pièces — condition préalable à l'analyse du changement de statut). "expected_value" doit rester null. VERIFIED = titre documenté.
            - Critère F-IM-12 Asile avancé (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM12_DISPOSITIF_ASILE (dispositif de protection internationale visé identifié dans les pièces : statut de réfugié, protection subsidiaire, apatride). "expected_value" doit rester null. VERIFIED = dispositif documenté.
            - Critère F-IM-13 Naturalisation (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM13_VOIE_NATURALISATION (voie de naturalisation identifiée dans les pièces : par décret, par mariage avec un ressortissant français, par renonciation à une nationalité étrangère). "expected_value" doit rester null. VERIFIED = voie documentée.
            - Critères F-IM-19 Mineurs (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM19_DATE_NAISSANCE (date de naissance du mineur identifiée dans les pièces — condition déterminante pour les droits au séjour), IM19_DATE_ENTREE (date d'entrée en France du mineur identifiée dans les pièces). Pour ces critères, "expected_value" doit rester null. VERIFIED = date documentée.
            - Critères F-IM-20 Mesures d'éloignement (droit de l'immigration, FRANCE UNIQUEMENT, binaires) : IM20_DISPOSITIF_ELOIGNEMENT (dispositif d'éloignement identifié dans les pièces : OQTF, interdiction du territoire français, reconduite à la frontière, expulsion), IM20_MOTIF_MENACE (motif de menace à l'ordre public ou à la sécurité publique identifié dans les pièces — fondement de la mesure d'éloignement). Pour ces critères, "expected_value" doit rester null. VERIFIED = élément documenté.
            - Critère F-IM-24 Victime de violences (droit de l'immigration, FRANCE UNIQUEMENT, binaire) : IM24_DATE_ORDONNANCE_PROTECTION (date de l'ordonnance de protection identifiée dans les pièces — ouvre droit au titre de séjour pour victime de violences conjugales selon l'article L.425-6 CESEDA). "expected_value" doit rester null. VERIFIED = date documentée.
            - Critère F-IM-08 Annexe 13 / OQT (droit de l'immigration, BELGIQUE UNIQUEMENT, binaire) : IM08_MOTIF_OQT_BE (motif de l'ordre de quitter le territoire belge identifié dans les pièces : absence de titre de séjour, menace à l'ordre public, refus de séjour). "expected_value" doit rester null. VERIFIED = motif documenté.
            - Critères F-IM-14 Procédure 9ter médicale (droit de l'immigration, BELGIQUE UNIQUEMENT, binaires) : BE_9TER_MALADIE_GRAVE (maladie grave identifiée dans les pièces — condition d'accès à la procédure 9ter séjour pour raisons médicales), BE_9TER_SOINS_BE (disponibilité des soins médicaux en Belgique documentée dans les pièces), BE_9TER_SOINS_INACCESSIBLES (inaccessibilité des soins dans le pays d'origine documentée dans les pièces — critère central de la procédure 9ter), BE_9TER_MENACE_ORDRE_PUBLIC (absence de menace pour l'ordre public documentée — condition négative de la procédure 9ter). Pour ces critères, "expected_value" doit rester null. VERIFIED = élément documenté.
            - Critère F-IM-17 Régime algérien (droit de l'immigration, BELGIQUE UNIQUEMENT, binaire) : IM17_VOIE_REGIME_ALGERIEN (voie de l'accord franco-algérien applicable identifiée dans les pièces : carte de résident algérien, certificat de résidence 1 an, etc.). "expected_value" doit rester null. VERIFIED = voie documentée.
            Pour tout point sans lien avec ces critères, "critere_code" et "expected_value" restent null. Rétrocompat : format string legacy accepté. Si la procédure semble conforme, utilise "points_procedure": [].
            SF-96-06 — Durcissement : quand "critere_code" est null, "points_procedure" ne doit contenir QUE des vérifications binaires factuelles d'étapes légalement requises sur le dossier en cours (les 3 statuts ✅Vérifié / ❌Non conforme / ⚠️À vérifier doivent tous avoir du sens sur l'item). SONT INTERDITS dans "points_procedure" et doivent être redirigés ailleurs : (a) options stratégiques ("En cas de demande…", "Si l'avocat envisage…", "Possibilité de demander…", "Alternative…") → mettre dans "pistes_strategiques" (cf. ci-dessous) ; (b) opportunités futures à plus de 6 mois ("Après N ans de mariage…", "À partir de…", "Une fois N années révolues…") → "pistes_strategiques" si c'est une option à étudier, "risques" si elles imposent un délai à respecter ; (c) recommandations d'action ("Demande à déposer auprès de…", "Joindre la convention…", "Prendre attache avec…") → "pistes_strategiques" si c'est une décision stratégique, "questions_ouvertes" si ça suppose une réponse de l'avocat. Règle de répartition : on VÉRIFIE dans points_procedure, on PROPOSE dans pistes_strategiques, on ALERTE dans risques, on QUESTIONNE dans questions_ouvertes.
            F-176 — Le champ "pistes_strategiques" liste les options stratégiques, opportunités futures et recommandations d'action que l'avocat peut envisager pour ce dossier (c'est-à-dire ce que la règle SF-96-06 ci-dessus exclut de "points_procedure"). Chaque élément est un objet {"texte": "<description de la piste>", "base_juridique": "<articles, lois, jurisprudence référencés>" ou null, "horizon_temporel": "<court terme | moyen terme | long terme + délai approximatif>" ou null, "conditions": ["<condition 1>", "<condition 2>"] (array de strings, [] si aucune), "source": "<source factuelle dans le dossier>" ou null}. Exemples : {"texte": "Demande de Passeport talent — Chercheur (art. L.421-14 CESEDA)", "base_juridique": "Art. L.421-14 CESEDA", "horizon_temporel": "Court terme (3-6 mois)", "conditions": ["Convention d'accueil signée par INRIA/CNRS/LISN", "Doctorat soutenu ou en cours"], "source": null} ou {"texte": "Carte de résident envisageable après 3 ans de mariage", "base_juridique": "Art. L.423-6 CESEDA", "horizon_temporel": "Long terme (3 ans)", "conditions": ["Communauté de vie maintenue", "Intégration républicaine"], "source": null}. Si aucune piste stratégique pertinente, utilise "pistes_strategiques": [].
            Le champ "score_risque" est obligatoire : évalue le niveau de risque global du dossier. "niveau" est l'un de "FAIBLE", "MOYEN" ou "ELEVE". "valeur" est un entier entre 0 et 100 reflétant l'intensité du risque (0 = aucun risque, 100 = risque maximum).
            Le champ "delais_detectes" liste les délais légaux détectés dans les documents (ex: délai de recours, délai de prescription). Format : [{"label": "Délai de recours prud'homal", "date_detectee": "YYYY-MM-DD", "source": "<nom exact du fichier>"}]. Si aucun délai détectable, utilise "delais_detectes": [].

            Le champ "source_explanations" liste UNE explication par donnée factuelle clé, pour alimenter le popover d'incohérence (F-IA-03). Chaque explication SÉPARE STRICTEMENT 3 zones affichables :
            - sentence : règle juridique pure (≤ 220 car), SANS mention du nom du document/question/F96/pièce. Exemple CORRECT : "La convention BTP prévoit une prime de 12 %% après 15 ans." Exemple INCORRECT : "Selon contrat_dupont.pdf, la prime est de 12 %%."
            - label : nom CANONIQUE court de la source (nom de fichier exact pour DOCUMENT ; question complète pour QUESTION_AI ; description courte pour CHECKLIST_F96 ; intitulé court pour MISSING_PIECE).
            - secondaryText : citation ou détail verbatim (≤ 200 car). RÉUTILISE verbatim les "extrait" que tu as déjà produits ci-dessus dans faits/points_juridiques/risques. Exemples : "Clause 6.2 — 'Prime d'ancienneté : 12%% après 15 ans'" | "Réponse de l'avocat : '15 ans et 2 mois'" | "Marqué non conforme — 'Aucune LRAR dans les 5 jours'".
            Format : [{"sourceKey": "<snake_case|UPPER_F96_CODE>", "sourceType": "DOCUMENT"|"QUESTION_AI"|"CHECKLIST_F96"|"MISSING_PIECE"|"ANALYSIS_DETECTION", "label": "…", "sentence": "…", "secondaryText": "…", "anchorDocName": "<nom exact doc ou null>"}].
            sourceKeys génériques attendus si la donnée est dans la synthèse : convention_collective, date_entree, salaire_brut_mensuel, conges_contractuels, prime_anciennete_contractuelle, type_rupture, date_licenciement, duree_mariage, revenus_conjoints, nationalite_ue, type_titre_sejour, type_recours, date_notification_decision_contestee. Codes F96 additionnels possibles : FR_CONVOCATION, FR_MOTIVATION, BE_AUDITION, RC_CONSENTEMENT, RC_DELAI_RETRACTATION, DT09_TYPE_RUPTURE, FA05_VALEUR_VENALE, FA06_MODE_GARDE, IM05_MOTIF, IM06_RECOURS_TYPE, IM07_TITRE_TYPE, IM21_REGULARITE_SEJOUR_FR, IM21_DELAI_DEPOT_FR, IM21_PIECE_IDENTITE_FR, IM21_JUSTIF_DOMICILE_FR, IM21_ETAT_CIVIL_FR, IM21_PHOTO_FR, IM21_TIMBRE_FISCAL_FR, IM21_PIECES_MARIAGE_FR, IM21_COMMUNAUTE_VIE_FR, IM21_RESSOURCES_FR, IM21_CONVENTION_ACCUEIL_FR, IM21_REGULARITE_SEJOUR_BE, IM21_PIECE_IDENTITE_BE, IM21_PIECES_COHABITATION_BE, IM21_RESSOURCES_BE, IM21_LOGEMENT_BE, IM21_ASSURANCE_BE, IM21_EXTRAIT_CASIER_BE, etc. Produis uniquement les sourcekeys dont la donnée est concrète dans la synthèse ; omet les autres. Aucune invention : un label DOCUMENT doit correspondre à un fichier réellement listé dans le prompt utilisateur. Si aucune source unique n'est identifiable, utilise sourceType="ANALYSIS_DETECTION" et label="Synthèse du dossier". Si aucune donnée factuelle pertinente, "source_explanations": [].
            IMPORTANT : si plusieurs sources corroborent la même donnée (ex. un document ET une réponse à une question confirment la même convention), produis PLUSIEURS entries avec le MÊME sourceKey, chacune avec un sourceType et label différents. Cela permet d'afficher les sources côte à côte. Exemple : [{"sourceKey": "convention_collective", "sourceType": "DOCUMENT", "label": "contrat.pdf", ...}, {"sourceKey": "convention_collective", "sourceType": "QUESTION_AI", "label": "Quelle convention ?", ...}].

            Contraintes de longueur : produis jusqu'à %d entrées timeline, %d faits, %d points_juridiques, %d risques, %d questions_ouvertes, %d pièces manquantes, %d points procédure, %d pistes stratégiques. Pas de minimum — produis exactement ce que la richesse du dossier justifie, sans rembourrer pour atteindre les limites.
            """;

    static String buildSystemPrompt(String legalDomain, String country, AnalysisLimitsProperties.LevelLimits limits) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country),
                limits.getTimeline(), limits.getFaits(),
                limits.getPointsJuridiques(), limits.getRisques(), limits.getQuestionsOuvertes(),
                limits.getPiecesManquantes(), limits.getPointsProcedure(), limits.getPistesStrategiques())
                + LegalDomainPromptBuilder.domainSpecificInstruction(legalDomain);
    }

    record PreparedCaseAnalysis(UUID analysisId, String prompt, String systemPrompt, UUID caseFileId,
                                 AnalysisLimitsProperties.LevelLimits limits) {}

    /** SF — budget pour les extraits bruts injectés par doc (pour éviter les
     *  mauvaises classifications quand les doc-analyses ne captent pas le
     *  mécanisme factuel de rupture, ex. Convention de rupture signée vs
     *  requalification en licenciement). 2000 car ≈ ~500 tokens par doc. */
    static final int RAW_DOC_PREFIX_CHARS = 2_000;

    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService;
    private final AnalysisLimitsProperties analysisLimitsProperties;
    private final ProcedureCheckService procedureCheckService;
    private final StrategicOptionService strategicOptionService;
    private final CaseDeadlineService caseDeadlineService;
    private final SourceExplanationGenerator sourceExplanationGenerator;
    private final SourceExplanationService sourceExplanationService;
    private final PiecesPromptContext piecesPromptContext;
    private final JurisprudenceVerificationService jurisprudenceVerificationService;

    @Lazy @Autowired
    private CaseAnalysisService self;

    public CaseAnalysisService(DocumentAnalysisRepository documentAnalysisRepository,
                               DocumentExtractionRepository documentExtractionRepository,
                               CaseAnalysisRepository caseAnalysisRepository,
                               CaseFileRepository caseFileRepository,
                               AnthropicService anthropicService,
                               AnalysisJobRepository analysisJobRepository,
                               RabbitTemplate rabbitTemplate,
                               UsageEventService usageEventService,
                               ApplicationEventPublisher eventPublisher,
                               AnalysisDocumentSnapshotService analysisDocumentSnapshotService,
                               AnalysisLimitsProperties analysisLimitsProperties,
                               ProcedureCheckService procedureCheckService,
                               StrategicOptionService strategicOptionService,
                               CaseDeadlineService caseDeadlineService,
                               SourceExplanationGenerator sourceExplanationGenerator,
                               SourceExplanationService sourceExplanationService,
                               PiecesPromptContext piecesPromptContext,
                               JurisprudenceVerificationService jurisprudenceVerificationService) {
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.documentExtractionRepository = documentExtractionRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
        this.analysisDocumentSnapshotService = analysisDocumentSnapshotService;
        this.analysisLimitsProperties = analysisLimitsProperties;
        this.procedureCheckService = procedureCheckService;
        this.strategicOptionService = strategicOptionService;
        this.caseDeadlineService = caseDeadlineService;
        this.sourceExplanationGenerator = sourceExplanationGenerator;
        this.sourceExplanationService = sourceExplanationService;
        this.piecesPromptContext = piecesPromptContext;
        this.jurisprudenceVerificationService = jurisprudenceVerificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.CASE_ANALYSIS_QUEUE, concurrency = "3")
    public void consumeCaseAnalysis(CaseAnalysisMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedCaseAnalysis prepared = self.prepareCaseAnalysis(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Case analysis START for caseFile {} ({} chars, streaming)",
                    caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();

            // F-185 SF-185-01 — streaming : Sonnet streame les tokens, on alimente
            // un extracteur incrémental qui détecte les sections JSON top-level
            // complètes ; chaque section close → persistance + event SSE PARTIAL
            // pour que la page synthèse rende les sections au fil de l'eau.
            //
            // Fallback gracieux : si le streaming échoue (HTTP, parsing), on tombe
            // sur l'appel synchrone existant — aucune régression possible.
            //
            // F-142-04 : prompt caching ephemeral — le system prompt (plusieurs milliers
            // de tokens : domaine, limites, instruction, PiecesPromptContext) est
            // réutilisé entre appels successifs (re-analyse, question chat). Gain ~85 %
            // de latence prefill sur les appels dans la fenêtre de 5 min.
            // F-161 SF-161-02 : 64000 tokens output (dossiers riches).
            // F-185 SF-185-05 — compteurs diagnostic streaming pour observer en prod
            // si le bug "partial_state toujours NULL" vient de :
            //   - chunks=0           → RestClient ne stream pas (buffer)
            //   - chunks=N sections=0 → extracteur ne détecte aucune section close
            //   - chunks=N sections=M persists=K (K<M)  → persistPartialAndNotify échoue
            //   - chunks=N sections=M persists=M  → tout marche, bug ailleurs (transaction, frontend)
            java.util.concurrent.atomic.AtomicInteger chunkCount = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger sectionCount = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger persistCount = new java.util.concurrent.atomic.AtomicInteger();
            try {
                PartialJsonSectionExtractor extractor = new PartialJsonSectionExtractor();
                result = anthropicService.analyzeWithSystemCacheStreaming(
                        prepared.systemPrompt(), prepared.prompt(), 64000,
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
                                log.warn("Partial state update failed for caseFile {} (analysis continues): {}",
                                        caseFileId, ex.getMessage());
                            }
                        });
            } catch (Exception streamingFailure) {
                log.warn("Streaming Anthropic failed for caseFile {} ({}), falling back to synchronous mode",
                        caseFileId, streamingFailure.getMessage());
            }
            log.info("Case analysis STREAMING SUMMARY caseFile={} chunks={} sections={} persists={}",
                    caseFileId, chunkCount.get(), sectionCount.get(), persistCount.get());
            // F-185 SF-185-01 — fallback gracieux : streaming peut retourner null si la
            // bibliothèque renvoie un payload vide ou si le mock test n'a pas stubé la
            // méthode streaming ; dans tous les cas on retombe sur l'appel synchrone éprouvé.
            if (result == null) {
                result = anthropicService.analyzeWithSystemCache(
                        prepared.systemPrompt(), prepared.prompt(), 64000);
            }

            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Case analysis DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Case analysis FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeCaseAnalysis(prepared.analysisId(), prepared.caseFileId(), result, failure, prepared.limits());
    }

    /**
     * F-185 SF-185-01 — persiste l'état partiel courant ({@link PartialJsonSectionExtractor#snapshot()})
     * et publie un événement SSE {@code CASE_ANALYSIS_PARTIAL} après commit.
     *
     * <p>Transaction REQUIRES_NEW pour commit visible immédiatement (vu par
     * l'endpoint partial), sans attendre la fin du streaming.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void persistPartialAndNotify(UUID analysisId, UUID caseFileId, Map<String, String> sections) {
        // Reconstruit un JSON {"section1": <valueJson>, "section2": <valueJson>, ...}
        // en réinjectant les valeurs JSON brutes (qui sont déjà du JSON valide).
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
        // Bascule status PROCESSING → PARTIAL au 1er delta complet ; permet
        // au endpoint partial de filtrer simplement sur status IN (PROCESSING, PARTIAL).
        if (analysis.getAnalysisStatus() == AnalysisStatus.PROCESSING) {
            analysis.setAnalysisStatus(AnalysisStatus.PARTIAL);
        }
        caseAnalysisRepository.save(analysis);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(
                        caseFileId, AnalysisStatus.PARTIAL, JobType.CASE_ANALYSIS));
            }
        });
    }

    @Transactional
    public PreparedCaseAnalysis prepareCaseAnalysis(CaseAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        List<DocumentAnalysis> documentAnalyses = documentAnalysisRepository
                .findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE);

        if (documentAnalyses.isEmpty()) {
            log.warn("No DONE document analyses found for caseFile {} — case analysis skipped", caseFileId);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — case analysis skipped", caseFileId);
            return null;
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setVersion(nextVersion);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = caseAnalysisRepository.save(analysis);
        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = caseAnalysisRepository.save(analysis);

        analysisDocumentSnapshotService.snapshot(analysis.getId(), caseFile);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String legalDomain = ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL";
        String country     = ws != null ? ws.getCountry()     : "FRANCE";
        AnalysisLimitsProperties.LevelLimits limits = analysisLimitsProperties.forDomain(legalDomain).getDossier();
        String systemPrompt = buildSystemPrompt(legalDomain, country, limits);
        // F-146 SF-146-01 : préfixe le prompt utilisateur avec la liste des pièces
        // identifiées (F-145) pour que l'IA puisse produire des `sourceRef` précis.
        String piecesContext = piecesPromptContext.buildContextForCaseFile(caseFileId);
        String userPrompt = (piecesContext == null || piecesContext.isEmpty())
                ? buildAggregatedPrompt(documentAnalyses)
                : piecesContext + "\n" + buildAggregatedPrompt(documentAnalyses);
        return new PreparedCaseAnalysis(analysis.getId(), userPrompt, systemPrompt, caseFileId, limits);
    }

    @Transactional
    public void finalizeCaseAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure,
                                      AnalysisLimitsProperties.LevelLimits limits) {
        CaseAnalysis analysis = caseAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            String truncated = AnalysisJsonTruncator.truncateCaseAnalysis(result.content(), limits);
            analysis.setAnalysisResult(truncated);
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            CaseAnalysisResponse.populateCounts(analysis, truncated);
            CaseAnalysisResponse.populateRiskScore(analysis, truncated);
        }
        // F-185 SF-185-01 — purger l'état partiel : il est désormais remplacé par
        // analysisResult complet (DONE) ou rendu obsolète par l'échec (FAILED).
        analysis.setPartialState(null);
        caseAnalysisRepository.save(analysis);

        if (failure == null) {
            procedureCheckService.createChecks(analysis, analysis.getAnalysisResult());
            try {
                strategicOptionService.persistFromAnalysis(analysis, analysis.getAnalysisResult());
            } catch (Exception e) {
                log.warn("Fail-open: strategic options persistence failed for analysis {}: {}", analysis.getId(), e.getMessage());
            }
            try {
                caseDeadlineService.createAiDetectedDeadlines(analysis, analysis.getAnalysisResult());
            } catch (Exception e) {
                log.warn("Fail-open: AI deadline detection failed for analysis {}: {}", analysis.getId(), e.getMessage());
            }
            // SF-IA-03-15a/17 : génération des phrases d'explication par source via Haiku (synchrone, fail-open).
            try {
                caseFileRepository.findById(caseFileId).ifPresent(cf -> {
                    List<SourceExplanationData> explanations = sourceExplanationGenerator.generate(cf, analysis);
                    sourceExplanationService.persist(analysis, explanations);
                });
            } catch (Exception e) {
                log.warn("Fail-open: source explanation generation failed for analysis {}: {}",
                        analysis.getId(), e.getMessage());
            }
            // F-179 SF-179-01 : vérification des références jurisprudentielles citées
            // dans les documents uploadés (existence + fidélité). Post-traitement
            // fail-open : une exception laisse l'analyse DONE.
            try {
                jurisprudenceVerificationService.verifyForAnalysis(analysis);
            } catch (Exception e) {
                log.warn("Fail-open: jurisprudence verification failed for analysis {}: {}",
                        analysis.getId(), e.getMessage());
            }
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setProcessedItems(1);
        job.setStatus(analysis.getAnalysisStatus());
        if (analysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Case analysis failed");
            reportJobFailureToSentry(caseFileId, JobType.CASE_ANALYSIS, "Case analysis failed");
        }
        analysisJobRepository.save(job);

        AnalysisStatus finalStatus = analysis.getAnalysisStatus();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, finalStatus, JobType.CASE_ANALYSIS));
                if (finalStatus == AnalysisStatus.DONE) {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.AI_QUESTION_GENERATION_EXCHANGE,
                            RabbitMQConfig.AI_QUESTION_GENERATION_ROUTING_KEY,
                            new AiQuestionGenerationMessage(caseFileId));
                }
            }
        });

        if (finalStatus == AnalysisStatus.DONE) {
            int promptTokens = analysis.getPromptTokens();
            int completionTokens = analysis.getCompletionTokens();
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.CASE_ANALYSIS,
                        promptTokens, completionTokens));
        }
    }

    private void reportJobFailureToSentry(UUID caseFileId, JobType jobType, String errorMessage) {
        try {
            if (!Sentry.isEnabled()) return;
            SentryEvent event = new SentryEvent();
            event.setLevel(SentryLevel.ERROR);
            Message msg = new Message();
            msg.setMessage("IA job FAILED: %s for caseFile %s".formatted(jobType, caseFileId));
            event.setMessage(msg);
            event.setTag("caseFileId", caseFileId.toString());
            event.setTag("jobType", jobType.name());
            event.setTag("errorMessage", errorMessage);
            Sentry.captureEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to report job failure to Sentry", ex);
        }
    }

    private String buildAggregatedPrompt(List<DocumentAnalysis> documentAnalyses) {
        List<DocumentAnalysis> sorted = documentAnalyses.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();

        // Charge les extractions en batch pour récupérer le texte brut (prefix).
        // Permet à Claude de voir directement "Convention de rupture signée le..."
        // au lieu de s'appuyer uniquement sur les faits pré-extraits qui peuvent
        // être biaisés par les arguments de requalification.
        List<UUID> docIds = sorted.stream().map(da -> da.getDocument().getId()).toList();
        Map<UUID, DocumentExtraction> extractionsByDocId = documentExtractionRepository
                .findByDocumentIdIn(docIds).stream()
                .collect(Collectors.toMap(e -> e.getDocument().getId(), e -> e, (a, b) -> a, HashMap::new));

        return IntStream.range(0, sorted.size())
                .mapToObj(i -> {
                    DocumentAnalysis da = sorted.get(i);
                    String filename = da.getDocument().getOriginalFilename();
                    String label = filename != null ? filename : "document-%d".formatted(i);

                    String rawPrefix = "";
                    DocumentExtraction ex = extractionsByDocId.get(da.getDocument().getId());
                    if (ex != null && ex.getExtractionStatus() == ExtractionStatus.DONE
                            && ex.getExtractedText() != null && !ex.getExtractedText().isBlank()) {
                        String text = ex.getExtractedText();
                        String slice = text.length() <= RAW_DOC_PREFIX_CHARS
                                ? text : text.substring(0, RAW_DOC_PREFIX_CHARS) + " [...]";
                        rawPrefix = "\n[Extrait du document brut] " + slice.replace("\n", " ").trim() + "\n";
                    }

                    return "%s : %s%s".formatted(label, da.getAnalysisResult(), rawPrefix);
                })
                .collect(Collectors.joining("\n"));
    }
}

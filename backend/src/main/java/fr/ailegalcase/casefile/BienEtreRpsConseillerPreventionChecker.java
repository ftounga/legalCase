package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-30 : analyseur de conformité procédurale de la <b>saisine du
 * Conseiller en Prévention Aspects Psychosociaux (CPAP)</b> — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 04/08/1996</b> relative au bien-être des travailleurs
 *       lors de l'exécution de leur travail — <b>art. 32sexies</b>
 *       organisation de la prévention des risques psychosociaux,
 *       désignation obligatoire du CPAP par l'employeur (§ 2),
 *       protection contre les représailles 12 mois post-dépôt.</li>
 *   <li><b>Loi du 04/08/1996 art. 32/1 § 2</b> (introduit par la Loi
 *       du 28/02/2014) — définition étendue des risques psychosociaux :
 *       harcèlement moral, harcèlement sexuel, violence au travail,
 *       autres risques (stress, burn-out, conflits qualifiés, charge
 *       mentale).</li>
 *   <li><b>AR du 10/04/2014</b> relatif à la prévention des risques
 *       psychosociaux au travail — modalités de la procédure interne :
 *       <ul>
 *         <li><b>art. 11 à 13</b> — analyse des risques psychosociaux
 *             par l'employeur, mesures de prévention collective.</li>
 *         <li><b>art. 14 à 16</b> — demande d'intervention psychosociale
 *             informelle (médiation interne confidentielle, pas de
 *             rapport employeur).</li>
 *         <li><b>art. 16 § 1</b> — entretien préalable obligatoire avec
 *             le CPAP avant tout dépôt de demande formelle, fixé dans
 *             les 10 jours calendrier du 1er contact.</li>
 *         <li><b>art. 16 § 2 et 3</b> — formalisme demande formelle :
 *             écrite, signée, datée, contenu (description faits,
 *             personnes impliquées, mesures souhaitées).</li>
 *         <li><b>art. 16 § 4</b> — accusé de réception remis au
 *             travailleur dans les 10 jours.</li>
 *         <li><b>art. 17 § 1</b> — notification de la demande à
 *             l'employeur par le CPAP après acceptation.</li>
 *         <li><b>art. 22 § 1</b> — délai de 3 mois pour rendre l'avis
 *             motivé, prolongeable de 3 mois sur demande motivée
 *             (6 mois maximum).</li>
 *         <li><b>art. 32</b> — délai de 2 mois pour l'employeur pour
 *             notifier les mesures prises au CPAP et au travailleur.</li>
 *       </ul>
 *   </li>
 *   <li><b>CCT n° 72 du 30/03/1999</b> (rendue obligatoire AR
 *       21/06/1999) — gestion du stress au travail, prévention
 *       collective.</li>
 *   <li><b>Cass. BE 23/12/2014 S.14.0026.N</b> — interprétation stricte
 *       du formalisme de la demande formelle (écrit, signature,
 *       contenu) pour la validité de la saisine.</li>
 *   <li><b>Cass. BE 20/01/2014 S.12.0064.F</b> — protection 12 mois
 *       art. 32sexies opposable même si la qualification harcèlement
 *       n'est finalement pas retenue, dès lors que la demande formelle
 *       a été déposée de bonne foi.</li>
 * </ul>
 *
 * <h2>Logique du qualificateur</h2>
 * <ol>
 *   <li>Si {@code conseillerIdentifie = false} → verdict
 *       {@link BienEtreRpsConseillerPreventionVerdict#NON_CONFORME_PAS_DE_CONSEILLER}
 *       (manquement employeur art. 32sexies § 2).</li>
 *   <li>Sinon, calcule {@code formalitesPrealablesCompletes} selon le
 *       mode de demande et l'étape atteinte :
 *       <ul>
 *         <li>Informelle → entretien préalable suffit (l'écrit n'est
 *             pas exigé).</li>
 *         <li>Formelle (individuelle ou collective) → entretien
 *             préalable + écrit signé + accusé de réception requis.</li>
 *       </ul>
 *   </li>
 *   <li>Si l'étape est ≥ {@link BienEtreRpsConseillerPreventionEtape#DEMANDE_FORMELLE_DEPOSEE}
 *       et que les formalités préalables ne sont pas complètes →
 *       {@link BienEtreRpsConseillerPreventionVerdict#NON_CONFORME_FORMALITES_MANQUANTES}.</li>
 *   <li>Selon l'étape atteinte :
 *       <ul>
 *         <li>{@code INTENTION_DEMANDE} ou {@code ENTRETIEN_PREALABLE_REALISE}
 *             → {@link BienEtreRpsConseillerPreventionVerdict#SAISINE_CONFORME}
 *             (préparation conforme).</li>
 *         <li>{@code DEMANDE_INFORMELLE_EN_COURS} →
 *             {@link BienEtreRpsConseillerPreventionVerdict#SAISINE_INFORMELLE_EN_COURS}.</li>
 *         <li>{@code DEMANDE_FORMELLE_DEPOSEE} →
 *             {@link BienEtreRpsConseillerPreventionVerdict#SAISINE_FORMELLE_EN_COURS}.</li>
 *         <li>{@code AVIS_RENDU} ou {@code MESURES_PRISES_PAR_EMPLOYEUR} →
 *             vérifie le respect du délai 3 mois enquête :
 *             {@link BienEtreRpsConseillerPreventionVerdict#AVIS_RENDU_DELAI_RESPECTE}
 *             ou
 *             {@link BienEtreRpsConseillerPreventionVerdict#AVIS_RENDU_DELAI_DEPASSE}.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h2>Calcul des échéances</h2>
 * <ul>
 *   <li>{@code echeanceEnqueteTroisMois} = {@code dateDepotDemande}
 *       + 3 mois (art. 22 § 1 AR 10/04/2014).</li>
 *   <li>{@code echeanceMesuresEmployeurDeuxMois} = {@code dateAvisRendu}
 *       + 2 mois (art. 32 AR 10/04/2014).</li>
 *   <li>{@code finProtectionRepresailles} = {@code dateDepotDemande}
 *       + 12 mois (Loi 04/08/1996 art. 32sexies).</li>
 *   <li>{@code protectionRepresaillesActive} = true si demande formelle
 *       déposée (les modes informels ne déclenchent pas la protection
 *       12 mois — Cass. BE 20/01/2014 S.12.0064.F).</li>
 * </ul>
 */
public final class BienEtreRpsConseillerPreventionChecker {

    /** Délai de l'enquête CPAP en demande formelle (AR 10/04/2014 art. 22). */
    static final int DELAI_ENQUETE_MOIS = 3;

    /** Délai mesures employeur (AR 10/04/2014 art. 32). */
    static final int DELAI_MESURES_EMPLOYEUR_MOIS = 2;

    /** Durée de la protection contre les représailles (Loi 04/08/1996 art. 32sexies). */
    static final int PROTECTION_REPRESAILLES_MOIS = 12;

    /** Délai max pour fixer l'entretien préalable (AR 10/04/2014 art. 16 § 1). */
    static final int DELAI_ENTRETIEN_PREALABLE_JOURS = 10;

    /** Délai d'avis maximal après prolongation motivée (3 + 3 mois). */
    static final int DELAI_ENQUETE_PROLONGE_MOIS = 6;

    static final String BASE_JURIDIQUE =
            "Loi du 04/08/1996 M.B. 18/09/1996 relative au bien-être"
                    + " des travailleurs lors de l'exécution de leur"
                    + " travail art. 32sexies organisation de la"
                    + " prévention des risques psychosociaux"
                    + " désignation obligatoire du Conseiller en"
                    + " Prévention Aspects Psychosociaux CPAP par"
                    + " l'employeur paragraphe 2 et protection contre"
                    + " les représailles douze mois post-dépôt ; Loi"
                    + " du 04/08/1996 art. 32/1 paragraphe 2 introduit"
                    + " par la Loi du 28/02/2014 M.B. 28/04/2014"
                    + " définition étendue des risques psychosociaux"
                    + " harcèlement moral harcèlement sexuel violence"
                    + " au travail autres risques stress burn-out"
                    + " conflits qualifiés charge mentale ; AR du"
                    + " 10/04/2014 M.B. 28/04/2014 relatif à la"
                    + " prévention des risques psychosociaux au travail"
                    + " art. 11 à 13 analyse des risques par l'employeur"
                    + " et mesures de prévention collective art. 14 à"
                    + " 16 demande d'intervention psychosociale"
                    + " informelle médiation interne confidentielle"
                    + " sans rapport employeur art. 16 paragraphe 1"
                    + " entretien préalable obligatoire avec le CPAP"
                    + " avant tout dépôt de demande formelle fixé dans"
                    + " les dix jours calendrier du premier contact"
                    + " art. 16 paragraphes 2 et 3 formalisme demande"
                    + " formelle écrite signée datée avec description"
                    + " des faits personnes impliquées et mesures"
                    + " souhaitées art. 16 paragraphe 4 accusé de"
                    + " réception remis au travailleur dans les dix"
                    + " jours art. 17 paragraphe 1 notification de la"
                    + " demande à l'employeur par le CPAP après"
                    + " acceptation art. 22 paragraphe 1 délai de trois"
                    + " mois pour rendre l'avis motivé prolongeable de"
                    + " trois mois sur demande motivée six mois maximum"
                    + " art. 32 délai de deux mois pour l'employeur"
                    + " pour notifier les mesures prises au CPAP et au"
                    + " travailleur ; CCT n° 72 du 30/03/1999 rendue"
                    + " obligatoire par AR du 21/06/1999 M.B. 09/07/1999"
                    + " gestion du stress au travail et prévention"
                    + " collective ; Cass. BE 23/12/2014 S.14.0026.N"
                    + " interprétation stricte du formalisme de la"
                    + " demande formelle écrit signature contenu pour"
                    + " la validité de la saisine ; Cass. BE 20/01/2014"
                    + " S.12.0064.F protection douze mois art. 32sexies"
                    + " opposable même si la qualification harcèlement"
                    + " n'est finalement pas retenue dès lors que la"
                    + " demande formelle a été déposée de bonne foi.";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse de saisine du Conseiller en Prévention"
                    + " Aspects Psychosociaux CPAP produit un verdict"
                    + " indicatif fondé sur les déclarations de"
                    + " l'avocat sur la conformité procédurale de la"
                    + " demande au regard de l'AR du 10/04/2014 et de"
                    + " la Loi du 04/08/1996 art. 32sexies. L'outil ne"
                    + " se prononce PAS sur le fond du harcèlement"
                    + " moral sexuel ou de la violence au travail —"
                    + " cette qualification matérielle relève des"
                    + " outils harcèlement BE dédiés voir SF-213-07"
                    + " harcelement-be-procedure-formelle qui couvre la"
                    + " procédure formelle déjà déposée et F-DT-30"
                    + " harcelement-licenciement-nul-section qui couvre"
                    + " la nullité du licenciement représailles."
                    + " L'avocat spécialisé en droit social BE doit"
                    + " confirmer pour la situation considérée : (i) la"
                    + " désignation effective d'un CPAP interne ou"
                    + " externe SEPP par l'employeur art. 32sexies"
                    + " paragraphe 2 ; (ii) la qualification provisoire"
                    + " du risque psychosocial parmi les quatre"
                    + " catégories de l'art. 32/1 paragraphe 2 ; (iii)"
                    + " la conformité du formalisme de la demande"
                    + " formelle écrit signé daté avec description"
                    + " circonstanciée des faits personnes impliquées"
                    + " et mesures souhaitées art. 16 paragraphes 2"
                    + " et 3 AR 10/04/2014 sous peine de nullité de la"
                    + " saisine Cass. BE 23/12/2014 S.14.0026.N ; (iv)"
                    + " la remise effective de l'accusé de réception"
                    + " art. 16 paragraphe 4 et la notification à"
                    + " l'employeur art. 17 paragraphe 1 ; (v) le"
                    + " respect des délais légaux entretien préalable"
                    + " dix jours art. 16 paragraphe 1 avis motivé"
                    + " trois mois prolongeable à six mois art. 22"
                    + " paragraphe 1 mesures employeur deux mois"
                    + " art. 32 ; (vi) le déclenchement de la"
                    + " protection contre les représailles douze mois"
                    + " art. 32sexies effective dès le dépôt de la"
                    + " demande formelle Cass. BE 20/01/2014"
                    + " S.12.0064.F ; (vii) la possibilité de"
                    + " basculer d'une demande informelle vers une"
                    + " demande formelle à tout moment de la procédure ;"
                    + " (viii) l'articulation avec les voies externes"
                    + " saisine de l'Inspection Contrôle du bien-être"
                    + " au travail Loi 04/08/1996 art. 80 et suivants"
                    + " tribunal du travail art. 578 C. jud. action en"
                    + " cessation art. 588 C. jud. action pénale"
                    + " art. 119 C. pén. soc. ; (ix) le choix du mode"
                    + " formel individuel ou collectif selon que les"
                    + " faits visent une personne précise ou une"
                    + " situation organisationnelle art. 16"
                    + " paragraphe 2 AR 10/04/2014. La présente"
                    + " analyse constitue un éclairage indicatif et ne"
                    + " remplace pas la consultation d'un avocat"
                    + " compétent en droit social BE.";

    private BienEtreRpsConseillerPreventionChecker() {
    }

    public static BienEtreRpsConseillerPreventionResult analyze(
            BienEtreRpsConseillerPreventionRequest request) {
        validateInputs(request);

        boolean formalitesCompletes = formalitesPrealablesCompletes(request);
        LocalDate echeanceEnquete = request.dateDepotDemande() == null
                ? null
                : request.dateDepotDemande().plusMonths(DELAI_ENQUETE_MOIS);
        LocalDate echeanceMesures = request.dateAvisRendu() == null
                ? null
                : request.dateAvisRendu().plusMonths(DELAI_MESURES_EMPLOYEUR_MOIS);
        LocalDate finProtection = request.dateDepotDemande() == null
                ? null
                : request.dateDepotDemande().plusMonths(PROTECTION_REPRESAILLES_MOIS);
        boolean protectionActive = isProtectionActive(request);

        boolean delaiEnqueteRespecte = isDelaiEnqueteRespecte(request);

        // 1) Pas de CPAP désigné — manquement art. 32sexies § 2
        if (!request.conseillerIdentifie()) {
            return buildPasDeConseiller(request, formalitesCompletes,
                    echeanceEnquete, echeanceMesures, finProtection,
                    protectionActive, delaiEnqueteRespecte);
        }

        // 2) Formalités préalables manquantes pour étapes ≥ dépôt formel
        boolean etapeRequiertFormalitesCompletes =
                request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE
                        || request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.AVIS_RENDU
                        || request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.MESURES_PRISES_PAR_EMPLOYEUR;
        if (etapeRequiertFormalitesCompletes && !formalitesCompletes) {
            return buildFormalitesManquantes(request, formalitesCompletes,
                    echeanceEnquete, echeanceMesures, finProtection,
                    protectionActive, delaiEnqueteRespecte);
        }

        // 3) Branches selon étape
        return switch (request.etapeProcedure()) {
            case INTENTION_DEMANDE, ENTRETIEN_PREALABLE_REALISE ->
                    buildSaisineConforme(request, formalitesCompletes,
                            echeanceEnquete, echeanceMesures, finProtection,
                            protectionActive, delaiEnqueteRespecte);
            case DEMANDE_INFORMELLE_EN_COURS ->
                    buildInformelleEnCours(request, formalitesCompletes,
                            echeanceEnquete, echeanceMesures, finProtection,
                            protectionActive, delaiEnqueteRespecte);
            case DEMANDE_FORMELLE_DEPOSEE ->
                    buildFormelleEnCours(request, formalitesCompletes,
                            echeanceEnquete, echeanceMesures, finProtection,
                            protectionActive, delaiEnqueteRespecte);
            case AVIS_RENDU, MESURES_PRISES_PAR_EMPLOYEUR -> {
                if (delaiEnqueteRespecte) {
                    yield buildAvisDelaiRespecte(request, formalitesCompletes,
                            echeanceEnquete, echeanceMesures, finProtection,
                            protectionActive, delaiEnqueteRespecte);
                }
                yield buildAvisDelaiDepasse(request, formalitesCompletes,
                        echeanceEnquete, echeanceMesures, finProtection,
                        protectionActive, delaiEnqueteRespecte);
            }
        };
    }

    // ── Branches verdict ────────────────────────────────────────────────────

    private static BienEtreRpsConseillerPreventionResult buildPasDeConseiller(
            BienEtreRpsConseillerPreventionRequest req,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte) {
        String synthese = "Aucun Conseiller en Prévention Aspects"
                + " Psychosociaux CPAP n'a été identifié dans"
                + " l'entreprise. Or, la Loi du 04/08/1996 art."
                + " 32sexies paragraphe 2 impose à l'employeur de"
                + " désigner soit un CPAP interne (entreprises ≥ 50"
                + " travailleurs avec service interne pour la"
                + " prévention et la protection au travail SIPP), soit"
                + " de recourir à un Service Externe pour la Prévention"
                + " et la Protection au travail SEPP. L'absence de CPAP"
                + " désigné constitue un manquement employeur ouvrant"
                + " plusieurs voies : (a) saisine directe de"
                + " l'Inspection Contrôle du bien-être au travail"
                + " art. 80 et suivants Loi 04/08/1996 pour"
                + " constatation de l'infraction au Code pénal social"
                + " art. 119 et suivants ; (b) action en cessation"
                + " devant le tribunal du travail art. 588 C. jud. ;"
                + " (c) saisine de l'auditeur du travail pour"
                + " poursuite pénale art. 138bis C. jud. — voir SF-219-25"
                + " auditorat-travail-be. L'avocat doit caractériser"
                + " l'absence effective de désignation CPAP en"
                + " consultant le règlement de travail, l'organigramme"
                + " du SIPP ou le contrat de service SEPP avant toute"
                + " action.";
        return build(req,
                BienEtreRpsConseillerPreventionVerdict.NON_CONFORME_PAS_DE_CONSEILLER,
                formalitesCompletes, echeanceEnquete, echeanceMesures,
                finProtection, protectionActive, delaiEnqueteRespecte,
                "ABSENCE_DESIGNATION_CPAP_ART_32SEXIES_PARAGRAPHE_2",
                synthese);
    }

    private static BienEtreRpsConseillerPreventionResult buildFormalitesManquantes(
            BienEtreRpsConseillerPreventionRequest req,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte) {
        StringBuilder manquants = new StringBuilder();
        if (!req.entretienPrealableRealise()) {
            manquants.append(" entretien préalable obligatoire art. 16"
                    + " paragraphe 1 AR 10/04/2014 dix jours calendrier"
                    + " ;");
        }
        if (!req.demandeEcriteSignee()) {
            manquants.append(" demande écrite signée datée art. 16"
                    + " paragraphes 2 et 3 AR 10/04/2014 ;");
        }
        if (!req.accuseReceptionRecu()) {
            manquants.append(" accusé de réception remis par le CPAP"
                    + " art. 16 paragraphe 4 AR 10/04/2014 ;");
        }
        String synthese = "La saisine du CPAP est NON CONFORME : des"
                + " formalités préalables requises pour une demande"
                + " formelle sont manquantes —" + manquants
                + " Le formalisme est interprété strictement par la"
                + " jurisprudence Cass. BE 23/12/2014 S.14.0026.N et la"
                + " nullité de la saisine peut être prononcée si"
                + " l'employeur ou le CPAP soulèvent l'irrégularité"
                + " formelle. Conséquences pratiques : (a) la protection"
                + " contre les représailles art. 32sexies douze mois"
                + " peut être contestée par l'employeur en cas de"
                + " représailles ultérieures (mesure défavorable,"
                + " licenciement) ; (b) le tribunal du travail saisi"
                + " ultérieurement peut écarter la demande formelle"
                + " pour vice de forme ; (c) l'avis CPAP rendu sur"
                + " une demande irrégulière a une force probante"
                + " réduite. L'avocat doit régulariser sans délai en"
                + " complétant les formalités manquantes (entretien,"
                + " écrit signé, AR) ou en redéposant une nouvelle"
                + " demande formelle conforme.";
        return build(req,
                BienEtreRpsConseillerPreventionVerdict.NON_CONFORME_FORMALITES_MANQUANTES,
                formalitesCompletes, echeanceEnquete, echeanceMesures,
                finProtection, protectionActive, delaiEnqueteRespecte,
                "FORMALITES_PREALABLES_MANQUANTES_ART_16_AR_10_04_2014",
                synthese);
    }

    private static BienEtreRpsConseillerPreventionResult buildSaisineConforme(
            BienEtreRpsConseillerPreventionRequest req,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte) {
        String synthese = "La préparation de la saisine du CPAP est"
                + " CONFORME aux exigences procédurales de l'AR du"
                + " 10/04/2014 art. 11 à 32. Le CPAP a été identifié"
                + " conformément à la Loi du 04/08/1996 art. 32sexies"
                + " paragraphe 2 (CPAP interne SIPP ou Service Externe"
                + " pour la Prévention et la Protection au travail"
                + " SEPP). L'avocat conseille le travailleur sur le"
                + " choix entre demande informelle (médiation interne"
                + " confidentielle sans rapport employeur art. 14 à 16"
                + " AR 10/04/2014) et demande formelle (enquête écrite"
                + " avec rapport motivé adressé à l'employeur art. 16"
                + " à 22 AR 10/04/2014). Points d'attention pour la"
                + " suite : (a) l'entretien préalable obligatoire avant"
                + " demande formelle doit être fixé dans les dix jours"
                + " calendrier du premier contact art. 16 paragraphe 1"
                + " AR 10/04/2014 ; (b) la demande formelle doit être"
                + " écrite signée datée avec description précise des"
                + " faits personnes impliquées et mesures souhaitées"
                + " art. 16 paragraphes 2 et 3 ; (c) la protection"
                + " contre les représailles art. 32sexies douze mois"
                + " ne s'enclenche qu'au dépôt de la demande formelle"
                + " — la voie informelle ne couvre pas Cass. BE"
                + " 20/01/2014 S.12.0064.F ; (d) le CPAP a trois mois"
                + " pour rendre son avis motivé prolongeable à six mois"
                + " sur demande motivée art. 22 paragraphe 1 ; (e)"
                + " l'employeur dispose ensuite de deux mois pour"
                + " notifier les mesures prises art. 32.";
        return build(req,
                BienEtreRpsConseillerPreventionVerdict.SAISINE_CONFORME,
                formalitesCompletes, echeanceEnquete, echeanceMesures,
                finProtection, protectionActive, delaiEnqueteRespecte,
                "FORMALITES_PREALABLES_CONFORMES",
                synthese);
    }

    private static BienEtreRpsConseillerPreventionResult buildInformelleEnCours(
            BienEtreRpsConseillerPreventionRequest req,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte) {
        String synthese = "La demande d'intervention psychosociale"
                + " INFORMELLE est en cours auprès du CPAP. La voie"
                + " informelle art. 14 à 16 AR 10/04/2014 vise une"
                + " conciliation interne confidentielle sans rapport"
                + " adressé à l'employeur ; aucun délai légal strict"
                + " n'est imposé. Points d'attention : (a) la protection"
                + " contre les représailles art. 32sexies douze mois"
                + " ne s'enclenche PAS pour la demande informelle"
                + " Cass. BE 20/01/2014 S.12.0064.F — le travailleur"
                + " reste exposé à d'éventuelles mesures défavorables"
                + " jusqu'au dépôt d'une demande formelle ; (b) si la"
                + " médiation échoue ou si l'employeur refuse de"
                + " coopérer le travailleur peut basculer à tout moment"
                + " vers la demande formelle sans préjudice du temps"
                + " déjà passé en informelle ; (c) l'écrit de la"
                + " demande informelle n'est pas exigé par l'AR"
                + " 10/04/2014 mais reste recommandé pour préconstituer"
                + " la preuve de la saisine ; (d) le CPAP est tenu au"
                + " secret professionnel art. 458 Code pénal pour les"
                + " échanges informels.";
        return build(req,
                BienEtreRpsConseillerPreventionVerdict.SAISINE_INFORMELLE_EN_COURS,
                formalitesCompletes, echeanceEnquete, echeanceMesures,
                finProtection, protectionActive, delaiEnqueteRespecte,
                "DEMANDE_INFORMELLE_EN_COURS_ART_14_16_AR_10_04_2014",
                synthese);
    }

    private static BienEtreRpsConseillerPreventionResult buildFormelleEnCours(
            BienEtreRpsConseillerPreventionRequest req,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte) {
        String mode = req.modeDemande()
                == BienEtreRpsConseillerPreventionModeDemande.FORMELLE_COLLECTIVE
                ? "FORMELLE COLLECTIVE (analyse organisationnelle de la"
                        + " situation de travail visant un groupe de"
                        + " travailleurs art. 16 paragraphe 2 AR"
                        + " 10/04/2014)"
                : "FORMELLE INDIVIDUELLE (enquête écrite individualisée"
                        + " avec rapport motivé adressé à l'employeur"
                        + " art. 16 à 22 AR 10/04/2014)";
        String echeance = echeanceEnquete == null
                ? "non calculable (date de dépôt non renseignée)"
                : echeanceEnquete.toString();
        String synthese = "La demande d'intervention psychosociale "
                + mode + " est en cours auprès du CPAP. Le délai"
                + " d'enquête CPAP court : le CPAP doit rendre son avis"
                + " motivé dans un délai de trois mois à compter du"
                + " dépôt de la demande art. 22 paragraphe 1 AR"
                + " 10/04/2014, prolongeable de trois mois maximum sur"
                + " demande motivée notifiée à l'employeur et au"
                + " travailleur — soit six mois maximum. Échéance"
                + " calculée trois mois : " + echeance + ". Points"
                + " d'attention : (a) la protection contre les"
                + " représailles art. 32sexies douze mois est ACTIVE"
                + " depuis le dépôt de la demande formelle — toute"
                + " mesure défavorable prise par l'employeur dans cette"
                + " fenêtre est présumée représailles et entraîne la"
                + " nullité du licenciement plus indemnité voir F-DT-30"
                + " ; (b) la notification de la demande à l'employeur"
                + " par le CPAP art. 17 paragraphe 1 est obligatoire et"
                + " doit être effectuée après acceptation de la"
                + " demande ; (c) le travailleur conserve le droit"
                + " d'exercer en parallèle une action en cessation"
                + " art. 588 C. jud. ou de saisir l'auditorat du"
                + " travail pour les faits constitutifs d'infraction"
                + " pénale sociale voir SF-219-25 auditorat-travail-be ;"
                + " (d) le CPAP est tenu au secret professionnel"
                + " art. 458 Code pénal mais peut rapporter les faits"
                + " objectivement dans l'avis motivé ; (e) si"
                + " l'identité du demandeur doit rester confidentielle"
                + " le travailleur peut demander l'anonymisation à"
                + " l'égard du présumé harceleur sous réserve de"
                + " l'effectivité du contradictoire.";
        return build(req,
                BienEtreRpsConseillerPreventionVerdict.SAISINE_FORMELLE_EN_COURS,
                formalitesCompletes, echeanceEnquete, echeanceMesures,
                finProtection, protectionActive, delaiEnqueteRespecte,
                "DEMANDE_FORMELLE_EN_COURS_DELAI_ENQUETE_TROIS_MOIS",
                synthese);
    }

    private static BienEtreRpsConseillerPreventionResult buildAvisDelaiRespecte(
            BienEtreRpsConseillerPreventionRequest req,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte) {
        String echMesures = echeanceMesures == null
                ? "non calculable (date de l'avis non renseignée)"
                : echeanceMesures.toString();
        String synthese = "Le CPAP a rendu son AVIS MOTIVÉ dans les"
                + " délais légaux art. 22 paragraphe 1 AR 10/04/2014"
                + " (trois mois prolongeable à six mois). L'employeur"
                + " dispose maintenant d'un délai de deux mois à"
                + " compter de la notification de l'avis pour notifier"
                + " au CPAP et au travailleur les mesures qu'il prend"
                + " art. 32 AR 10/04/2014 — échéance calculée : "
                + echMesures + ". Points d'attention : (a) la"
                + " protection contre les représailles art. 32sexies"
                + " douze mois reste ACTIVE jusqu'à expiration de la"
                + " fenêtre depuis le dépôt de la demande formelle ;"
                + " (b) si les mesures employeur sont insuffisantes ou"
                + " inadéquates le travailleur peut saisir le tribunal"
                + " du travail art. 578 C. jud. action en cessation"
                + " art. 588 C. jud. ou demande d'indemnisation art."
                + " 32sexies pour les conséquences du harcèlement ;"
                + " (c) le travailleur peut consulter l'avis motivé"
                + " complet auprès du CPAP et en obtenir copie pour le"
                + " produire en justice ; (d) si le CPAP a constaté un"
                + " danger grave et immédiat il a pu activer la"
                + " procédure d'urgence art. 32septies AR 10/04/2014"
                + " avec saisine de l'Inspection Contrôle du bien-être ;"
                + " (e) l'avis motivé est confidentiel mais peut être"
                + " produit dans une procédure judiciaire ultérieure"
                + " sous réserve du respect des droits de la défense"
                + " du présumé harceleur.";
        return build(req,
                BienEtreRpsConseillerPreventionVerdict.AVIS_RENDU_DELAI_RESPECTE,
                formalitesCompletes, echeanceEnquete, echeanceMesures,
                finProtection, protectionActive, delaiEnqueteRespecte,
                "AVIS_MOTIVE_RENDU_DELAI_ART_22_RESPECTE",
                synthese);
    }

    private static BienEtreRpsConseillerPreventionResult buildAvisDelaiDepasse(
            BienEtreRpsConseillerPreventionRequest req,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte) {
        String synthese = "Le CPAP a rendu son AVIS MOTIVÉ HORS DÉLAI"
                + " — au-delà des trois mois (ou six mois si"
                + " prolongation motivée) prévus par l'art. 22"
                + " paragraphe 1 AR 10/04/2014. Cette irrégularité de"
                + " délai engage la responsabilité professionnelle du"
                + " CPAP et du Service Externe pour la Prévention"
                + " SEPP, mais N'INVALIDE PAS l'avis rendu (le délai"
                + " de trois mois est un délai d'ordre, non sanctionné"
                + " par la nullité). Conséquences pratiques : (a)"
                + " l'employeur conserve son délai de deux mois pour"
                + " notifier les mesures à compter de la réception"
                + " effective de l'avis art. 32 AR 10/04/2014 ; (b) la"
                + " protection contre les représailles art. 32sexies"
                + " douze mois court depuis le dépôt de la demande"
                + " formelle et reste donc opposable sous réserve"
                + " d'expiration ; (c) le travailleur peut signaler le"
                + " dépassement de délai à l'Inspection Contrôle du"
                + " bien-être au travail art. 80 et suivants Loi"
                + " 04/08/1996 et demander l'application des sanctions"
                + " pénales prévues à l'art. 119 et suivants Code"
                + " pénal social ; (d) le retard peut être invoqué"
                + " comme circonstance aggravante en cas de contentieux"
                + " ultérieur devant le tribunal du travail art. 578 C."
                + " jud. ; (e) le travailleur peut demander des"
                + " dommages et intérêts au CPAP fautif ou à"
                + " l'employeur pour défaut d'organisation de la"
                + " procédure dans les délais légaux.";
        return build(req,
                BienEtreRpsConseillerPreventionVerdict.AVIS_RENDU_DELAI_DEPASSE,
                formalitesCompletes, echeanceEnquete, echeanceMesures,
                finProtection, protectionActive, delaiEnqueteRespecte,
                "AVIS_MOTIVE_RENDU_DELAI_ART_22_DEPASSE",
                synthese);
    }

    // ── Helpers calculs ─────────────────────────────────────────────────────

    private static boolean formalitesPrealablesCompletes(
            BienEtreRpsConseillerPreventionRequest req) {
        return switch (req.modeDemande()) {
            case INFORMELLE ->
                    // Informelle : entretien préalable suffit (écrit non exigé)
                    req.entretienPrealableRealise();
            case FORMELLE_INDIVIDUELLE, FORMELLE_COLLECTIVE ->
                    // Formelle : entretien + écrit signé + accusé de réception
                    req.entretienPrealableRealise()
                            && req.demandeEcriteSignee()
                            && req.accuseReceptionRecu();
        };
    }

    private static boolean isProtectionActive(
            BienEtreRpsConseillerPreventionRequest req) {
        // Protection 12 mois art. 32sexies enclenchée uniquement par la
        // demande formelle (Cass. BE 20/01/2014 S.12.0064.F)
        boolean modeFormel = req.modeDemande()
                != BienEtreRpsConseillerPreventionModeDemande.INFORMELLE;
        boolean etapeAtteinte =
                req.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE
                        || req.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.AVIS_RENDU
                        || req.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.MESURES_PRISES_PAR_EMPLOYEUR;
        return modeFormel && etapeAtteinte;
    }

    private static boolean isDelaiEnqueteRespecte(
            BienEtreRpsConseillerPreventionRequest req) {
        // Pertinent uniquement à partir de AVIS_RENDU.
        if (req.etapeProcedure() != BienEtreRpsConseillerPreventionEtape.AVIS_RENDU
                && req.etapeProcedure() != BienEtreRpsConseillerPreventionEtape.MESURES_PRISES_PAR_EMPLOYEUR) {
            return true;
        }
        if (req.dateDepotDemande() == null || req.dateAvisRendu() == null) {
            // Si pas de dates, on ne peut pas conclure au dépassement
            // — on retient le défaut de présomption favorable.
            return true;
        }
        LocalDate limite = req.dateDepotDemande()
                .plusMonths(DELAI_ENQUETE_PROLONGE_MOIS);
        return !req.dateAvisRendu().isAfter(limite);
    }

    // ── Construction du résultat ────────────────────────────────────────────

    private static BienEtreRpsConseillerPreventionResult build(
            BienEtreRpsConseillerPreventionRequest req,
            BienEtreRpsConseillerPreventionVerdict verdict,
            boolean formalitesCompletes,
            LocalDate echeanceEnquete, LocalDate echeanceMesures,
            LocalDate finProtection, boolean protectionActive,
            boolean delaiEnqueteRespecte,
            String raison, String synthese) {
        return new BienEtreRpsConseillerPreventionResult(
                req.typeRisque(),
                req.modeDemande(),
                req.etapeProcedure(),
                req.conseillerIdentifie(),
                req.entretienPrealableRealise(),
                req.demandeEcriteSignee(),
                req.accuseReceptionRecu(),
                req.notificationEmployeurFaite(),
                req.dateDepotDemande(),
                req.dateAvisRendu(),
                req.joursDepuisDepot(),
                verdict,
                formalitesCompletes,
                delaiEnqueteRespecte,
                echeanceEnquete,
                echeanceMesures,
                finProtection,
                protectionActive,
                raison, synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static void validateInputs(
            BienEtreRpsConseillerPreventionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.typeRisque() == null) {
            throw new IllegalArgumentException("typeRisque est requis");
        }
        if (request.modeDemande() == null) {
            throw new IllegalArgumentException("modeDemande est requis");
        }
        if (request.etapeProcedure() == null) {
            throw new IllegalArgumentException("etapeProcedure est requis");
        }
        if (request.conseillerIdentifie() == null) {
            throw new IllegalArgumentException(
                    "conseillerIdentifie est requis");
        }
        if (request.entretienPrealableRealise() == null) {
            throw new IllegalArgumentException(
                    "entretienPrealableRealise est requis");
        }
        if (request.demandeEcriteSignee() == null) {
            throw new IllegalArgumentException(
                    "demandeEcriteSignee est requis");
        }
        if (request.accuseReceptionRecu() == null) {
            throw new IllegalArgumentException(
                    "accuseReceptionRecu est requis");
        }
        if (request.notificationEmployeurFaite() == null) {
            throw new IllegalArgumentException(
                    "notificationEmployeurFaite est requis");
        }
        if (request.joursDepuisDepot() != null
                && request.joursDepuisDepot() < 0) {
            throw new IllegalArgumentException(
                    "joursDepuisDepot ne peut pas être négatif");
        }
        // Cohérence date : si l'étape requiert un dépôt, la date doit être présente
        if ((request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.DEMANDE_FORMELLE_DEPOSEE
                || request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.AVIS_RENDU
                || request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.MESURES_PRISES_PAR_EMPLOYEUR)
                && request.dateDepotDemande() == null) {
            throw new IllegalArgumentException(
                    "dateDepotDemande est requise à partir de l'étape"
                            + " DEMANDE_FORMELLE_DEPOSEE");
        }
        if ((request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.AVIS_RENDU
                || request.etapeProcedure() == BienEtreRpsConseillerPreventionEtape.MESURES_PRISES_PAR_EMPLOYEUR)
                && request.dateAvisRendu() == null) {
            throw new IllegalArgumentException(
                    "dateAvisRendu est requise à partir de l'étape"
                            + " AVIS_RENDU");
        }
    }
}

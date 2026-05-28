package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * SF-219-28 : analyseur de la reconnaissance d'une maladie
 * professionnelle Fedris BE — fonction <b>pure</b> (sans dépendance
 * HTTP / persistance / horloge — l'horloge ne sert qu'à la
 * proratisation prospective via {@link LocalDate#now(ZoneId)} lorsque
 * {@code dateDeclarationEnvisagee} est null).
 *
 * <h2>Source juridique BE</h2>
 * <ul>
 *   <li><b>Lois coordonnées du 03/06/1970</b> relatives à la
 *       prévention des maladies professionnelles et à la réparation
 *       des dommages résultant de celles-ci (M.B. 27/08/1970) —
 *       régime de réparation forfaitaire des maladies professionnelles
 *       financé par Fedris (ex-Fonds des Maladies Professionnelles
 *       fusionné avec le FAT au 01/01/2017).</li>
 *   <li><b>Art. 30 lois coordonnées 03/06/1970</b> — système de liste
 *       fermée : sont reconnues les maladies inscrites sur la liste
 *       dressée par le Roi (AR 28/03/1969).</li>
 *   <li><b>Art. 30bis lois coordonnées 03/06/1970</b> (introduit par
 *       Loi du 29/12/1990) — système ouvert : peut également être
 *       reconnue toute maladie non listée pour laquelle la victime
 *       établit que le lien causal avec l'exposition professionnelle
 *       est direct et déterminant.</li>
 *   <li><b>Art. 32 lois coordonnées 03/06/1970</b> — présomption
 *       juridique de causalité : pour les maladies de la liste fermée,
 *       le lien causal est présumé dès lors que la victime a été
 *       exposée au risque pendant la période minimale requise par
 *       l'AR. Présomption renversable mais difficilement (Cass. BE
 *       13/12/1989 Pas. 1990 I 451).</li>
 *   <li><b>Art. 31 lois coordonnées 03/06/1970</b> — délai de
 *       prescription de l'action en réparation : 3 ans à compter de
 *       la date à laquelle la victime a eu connaissance du caractère
 *       professionnel de la maladie.</li>
 *   <li><b>Art. 53 lois coordonnées 03/06/1970</b> — voie de recours :
 *       tribunal du travail dans le délai d'un an à compter de la
 *       décision de refus de Fedris (art. 580 1° C. jud.).</li>
 *   <li><b>AR du 28/03/1969</b> (M.B. 04/04/1969) dressant la liste
 *       des maladies professionnelles donnant lieu à réparation,
 *       modifié à de nombreuses reprises et en dernier lieu par l'AR
 *       du 06/12/2018 (M.B. 24/12/2018). Sections 1.x agents
 *       chimiques, 2.x agents physiques (notamment 2.1 vibrations,
 *       2.2 bruit, 2.3 amiante), 3.x agents biologiques, 4.x maladies
 *       cutanées et respiratoires, 5.x maladies musculosquelettiques
 *       (section ouverte par l'AR 06/12/2018), 6.x cancers
 *       professionnels.</li>
 *   <li><b>AR du 16/12/1985</b> (M.B. 31/12/1985) relatif aux
 *       travailleurs occupés à un travail comportant un risque
 *       d'apparition d'une maladie professionnelle — modalités du
 *       système ouvert.</li>
 *   <li><b>Loi du 11/01/2018</b> (M.B. 12/02/2018) portant
 *       l'organisation administrative de Fedris — fusion FMP (Fonds
 *       des Maladies Professionnelles) et FAT (Fonds des Accidents
 *       du Travail) au 01/01/2017.</li>
 *   <li><b>Cass. BE 28/05/2008 S.07.0033.F</b> — système ouvert :
 *       l'exposition professionnelle doit être la cause directe et
 *       déterminante de la maladie ; un simple facteur favorisant
 *       parmi d'autres ne suffit pas.</li>
 *   <li><b>Cass. BE 04/02/2002 S.01.0095.F</b> — confirme le caractère
 *       direct et essentiel requis pour la reconnaissance en système
 *       ouvert.</li>
 *   <li><b>Cass. BE 13/12/1989 Pas. 1990 I 451</b> — présomption art.
 *       32 lois coordonnées est juridique et non médicale, mais
 *       renversable par la preuve contraire de l'absence de lien
 *       causal.</li>
 *   <li><b>Cour const. arrêt 51/2008 du 13/03/2008</b> — conformité
 *       du régime de réparation forfaitaire MP au principe d'égalité
 *       et au droit à la réparation intégrale du préjudice corporel.</li>
 * </ul>
 *
 * <h2>Logique de l'analyseur</h2>
 * <ol>
 *   <li>Calcule la date de prescription = (dateConnaissanceProfessionnel
 *       ou dateConstatationMedicale a defaut) + 3 ans, et le drapeau
 *       prescriteAuJour selon que la dateDeclarationEnvisagee (ou la
 *       date du jour) est posterieure.</li>
 *   <li>Calcule les jours avant prescription (negatif = jours de
 *       retard, positif = marge restante).</li>
 *   <li>Si prescrite : verdict DECLARATION_PRESCRITE (priorite
 *       absolue).</li>
 *   <li>Si typeMaladie = LISTE_FERMEE :
 *     <ul>
 *       <li>exposition AVEREE : verdict
 *           MALADIE_LISTE_FERMEE_PRESOMPTION (art. 32 joue).</li>
 *       <li>exposition INSUFFISANTE : verdict
 *           MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE.</li>
 *       <li>exposition INDETERMINEE : verdict A_QUALIFIER.</li>
 *     </ul>
 *   </li>
 *   <li>Si typeMaladie = HORS_LISTE (systeme ouvert art. 30bis) :
 *     <ul>
 *       <li>causalite DIRECTE_DETERMINANTE : verdict
 *           SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE.</li>
 *       <li>causalite INSUFFISANTE : verdict
 *           SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE.</li>
 *       <li>causalite INDETERMINEE ou NON_APPLICABLE : verdict
 *           A_QUALIFIER.</li>
 *     </ul>
 *   </li>
 * </ol>
 */
public final class MpFedrisReconnaissanceChecker {

    /** Delai de prescription de l'action en reparation (art. 31 lois 1970). */
    public static final int PRESCRIPTION_ANS = 3;

    /** Fuseau horaire belge — coherent avec les autres outils BE. */
    public static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    static final String BASE_JURIDIQUE =
            "Lois coordonnees du 03/06/1970 relatives a la prevention des"
                    + " maladies professionnelles et a la reparation des"
                    + " dommages resultant de celles-ci M.B. 27/08/1970 ;"
                    + " art. 30 systeme de liste fermee, art. 30bis"
                    + " systeme ouvert preuve causalite directe et"
                    + " determinante introduit par la Loi du 29/12/1990,"
                    + " art. 31 prescription triennale de l'action en"
                    + " reparation a compter de la connaissance du"
                    + " caractere professionnel, art. 32 presomption"
                    + " juridique de causalite pour les maladies de la"
                    + " liste, art. 53 recours devant le tribunal du"
                    + " travail dans le delai d'un an art. 580 1° C. jud. ;"
                    + " AR du 28/03/1969 M.B. 04/04/1969 dressant la liste"
                    + " des maladies professionnelles donnant lieu a"
                    + " reparation modifie par l'AR du 06/12/2018 M.B."
                    + " 24/12/2018 sections 1.x agents chimiques, 2.x"
                    + " agents physiques notamment 2.1 vibrations, 2.2"
                    + " bruit, 2.3 amiante, 3.x agents biologiques, 4.x"
                    + " maladies cutanees et respiratoires, 5.x maladies"
                    + " musculosquelettiques section ouverte par l'AR du"
                    + " 06/12/2018, 6.x cancers professionnels ; AR du"
                    + " 16/12/1985 M.B. 31/12/1985 relatif aux travailleurs"
                    + " occupes a un travail comportant un risque"
                    + " d'apparition d'une maladie professionnelle"
                    + " modalites du systeme ouvert ; Loi du 11/01/2018"
                    + " M.B. 12/02/2018 portant l'organisation"
                    + " administrative de Fedris fusion FMP / FAT au"
                    + " 01/01/2017 ; Cass. BE 28/05/2008 S.07.0033.F"
                    + " systeme ouvert exposition professionnelle cause"
                    + " directe et determinante un simple facteur"
                    + " favorisant parmi d'autres ne suffit pas ; Cass."
                    + " BE 04/02/2002 S.01.0095.F caractere direct et"
                    + " essentiel requis ; Cass. BE 13/12/1989 Pas. 1990"
                    + " I 451 presomption art. 32 juridique et non"
                    + " medicale renversable par la preuve contraire ;"
                    + " Cour const. arret 51/2008 du 13/03/2008"
                    + " conformite du regime de reparation forfaitaire MP"
                    + " au principe d'egalite et au droit a la reparation"
                    + " integrale du prejudice corporel.";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse de reconnaissance d'une maladie"
                    + " professionnelle Fedris BE produit un verdict"
                    + " indicatif fonde sur les declarations de l'avocat."
                    + " L'avocat specialise en droit social BE (axe"
                    + " accidents du travail et maladies professionnelles)"
                    + " doit confirmer pour la situation consideree : (i)"
                    + " la correspondance exacte de la maladie a un code"
                    + " de la liste fermee AR 28/03/1969 (sections 1.x"
                    + " a 6.x) ou la justification du recours au systeme"
                    + " ouvert art. 30bis lois coordonnees 03/06/1970 ;"
                    + " (ii) le respect des conditions specifiques de"
                    + " duree minimale d'exposition et de niveau"
                    + " d'exposition prevues par la section concernee de"
                    + " la liste fermee (par exemple : exposition aux"
                    + " poussieres d'amiante section 2.3.1 - duree"
                    + " minimale variable selon la pathologie : 1 an pour"
                    + " la fibrose pleurale, 10 ans pour la mesotheliome ;"
                    + " exposition au bruit section 2.2.2 - intensite"
                    + " minimale 85 dBA pendant 5 ans cumules ;"
                    + " vibrations section 2.1.1 - duree minimale 2 ans) ;"
                    + " (iii) la constitution du dossier de declaration"
                    + " Fedris formulaire 501 victime + 503 employeur +"
                    + " rapports medicaux + CV professionnel + attestations"
                    + " employeurs successifs documentant l'exposition ;"
                    + " (iv) le calcul exact des indemnites Fedris -"
                    + " indemnite d'incapacite temporaire (90% remuneration"
                    + " moyenne base art. 34 lois coordonnees plafond"
                    + " annuel ONSS), indemnite d'incapacite permanente"
                    + " (% IPP attribue par Fedris x remuneration de base"
                    + " art. 35), capital de deces art. 37 si maladie"
                    + " ayant entraine le deces dans les 365 jours, frais"
                    + " medicaux art. 31 octies, prothese art. 32 ;"
                    + " (v) le respect du delai de declaration a l'employeur"
                    + " et a Fedris art. 24 lois coordonnees - declaration"
                    + " immediate des qu'il y a soupcon legitime ; (vi)"
                    + " l'eventuelle option recursoire de l'employeur ou"
                    + " du tiers responsable art. 51 lois coordonnees"
                    + " (responsabilite extracontractuelle subsiste pour"
                    + " la partie du dommage non couverte par Fedris,"
                    + " action devant le tribunal civil du domicile du"
                    + " defendeur) ; (vii) la coordination avec le regime"
                    + " accidents du travail Loi du 10/04/1971 (un meme"
                    + " evenement ne peut etre qualifie a la fois MP et"
                    + " AT - choix par la victime art. 6 lois coordonnees) ;"
                    + " (viii) la prescription triennale art. 31 lois"
                    + " coordonnees - point de depart : date a laquelle"
                    + " la victime a eu connaissance du caractere"
                    + " professionnel de la maladie - causes d'interruption"
                    + " art. 2244 anc. C. civ. / 2.5.5 nouveau C. civ. et"
                    + " causes de suspension art. 2251 anc. C. civ. ;"
                    + " (ix) la voie de recours en cas de refus Fedris :"
                    + " tribunal du travail competence art. 580 1° C. jud."
                    + " ressort du domicile de la victime art. 627 C. jud.,"
                    + " delai 1 an a compter de la notification de la"
                    + " decision art. 23 lois coordonnees, requete"
                    + " contradictoire art. 1034bis C. jud. avec rapport"
                    + " medical certifie a peine d'irrecevabilite ; (x)"
                    + " la possibilite de saisine du tribunal du travail"
                    + " en designation d'expert medical contradictoire si"
                    + " contestation de l'evaluation Fedris ; (xi) la voie"
                    + " de cassation devant la Cour du travail puis Cass."
                    + " art. 580 et 109 C. jud. - moyen de cassation"
                    + " article precis lois coordonnees ou regle de droit"
                    + " medical europeen Reg. CE 1408/71 maintenu Reg. UE"
                    + " 883/2004 pour les travailleurs ayant exerce dans"
                    + " plusieurs Etats membres.";

    private MpFedrisReconnaissanceChecker() {
    }

    public static MpFedrisReconnaissanceResult analyze(
            MpFedrisReconnaissanceRequest request) {
        validateInputs(request);

        LocalDate dateRefPrescription = request.dateConnaissanceProfessionnel() != null
                ? request.dateConnaissanceProfessionnel()
                : request.dateConstatationMedicale();

        LocalDate datePrescription = dateRefPrescription.plusYears(PRESCRIPTION_ANS);

        LocalDate dateActionEffective = request.dateDeclarationEnvisagee() != null
                ? request.dateDeclarationEnvisagee()
                : LocalDate.now(ZONE_BRUSSELS);

        long joursAvantPrescription = ChronoUnit.DAYS.between(
                dateActionEffective, datePrescription);
        boolean prescriteAuJour = dateActionEffective.isAfter(datePrescription);

        // Priorite absolue : prescription
        if (prescriteAuJour) {
            return buildPrescrit(
                    request, datePrescription, joursAvantPrescription);
        }

        return switch (request.typeMaladie()) {
            case LISTE_FERMEE -> analyzeListeFermee(
                    request, datePrescription, joursAvantPrescription);
            case HORS_LISTE -> analyzeSystemeOuvert(
                    request, datePrescription, joursAvantPrescription);
        };
    }

    // == Branche LISTE_FERMEE ==

    private static MpFedrisReconnaissanceResult analyzeListeFermee(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        return switch (req.exposition()) {
            case AVEREE -> buildListeFermeePresomption(
                    req, datePrescription, joursAvantPrescription);
            case INSUFFISANTE -> buildListeFermeeExpositionInsuffisante(
                    req, datePrescription, joursAvantPrescription);
            case INDETERMINEE -> buildAQualifierListeFermee(
                    req, datePrescription, joursAvantPrescription);
        };
    }

    private static MpFedrisReconnaissanceResult buildListeFermeePresomption(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        String code = req.codeMaladieListe() != null && !req.codeMaladieListe().isBlank()
                ? req.codeMaladieListe() : "code non precise";
        String synthese = "La maladie declaree (" + req.libelleMaladie()
                + ", code liste " + code + ") figure sur la liste"
                + " fermee AR du 28/03/1969 modifie par l'AR du"
                + " 06/12/2018 et l'exposition professionnelle au risque"
                + " correspondant est avere durant la periode minimale"
                + " requise par la section concernee. La presomption"
                + " juridique de causalite art. 32 lois coordonnees"
                + " 03/06/1970 s'applique : Fedris est tenu de"
                + " reconnaitre la maladie professionnelle sauf a"
                + " renverser la presomption par la preuve contraire"
                + " (cas exceptionnel - Cass. BE 13/12/1989 Pas. 1990"
                + " I 451). Le delai d'action en reparation art. 31"
                + " (3 ans a compter de la connaissance du caractere"
                + " professionnel, date de reference "
                + getDateRefPrescription(req) + ") expire le "
                + datePrescription + " (" + joursAvantPrescription
                + " jour(s) restant(s) avant prescription). Etapes"
                + " procedurales : (1) constitution du dossier"
                + " formulaire 501 victime + 503 employeur + rapports"
                + " medicaux + CV professionnel documentant l'exposition,"
                + " (2) declaration immediate a Fedris art. 24 lois"
                + " coordonnees, (3) examen medical Fedris -"
                + " determination du taux d'incapacite permanente"
                + " partielle (% IPP), (4) calcul indemnites art. 34-37"
                + " (incapacite temporaire 90% remuneration moyenne,"
                + " incapacite permanente % IPP x remuneration de base"
                + " plafonnee, capital deces si applicable, frais"
                + " medicaux art. 31 octies), (5) en cas de refus :"
                + " recours tribunal du travail art. 580 1° C. jud.,"
                + " delai 1 an, requete contradictoire art. 1034bis"
                + " C. jud.";
        return new MpFedrisReconnaissanceResult(
                req.typeMaladie(), req.codeMaladieListe(), req.libelleMaladie(),
                req.dateConstatationMedicale(),
                req.dateConnaissanceProfessionnel(),
                req.dateDeclarationEnvisagee(),
                req.exposition(), req.causalite(),
                MpFedrisReconnaissanceVerdict.MALADIE_LISTE_FERMEE_PRESOMPTION,
                true, false,
                datePrescription, false, joursAvantPrescription,
                "LISTE_FERMEE_EXPOSITION_AVEREE_PRESOMPTION_ART_32",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static MpFedrisReconnaissanceResult buildListeFermeeExpositionInsuffisante(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        String synthese = "La maladie declaree (" + req.libelleMaladie()
                + ") figure sur la liste fermee AR du 28/03/1969 mais"
                + " l'exposition professionnelle au risque correspondant"
                + " n'est pas suffisamment documentee au regard des"
                + " conditions de duree minimale et d'intensite"
                + " specifiques a la section concernee. La presomption"
                + " art. 32 lois coordonnees 03/06/1970 ne joue pas en"
                + " l'etat. Pistes : (i) completer la documentation de"
                + " l'exposition (CV professionnel detaille, attestations"
                + " employeurs successifs, fiches de poste, mesures"
                + " d'ambiance bruit/poussieres/vibrations, registre"
                + " amiante art. 23 AR 16/03/2006 si exposition amiante,"
                + " carnet medical surveillance sante art. 27 AR"
                + " 28/05/2003 surveillance sante travailleurs) ; (ii)"
                + " basculer en systeme ouvert art. 30bis lois"
                + " coordonnees - charge de prouver le lien causal"
                + " direct et determinant - cette voie reste possible"
                + " meme pour les maladies listees lorsque l'exposition"
                + " ne remplit pas strictement les conditions de la"
                + " liste mais que la causalite est etablie par"
                + " expertise medicale ; (iii) deposer la declaration en"
                + " l'etat et provoquer la decision de refus Fedris"
                + " pour saisir le tribunal du travail art. 580 1°"
                + " C. jud., expertise medicale judiciaire art."
                + " 962-991 C. jud. Le delai d'action en reparation"
                + " art. 31 expire le " + datePrescription + " ("
                + joursAvantPrescription + " jour(s) restant(s) avant"
                + " prescription).";
        return new MpFedrisReconnaissanceResult(
                req.typeMaladie(), req.codeMaladieListe(), req.libelleMaladie(),
                req.dateConstatationMedicale(),
                req.dateConnaissanceProfessionnel(),
                req.dateDeclarationEnvisagee(),
                req.exposition(), req.causalite(),
                MpFedrisReconnaissanceVerdict.MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE,
                false, false,
                datePrescription, false, joursAvantPrescription,
                "LISTE_FERMEE_EXPOSITION_INSUFFISANTE_PRESOMPTION_BLOQUEE",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static MpFedrisReconnaissanceResult buildAQualifierListeFermee(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        String synthese = "La maladie declaree (" + req.libelleMaladie()
                + ") figure sur la liste fermee AR du 28/03/1969 mais"
                + " l'exposition professionnelle au risque correspondant"
                + " est INDETERMINEE en l'etat. L'avocat doit caracteriser"
                + " l'exposition par les elements du dossier (CV"
                + " professionnel, contrats de travail successifs, fiches"
                + " de poste, mesures d'ambiance, attestations"
                + " employeurs) avant de declencher la declaration"
                + " Fedris. La presomption art. 32 lois coordonnees"
                + " 03/06/1970 ne pourra jouer qu'apres caracterisation"
                + " de l'exposition au risque pendant la periode minimale"
                + " requise par la section concernee de la liste. Le"
                + " delai d'action en reparation art. 31 expire le "
                + datePrescription + " (" + joursAvantPrescription
                + " jour(s) restant(s) avant prescription) - ne pas"
                + " differer la declaration en cas de doute prescription.";
        return new MpFedrisReconnaissanceResult(
                req.typeMaladie(), req.codeMaladieListe(), req.libelleMaladie(),
                req.dateConstatationMedicale(),
                req.dateConnaissanceProfessionnel(),
                req.dateDeclarationEnvisagee(),
                req.exposition(), req.causalite(),
                MpFedrisReconnaissanceVerdict.A_QUALIFIER,
                false, false,
                datePrescription, false, joursAvantPrescription,
                "LISTE_FERMEE_EXPOSITION_INDETERMINEE",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    // == Branche SYSTEME_OUVERT ==

    private static MpFedrisReconnaissanceResult analyzeSystemeOuvert(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        return switch (req.causalite()) {
            case DIRECTE_DETERMINANTE -> buildSystemeOuvertCausaliteEtablie(
                    req, datePrescription, joursAvantPrescription);
            case INSUFFISANTE -> buildSystemeOuvertCausaliteInsuffisante(
                    req, datePrescription, joursAvantPrescription);
            case INDETERMINEE, NON_APPLICABLE ->
                    buildAQualifierSystemeOuvert(
                            req, datePrescription, joursAvantPrescription);
        };
    }

    private static MpFedrisReconnaissanceResult buildSystemeOuvertCausaliteEtablie(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        String synthese = "La maladie declaree (" + req.libelleMaladie()
                + ") n'est pas inscrite sur la liste fermee AR du"
                + " 28/03/1969 mais le dossier presente des elements"
                + " medicaux et professionnels concordants etablissant"
                + " un lien de causalite DIRECT et DETERMINANT avec"
                + " l'exposition professionnelle au sens de l'art."
                + " 30bis lois coordonnees 03/06/1970 (systeme ouvert"
                + " introduit par la Loi du 29/12/1990) et conforme au"
                + " standard jurisprudentiel Cass. BE 28/05/2008"
                + " S.07.0033.F (l'exposition professionnelle est la"
                + " cause directe et essentielle, pas un simple facteur"
                + " favorisant parmi d'autres). Pas de presomption art."
                + " 32 - la charge de la preuve incombe a la victime"
                + " mais reste supportable des lors que l'expertise"
                + " medicale est concordante. Etapes procedurales : (1)"
                + " constitution dossier formulaire 501 + 503 + rapports"
                + " medicaux specialistes + CV professionnel + analyse"
                + " d'exposition + bibliographie scientifique etablissant"
                + " la relation dose-effet ; (2) declaration Fedris art."
                + " 24 lois coordonnees ; (3) expertise medicale Fedris"
                + " contradictoire avec le medecin traitant de la victime ;"
                + " (4) en cas de refus : recours tribunal du travail art."
                + " 580 1° C. jud., expertise medicale judiciaire art."
                + " 962-991 C. jud. avec mission specifique sur la"
                + " causalite directe et determinante. Le delai d'action"
                + " en reparation art. 31 expire le " + datePrescription
                + " (" + joursAvantPrescription + " jour(s) restant(s)"
                + " avant prescription).";
        return new MpFedrisReconnaissanceResult(
                req.typeMaladie(), req.codeMaladieListe(), req.libelleMaladie(),
                req.dateConstatationMedicale(),
                req.dateConnaissanceProfessionnel(),
                req.dateDeclarationEnvisagee(),
                req.exposition(), req.causalite(),
                MpFedrisReconnaissanceVerdict.SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE,
                false, true,
                datePrescription, false, joursAvantPrescription,
                "SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE_ART_30BIS",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static MpFedrisReconnaissanceResult buildSystemeOuvertCausaliteInsuffisante(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        String synthese = "La maladie declaree (" + req.libelleMaladie()
                + ") n'est pas inscrite sur la liste fermee AR du"
                + " 28/03/1969 et le lien causal direct et determinant"
                + " avec l'exposition professionnelle n'est pas etabli"
                + " au standard requis par l'art. 30bis lois coordonnees"
                + " 03/06/1970 (Cass. BE 28/05/2008 S.07.0033.F - simple"
                + " facteur favorisant insuffisant ; Cass. BE 04/02/2002"
                + " S.01.0095.F - caractere direct et essentiel exige)."
                + " Pronostic Fedris : refus probable. Pistes : (i)"
                + " renforcer l'expertise medicale par un specialiste"
                + " (medecin du travail INAMI, pneumologue,"
                + " rhumatologue, neurologue selon pathologie) ; (ii)"
                + " documenter precisement l'exposition professionnelle"
                + " et exclure les facteurs personnels predominants"
                + " (antecedents medicaux, mode de vie) ; (iii) si le"
                + " refus Fedris est inevitable, le provoquer rapidement"
                + " pour saisir le tribunal du travail art. 580 1°"
                + " C. jud., delai 1 an art. 23 lois coordonnees,"
                + " expertise judiciaire art. 962-991 C. jud. avec"
                + " mission contradictoire renforcee. Le delai d'action"
                + " en reparation art. 31 expire le " + datePrescription
                + " (" + joursAvantPrescription + " jour(s) restant(s)"
                + " avant prescription).";
        return new MpFedrisReconnaissanceResult(
                req.typeMaladie(), req.codeMaladieListe(), req.libelleMaladie(),
                req.dateConstatationMedicale(),
                req.dateConnaissanceProfessionnel(),
                req.dateDeclarationEnvisagee(),
                req.exposition(), req.causalite(),
                MpFedrisReconnaissanceVerdict.SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE,
                false, true,
                datePrescription, false, joursAvantPrescription,
                "SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static MpFedrisReconnaissanceResult buildAQualifierSystemeOuvert(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        String synthese = "La maladie declaree (" + req.libelleMaladie()
                + ") n'est pas inscrite sur la liste fermee AR du"
                + " 28/03/1969. L'analyse en systeme ouvert art. 30bis"
                + " lois coordonnees 03/06/1970 requiert la caracterisation"
                + " du lien de causalite directe et determinante (Cass."
                + " BE 28/05/2008 S.07.0033.F) - en l'etat la causalite"
                + " est INDETERMINEE ou non applicable au scenario. L'avocat"
                + " doit reunir : (i) rapports medicaux d'un specialiste"
                + " etablissant la relation entre la pathologie et le"
                + " type d'exposition documente ; (ii) analyse"
                + " d'exposition professionnelle quantitative (mesures"
                + " d'ambiance, fiches de poste, registres d'exposition) ;"
                + " (iii) bibliographie scientifique etablissant la"
                + " relation dose-effet entre le risque et la pathologie ;"
                + " (iv) absence de facteurs personnels predominants"
                + " (antecedents medicaux, mode de vie, comorbidites)"
                + " avant de declencher la declaration Fedris. Le delai"
                + " d'action en reparation art. 31 expire le "
                + datePrescription + " (" + joursAvantPrescription
                + " jour(s) restant(s) avant prescription).";
        return new MpFedrisReconnaissanceResult(
                req.typeMaladie(), req.codeMaladieListe(), req.libelleMaladie(),
                req.dateConstatationMedicale(),
                req.dateConnaissanceProfessionnel(),
                req.dateDeclarationEnvisagee(),
                req.exposition(), req.causalite(),
                MpFedrisReconnaissanceVerdict.A_QUALIFIER,
                false, true,
                datePrescription, false, joursAvantPrescription,
                "SYSTEME_OUVERT_CAUSALITE_INDETERMINEE",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    // == Branche PRESCRIPTION ==

    private static MpFedrisReconnaissanceResult buildPrescrit(
            MpFedrisReconnaissanceRequest req,
            LocalDate datePrescription,
            long joursAvantPrescription) {
        long joursDepasses = -joursAvantPrescription;
        String synthese = "Le delai de prescription triennale de l'action"
                + " en reparation art. 31 lois coordonnees 03/06/1970"
                + " est dépasse : 3 ans a compter de la date de"
                + " connaissance du caractere professionnel ("
                + getDateRefPrescription(req) + "), expirant le "
                + datePrescription + ", " + joursDepasses + " jour(s)"
                + " ecoule(s) depuis l'expiration. L'action en reparation"
                + " Fedris est en principe eteinte. L'avocat doit"
                + " verifier : (i) la date exacte de connaissance du"
                + " caractere professionnel (point de depart de la"
                + " prescription, distinct de la date de premier"
                + " constatation medicale - jurisprudence constante :"
                + " la connaissance du caractere professionnel suppose"
                + " que la victime ait ete informee du lien possible"
                + " entre sa pathologie et son exposition par un medecin"
                + " ou par Fedris) ; (ii) les causes d'interruption de"
                + " prescription art. 2244 anc. C. civ. / 2.5.5 nouveau"
                + " C. civ. (citation en justice, commandement,"
                + " reconnaissance de dette par Fedris, conciliation) ;"
                + " (iii) les causes de suspension art. 2251 anc. C. civ."
                + " (mineurs, interdits, suspension legale - art. 2252"
                + " ; (iv) la theorie de la decouverte progressive de"
                + " l'aggravation - la prescription peut courir a partir"
                + " de chaque aggravation distincte si elle est"
                + " medicalement documentee comme une evolution distincte"
                + " et non prévisible (Cass. BE 24/04/1995 Pas. 1995"
                + " I 429) ; (v) la voie residuelle de l'action en"
                + " responsabilite civile de droit commun art. 1382"
                + " anc. C. civ. / 5.83 nouveau C. civ. contre"
                + " l'employeur ou un tiers responsable (prescription 5"
                + " ans art. 2262bis anc. C. civ., 5 ans art. 5.149"
                + " nouveau C. civ. depuis 01/01/2023), action devant le"
                + " tribunal civil du domicile du defendeur, en"
                + " complement de la reparation forfaitaire Fedris"
                + " art. 51 lois coordonnees.";
        return new MpFedrisReconnaissanceResult(
                req.typeMaladie(), req.codeMaladieListe(), req.libelleMaladie(),
                req.dateConstatationMedicale(),
                req.dateConnaissanceProfessionnel(),
                req.dateDeclarationEnvisagee(),
                req.exposition(), req.causalite(),
                MpFedrisReconnaissanceVerdict.DECLARATION_PRESCRITE,
                false, false,
                datePrescription, true, joursAvantPrescription,
                "PRESCRIPTION_TRIENNALE_ART_31_LOIS_COORDONNEES_DEPASSEE",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    // == Helpers ==

    private static LocalDate getDateRefPrescription(MpFedrisReconnaissanceRequest req) {
        return req.dateConnaissanceProfessionnel() != null
                ? req.dateConnaissanceProfessionnel()
                : req.dateConstatationMedicale();
    }

    private static void validateInputs(MpFedrisReconnaissanceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requete requise");
        }
        if (request.typeMaladie() == null) {
            throw new IllegalArgumentException("typeMaladie est requis");
        }
        if (request.libelleMaladie() == null || request.libelleMaladie().isBlank()) {
            throw new IllegalArgumentException("libelleMaladie est requis");
        }
        if (request.dateConstatationMedicale() == null) {
            throw new IllegalArgumentException(
                    "dateConstatationMedicale est requis");
        }
        if (request.exposition() == null) {
            throw new IllegalArgumentException("exposition est requis");
        }
        if (request.causalite() == null) {
            throw new IllegalArgumentException("causalite est requis");
        }
        // Coherence des dates : connaissance professionnel >= constatation medicale
        if (request.dateConnaissanceProfessionnel() != null
                && request.dateConnaissanceProfessionnel().isBefore(
                        request.dateConstatationMedicale())) {
            throw new IllegalArgumentException(
                    "dateConnaissanceProfessionnel doit etre posterieure"
                            + " ou egale a dateConstatationMedicale");
        }
        // Coherence : si typeMaladie = LISTE_FERMEE, codeMaladieListe doit etre fourni
        if (request.typeMaladie() == MpFedrisReconnaissanceTypeMaladie.LISTE_FERMEE
                && (request.codeMaladieListe() == null
                        || request.codeMaladieListe().isBlank())) {
            throw new IllegalArgumentException(
                    "codeMaladieListe est requis lorsque typeMaladie ="
                            + " LISTE_FERMEE");
        }
    }
}

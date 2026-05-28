package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-219-22 : vérificateur de conformité de l'obligation <i>égalité
 * salariale femmes/hommes BE</i> (Loi du 22/04/2012 visant à lutter
 * contre l'écart salarial entre hommes et femmes M.B. 28/08/2012 +
 * AR du 17/08/2013 portant exécution de l'art. 2 + AR du 25/04/2014
 * modifiant les formulaires de rapport d'analyse + CCT n° 25 du
 * 15/10/1975 du CNT sur l'égalité de rémunération entre travailleurs
 * masculins et féminins) — fonction <b>pure</b> (sans dépendance HTTP
 * / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 22/04/2012</b> visant à lutter contre l'écart
 *       salarial entre hommes et femmes (M.B. 28/08/2012) — pose le
 *       cadre national : obligation pour les employeurs occupant en
 *       moyenne ≥ 50 travailleurs d'établir tous les deux exercices
 *       comptables un rapport d'analyse de la structure de rémunération
 *       H/F (art. 2), de mettre en place un plan d'action si l'analyse
 *       révèle un écart non justifié (art. 5), faculté de désigner un
 *       médiateur en interne (art. 14), et interdit la discrimination
 *       salariale (art. 12).</li>
 *   <li><b>AR du 17/08/2013</b> portant exécution de l'art. 2 Loi
 *       22/04/2012 — détaille le contenu obligatoire de l'analyse
 *       (art. 4 : ventilation par niveau de fonction, ancienneté,
 *       qualification, régime de travail, composants de la
 *       rémunération).</li>
 *   <li><b>AR du 25/04/2014</b> modifiant l'AR du 17/08/2013 — fixe
 *       les formulaires standardisés : ann. I pour les entreprises
 *       ≥ 100 ETP (formulaire détaillé), ann. II pour les entreprises
 *       50 à 99 ETP (formulaire simplifié). Dépôt CE / DS + dépôt
 *       central électronique EBES auprès de la Banque nationale.</li>
 *   <li><b>CCT n° 25 du 15/10/1975</b> du CNT (NAR) concernant
 *       l'égalité de rémunération entre travailleurs masculins et
 *       féminins (rendue obligatoire par AR 09/12/1975 M.B.
 *       09/01/1976), modifiée par CCT n° 25bis du 19/12/2001 et
 *       n° 25ter du 09/07/2008 — socle conventionnel transposant
 *       Directive 75/117/CEE puis 2006/54/CE.</li>
 *   <li><b>Loi du 10/05/2007</b> tendant à lutter contre certaines
 *       formes de discrimination (M.B. 30/05/2007) — cadre général
 *       anti-discrimination genre (art. 14 et s.) en complément du
 *       cadre salarial spécifique.</li>
 *   <li><b>Loi du 12/01/2007</b> visant au contrôle de l'application
 *       des résolutions de la Conférence mondiale sur les femmes
 *       (gender mainstreaming) — obligation transversale d'intégrer
 *       la dimension de genre dans toutes les politiques publiques.</li>
 *   <li><b>C. pén. social du 06/06/2010</b>, art. 195/1 (inséré par
 *       Loi 22/04/2012 art. 16) — sanction de niveau 2 (amende
 *       administrative 80 à 800 € ou pénale 200 à 2 000 € par
 *       travailleur, plafonnée) en cas de manquement à l'obligation
 *       rapport d'analyse.</li>
 *   <li><b>Loi du 04/12/2007</b> sur les élections sociales, art. 7
 *       — méthode de calcul de l'effectif moyen (ETP, période de
 *       référence, sociétés liées) applicable au seuil de 50
 *       travailleurs.</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts (court-circuits successifs)</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — statut rapport
 *       {@link EgaliteFemmesHommesBeStatutRapport#INDETERMINE} ou
 *       {@link EgaliteFemmesHommesBeStatutRapport#EN_PREPARATION}.</li>
 *   <li><b>HORS_CHAMP_SEUIL_NON_ATTEINT</b> — effectif &lt; 50
 *       travailleurs : obligation rapport biennal non applicable.</li>
 *   <li><b>MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE</b> — délai dépassé,
 *       aucun rapport.</li>
 *   <li><b>NON_CONFORME_RAPPORT_INCOMPLET</b> — rapport déposé mais
 *       sections manquantes (ventilation art. 4 AR 17/08/2013).</li>
 *   <li><b>NON_CONFORME_PLAN_ACTION_MANQUANT</b> — rapport complet,
 *       écart constaté, mais plan d'action non établi.</li>
 *   <li>Sinon → <b>CONFORME_RAPPORT_PLAN_COMPLETS</b>.</li>
 * </ol>
 */
public final class EgaliteFemmesHommesBeChecker {

    /** Seuil d'effectif déclenchant l'obligation rapport (art. 2 Loi 22/04/2012). */
    static final int SEUIL_EFFECTIF_DECLENCHEUR = 50;

    /** Seuil distinguant le formulaire détaillé (ann. I) du simplifié (ann. II). */
    static final int SEUIL_FORMULAIRE_DETAILLE = 100;

    static final String FORMULAIRE_DETAILLE = "ANNEXE_I_DETAILLE";
    static final String FORMULAIRE_SIMPLIFIE = "ANNEXE_II_SIMPLIFIE";
    static final String FORMULAIRE_NON_APPLICABLE = "NON_APPLICABLE";

    static final String BASE_JURIDIQUE =
            "Loi du 22/04/2012 visant à lutter contre l'écart salarial"
                    + " entre hommes et femmes (M.B. 28/08/2012), art. 2"
                    + " (obligation pour les employeurs occupant en moyenne"
                    + " au moins 50 travailleurs d'établir tous les deux"
                    + " exercices comptables un rapport d'analyse de la"
                    + " structure de rémunération H/F), art. 5 (plan"
                    + " d'action si écart non justifié), art. 12"
                    + " (interdiction discrimination salariale), art. 14"
                    + " (faculté médiateur dans l'entreprise) ; AR du"
                    + " 17/08/2013 portant exécution de l'art. 2 Loi"
                    + " 22/04/2012 (art. 4 — contenu obligatoire de"
                    + " l'analyse : ventilation par niveau de fonction,"
                    + " ancienneté, qualification, régime de travail,"
                    + " composants de la rémunération) ; AR du 25/04/2014"
                    + " modifiant l'AR du 17/08/2013 (formulaires"
                    + " standardisés : ann. I détaillé ≥ 100 ETP, ann. II"
                    + " simplifié 50-99 ETP ; dépôt CE / DS + dépôt"
                    + " central électronique EBES auprès de la BNB) ;"
                    + " CCT n° 25 du 15/10/1975 du CNT (NAR) sur"
                    + " l'égalité de rémunération entre travailleurs"
                    + " masculins et féminins, rendue obligatoire par AR"
                    + " 09/12/1975 (M.B. 09/01/1976), modifiée par CCT"
                    + " n° 25bis du 19/12/2001 et n° 25ter du 09/07/2008"
                    + " — transpose Directive 75/117/CEE puis 2006/54/CE ;"
                    + " Loi du 10/05/2007 tendant à lutter contre"
                    + " certaines formes de discrimination (M.B."
                    + " 30/05/2007), art. 14 et s. — cadre général"
                    + " anti-discrimination genre ; Loi du 12/01/2007"
                    + " gender mainstreaming ; C. pén. social du"
                    + " 06/06/2010, art. 195/1 (inséré par Loi"
                    + " 22/04/2012 art. 16) — sanction de niveau 2 en"
                    + " cas de manquement à l'obligation rapport ;"
                    + " Loi du 04/12/2007 sur les élections sociales,"
                    + " art. 7 (méthode de calcul de l'effectif moyen"
                    + " ETP applicable au seuil de 50 travailleurs).";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'analyse de la conformité égalité salariale H/F"
                    + " BE produit un verdict indicatif fondé sur les"
                    + " déclarations de l'avocat. L'avocat spécialisé en"
                    + " droit social BE doit confirmer pour la situation"
                    + " considérée : (i) le décompte exact de l'effectif"
                    + " moyen sur la période de référence biennale au"
                    + " sens de la Loi 04/12/2007 sur les élections"
                    + " sociales, art. 7 (équivalents temps plein, mois"
                    + " incomplets, intérimaires, sociétés liées) — un"
                    + " effectif autour des seuils de 50 ou 100 mérite"
                    + " un audit complet ; (ii) le formulaire applicable"
                    + " (AR 25/04/2014 ann. I détaillé ≥ 100 ETP versus"
                    + " ann. II simplifié 50-99 ETP) et la procédure de"
                    + " dépôt (CE en priorité, DS à défaut, dépôt central"
                    + " électronique EBES BNB) ; (iii) le caractère"
                    + " complet de la ventilation art. 4 AR 17/08/2013"
                    + " (niveau de fonction, ancienneté, qualification,"
                    + " régime de travail, composants de rémunération —"
                    + " salaire de base, primes, avantages complémentaires) ;"
                    + " (iv) la qualification objective ou non d'un écart"
                    + " constaté (art. 5 Loi 22/04/2012 — critères de"
                    + " justification objective stricte selon"
                    + " jurisprudence CJUE Bilka, Cadman) ; (v) la"
                    + " forme et le contenu d'un plan d'action conforme"
                    + " (objectifs chiffrés, calendrier de mise en œuvre,"
                    + " suivi annuel au CE) ; (vi) l'opportunité de"
                    + " désigner un médiateur égalité H/F (art. 14 Loi"
                    + " 22/04/2012 + CCT n° 25) en prévention des"
                    + " plaintes individuelles ; (vii) le risque de"
                    + " sanction pénale niveau 2 (C. pén. social"
                    + " art. 195/1) et l'action en cessation pouvant"
                    + " être introduite par les organisations syndicales,"
                    + " par l'Institut pour l'égalité des femmes et des"
                    + " hommes (IEFH) ou par tout intéressé ;"
                    + " (viii) l'opportunité pour la travailleuse"
                    + " individuelle de fonder une action en"
                    + " responsabilité civile contractuelle (art. 5.83"
                    + " nouveau C. civ.) ou une action en rectification"
                    + " salariale avec arriérés sur la période non"
                    + " prescrite (Loi 03/07/1978 art. 15 : 1 an /"
                    + " 5 ans selon nature, art. 26 délai de droit"
                    + " commun) — Cass. BE 22/01/2018 S.16.0094.F"
                    + " confirme le caractère contractuel de l'égalité"
                    + " de rémunération.";

    private EgaliteFemmesHommesBeChecker() {
    }

    public static EgaliteFemmesHommesBeResult analyze(
            EgaliteFemmesHommesBeRequest request) {
        validateInputs(request);

        boolean seuilAtteint =
                request.effectifEntreprise() >= SEUIL_EFFECTIF_DECLENCHEUR;
        boolean rapportDepose =
                request.statutRapport()
                        == EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_COMPLET
                        || request.statutRapport()
                        == EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_INCOMPLET;
        boolean ventilationComplete =
                request.ventilationNiveauFonctionFournie()
                        && request.ventilationAncienneteFournie()
                        && request.ventilationQualificationFournie()
                        && request.ventilationRegimeTravailFournie()
                        && request.ventilationComposantsRemunerationFournie();
        boolean planActionRequis = request.ecartSalarialNonJustifieConstate();
        boolean planActionConforme = !planActionRequis
                || request.planActionEtabli();

        String typeFormulaireRequis = seuilAtteint
                ? (request.effectifEntreprise() >= SEUIL_FORMULAIRE_DETAILLE
                        ? FORMULAIRE_DETAILLE : FORMULAIRE_SIMPLIFIE)
                : FORMULAIRE_NON_APPLICABLE;

        EgaliteFemmesHommesBeStatutRapport statut = request.statutRapport();

        // ── Hiérarchie 1 : statut indéterminé / en préparation → A_ANALYSER ─
        if (statut == EgaliteFemmesHommesBeStatutRapport.INDETERMINE) {
            String synthese = "Statut du rapport d'analyse égalité"
                    + " salariale H/F indéterminé : pas encore"
                    + " d'instruction sur l'existence d'un rapport"
                    + " biennal déposé au CE / DS. Une analyse au cas"
                    + " par cas est requise pour qualifier la situation"
                    + " au regard de l'art. 2 Loi du 22/04/2012.";
            return build(request,
                    EgaliteFemmesHommesBeVerdict.A_ANALYSER,
                    seuilAtteint, rapportDepose, ventilationComplete,
                    planActionRequis, planActionConforme,
                    typeFormulaireRequis,
                    "STATUT_RAPPORT_INDETERMINE",
                    synthese);
        }

        if (statut == EgaliteFemmesHommesBeStatutRapport.EN_PREPARATION) {
            String synthese = "L'exercice biennal est en cours et le"
                    + " rapport d'analyse égalité salariale H/F n'a"
                    + " pas encore été déposé, mais la collecte des"
                    + " données est engagée dans les délais (rapport"
                    + " dû dans les 3 mois de la clôture de l'exercice"
                    + " comptable suivant la période biennale). L'avocat"
                    + " confirmera l'échéancier précis applicable à"
                    + " l'entreprise (date de clôture, exercice de"
                    + " référence) et la complétude du brouillon de"
                    + " ventilation art. 4 AR 17/08/2013 avant le dépôt"
                    + " formel au CE / DS.";
            return build(request,
                    EgaliteFemmesHommesBeVerdict.A_ANALYSER,
                    seuilAtteint, false, ventilationComplete,
                    planActionRequis, planActionConforme,
                    typeFormulaireRequis,
                    "RAPPORT_EN_PREPARATION",
                    synthese);
        }

        // ── Hiérarchie 2 : seuil non atteint ────────────────────────────────
        if (!seuilAtteint) {
            String synthese = "L'entreprise occupe en moyenne "
                    + request.effectifEntreprise() + " travailleur(s),"
                    + " ce qui est inférieur au seuil de "
                    + SEUIL_EFFECTIF_DECLENCHEUR + " déclenchant"
                    + " l'obligation art. 2 § 1er Loi du 22/04/2012."
                    + " L'employeur n'est pas tenu d'établir un rapport"
                    + " biennal d'analyse de la structure de rémunération"
                    + " H/F. L'interdiction de discrimination salariale"
                    + " art. 12 reste pleinement applicable (CCT n° 25 du"
                    + " 15/10/1975 + Loi 10/05/2007 anti-discrimination)"
                    + " et toute travailleuse peut introduire une action"
                    + " individuelle en rectification salariale.";
            return build(request,
                    EgaliteFemmesHommesBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT,
                    false, rapportDepose, ventilationComplete,
                    planActionRequis, planActionConforme,
                    typeFormulaireRequis,
                    "EFFECTIF_INFERIEUR_SEUIL",
                    synthese);
        }

        // ── Hiérarchie 3 : délai dépassé sans rapport ───────────────────────
        if (statut == EgaliteFemmesHommesBeStatutRapport.NON_DEPOSE_DELAI_DEPASSE) {
            String synthese = "L'entreprise occupe en moyenne "
                    + request.effectifEntreprise() + " travailleurs"
                    + " (≥ " + SEUIL_EFFECTIF_DECLENCHEUR + "), le"
                    + " formulaire applicable est le " + typeFormulaireRequis
                    + " (AR 25/04/2014). Aucun rapport d'analyse n'a"
                    + " été déposé alors que le délai est dépassé."
                    + " Manquement frontal à l'art. 2 Loi 22/04/2012"
                    + " exposant l'employeur à la sanction pénale de"
                    + " niveau 2 (C. pén. social art. 195/1 — amende"
                    + " administrative 80 à 800 € ou pénale 200 à"
                    + " 2 000 € par travailleur, plafonnée), à une"
                    + " action en cessation par les organisations"
                    + " syndicales, par l'Institut pour l'égalité des"
                    + " femmes et des hommes (IEFH) ou par tout intéressé,"
                    + " et au risque réputationnel d'un signalement"
                    + " public via le dépôt central EBES manquant à la"
                    + " BNB. Les travailleuses individuelles peuvent"
                    + " fonder une action en responsabilité civile"
                    + " contractuelle (art. 5.83 nouveau C. civ.) ou"
                    + " une action en rectification salariale avec"
                    + " arriérés sur la période non prescrite (Loi"
                    + " 03/07/1978 art. 15 : 1 an / 5 ans selon"
                    + " nature).";
            return build(request,
                    EgaliteFemmesHommesBeVerdict.MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE,
                    true, false, false,
                    planActionRequis, planActionConforme,
                    typeFormulaireRequis,
                    "RAPPORT_NON_DEPOSE",
                    synthese);
        }

        // ── Hiérarchie 4 : rapport déposé mais ventilation incomplète ───────
        if (statut == EgaliteFemmesHommesBeStatutRapport.DEPOSE_FORMULAIRE_INCOMPLET
                || !ventilationComplete) {
            StringBuilder manques = new StringBuilder();
            if (!request.ventilationNiveauFonctionFournie()) {
                manques.append(" niveau de fonction (classification"
                        + " conventionnelle),");
            }
            if (!request.ventilationAncienneteFournie()) {
                manques.append(" ancienneté,");
            }
            if (!request.ventilationQualificationFournie()) {
                manques.append(" niveau de qualification,");
            }
            if (!request.ventilationRegimeTravailFournie()) {
                manques.append(" régime de travail (temps plein /"
                        + " temps partiel),");
            }
            if (!request.ventilationComposantsRemunerationFournie()) {
                manques.append(" composants de la rémunération (salaire"
                        + " de base, primes, avantages complémentaires),");
            }
            String manquants = manques.length() > 0
                    ? manques.substring(0, manques.length() - 1)
                    : " sections déclarées incomplètes côté avocat";
            String synthese = "Le rapport d'analyse égalité salariale"
                    + " H/F a été déposé sur le formulaire "
                    + typeFormulaireRequis + " (AR 25/04/2014) mais ne"
                    + " ventile pas l'ensemble des sections obligatoires"
                    + " art. 4 AR 17/08/2013. Manque(nt) :" + manquants
                    + ". Le SPF Emploi peut considérer l'obligation"
                    + " comme partiellement remplie et imposer la"
                    + " régularisation par injonction (C. pén. social"
                    + " art. 195/1) ; le CE / DS peut refuser de"
                    + " prendre acte du rapport et exiger un avenant"
                    + " correctif. L'avocat invite à compléter le"
                    + " rapport par avenant déposé dans le mois suivant"
                    + " la première constatation, en prévention d'une"
                    + " requalification du dépôt en non-dépôt.";
            return build(request,
                    EgaliteFemmesHommesBeVerdict.NON_CONFORME_RAPPORT_INCOMPLET,
                    true, true, false,
                    planActionRequis, planActionConforme,
                    typeFormulaireRequis,
                    "VENTILATION_INCOMPLETE",
                    synthese);
        }

        // ── Hiérarchie 5 : rapport complet mais plan d'action manquant ──────
        if (planActionRequis && !request.planActionEtabli()) {
            BigDecimal pct = request.pourcentageEcartConstate();
            String ecart = (pct != null)
                    ? " (écart de l'ordre de " + pct.stripTrailingZeros().toPlainString() + " %)"
                    : "";
            String synthese = "Le rapport d'analyse égalité salariale"
                    + " H/F est complet et ventilé conformément à"
                    + " l'art. 4 AR 17/08/2013, mais il révèle un"
                    + " écart de rémunération non justifié" + ecart
                    + " sans qu'un plan d'action concret avec objectifs"
                    + " chiffrés et calendrier ait été établi dans le"
                    + " délai imposé (CCT sectorielle ou à défaut 6"
                    + " mois — art. 5 Loi 22/04/2012). Manquement"
                    + " substantiel : l'obligation rapport ne se réduit"
                    + " pas au constat, elle inclut le remède."
                    + " L'employeur s'expose à la sanction C. pén. social"
                    + " art. 195/1 (niveau 2) et à une action en"
                    + " cessation. L'avocat conseille d'établir sans"
                    + " délai un plan formel (objectifs chiffrés"
                    + " d'alignement, calendrier de mise en œuvre"
                    + " ≤ 3 ans, suivi annuel au CE) en concertation"
                    + " avec les partenaires sociaux et, le cas"
                    + " échéant, le médiateur égalité H/F (art. 14 Loi"
                    + " 22/04/2012 + CCT n° 25).";
            return build(request,
                    EgaliteFemmesHommesBeVerdict.NON_CONFORME_PLAN_ACTION_MANQUANT,
                    true, true, true,
                    true, false,
                    typeFormulaireRequis,
                    "PLAN_ACTION_MANQUANT",
                    synthese);
        }

        // ── Cas qualifiant : rapport et plan complets ───────────────────────
        StringBuilder avertSb = new StringBuilder();
        if (!request.mediateurDesigne()) {
            avertSb.append(" La désignation d'un médiateur égalité"
                    + " H/F (art. 14 Loi 22/04/2012 + CCT n° 25) reste"
                    + " une bonne pratique recommandée pour la"
                    + " prévention et le traitement non contentieux des"
                    + " plaintes individuelles.");
        }
        if (request.plainteIefhOuInspectionEnCours()) {
            avertSb.append(" Une plainte IEFH ou un contrôle SPF"
                    + " Emploi étant en cours, l'avocat veillera à"
                    + " constituer un dossier de preuves démontrant"
                    + " l'objectivité des critères d'écarts persistants"
                    + " (justifications objectives strictes au sens de"
                    + " la CJUE — arrêts Bilka, Cadman, Brunnhofer).");
        }
        String ecartPart = planActionRequis
                ? " révèle un écart non justifié pour lequel un plan"
                + " d'action concret a été établi conformément à"
                + " l'art. 5 Loi 22/04/2012"
                : " ne révèle pas d'écart non justifié au sens de"
                + " l'art. 5 Loi 22/04/2012";
        String synthese = "L'entreprise occupe en moyenne "
                + request.effectifEntreprise() + " travailleurs et a"
                + " déposé un rapport d'analyse égalité salariale H/F"
                + " complet sur le formulaire " + typeFormulaireRequis
                + " (AR 25/04/2014)"
                + (request.dateDepotRapport() != null
                        ? " (dépôt " + request.dateDepotRapport() + ")" : "")
                + ". L'analyse" + ecartPart + ". Les cinq sections de"
                + " ventilation obligatoires art. 4 AR 17/08/2013 sont"
                + " traitées (niveau de fonction, ancienneté,"
                + " qualification, régime de travail, composants de"
                + " rémunération). Conformité globale à l'obligation"
                + " rapport biennal art. 2 Loi 22/04/2012."
                + avertSb.toString();
        return build(request,
                EgaliteFemmesHommesBeVerdict.CONFORME_RAPPORT_PLAN_COMPLETS,
                true, true, true,
                planActionRequis, planActionConforme,
                typeFormulaireRequis,
                "CONFORME_RAPPORT_PLAN_COMPLETS",
                synthese);
    }

    private static EgaliteFemmesHommesBeResult build(
            EgaliteFemmesHommesBeRequest request,
            EgaliteFemmesHommesBeVerdict verdict,
            boolean seuilAtteint,
            boolean rapportDepose,
            boolean ventilationComplete,
            boolean planActionRequis,
            boolean planActionConforme,
            String typeFormulaireRequis,
            String raison,
            String synthese) {
        return new EgaliteFemmesHommesBeResult(
                request.effectifEntreprise(),
                request.statutRapport(),
                request.dateDepotRapport(),
                request.ventilationNiveauFonctionFournie(),
                request.ventilationAncienneteFournie(),
                request.ventilationQualificationFournie(),
                request.ventilationRegimeTravailFournie(),
                request.ventilationComposantsRemunerationFournie(),
                request.ecartSalarialNonJustifieConstate(),
                request.pourcentageEcartConstate(),
                request.planActionEtabli(),
                request.mediateurDesigne(),
                request.plainteIefhOuInspectionEnCours(),
                verdict,
                seuilAtteint,
                rapportDepose,
                ventilationComplete,
                planActionRequis,
                planActionConforme,
                typeFormulaireRequis,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(EgaliteFemmesHommesBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.effectifEntreprise() == null) {
            throw new IllegalArgumentException(
                    "effectifEntreprise est requis");
        }
        if (request.effectifEntreprise() < 0) {
            throw new IllegalArgumentException(
                    "effectifEntreprise doit être positif ou nul");
        }
        if (request.statutRapport() == null) {
            throw new IllegalArgumentException(
                    "statutRapport est requis");
        }
        if (request.ventilationNiveauFonctionFournie() == null) {
            throw new IllegalArgumentException(
                    "ventilationNiveauFonctionFournie est requis");
        }
        if (request.ventilationAncienneteFournie() == null) {
            throw new IllegalArgumentException(
                    "ventilationAncienneteFournie est requis");
        }
        if (request.ventilationQualificationFournie() == null) {
            throw new IllegalArgumentException(
                    "ventilationQualificationFournie est requis");
        }
        if (request.ventilationRegimeTravailFournie() == null) {
            throw new IllegalArgumentException(
                    "ventilationRegimeTravailFournie est requis");
        }
        if (request.ventilationComposantsRemunerationFournie() == null) {
            throw new IllegalArgumentException(
                    "ventilationComposantsRemunerationFournie est requis");
        }
        if (request.ecartSalarialNonJustifieConstate() == null) {
            throw new IllegalArgumentException(
                    "ecartSalarialNonJustifieConstate est requis");
        }
        if (request.planActionEtabli() == null) {
            throw new IllegalArgumentException(
                    "planActionEtabli est requis");
        }
        if (request.mediateurDesigne() == null) {
            throw new IllegalArgumentException(
                    "mediateurDesigne est requis");
        }
        if (request.plainteIefhOuInspectionEnCours() == null) {
            throw new IllegalArgumentException(
                    "plainteIefhOuInspectionEnCours est requis");
        }
        if (request.pourcentageEcartConstate() != null) {
            BigDecimal v = request.pourcentageEcartConstate();
            if (v.signum() < 0
                    || v.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(
                        "pourcentageEcartConstate doit être compris"
                                + " entre 0 et 100");
            }
        }
    }
}

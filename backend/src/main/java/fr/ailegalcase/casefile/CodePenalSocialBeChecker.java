package fr.ailegalcase.casefile;

/**
 * SF-219-24 : qualificateur d'infraction sociale au regard du Code
 * pénal social du 06/06/2010 (M.B. 01/07/2010, e.e.v. 01/07/2011) —
 * fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 06/06/2010</b> introduisant le Code pénal social
 *       (M.B. 01/07/2010, e.e.v. 01/07/2011 par AR du 31/01/2011) —
 *       Livre 1 (principes généraux du droit social pénal,
 *       art. 1<sup>er</sup>-118) + Livre 2 (les infractions et leur
 *       répression en particulier, art. 119-235).</li>
 *   <li><b>Art. 101 à 103 C. pén. soc.</b> — barème unique de
 *       sanctions à 4 niveaux croissants de gravité.</li>
 *   <li><b>Art. 1<sup>er</sup> Loi du 05/03/1952</b> relative aux
 *       décimes additionnels sur les amendes pénales — coefficient
 *       d'indexation (+0,80 jusqu'au 31/12/2024 ; +0,90 depuis le
 *       01/01/2025 par AR 27/12/2024).</li>
 *   <li><b>Art. 105 C. pén. soc.</b> — multiplication × 5 pour les
 *       personnes morales et plafond global 100 × maximum unitaire.</li>
 *   <li><b>Art. 109 C. pén. soc.</b> — imputation aux préposés et
 *       mandataires.</li>
 *   <li><b>Art. 110 C. pén. soc.</b> — récidive légale : doublement
 *       du plafond dans le délai d'un an.</li>
 *   <li><b>Art. 119-235 C. pén. soc.</b> — qualifications spéciales
 *       (Titres 2 à 12 du Livre 2 : bien-être, durée du travail,
 *       Dimona / DmfA, registre du personnel, intérim,
 *       détachement, faux social, etc.).</li>
 *   <li><b>Art. 73-78 C. pén. soc.</b> — option administrative ou
 *       pénale de l'auditorat du travail (« règle Una Via »
 *       art. 73, Cour const. arrêt 61/2014 du 03/04/2014).</li>
 *   <li><b>Cass. ch. néerl. 21/11/2017 P.17.0532.N</b> — qualification
 *       du travail non déclaré comme infraction de niveau 4
 *       (art. 181 C. pén. soc.).</li>
 *   <li><b>Cour const. arrêt 158/2009 du 13/10/2009</b> + arrêt
 *       61/2014 du 03/04/2014 — conformité du Code pénal social aux
 *       principes de légalité et de proportionnalité des peines.</li>
 * </ul>
 *
 * <h2>Logique du qualificateur</h2>
 * <ol>
 *   <li>Si le type est {@link
 *       CodePenalSocialBeTypeInfraction#AUTRE_QUALIFICATION} et
 *       qu'aucun niveau n'est renseigné → verdict
 *       {@link CodePenalSocialBeVerdict#A_QUALIFIER}.</li>
 *   <li>Sinon, le niveau de référence est dérivé du type
 *       d'infraction selon la table doctrinale standard
 *       ({@link #niveauPourType}), avec ajustement par l'élément
 *       moral pour {@link
 *       CodePenalSocialBeTypeInfraction#ABSENCE_DMFA} et
 *       {@link
 *       CodePenalSocialBeTypeInfraction#INFRACTION_DETACHEMENT}.</li>
 *   <li>Si l'avocat fournit explicitement un niveau (
 *       {@code niveauPropose} ≠ null), celui-ci prime sur le
 *       niveau dérivé.</li>
 *   <li>Les bornes d'amende sont restituées à leur valeur de base
 *       (avant décimes additionnels — l'indexation appartient à
 *       l'avocat).</li>
 * </ol>
 */
public final class CodePenalSocialBeChecker {

    /** Bornes des amendes — art. 101 § 1<sup>er</sup> C. pén. soc. (valeur de base, avant indexation). */
    static final int N1_ADMIN_MIN = 10;
    static final int N1_ADMIN_MAX = 100;
    static final int N1_PENAL_MIN = 0;
    static final int N1_PENAL_MAX = 0;

    static final int N2_ADMIN_MIN = 50;
    static final int N2_ADMIN_MAX = 500;
    static final int N2_PENAL_MIN = 50;
    static final int N2_PENAL_MAX = 500;

    static final int N3_ADMIN_MIN = 100;
    static final int N3_ADMIN_MAX = 1000;
    static final int N3_PENAL_MIN = 100;
    static final int N3_PENAL_MAX = 1000;

    static final int N4_ADMIN_MIN = 300;
    static final int N4_ADMIN_MAX = 3000;
    static final int N4_PENAL_MIN = 600;
    static final int N4_PENAL_MAX = 6000;
    static final int N4_EMPRISONNEMENT_MIN_MOIS = 6;
    static final int N4_EMPRISONNEMENT_MAX_MOIS = 36;

    static final String BASE_JURIDIQUE =
            "Loi du 06/06/2010 introduisant le Code pénal social"
                    + " (M.B. 01/07/2010, e.e.v. 01/07/2011 par AR du"
                    + " 31/01/2011) — Livre 1 art. 1er à 118"
                    + " (principes généraux du droit social pénal :"
                    + " champ d'application matériel, dépénalisation"
                    + " partielle, échelle des sanctions, récidive,"
                    + " personnes responsables) + Livre 2 art. 119 à"
                    + " 235 (les infractions et leur répression en"
                    + " particulier : Titres 2 bien-être au travail,"
                    + " 3 durée du travail, 4 protection de la"
                    + " rémunération, 5 documents sociaux et Dimona,"
                    + " 6 inspection, 7 occupation de travailleurs"
                    + " étrangers, 8 contrats à durée déterminée"
                    + " spéciaux et intérim, 9 sécurité sociale,"
                    + " 10 discrimination, 11 entrave à l'inspection"
                    + " sociale, 12 faux social) ; art. 101 à 103"
                    + " C. pén. soc. (échelle unifiée des sanctions à"
                    + " 4 niveaux croissants de gravité — niveau 1"
                    + " amende administrative seule 10 à 100 € non"
                    + " indexée, niveaux 2-3-4 amendes administratives"
                    + " ou pénales au choix de l'auditorat du travail"
                    + " selon la « règle Una Via » art. 73, niveau 4"
                    + " incluant un emprisonnement alternatif 6 mois à"
                    + " 3 ans, multiplication par le nombre de"
                    + " travailleurs concernés art. 103 § 2 avec"
                    + " plafond global 100 × maximum unitaire) ;"
                    + " art. 105 C. pén. soc. (multiplication × 5 des"
                    + " amendes pour les personnes morales) ;"
                    + " art. 109 C. pén. soc. (imputation aux préposés"
                    + " et mandataires de l'employeur) ; art. 110"
                    + " C. pén. soc. (récidive légale dans le délai"
                    + " d'un an depuis la condamnation pénale ou"
                    + " décision administrative définitive antérieure"
                    + " pour une infraction de même niveau : doublement"
                    + " du plafond de l'amende) ; art. 73 à 78"
                    + " C. pén. soc. (« règle Una Via » — choix de"
                    + " l'auditorat du travail entre voie administrative"
                    + " ou voie pénale, exclusivité du non bis in idem,"
                    + " Cour const. arrêt 61/2014 du 03/04/2014) ;"
                    + " art. 1er Loi du 05/03/1952 relative aux décimes"
                    + " additionnels sur les amendes pénales —"
                    + " coefficient d'indexation +0,80 décimes"
                    + " additionnels jusqu'au 31/12/2024 puis +0,90"
                    + " depuis le 01/01/2025 par AR du 27/12/2024 ;"
                    + " Cass. ch. néerl. 21/11/2017 P.17.0532.N"
                    + " (qualification du travail non déclaré comme"
                    + " infraction de niveau 4 art. 181 C. pén. soc.) ;"
                    + " Cour const. arrêt 158/2009 du 13/10/2009"
                    + " (conformité du Code pénal social aux principes"
                    + " de légalité et de proportionnalité des peines).";

    static final String AVERT_AVOCAT_BE =
            "L'outil de qualification d'infraction sociale au regard"
                    + " du Code pénal social BE produit un verdict"
                    + " indicatif fondé sur les déclarations de"
                    + " l'avocat. L'avocat spécialisé en droit social"
                    + " pénal BE doit confirmer pour la situation"
                    + " considérée : (i) la qualification exacte de"
                    + " l'infraction au regard du Livre 2 du Code"
                    + " pénal social (art. 119 à 235) — un même fait"
                    + " peut tomber sous plusieurs qualifications"
                    + " concurrentes et le concours d'infractions"
                    + " obéit aux règles de l'art. 65 C. pén. (peine"
                    + " unique du chef le plus grave) ; (ii)"
                    + " l'indexation in concreto des amendes à la date"
                    + " des faits par application des décimes"
                    + " additionnels art. 1er Loi 05/03/1952 (+0,80"
                    + " jusqu'au 31/12/2024 ; +0,90 depuis le"
                    + " 01/01/2025), l'outil restituant les valeurs de"
                    + " base ; (iii) l'option administrative ou pénale"
                    + " de l'auditorat du travail art. 73-78 C. pén."
                    + " soc. (« règle Una Via », non bis in idem"
                    + " strictement opposable, Cour const. 61/2014) ;"
                    + " (iv) la multiplication par le nombre de"
                    + " travailleurs concernés art. 103 § 2 C. pén."
                    + " soc., plafonnée à 100 × le maximum unitaire de"
                    + " l'amende pour une personne morale art. 105 ;"
                    + " (v) la majoration × 5 pour personne morale"
                    + " art. 105 C. pén. soc. cumulable avec la"
                    + " multiplication travailleurs (l'ordre de"
                    + " calcul retenu par la doctrine est :"
                    + " amende unitaire × nb travailleurs × 5"
                    + " personne morale, plafonné) ; (vi) la"
                    + " caractérisation effective de la récidive"
                    + " légale art. 110 C. pén. soc. (condamnation ou"
                    + " décision administrative définitive antérieure"
                    + " pour une infraction de même niveau, date"
                    + " certaine, identité de l'auteur et de la"
                    + " personne morale) ; (vii) l'imputation aux"
                    + " préposés ou mandataires art. 109 C. pén. soc."
                    + " (champ d'application personnel : qualité de"
                    + " préposé / mandataire au moment des faits,"
                    + " délégation de pouvoirs écrite éventuelle,"
                    + " responsabilité civile de l'employeur)"
                    + " ; (viii) l'opportunité d'une demande de"
                    + " sursis (art. 8 Loi du 29/06/1964) ou d'une"
                    + " suspension du prononcé pour les amendes"
                    + " pénales, ou d'une transaction administrative"
                    + " avec le SPF Emploi (art. 84 C. pén. soc.) ;"
                    + " (ix) l'articulation avec une éventuelle"
                    + " procédure civile parallèle (paiement des"
                    + " arriérés salariaux Loi 12/04/1965, indemnités"
                    + " de rupture Loi 03/07/1978, dommages et intérêts"
                    + " art. 1382 anc. C. civ. / 5.83 nouveau C. civ.)"
                    + " ; (x) les voies de recours : tribunal du"
                    + " travail si voie administrative (art. 580, 1°"
                    + " C. jud.), tribunal correctionnel si voie"
                    + " pénale (art. 76 C. jud.), Cour du travail puis"
                    + " Cass. comme juridiction de cassation.";

    private CodePenalSocialBeChecker() {
    }

    public static CodePenalSocialBeResult analyze(
            CodePenalSocialBeRequest request) {
        validateInputs(request);

        CodePenalSocialBeTypeInfraction type = request.typeInfraction();
        Integer niveauPropose = request.niveauPropose();

        // ── Cas A_QUALIFIER : type ouvert sans niveau renseigné ─────────────
        if (type == CodePenalSocialBeTypeInfraction.AUTRE_QUALIFICATION
                && niveauPropose == null) {
            String synthese = "Le type d'infraction est ouvert"
                    + " (AUTRE_QUALIFICATION) et aucun niveau de sanction"
                    + " n'a été renseigné. Une qualification précise au"
                    + " regard du Livre 2 du Code pénal social"
                    + " (art. 119 à 235 — Titres 2 à 12) est requise"
                    + " préalablement à toute évaluation du risque pénal."
                    + " L'avocat identifie l'infraction (siège textuel)"
                    + " puis renseigne explicitement le niveau de"
                    + " sanction associé selon l'art. 101 C. pén. soc.";
            return build(request,
                    CodePenalSocialBeVerdict.A_QUALIFIER,
                    0, 0, 0, 0, 0,
                    false, 0, 0,
                    false, false, false,
                    "QUALIFICATION_OUVERTE",
                    synthese);
        }

        // ── Détermination du niveau effectif ────────────────────────────────
        int niveauEffectif = (niveauPropose != null)
                ? niveauPropose
                : niveauPourType(type, request.elementMoralIntentionnel());

        boolean multTravailleurs = request.nombreTravailleursConcernes() > 0;
        boolean majPM = request.personneMorale();
        boolean majRecidive = request.recidiveDansLAn();

        switch (niveauEffectif) {
            case 1: {
                String synthese = "Infraction de "
                        + describeType(type)
                        + " qualifiée au niveau 1 de l'échelle"
                        + " art. 101 § 1er C. pén. soc. : amende"
                        + " administrative seule de "
                        + N1_ADMIN_MIN + " à " + N1_ADMIN_MAX + " €"
                        + " (montant non indexé, art. 101 § 1er al."
                        + " 1er — seule l'amende pénale est indexée"
                        + " par les décimes additionnels)."
                        + " Pas de voie pénale possible : la sanction"
                        + " est exclusivement administrative,"
                        + " prononcée par le SPF Emploi (art. 79"
                        + " C. pén. soc.) ;"
                        + suffixeMajorations(majPM, multTravailleurs,
                                request.nombreTravailleursConcernes(),
                                majRecidive, true);
                return build(request,
                        CodePenalSocialBeVerdict.SANCTION_NIVEAU_1,
                        1, N1_ADMIN_MIN, N1_ADMIN_MAX, N1_PENAL_MIN, N1_PENAL_MAX,
                        false, 0, 0,
                        multTravailleurs, majPM, majRecidive,
                        "SANCTION_NIVEAU_1_ADMINISTRATIVE_SEULE",
                        synthese);
            }
            case 2: {
                String synthese = "Infraction de "
                        + describeType(type)
                        + " qualifiée au niveau 2 de l'échelle"
                        + " art. 101 § 1er C. pén. soc. : amende"
                        + " administrative de "
                        + N2_ADMIN_MIN + " à " + N2_ADMIN_MAX + " € OU"
                        + " amende pénale de " + N2_PENAL_MIN + " à"
                        + " " + N2_PENAL_MAX + " € au choix de"
                        + " l'auditorat du travail (art. 73-78"
                        + " C. pén. soc., « règle Una Via », non bis"
                        + " in idem, Cour const. arrêt 61/2014). Pas"
                        + " d'emprisonnement au niveau 2 ;"
                        + suffixeMajorations(majPM, multTravailleurs,
                                request.nombreTravailleursConcernes(),
                                majRecidive, false);
                return build(request,
                        CodePenalSocialBeVerdict.SANCTION_NIVEAU_2,
                        2, N2_ADMIN_MIN, N2_ADMIN_MAX, N2_PENAL_MIN, N2_PENAL_MAX,
                        false, 0, 0,
                        multTravailleurs, majPM, majRecidive,
                        "SANCTION_NIVEAU_2_ADMIN_OU_PENALE",
                        synthese);
            }
            case 3: {
                String synthese = "Infraction de "
                        + describeType(type)
                        + " qualifiée au niveau 3 de l'échelle"
                        + " art. 101 § 1er C. pén. soc. : amende"
                        + " administrative de "
                        + N3_ADMIN_MIN + " à " + N3_ADMIN_MAX + " € OU"
                        + " amende pénale de " + N3_PENAL_MIN + " à"
                        + " " + N3_PENAL_MAX + " € au choix de"
                        + " l'auditorat du travail. Pas"
                        + " d'emprisonnement au niveau 3 — l'aggravation"
                        + " par rapport au niveau 2 réside dans le"
                        + " décuplement du plafond de l'amende ;"
                        + suffixeMajorations(majPM, multTravailleurs,
                                request.nombreTravailleursConcernes(),
                                majRecidive, false);
                return build(request,
                        CodePenalSocialBeVerdict.SANCTION_NIVEAU_3,
                        3, N3_ADMIN_MIN, N3_ADMIN_MAX, N3_PENAL_MIN, N3_PENAL_MAX,
                        false, 0, 0,
                        multTravailleurs, majPM, majRecidive,
                        "SANCTION_NIVEAU_3_ADMIN_OU_PENALE",
                        synthese);
            }
            case 4: {
                String synthese = "Infraction de "
                        + describeType(type)
                        + " qualifiée au niveau 4 de l'échelle"
                        + " art. 101 § 1er C. pén. soc., niveau le"
                        + " plus grave réservé aux atteintes"
                        + " caractérisées aux droits fondamentaux des"
                        + " travailleurs : amende administrative de "
                        + N4_ADMIN_MIN + " à " + N4_ADMIN_MAX + " € OU"
                        + " amende pénale de " + N4_PENAL_MIN + " à"
                        + " " + N4_PENAL_MAX + " € (art. 102 C. pén."
                        + " soc.), ET / OU emprisonnement de "
                        + N4_EMPRISONNEMENT_MIN_MOIS + " mois à "
                        + N4_EMPRISONNEMENT_MAX_MOIS / 12 + " ans"
                        + " (art. 103 C. pén. soc.) au choix de la"
                        + " juridiction. La voie pénale est"
                        + " quasi-systématique pour les infractions de"
                        + " niveau 4 visant le travail non déclaré, le"
                        + " faux social et l'accident grave non déclaré ;"
                        + suffixeMajorations(majPM, multTravailleurs,
                                request.nombreTravailleursConcernes(),
                                majRecidive, false);
                return build(request,
                        CodePenalSocialBeVerdict.SANCTION_NIVEAU_4,
                        4, N4_ADMIN_MIN, N4_ADMIN_MAX, N4_PENAL_MIN, N4_PENAL_MAX,
                        true, N4_EMPRISONNEMENT_MIN_MOIS,
                        N4_EMPRISONNEMENT_MAX_MOIS,
                        multTravailleurs, majPM, majRecidive,
                        "SANCTION_NIVEAU_4_AMENDE_ET_OU_EMPRISONNEMENT",
                        synthese);
            }
            default:
                throw new IllegalArgumentException(
                        "Niveau de sanction inattendu : " + niveauEffectif);
        }
    }

    /**
     * Niveau par défaut associé au type d'infraction selon la doctrine
     * standard (Van Eeckhoutte, Henrard). Ajusté par l'élément moral
     * pour ABSENCE_DMFA et INFRACTION_DETACHEMENT.
     */
    static int niveauPourType(CodePenalSocialBeTypeInfraction type,
                              boolean intentionnel) {
        switch (type) {
            case TRAVAIL_NON_DECLARE:
            case ABSENCE_DIMONA:
            case ACCIDENT_GRAVE_NON_DECLARE:
            case FAUX_SOCIAL:
                return 4;
            case ABSENCE_DMFA:
                // Niveau 4 si intentionnel (fraude ONSS), niveau 2 sinon.
                return intentionnel ? 4 : 2;
            case INFRACTION_DETACHEMENT:
                // Niveau 4 si intentionnel (fraude LIMOSA), niveau 3 sinon.
                return intentionnel ? 4 : 3;
            case DEPASSEMENT_TEMPS_TRAVAIL:
            case INFRACTION_BIEN_ETRE:
            case ENTRAVE_INSPECTION_SOCIALE:
            case ABSENCE_REGISTRE_PERSONNEL:
            case INFRACTION_INTERIM:
                return 3;
            case NON_PAIEMENT_REMUNERATION:
            case DISCRIMINATION_EMPLOI:
                return 2;
            case AUTRE_QUALIFICATION:
                // Doit avoir un niveauPropose ; sinon court-circuit A_QUALIFIER.
                return 1;
            default:
                throw new IllegalArgumentException(
                        "Type d'infraction non pris en charge : " + type);
        }
    }

    private static String describeType(CodePenalSocialBeTypeInfraction type) {
        switch (type) {
            case TRAVAIL_NON_DECLARE:
                return "travail non déclaré (art. 181 C. pén. soc.,"
                        + " absence de Dimona / d'inscription au"
                        + " registre du personnel)";
            case ABSENCE_DIMONA:
                return "absence de déclaration immédiate de l'emploi"
                        + " Dimona à l'ONSS (art. 181 C. pén. soc.)";
            case ABSENCE_DMFA:
                return "absence ou inexactitude de la déclaration"
                        + " multifonctionnelle DmfA à l'ONSS"
                        + " (art. 223 C. pén. soc.)";
            case NON_PAIEMENT_REMUNERATION:
                return "non-paiement de la rémunération ou paiement"
                        + " irrégulier (art. 162-167 C. pén. soc.,"
                        + " Loi du 12/04/1965 sur la protection de la"
                        + " rémunération des travailleurs)";
            case DEPASSEMENT_TEMPS_TRAVAIL:
                return "dépassement de la durée du travail / heures"
                        + " supplémentaires hors dérogations"
                        + " (art. 137-138 C. pén. soc., Loi du"
                        + " 16/03/1971 sur le travail)";
            case INFRACTION_BIEN_ETRE:
                return "manquement aux dispositions sur le bien-être"
                        + " des travailleurs au travail"
                        + " (art. 119-132 C. pén. soc., Loi du"
                        + " 04/08/1996 + Code du bien-être au travail"
                        + " AR 28/04/2017)";
            case ACCIDENT_GRAVE_NON_DECLARE:
                return "omission de déclaration d'un accident grave à"
                        + " l'auditorat du travail / inspection"
                        + " sociale (art. 124 C. pén. soc.)";
            case DISCRIMINATION_EMPLOI:
                return "discrimination à l'emploi (art. 145 C. pén."
                        + " soc., Loi du 10/05/2007 tendant à lutter"
                        + " contre certaines formes de discrimination)";
            case ENTRAVE_INSPECTION_SOCIALE:
                return "entrave à l'exercice de la surveillance"
                        + " sociale (art. 209-212 C. pén. soc. —"
                        + " refus d'accès aux lieux de travail, refus"
                        + " de communication de documents, fausse"
                        + " déclaration à l'inspecteur)";
            case FAUX_SOCIAL:
                return "faux en écritures sociales (art. 232-235"
                        + " C. pén. soc. — Dimona / DmfA / registres"
                        + " du personnel ou documents sociaux"
                        + " sciemment falsifiés)";
            case ABSENCE_REGISTRE_PERSONNEL:
                return "absence de registre général du personnel ou"
                        + " inscription manquante (art. 185 C. pén."
                        + " soc., AR du 08/08/1980)";
            case INFRACTION_INTERIM:
                return "infraction à la réglementation du travail"
                        + " intérimaire (art. 178 C. pén. soc., Loi"
                        + " du 24/07/1987 — motif non autorisé, durée"
                        + " maximale dépassée, parité salariale"
                        + " violée, contrat écrit ETI absent)";
            case INFRACTION_DETACHEMENT:
                return "infraction au régime du détachement /"
                        + " LIMOSA (art. 182-184 C. pén. soc., Loi du"
                        + " 05/03/2002 LIMOSA, Directive 96/71/CE"
                        + " transposée par la Loi du 11/12/2016)";
            case AUTRE_QUALIFICATION:
                return "infraction sociale qualifiée explicitement par"
                        + " l'avocat (qualification précise hors table"
                        + " standard)";
            default:
                return type.name();
        }
    }

    private static String suffixeMajorations(
            boolean personneMorale,
            boolean multTravailleurs,
            int nbTravailleurs,
            boolean recidive,
            boolean niveau1Special) {
        StringBuilder sb = new StringBuilder();
        if (niveau1Special) {
            // Au niveau 1, pas de multiplication PM × 5 (jurisprudence
            // controversée — l'art. 105 vise les "amendes pénales" et
            // les amendes administratives de niveau 2-4 ; la doctrine
            // dominante exclut la multiplication × 5 pour les amendes
            // administratives de niveau 1). On signale néanmoins le
            // fait pour information.
            if (personneMorale) {
                sb.append(" l'employeur étant une personne morale,"
                        + " l'application de la multiplication × 5"
                        + " art. 105 C. pén. soc. est doctrinalement"
                        + " controversée au niveau 1 (l'art. 105 vise"
                        + " principalement les amendes pénales et les"
                        + " amendes administratives des niveaux 2 à 4) ;"
                        + " l'avocat tranchera en fonction du texte"
                        + " applicable et de la jurisprudence locale ;");
            }
        } else {
            if (personneMorale) {
                sb.append(" l'employeur étant une personne morale,"
                        + " l'amende est multipliée par 5 (art. 105"
                        + " § 1er C. pén. soc.) ;");
            }
        }
        if (multTravailleurs && nbTravailleurs > 0) {
            sb.append(" l'amende est multipliée par le nombre de"
                    + " travailleurs concernés, soit ")
              .append(nbTravailleurs)
              .append(" (art. 103 § 2 C. pén. soc.), avec un plafond"
                    + " global de 100 fois le maximum unitaire pour"
                    + " une personne morale (art. 105 § 1er) ;");
        }
        if (recidive) {
            sb.append(" la récidive dans l'année à compter de la"
                    + " condamnation pénale ou de la décision"
                    + " administrative définitive antérieure pour"
                    + " une infraction de même niveau emporte"
                    + " doublement du plafond de l'amende (art. 110"
                    + " C. pén. soc.) ;");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            sb.append(".");
        } else {
            sb.append(" aucune majoration ne s'applique en l'état des"
                    + " déclarations.");
        }
        return sb.toString();
    }

    private static CodePenalSocialBeResult build(
            CodePenalSocialBeRequest request,
            CodePenalSocialBeVerdict verdict,
            int niveauSanction,
            int amendeAdminMin,
            int amendeAdminMax,
            int amendePenaleMin,
            int amendePenaleMax,
            boolean emprisonnement,
            int emprisonnementMinMois,
            int emprisonnementMaxMois,
            boolean multTravailleurs,
            boolean majPM,
            boolean majRecidive,
            String raison,
            String synthese) {
        return new CodePenalSocialBeResult(
                request.typeInfraction(),
                request.niveauPropose(),
                request.dateFaits(),
                request.nombreTravailleursConcernes(),
                request.personneMorale(),
                request.recidiveDansLAn(),
                request.prevenuPreposeOuMandataire(),
                request.elementMoralIntentionnel(),
                verdict,
                niveauSanction,
                amendeAdminMin,
                amendeAdminMax,
                amendePenaleMin,
                amendePenaleMax,
                emprisonnement,
                emprisonnementMinMois,
                emprisonnementMaxMois,
                multTravailleurs,
                majPM,
                majRecidive,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(CodePenalSocialBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.typeInfraction() == null) {
            throw new IllegalArgumentException(
                    "typeInfraction est requis");
        }
        if (request.niveauPropose() != null
                && (request.niveauPropose() < 1 || request.niveauPropose() > 4)) {
            throw new IllegalArgumentException(
                    "niveauPropose doit être entre 1 et 4");
        }
        if (request.nombreTravailleursConcernes() < 0) {
            throw new IllegalArgumentException(
                    "nombreTravailleursConcernes doit être positif ou nul");
        }
        if (request.personneMorale() == null) {
            throw new IllegalArgumentException(
                    "personneMorale est requis");
        }
        if (request.recidiveDansLAn() == null) {
            throw new IllegalArgumentException(
                    "recidiveDansLAn est requis");
        }
        if (request.prevenuPreposeOuMandataire() == null) {
            throw new IllegalArgumentException(
                    "prevenuPreposeOuMandataire est requis");
        }
        if (request.elementMoralIntentionnel() == null) {
            throw new IllegalArgumentException(
                    "elementMoralIntentionnel est requis");
        }
    }
}

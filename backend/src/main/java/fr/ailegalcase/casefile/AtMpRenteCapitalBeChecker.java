package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.TreeMap;

/**
 * SF-219-29 : analyseur de la rente AT/MP vs capitalisation BE -
 * fonction <b>pure</b> (sans dependance HTTP / persistance / horloge -
 * l'horloge ne sert qu'a la proratisation prospective via
 * {@link LocalDate#now(ZoneId)} lorsque {@code dateDemandeConversion}
 * est null).
 *
 * <h2>Source juridique BE</h2>
 * <ul>
 *   <li><b>Loi du 10/04/1971 sur les accidents du travail</b> (M.B.
 *       29/04/1971), art. 24 - mode de versement de l'indemnite
 *       d'incapacite permanente : rente annuelle viagere si taux IPP
 *       &gt;= 19% ; capital forfaitaire unique si taux IPP &lt; 19%
 *       (capitalisation d'office).</li>
 *   <li><b>Art. 34 Loi 10/04/1971</b> - remuneration de base : derniere
 *       remuneration annuelle brute de la victime - plafond annuel
 *       art. 39 (49 853,86 EUR au 01/01/2024, indexe).</li>
 *   <li><b>Art. 39 Loi 10/04/1971</b> - plafond annuel d'indemnisation
 *       indexe (49 853,86 EUR / 2024 selon AR annuel
 *       d'indexation).</li>
 *   <li><b>Art. 45ter Loi 10/04/1971</b> - faculte de conversion
 *       partielle de la rente en capital : 1/3 maximum, apres 3 ans
 *       depuis la consolidation (delai d'epreuve), sur demande de la
 *       victime.</li>
 *   <li><b>Art. 64 Loi 10/04/1971</b> - prescription de l'action en
 *       reparation : 3 ans a compter de l'accident (ou du deces) ;
 *       contestation des decisions de l'assureur-loi devant le
 *       tribunal du travail art. 580 1° C. jud.</li>
 *   <li><b>Lois coordonnees du 03/06/1970</b>, art. 35 - les regles de
 *       calcul du regime AT s'appliquent aux MP (incapacite permanente
 *       en cas de MP : meme regime de rente / capital que les
 *       AT).</li>
 *   <li><b>AR du 21/12/1971</b> portant execution de certaines
 *       dispositions de la Loi 10/04/1971 sur les accidents du travail
 *       (M.B. 28/12/1971) - bareme de capitalisation Table I-bis et
 *       coefficients d'age.</li>
 *   <li><b>AR du 03/07/1996</b> portant execution de la Loi du
 *       10/04/1971 sur les accidents du travail - art. 24 § 2 et § 3 :
 *       date de consolidation, delai d'epreuve de 3 ans.</li>
 *   <li><b>AR du 10/12/1987</b> fixant les regles de conversion d'une
 *       rente en capital - regle de 1/3 maximum, age limite.</li>
 *   <li><b>AR du 24/02/2005</b> (M.B. 03/03/2005) actualisant le
 *       bareme de capitalisation Table I-bis - coefficients d'age
 *       decroissant utilises pour la conversion rente -&gt; capital et
 *       pour la capitalisation d'office.</li>
 *   <li><b>Cass. BE 06/11/2000 S.99.0119.F</b> Pas. 2000 I 1660 - la
 *       capitalisation d'office sous le seuil de 19% est de plein
 *       droit, non subordonnee a une demande de la victime.</li>
 *   <li><b>Cass. BE 26/05/2003 S.01.0079.F</b> - la rente AT est
 *       viagere ; elle continue jusqu'au deces de la victime ; la
 *       conversion partielle ne modifie que la fraction convertie
 *       (1/3 max), la fraction residuelle restant viagere.</li>
 *   <li><b>Cass. BE 23/03/2009 S.08.0072.F</b> - le delai d'epreuve de
 *       3 ans court a compter de la consolidation effective de
 *       l'incapacite ; conditionne la recevabilite de la demande de
 *       conversion partielle.</li>
 * </ul>
 *
 * <h2>Logique de l'analyseur</h2>
 * <ol>
 *   <li>Si statutReconnaissance = CONTESTE ou EN_COURS : verdict
 *       INELIGIBLE_NON_RECONNU ou IPP_NON_DETERMINE (calcul indicatif
 *       uniquement).</li>
 *   <li>Si tauxIpp = null : verdict IPP_NON_DETERMINE.</li>
 *   <li>Calcule l'age a la consolidation (annees revolues entre
 *       dateNaissance et dateConsolidation, ou
 *       ageConsolidationOverride si fourni - utile pour cas
 *       hypothetique avocat).</li>
 *   <li>Calcule le coefficient d'age via la Table I-bis (AR
 *       24/02/2005). Le coefficient decroit avec l'age (~12,5 a 18
 *       ans, ~9 a 50 ans, ~6 a 65 ans, ~3 a 75 ans, 0 a partir de 90
 *       ans - bareme indicatif simplifie).</li>
 *   <li>Calcule la rente annuelle : remunerationBaseAnnuelle x
 *       tauxIpp / 100.</li>
 *   <li>Si tauxIpp &lt; 19 : verdict CAPITAL_FORFAITAIRE_LT_19 -
 *       capital = renteAnnuelle x coefficientAge (capitalisation
 *       d'office).</li>
 *   <li>Si tauxIpp &gt;= 19 : verdict RENTE_ANNUELLE_GE_19 -
 *       <ul>
 *         <li>Si demandeConversionPartielle et consolidation &gt;= 3
 *             ans depuis dateDemandeConversion (ou aujourd'hui par
 *             defaut) : conversion possible, capital de conversion =
 *             (1/3 x renteAnnuelle) x coefficientAge, rente residuelle
 *             = 2/3 x renteAnnuelle.</li>
 *         <li>Sinon : conversion non encore possible, rente integrale
 *             versee.</li>
 *       </ul>
 *   </li>
 * </ol>
 */
public final class AtMpRenteCapitalBeChecker {

    /** Seuil de basculement rente / capital art. 24 Loi 10/04/1971. */
    public static final BigDecimal SEUIL_RENTE_CAPITAL = BigDecimal.valueOf(19);

    /** Delai d'epreuve avant conversion partielle art. 45ter + AR 03/07/1996. */
    public static final int DELAI_EPREUVE_CONVERSION_ANS = 3;

    /** Fraction maximale convertible en capital art. 45ter Loi 10/04/1971. */
    public static final BigDecimal FRACTION_CONVERSION = new BigDecimal("0.3333333333");

    /** Fuseau horaire belge - coherent avec les autres outils BE. */
    public static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    /**
     * Bareme de capitalisation Table I-bis AR 24/02/2005 - coefficient
     * d'age decroissant utilise pour le passage rente annuelle -&gt;
     * capital. Bareme indicatif simplifie - le bareme officiel comporte
     * des coefficients pour chaque age annee par annee ; l'avocat doit
     * verifier le coefficient exact en fonction de l'AR d'indexation en
     * vigueur a la date de consolidation.
     */
    private static final TreeMap<Integer, BigDecimal> COEFFICIENT_AGE;

    static {
        COEFFICIENT_AGE = new TreeMap<>();
        COEFFICIENT_AGE.put(16, new BigDecimal("12.8000"));
        COEFFICIENT_AGE.put(18, new BigDecimal("12.6500"));
        COEFFICIENT_AGE.put(20, new BigDecimal("12.5000"));
        COEFFICIENT_AGE.put(25, new BigDecimal("12.0000"));
        COEFFICIENT_AGE.put(30, new BigDecimal("11.4000"));
        COEFFICIENT_AGE.put(35, new BigDecimal("10.7000"));
        COEFFICIENT_AGE.put(40, new BigDecimal("9.9000"));
        COEFFICIENT_AGE.put(45, new BigDecimal("9.0000"));
        COEFFICIENT_AGE.put(50, new BigDecimal("8.0500"));
        COEFFICIENT_AGE.put(55, new BigDecimal("7.0500"));
        COEFFICIENT_AGE.put(60, new BigDecimal("6.0000"));
        COEFFICIENT_AGE.put(65, new BigDecimal("5.0500"));
        COEFFICIENT_AGE.put(70, new BigDecimal("4.1500"));
        COEFFICIENT_AGE.put(75, new BigDecimal("3.3000"));
        COEFFICIENT_AGE.put(80, new BigDecimal("2.5500"));
        COEFFICIENT_AGE.put(85, new BigDecimal("1.8000"));
        COEFFICIENT_AGE.put(90, new BigDecimal("1.2000"));
        COEFFICIENT_AGE.put(95, new BigDecimal("0.7000"));
        COEFFICIENT_AGE.put(100, new BigDecimal("0.3000"));
    }

    static final String BASE_JURIDIQUE =
            "Loi du 10/04/1971 sur les accidents du travail M.B."
                    + " 29/04/1971 art. 24 mode de versement de"
                    + " l'indemnite d'incapacite permanente partielle :"
                    + " rente annuelle viagere si taux IPP superieur ou"
                    + " egal a 19 pour cent, capital forfaitaire unique"
                    + " si taux IPP strictement inferieur a 19 pour cent"
                    + " (capitalisation d'office) ; art. 34 remuneration"
                    + " de base derniere remuneration annuelle brute de"
                    + " la victime ; art. 39 plafond annuel d'indemnisation"
                    + " indexe environ 49853,86 EUR au 01/01/2024 ; art."
                    + " 45ter faculte de conversion partielle de la rente"
                    + " en capital un tiers maximum apres delai d'epreuve"
                    + " de 3 ans depuis la consolidation ; art. 64"
                    + " contestation devant le tribunal du travail art."
                    + " 580 1° C. jud. delai 1 an ; Lois coordonnees du"
                    + " 03/06/1970 art. 35 maladies professionnelles"
                    + " regime de rente / capital identique au regime AT"
                    + " par renvoi exprès ; AR du 21/12/1971 portant"
                    + " execution de certaines dispositions de la Loi"
                    + " 10/04/1971 M.B. 28/12/1971 bareme de"
                    + " capitalisation Table I-bis et coefficients d'age"
                    + " decroissants ; AR du 03/07/1996 art. 24 § 2 et"
                    + " § 3 date de consolidation et delai d'epreuve de"
                    + " 3 ans ; AR du 10/12/1987 regles de conversion"
                    + " partielle un tiers maximum age limite ; AR du"
                    + " 24/02/2005 M.B. 03/03/2005 actualisation du"
                    + " bareme de capitalisation Table I-bis coefficients"
                    + " d'age decroissant de quasi-13 a 16 ans jusqu'a"
                    + " quasi-0 a 90 ans utilises pour la conversion"
                    + " rente vers capital et la capitalisation d'office ;"
                    + " Cass. BE 06/11/2000 S.99.0119.F Pas. 2000 I 1660"
                    + " capitalisation d'office sous le seuil de 19 pour"
                    + " cent de plein droit non subordonnee a demande de"
                    + " la victime ; Cass. BE 26/05/2003 S.01.0079.F la"
                    + " rente AT est viagere et continue jusqu'au deces"
                    + " la conversion partielle modifie uniquement la"
                    + " fraction convertie un tiers maximum la fraction"
                    + " residuelle restant viagere ; Cass. BE 23/03/2009"
                    + " S.08.0072.F le delai d'epreuve de 3 ans court a"
                    + " compter de la consolidation effective de"
                    + " l'incapacite et conditionne la recevabilite de la"
                    + " demande de conversion partielle.";

    static final String AVERT_AVOCAT_BE =
            "L'outil de calcul de rente AT/MP vs capitalisation BE produit"
                    + " un montant indicatif fonde sur les declarations de"
                    + " l'avocat et un bareme de capitalisation simplifie."
                    + " L'avocat specialise en droit social BE (axe AT/MP)"
                    + " doit confirmer pour la situation consideree :"
                    + " (i) le coefficient d'age exact selon l'AR"
                    + " d'indexation du bareme de capitalisation en vigueur"
                    + " a la date de consolidation - le coefficient retenu"
                    + " ici est une approximation simplifiee de la Table"
                    + " I-bis AR 24/02/2005 alors que le bareme officiel"
                    + " comporte un coefficient annee par annee ;"
                    + " (ii) le calcul exact de la remuneration de base art."
                    + " 34 Loi 10/04/1971 - derniere remuneration annuelle"
                    + " brute incluant primes, avantages en nature,"
                    + " heures supplementaires habituelles, pecule de"
                    + " vacances simple et double, plafonnement art. 39"
                    + " (plafond annuel indexe 49853,86 EUR au 01/01/2024"
                    + " - verifier l'AR d'indexation a la date de"
                    + " consolidation), proratisation si periode de"
                    + " reference inferieure a 12 mois art. 24 § 4 AR"
                    + " 03/07/1996 ;"
                    + " (iii) la procedure de declaration AT - declaration"
                    + " a l'employeur dans les 8 jours art. 62 Loi"
                    + " 10/04/1971, declaration a l'assureur-loi par"
                    + " l'employeur dans les 8 jours suivants, certificat"
                    + " medical descriptif art. 63 ; ou pour les MP -"
                    + " declaration a Fedris formulaires 501 victime et"
                    + " 503 employeur, rapports medicaux specialistes,"
                    + " CV professionnel documentant l'exposition ;"
                    + " (iv) la procedure de fixation de la consolidation"
                    + " et du taux IPP - examen medical par le medecin de"
                    + " l'assureur-loi (AT) ou de Fedris (MP),"
                    + " contre-expertise contradictoire du medecin"
                    + " traitant de la victime, conciliation amiable ou"
                    + " expertise judiciaire art. 962-991 C. jud. en cas"
                    + " de desaccord, decision finale art. 24 § 2 AR"
                    + " 03/07/1996 ;"
                    + " (v) la verification du seuil de 19 pour cent -"
                    + " strictement inferieur a 19 pour cent equivaut a"
                    + " la capitalisation d'office Cass. BE 06/11/2000"
                    + " S.99.0119.F, superieur ou egal a 19 pour cent"
                    + " ouvre droit a la rente annuelle viagere art. 24"
                    + " al. 1 Loi 10/04/1971 ;"
                    + " (vi) la faculte de conversion partielle art."
                    + " 45ter Loi 10/04/1971 + AR 10/12/1987 - delai"
                    + " d'epreuve de 3 ans apres consolidation Cass. BE"
                    + " 23/03/2009 S.08.0072.F, plafond un tiers maximum"
                    + " de la rente, demande motivee de la victime,"
                    + " homologation tribunal du travail art. 64 § 2 Loi"
                    + " 10/04/1971 si conversion superieure a un tiers ;"
                    + " (vii) l'indexation viagere de la rente art. 27 Loi"
                    + " 10/04/1971 - liaison a l'indice-pivot des prix a"
                    + " la consommation - revalorisation automatique ;"
                    + " (viii) l'allocation pour aide d'une tierce"
                    + " personne art. 24 al. 4 Loi 10/04/1971 -"
                    + " supplement si la victime ne peut accomplir seule"
                    + " les actes essentiels de la vie quotidienne -"
                    + " evaluation distincte de l'IPP ;"
                    + " (ix) la rente de survie art. 13-20bis Loi"
                    + " 10/04/1971 - en cas de deces consecutif a l'AT/MP,"
                    + " 30 pour cent de la remuneration de base au"
                    + " conjoint survivant, 15 pour cent par enfant"
                    + " mineur, dans la limite cumulee de 75 pour cent ;"
                    + " (x) la coordination avec le regime des pensions"
                    + " - la rente AT/MP est cumulable avec la pension"
                    + " legale de retraite ; elle se substitue toutefois"
                    + " a l'incapacite de travail INAMI sur la partie"
                    + " couverte ; verifier les regles de cumul avec la"
                    + " GRAPA pour les beneficiaires modestes ;"
                    + " (xi) la voie de recours en cas de contestation -"
                    + " tribunal du travail art. 580 1° C. jud., ressort"
                    + " du domicile de la victime art. 627 C. jud., delai"
                    + " 1 an a compter de la notification de la decision"
                    + " art. 64 Loi 10/04/1971 / art. 23 lois coordonnees"
                    + " 03/06/1970, requete contradictoire art. 1034bis"
                    + " C. jud. accompagnee d'un rapport medical certifie"
                    + " a peine d'irrecevabilite ;"
                    + " (xii) la prescription triennale art. 64 § 1 Loi"
                    + " 10/04/1971 de l'action en reparation - point de"
                    + " depart la date de l'accident pour les AT, la"
                    + " date de connaissance du caractere professionnel"
                    + " pour les MP art. 31 lois coordonnees 03/06/1970,"
                    + " causes d'interruption art. 2244 anc. C. civ. / 2.5.5"
                    + " nouveau C. civ. et de suspension art. 2251 anc."
                    + " C. civ. ;"
                    + " (xiii) l'option recursoire de l'employeur ou du"
                    + " tiers responsable art. 46 Loi 10/04/1971 -"
                    + " responsabilite extracontractuelle subsiste pour"
                    + " la partie du dommage non couverte par la rente /"
                    + " capital, action devant le tribunal civil du"
                    + " domicile du defendeur, prescription 5 ans art."
                    + " 2262bis anc. C. civ. / 5.149 nouveau C. civ.";

    private AtMpRenteCapitalBeChecker() {
    }

    public static AtMpRenteCapitalBeResult analyze(
            AtMpRenteCapitalBeRequest request) {
        validateInputs(request);

        // Statut de reconnaissance prealable - arret precoce si non reconnu.
        if (request.statutReconnaissance()
                == AtMpRenteCapitalBeStatutReconnaissance.CONTESTE) {
            return buildIneligibleNonReconnu(request);
        }

        // IPP non determine - calcul premature.
        if (request.tauxIpp() == null
                || request.statutReconnaissance()
                        == AtMpRenteCapitalBeStatutReconnaissance.EN_COURS) {
            return buildIppNonDetermine(request);
        }

        // Calcul age a la consolidation
        Integer age = computeAge(request);
        if (age == null || age < 0 || age > 120) {
            return buildAQualifier(request, age,
                    "AGE_CONSOLIDATION_INVALIDE",
                    "L'age a la consolidation calcule (" + age + ") est"
                            + " incoherent. Analyse complementaire requise.");
        }
        BigDecimal coefficient = coefficientAge(age);

        BigDecimal renteAnnuelle = computeRenteAnnuelle(
                request.remunerationBaseAnnuelle(), request.tauxIpp());

        // Branche capitalisation d'office IPP < 19%
        if (request.tauxIpp().compareTo(SEUIL_RENTE_CAPITAL) < 0) {
            return buildCapitalForfaitaire(
                    request, age, coefficient, renteAnnuelle);
        }

        // Branche rente annuelle IPP >= 19%
        return buildRenteAnnuelle(
                request, age, coefficient, renteAnnuelle);
    }

    // == Branche CAPITAL_FORFAITAIRE_LT_19 ==

    private static AtMpRenteCapitalBeResult buildCapitalForfaitaire(
            AtMpRenteCapitalBeRequest req,
            int age,
            BigDecimal coefficient,
            BigDecimal renteAnnuelle) {
        BigDecimal capital = renteAnnuelle.multiply(coefficient)
                .setScale(2, RoundingMode.HALF_UP);
        String synthese = "Taux d'incapacite permanente partielle IPP "
                + req.tauxIpp() + " pour cent inferieur strictement au"
                + " seuil de 19 pour cent prevu a l'art. 24 al. 2 Loi"
                + " 10/04/1971 sur les accidents du travail (ou art. 35"
                + " Lois coordonnees 03/06/1970 par renvoi pour les"
                + " maladies professionnelles). La capitalisation est"
                + " d'office, de plein droit, non subordonnee a une"
                + " demande de la victime (Cass. BE 06/11/2000 S.99.0119.F"
                + " Pas. 2000 I 1660). Calcul indicatif : remuneration"
                + " de base " + req.remunerationBaseAnnuelle() + " EUR x"
                + " taux IPP " + req.tauxIpp() + " pour cent = rente"
                + " annuelle theorique " + renteAnnuelle + " EUR x"
                + " coefficient d'age " + coefficient + " (age "
                + age + " ans, Table I-bis AR 24/02/2005) = capital"
                + " forfaitaire indicatif " + capital + " EUR. Le"
                + " coefficient retenu est une approximation simplifiee"
                + " du bareme officiel - l'avocat doit verifier le"
                + " coefficient exact selon l'AR d'indexation en vigueur"
                + " a la date de consolidation. Versement unique par"
                + " l'assureur-loi (AT) ou Fedris (MP) sans option de"
                + " conversion en rente. Plafonnement art. 39 Loi"
                + " 10/04/1971 a verifier - plafond annuel indexe a la"
                + " date de consolidation environ 49853,86 EUR au"
                + " 01/01/2024.";
        return new AtMpRenteCapitalBeResult(
                req.origine(), req.statutReconnaissance(),
                req.dateConsolidation(), req.tauxIpp(),
                req.remunerationBaseAnnuelle(), req.dateNaissance(),
                req.demandeConversionPartielle(),
                req.dateDemandeConversion(),
                req.ageConsolidationOverride(),
                AtMpRenteCapitalBeVerdict.CAPITAL_FORFAITAIRE_LT_19,
                true, false,
                age, coefficient, renteAnnuelle, capital, null, null,
                "IPP_LT_19_CAPITALISATION_OFFICE_ART_24_AL_2",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    // == Branche RENTE_ANNUELLE_GE_19 ==

    private static AtMpRenteCapitalBeResult buildRenteAnnuelle(
            AtMpRenteCapitalBeRequest req,
            int age,
            BigDecimal coefficient,
            BigDecimal renteAnnuelle) {
        boolean conversionPossible = canConvert(req);
        BigDecimal capitalConversion = null;
        BigDecimal renteResiduelle = null;
        if (req.demandeConversionPartielle() && conversionPossible) {
            BigDecimal fractionConvertie = renteAnnuelle.multiply(
                            FRACTION_CONVERSION)
                    .setScale(2, RoundingMode.HALF_UP);
            capitalConversion = fractionConvertie.multiply(coefficient)
                    .setScale(2, RoundingMode.HALF_UP);
            renteResiduelle = renteAnnuelle.subtract(fractionConvertie)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        String conversionPart;
        if (req.demandeConversionPartielle() && conversionPossible) {
            conversionPart = " Conversion partielle demandee et possible"
                    + " (delai d'epreuve de 3 ans depuis consolidation"
                    + " ecoule art. 45ter Loi 10/04/1971 + AR 10/12/1987,"
                    + " Cass. BE 23/03/2009 S.08.0072.F) : fraction"
                    + " convertie = un tiers de la rente, capital de"
                    + " conversion indicatif " + capitalConversion
                    + " EUR, rente residuelle viagere " + renteResiduelle
                    + " EUR/an (deux tiers de la rente, indexation art."
                    + " 27 Loi 10/04/1971).";
        } else if (req.demandeConversionPartielle()) {
            conversionPart = " Conversion partielle demandee mais le"
                    + " delai d'epreuve de 3 ans depuis la consolidation"
                    + " n'est pas ecoule a la date de demande envisagee"
                    + " (art. 45ter Loi 10/04/1971 + AR 03/07/1996,"
                    + " Cass. BE 23/03/2009 S.08.0072.F) - la demande"
                    + " sera irrecevable en l'etat, a re-introduire"
                    + " apres l'expiration du delai.";
        } else {
            conversionPart = " Aucune demande de conversion partielle -"
                    + " versement integral en rente viagere. La victime"
                    + " peut a tout moment, apres l'expiration du delai"
                    + " d'epreuve de 3 ans, solliciter la conversion"
                    + " d'un tiers maximum en capital art. 45ter Loi"
                    + " 10/04/1971.";
        }
        String synthese = "Taux d'incapacite permanente partielle IPP "
                + req.tauxIpp() + " pour cent superieur ou egal au seuil"
                + " de 19 pour cent prevu a l'art. 24 al. 1 Loi"
                + " 10/04/1971 sur les accidents du travail (ou art. 35"
                + " Lois coordonnees 03/06/1970 par renvoi pour les"
                + " maladies professionnelles). La reparation est versee"
                + " en RENTE ANNUELLE VIAGERE - Cass. BE 26/05/2003"
                + " S.01.0079.F. Calcul indicatif : remuneration de base "
                + req.remunerationBaseAnnuelle() + " EUR x taux IPP "
                + req.tauxIpp() + " pour cent = rente annuelle "
                + renteAnnuelle + " EUR (coefficient d'age "
                + coefficient + " a " + age + " ans Table I-bis AR"
                + " 24/02/2005 - utile pour la conversion partielle"
                + " art. 45ter)."
                + conversionPart
                + " La rente est indexee art. 27 Loi 10/04/1971 par"
                + " liaison a l'indice-pivot des prix a la consommation."
                + " Plafonnement art. 39 Loi 10/04/1971 a verifier -"
                + " plafond annuel indexe a la date de consolidation"
                + " environ 49853,86 EUR au 01/01/2024.";
        return new AtMpRenteCapitalBeResult(
                req.origine(), req.statutReconnaissance(),
                req.dateConsolidation(), req.tauxIpp(),
                req.remunerationBaseAnnuelle(), req.dateNaissance(),
                req.demandeConversionPartielle(),
                req.dateDemandeConversion(),
                req.ageConsolidationOverride(),
                AtMpRenteCapitalBeVerdict.RENTE_ANNUELLE_GE_19,
                false, conversionPossible,
                age, coefficient, renteAnnuelle, null,
                capitalConversion, renteResiduelle,
                "IPP_GE_19_RENTE_VIAGERE_ART_24_AL_1",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    // == Branche INELIGIBLE_NON_RECONNU ==

    private static AtMpRenteCapitalBeResult buildIneligibleNonReconnu(
            AtMpRenteCapitalBeRequest req) {
        String origineLabel = req.origine()
                == AtMpRenteCapitalBeOrigine.ACCIDENT_TRAVAIL
                ? "accident du travail Loi 10/04/1971 art. 7-8"
                : "maladie professionnelle Lois coordonnees 03/06/1970";
        String synthese = "Le " + origineLabel + " a l'origine de"
                + " l'incapacite n'est pas (encore) reconnu par"
                + " l'assureur-loi / Fedris : ni rente ni capital ne"
                + " sont dus tant que la decision de reconnaissance n'a"
                + " pas ete acquise. Le calcul est suspendu. Voie de"
                + " recours en cas de refus : tribunal du travail art."
                + " 580 1° C. jud., delai 1 an a compter de la"
                + " notification de la decision (art. 64 Loi 10/04/1971"
                + " pour les AT, art. 23 Lois coordonnees 03/06/1970"
                + " pour les MP), requete contradictoire art. 1034bis"
                + " C. jud. accompagnee d'un rapport medical certifie a"
                + " peine d'irrecevabilite. Une fois la reconnaissance"
                + " acquise (decision favorable ou jugement), reactiver"
                + " l'outil pour calculer le mode de versement effectif"
                + " et le montant indicatif.";
        return new AtMpRenteCapitalBeResult(
                req.origine(), req.statutReconnaissance(),
                req.dateConsolidation(), req.tauxIpp(),
                req.remunerationBaseAnnuelle(), req.dateNaissance(),
                req.demandeConversionPartielle(),
                req.dateDemandeConversion(),
                req.ageConsolidationOverride(),
                AtMpRenteCapitalBeVerdict.INELIGIBLE_NON_RECONNU,
                false, false,
                null, null, null, null, null, null,
                "RECONNAISSANCE_CONTESTEE_PAS_DE_CALCUL",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    // == Branche IPP_NON_DETERMINE ==

    private static AtMpRenteCapitalBeResult buildIppNonDetermine(
            AtMpRenteCapitalBeRequest req) {
        String synthese = "Le taux d'incapacite permanente partielle"
                + " IPP n'est pas encore arrete par l'expertise medicale"
                + " (assureur-loi pour les AT, Fedris pour les MP) ou"
                + " la consolidation n'est pas acquise (instruction en"
                + " cours). Le calcul rente vs capital est premature -"
                + " attendre la decision de consolidation art. 24 § 2"
                + " AR 03/07/1996. Etapes : (1) certificat medical"
                + " descriptif art. 63 Loi 10/04/1971 / formulaire 501"
                + " Fedris pour MP, (2) examen medical par le medecin"
                + " conseil de l'assureur-loi ou de Fedris, (3)"
                + " contre-expertise contradictoire avec le medecin"
                + " traitant de la victime, (4) en cas de desaccord"
                + " persistant, expertise judiciaire art. 962-991 C."
                + " jud. devant le tribunal du travail. Tant que la"
                + " consolidation n'est pas acquise, la victime percoit"
                + " l'indemnite d'incapacite temporaire (90 pour cent"
                + " de la remuneration moyenne art. 22 Loi 10/04/1971 /"
                + " art. 34 lois coordonnees pour les MP).";
        return new AtMpRenteCapitalBeResult(
                req.origine(), req.statutReconnaissance(),
                req.dateConsolidation(), req.tauxIpp(),
                req.remunerationBaseAnnuelle(), req.dateNaissance(),
                req.demandeConversionPartielle(),
                req.dateDemandeConversion(),
                req.ageConsolidationOverride(),
                AtMpRenteCapitalBeVerdict.IPP_NON_DETERMINE,
                false, false,
                null, null, null, null, null, null,
                "IPP_PAS_ENCORE_ARRETE_PAR_EXPERTISE",
                synthese, BASE_JURIDIQUE, AVERT_AVOCAT_BE);
    }

    private static AtMpRenteCapitalBeResult buildAQualifier(
            AtMpRenteCapitalBeRequest req,
            Integer age,
            String raison,
            String syntheseSpecifique) {
        return new AtMpRenteCapitalBeResult(
                req.origine(), req.statutReconnaissance(),
                req.dateConsolidation(), req.tauxIpp(),
                req.remunerationBaseAnnuelle(), req.dateNaissance(),
                req.demandeConversionPartielle(),
                req.dateDemandeConversion(),
                req.ageConsolidationOverride(),
                AtMpRenteCapitalBeVerdict.A_QUALIFIER,
                false, false,
                age, null, null, null, null, null,
                raison, syntheseSpecifique, BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    // == Helpers ==

    private static Integer computeAge(AtMpRenteCapitalBeRequest req) {
        if (req.ageConsolidationOverride() != null) {
            return req.ageConsolidationOverride();
        }
        if (req.dateNaissance() == null) {
            return null;
        }
        LocalDate dateRef = req.dateConsolidation() != null
                ? req.dateConsolidation()
                : LocalDate.now(ZONE_BRUSSELS);
        if (req.dateNaissance().isAfter(dateRef)) {
            return null;
        }
        return Period.between(req.dateNaissance(), dateRef).getYears();
    }

    private static BigDecimal coefficientAge(int age) {
        // Bareme indicatif simplifie Table I-bis AR 24/02/2005.
        if (age <= 16) {
            return COEFFICIENT_AGE.get(16);
        }
        // Recherche du palier exact ou interpolation lineaire entre 2 paliers.
        Integer floor = COEFFICIENT_AGE.floorKey(age);
        Integer ceiling = COEFFICIENT_AGE.ceilingKey(age);
        if (floor == null) {
            return BigDecimal.ZERO;
        }
        if (ceiling == null) {
            return BigDecimal.ZERO;
        }
        if (floor.equals(ceiling)) {
            return COEFFICIENT_AGE.get(floor);
        }
        BigDecimal cFloor = COEFFICIENT_AGE.get(floor);
        BigDecimal cCeil = COEFFICIENT_AGE.get(ceiling);
        // Interpolation lineaire
        BigDecimal span = BigDecimal.valueOf((long) ceiling - floor);
        BigDecimal offset = BigDecimal.valueOf((long) age - floor);
        BigDecimal delta = cCeil.subtract(cFloor);
        return cFloor.add(delta.multiply(offset)
                        .divide(span, 4, RoundingMode.HALF_UP))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal computeRenteAnnuelle(
            BigDecimal remuneration, BigDecimal tauxIpp) {
        return remuneration.multiply(tauxIpp)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static boolean canConvert(AtMpRenteCapitalBeRequest req) {
        if (req.dateConsolidation() == null) {
            return false;
        }
        LocalDate dateDemande = req.dateDemandeConversion() != null
                ? req.dateDemandeConversion()
                : LocalDate.now(ZONE_BRUSSELS);
        LocalDate finDelaiEpreuve = req.dateConsolidation()
                .plusYears(DELAI_EPREUVE_CONVERSION_ANS);
        return !dateDemande.isBefore(finDelaiEpreuve);
    }

    private static void validateInputs(AtMpRenteCapitalBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requete requise");
        }
        if (request.origine() == null) {
            throw new IllegalArgumentException("origine est requis");
        }
        if (request.statutReconnaissance() == null) {
            throw new IllegalArgumentException(
                    "statutReconnaissance est requis");
        }
        if (request.remunerationBaseAnnuelle() == null) {
            throw new IllegalArgumentException(
                    "remunerationBaseAnnuelle est requis");
        }
        if (request.remunerationBaseAnnuelle()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "remunerationBaseAnnuelle doit etre strictement positive");
        }
        if (request.dateNaissance() == null) {
            throw new IllegalArgumentException("dateNaissance est requis");
        }
        if (request.tauxIpp() != null) {
            if (request.tauxIpp().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "tauxIpp doit etre superieur ou egal a 0");
            }
            if (request.tauxIpp().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException(
                        "tauxIpp doit etre inferieur ou egal a 100");
            }
        }
        // Coherence dates : naissance avant consolidation
        if (request.dateConsolidation() != null
                && request.dateNaissance().isAfter(
                        request.dateConsolidation())) {
            throw new IllegalArgumentException(
                    "dateNaissance doit etre anterieure ou egale a"
                            + " dateConsolidation");
        }
        // Coherence : si statut RECONNU, dateConsolidation requise pour le bareme
        if (request.statutReconnaissance()
                == AtMpRenteCapitalBeStatutReconnaissance.RECONNU
                && request.dateConsolidation() == null
                && request.ageConsolidationOverride() == null) {
            throw new IllegalArgumentException(
                    "dateConsolidation ou ageConsolidationOverride requis"
                            + " lorsque statutReconnaissance = RECONNU");
        }
    }
}

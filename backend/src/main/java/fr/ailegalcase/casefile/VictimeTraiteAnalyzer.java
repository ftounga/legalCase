package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-214-21 : analyseur de l'éligibilité au titre « victime de la traite des êtres
 * humains » (L. 425-1 CESEDA, ancien L. 316-1). Outil single-country FR.
 *
 * <p>Le titre L. 425-1 protège l'étranger qui dépose plainte (ou fait l'objet d'un
 * signalement par une association agréée) contre une personne pour des faits de
 * traite des êtres humains ou de proxénétisme, et qui collabore avec les services
 * d'enquête (OCRTEH). Il ouvre droit à une APS de 6 mois renouvelable avec droit au
 * travail attaché, et à des mesures de protection (hébergement d'urgence).</p>
 *
 * <p>Source juridique :
 * <ul>
 *   <li>L. 425-1 CESEDA (ancien L. 316-1) — APS 6 mois victime TEH, droit au travail</li>
 *   <li>L. 225-4-1 à L. 225-4-9 Code pénal — traite des êtres humains</li>
 *   <li>Protocole de Palerme (ONU, 2000) — définition de la TEH</li>
 *   <li>Directive UE 2011/36 — protection des victimes de la TEH</li>
 *   <li>Circ. du 19/05/2015 (Taubira) — identification des victimes de TEH</li>
 * </ul>
 *
 * <p><b>Distinction L. 425-6 (violences conjugales, ordonnance JAF — F-208) vs L. 425-1
 * (traite des êtres humains — cet outil)</b> : deux situations juridiques distinctes,
 * deux outils distincts, conforme à l'invariant « un outil décisionnel = une situation
 * métier ».</p>
 */
public final class VictimeTraiteAnalyzer {

    /** Plainte déposée (ou signalement ONG agréée) ET collaboration OCRTEH établie. */
    public static final String VERDICT_ELIGIBLE_PROBABLE = "ELIGIBLE_PROBABLE";
    /** Collaboration OCRTEH établie mais plainte non encore déposée. */
    public static final String VERDICT_ELIGIBLE_SOUS_RESERVE_PLAINTE = "ELIGIBLE_SOUS_RESERVE_PLAINTE";
    /** Ni plainte ni collaboration — critères L. 425-1 non remplis. */
    public static final String VERDICT_NON_ELIGIBLE = "NON_ELIGIBLE";
    /** Plainte déposée sans collaboration encore engagée — identification en cours. */
    public static final String VERDICT_EN_COURS_IDENTIFICATION = "EN_COURS_IDENTIFICATION";

    /** Codes de critères non remplis exposés sous forme de chips. */
    public static final String CHIP_PLAINTE_NON_DEPOSEE = "PLAINTE_NON_DEPOSEE";
    public static final String CHIP_COLLABORATION_OCRTEH_ABSENTE = "COLLABORATION_OCRTEH_ABSENTE";
    public static final String CHIP_IDENTIFICATION_VICTIME_A_CONFIRMER = "IDENTIFICATION_VICTIME_A_CONFIRMER";

    private static final String BASE_JURIDIQUE =
            "CESEDA L. 425-1 (ancien L. 316-1, APS 6 mois victime TEH avec droit au travail) ; "
            + "Code pénal L. 225-4-1 à L. 225-4-9 (traite des êtres humains) ; "
            + "Protocole de Palerme (ONU, 2000) ; Directive UE 2011/36 ; "
            + "Circ. 19/05/2015 (identification des victimes de TEH, rôle OCRTEH)";

    public static final int TITRE_ACTUEL_MAX_LENGTH = 120;

    private VictimeTraiteAnalyzer() {}

    /**
     * Analyse l'éligibilité au titre victime de la traite des êtres humains L. 425-1.
     *
     * @param plainteDeposee                  plainte déposée (ou signalement ONG agréée)
     * @param collaborationOCRTEH             collaboration avec les services d'enquête (OCRTEH)
     * @param datePlainte                     date de dépôt de plainte (optionnelle)
     * @param titreActuel                     titre de séjour actuel (optionnel, ≤ 120)
     * @param presenceAutoriteRefugieDetectee présence de l'auteur présumé / réseau détectée
     * @return résultat de l'analyse
     */
    public static VictimeTraiteResult analyze(boolean plainteDeposee,
                                              boolean collaborationOCRTEH,
                                              LocalDate datePlainte,
                                              String titreActuel,
                                              boolean presenceAutoriteRefugieDetectee) {
        validateInputs(titreActuel);

        List<String> chips = new ArrayList<>();
        if (!plainteDeposee) {
            chips.add(CHIP_PLAINTE_NON_DEPOSEE);
        }
        if (!collaborationOCRTEH) {
            chips.add(CHIP_COLLABORATION_OCRTEH_ABSENTE);
        }
        // L'identification formelle comme victime TEH par l'OCRTEH ou une association
        // agréée reste à confirmer tant que la collaboration n'est pas établie.
        if (!collaborationOCRTEH) {
            chips.add(CHIP_IDENTIFICATION_VICTIME_A_CONFIRMER);
        }

        String verdict = determineVerdict(plainteDeposee, collaborationOCRTEH);

        // Alerte sécurité : la victime n'a pas porté plainte alors que la présence de
        // l'auteur présumé / du réseau est détectée → mise en danger.
        boolean risqueVictimeEnDanger = presenceAutoriteRefugieDetectee && !plainteDeposee;

        List<String> mesuresProtection = buildMesuresProtection(verdict, risqueVictimeEnDanger);
        List<String> recommandations = buildRecommandations(plainteDeposee, collaborationOCRTEH,
                risqueVictimeEnDanger);

        return new VictimeTraiteResult(
                plainteDeposee,
                collaborationOCRTEH,
                datePlainte == null ? null : datePlainte.toString(),
                titreActuel,
                presenceAutoriteRefugieDetectee,
                verdict,
                chips,
                mesuresProtection,
                risqueVictimeEnDanger,
                recommandations,
                BASE_JURIDIQUE);
    }

    private static String determineVerdict(boolean plainteDeposee, boolean collaborationOCRTEH) {
        // Plainte + collaboration → les deux critères centraux de L. 425-1 sont réunis.
        if (plainteDeposee && collaborationOCRTEH) {
            return VERDICT_ELIGIBLE_PROBABLE;
        }
        // Collaboration sans plainte formelle → éligibilité possible sous réserve du
        // dépôt de plainte (ou d'un signalement par une association agréée).
        if (collaborationOCRTEH) {
            return VERDICT_ELIGIBLE_SOUS_RESERVE_PLAINTE;
        }
        // Plainte sans collaboration encore engagée → identification de la victime en cours.
        if (plainteDeposee) {
            return VERDICT_EN_COURS_IDENTIFICATION;
        }
        // Ni plainte ni collaboration → critères L. 425-1 non remplis.
        return VERDICT_NON_ELIGIBLE;
    }

    private static List<String> buildMesuresProtection(String verdict, boolean risqueVictimeEnDanger) {
        List<String> mesures = new ArrayList<>();
        if (risqueVictimeEnDanger) {
            mesures.add("Hébergement d'urgence et mise en sécurité immédiate (dispositif Ac.Sé / 115).");
        }
        if (VERDICT_NON_ELIGIBLE.equals(verdict)) {
            return mesures;
        }
        if (VERDICT_ELIGIBLE_PROBABLE.equals(verdict)) {
            mesures.add("APS de 6 mois renouvelable au titre de L. 425-1 CESEDA.");
            mesures.add("Droit au travail attaché à l'APS L. 425-1.");
            if (!risqueVictimeEnDanger) {
                mesures.add("Accès à l'hébergement et à l'accompagnement social du dispositif national d'accueil.");
            }
        } else {
            // ELIGIBLE_SOUS_RESERVE_PLAINTE ou EN_COURS_IDENTIFICATION
            mesures.add("Délai de réflexion de 30 jours (art. R. 425-1 et s. CESEDA) avant régularisation.");
            mesures.add("APS de 6 mois L. 425-1 ouverte dès la réunion des critères (plainte + collaboration).");
        }
        return mesures;
    }

    private static List<String> buildRecommandations(boolean plainteDeposee,
                                                     boolean collaborationOCRTEH,
                                                     boolean risqueVictimeEnDanger) {
        List<String> reco = new ArrayList<>();
        if (risqueVictimeEnDanger) {
            reco.add("PRIORITÉ : orienter sans délai vers une mise en sécurité (Ac.Sé / 115) "
                    + "avant toute démarche administrative.");
        }
        if (!plainteDeposee) {
            reco.add("Accompagner le dépôt de plainte contre l'auteur des faits, ou obtenir un "
                    + "signalement par une association agréée (alternative à la plainte, L. 425-1).");
        }
        if (!collaborationOCRTEH) {
            reco.add("Engager la collaboration avec l'OCRTEH / les services d'enquête et "
                    + "documenter l'identification de la victime (Circ. 19/05/2015).");
        }
        if (plainteDeposee && collaborationOCRTEH) {
            reco.add("Déposer la demande d'APS L. 425-1 en préfecture en joignant le récépissé "
                    + "de plainte et l'attestation de collaboration.");
        }
        reco.add("Constituer le dossier sous l'angle du Protocole de Palerme et de la "
                + "Directive UE 2011/36 (qualification de la traite, mesures de protection).");
        return reco;
    }

    private static void validateInputs(String titreActuel) {
        if (titreActuel != null && titreActuel.length() > TITRE_ACTUEL_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "titreActuel ne peut pas dépasser " + TITRE_ACTUEL_MAX_LENGTH + " caractères");
        }
    }
}

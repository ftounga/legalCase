package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-214-39 : analyseur du droit au séjour des citoyens UE/EEE/Suisse en France
 * et des membres de leur famille. Outil single-country FR.
 *
 * <p>Régime totalement distinct du CESEDA « pays tiers » : les citoyens de
 * l'Union, de l'EEE (Norvège, Islande, Liechtenstein) et de Suisse bénéficient
 * d'un droit de séjour automatique en application de la directive 2004/38/CE,
 * transposée aux articles L. 233-1 et suivants du CESEDA.</p>
 *
 * <ul>
 *   <li>Droit de séjour automatique pour les 3 premiers mois (art. 6 directive
 *       2004/38) : aucune condition pour tout citoyen UE/EEE/Suisse.</li>
 *   <li>Droit de séjour au-delà de 3 mois (art. 7) : sous conditions d'activité
 *       (salarié/indépendant) ou de ressources suffisantes (étudiant/retraité/
 *       inactif disposant de ressources).</li>
 *   <li>Droit de séjour permanent (art. 16) : après 5 ans (60 mois) de séjour
 *       légal et ininterrompu, sous réserve d'une activité ou de ressources sur
 *       la période.</li>
 *   <li>Titre : l'attestation d'enregistrement est facultative pour le citoyen
 *       UE ; la carte de séjour « membre de famille de citoyen de l'Union » est
 *       obligatoire pour le membre de famille ressortissant d'un pays tiers
 *       (art. 10 directive 2004/38).</li>
 * </ul>
 *
 * <p>Source juridique :
 * <ul>
 *   <li>Directive 2004/38/CE du 29/04/2004</li>
 *   <li>L. 233-1 à L. 234-12 CESEDA (anciens L. 121-1+)</li>
 *   <li>R. 233-1 à R. 234-10 CESEDA</li>
 *   <li>CE 6 novembre 2015, n° 385654 ; CJUE 21 juillet 2011, C-325/09 Dias</li>
 * </ul>
 */
public final class UeEeeSuisseSejourAnalyzer {

    /** Activités professionnelles reconnues pour apprécier le droit de séjour. */
    public static final String ACTIVITE_SALARIE = "SALARIE";
    public static final String ACTIVITE_INDEPENDANT = "INDEPENDANT";
    public static final String ACTIVITE_ETUDIANT = "ETUDIANT";
    public static final String ACTIVITE_RETRAITE = "RETRAITE";
    public static final String ACTIVITE_SANS_ACTIVITE_RESSOURCES_SUFFISANTES =
            "SANS_ACTIVITE_RESSOURCES_SUFFISANTES";

    public static final Set<String> ACTIVITES_VALIDES = Set.of(
            ACTIVITE_SALARIE, ACTIVITE_INDEPENDANT, ACTIVITE_ETUDIANT,
            ACTIVITE_RETRAITE, ACTIVITE_SANS_ACTIVITE_RESSOURCES_SUFFISANTES);

    /** Titres de séjour exposés selon la situation. */
    public static final String TITRE_ATTESTATION_ENREGISTREMENT = "ATTESTATION_ENREGISTREMENT";
    public static final String TITRE_CARTE_SEJOUR_MEMBRE_FAMILLE = "CARTE_SEJOUR_MEMBRE_FAMILLE";

    /** Durée minimale du séjour légal pour le droit permanent (art. 16) — 60 mois. */
    public static final int DUREE_SEJOUR_PERMANENT_MINIMALE_MOIS = 60;

    private static final String BASE_JURIDIQUE =
            "Directive 2004/38/CE du 29/04/2004 (libre circulation des citoyens de "
            + "l'Union) ; CESEDA L. 233-1 à L. 234-12 (transposition) ; "
            + "R. 233-1 à R. 234-10 (attestation d'enregistrement, carte de séjour "
            + "membre de famille) ; CE 6 nov. 2015 n° 385654 ; "
            + "CJUE 21 juill. 2011 C-325/09 Dias (droit permanent art. 16)";

    private UeEeeSuisseSejourAnalyzer() {}

    /**
     * Analyse le droit au séjour UE/EEE/Suisse.
     *
     * @param nationalite           nationalité déclarée (libre, optionnelle)
     * @param estCitoyenUE          true si citoyen UE/EEE/Suisse
     * @param membreFamilleNonUE    true si membre de famille (ressortissant pays tiers) d'un citoyen UE
     * @param dureeSejourMois       durée du séjour en France en mois (≥ 0)
     * @param activiteProfessionnelle SALARIE | INDEPENDANT | ETUDIANT | RETRAITE | SANS_ACTIVITE_RESSOURCES_SUFFISANTES
     * @return résultat de l'analyse
     */
    public static UeEeeSuisseSejourResult analyze(String nationalite,
                                                  boolean estCitoyenUE,
                                                  boolean membreFamilleNonUE,
                                                  int dureeSejourMois,
                                                  String activiteProfessionnelle) {
        validateInputs(dureeSejourMois, activiteProfessionnelle);

        boolean activitePourvoyeuse = !ACTIVITE_SANS_ACTIVITE_RESSOURCES_SUFFISANTES
                .equals(activiteProfessionnelle);

        // Art. 6 directive 2004/38 : droit de séjour automatique 3 mois pour tout
        // citoyen UE/EEE/Suisse, sans condition.
        boolean droitSejourAutomatique3Mois = estCitoyenUE;

        // Art. 16 directive 2004/38 : droit permanent après 5 ans (60 mois) de séjour
        // légal, sous réserve d'une activité ou de ressources sur la période.
        boolean droitSejourPlus5Ans = dureeSejourMois >= DUREE_SEJOUR_PERMANENT_MINIMALE_MOIS
                && activitePourvoyeuse;

        // Titre : attestation d'enregistrement (facultative) pour le citoyen UE ;
        // carte de séjour membre de famille (obligatoire) pour le membre pays tiers.
        String titreObtenu = membreFamilleNonUE
                ? TITRE_CARTE_SEJOUR_MEMBRE_FAMILLE
                : TITRE_ATTESTATION_ENREGISTREMENT;

        List<String> conditionsRespectees = buildConditions(
                estCitoyenUE, activiteProfessionnelle, activitePourvoyeuse,
                dureeSejourMois, droitSejourPlus5Ans);

        String situationMembreNonUE = membreFamilleNonUE
                ? "Membre de famille (ressortissant d'un pays tiers) d'un citoyen de "
                + "l'Union : la carte de séjour « membre de famille de citoyen de "
                + "l'Union » est OBLIGATOIRE (art. 10 directive 2004/38/CE, "
                + "R. 233-1+ CESEDA). Le droit de séjour est dérivé de celui du "
                + "citoyen UE rejoint."
                : null;

        return new UeEeeSuisseSejourResult(
                nationalite,
                estCitoyenUE,
                membreFamilleNonUE,
                dureeSejourMois,
                activiteProfessionnelle,
                droitSejourAutomatique3Mois,
                droitSejourPlus5Ans,
                titreObtenu,
                conditionsRespectees,
                situationMembreNonUE,
                BASE_JURIDIQUE);
    }

    private static List<String> buildConditions(boolean estCitoyenUE,
                                                String activite,
                                                boolean activitePourvoyeuse,
                                                int dureeSejourMois,
                                                boolean droitSejourPlus5Ans) {
        List<String> conditions = new ArrayList<>();

        if (estCitoyenUE) {
            conditions.add("Droit de séjour automatique pour les 3 premiers mois "
                    + "(art. 6 directive 2004/38), sans condition.");
        }

        switch (activite) {
            case ACTIVITE_SALARIE -> conditions.add(
                    "Activité salariée : droit de séjour au-delà de 3 mois "
                    + "(art. 7 §1 a), sans condition de ressources.");
            case ACTIVITE_INDEPENDANT -> conditions.add(
                    "Activité indépendante : droit de séjour au-delà de 3 mois "
                    + "(art. 7 §1 a), sans condition de ressources.");
            case ACTIVITE_ETUDIANT -> conditions.add(
                    "Étudiant : droit de séjour au-delà de 3 mois sous réserve de "
                    + "ressources suffisantes et d'une assurance maladie "
                    + "(art. 7 §1 c).");
            case ACTIVITE_RETRAITE -> conditions.add(
                    "Retraité/inactif : droit de séjour au-delà de 3 mois sous "
                    + "réserve de ressources suffisantes et d'une assurance maladie "
                    + "(art. 7 §1 b).");
            case ACTIVITE_SANS_ACTIVITE_RESSOURCES_SUFFISANTES -> conditions.add(
                    "Sans activité : droit de séjour au-delà de 3 mois subordonné à "
                    + "des ressources suffisantes ET une assurance maladie "
                    + "(art. 7 §1 b) — à justifier impérativement.");
            default -> { /* déjà validé en amont */ }
        }

        if (droitSejourPlus5Ans) {
            conditions.add(String.format(
                    "Séjour légal de %d mois (≥ 60) avec activité/ressources : "
                    + "droit de séjour PERMANENT acquis (art. 16 directive 2004/38).",
                    dureeSejourMois));
        } else if (dureeSejourMois >= DUREE_SEJOUR_PERMANENT_MINIMALE_MOIS
                && !activitePourvoyeuse) {
            conditions.add("Séjour ≥ 60 mois mais sans activité ni ressources "
                    + "suffisantes sur la période : le droit permanent (art. 16) "
                    + "n'est pas constitué.");
        }

        return conditions;
    }

    private static void validateInputs(int dureeSejourMois, String activiteProfessionnelle) {
        if (dureeSejourMois < 0) {
            throw new IllegalArgumentException("dureeSejourMois ne peut pas être négatif");
        }
        if (activiteProfessionnelle == null
                || !ACTIVITES_VALIDES.contains(activiteProfessionnelle)) {
            throw new IllegalArgumentException(
                    "activiteProfessionnelle inconnue — valeurs attendues : " + ACTIVITES_VALIDES);
        }
    }
}

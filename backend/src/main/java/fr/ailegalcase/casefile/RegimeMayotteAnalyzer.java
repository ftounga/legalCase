package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-220-02 : analyseur de la portée territoriale d'un titre de séjour délivré à
 * Mayotte (F-IM-48-regime-mayotte-fr). Outil single-country FR.
 *
 * <p><b>Objet</b> : la <b>dérogation territoriale mahoraise</b>, pas le choix du
 * titre. Un titre délivré à Mayotte ne vaut <b>pas</b> autorisation de circuler /
 * séjourner en métropole sans démarche spécifique (visa territorialisé,
 * régularisation). Distinct de F-IM-05 (arbre décisionnel titre généraliste
 * métropolitain) : ici on aiguille uniquement sur la <b>portée territoriale</b> +
 * les obligations dérogatoires. L'AME Mayotte (droit social connexe) est écartée.</p>
 *
 * <p>Source juridique (à vérifier par avocat) :
 * <ul>
 *   <li>Ordonnance n° 2014-464 du 7 mai 2014 (régime mahorais)</li>
 *   <li>CESEDA L.832-1 et suivants (dispositions applicables à Mayotte)</li>
 * </ul>
 * Toutes les particularités sont annotées « à vérifier par avocat » : l'outil
 * aiguille, il ne se substitue pas à la vérification du texte applicable.</p>
 */
public final class RegimeMayotteAnalyzer {

    // Verdicts de portée territoriale.
    public static final String PORTEE_MAYOTTE_UNIQUEMENT = "MAYOTTE_UNIQUEMENT";
    public static final String PORTEE_DROIT_COMMUN = "DROIT_COMMUN";

    // Sous-statut déplacement métropole.
    public static final String DEPLACEMENT_BLOCAGE = "BLOCAGE_DEPLACEMENT";
    public static final String DEPLACEMENT_LIBRE = "DEPLACEMENT_LIBRE";

    // Types de titre.
    public static final String TYPE_VPF = "VPF";
    public static final String TYPE_SALARIE = "SALARIE";
    public static final String TYPE_ETUDIANT = "ETUDIANT";
    public static final String TYPE_RESIDENT = "RESIDENT";
    public static final String TYPE_AUTRE = "AUTRE";

    public static final Set<String> TYPES_TITRE_VALIDES = Set.of(
            TYPE_VPF, TYPE_SALARIE, TYPE_ETUDIANT, TYPE_RESIDENT, TYPE_AUTRE);

    private static final String BASE_ORD_2014 =
            "Ordonnance n° 2014-464 du 7 mai 2014 (régime mahorais) — à vérifier par avocat";
    private static final String BASE_CESEDA_L832 =
            "CESEDA L.832-1 et suivants (dispositions applicables à Mayotte) — à vérifier par avocat";

    private static final String MSG_PORTEE_MAYOTTE =
            "Titre délivré à Mayotte : il ne vaut PAS autorisation de circuler ou de "
            + "séjourner en métropole sans démarche spécifique (visa de circulation / "
            + "régularisation territorialisée). À vérifier par avocat.";
    private static final String MSG_DROIT_COMMUN =
            "Titre non délivré à Mayotte : régime de droit commun — la portée "
            + "territoriale dérogatoire mahoraise ne s'applique pas. Pour le choix du "
            + "titre lui-même, voir F-IM-05 (arbre décisionnel titre).";

    private RegimeMayotteAnalyzer() {}

    /**
     * Analyse la portée territoriale d'un titre au regard du régime mahorais.
     *
     * @param titreDelivreAMayotte         true si le titre a été délivré à Mayotte
     * @param typeTitre                    VPF | SALARIE | ETUDIANT | RESIDENT | AUTRE
     * @param projetDeplacementMetropole   true si un déplacement vers la métropole est projeté
     * @return résultat d'analyse de portée territoriale
     */
    public static RegimeMayotteResult analyze(boolean titreDelivreAMayotte,
                                              String typeTitre,
                                              boolean projetDeplacementMetropole) {
        validateInputs(typeTitre);

        List<String> obligationsSpecifiques = new ArrayList<>();
        List<String> demarchesDeplacementMetropole = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        String porteeTerritoriale;
        String sousStatutDeplacement;

        if (titreDelivreAMayotte) {
            porteeTerritoriale = PORTEE_MAYOTTE_UNIQUEMENT;
            bases.add(BASE_ORD_2014);
            bases.add(BASE_CESEDA_L832);
            messages.add(MSG_PORTEE_MAYOTTE);

            obligationsSpecifiques.addAll(obligationsParTypeTitre(typeTitre));

            if (projetDeplacementMetropole) {
                sousStatutDeplacement = DEPLACEMENT_BLOCAGE;
                messages.add("Déplacement vers la métropole projeté avec un titre "
                        + "territorialisé Mayotte : déplacement BLOQUÉ en l'état. Démarches "
                        + "préalables requises (à vérifier par avocat).");
                demarchesDeplacementMetropole.add("Solliciter un visa de circulation / "
                        + "d'entrée en métropole adapté au titre mahorais (à vérifier par avocat)");
                demarchesDeplacementMetropole.add("Vérifier les conditions d'une régularisation "
                        + "ou d'un changement de portée territoriale du titre (à vérifier par avocat)");
                demarchesDeplacementMetropole.add("À défaut, le déplacement en métropole expose à "
                        + "un séjour irrégulier hors Mayotte (à vérifier par avocat)");
            } else {
                sousStatutDeplacement = DEPLACEMENT_LIBRE;
            }
        } else {
            porteeTerritoriale = PORTEE_DROIT_COMMUN;
            sousStatutDeplacement = DEPLACEMENT_LIBRE;
            bases.add(BASE_CESEDA_L832);
            messages.add(MSG_DROIT_COMMUN);
        }

        return new RegimeMayotteResult(
                titreDelivreAMayotte,
                typeTitre,
                projetDeplacementMetropole,
                porteeTerritoriale,
                sousStatutDeplacement,
                obligationsSpecifiques,
                demarchesDeplacementMetropole,
                bases,
                messages);
    }

    private static List<String> obligationsParTypeTitre(String typeTitre) {
        List<String> obligations = new ArrayList<>();
        obligations.add("Titre territorialisé Mayotte : portée géographique limitée à "
                + "Mayotte, contrôle renforcé à l'embarquement vers la métropole (à vérifier par avocat)");
        switch (typeTitre) {
            case TYPE_VPF -> obligations.add("VPF mahoraise : conditions propres de "
                    + "délivrance et de renouvellement à Mayotte (à vérifier par avocat)");
            case TYPE_SALARIE -> obligations.add("Titre salarié mahorais : autorisation de "
                    + "travail circonscrite à Mayotte (à vérifier par avocat)");
            case TYPE_ETUDIANT -> obligations.add("Titre étudiant mahorais : poursuite des "
                    + "études en métropole à valider au regard de la portée territoriale (à vérifier par avocat)");
            case TYPE_RESIDENT -> obligations.add("Carte de résident mahoraise : examiner les "
                    + "conditions d'extension hexagonale de la portée territoriale (à vérifier par avocat)");
            default -> obligations.add("Examiner les conditions territoriales propres au titre "
                    + "considéré au regard de l'ordonnance 2014-464 (à vérifier par avocat)");
        }
        return obligations;
    }

    private static void validateInputs(String typeTitre) {
        if (typeTitre == null || !TYPES_TITRE_VALIDES.contains(typeTitre)) {
            throw new IllegalArgumentException(
                    "typeTitre inconnu — valeurs attendues : " + TYPES_TITRE_VALIDES);
        }
    }
}

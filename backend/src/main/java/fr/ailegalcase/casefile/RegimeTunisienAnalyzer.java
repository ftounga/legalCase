package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-220-01 : analyseur d'aiguillage du régime de séjour applicable à un
 * ressortissant tunisien (F-IM-47-regime-tunisien-fr). Outil single-country FR.
 *
 * <p><b>Invariant anti-gadget</b> : la Tunisie reste <b>largement renvoyée au
 * droit commun CESEDA</b>. L'accord franco-tunisien du 17/03/1988 n'apporte que
 * des <b>particularités ponctuelles</b> (étudiant, commerçant, salarié). Cet
 * outil n'est <b>pas</b> un régime parallèle fermé cloné de l'algérien
 * (F-IM-17, accord 1968) : il affiche « droit commun CESEDA SAUF
 * [particularités accord 1988] » et renvoie explicitement vers F-IM-05 /
 * outils CESEDA pour les catégories non dérogatoires.</p>
 *
 * <p>Source juridique (à vérifier par avocat) :
 * <ul>
 *   <li>Accord franco-tunisien du 17/03/1988 relatif au séjour et au travail
 *       + protocole + avenants</li>
 *   <li>CESEDA — droit commun applicable par renvoi pour tout ce que l'accord
 *       ne régit pas</li>
 * </ul>
 * Toutes les particularités sont annotées « à vérifier par avocat » : l'outil
 * aiguille, il ne se substitue pas à la vérification du texte applicable.</p>
 */
public final class RegimeTunisienAnalyzer {

    // Verdicts du régime applicable.
    public static final String REGIME_ACCORD_1988_DEROGATOIRE = "ACCORD_1988_DEROGATOIRE";
    public static final String REGIME_DROIT_COMMUN_CESEDA = "DROIT_COMMUN_CESEDA";
    public static final String REGIME_MIXTE = "MIXTE";

    // Catégories de demande.
    public static final String CATEGORIE_ETUDIANT = "ETUDIANT";
    public static final String CATEGORIE_COMMERCANT = "COMMERCANT";
    public static final String CATEGORIE_SALARIE = "SALARIE";
    public static final String CATEGORIE_FAMILIAL = "FAMILIAL";
    public static final String CATEGORIE_AUTRE = "AUTRE";

    public static final Set<String> CATEGORIES_VALIDES = Set.of(
            CATEGORIE_ETUDIANT, CATEGORIE_COMMERCANT, CATEGORIE_SALARIE,
            CATEGORIE_FAMILIAL, CATEGORIE_AUTRE);

    private static final String BASE_ACCORD_1988 =
            "Accord franco-tunisien du 17/03/1988 relatif au séjour et au travail "
            + "(+ protocole et avenants) — à vérifier par avocat";
    private static final String BASE_CESEDA =
            "CESEDA — droit commun applicable par renvoi — à vérifier par avocat";

    private static final String MSG_RENVOI_DROIT_COMMUN =
            "Catégorie non dérogatoire : l'accord franco-tunisien de 1988 ne déroge pas "
            + "ici, le régime de droit commun CESEDA s'applique. Voir F-IM-05 "
            + "(arbre décisionnel titre) et les outils CESEDA correspondants.";

    private RegimeTunisienAnalyzer() {}

    /**
     * Aiguille le régime applicable à un ressortissant tunisien.
     *
     * @param categorie               ETUDIANT | COMMERCANT | SALARIE | FAMILIAL | AUTRE
     * @param dureeSejourEnvisageeMois durée de séjour envisagée en mois (≥ 0, nullable)
     * @param titreEnCours            true si un titre de séjour est déjà en cours
     * @param dejaResident            true si le ressortissant réside déjà régulièrement en France
     * @return résultat d'aiguillage
     */
    public static RegimeTunisienResult analyze(String categorie,
                                               Integer dureeSejourEnvisageeMois,
                                               boolean titreEnCours,
                                               boolean dejaResident) {
        validateInputs(categorie, dureeSejourEnvisageeMois);

        List<String> particularites = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        String regime;
        boolean renvoiDroitCommun;

        switch (categorie) {
            case CATEGORIE_ETUDIANT -> {
                regime = REGIME_ACCORD_1988_DEROGATOIRE;
                renvoiDroitCommun = false;
                particularites.add("Carte de séjour « étudiant » selon les stipulations de "
                        + "l'accord de 1988 (à vérifier par avocat)");
                particularites.add("Stipulations propres au renouvellement et à la durée du "
                        + "titre étudiant tunisien (à vérifier par avocat)");
                bases.add(BASE_ACCORD_1988);
                bases.add(BASE_CESEDA);
                messages.add("Régime étudiant : particularités de l'accord franco-tunisien de "
                        + "1988 à confronter au droit commun CESEDA (à vérifier par avocat).");
            }
            case CATEGORIE_COMMERCANT -> {
                regime = REGIME_ACCORD_1988_DEROGATOIRE;
                renvoiDroitCommun = false;
                particularites.add("Catégorie « commerçant » spécifique à l'accord franco-tunisien "
                        + "de 1988, avec conditions propres (à vérifier par avocat)");
                particularites.add("Divergence notable avec le régime « entrepreneur / profession "
                        + "non salariée » du CESEDA généraliste (à vérifier par avocat)");
                bases.add(BASE_ACCORD_1988);
                bases.add(BASE_CESEDA);
                messages.add("Régime commerçant : particularité notable de l'accord de 1988, "
                        + "distincte du droit commun CESEDA (à vérifier par avocat).");
            }
            case CATEGORIE_SALARIE -> {
                regime = REGIME_MIXTE;
                renvoiDroitCommun = false;
                particularites.add("Particularités de délivrance du titre salarié selon l'accord "
                        + "de 1988 et son protocole (à vérifier par avocat)");
                particularites.add("Opposabilité de la situation de l'emploi appréciée selon les "
                        + "stipulations propres de l'accord, droit commun CESEDA pour le reste "
                        + "(à vérifier par avocat)");
                bases.add(BASE_ACCORD_1988);
                bases.add(BASE_CESEDA);
                messages.add("Régime salarié : particularité ponctuelle de l'accord de 1988 sur "
                        + "l'opposabilité de la situation de l'emploi ; droit commun CESEDA pour "
                        + "le reste (régime MIXTE, à vérifier par avocat).");
            }
            default -> {
                // FAMILIAL / AUTRE → droit commun CESEDA : l'accord ne déroge pas.
                regime = REGIME_DROIT_COMMUN_CESEDA;
                renvoiDroitCommun = true;
                bases.add(BASE_CESEDA);
                messages.add(MSG_RENVOI_DROIT_COMMUN);
            }
        }

        // Contexte non bloquant ajouté aux messages (n'altère pas le régime).
        if (titreEnCours) {
            messages.add("Titre de séjour en cours signalé : vérifier les stipulations de "
                    + "renouvellement applicables (accord 1988 ou CESEDA selon la catégorie).");
        }
        if (dejaResident) {
            messages.add("Résidence régulière déjà établie en France : examiner les conditions "
                    + "de changement de statut au regard de l'accord et du CESEDA.");
        }
        if (dureeSejourEnvisageeMois != null && dureeSejourEnvisageeMois > 0) {
            messages.add("Durée de séjour envisagée : " + dureeSejourEnvisageeMois
                    + " mois — confronter à la durée du titre prévue par le régime applicable.");
        }

        return new RegimeTunisienResult(
                categorie,
                dureeSejourEnvisageeMois,
                titreEnCours,
                dejaResident,
                regime,
                particularites,
                bases,
                renvoiDroitCommun,
                messages);
    }

    private static void validateInputs(String categorie, Integer dureeSejourEnvisageeMois) {
        if (categorie == null || !CATEGORIES_VALIDES.contains(categorie)) {
            throw new IllegalArgumentException(
                    "categorie inconnue — valeurs attendues : " + CATEGORIES_VALIDES);
        }
        if (dureeSejourEnvisageeMois != null && dureeSejourEnvisageeMois < 0) {
            throw new IllegalArgumentException("dureeSejourEnvisageeMois ne peut pas être négatif");
        }
    }
}

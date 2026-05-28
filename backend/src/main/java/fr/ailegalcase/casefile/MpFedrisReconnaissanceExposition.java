package fr.ailegalcase.casefile;

/**
 * SF-219-28 : caractérisation de l'exposition professionnelle au risque
 * correspondant à la maladie déclarée (durée, intensité, secteur).
 *
 * <p>Pour la <b>liste fermée</b> (AR 28/03/1969), l'exposition au
 * risque correspondant déclenche la présomption de causalité art. 32
 * lois coordonnées 03/06/1970 (présomption juridique non médicale —
 * Cass. BE 13/12/1989). Pour le <b>système ouvert</b>, l'exposition
 * n'est qu'un des éléments du faisceau d'indices de causalité directe
 * et déterminante.</p>
 *
 * <ul>
 *   <li>{@link #AVEREE} — exposition documentée et conforme aux
 *       conditions de durée et d'intensité prévues par la section de
 *       la liste (CV professionnel, contrats, fiches de poste,
 *       attestations employeurs, mesures d'exposition).</li>
 *   <li>{@link #INSUFFISANTE} — exposition partielle, durée trop
 *       courte, secteur hors champ ou conditions de l'AR 28/03/1969
 *       non remplies. La présomption ne joue pas.</li>
 *   <li>{@link #INDETERMINEE} — éléments insuffisants pour qualifier
 *       l'exposition — l'avocat doit compléter le dossier avant
 *       déclaration Fedris.</li>
 * </ul>
 */
public enum MpFedrisReconnaissanceExposition {
    /** Exposition professionnelle au risque avérée et documentée. */
    AVEREE,

    /** Exposition partielle ou non conforme aux conditions de la liste. */
    INSUFFISANTE,

    /** Exposition à caractériser — analyse complémentaire. */
    INDETERMINEE
}

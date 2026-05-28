package fr.ailegalcase.casefile;

/**
 * SF-219-28 : caractérisation du lien de causalité entre l'exposition
 * professionnelle et la maladie déclarée — décisive en système ouvert
 * (art. 30bis lois coordonnées 03/06/1970 + AR du 16/12/1985), non
 * requise pour la liste fermée (présomption art. 32).
 *
 * <p>Standard jurisprudentiel : la causalité doit être <b>directe et
 * déterminante</b> (Cass. BE 28/05/2008 S.07.0033.F ; Cass. BE
 * 04/02/2002 S.01.0095.F — l'exposition professionnelle doit être la
 * cause directe et essentielle, et pas seulement un facteur favorisant
 * parmi d'autres). Une exposition de fond marginalisée par des
 * facteurs personnels prédominants ne suffit pas.</p>
 *
 * <ul>
 *   <li>{@link #DIRECTE_DETERMINANTE} — l'exposition professionnelle
 *       est la cause directe et essentielle de la maladie (rapports
 *       médicaux concordants, expertise médecin du travail ou
 *       spécialiste, absence de facteurs personnels prédominants).</li>
 *   <li>{@link #INSUFFISANTE} — lien causal possible mais non démontré
 *       comme direct et déterminant (facteurs personnels prédominants,
 *       absence d'expertise médicale concordante).</li>
 *   <li>{@link #INDETERMINEE} — éléments médicaux non encore réunis —
 *       expertise médicale Fedris à demander.</li>
 *   <li>{@link #NON_APPLICABLE} — non applicable (maladie sur liste
 *       fermée où la présomption art. 32 dispense de la preuve de
 *       causalité).</li>
 * </ul>
 */
public enum MpFedrisReconnaissanceCausalite {
    /** Causalité directe et déterminante établie. */
    DIRECTE_DETERMINANTE,

    /** Causalité possible mais insuffisamment démontrée. */
    INSUFFISANTE,

    /** Causalité à établir — expertise médicale Fedris. */
    INDETERMINEE,

    /** Non applicable — maladie liste fermée (présomption art. 32). */
    NON_APPLICABLE
}

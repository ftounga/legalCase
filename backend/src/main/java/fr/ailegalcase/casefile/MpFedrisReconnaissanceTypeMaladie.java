package fr.ailegalcase.casefile;

/**
 * SF-219-28 : qualification de la maladie au regard de la liste fermée
 * AR du 28/03/1969 (modifié par l'AR du 06/12/2018) — point d'entrée
 * de l'analyse de reconnaissance.
 *
 * <ul>
 *   <li>{@link #LISTE_FERMEE} — la maladie figure dans l'une des
 *       sections de la liste fermée AR 28/03/1969 (1.x agents
 *       chimiques, 2.x agents physiques, 3.x agents biologiques,
 *       4.x maladies cutanées et respiratoires, 5.x maladies
 *       musculosquelettiques tableau ouvert depuis 2018, 6.x cancers
 *       professionnels). Activation de la présomption de causalité
 *       art. 32 lois coordonnées 03/06/1970.</li>
 *   <li>{@link #HORS_LISTE} — la maladie ne figure pas dans la liste
 *       fermée mais l'avocat sollicite la reconnaissance via le système
 *       ouvert (art. 30bis lois coordonnées + AR du 16/12/1985). La
 *       causalité doit être établie par expertise médicale Fedris
 *       (direct et déterminant — Cass. BE 28/05/2008 S.07.0033.F).</li>
 * </ul>
 */
public enum MpFedrisReconnaissanceTypeMaladie {
    /** Maladie inscrite sur la liste fermée AR 28/03/1969. */
    LISTE_FERMEE,

    /** Maladie hors liste — système ouvert preuve causalité. */
    HORS_LISTE
}

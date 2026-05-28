package fr.ailegalcase.casefile;

/**
 * SF-219-28 : verdict de l'outil <i>Reconnaissance d'une maladie
 * professionnelle Fedris BE</i> (Lois coordonnées du 03/06/1970
 * relatives à la prévention des maladies professionnelles et à la
 * réparation des dommages résultant de celles-ci ; AR du 28/03/1969
 * dressant la liste des maladies professionnelles donnant lieu à
 * réparation modifié par l'AR du 06/12/2018 ; AR du 16/12/1985
 * relatif aux travailleurs occupés à un travail comportant un risque
 * d'apparition d'une maladie professionnelle — système ouvert ;
 * Loi du 11/01/2018 réformant Fedris fusion FMP / FAT).
 *
 * <ul>
 *   <li>{@link #MALADIE_LISTE_FERMEE_PRESOMPTION} — la maladie déclarée
 *       est inscrite sur la liste fermée AR 28/03/1969 et l'exposition
 *       professionnelle au risque correspondant est avérée pendant la
 *       durée minimale requise. La présomption juridique de causalité
 *       (art. 32 lois coordonnées du 03/06/1970) s'applique : Fedris ne
 *       peut refuser la reconnaissance qu'en renversant la présomption
 *       (cas exceptionnel — Cass. BE 13/12/1989).</li>
 *   <li>{@link #MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE} — la
 *       maladie figure sur la liste fermée mais l'exposition
 *       professionnelle n'est pas démontrée (durée trop courte, secteur
 *       hors champ, exposition non documentée). La présomption ne joue
 *       pas. L'avocat doit étayer l'exposition ou basculer en système
 *       ouvert (preuve causalité par expertise).</li>
 *   <li>{@link #SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE} — la
 *       maladie n'est pas sur la liste fermée mais l'avocat dispose
 *       d'éléments médicaux et professionnels établissant un lien de
 *       causalité <b>direct et déterminant</b> avec l'exposition
 *       professionnelle (art. 30bis lois coordonnées 03/06/1970,
 *       système ouvert introduit par la Loi du 29/12/1990 et l'AR du
 *       16/12/1985). La causalité doit être établie par expertise
 *       médicale Fedris.</li>
 *   <li>{@link #SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE} — maladie hors
 *       liste et lien causal direct et déterminant non établi : Fedris
 *       refusera la reconnaissance. Voie : recours devant le tribunal
 *       du travail art. 580 1° C. jud., délai 1 an art. 23 lois
 *       coordonnées.</li>
 *   <li>{@link #DECLARATION_PRESCRITE} — le délai de prescription
 *       triennal de l'action en réparation art. 31 lois coordonnées
 *       du 03/06/1970 (3 ans à compter de la date à laquelle la
 *       victime a eu connaissance du caractère professionnel de la
 *       maladie) est dépassé. La réparation est éteinte sauf cause
 *       d'interruption ou de suspension.</li>
 *   <li>{@link #A_QUALIFIER} — inputs ambigus (exposition INDETERMINEE
 *       ou maladie listée sans champ d'exposition renseigné) — analyse
 *       complémentaire avocat / médecin du travail requise avant prise
 *       de position.</li>
 * </ul>
 *
 * <h2>Articulation FR / BE</h2>
 * <p>L'équivalent FR (reconnaissance MP CPAM, tableaux de l'art.
 * L. 461-2 CSS, comité régional de reconnaissance des maladies
 * professionnelles CRRMP art. L. 461-1 CSS) est juridiquement
 * distinct : régime CPAM et non Fedris, tableaux fermés FR différents
 * des annexes AR 28/03/1969 BE, délai prescription 2 ans (FR) vs 3 ans
 * (BE). Restitution séparée par outil — pas de tentative
 * d'harmonisation FR/BE.</p>
 */
public enum MpFedrisReconnaissanceVerdict {
    /** Maladie sur liste fermée + exposition avérée — présomption causalité. */
    MALADIE_LISTE_FERMEE_PRESOMPTION,

    /** Maladie sur liste fermée mais exposition non démontrée. */
    MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE,

    /** Maladie hors liste — causalité directe et déterminante prouvée. */
    SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE,

    /** Maladie hors liste — causalité insuffisante. */
    SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE,

    /** Action en réparation prescrite (3 ans art. 31 lois coordonnées 1970). */
    DECLARATION_PRESCRITE,

    /** Inputs ambigus — analyse complémentaire requise. */
    A_QUALIFIER
}

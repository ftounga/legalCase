package fr.ailegalcase.casefile;

/**
 * SF-212-25 : données d'entrée du calculateur d'évaluation de la
 * protection du lanceur d'alerte (F-DT-61, FRANCE — L. 1132-3-3 CT ;
 * loi Sapin II n° 2016-1691 du 09/12/2016 ; loi Waserman n° 2022-401
 * du 21/03/2022 transposant la directive UE 2019/1937).
 *
 * @param natureSignalement              nature du signalement
 *                                       (CRIME_DELIT, VIOLATION_DROIT_UE,
 *                                       MENACE_INTERET_GENERAL, AUTRE)
 * @param contrepartieFinanciere         contrepartie financière directe
 *                                       reçue par le lanceur d'alerte
 *                                       (cause d'exclusion du statut, loi
 *                                       Waserman 2022)
 * @param procedureSignalement           procédure utilisée
 *                                       (INTERNE, EXTERNE,
 *                                       DIVULGATION_PUBLIQUE)
 * @param referentInterneSaisi           référent interne saisi (null si
 *                                       inapplicable — entreprise < 50
 *                                       salariés ou procédure externe)
 * @param mesureRepresailleDetectee      mesure de représailles détectée
 *                                       (licenciement, sanction,
 *                                       discrimination — L. 1132-3-3 CT)
 * @param natureMesureRepresaille        nature de la mesure
 *                                       (LICENCIEMENT, SANCTION,
 *                                       MESURE_DISCRIMINATOIRE, AUTRE,
 *                                       AUCUNE)
 */
public record LanceurAlerteProtectionInput(
        NatureSignalement natureSignalement,
        Boolean contrepartieFinanciere,
        ProcedureSignalement procedureSignalement,
        Boolean referentInterneSaisi,
        Boolean mesureRepresailleDetectee,
        NatureMesureRepresaille natureMesureRepresaille
) {

    /** Nature du signalement effectué par le lanceur d'alerte. */
    public enum NatureSignalement {
        CRIME_DELIT,
        VIOLATION_DROIT_UE,
        MENACE_INTERET_GENERAL,
        AUTRE
    }

    /** Procédure de signalement utilisée. */
    public enum ProcedureSignalement {
        INTERNE,
        EXTERNE,
        DIVULGATION_PUBLIQUE
    }

    /** Nature de la mesure de représailles détectée. */
    public enum NatureMesureRepresaille {
        LICENCIEMENT,
        SANCTION,
        MESURE_DISCRIMINATOIRE,
        AUTRE,
        AUCUNE
    }
}

package fr.ailegalcase.casefile;

/**
 * SF-212-25 : requête HTTP pour l'endpoint d'évaluation de la
 * protection du lanceur d'alerte (F-DT-61, FRANCE — L. 1132-3-3 CT ;
 * loi Sapin II ; loi Waserman 2022).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record LanceurAlerteProtectionRequest(
        LanceurAlerteProtectionInput.NatureSignalement natureSignalement,
        Boolean contrepartieFinanciere,
        LanceurAlerteProtectionInput.ProcedureSignalement procedureSignalement,
        Boolean referentInterneSaisi,
        Boolean mesureRepresailleDetectee,
        LanceurAlerteProtectionInput.NatureMesureRepresaille natureMesureRepresaille
) {

    LanceurAlerteProtectionInput toInput() {
        return new LanceurAlerteProtectionInput(
                natureSignalement,
                contrepartieFinanciere,
                procedureSignalement,
                referentInterneSaisi,
                mesureRepresailleDetectee,
                natureMesureRepresaille
        );
    }
}

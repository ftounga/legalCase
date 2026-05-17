package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-DT-36-01 : requête HTTP pour l'endpoint d'analyse des nullités de procédure
 * de licenciement.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ProcedureNulliteLicenciementRequest(
        Boolean convocationEnvoyee,
        LocalDate dateConvocationPresentee,
        LocalDate dateEntretienPrealable,
        Boolean entretienTenu,
        LocalDate dateNotificationLicenciement,
        Boolean lettreLicenciementEcrite,
        Boolean lettreMotivee,
        Boolean motivationSuffisante,
        String motivationCommentaire,
        Boolean licenciementPourMotifGrave,
        Boolean licenciementCollectif,
        Boolean procedureCseRespectee,
        Boolean conventionCollectiveApplicable,
        Boolean conventionCollectiveRespectee,
        String conventionCollectiveCommentaire
) {

    ProcedureNulliteLicenciementInput toInput() {
        return new ProcedureNulliteLicenciementInput(
                convocationEnvoyee,
                dateConvocationPresentee,
                dateEntretienPrealable,
                entretienTenu,
                dateNotificationLicenciement,
                lettreLicenciementEcrite,
                lettreMotivee,
                motivationSuffisante,
                motivationCommentaire,
                licenciementPourMotifGrave,
                licenciementCollectif,
                procedureCseRespectee,
                conventionCollectiveApplicable,
                conventionCollectiveRespectee,
                conventionCollectiveCommentaire
        );
    }
}

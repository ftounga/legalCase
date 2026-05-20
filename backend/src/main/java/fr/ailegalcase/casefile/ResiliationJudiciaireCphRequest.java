package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-206-07 : requête HTTP pour l'endpoint d'analyse d'une demande de
 * résiliation judiciaire du contrat de travail aux torts de l'employeur
 * (FRANCE).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ResiliationJudiciaireCphRequest(
        Boolean defautPaiementSalaire,
        BigDecimal montantImpayesEur,
        Boolean harcelement,
        Boolean manquementSecurite,
        Boolean modificationUnilateraleContrat,
        Boolean declassement,
        Boolean discrimination,
        Boolean heuresSupNonPayees,
        Boolean nonRespectDureesRepos,
        Boolean manquementsPersistantsAuJourDemande,
        Boolean salarieToujoursEnPoste,
        Boolean licenciementIntervenuEnCoursInstance,
        String manquementsCommentaire
) {

    ResiliationJudiciaireCphInput toInput() {
        return new ResiliationJudiciaireCphInput(
                defautPaiementSalaire,
                montantImpayesEur,
                harcelement,
                manquementSecurite,
                modificationUnilateraleContrat,
                declassement,
                discrimination,
                heuresSupNonPayees,
                nonRespectDureesRepos,
                manquementsPersistantsAuJourDemande,
                salarieToujoursEnPoste,
                licenciementIntervenuEnCoursInstance,
                manquementsCommentaire
        );
    }
}

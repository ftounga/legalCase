package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-206-05 : requête HTTP pour l'endpoint d'analyse d'une prise d'acte de
 * la rupture aux torts de l'employeur (FRANCE).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record PriseActeRuptureRequest(
        Boolean defautPaiementSalaire,
        BigDecimal montantImpayesEur,
        Boolean harcelement,
        Boolean manquementSecurite,
        Boolean modificationUnilateraleContrat,
        Boolean declassement,
        Boolean discrimination,
        Boolean heuresSupNonPayees,
        Boolean nonRespectDureesRepos,
        Boolean griefsActuelsEtPersistants,
        Boolean griefRendImpossiblePoursuite,
        String griefsCommentaire
) {

    PriseActeRuptureInput toInput() {
        return new PriseActeRuptureInput(
                defautPaiementSalaire,
                montantImpayesEur,
                harcelement,
                manquementSecurite,
                modificationUnilateraleContrat,
                declassement,
                discrimination,
                heuresSupNonPayees,
                nonRespectDureesRepos,
                griefsActuelsEtPersistants,
                griefRendImpossiblePoursuite,
                griefsCommentaire
        );
    }
}

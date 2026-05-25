package fr.ailegalcase.casefile;

/**
 * SF-212-33 : requête HTTP pour l'endpoint de requalification d'un
 * contrat à temps partiel en temps complet
 * (F-DT-49-temps-partiel-requalification, FRANCE — L. 3123-1 à
 * L. 3123-20 CT, L. 3245-1 CT prescription rappel salaire).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record TempsPartielRequalificationRequest(
        Double dureeContractuelleHeures,
        Boolean mentionsDureePresentes,
        Boolean mentionsRepartitionPresentes,
        Boolean mentionsHCPresentes,
        Double heuresComplementairesReelleMoyenneHeures,
        Boolean modificationRepartitionUnilaterale,
        Integer ancienneteMois,
        Double salaireMensuelContractuelEuros
) {

    TempsPartielRequalificationInput toInput() {
        return new TempsPartielRequalificationInput(
                dureeContractuelleHeures,
                mentionsDureePresentes,
                mentionsRepartitionPresentes,
                mentionsHCPresentes,
                heuresComplementairesReelleMoyenneHeures,
                modificationRepartitionUnilaterale,
                ancienneteMois,
                salaireMensuelContractuelEuros
        );
    }
}

package fr.ailegalcase.casefile;

/**
 * SF-212-35 : requête HTTP pour l'endpoint d'analyse de la conformité d'un
 * PDV ou d'une RCC (F-DT-46-pdv-rcc-conformite, FRANCE — L. 1237-17 à
 * L. 1237-19-14 CT).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record PdvRccConformiteRequest(
        PdvRccConformiteInput.TypeDispositif typeDispositif,
        boolean accordCollectifMajoritaire,
        boolean validationDREETSObtenue,
        Boolean delaiValidation15JRespectee,
        boolean indemnitesAuMoinsLegales,
        boolean adhesionVolontaireIndividuelle,
        boolean informationIndividuelleComplete
) {

    PdvRccConformiteInput toInput() {
        return new PdvRccConformiteInput(
                typeDispositif,
                accordCollectifMajoritaire,
                validationDREETSObtenue,
                delaiValidation15JRespectee,
                indemnitesAuMoinsLegales,
                adhesionVolontaireIndividuelle,
                informationIndividuelleComplete
        );
    }
}

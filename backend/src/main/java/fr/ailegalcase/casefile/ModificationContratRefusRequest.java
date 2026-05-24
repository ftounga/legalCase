package fr.ailegalcase.casefile;

/**
 * SF-212-11 : requête HTTP pour l'endpoint d'analyse de la modification du
 * contrat refusée par le salarié (F-DT-70, FRANCE — Cass. soc. ; L. 1222-6 CT).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ModificationContratRefusRequest(
        ModificationContratRefusInput.ElementModifie elementModifie,
        boolean elementExplicitementContractualise,
        boolean motifEconomique,
        Boolean notificationEcriteL1222_6,
        Integer delaiReflexionRespecteMois,
        ModificationContratRefusInput.ReponseSalarie reponseSalarie
) {

    ModificationContratRefusInput toInput() {
        return new ModificationContratRefusInput(
                elementModifie,
                elementExplicitementContractualise,
                motifEconomique,
                notificationEcriteL1222_6,
                delaiReflexionRespecteMois,
                reponseSalarie
        );
    }
}

package fr.ailegalcase.casefile;

/**
 * SF-212-11 : données d'entrée du calculateur d'analyse de la modification du
 * contrat de travail refusée par le salarié (F-DT-70, FRANCE — Cass. soc. sur
 * la distinction modification du contrat / changement des conditions de travail ;
 * L. 1222-6 CT — modification pour motif économique).
 *
 * @param elementModifie                          élément du contrat modifié
 *                                                (REMUNERATION, QUALIFICATION,
 *                                                DUREE_TRAVAIL, LIEU_TRAVAIL,
 *                                                HORAIRES, TACHES, AUTRE)
 * @param elementExplicitementContractualise      l'élément est explicitement
 *                                                contractualisé (clause écrite
 *                                                dans le contrat ou avenant)
 * @param motifEconomique                         la modification est proposée
 *                                                pour un motif économique au
 *                                                sens de L. 1222-6 CT
 * @param notificationEcriteL1222_6               notification écrite L. 1222-6
 *                                                envoyée par LRAR (null si non
 *                                                applicable / non concerné)
 * @param delaiReflexionRespecteMois              délai de réflexion respecté
 *                                                en mois (1 mois standard,
 *                                                2 mois si entreprise en PSE)
 *                                                — null si non applicable
 * @param reponseSalarie                          réponse du salarié (REFUS,
 *                                                ACCEPTATION, EN_ATTENTE)
 */
public record ModificationContratRefusInput(
        ElementModifie elementModifie,
        boolean elementExplicitementContractualise,
        boolean motifEconomique,
        Boolean notificationEcriteL1222_6,
        Integer delaiReflexionRespecteMois,
        ReponseSalarie reponseSalarie
) {

    /** Élément du contrat objet de la modification. */
    public enum ElementModifie {
        REMUNERATION,
        QUALIFICATION,
        DUREE_TRAVAIL,
        LIEU_TRAVAIL,
        HORAIRES,
        TACHES,
        AUTRE
    }

    /** Réponse du salarié à la proposition de modification. */
    public enum ReponseSalarie {
        REFUS,
        ACCEPTATION,
        EN_ATTENTE
    }
}

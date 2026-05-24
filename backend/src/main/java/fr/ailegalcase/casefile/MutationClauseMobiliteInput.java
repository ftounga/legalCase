package fr.ailegalcase.casefile;

/**
 * SF-212-13 : données d'entrée du calculateur d'analyse de la validité d'une
 * clause de mobilité et des conséquences d'un refus de mutation
 * (F-DT-71-mutation-clause-mobilite, FRANCE — jurisprudence Cass. soc. sur la
 * clause de mobilité : zone géographique précise, intérêt légitime, mise en
 * œuvre de bonne foi, délai de prévenance raisonnable, atteinte à la vie
 * personnelle / familiale).
 *
 * @param clauseMobilitePresente                clause de mobilité explicitement
 *                                              présente dans le contrat de
 *                                              travail (ou avenant signé)
 * @param zoneGeographiquePrecise               zone géographique de la clause
 *                                              suffisamment précise (Cass. soc.
 *                                              07/06/2006 n°04-45.846 ;
 *                                              Cass. soc. 24/01/2008
 *                                              n°06-45.088 — "France entière"
 *                                              insuffisant sauf justification
 *                                              par le secteur)
 * @param interetLegitimeEmployeur              mutation justifiée par un intérêt
 *                                              légitime de l'entreprise (Cass.
 *                                              soc. 23/02/2005 — pas de
 *                                              détournement de pouvoir)
 * @param delaiPrevenanceSemaines               délai de prévenance accordé au
 *                                              salarié pour rejoindre son
 *                                              nouveau poste, exprimé en
 *                                              semaines (Cass. soc. 03/03/2010
 *                                              — délai insuffisant = modification
 *                                              du contrat)
 * @param situationFamilialeSalarieContraignante situation familiale du salarié
 *                                              contraignante (enfants, conjoint,
 *                                              soins, garde) — atteinte
 *                                              disproportionnée à la vie
 *                                              personnelle (Cass. soc.
 *                                              14/10/2008 n°07-40.523)
 * @param motifMutationProfessionnel            motif de la mutation rattaché à
 *                                              un besoin professionnel objectif
 *                                              (réorganisation, ouverture de
 *                                              site, transfert de poste) — par
 *                                              opposition à un motif personnel
 *                                              ou disciplinaire déguisé
 * @param reponseSalarie                        réponse du salarié à la mutation
 *                                              (REFUS, ACCEPTATION, EN_ATTENTE)
 */
public record MutationClauseMobiliteInput(
        boolean clauseMobilitePresente,
        boolean zoneGeographiquePrecise,
        boolean interetLegitimeEmployeur,
        Integer delaiPrevenanceSemaines,
        boolean situationFamilialeSalarieContraignante,
        boolean motifMutationProfessionnel,
        ReponseSalarie reponseSalarie
) {

    /** Réponse du salarié à la mutation. */
    public enum ReponseSalarie {
        REFUS,
        ACCEPTATION,
        EN_ATTENTE
    }
}

package fr.ailegalcase.casefile;

/**
 * SF-212-13 : requête HTTP pour l'endpoint d'analyse de la validité d'une
 * clause de mobilité et des conséquences d'un refus de mutation
 * (F-DT-71-mutation-clause-mobilite, FRANCE — Cass. soc. constante sur la
 * clause de mobilité).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record MutationClauseMobiliteRequest(
        boolean clauseMobilitePresente,
        boolean zoneGeographiquePrecise,
        boolean interetLegitimeEmployeur,
        Integer delaiPrevenanceSemaines,
        boolean situationFamilialeSalarieContraignante,
        boolean motifMutationProfessionnel,
        MutationClauseMobiliteInput.ReponseSalarie reponseSalarie
) {

    MutationClauseMobiliteInput toInput() {
        return new MutationClauseMobiliteInput(
                clauseMobilitePresente,
                zoneGeographiquePrecise,
                interetLegitimeEmployeur,
                delaiPrevenanceSemaines,
                situationFamilialeSalarieContraignante,
                motifMutationProfessionnel,
                reponseSalarie
        );
    }
}

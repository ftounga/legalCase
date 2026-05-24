package fr.ailegalcase.casefile;

/**
 * SF-212-19 : requête HTTP pour l'endpoint d'analyse de la régularité d'une
 * mise à pied disciplinaire (F-DT-48-mise-a-pied-disciplinaire, FRANCE —
 * L. 1331-1 CT ; L. 1332-1 à L. 1332-4 CT ; Cass. soc. constante).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record MiseAPiedDisciplinaireRequest(
        MiseAPiedDisciplinaireInput.NatureMiseAPied natureMiseAPied,
        boolean procedureEntretienSuivie,
        boolean prescriptionFauteVerifiee,
        boolean dureeDefiniedansRI,
        Integer dureeJours,
        boolean salaireSuspendu,
        boolean sancionsAnterieuresMemesFaits,
        double salaireMensuelBrutEuros
) {

    MiseAPiedDisciplinaireInput toInput() {
        return new MiseAPiedDisciplinaireInput(
                natureMiseAPied,
                procedureEntretienSuivie,
                prescriptionFauteVerifiee,
                dureeDefiniedansRI,
                dureeJours,
                salaireSuspendu,
                sancionsAnterieuresMemesFaits,
                salaireMensuelBrutEuros
        );
    }
}

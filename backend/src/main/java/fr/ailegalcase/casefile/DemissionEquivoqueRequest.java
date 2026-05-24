package fr.ailegalcase.casefile;

/**
 * SF-212-21 : requête HTTP pour l'endpoint d'analyse de la validité d'une
 * démission au regard de la jurisprudence exigeant une volonté claire et non
 * équivoque (F-DT-41-demission-validite-equivoque, FRANCE — L. 1237-1 CT ;
 * Cass. soc. 09/05/2007).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record DemissionEquivoqueRequest(
        DemissionEquivoqueInput.ModeExpressionDemission modeExpressionDemission,
        boolean contexteAltercation,
        boolean pressionOuMenace,
        boolean retractationDansDelai,
        Integer delaiRetractationJours,
        boolean manquementsEmployeurContemporains,
        boolean etatEmotionnelPerturbe
) {

    DemissionEquivoqueInput toInput() {
        return new DemissionEquivoqueInput(
                modeExpressionDemission,
                contexteAltercation,
                pressionOuMenace,
                retractationDansDelai,
                delaiRetractationJours,
                manquementsEmployeurContemporains,
                etatEmotionnelPerturbe
        );
    }
}

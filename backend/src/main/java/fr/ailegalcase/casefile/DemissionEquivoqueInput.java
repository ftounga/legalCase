package fr.ailegalcase.casefile;

/**
 * SF-212-21 : données d'entrée du calculateur d'analyse de la validité d'une
 * démission au regard de la jurisprudence exigeant une volonté claire et non
 * équivoque (F-DT-41-demission-validite-equivoque, FRANCE — fondements :
 * L. 1237-1 CT — démission régulière du CDI par le salarié ; Cass. soc.
 * 09/05/2007 — la démission ne peut résulter que d'une volonté claire et non
 * équivoque ; Cass. soc. constante — requalification possible en prise d'acte
 * de la rupture aux torts de l'employeur ou en licenciement sans cause réelle
 * et sérieuse lorsque l'équivocité est démontrée).
 *
 * @param modeExpressionDemission           mode d'expression effectif de la
 *                                          démission — voir l'enum
 *                                          {@link ModeExpressionDemission} (LRAR /
 *                                          courrier formel, e-mail, SMS, oral
 *                                          ou autre forme)
 * @param contexteAltercation               la démission a été donnée dans un
 *                                          contexte d'altercation, dispute,
 *                                          réunion conflictuelle immédiate
 *                                          (facteur d'équivocité)
 * @param pressionOuMenace                  la démission a été donnée sous
 *                                          pression, menace, contrainte morale
 *                                          ou physique (facteur fort
 *                                          d'équivocité)
 * @param retractationDansDelai             le salarié a rétracté sa démission
 *                                          (par courrier, e-mail, demande de
 *                                          réintégration) — élément factuel
 *                                          fort de non-volonté
 * @param delaiRetractationJours            délai en jours entre la démission
 *                                          et sa rétractation, si rétractation
 *                                          ; nul si pas de rétractation
 * @param manquementsEmployeurContemporains des manquements graves de
 *                                          l'employeur (impayés, harcèlement,
 *                                          modification unilatérale) sont
 *                                          contemporains de la démission —
 *                                          ouvrent la voie à une
 *                                          requalification en prise d'acte
 * @param etatEmotionnelPerturbe            le salarié était dans un état
 *                                          émotionnel perturbé (burn-out, deuil,
 *                                          syndrome dépressif) au moment de la
 *                                          démission — facteur d'équivocité
 */
public record DemissionEquivoqueInput(
        ModeExpressionDemission modeExpressionDemission,
        boolean contexteAltercation,
        boolean pressionOuMenace,
        boolean retractationDansDelai,
        Integer delaiRetractationJours,
        boolean manquementsEmployeurContemporains,
        boolean etatEmotionnelPerturbe
) {

    /** Mode d'expression de la démission. */
    public enum ModeExpressionDemission {
        /** LRAR ou courrier recommandé classique — forme la plus formelle. */
        ECRIT_FORMEL,
        /** E-mail — forme à valeur probante mais souvent impulsive. */
        EMAIL,
        /** SMS — forme à valeur probante limitée et souvent impulsive. */
        SMS,
        /** Oral — démission verbale, à confirmer par écrit pour être opposable. */
        ORAL,
        /** Autre forme. */
        AUTRE
    }
}

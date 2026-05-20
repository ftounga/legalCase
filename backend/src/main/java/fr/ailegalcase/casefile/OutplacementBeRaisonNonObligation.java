package fr.ailegalcase.casefile;

/**
 * SF-207-08 : raison pour laquelle l'employeur n'est pas tenu à l'obligation
 * d'outplacement (CCT n°82 ; CCT n°82 bis ; Loi du 5 septembre 2001 art. 13).
 *
 * <p>Émise UNIQUEMENT pour le verdict
 * {@link OutplacementBeResult.Verdict#OUTPLACEMENT_NON_DU} ; {@code null}
 * lorsque l'obligation est applicable.</p>
 */
public enum OutplacementBeRaisonNonObligation {

    /** Salarié âgé de moins de 45 ans à la date du licenciement. */
    AGE_INFERIEUR_45,

    /** Ancienneté du salarié strictement inférieure à 1 an. */
    ANCIENNETE_INFERIEURE_1AN,

    /** Licenciement pour faute grave (art. 35 Loi 03/07/1978) — l'employeur est dispensé. */
    FAUTE_GRAVE,

    /** Démission du salarié — l'obligation employeur ne joue pas. */
    DEMISSION
}

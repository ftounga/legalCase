package fr.ailegalcase.casefile;

/**
 * SF-FA-09-01 : énumération des 8 types de fautes recevables en divorce pour faute
 * (art. 242 Cciv). Constituent une "violation grave ou renouvelée des devoirs et
 * obligations du mariage rendant intolérable le maintien de la vie commune".
 *
 * <p>Chaque entrée correspond à un chef de faute couramment invoqué devant le JAF.
 * La liste n'est pas limitative ({@link #AUTRE} permet de saisir d'autres faits
 * qualifiables au sens de l'art. 242 Cciv).
 */
public enum DivorceFauteType {

    /** Adultère — violation du devoir de fidélité (art. 212 Cciv). */
    ADULTERE,

    /** Violences physiques ou psychologiques — intolérables au sens de l'art. 242. */
    VIOLENCES,

    /** Abandon du domicile conjugal — violation du devoir de communauté de vie. */
    ABANDON,

    /** Outrages, injures, propos déshonorants. */
    OUTRAGES,

    /** Violation du devoir d'assistance (art. 212 Cciv) — ex. refus de soins. */
    DEVOIR_ASSISTANCE,

    /** Violation du devoir de fidélité (non réductible à l'adultère). */
    DEVOIR_FIDELITE,

    /** Violation du devoir de communauté de vie (art. 215 Cciv). */
    DEVOIR_COMMUNAUTE_VIE,

    /** Tout autre fait qualifiable au sens de l'art. 242 Cciv. */
    AUTRE
}

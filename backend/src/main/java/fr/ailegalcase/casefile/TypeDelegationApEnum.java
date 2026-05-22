package fr.ailegalcase.casefile;

/**
 * SF-216-09 : type de délégation d'autorité parentale envisagée par l'avocat
 * lorsqu'il interroge l'outil F-FA-XX-delegation-ap (art. 376-1 Cciv).
 *
 * <ul>
 *   <li>{@link #VOLONTAIRE_CONJOINTE} — délégation volontaire à l'initiative
 *       des deux parents au profit d'un tiers acceptant (art. 376-1 al. 1).</li>
 *   <li>{@link #JUDICIAIRE_TIERS} — délégation judiciaire saisie par un tiers
 *       qui exerce déjà l'autorité parentale de fait, à défaut d'accord des
 *       parents (art. 376-1 al. 2).</li>
 *   <li>{@link #JUDICIAIRE_DESINTERET} — délégation judiciaire fondée sur le
 *       désintérêt manifeste d'un parent depuis plus d'un an (art. 376-1 al. 2).</li>
 * </ul>
 */
public enum TypeDelegationApEnum {
    VOLONTAIRE_CONJOINTE,
    JUDICIAIRE_TIERS,
    JUDICIAIRE_DESINTERET
}

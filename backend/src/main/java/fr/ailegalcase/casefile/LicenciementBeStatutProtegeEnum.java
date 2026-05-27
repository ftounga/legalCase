package fr.ailegalcase.casefile;

/**
 * SF-213-08 : statut protégé du travailleur belge dans le cadre de la
 * protection contre le licenciement des représentants des travailleurs.
 *
 * <p>Source : <b>Loi du 19 mars 1991</b> relative à la protection contre le
 * licenciement des représentants des travailleurs au Conseil d'Entreprise
 * (CE) et au Comité pour la Prévention et la Protection au Travail (CPPT),
 * complétée par la <b>CCT n° 5 du 24 mai 1971</b> relative au statut des
 * délégations syndicales.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif distinct (statut
 * protégé délégué / CSE / conseiller prud'homme L. 2411-1 et s. C. trav.)
 * avec des procédures et indemnités différentes.</p>
 *
 * <h2>Périodes de protection</h2>
 * <ul>
 *   <li>{@link #DELEGUE_SYNDICAL_ELU} : durant tout le mandat (4 ans, cycle
 *       élections sociales) + protection résiduelle 2 ans après expiration.</li>
 *   <li>{@link #CANDIDAT_NON_ELU} : 2 ans après les élections sociales.</li>
 *   <li>{@link #DELEGUE_CCT5} : représentant syndical non lié au CPPT/CE —
 *       protection 2 ans (régime CCT n° 5).</li>
 *   <li>{@link #CONSEILLER_CPPT} : membre du Comité pour la Prévention et la
 *       Protection au Travail, traitement aligné sur les délégués élus
 *       (4 ans cycle).</li>
 * </ul>
 */
public enum LicenciementBeStatutProtegeEnum {
    /** Délégué syndical élu au CE/CPPT — mandat 4 ans + 2 ans résiduels. */
    DELEGUE_SYNDICAL_ELU,

    /** Candidat non élu aux élections sociales — protection 2 ans. */
    CANDIDAT_NON_ELU,

    /** Délégué syndical relevant exclusivement de la CCT n° 5 — 2 ans. */
    DELEGUE_CCT5,

    /** Conseiller / membre CPPT élu — protection mandat 4 ans + résiduel. */
    CONSEILLER_CPPT
}

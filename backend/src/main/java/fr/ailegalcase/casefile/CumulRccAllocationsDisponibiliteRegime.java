package fr.ailegalcase.casefile;

/**
 * SF-219-04 : régime de disponibilité applicable au bénéficiaire RCC.
 *
 * <p>L'ONEM impose une <b>disponibilité ajustée</b> sur le marché de
 * l'emploi pour les bénéficiaires de RCC, modulée selon l'âge à la prise
 * de cours.</p>
 *
 * <ul>
 *   <li>{@link #DISPONIBILITE_ADAPTEE} — bénéficiaires < 62 ans (et
 *       certains régimes long carrière / métiers lourds avec conditions
 *       d'ancienneté professionnelle ≥ 42 ans) : recherche active mais
 *       allégée, accompagnement Forem/Actiris/VDAB.</li>
 *   <li>{@link #DISPONIBILITE_PASSIVE} — bénéficiaires ≥ 62 ans ou
 *       relevant des régimes RCC dispensés (long carrière ≥ 42 ans /
 *       métiers lourds spécifiques) : dispense de recherche active, mais
 *       maintien de l'inscription comme demandeur d'emploi.</li>
 *   <li>{@link #DISPENSE_RECHERCHE_EMPLOI} — dispense totale (cas marginal :
 *       inaptitude médicale ONEM, fin de carrière proche de la pension
 *       légale).</li>
 * </ul>
 */
public enum CumulRccAllocationsDisponibiliteRegime {
    /** Disponibilité adaptée — recherche active allégée. */
    DISPONIBILITE_ADAPTEE,

    /** Disponibilité passive — dispense recherche active, inscription maintenue. */
    DISPONIBILITE_PASSIVE,

    /** Dispense totale de recherche d'emploi (inaptitude, proche pension). */
    DISPENSE_RECHERCHE_EMPLOI
}

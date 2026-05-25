package fr.ailegalcase.casefile;

/**
 * SF-213-01 : zone géographique d'application d'une clause de non-concurrence BE.
 *
 * <p>Conformément à l'art. 65 §2 de la Loi du 03/07/1978 et à la CCT n°13, la
 * clause ne peut pas s'étendre au-delà du territoire belge sauf si l'activité
 * du salarié a une portée internationale prouvée (voir
 * {@link ClauseNonConcurrenceBeRequest#activiteInternationaleProuvee()}).</p>
 */
public enum ClauseNonConcurrenceBeZoneEnum {
    /** Clause limitée à la Belgique — régime standard, conforme à la CCT 13. */
    BELGIQUE_UNIQUEMENT,
    /** Clause couvrant la Belgique et l'étranger — exige une justification métier. */
    BELGIQUE_ET_ETRANGER,
    /** Zone non renseignée dans la clause — assimilée à un défaut de validité. */
    NON_SPECIFIEE
}

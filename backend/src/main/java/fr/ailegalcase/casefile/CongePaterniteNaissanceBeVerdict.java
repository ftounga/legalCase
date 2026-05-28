package fr.ailegalcase.casefile;

/**
 * SF-219-31 : verdict de l'outil <i>Congé paternité / naissance BE —
 * calcul de durée et de protection</i> (Loi du 03/07/1978 art. 30 § 2 +
 * Loi du 12/08/2000 + Loi du 07/04/2023 extension Deal pour l'emploi).
 *
 * <p>L'outil qualifie le droit au congé de naissance en combinant : la
 * durée applicable en fonction de la date de naissance (20 jours
 * ouvrables pour naissances à partir du 01/01/2023, 15 jours du
 * 01/01/2021 au 31/12/2022, 10 jours avant le 01/01/2021), le statut
 * professionnel du travailleur (salarié, indépendant, agent public,
 * autre), le lien de filiation ou parental établi, et l'avancement de
 * la prise des jours dans la fenêtre de 4 mois post-naissance art. 30
 * § 2 al. 2.</p>
 *
 * <p>Les verdicts intègrent la protection contre le licenciement de
 * 5 mois (à compter de la notification à l'employeur et jusqu'à 5 mois
 * après la prise complète du congé) prévue par l'art. 30 § 4 Loi
 * 03/07/1978 — opposable à l'employeur en cas de licenciement non
 * fondé sur un motif étranger au congé (Cass. BE 16/03/2015
 * S.13.0094.F).</p>
 *
 * <ul>
 *   <li>{@link #ELIGIBLE_CONGE_OUVERT} — droit ouvert, en attente de
 *       prise ou en cours.</li>
 *   <li>{@link #CONGE_EN_COURS_PROTECTION_ACTIVE} — congé en cours de
 *       prise, protection 5 mois active.</li>
 *   <li>{@link #CONGE_PRIS_PROTECTION_RESIDUELLE} — congé entièrement
 *       pris, protection 5 mois résiduelle.</li>
 *   <li>{@link #INELIGIBLE_STATUT_NON_COUVERT} — statut professionnel
 *       hors champ Loi 03/07/1978 (à orienter vers régime spécifique).</li>
 *   <li>{@link #INELIGIBLE_FILIATION_NON_ETABLIE} — lien de filiation
 *       ou parental non établi.</li>
 *   <li>{@link #DROIT_PERDU_DELAI_DEPASSE} — délai 4 mois dépassé sans
 *       prise complète et sans cas de report légal.</li>
 *   <li>{@link #INELIGIBLE_NAISSANCE_FUTURE} — droit en formation,
 *       calcul indicatif seulement.</li>
 *   <li>{@link #A_QUALIFIER} — inputs insuffisants ou contradictoires.</li>
 * </ul>
 */
public enum CongePaterniteNaissanceBeVerdict {
    /** Droit au congé ouvert, en attente de prise ou prise partielle. */
    ELIGIBLE_CONGE_OUVERT,

    /** Congé en cours de prise, protection 5 mois licenciement active. */
    CONGE_EN_COURS_PROTECTION_ACTIVE,

    /** Congé entièrement pris, protection 5 mois résiduelle. */
    CONGE_PRIS_PROTECTION_RESIDUELLE,

    /** Statut hors champ Loi 03/07/1978. */
    INELIGIBLE_STATUT_NON_COUVERT,

    /** Filiation ou lien parental non établi. */
    INELIGIBLE_FILIATION_NON_ETABLIE,

    /** Délai 4 mois post-naissance dépassé sans prise complète. */
    DROIT_PERDU_DELAI_DEPASSE,

    /** Naissance future — calcul indicatif. */
    INELIGIBLE_NAISSANCE_FUTURE,

    /** Inputs insuffisants — analyse complémentaire avocat requise. */
    A_QUALIFIER
}

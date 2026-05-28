package fr.ailegalcase.casefile;

/**
 * SF-219-30 : mode de demande d'intervention psychosociale auprès du
 * CPAP — distinction Loi du 04/08/1996 art. 32/2 + AR du 10/04/2014
 * art. 11 à 32.
 *
 * <p>L'AR du 10/04/2014 organise deux voies clairement distinctes
 * (procédure interne) : la demande d'<b>intervention psychosociale
 * informelle</b> (art. 14 à 16 — médiation interne, conciliation
 * confidentielle, sans rapport au employeur) et la demande
 * d'<b>intervention psychosociale formelle</b> (art. 16 à 22 — enquête
 * écrite avec rapport motivé adressé à l'employeur dans les 3 mois).
 * Le travailleur choisit librement et peut basculer de l'informelle
 * vers la formelle.</p>
 *
 * <p>La demande formelle se subdivise selon que le traumatisme est à
 * dimension principalement <b>individuelle</b> (faits de harcèlement
 * ou violence identifiés) ou <b>collective</b> (charge psychosociale
 * de l'organisation visant un groupe de travailleurs) — la distinction
 * conditionne la nature de l'analyse menée par le CPAP (AR 10/04/2014
 * art. 16 § 2).</p>
 *
 * <ul>
 *   <li>{@link #INFORMELLE} — médiation interne / conciliation, sans
 *       rapport employeur (AR 10/04/2014 art. 14 à 16, confidentielle).</li>
 *   <li>{@link #FORMELLE_INDIVIDUELLE} — enquête écrite individualisée
 *       avec rapport au employeur (AR 10/04/2014 art. 16 à 22).</li>
 *   <li>{@link #FORMELLE_COLLECTIVE} — analyse organisationnelle de la
 *       situation de travail (AR 10/04/2014 art. 16 § 2).</li>
 * </ul>
 */
public enum BienEtreRpsConseillerPreventionModeDemande {
    /** Demande informelle — médiation interne sans rapport employeur. */
    INFORMELLE,

    /** Demande formelle individuelle — enquête écrite individualisée. */
    FORMELLE_INDIVIDUELLE,

    /** Demande formelle collective — analyse organisationnelle. */
    FORMELLE_COLLECTIVE
}

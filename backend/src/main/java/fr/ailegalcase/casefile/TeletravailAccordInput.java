package fr.ailegalcase.casefile;

/**
 * SF-212-15 : données d'entrée du calculateur d'analyse de la conformité du
 * dispositif de télétravail et des litiges courants
 * (F-DT-82-teletravail-accord, FRANCE — L. 1222-9 à L. 1222-11 CT ; ANI
 * télétravail du 26/11/2020 ; accord d'entreprise ou charte unilatérale).
 *
 * @param cadreTeletravail                       cadre juridique du télétravail
 *                                               (ACCORD_COLLECTIF, CHARTE_UNILATERALE,
 *                                               ACCORD_INDIVIDUEL, AUCUN)
 * @param doubleVolontariatRespectee             double volontariat employeur +
 *                                               salarié respecté (L. 1222-9
 *                                               al. 1) — false admis si
 *                                               circonstances exceptionnelles
 *                                               (L. 1222-11 — pandémie, force
 *                                               majeure)
 * @param indemniteOccupationVersee              indemnité d'occupation /
 *                                               remboursement de frais
 *                                               professionnels versée au
 *                                               salarié (ANI 2020 art. 6.2 ;
 *                                               tolérance URSSAF 2,60 €/jour
 *                                               ou justificatifs)
 * @param montantIndemniteJournalierEuros        montant journalier de
 *                                               l'indemnité d'occupation en
 *                                               euros (utilisé pour estimer
 *                                               l'indemnité due en cas de
 *                                               non-versement)
 * @param accidentDomicileDetecte                accident survenu au domicile
 *                                               pendant la plage horaire de
 *                                               télétravail détecté
 *                                               (présomption L. 1222-9 al. 4)
 * @param retourBureauImposeUnilateralement      retour au bureau imposé
 *                                               unilatéralement par
 *                                               l'employeur sans accord ni
 *                                               délai de prévenance
 * @param refusTeletravailCauseIncrimination     refus du salarié de
 *                                               télétravailler invoqué par
 *                                               l'employeur comme cause ou
 *                                               motif aggravant dans une
 *                                               procédure disciplinaire ou un
 *                                               licenciement (L. 1222-9 al. 6
 *                                               interdit cette qualification)
 */
public record TeletravailAccordInput(
        CadreTeletravail cadreTeletravail,
        boolean doubleVolontariatRespectee,
        boolean indemniteOccupationVersee,
        Double montantIndemniteJournalierEuros,
        boolean accidentDomicileDetecte,
        boolean retourBureauImposeUnilateralement,
        boolean refusTeletravailCauseIncrimination
) {

    /** Cadre juridique du télétravail (L. 1222-9 CT). */
    public enum CadreTeletravail {
        /** Accord d'entreprise ou de branche (L. 1222-9 al. 1). */
        ACCORD_COLLECTIF,
        /** Charte élaborée par l'employeur après consultation du CSE (L. 1222-9 al. 2). */
        CHARTE_UNILATERALE,
        /** Accord direct par tout moyen, avenant ou échange écrit (L. 1222-9 al. 3). */
        ACCORD_INDIVIDUEL,
        /** Aucun cadre formalisé — pratique informelle. */
        AUCUN
    }
}

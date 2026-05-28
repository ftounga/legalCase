package fr.ailegalcase.casefile;

/**
 * SF-219-30 : étape procédurale de la saisine du CPAP — AR du
 * 10/04/2014 art. 11 à 32.
 *
 * <p>L'AR du 10/04/2014 organise la chronologie de la procédure
 * interne :</p>
 *
 * <ol>
 *   <li><b>Intention</b> — le travailleur envisage de saisir le CPAP
 *       (consultation préalable possible avec la personne de confiance
 *       art. 31 AR 10/04/2014).</li>
 *   <li><b>Entretien préalable obligatoire</b> avec le CPAP avant tout
 *       dépôt d'une demande formelle (art. 16 § 1 AR 10/04/2014) —
 *       délai de 10 jours calendrier après le 1er contact pour fixer
 *       cet entretien.</li>
 *   <li><b>Demande informelle en cours</b> — conciliation / médiation
 *       confidentielle, pas de délai légal strict.</li>
 *   <li><b>Demande formelle déposée</b> — accusé de réception remis au
 *       travailleur ; le CPAP a 3 mois pour rendre son avis motivé à
 *       l'employeur (art. 22 § 1 AR 10/04/2014), éventuellement
 *       prolongeable de 3 mois sur demande motivée.</li>
 *   <li><b>Avis rendu</b> — rapport motivé transmis à l'employeur et
 *       au travailleur ; l'employeur dispose de 2 mois pour notifier
 *       les mesures prises (art. 32 AR 10/04/2014).</li>
 *   <li><b>Mesures prises par employeur</b> — clôture de la procédure
 *       interne ; ouverture éventuelle des voies externes
 *       (Inspection Contrôle du bien-être au travail art. 80 et s.,
 *       tribunal du travail art. 578 C. jud.).</li>
 * </ol>
 */
public enum BienEtreRpsConseillerPreventionEtape {
    /** Étape 1 — intention de saisine, consultation personne de confiance. */
    INTENTION_DEMANDE,

    /** Étape 2 — entretien préalable obligatoire CPAP réalisé. */
    ENTRETIEN_PREALABLE_REALISE,

    /** Étape 3 — demande informelle en cours (médiation interne). */
    DEMANDE_INFORMELLE_EN_COURS,

    /** Étape 4 — demande formelle déposée, accusé de réception remis. */
    DEMANDE_FORMELLE_DEPOSEE,

    /** Étape 5 — avis motivé rendu par CPAP. */
    AVIS_RENDU,

    /** Étape 6 — mesures notifiées par employeur, procédure interne clôturée. */
    MESURES_PRISES_PAR_EMPLOYEUR
}

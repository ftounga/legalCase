package fr.ailegalcase.casefile;

/**
 * SF-219-31 : étape procédurale du droit au congé de naissance BE
 * (Loi du 03/07/1978 art. 30 § 2 + AR du 17/10/1994 modalités
 * d'information et de prise + INAMI/AR du 03/07/1996 indemnisation).
 *
 * <ol>
 *   <li><b>Avant la naissance</b> — droit en formation, vérification
 *       des conditions d'ouverture (contrat de travail en cours, statut
 *       salarié, filiation établie ou en préparation).</li>
 *   <li><b>Naissance survenue</b> — point de départ du délai légal de
 *       4 mois pour la prise des jours art. 30 § 2 al. 2 Loi 03/07/1978.</li>
 *   <li><b>Notification employeur faite</b> — formalité d'avis à
 *       l'employeur (AR du 17/10/1994), avec présentation d'un
 *       justificatif (acte de naissance, attestation hôpital).</li>
 *   <li><b>Congé en cours de prise</b> — au moins un jour pris, période
 *       de protection contre le licenciement active art. 30 § 4 Loi
 *       03/07/1978.</li>
 *   <li><b>Congé pris entièrement</b> — clôture du droit, fin de la
 *       protection 5 mois post-prise.</li>
 *   <li><b>Délai 4 mois dépassé</b> — droit en partie ou totalement
 *       perdu (sauf reports légaux exceptionnels — incapacité de la
 *       mère, hospitalisation de l'enfant art. 30 § 2 al. 4).</li>
 * </ol>
 */
public enum CongePaterniteNaissanceBeEtape {
    /** Étape 1 — avant la naissance, droit en formation. */
    AVANT_NAISSANCE,

    /** Étape 2 — naissance survenue, délai 4 mois ouvert. */
    NAISSANCE_SURVENUE,

    /** Étape 3 — notification à l'employeur effectuée. */
    NOTIFICATION_EMPLOYEUR_FAITE,

    /** Étape 4 — au moins un jour pris, protection 5 mois active. */
    CONGE_EN_COURS_DE_PRISE,

    /** Étape 5 — congé totalement pris, droit clôturé. */
    CONGE_PRIS_ENTIEREMENT,

    /** Étape 6 — délai légal 4 mois dépassé sans prise complète. */
    DELAI_QUATRE_MOIS_DEPASSE
}

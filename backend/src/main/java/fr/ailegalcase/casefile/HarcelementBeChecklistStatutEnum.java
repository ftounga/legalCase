package fr.ailegalcase.casefile;

/**
 * SF-213-07 : statut d'un item de checklist de la procédure interne BE de
 * plainte pour harcèlement.
 *
 * <p>Sert à signaler à l'avocat l'état réel de l'action à mener pour
 * accompagner son client dans la procédure :</p>
 *
 * <ul>
 *   <li>{@link #A_FAIRE} — action à initier — pas encore commencée.</li>
 *   <li>{@link #EN_COURS} — action en cours d'exécution (ex. délai 90 j
 *       enquête CPAP, démarche informelle).</li>
 *   <li>{@link #TERMINE} — action close (ex. enquête CPAP rendue).</li>
 *   <li>{@link #ACTIF} — état actif au sens régime juridique (ex. la
 *       protection contre représailles est « active » pendant 12 mois,
 *       elle n'est pas « en cours » d'exécution mais un statut juridique
 *       du salarié).</li>
 * </ul>
 */
public enum HarcelementBeChecklistStatutEnum {
    /** À initier. */
    A_FAIRE,

    /** En cours d'exécution. */
    EN_COURS,

    /** Action close. */
    TERMINE,

    /** État actif (protection juridique, fenêtre temporelle ouverte). */
    ACTIF
}

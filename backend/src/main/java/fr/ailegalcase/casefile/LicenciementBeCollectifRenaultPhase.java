package fr.ailegalcase.casefile;

/**
 * SF-219-07 : phase courante de la procédure de licenciement collectif
 * Loi du 13/02/1998 (loi Renault) atteinte par l'employeur à la date du
 * contrôle.
 *
 * <p>La Loi Renault impose une procédure en 3 phases strictement
 * séquentielles, déclenchées par une notification écrite à l'autorité
 * régionale de l'emploi (Forem / Actiris / VDAB) :</p>
 *
 * <ol>
 *   <li>{@link #INFORMATION} — information écrite et préalable du
 *       Conseil d'Entreprise (CE), à défaut du Comité pour la Prévention
 *       et la Protection au Travail (CPPT), à défaut de la délégation
 *       syndicale. L'employeur fournit motifs économiques / techniques,
 *       nombre et catégories de travailleurs concernés, période, critères
 *       de sélection (CCT n° 24 art. 6).</li>
 *   <li>{@link #CONSULTATION} — consultation effective : l'employeur doit
 *       répondre par écrit aux questions et contre-propositions des
 *       représentants, examiner les mesures d'évitement / réduction du
 *       nombre de licenciements et les mesures sociales d'accompagnement
 *       (CCT n° 24 art. 7 + CCT n° 39).</li>
 *   <li>{@link #DECISION_ET_NOTIFICATION} — notification écrite à
 *       l'autorité régionale de l'emploi : déclenche le délai d'attente
 *       de 30 jours pendant lequel <b>aucun préavis individuel ne peut
 *       être notifié</b> sous peine de nullité.</li>
 * </ol>
 *
 * <p>Une 4e valeur {@link #AUCUNE_PROCEDURE_DEMARREE} signale que
 * l'employeur n'a encore engagé aucune des 3 phases — utile pour les
 * conseils préventifs au stade du projet.</p>
 */
public enum LicenciementBeCollectifRenaultPhase {
    /** Aucune phase entamée — projet seulement. */
    AUCUNE_PROCEDURE_DEMARREE,

    /** Phase 1 : information écrite préalable du CE / CPPT / DS. */
    INFORMATION,

    /** Phase 2 : consultation effective + réponses motivées employeur. */
    CONSULTATION,

    /** Phase 3 : notification à l'autorité régionale + délai 30 jours. */
    DECISION_ET_NOTIFICATION
}

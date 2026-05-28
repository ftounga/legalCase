package fr.ailegalcase.casefile;

/**
 * SF-219-30 : verdict de l'outil <i>Saisine du Conseiller en
 * Prévention Aspects Psychosociaux (CPAP) — procédure RPS interne BE</i>
 * (Loi du 04/08/1996 art. 32sexies + AR du 10/04/2014).
 *
 * <p>L'outil ne se prononce <b>pas</b> sur le fond du harcèlement —
 * il qualifie la <b>conformité procédurale</b> de la saisine au regard
 * de l'AR du 10/04/2014 (entretien préalable, choix mode demande,
 * formalisme écrit, délais 10 jours / 3 mois / 2 mois). La qualification
 * matérielle des faits relève des outils harcèlement BE
 * (voir SF-213-07 procédure formelle, F-DT-30 protection contre les
 * représailles art. 32sexies).</p>
 *
 * <h2>Articulation avec les outils harcèlement existants</h2>
 * <ul>
 *   <li>SF-213-07 {@code harcelement-be-procedure-formelle} —
 *       <b>suite</b> de la procédure : checklist détaillée de la
 *       demande formelle déjà déposée, calcul protection 12 mois
 *       art. 32sexies.</li>
 *   <li>F-DT-30 {@code harcelement-licenciement-nul-section} —
 *       <b>conséquence</b> en cas de représailles : nullité du
 *       licenciement.</li>
 *   <li>SF-219-30 (présente) — <b>amont</b> : qualifie la conformité de
 *       la <i>saisine</i> elle-même (entretien préalable, formalisme,
 *       respect des délais légaux).</li>
 * </ul>
 *
 * <ul>
 *   <li>{@link #SAISINE_CONFORME} — toutes les formalités préalables à
 *       la demande sont remplies (CPAP identifié, entretien préalable
 *       réalisé, demande écrite signée et accusée si formelle).</li>
 *   <li>{@link #SAISINE_INFORMELLE_EN_COURS} — médiation interne en
 *       cours ; pas de délai légal strict ; basculement formelle
 *       possible à tout moment.</li>
 *   <li>{@link #SAISINE_FORMELLE_EN_COURS} — demande formelle déposée,
 *       enquête CPAP en cours, dans les 3 mois légaux.</li>
 *   <li>{@link #AVIS_RENDU_DELAI_RESPECTE} — avis motivé rendu dans
 *       les 3 mois (ou prolongation motivée 6 mois max).</li>
 *   <li>{@link #AVIS_RENDU_DELAI_DEPASSE} — avis rendu hors délai ; le
 *       CPAP commet une irrégularité mais l'avis reste valide.</li>
 *   <li>{@link #NON_CONFORME_FORMALITES_MANQUANTES} — entretien préalable
 *       non réalisé pour une demande formelle, demande non écrite ou
 *       non signée, accusé de réception manquant.</li>
 *   <li>{@link #NON_CONFORME_PAS_DE_CONSEILLER} — aucun CPAP identifié
 *       (entreprise n'a pas désigné de CPAP interne ni recouru à un
 *       SEPP externe) — manquement employeur art. 32sexies § 2.</li>
 *   <li>{@link #A_QUALIFIER} — inputs insuffisants ou contradictoires.</li>
 * </ul>
 */
public enum BienEtreRpsConseillerPreventionVerdict {
    /** Saisine conforme — toutes formalités préalables remplies. */
    SAISINE_CONFORME,

    /** Saisine informelle en cours — médiation interne. */
    SAISINE_INFORMELLE_EN_COURS,

    /** Saisine formelle en cours — enquête CPAP dans les 3 mois. */
    SAISINE_FORMELLE_EN_COURS,

    /** Avis motivé CPAP rendu dans les délais légaux. */
    AVIS_RENDU_DELAI_RESPECTE,

    /** Avis motivé CPAP rendu hors délai (irrégularité, avis valide). */
    AVIS_RENDU_DELAI_DEPASSE,

    /** Formalités préalables manquantes (entretien, signature, AR). */
    NON_CONFORME_FORMALITES_MANQUANTES,

    /** Aucun CPAP identifié — manquement employeur art. 32sexies § 2. */
    NON_CONFORME_PAS_DE_CONSEILLER,

    /** Inputs insuffisants — analyse complémentaire avocat requise. */
    A_QUALIFIER
}

package fr.ailegalcase.casefile;

/**
 * SF-219-07 : verdict de l'outil checklist procédurale licenciement
 * collectif BE selon la Loi du 13/02/1998 (procédure dite "Renault") +
 * CCT n° 24 (information / consultation des travailleurs) + CCT n° 39
 * (mesures sociales d'accompagnement).
 *
 * <p>L'outil n'est <b>pas un calculateur</b> mais une checklist
 * procédurale : il vérifie si l'employeur a respecté les 3 phases de la
 * procédure (information du CE/CPPT, consultation, décision +
 * notification autorité régionale) et le délai d'attente minimum de 30
 * jours après notification, qui conditionnent la validité du
 * licenciement collectif.</p>
 *
 * <p>Le non-respect de la procédure expose à des sanctions civiles :
 * réintégration / indemnité spéciale de protection (Loi 13/02/1998
 * art. 67) + nullité possible des préavis notifiés en violation du délai
 * d'attente.</p>
 *
 * <ul>
 *   <li>{@link #NON_APPLICABLE_SEUIL} — l'entreprise ne franchit pas le
 *       seuil de déclenchement du licenciement collectif (10/20/30
 *       licenciements sur 60 jours selon la taille de l'entreprise) —
 *       procédure Renault non applicable, mais d'autres procédures
 *       restent possibles (CCT n° 9 information générale, CCT n° 32bis
 *       transfert).</li>
 *   <li>{@link #CONFORME} — les 3 phases ont été respectées dans l'ordre
 *       et le délai d'attente de 30 jours après notification est échu —
 *       les préavis individuels peuvent être notifiés sans risque de
 *       nullité procédurale.</li>
 *   <li>{@link #NON_CONFORME_PHASE_INFORMATION_INCOMPLETE} — la phase 1
 *       d'information du CE / CPPT / délégation syndicale est incomplète
 *       (notification écrite manquante, documents légaux non fournis,
 *       motifs économiques non explicités).</li>
 *   <li>{@link #NON_CONFORME_CONSULTATION_INSUFFISANTE} — la phase 2 de
 *       consultation n'a pas permis un débat réel : pas de réunion
 *       organisée, pas de réponse motivée de l'employeur aux questions /
 *       contre-propositions des représentants.</li>
 *   <li>{@link #NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE} — la
 *       notification écrite à l'autorité régionale de l'emploi (Forem /
 *       Actiris / VDAB) n'a pas été effectuée — le délai d'attente de
 *       30 jours ne commence pas à courir, tout préavis est nul.</li>
 *   <li>{@link #NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE} — l'employeur a
 *       notifié les préavis individuels avant l'expiration du délai
 *       d'attente de 30 jours après notification à l'autorité — sanction
 *       nullité des préavis + indemnité spéciale art. 67.</li>
 * </ul>
 */
public enum LicenciementBeCollectifRenaultVerdict {
    /** Seuil non franchi : procédure Renault non applicable. */
    NON_APPLICABLE_SEUIL,

    /** 3 phases + délai 30 jours respectés : licenciement collectif valide. */
    CONFORME,

    /** Phase 1 information CE / CPPT incomplète : sanction art. 67. */
    NON_CONFORME_PHASE_INFORMATION_INCOMPLETE,

    /** Phase 2 consultation insuffisante (pas de débat réel) : sanction art. 67. */
    NON_CONFORME_CONSULTATION_INSUFFISANTE,

    /** Notification autorité régionale manquante : préavis nuls. */
    NON_CONFORME_NOTIFICATION_AUTORITE_MANQUANTE,

    /** Délai d'attente 30 jours non respecté : préavis nuls + indemnité art. 67. */
    NON_CONFORME_DELAI_ATTENTE_NON_RESPECTE
}
